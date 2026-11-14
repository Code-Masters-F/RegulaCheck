package br.com.regulacheck.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.MultipartTeste;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.extracao.CenarioExtracao;
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
 * Verificacao da RC-13 contra o <strong>Oracle real</strong> da faculdade (opt-in): analise gravada com resultados,
 * pendencias com motivo e evidencias (origem lida da leitura da revisao), repeticao sem duplicar, segunda analise da
 * mesma revisao depois de completar a conferencia (restricao {@code RC_ANALISE_UK_CONFERENCIA} e o
 * {@code CONFERENCIA_EM} sem fuso) e pedidos simultaneos sob o bloqueio de linha real.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como <strong>NAO VERIFICADO</strong>.
 * Pre-requisito: scripts 01 a 09 e <strong>10_analise_historico.sql</strong> executados na conta (ou um 01_ atual).
 * Usa a conta ficticia da carga, as amostras sinteticas oficiais e uma pasta temporaria propria; ao final remove
 * somente o que criou. A leitura e <strong>simulada</strong>.
 */
class AnaliseOracleTest {

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
        // Sem o 10_ as rotas de analise respondem 503 e a limpeza nao acharia as tabelas novas: falha aqui, antes de
        // criar qualquer dado no banco compartilhado.
        assertTrue(esquemaDaRc13Aplicado(), "Esquema sem a RC-13: execute database/scripts/10_analise_historico.sql "
                + "na conta configurada em DB_USERNAME antes deste teste (nenhum dado foi criado)");
        raiz = Files.createTempDirectory("regulacheck-oracle-analise-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
    }

    /** @return {@code true} se a conta tem as colunas e a tabela criadas pelo script 10_ */
    private static boolean esquemaDaRc13Aplicado() throws Exception {
        try (Connection c = banco.conexao(); PreparedStatement p = c.prepareStatement("""
                SELECT (SELECT COUNT(*) FROM USER_TAB_COLUMNS
                         WHERE (TABLE_NAME, COLUMN_NAME) IN (('RC_SOLICITACAO', 'ANALISE_INICIADA_EM'),
                               ('RC_ANALISE', 'CONFERENCIA_EM'), ('RC_ANALISE', 'DATA_REFERENCIA'),
                               ('RC_PENDENCIA', 'MOTIVO')))
                     + (SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'RC_PENDENCIA_EVIDENCIA')
                  FROM DUAL"""); ResultSet r = p.executeQuery()) {
            r.next();
            return r.getInt(1) == 5;
        }
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            try (Connection c = banco.conexao()) {
                for (long id : PEDIDOS) {
                    String daAnalise = "(SELECT ID FROM RC_ANALISE WHERE SOLICITACAO_ID = ?)";
                    executar(c, "DELETE FROM RC_PENDENCIA_EVIDENCIA WHERE ANALISE_ID IN " + daAnalise, id);
                    executar(c, "DELETE FROM RC_PENDENCIA WHERE ANALISE_ID IN " + daAnalise, id);
                    executar(c, "DELETE FROM RC_RESULTADO_REGRA WHERE ANALISE_ID IN " + daAnalise, id);
                    executar(c, "DELETE FROM RC_ANALISE WHERE SOLICITACAO_ID = ?", id);
                    executar(c, """
                            DELETE FROM RC_CONFERENCIA_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN (
                                SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID
                                 WHERE e.SOLICITACAO_ID = ?)""", id);
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

    private static int contar(String sql, long id) throws Exception {
        try (Connection c = banco.conexao(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static Map<String, String> entrar(ServidorDeTeste api) throws Exception {
        String token = api.entrar(EMAIL_A, SENHA_DEMONSTRACAO);
        HASHES.add(GERADOR.hash(token));
        return ServidorDeTeste.bearer(token);
    }

    /** Pedido com o encaminhamento e a identificacao oficiais enviados e lidos; devolve pedido e leitura. */
    private static String[] pedidoLido(ServidorDeTeste api, Map<String, String> auth) throws Exception {
        String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
        Resposta criado = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + padrao + "\"}", auth);
        assertEquals(201, criado.status(), criado.corpo());
        long pedido = criado.json().get("id").asLong();
        PEDIDOS.add(pedido);
        for (Amostra amostra : AmostrasSinteticas.oficiais()) {
            String mime = amostra.nome().endsWith(".pdf") ? "application/pdf" : "image/png";
            MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", CenarioExtracao.tipoInformado(amostra))
                    .arquivo("arquivo", amostra.nome(), mime, amostra.bytes());
            Resposta enviado = api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(),
                    corpo.contentType(), auth);
            assertEquals(201, enviado.status(), enviado.corpo());
        }
        Resposta lida = api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes", null, auth);
        assertEquals(201, lida.status(), lida.corpo());
        return new String[] {Long.toString(pedido), lida.json().get("id").asText()};
    }

    /** Confirma todos os campos identificados da leitura pela rota real da RC-11. */
    private static void confirmarTudo(ServidorDeTeste api, Map<String, String> auth, String pedido, String leitura)
            throws Exception {
        String caminho = "/solicitacoes/" + pedido + "/extracoes/" + leitura + "/conferencia";
        List<String> itens = new ArrayList<>();
        for (JsonNode documento : api.get(caminho, auth).json().get("documentos")) {
            for (JsonNode campo : documento.get("campos")) {
                if ("IDENTIFICADO".equals(campo.get("situacao").asText())) {
                    itens.add("{\"documentoId\":\"" + documento.get("documentoId").asText() + "\",\"codigo\":\""
                            + campo.get("codigo").asText() + "\",\"decisao\":\"CONFIRMADA\"}");
                }
            }
        }
        Resposta r = api.enviar("PUT", caminho, "{\"decisoes\":[" + String.join(",", itens) + "]}", auth);
        assertEquals(200, r.status(), r.corpo());
    }

    @Test
    void gravaRepeteSemDuplicarEPermiteSegundaAnaliseDaMesmaRevisao() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String[] pronto = pedidoLido(api, auth);
            String pedido = pronto[0];
            long numero = Long.parseLong(pedido);

            Resposta primeira = api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
            assertEquals(201, primeira.status(), primeira.corpo());
            assertEquals("REQUER_CONFERENCIA", primeira.json().get("resultadoGeral").asText());
            assertEquals(6, primeira.json().get("resultados").size());
            assertEquals("TRANSCRICAO_NAO_CONFERIDA",
                    primeira.json().get("pendencias").get(0).get("motivo").asText());
            JsonNode evidencia = primeira.json().get("pendencias").get(0).get("evidencias").get(0);
            assertTrue(evidencia.has("origem"), "origem lida da leitura da mesma revisao");
            assertTrue(contar("SELECT COUNT(*) FROM RC_PENDENCIA_EVIDENCIA WHERE ANALISE_ID IN "
                    + "(SELECT ID FROM RC_ANALISE WHERE SOLICITACAO_ID = ?)", numero) > 0);

            Resposta repetida = api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals(primeira.json().get("id").asText(), repetida.json().get("id").asText());

            confirmarTudo(api, auth, pedido, pronto[1]);
            assertFalse(api.get("/solicitacoes/" + pedido + "/analises", auth).json().has("vigenteId"));
            Resposta segunda = api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
            assertEquals(201, segunda.status(), segunda.corpo());
            assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", segunda.json().get("resultadoGeral").asText());

            JsonNode historico = api.get("/solicitacoes/" + pedido + "/analises", auth).json();
            assertEquals(2, historico.get("analises").size());
            assertEquals(segunda.json().get("id").asText(), historico.get("vigenteId").asText());
            assertEquals(historico.get("analises").get(0).get("revisao").asInt(),
                    historico.get("analises").get(1).get("revisao").asInt());
            assertNotEquals(historico.get("analises").get(0).get("conferenciaConsiderada").asText(),
                    historico.get("analises").get(1).get("conferenciaConsiderada").asText());
            assertEquals(segunda.json().get("conferenciaConsiderada").asText(),
                    historico.get("analises").get(0).get("conferenciaConsiderada").asText(),
                    "CONFERENCIA_EM (TIMESTAMP em UTC) volta igual");
            assertEquals("SEM_PENDENCIAS_IDENTIFICADAS",
                    api.get("/solicitacoes/" + pedido, auth).json().get("estado").asText());
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_ANALISE WHERE SOLICITACAO_ID = ? AND VIGENTE = 1",
                    numero));
        }
    }

    @Test
    void pedidosSimultaneosGravamUmaAnaliseSobOBloqueioReal() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String[] pronto = pedidoLido(api, auth);
            confirmarTudo(api, auth, pronto[0], pronto[1]);
            ExecutorService threads = Executors.newFixedThreadPool(4);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resposta>> respostas = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                respostas.add(threads.submit(() -> {
                    largada.await();
                    return api.enviar("POST", "/solicitacoes/" + pronto[0] + "/analises", null, auth);
                }));
            }
            largada.countDown();
            int criadas = 0;
            for (Future<Resposta> futura : respostas) {
                Resposta r = futura.get(60, TimeUnit.SECONDS);
                assertTrue(List.of(200, 201, 409).contains(r.status()), r.status() + " " + r.corpo());
                criadas += r.status() == 201 ? 1 : 0;
            }
            threads.shutdown();
            assertEquals(1, criadas);
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_ANALISE WHERE SOLICITACAO_ID = ?",
                    Long.parseLong(pronto[0])));
        }
    }
}
