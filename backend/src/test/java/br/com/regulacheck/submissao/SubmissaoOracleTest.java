package br.com.regulacheck.submissao;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Verificacao da RC-17 contra o <strong>Oracle real</strong> (opt-in): conclusao gravada em {@code RC_SUBMISSAO} com a
 * analise vigente e o pedido em {@code SUBMETIDA_SIMULACAO} na mesma transacao, repeticao e consulta devolvendo o
 * mesmo comprovante, pedidos simultaneos sob o bloqueio de linha real e leitura da submissao da carga sintetica
 * ({@code SIM-2026-0001}).
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como <strong>NAO VERIFICADO</strong>.
 * Pre-requisito: scripts 01 a 10 (nenhum script novo na RC-17). Usa a conta ficticia da carga, as amostras sinteticas
 * oficiais e uma pasta temporaria propria; ao final remove somente o que criou (nunca a submissao da carga). A leitura
 * e simulada e a submissao e simulada.
 */
class SubmissaoOracleTest {

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
        raiz = Files.createTempDirectory("regulacheck-oracle-submissao-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco != null) {
            try (Connection c = banco.conexao()) {
                for (long id : PEDIDOS) {
                    executar(c, "DELETE FROM RC_SUBMISSAO WHERE SOLICITACAO_ID = ?", id);
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

    @Test
    void concluiRepeteConsultaEDeixaOPedidoSoParaConsulta() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String pedido = pedidoPronto(api, auth);
            long numero = Long.parseLong(pedido);
            String rota = "/solicitacoes/" + pedido + "/submissoes-simuladas";

            Resposta primeira = api.enviar("POST", rota, null, auth);
            Resposta repetida = api.enviar("POST", rota, null, auth);
            Resposta consultada = api.get(rota, auth);

            assertEquals(201, primeira.status(), primeira.corpo());
            JsonNode comprovante = primeira.json();
            assertTrue(comprovante.get("referencia").asText().matches("^SIM-\\d{4}-0*" + numero + "$"),
                    comprovante.get("referencia").asText());
            assertEquals("SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", comprovante.get("destaque").asText());
            assertEquals("v0.1", comprovante.get("versaoAnalisada").get("protocolo").get("versao").asText());
            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals(comprovante, repetida.json(), "SUBMETIDA_EM volta igual do TIMESTAMP WITH TIME ZONE");
            assertEquals(200, consultada.status(), consultada.corpo());
            assertEquals(comprovante, consultada.json());
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_SUBMISSAO WHERE SOLICITACAO_ID = ? AND SIMULADA = 1",
                    numero));
            assertEquals(1, contar("""
                    SELECT COUNT(*) FROM RC_SUBMISSAO s JOIN RC_ANALISE a ON a.ID = s.ANALISE_ID
                     WHERE s.SOLICITACAO_ID = ? AND a.VIGENTE = 1 AND a.RESULTADO_GERAL = 'SEM_PENDENCIAS_IDENTIFICADAS'""",
                    numero), "a submissao aponta a analise vigente sem pendencias");
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_SOLICITACAO WHERE ID = ? AND ESTADO = 'SUBMETIDA_SIMULACAO'",
                    numero));
            Resposta reenvio = api.enviar("POST", "/solicitacoes/" + pedido + "/reenvio", null, auth);
            assertEquals(409, reenvio.status(), reenvio.corpo());
        }
    }

    @Test
    void conclusoesSimultaneasGravamUmaSoSobOBloqueioReal() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String pedido = pedidoPronto(api, auth);
            ExecutorService threads = Executors.newFixedThreadPool(4);
            List<Future<Resposta>> respostas = new ArrayList<>();
            int criadas = 0;
            try {
                CountDownLatch largada = new CountDownLatch(1);
                for (int i = 0; i < 4; i++) {
                    respostas.add(threads.submit(() -> {
                        largada.await();
                        return api.enviar("POST", "/solicitacoes/" + pedido + "/submissoes-simuladas", null, auth);
                    }));
                }
                largada.countDown();
                for (Future<Resposta> futura : respostas) {
                    Resposta r = futura.get(60, TimeUnit.SECONDS);
                    assertTrue(List.of(200, 201).contains(r.status()), r.status() + " " + r.corpo());
                    criadas += r.status() == 201 ? 1 : 0;
                }
            } finally {
                respostas.forEach(futura -> futura.cancel(true));
                threads.shutdownNow();
                assertTrue(threads.awaitTermination(60, TimeUnit.SECONDS), "threads do teste nao terminaram");
            }
            assertEquals(1, criadas);
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_SUBMISSAO WHERE SOLICITACAO_ID = ?",
                    Long.parseLong(pedido)));
        }
    }

    @Test
    void submissaoDaCargaSinteticaEConsultavelESemEfeitoAoRepetir() throws Exception {
        long pedidoDaCarga = idDoPedidoDaCarga();
        assumeTrue(pedidoDaCarga > 0, "carga 02_ sem a submissao SIM-2026-0001 da paciente A: caso ignorado");
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String rota = "/solicitacoes/" + pedidoDaCarga + "/submissoes-simuladas";

            Resposta consultada = api.get(rota, auth);
            Resposta repetida = api.enviar("POST", rota, null, auth);

            assertEquals(200, consultada.status(), consultada.corpo());
            assertEquals("SIM-2026-0001", consultada.json().get("referencia").asText());
            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals(consultada.json(), repetida.json());
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_SUBMISSAO WHERE SOLICITACAO_ID = ?", pedidoDaCarga));
        }
    }

    /** Pedido da paciente A com as amostras oficiais lidas, conferidas e analisadas sem pendencias. */
    private static String pedidoPronto(ServidorDeTeste api, Map<String, String> auth) throws Exception {
        String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
        Resposta criado = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + padrao + "\"}", auth);
        assertEquals(201, criado.status(), criado.corpo());
        long numero = criado.json().get("id").asLong();
        PEDIDOS.add(numero);
        String pedido = Long.toString(numero);
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
        confirmarTudo(api, auth, pedido, lida.json().get("id").asText());
        Resposta analise = api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
        assertEquals(201, analise.status(), analise.corpo());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", analise.json().get("resultadoGeral").asText());
        return pedido;
    }

    private static Map<String, String> entrar(ServidorDeTeste api) throws Exception {
        String token = api.entrar(EMAIL_A, SENHA_DEMONSTRACAO);
        HASHES.add(GERADOR.hash(token));
        return ServidorDeTeste.bearer(token);
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

    /** @return id do pedido da carga com a submissao {@code SIM-2026-0001} da paciente A, ou {@code 0} se nao ha */
    private static long idDoPedidoDaCarga() throws Exception {
        try (Connection c = banco.conexao(); PreparedStatement p = c.prepareStatement("""
                SELECT s.SOLICITACAO_ID FROM RC_SUBMISSAO s
                  JOIN RC_SOLICITACAO sol ON sol.ID = s.SOLICITACAO_ID
                  JOIN RC_USUARIO u ON u.ID = sol.PACIENTE_ID
                 WHERE s.REFERENCIA = 'SIM-2026-0001' AND u.EMAIL = ?""")) {
            p.setString(1, EMAIL_A);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getLong(1) : 0;
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
}
