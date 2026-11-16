package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-21/RC-22 contra o <strong>Oracle real</strong> da faculdade (opt-in): abertura da sessao com o
 * bloqueio de linha real e o indice {@code RC_SESSAO_CAPTURA_UK_ABERTA}, leituras gravadas pela rota do simulador,
 * telemetria lida pelas consultas JDBC e exclusao do rascunho apagando sessao e leituras.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como <strong>NAO VERIFICADO</strong>.
 * Pre-requisito: scripts 01 a 10 e <strong>11_iot_sessao_captura.sql</strong> executados na conta (ou um 01_ atual).
 * Usa a conta ficticia da carga e uma chave ficticia do simulador definida so neste teste; ao final remove somente o
 * que criou. Os sensores sao <strong>simulados</strong>.
 */
class CapturaOracleTest {

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
        banco = BancoDados.criar(ambiente.banco());
        // Sem o 11_ a API funciona, mas a garantia de sessao unica no banco nao existe: falha aqui, antes de criar
        // qualquer dado no banco compartilhado.
        assertTrue(indiceDaRc21Aplicado(), "Esquema sem a RC-21: execute database/scripts/11_iot_sessao_captura.sql "
                + "na conta configurada em DB_USERNAME antes deste teste (nenhum dado foi criado)");
        raiz = Files.createTempDirectory("regulacheck-oracle-iot-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(),
                CenarioIot.CHAVE, raiz.toString(), null);
    }

    private static boolean indiceDaRc21Aplicado() throws Exception {
        try (Connection c = banco.conexao(); PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM USER_INDEXES WHERE INDEX_NAME = 'RC_SESSAO_CAPTURA_UK_ABERTA'");
                ResultSet r = p.executeQuery()) {
            r.next();
            return r.getInt(1) == 1;
        }
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            try (Connection c = banco.conexao()) {
                for (long id : PEDIDOS) {
                    executar(c, "DELETE FROM RC_LEITURA_SENSOR WHERE SESSAO_ID IN "
                            + "(SELECT ID FROM RC_SESSAO_CAPTURA WHERE SOLICITACAO_ID = ?)", id);
                    executar(c, "DELETE FROM RC_SESSAO_CAPTURA WHERE SOLICITACAO_ID = ?", id);
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

    @Test
    void abreEnviaConsultaEExcluiNoOracle() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String pedido = criarPedido(api, auth);

            Resposta aberta = api.enviar("POST", "/solicitacoes/" + pedido + "/sessoes-captura", null, auth);
            assertEquals(201, aberta.status(), aberta.corpo());
            String sessao = aberta.json().get("id").asText();
            Resposta reaberta = api.enviar("POST", "/solicitacoes/" + pedido + "/sessoes-captura", null, auth);
            assertEquals(200, reaberta.status(), reaberta.corpo());
            assertEquals(sessao, reaberta.json().get("id").asText());

            Map<String, String> simulador = Map.of("X-Chave-Simulador", CenarioIot.CHAVE);
            String caminho = "/sessoes-captura/" + sessao + "/leituras";
            Instant agora = Instant.now();
            assertEquals(201, api.postJson(caminho, CenarioIot.leitura(sessao, "LUMINOSIDADE", "120.5", "LUX",
                    agora.minusSeconds(2)), simulador).status());
            assertEquals(201, api.postJson(caminho, CenarioIot.leitura(sessao, "LUMINOSIDADE", "480", "LUX", agora),
                    simulador).status());
            assertEquals(201, api.postJson(caminho, CenarioIot.leitura(sessao, "DISTANCIA", "30", "CM", agora),
                    simulador).status());
            assertEquals(400, api.postJson(caminho, CenarioIot.leitura(sessao, "DISTANCIA", "30", "LUX", agora),
                    simulador).status());

            JsonNode telemetria = api.get("/solicitacoes/" + pedido + "/leituras", auth).json();
            assertEquals(sessao, telemetria.get("sessao").get("id").asText());
            assertEquals("DENTRO_DA_FAIXA", telemetria.get("sensores").get(0).get("situacao").asText());
            assertEquals(480, telemetria.get("sensores").get(0).get("ultimaLeitura").get("valor").asInt());
            assertEquals("DENTRO_DA_FAIXA", telemetria.get("sensores").get(1).get("situacao").asText());
            assertEquals(3, contar("SELECT COUNT(*) FROM RC_LEITURA_SENSOR WHERE SESSAO_ID = ?", Long.parseLong(sessao)));

            Resposta excluido = api.enviar("DELETE", "/solicitacoes/" + pedido, null,
                    Map.of("Authorization", auth.get("Authorization"), "X-Confirmar-Exclusao", "EXCLUIR"));
            assertEquals(204, excluido.status(), excluido.corpo());
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_SESSAO_CAPTURA WHERE SOLICITACAO_ID = ?",
                    Long.parseLong(pedido)));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_LEITURA_SENSOR WHERE SESSAO_ID = ?",
                    Long.parseLong(sessao)));
        }
    }

    @Test
    void aberturasSimultaneasCriamUmaSessaoNoOracle() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String pedido = criarPedido(api, auth);
            ExecutorService executor = Executors.newFixedThreadPool(5);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resposta>> futuros = new ArrayList<>();
            try {
                for (int i = 0; i < 5; i++) {
                    futuros.add(executor.submit(() -> {
                        largada.await();
                        return api.enviar("POST", "/solicitacoes/" + pedido + "/sessoes-captura", null, auth);
                    }));
                }
                largada.countDown();
                int criadas = 0;
                for (Future<Resposta> futuro : futuros) {
                    Resposta r = futuro.get();
                    assertTrue(r.status() == 201 || r.status() == 200, r.corpo());
                    criadas += r.status() == 201 ? 1 : 0;
                }
                assertEquals(1, criadas);
                assertEquals(1, contar("SELECT COUNT(*) FROM RC_SESSAO_CAPTURA WHERE SOLICITACAO_ID = ?",
                        Long.parseLong(pedido)));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static String criarPedido(ServidorDeTeste api, Map<String, String> auth) throws Exception {
        String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
        Resposta criado = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + padrao + "\"}", auth);
        assertEquals(201, criado.status(), criado.corpo());
        PEDIDOS.add(criado.json().get("id").asLong());
        return criado.json().get("id").asText();
    }

    private static Map<String, String> entrar(ServidorDeTeste api) throws Exception {
        String token = api.entrar(EMAIL_A, SENHA_DEMONSTRACAO);
        HASHES.add(GERADOR.hash(token));
        return ServidorDeTeste.bearer(token);
    }

    private static void executar(Connection c, String sql, long id) throws Exception {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            p.executeUpdate();
        }
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
}
