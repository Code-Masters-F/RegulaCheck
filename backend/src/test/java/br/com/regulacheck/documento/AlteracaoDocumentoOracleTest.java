package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
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
import java.util.UUID;
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
 * Verificacao da RC-09 contra o <strong>Oracle real</strong> da faculdade (opt-in): remover, substituir e excluir.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO. Pre-requisito:
 * scripts 01, 02, 05, 06 e 07 executados na conta (a RC-09 nao tem script novo). Usa as contas ficticias da carga e
 * uma pasta temporaria propria; ao final remove somente os documentos, revisoes, pedidos e sessoes que criou.
 *
 * <p>E este teste que comprova, no banco real, o {@code SELECT ... FOR UPDATE} sob remocoes e substituicoes
 * simultaneas, a anulacao da chave de envio (indice unico) e a ordem de exclusao pelas chaves estrangeiras.
 * <strong>Nao comprova</strong> a invalidacao de analise vigente nem os estados de analise e de submissao: nenhuma
 * rota os produz ainda, entao o {@code UPDATE RC_ANALISE} e exercitado apenas como "zero linhas".
 */
class AlteracaoDocumentoOracleTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";
    private static final String EMAIL_A = "paciente.a@exemplo.invalid";
    private static final String EMAIL_B = "paciente.b@exemplo.invalid";
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
        raiz = Files.createTempDirectory("regulacheck-oracle-alt-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
        banco = BancoDados.criar(ambiente.banco());
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            try (Connection c = banco.conexao()) {
                for (long id : PEDIDOS) {
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

    private static Map<String, String> entrar(ServidorDeTeste api, String email) throws Exception {
        String token = api.entrar(email, SENHA_DEMONSTRACAO);
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

    private static MultipartTeste multipart(String tipo, byte[] conteudo) {
        return new MultipartTeste().texto("tipoDocumental", tipo).arquivo("arquivo", "teste.pdf", "application/pdf",
                conteudo);
    }

    private static Resposta enviar(ServidorDeTeste api, Map<String, String> auth, long pedido, byte[] conteudo,
            String chave) throws Exception {
        MultipartTeste corpo = multipart("OUTRO", conteudo);
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (chave != null) {
            cabecalhos.put("X-Chave-Envio", chave);
        }
        return api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(), corpo.contentType(),
                cabecalhos);
    }

    private static Resposta substituir(ServidorDeTeste api, Map<String, String> auth, long pedido, long documento,
            byte[] conteudo, String chave) throws Exception {
        MultipartTeste corpo = multipart("OUTRO", conteudo);
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (chave != null) {
            cabecalhos.put("X-Chave-Envio", chave);
        }
        return api.enviarBinario("PUT", "/solicitacoes/" + pedido + "/documentos/" + documento, corpo.corpo(),
                corpo.contentType(), cabecalhos);
    }

    private static Resposta remover(ServidorDeTeste api, Map<String, String> auth, long pedido, long documento)
            throws Exception {
        return api.enviar("DELETE", "/solicitacoes/" + pedido + "/documentos/" + documento, null, auth);
    }

    private static Resposta excluir(ServidorDeTeste api, Map<String, String> auth, long pedido, String confirmacao)
            throws Exception {
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (confirmacao != null) {
            cabecalhos.put("X-Confirmar-Exclusao", confirmacao);
        }
        return api.enviar("DELETE", "/solicitacoes/" + pedido, null, cabecalhos);
    }

    private static List<String> revisoes(long id) throws Exception {
        List<String> itens = new ArrayList<>();
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement(
                        "SELECT NUMERO, MOTIVO FROM RC_REVISAO WHERE SOLICITACAO_ID = ? ORDER BY NUMERO")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    itens.add(r.getInt(1) + ":" + r.getString(2));
                }
            }
        }
        return itens;
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

    private static int disponiveis(long pedido) throws Exception {
        return contar("SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ? AND REVISAO_REMOCAO IS NULL", pedido);
    }

    private static long arquivosGuardados() throws Exception {
        try (Stream<Path> itens = Files.list(raiz)) {
            return itens.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void removerMarcaORegistroAnulaAChaveApagaOArquivoELiberaAChaveNoIndiceUnico() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            String chave = "alt-" + UUID.randomUUID();
            long antes = arquivosGuardados();
            Resposta envio = enviar(api, auth, pedido, ArquivosDeTeste.pdf(1), chave);
            assertEquals(201, envio.status(), envio.corpo());
            long doc = envio.json().get("id").asLong();
            assertEquals(antes + 1, arquivosGuardados());

            Resposta r = remover(api, auth, pedido, doc);

            assertEquals(200, r.status(), r.corpo());
            assertEquals(3, r.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(0, r.json().get("documentosDisponiveis").asInt());
            assertEquals(List.of("1:CRIACAO", "2:DOCUMENTO_ENVIADO", "3:DOCUMENTO_REMOVIDO"), revisoes(pedido));
            try (Connection c = banco.conexao();
                    PreparedStatement p = c.prepareStatement(
                            "SELECT REVISAO_REMOCAO, CHAVE_ENVIO FROM RC_DOCUMENTO WHERE ID = ?")) {
                p.setLong(1, doc);
                try (ResultSet linha = p.executeQuery()) {
                    assertTrue(linha.next(), "o registro fica como historico");
                    assertEquals(3, linha.getInt("REVISAO_REMOCAO"));
                    assertNull(linha.getString("CHAVE_ENVIO"), "a chave e anulada para liberar o indice unico");
                }
            }
            assertEquals(antes, arquivosGuardados(), "o arquivo foi apagado");
            assertEquals(404, api.get("/solicitacoes/" + pedido + "/documentos/" + doc, auth).status());
            assertEquals(404, remover(api, auth, pedido, doc).status(), "repetir nao remove outro nem muda a versao");
            assertEquals(List.of("1:CRIACAO", "2:DOCUMENTO_ENVIADO", "3:DOCUMENTO_REMOVIDO"), revisoes(pedido));
            // Mesma chave de novo: sem anular a chave, o indice unico dispararia ORA-00001.
            assertEquals(201, enviar(api, auth, pedido, ArquivosDeTeste.pdf(2), chave).status());
        }
    }

    @Test
    void substituirTrocaOsDoisRegistrosNumaRevisaoSoERecusaOUsoIndevidoDaChave() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            long doc = enviar(api, auth, pedido, ArquivosDeTeste.pdf(1), null).json().get("id").asLong();
            byte[] novo = ArquivosDeTeste.pdf(2);
            String chave = "troca-" + UUID.randomUUID();
            long antes = arquivosGuardados();

            Resposta troca = substituir(api, auth, pedido, doc, novo, chave);

            assertEquals(201, troca.status(), troca.corpo());
            long idNovo = troca.json().get("id").asLong();
            assertEquals(3, troca.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(1, troca.json().get("documentosDisponiveis").asInt());
            assertEquals(List.of("1:CRIACAO", "2:DOCUMENTO_ENVIADO", "3:DOCUMENTO_SUBSTITUIDO"), revisoes(pedido));
            assertEquals(1, disponiveis(pedido));
            assertEquals(antes, arquivosGuardados(), "entrou um arquivo e saiu outro");
            var baixa = api.getBinario("/solicitacoes/" + pedido + "/documentos/" + idNovo, auth);
            assertArrayEquals(novo, baixa.corpo());
            assertEquals(404, api.get("/solicitacoes/" + pedido + "/documentos/" + doc, auth).status());

            // Repeticao reconhecida antes de procurar o documento do caminho (que ja nao existe).
            Resposta repetida = substituir(api, auth, pedido, doc, novo, chave);
            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals(idNovo, repetida.json().get("id").asLong());
            assertEquals(List.of("1:CRIACAO", "2:DOCUMENTO_ENVIADO", "3:DOCUMENTO_SUBSTITUIDO"), revisoes(pedido));
            assertEquals(409, substituir(api, auth, pedido, idNovo, ArquivosDeTeste.pdf(3), chave).status());
            assertEquals(415, substituir(api, auth, pedido, idNovo, ArquivosDeTeste.texto(), null).status());
            assertEquals(antes, arquivosGuardados(), "recusas nao deixam arquivo novo");
            assertEquals(1, disponiveis(pedido));

            // Outra paciente nao enxerga o documento.
            Map<String, String> authB = entrar(api, EMAIL_B);
            assertEquals(404, remover(api, authB, pedido, idNovo).status());
            assertEquals(1, disponiveis(pedido));
        }
    }

    @Test
    void excluirRascunhoApagaDocumentosRevisoesPedidoEArquivosNaOrdemDasChaves() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            long antes = arquivosGuardados();
            long d1 = enviar(api, auth, pedido, ArquivosDeTeste.pdf(1), null).json().get("id").asLong();
            enviar(api, auth, pedido, ArquivosDeTeste.pdf(2), null);
            assertEquals(200, remover(api, auth, pedido, d1).status(), "um removido tambem sai na exclusao");

            assertEquals(400, excluir(api, auth, pedido, null).status());
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_SOLICITACAO WHERE ID = ?", pedido), "nada foi apagado");
            Resposta r = excluir(api, auth, pedido, "EXCLUIR");

            assertEquals(204, r.status(), r.corpo());
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?", pedido));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_REVISAO WHERE SOLICITACAO_ID = ?", pedido));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_SOLICITACAO WHERE ID = ?", pedido));
            assertEquals(antes, arquivosGuardados(), "nenhum arquivo do pedido restou");
            assertEquals(404, api.get("/solicitacoes/" + pedido, auth).status());
            assertEquals(404, excluir(api, auth, pedido, "EXCLUIR").status());
        }
    }

    @Test
    void removerESubstituirSimultaneosNaoGeramSaltoDeRevisaoNemPassamDeTresNemDeixamArquivoOrfao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            long antes = arquivosGuardados();
            long d1 = enviar(api, auth, pedido, ArquivosDeTeste.pdf(1), null).json().get("id").asLong();
            long d2 = enviar(api, auth, pedido, ArquivosDeTeste.pdf(2), null).json().get("id").asLong();
            enviar(api, auth, pedido, ArquivosDeTeste.pdf(3), null);
            byte[] novo = ArquivosDeTeste.pdf(4);
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> trabalhos = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                boolean remocao = t % 2 == 0;
                trabalhos.add(pool.submit(() -> {
                    largada.await();
                    return remocao ? remover(api, auth, pedido, d1).status()
                            : substituir(api, auth, pedido, d2, novo, null).status();
                }));
            }
            largada.countDown();
            int remocoes = 0;
            int trocas = 0;
            for (int t = 0; t < threads; t++) {
                int status = trabalhos.get(t).get(120, TimeUnit.SECONDS);
                if (t % 2 == 0) {
                    assertTrue(status == 200 || status == 404, "remover: status " + status);
                    remocoes += status == 200 ? 1 : 0;
                } else {
                    assertTrue(status == 201 || status == 404, "substituir: status " + status);
                    trocas += status == 201 ? 1 : 0;
                }
            }
            pool.shutdown();

            assertEquals(1, remocoes, "so uma remocao vale");
            assertEquals(1, trocas, "so uma troca vale");
            assertEquals(2, disponiveis(pedido));
            assertEquals(List.of("1:CRIACAO", "2:DOCUMENTO_ENVIADO", "3:DOCUMENTO_ENVIADO", "4:DOCUMENTO_ENVIADO"),
                    revisoes(pedido).subList(0, 4));
            List<String> todas = revisoes(pedido);
            assertEquals(6, todas.size(), "criacao + 3 envios + 1 remocao + 1 troca, sem salto nem repeticao");
            for (int i = 0; i < todas.size(); i++) {
                assertEquals(String.valueOf(i + 1), todas.get(i).split(":")[0]);
            }
            assertEquals(antes + 2, arquivosGuardados(), "so os arquivos dos 2 documentos disponiveis restam");
        }
    }
}
