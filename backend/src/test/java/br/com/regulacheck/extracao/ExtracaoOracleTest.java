package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.MultipartTeste;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-10 contra o <strong>Oracle real</strong> da faculdade (opt-in): leitura simulada gravada e
 * consultada, uma leitura por versao sob duas threads, restricoes de campo e exclusao de rascunho sem residuo.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO. Pre-requisito:
 * scripts 01, 02, 05, 06, 07 e <strong>08_extracao.sql</strong> executados na conta (o {@code 08_} so roda com
 * autorizacao especifica do responsavel). Usa as contas ficticias da carga, as amostras sinteticas de
 * {@code samples/} e uma pasta temporaria propria; ao final remove somente as leituras, documentos, revisoes,
 * pedidos e sessoes que criou.
 *
 * <p>A leitura e <strong>simulada</strong>: este teste comprova a persistencia, as restricoes e o bloqueio de linha,
 * nao leitura real de arquivo.
 */
class ExtracaoOracleTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";
    private static final String EMAIL_A = "paciente.a@exemplo.invalid";
    private static final GeradorToken GERADOR = new GeradorToken();

    private static BancoDados banco;
    private static Configuracao configuracao;
    private static Path raiz;
    private static final Set<Long> PEDIDOS = new HashSet<>();
    private static final Set<String> HASHES = new HashSet<>();

    @BeforeAll
    static void preparar() throws Exception {
        Configuracao ambiente = Configuracao.deAmbiente();
        assumeTrue("SIM".equals(System.getenv("RC_TESTE_ORACLE")), "RC_TESTE_ORACLE != SIM: teste de Oracle ignorado");
        assumeTrue(ambiente.banco().configurado(), "DB_* nao configuradas: teste de Oracle ignorado");
        raiz = Files.createTempDirectory("regulacheck-oracle-ext-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
        banco = BancoDados.criar(ambiente.banco());
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            try (Connection c = banco.conexao()) {
                for (long id : PEDIDOS) {
                    executar(c, """
                            DELETE FROM RC_EXTRACAO_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN (
                                SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID
                                 WHERE e.SOLICITACAO_ID = ?)""", id);
                    executar(c, "DELETE FROM RC_EXTRACAO_DOCUMENTO WHERE EXTRACAO_ID IN "
                            + "(SELECT ID FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?)", id);
                    executar(c, "DELETE FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?", id);
                    executar(c, "DELETE FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?", id);
                    executar(c, "DELETE FROM RC_REVISAO WHERE SOLICITACAO_ID = ?", id);
                    executar(c, "DELETE FROM RC_SOLICITACAO WHERE ID = ?", id);
                }
                for (String hash : HASHES) {
                    try (PreparedStatement p = c.prepareStatement("DELETE FROM RC_SESSAO_LOGIN WHERE TOKEN_HASH = ?")) {
                        p.setString(1, hash);
                        p.executeUpdate();
                    }
                }
            }
            banco.fechar();
        }
        if (raiz != null) {
            try (Stream<Path> arvore = Files.walk(raiz)) {
                arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static void executar(Connection c, String sql, long id) throws Exception {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private static Map<String, String> entrar(ServidorDeTeste api) throws Exception {
        String token = api.entrar(EMAIL_A, SENHA_DEMONSTRACAO);
        HASHES.add(GERADOR.hash(token));
        return ServidorDeTeste.bearer(token);
    }

    private static long criarRascunho(ServidorDeTeste api, Map<String, String> auth) throws Exception {
        String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
        Resposta r = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + padrao + "\"}", auth);
        assertEquals(201, r.status(), r.corpo());
        long id = r.json().get("id").asLong();
        PEDIDOS.add(id);
        return id;
    }

    private static long enviar(ServidorDeTeste api, Map<String, String> auth, long pedido, String tipo, String nome,
            String mime, byte[] conteudo) throws Exception {
        MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", tipo).arquivo("arquivo", nome, mime,
                conteudo);
        Resposta r = api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(), corpo.contentType(),
                auth);
        assertEquals(201, r.status(), r.corpo());
        return r.json().get("id").asLong();
    }

    private static long enviarAmostra(ServidorDeTeste api, Map<String, String> auth, long pedido, Amostra amostra)
            throws Exception {
        String mime = amostra.nome().endsWith(".pdf") ? "application/pdf" : "image/png";
        return enviar(api, auth, pedido, CenarioExtracao.tipoInformado(amostra), amostra.nome(), mime,
                amostra.bytes());
    }

    private static Resposta ler(ServidorDeTeste api, Map<String, String> auth, long pedido) throws Exception {
        return api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes", null, auth);
    }

    private static int contar(String sql, long id) throws Exception {
        try (Connection c = banco.conexao(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static int leituras(long pedido) throws Exception {
        return contar("SELECT COUNT(*) FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?", pedido);
    }

    private static JsonNode campo(JsonNode documento, String codigo) {
        for (JsonNode c : documento.get("campos")) {
            if (c.get("codigo").asText().equals(codigo)) {
                return c;
            }
        }
        throw new AssertionError("campo ausente: " + codigo);
    }

    @Test
    void gravaConsultaERepeteComOsDadosDoGabaritoENoBancoRealRespeitaAsRestricoes() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            long pedido = criarRascunho(api, auth);
            long pdf = enviarAmostra(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            long foto = enviarAmostra(api, auth, pedido, AmostrasSinteticas.IDENTIFICACAO);
            long vazio = enviar(api, auth, pedido, "OUTRO", "qualquer.pdf", "application/pdf",
                    ArquivosDeTeste.pdf(1));

            Resposta criada = ler(api, auth, pedido);

            assertEquals(201, criada.status(), criada.corpo());
            JsonNode corpo = criada.json();
            assertEquals("ATUAL", corpo.get("situacao").asText());
            assertTrue(corpo.get("simulada").asBoolean());
            assertEquals(4, corpo.get("versaoConjuntoDocumental").asInt());
            for (JsonNode doc : corpo.get("documentos")) {
                long id = doc.get("documentoId").asLong();
                if (id == pdf) {
                    assertEquals("LIDO", doc.get("situacao").asText());
                    assertEquals("Avaliacao cardiologica ambulatorial (texto sintetico)",
                            campo(doc, "MOTIVO_ENCAMINHAMENTO").get("valor").asText());
                    assertEquals(1, campo(doc, "MOTIVO_ENCAMINHAMENTO").get("origem").get("pagina").asInt());
                    assertEquals("NAO_LOCALIZADO", campo(doc, "AUTORIZACAO_ASSINADA").get("situacao").asText());
                } else if (id == foto) {
                    assertEquals("Maria Exemplo da Silva", campo(doc, "NOME").get("valor").asText());
                    assertFalse(campo(doc, "NOME").get("origem").has("pagina"));
                } else {
                    assertEquals(vazio, id);
                    assertEquals("LEITURA_VAZIA", doc.get("situacao").asText());
                    assertEquals(0, doc.get("campos").size());
                }
            }
            assertEquals(1, leituras(pedido));
            assertEquals(3, contar("SELECT COUNT(*) FROM RC_EXTRACAO_DOCUMENTO WHERE EXTRACAO_ID IN "
                    + "(SELECT ID FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?)", pedido));
            // 6 campos em cada um dos 2 documentos lidos; o documento de leitura vazia nao tem campo.
            assertEquals(12, contar("SELECT COUNT(*) FROM RC_EXTRACAO_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN "
                    + "(SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID "
                    + "WHERE e.SOLICITACAO_ID = ?)", pedido));
            // Nenhum campo sem valor carrega valor ou origem (as CHECKs do 08_ sustentam isso).
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_EXTRACAO_CAMPO WHERE SITUACAO <> 'IDENTIFICADO' AND "
                    + "(VALOR IS NOT NULL OR PAGINA IS NOT NULL OR TRECHO IS NOT NULL) AND EXTRACAO_DOCUMENTO_ID IN "
                    + "(SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID "
                    + "WHERE e.SOLICITACAO_ID = ?)", pedido));

            Resposta repetida = ler(api, auth, pedido);
            assertEquals(200, repetida.status());
            assertEquals(corpo, repetida.json());
            assertEquals(1, leituras(pedido));
            assertEquals(corpo, api.get("/solicitacoes/" + pedido + "/extracoes", auth).json());
            assertEquals("RASCUNHO", api.get("/solicitacoes/" + pedido, auth).json().get("estado").asText());
        }
    }

    @Test
    void substituirDocumentoDeixaALeituraDesatualizadaENovaLeituraGravaOutraRevisao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            long pedido = criarRascunho(api, auth);
            long doc = enviarAmostra(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            assertEquals(201, ler(api, auth, pedido).status());

            MultipartTeste troca = new MultipartTeste().texto("tipoDocumental", "IDENTIFICACAO").arquivo("arquivo",
                    AmostrasSinteticas.IDENTIFICACAO.nome(), "image/png", AmostrasSinteticas.IDENTIFICACAO.bytes());
            Resposta substituicao = api.enviarBinario("PUT", "/solicitacoes/" + pedido + "/documentos/" + doc,
                    troca.corpo(), troca.contentType(), auth);
            assertEquals(201, substituicao.status(), substituicao.corpo());

            assertEquals("DESATUALIZADA",
                    api.get("/solicitacoes/" + pedido + "/extracoes", auth).json().get("situacao").asText());
            Resposta nova = ler(api, auth, pedido);
            assertEquals(201, nova.status());
            assertEquals("ATUAL", nova.json().get("situacao").asText());
            assertEquals("IDENTIFICACAO", campo(nova.json().get("documentos").get(0), "TIPO_DOCUMENTO").get("valor")
                    .asText());
            assertEquals(2, leituras(pedido));
        }
    }

    @Test
    void leiturasSimultaneasDaMesmaVersaoGravamUmaSoSobOBloqueioDeLinha() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            long pedido = criarRascunho(api, auth);
            enviarAmostra(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            enviarAmostra(api, auth, pedido, AmostrasSinteticas.IDENTIFICACAO);

            int threads = 6;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resposta>> futuros = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return ler(api, auth, pedido);
                }));
            }
            largada.countDown();
            int criadas = 0;
            for (Future<Resposta> futuro : futuros) {
                Resposta r = futuro.get(60, TimeUnit.SECONDS);
                assertTrue(r.status() == 200 || r.status() == 201, r.corpo());
                if (r.status() == 201) {
                    criadas++;
                }
            }
            pool.shutdownNow();

            assertEquals(1, criadas);
            assertEquals(1, leituras(pedido));
        }
    }

    @Test
    void excluirRascunhoComLeituraApagaTudoNaOrdemDasChavesEstrangeiras() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            long pedido = criarRascunho(api, auth);
            enviarAmostra(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            assertEquals(201, ler(api, auth, pedido).status());
            assertEquals(1, leituras(pedido));
            Map<String, String> cabecalhos = new HashMap<>(auth);
            cabecalhos.put("X-Confirmar-Exclusao", "EXCLUIR");

            Resposta r = api.enviar("DELETE", "/solicitacoes/" + pedido, null, cabecalhos);

            assertEquals(204, r.status(), r.corpo());
            assertEquals(0, leituras(pedido));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?", pedido));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_SOLICITACAO WHERE ID = ?", pedido));
            assertEquals(404, api.get("/solicitacoes/" + pedido + "/extracoes", auth).status());
        }
    }
}
