package br.com.regulacheck.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-07 contra o <strong>Oracle real</strong> da faculdade (opt-in).
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO.
 * Pre-requisito: scripts 01, 02, 05 e 06 executados na conta (sem o 06, a troca de servico falha ao registrar a
 * revisao). Usa as contas ficticias da carga e <strong>cria e remove</strong> um protocolo de teste proprio
 * ({@code TESTE-RC07-...}); ao final remove somente os pedidos, revisoes e sessoes que ele mesmo criou.
 *
 * <p>E este teste que comprova o bloqueio de linha ({@code SELECT ... FOR UPDATE}) sob concorrencia real; o teste
 * equivalente com repositorio em memoria nao o comprova.
 */
class SolicitacaoOracleTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";
    private static final String EMAIL_A = "paciente.a@exemplo.invalid";
    private static final String EMAIL_B = "paciente.b@exemplo.invalid";
    private static final GeradorToken GERADOR = new GeradorToken();

    private static BancoDados banco;
    private static Configuracao configuracao;
    private static String identificadorTeste;
    private static final Set<Long> PEDIDOS = new HashSet<>();
    private static final Set<String> HASHES = new HashSet<>();

    @BeforeAll
    static void preparar() throws Exception {
        Configuracao ambiente = Configuracao.deAmbiente();
        assumeTrue("SIM".equals(System.getenv("RC_TESTE_ORACLE")), "RC_TESTE_ORACLE != SIM: teste de Oracle ignorado");
        assumeTrue(ambiente.banco().configurado(), "DB_* nao configuradas: teste de Oracle ignorado");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos());
        banco = BancoDados.criar(ambiente.banco());
        identificadorTeste = "TESTE-RC07-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement("INSERT INTO RC_PROTOCOLO (IDENTIFICADOR, VERSAO, LOCALIDADE, "
                        + "ESPECIALIDADE, AVISO_FICTICIO) VALUES (?, 'v0', 'Municipio de Teste', 'Teste', "
                        + "'Protocolo de teste automatizado, ficticio.')")) {
            p.setString(1, identificadorTeste);
            p.executeUpdate();
        }
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco == null) {
            return;
        }
        // Remove somente o que este teste criou: revisoes e pedidos por id, sessoes por hash, o protocolo de teste.
        try (Connection c = banco.conexao()) {
            for (long id : PEDIDOS) {
                executar(c, "DELETE FROM RC_REVISAO WHERE SOLICITACAO_ID = ?", id);
                executar(c, "DELETE FROM RC_SOLICITACAO WHERE ID = ?", id);
            }
            for (String hash : HASHES) {
                try (PreparedStatement p = c.prepareStatement("DELETE FROM RC_SESSAO_LOGIN WHERE TOKEN_HASH = ?")) {
                    p.setString(1, hash);
                    p.executeUpdate();
                }
            }
            try (PreparedStatement p = c.prepareStatement("DELETE FROM RC_PROTOCOLO WHERE IDENTIFICADOR = ?")) {
                p.setString(1, identificadorTeste);
                p.executeUpdate();
            }
        }
        banco.fechar();
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

    private static long criar(ServidorDeTeste api, Map<String, String> auth, String protocoloId) throws Exception {
        Resposta r = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + protocoloId + "\"}", auth);
        assertEquals(201, r.status(), r.corpo());
        long id = r.json().get("id").asLong();
        PEDIDOS.add(id);
        return id;
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

    private static String motivo(long id, int numero) throws Exception {
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement(
                        "SELECT MOTIVO FROM RC_REVISAO WHERE SOLICITACAO_ID = ? AND NUMERO = ?")) {
            p.setLong(1, id);
            p.setInt(2, numero);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }

    @Test
    void criarETrocarGravamPedidoERevisoesNoBanco() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            Resposta catalogo = api.get("/protocolos", auth);
            assertEquals(200, catalogo.status());
            String padrao = catalogo.json().get("itens").get(0).get("protocoloId").asText();
            String teste = identificadorTeste + "-v0";

            long id = criar(api, auth, padrao);
            Resposta criada = api.get("/solicitacoes/" + id, auth);
            assertEquals(1, criada.json().get("revisao").asInt());
            assertEquals(List.of(1), revisoes(id));
            assertEquals("CRIACAO", motivo(id, 1));

            Resposta trocada = api.enviar("PUT", "/solicitacoes/" + id, "{\"protocoloId\":\"" + teste + "\"}", auth);
            assertEquals(200, trocada.status(),
                    "se for 500: o script 06_motivo_revisao_protocolo.sql ainda nao foi executado nesta conta. "
                            + trocada.corpo());
            assertEquals(2, trocada.json().get("revisao").asInt());
            assertEquals(List.of(1, 2), revisoes(id));
            assertEquals("PROTOCOLO_ALTERADO", motivo(id, 2));
            // Os instantes devolvidos pela troca sao os mesmos que uma leitura posterior devolve (precisao do banco).
            assertEquals(trocada.json(), api.get("/solicitacoes/" + id, auth).json());

            Resposta mesmo = api.enviar("PUT", "/solicitacoes/" + id, "{\"protocoloId\":\"" + teste + "\"}", auth);
            assertEquals(200, mesmo.status());
            assertEquals(2, mesmo.json().get("revisao").asInt());
            assertEquals(List.of(1, 2), revisoes(id));
        }
    }

    @Test
    void isolamentoEntreAsDuasPacientesNoBancoReal() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> authA = entrar(api, EMAIL_A);
            Map<String, String> authB = entrar(api, EMAIL_B);
            String padrao = api.get("/protocolos", authA).json().get("itens").get(0).get("protocoloId").asText();
            long deA = criar(api, authA, padrao);

            Resposta alheio = api.get("/solicitacoes/" + deA, authB);
            Resposta inexistente = api.get("/solicitacoes/999999999", authB);
            Resposta alteracao = api.enviar("PUT", "/solicitacoes/" + deA, "{\"protocoloId\":\"" + padrao + "\"}",
                    authB);

            assertEquals(404, alheio.status());
            assertEquals(alheio.corpo(), inexistente.corpo());
            assertEquals(alheio.corpo(), alteracao.corpo());
            List<String> idsDeB = new ArrayList<>();
            api.get("/solicitacoes", authB).json().get("itens").forEach(n -> idsDeB.add(n.get("id").asText()));
            assertTrue(!idsDeB.contains(Long.toString(deA)), "pedido de A apareceu na lista de B");
        }
    }

    @Test
    void trocasSimultaneasNoOracleNaoGeramSaltoNemRepeticaoDeRevisao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api, EMAIL_A);
            String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
            String teste = identificadorTeste + "-v0";
            long id = criar(api, auth, padrao);

            int threads = 6;
            int tentativas = 6;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> trabalhos = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int inicio = t;
                trabalhos.add(pool.submit(() -> {
                    largada.await();
                    int sucessos = 0;
                    for (int i = 0; i < tentativas; i++) {
                        String alvo = (inicio + i) % 2 == 0 ? padrao : teste;
                        Resposta r = api.enviar("PUT", "/solicitacoes/" + id, "{\"protocoloId\":\"" + alvo + "\"}",
                                auth);
                        assertEquals(200, r.status(), r.corpo());
                        sucessos++;
                    }
                    return sucessos;
                }));
            }
            largada.countDown();
            for (Future<Integer> trabalho : trabalhos) {
                assertEquals(tentativas, trabalho.get(120, TimeUnit.SECONDS));
            }
            pool.shutdown();

            int atual = api.get("/solicitacoes/" + id, auth).json().get("revisao").asInt();
            List<Integer> numeros = revisoes(id);
            List<Integer> esperado = new ArrayList<>();
            for (int n = 1; n <= atual; n++) {
                esperado.add(n);
            }
            assertEquals(esperado, numeros, "revisoes devem ser 1..n, sem salto nem repeticao");
            assertTrue(atual > 1, "o cenario precisa ter trocado de servico ao menos uma vez");
        }
    }
}
