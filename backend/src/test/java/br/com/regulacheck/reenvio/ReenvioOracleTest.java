package br.com.regulacheck.reenvio;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-16 contra o <strong>Oracle real</strong> (opt-in): a inclusao do documento que faltava depois da
 * analise passa pelo caminho JDBC novo ({@code DocumentoRepositoryJdbc.registrar} aceita o pedido ja analisado e tira
 * a vigencia da analise na mesma transacao), e a verificacao da versao corrigida grava a nova analise vigente com a
 * comparacao.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como <strong>NAO VERIFICADO</strong>.
 * Pre-requisito: scripts 01 a 10 (nenhum script novo na RC-16). Usa a conta ficticia da carga, as amostras sinteticas
 * oficiais e uma pasta temporaria propria; ao final remove somente o que criou. Leitura e resumo sao simulados.
 */
class ReenvioOracleTest {

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
        raiz = Files.createTempDirectory("regulacheck-oracle-reenvio-");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos(), null,
                raiz.toString(), null);
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

    @Test
    void inclusaoAposAnaliseNoJdbcTiraAVigenciaEAVerificacaoResolveAPendencia() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            String padrao = api.get("/protocolos", auth).json().get("itens").get(0).get("protocoloId").asText();
            Resposta criado = api.postJson("/solicitacoes", "{\"protocoloId\":\"" + padrao + "\"}", auth);
            assertEquals(201, criado.status(), criado.corpo());
            long numero = criado.json().get("id").asLong();
            PEDIDOS.add(numero);
            String pedido = Long.toString(numero);
            enviar(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            Resposta lida = api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes", null, auth);
            assertEquals(201, lida.status(), lida.corpo());
            confirmarTudo(api, auth, pedido, lida.json().get("id").asText());
            Resposta primeira = api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
            assertEquals(201, primeira.status(), primeira.corpo());
            assertEquals("COM_PENDENCIAS", primeira.json().get("resultadoGeral").asText());

            enviar(api, auth, pedido, AmostrasSinteticas.IDENTIFICACAO);

            assertEquals(0, contar("SELECT COUNT(*) FROM RC_ANALISE WHERE SOLICITACAO_ID = ? AND VIGENTE = 1",
                    numero), "a inclusao tirou a vigencia na mesma transacao");
            assertEquals("COM_PENDENCIAS", api.get("/solicitacoes/" + pedido, auth).json().get("estado").asText());
            assertEquals(1, contar("""
                    SELECT COUNT(*) FROM RC_REVISAO r
                     WHERE r.SOLICITACAO_ID = ? AND r.MOTIVO = 'DOCUMENTO_ENVIADO'
                       AND r.NUMERO = (SELECT MAX(NUMERO) FROM RC_REVISAO WHERE SOLICITACAO_ID = r.SOLICITACAO_ID)""",
                    numero), "a revisao mais recente e a da inclusao");

            Resposta parada = api.enviar("POST", "/solicitacoes/" + pedido + "/reenvio", null, auth);
            assertEquals(200, parada.status(), parada.corpo());
            assertEquals("CONFERENCIA", parada.json().get("etapa").asText());
            confirmarTudo(api, auth, pedido, parada.json().get("extracaoId").asText());
            Resposta concluida = api.enviar("POST", "/solicitacoes/" + pedido + "/reenvio", null, auth);

            assertEquals(201, concluida.status(), concluida.corpo());
            JsonNode nova = concluida.json().get("analise");
            assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", nova.get("resultadoGeral").asText());
            assertEquals(primeira.json().get("id").asText(), nova.get("comparacao").get("analiseAnteriorId").asText());
            boolean r03Resolvida = false;
            for (JsonNode item : nova.get("comparacao").get("itens")) {
                r03Resolvida |= "RC-R03".equals(item.get("codigoRegra").asText())
                        && "RESOLVIDA".equals(item.get("situacao").asText());
            }
            assertTrue(r03Resolvida, nova.get("comparacao").toString());
            assertEquals(2, contar("SELECT COUNT(*) FROM RC_ANALISE WHERE SOLICITACAO_ID = ?", numero));
            assertEquals(1, contar("SELECT COUNT(*) FROM RC_ANALISE WHERE SOLICITACAO_ID = ? AND VIGENTE = 1",
                    numero));
        }
    }

    private static Map<String, String> entrar(ServidorDeTeste api) throws Exception {
        String token = api.entrar(EMAIL_A, SENHA_DEMONSTRACAO);
        HASHES.add(GERADOR.hash(token));
        return ServidorDeTeste.bearer(token);
    }

    private static Resposta enviar(ServidorDeTeste api, Map<String, String> auth, String pedido, Amostra amostra)
            throws Exception {
        String mime = amostra.nome().endsWith(".pdf") ? "application/pdf" : "image/png";
        MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", CenarioExtracao.tipoInformado(amostra))
                .arquivo("arquivo", amostra.nome(), mime, amostra.bytes());
        Resposta enviado = api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(),
                corpo.contentType(), auth);
        assertEquals(201, enviado.status(), enviado.corpo());
        return enviado;
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
