package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Verificacao da RC-08 contra o <strong>Oracle real</strong> da faculdade (opt-in).
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO.
 * Pre-requisito: scripts 01, 02, 05, 06 e <strong>07</strong> executados na conta (sem o 07, o envio falha ao
 * gravar). Usa as contas ficticias da carga e uma pasta temporaria propria para os arquivos; ao final remove
 * somente os documentos, revisoes, pedidos e sessoes que ele mesmo criou, e a pasta.
 *
 * <p>E este teste que comprova, no banco real, o bloqueio de linha sob envios simultaneos (limite de 3 e numeracao
 * de revisoes) e a restricao unica da chave de envio; o teste equivalente com repositorio em memoria
 * ({@code ConcorrenciaEnvioDocumentoTest}) nao os comprova.
 */
class DocumentoOracleTest {

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
        raiz = Files.createTempDirectory("regulacheck-oracle-docs-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
        banco = BancoDados.criar(ambiente.banco());
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            // Remove somente o que este teste criou: documentos, revisoes e pedidos por id; sessoes por hash.
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

    private static Resposta enviar(ServidorDeTeste api, Map<String, String> auth, long pedido, String tipo,
            byte[] conteudo, String chave) throws Exception {
        MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", tipo).arquivo("arquivo", "teste.pdf",
                "application/pdf", conteudo);
        Map<String, String> cabecalhos = new java.util.HashMap<>(auth);
        if (chave != null) {
            cabecalhos.put("X-Chave-Envio", chave);
        }
        return api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(), corpo.contentType(),
                cabecalhos);
    }

    private static List<Integer> revisoes(long id) throws Exception {
        List<Integer> numeros = new ArrayList<>();
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement(
                        "SELECT NUMERO FROM RC_REVISAO WHERE SOLICITACAO_ID = ? ORDER BY NUMERO")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    numeros.add(r.getInt(1));
                }
            }
        }
        return numeros;
    }

    private static int contarDocumentos(long pedido) throws Exception {
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?")) {
            p.setLong(1, pedido);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static long arquivosGuardados() throws Exception {
        try (Stream<Path> itens = Files.list(raiz)) {
            return itens.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void envioGravaDocumentoERevisaoNoBancoEODownloadDevolveOsMesmosBytes() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            byte[] pdf = ArquivosDeTeste.pdf(2);
            String chave = "oracle-" + UUID.randomUUID();

            Resposta envio = enviar(api, auth, pedido, "ENCAMINHAMENTO", pdf, chave);

            assertEquals(201, envio.status(), "se for 500: o script 07_documento_tipo_e_tentativa.sql ainda nao foi "
                    + "executado nesta conta. " + envio.corpo());
            assertEquals(List.of(1, 2), revisoes(pedido));
            try (Connection c = banco.conexao();
                    PreparedStatement p = c.prepareStatement("SELECT TIPO_DOCUMENTAL, HASH_CONTEUDO, REVISAO_ENVIO, "
                            + "CHAVE_ENVIO, TIPO, PAGINAS FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?")) {
                p.setLong(1, pedido);
                try (ResultSet r = p.executeQuery()) {
                    assertTrue(r.next());
                    assertEquals("ENCAMINHAMENTO", r.getString("TIPO_DOCUMENTAL"));
                    assertTrue(r.getString("HASH_CONTEUDO").matches("[0-9a-f]{64}"));
                    assertEquals(2, r.getInt("REVISAO_ENVIO"));
                    assertEquals(chave, r.getString("CHAVE_ENVIO"));
                    assertEquals("PDF", r.getString("TIPO"));
                    assertEquals(2, r.getInt("PAGINAS"));
                }
            }
            JsonNode pedidoLido = api.get("/solicitacoes/" + pedido, auth).json();
            assertEquals(2, pedidoLido.get("versaoConjuntoDocumental").asInt());
            assertEquals(1, pedidoLido.get("documentosDisponiveis").asInt());

            Resposta repetido = enviar(api, auth, pedido, "ENCAMINHAMENTO", pdf, chave);
            assertEquals(200, repetido.status(), repetido.corpo());
            assertEquals(envio.json().get("id"), repetido.json().get("id"));
            assertEquals(1, contarDocumentos(pedido));
            assertEquals(List.of(1, 2), revisoes(pedido));
            assertEquals(409, enviar(api, auth, pedido, "OUTRO", pdf, chave).status());

            long id = envio.json().get("id").asLong();
            var baixa = api.getBinario("/solicitacoes/" + pedido + "/documentos/" + id, auth);
            assertEquals(200, baixa.status());
            assertArrayEquals(pdf, baixa.corpo());
        }
    }

    @Test
    void isolamentoEntreAsDuasPacientesNoBancoReal() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> authA = entrar(api, EMAIL_A);
            Map<String, String> authB = entrar(api, EMAIL_B);
            long pedido = criarRascunho(api, authA);
            long id = enviar(api, authA, pedido, "OUTRO", ArquivosDeTeste.pdf(1), null).json().get("id").asLong();

            Resposta listaAlheia = api.get("/solicitacoes/" + pedido + "/documentos", authB);
            var baixaAlheia = api.getBinario("/solicitacoes/" + pedido + "/documentos/" + id, authB);
            Resposta envioAlheio = enviar(api, authB, pedido, "OUTRO", ArquivosDeTeste.pdf(1), null);

            assertEquals(404, listaAlheia.status());
            assertEquals(404, baixaAlheia.status());
            assertEquals(404, envioAlheio.status());
            assertEquals(1, contarDocumentos(pedido));
        }
    }

    @Test
    void enviosSimultaneosNaoPassamDeTresDocumentosNemGeramSaltoDeRevisao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> trabalhos = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int numero = t;
                trabalhos.add(pool.submit(() -> {
                    largada.await();
                    return enviar(api, auth, pedido, "OUTRO", ArquivosDeTeste.pdf(1), "chave-oracle-" + numero)
                            .status();
                }));
            }
            largada.countDown();
            int aceitos = 0;
            int recusados = 0;
            for (Future<Integer> trabalho : trabalhos) {
                int status = trabalho.get(120, TimeUnit.SECONDS);
                if (status == 201) {
                    aceitos++;
                } else {
                    assertEquals(422, status, "unica recusa admitida: limite de documentos");
                    recusados++;
                }
            }
            pool.shutdown();

            assertEquals(3, aceitos);
            assertEquals(threads - 3, recusados);
            assertEquals(3, contarDocumentos(pedido));
            assertEquals(List.of(1, 2, 3, 4), revisoes(pedido), "revisoes 1..n, sem salto nem repeticao");
        }
    }

    @Test
    void mesmaChaveSimultaneaGeraUmDocumentoEUmaRevisao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            long pedido = criarRascunho(api, auth);
            byte[] pdf = ArquivosDeTeste.pdf(1);
            String chave = "mesma-chave-" + UUID.randomUUID();
            long antes = arquivosGuardados();
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> trabalhos = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                trabalhos.add(pool.submit(() -> {
                    largada.await();
                    return enviar(api, auth, pedido, "OUTRO", pdf, chave).status();
                }));
            }
            largada.countDown();
            int criados = 0;
            for (Future<Integer> trabalho : trabalhos) {
                int status = trabalho.get(120, TimeUnit.SECONDS);
                assertTrue(status == 201 || status == 200, "status " + status);
                criados += status == 201 ? 1 : 0;
            }
            pool.shutdown();

            assertEquals(1, criados);
            assertEquals(1, contarDocumentos(pedido));
            assertEquals(List.of(1, 2), revisoes(pedido));
            assertEquals(antes + 1, arquivosGuardados(), "os arquivos das repeticoes foram removidos");
        }
    }

    @Test
    void documentosDaCargaSinteticaAparecemNaListaESemArquivoDao503NoDownload() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            JsonNode itens = api.get("/solicitacoes", auth).json().get("itens");
            // A conta tambem tem documentos reais criados por outros testes (download 200): a API nao distingue a
            // origem, entao percorre todos. Cada download e 200 (arquivo guardado) ou 503 (carga sintetica, sem
            // arquivo em disco); nunca outro codigo.
            int listados = 0;
            int semArquivo = 0;
            for (JsonNode item : itens) {
                if (item.get("documentosDisponiveis").asInt() == 0) {
                    continue;
                }
                long pedido = item.get("id").asLong();
                Resposta lista = api.get("/solicitacoes/" + pedido + "/documentos", auth);
                assertEquals(200, lista.status(), lista.corpo());
                for (JsonNode documento : lista.json().get("documentos")) {
                    listados++;
                    int status = api.getBinario("/solicitacoes/" + pedido + "/documentos/"
                            + documento.get("id").asLong(), auth).status();
                    assertTrue(status == 200 || status == 503, "download com status " + status);
                    semArquivo += status == 503 ? 1 : 0;
                }
            }
            assumeTrue(listados > 0, "a conta nao tem pedido com documentos");
            assumeTrue(semArquivo > 0, "a conta nao tem documento da carga sintetica (sem arquivo em disco)");
        }
    }
}
