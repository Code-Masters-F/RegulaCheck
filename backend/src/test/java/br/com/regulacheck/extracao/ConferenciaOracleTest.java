package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import java.sql.SQLException;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-11 contra o <strong>Oracle real</strong> da faculdade (opt-in): decisoes gravadas, substituidas e
 * repetidas, uma decisao vigente sob duas threads no mesmo campo, restricoes {@code CHECK} e chave estrangeira
 * composta da tabela, leitura desatualizada recusada e exclusao de rascunho sem residuo.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como <strong>NAO VERIFICADO</strong>.
 * Pre-requisito: scripts 01, 02, 05, 06, 07, 08 e <strong>09_conferencia.sql</strong> executados na conta (o
 * {@code 09_} so roda com autorizacao especifica do responsavel). Usa as contas ficticias da carga, as amostras
 * sinteticas e uma pasta temporaria propria; ao final remove somente o que criou.
 *
 * <p>A leitura e <strong>simulada</strong>: este teste comprova a persistencia, as restricoes e o bloqueio de
 * linha, nao leitura real de arquivo.
 */
class ConferenciaOracleTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";
    private static final String EMAIL_A = "paciente.a@exemplo.invalid";
    private static final GeradorToken GERADOR = new GeradorToken();
    private static final String DECISOES_DO_PEDIDO = """
            SELECT COUNT(*) FROM RC_CONFERENCIA_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN (
                SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID
                 WHERE e.SOLICITACAO_ID = ?)""";

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
        raiz = Files.createTempDirectory("regulacheck-oracle-conf-");
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

    private static long enviarAmostra(ServidorDeTeste api, Map<String, String> auth, long pedido, Amostra amostra)
            throws Exception {
        String mime = amostra.nome().endsWith(".pdf") ? "application/pdf" : "image/png";
        MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", CenarioExtracao.tipoInformado(amostra))
                .arquivo("arquivo", amostra.nome(), mime, amostra.bytes());
        Resposta r = api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(), corpo.contentType(),
                auth);
        assertEquals(201, r.status(), r.corpo());
        return r.json().get("id").asLong();
    }

    private static String caminho(long pedido, String extracao) {
        return "/solicitacoes/" + pedido + "/extracoes/" + extracao + "/conferencia";
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

    /** Leitura pronta: pedido com o encaminhamento (5 campos identificados) lido. */
    private record Pronto(long pedido, long documento, String extracao) {
    }

    private static Pronto prepararLeitura(ServidorDeTeste api, Map<String, String> auth) throws Exception {
        long pedido = criarRascunho(api, auth);
        long documento = enviarAmostra(api, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        Resposta lida = api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes", null, auth);
        assertEquals(201, lida.status(), lida.corpo());
        return new Pronto(pedido, documento, lida.json().get("id").asText());
    }

    private static String confirmar(long documento, String codigo) {
        return CenarioConferencia.lote(CenarioConferencia.confirmar(Long.toString(documento), codigo));
    }

    private static int codigoOra(SQLException falha) {
        return falha.getErrorCode();
    }

    @Test
    void gravaSubstituiRepeteERecusaLeituraDesatualizadaNoBancoReal() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            Pronto p = prepararLeitura(api, auth);

            Resposta primeira = api.enviar("PUT", caminho(p.pedido(), p.extracao()), confirmar(p.documento(), "NOME"),
                    auth);
            assertEquals(200, primeira.status(), primeira.corpo());
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()));
            String instante = primeira.json().get("atualizadaEm").asText();

            // repetir a mesma decisao: no-op (mesmo instante, uma linha)
            Resposta repetida = api.enviar("PUT", caminho(p.pedido(), p.extracao()), confirmar(p.documento(), "NOME"),
                    auth);
            assertEquals(200, repetida.status());
            assertEquals(instante, repetida.json().get("atualizadaEm").asText());
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()));

            // trocar a decisao: continua uma so
            Resposta trocada = api.enviar("PUT", caminho(p.pedido(), p.extracao()), CenarioConferencia.lote(
                    CenarioConferencia.divergir(Long.toString(p.documento()), "NOME",
                            "VALOR_DIFERENTE_DO_DOCUMENTO")), auth);
            assertEquals(200, trocada.status(), trocada.corpo());
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()));
            assertEquals("REQUER_CONFERENCIA", trocada.json().get("situacao").asText());

            // lote invalido nao grava nada (campo ilegivel nao existe na amostra oficial; usa nao localizado)
            Resposta invalida = api.enviar("PUT", caminho(p.pedido(), p.extracao()),
                    CenarioConferencia.lote(CenarioConferencia.confirmar(Long.toString(p.documento()), "DATA_EMISSAO"),
                            CenarioConferencia.confirmar(Long.toString(p.documento()), "AUTORIZACAO_ASSINADA")),
                    auth);
            assertEquals(422, invalida.status(), invalida.corpo());
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()));

            // estado e revisao do pedido nao mudaram
            JsonNode pedido = api.get("/solicitacoes/" + p.pedido(), auth).json();
            assertEquals("RASCUNHO", pedido.get("estado").asText());

            // substituir o documento desatualiza a leitura: GET mostra DESATUALIZADA e PUT recusa
            Amostra outra = AmostrasSinteticas.SEM_MOTIVO;
            MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", "ENCAMINHAMENTO")
                    .arquivo("arquivo", outra.nome(), "application/pdf", outra.bytes());
            assertEquals(201, api.enviarBinario("PUT", "/solicitacoes/" + p.pedido() + "/documentos/"
                    + p.documento(), corpo.corpo(), corpo.contentType(), auth).status());
            assertEquals("DESATUALIZADA", api.get(caminho(p.pedido(), p.extracao()), auth).json().get("situacao")
                    .asText());
            Resposta velha = api.enviar("PUT", caminho(p.pedido(), p.extracao()),
                    confirmar(p.documento(), "DATA_EMISSAO"), auth);
            assertEquals(409, velha.status(), velha.corpo());
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()));

            // excluir o rascunho apaga decisoes, leituras, documentos e o pedido, sem residuo
            Map<String, String> confirmacao = new HashMap<>(auth);
            confirmacao.put("X-Confirmar-Exclusao", "EXCLUIR");
            assertEquals(204, api.enviar("DELETE", "/solicitacoes/" + p.pedido(), null, confirmacao).status());
            assertEquals(0, contar(DECISOES_DO_PEDIDO, p.pedido()));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?", p.pedido()));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?", p.pedido()));
            assertEquals(0, contar("SELECT COUNT(*) FROM RC_SOLICITACAO WHERE ID = ?", p.pedido()));
        }
    }

    @Test
    void duasThreadsNoMesmoCampoDeixamUmaDecisaoVigente() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            Pronto p = prepararLeitura(api, auth);
            String confirma = confirmar(p.documento(), "NOME");
            String diverge = CenarioConferencia.lote(CenarioConferencia.divergir(Long.toString(p.documento()), "NOME",
                    "VALOR_DIFERENTE_DO_DOCUMENTO"));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch largada = new CountDownLatch(1);
            try {
                List<Future<Resposta>> futuros = new ArrayList<>();
                for (String corpo : new String[] {confirma, diverge}) {
                    futuros.add(pool.submit(() -> {
                        largada.await();
                        return api.enviar("PUT", caminho(p.pedido(), p.extracao()), corpo, auth);
                    }));
                }
                largada.countDown();
                for (Future<Resposta> futuro : futuros) {
                    Resposta r = futuro.get();
                    assertEquals(200, r.status(), r.corpo());
                }
            } finally {
                pool.shutdownNow();
            }
            assertEquals(1, contar(DECISOES_DO_PEDIDO, p.pedido()), "uma decisao vigente por campo");
        }
    }

    @Test
    void restricoesEChaveEstrangeiraDaTabelaRecusamDadoInvalido() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Map<String, String> auth = entrar(api);
            Pronto p = prepararLeitura(api, auth);
            long leituraDocumento;
            try (Connection c = banco.conexao(); PreparedStatement q = c.prepareStatement(
                    "SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID "
                            + "WHERE e.SOLICITACAO_ID = ?")) {
                q.setLong(1, p.pedido());
                try (ResultSet r = q.executeQuery()) {
                    assertTrue(r.next());
                    leituraDocumento = r.getLong(1);
                }
            }
            // motivo em CONFIRMADA, DIVERGENTE sem motivo, motivo fora da lista: ORA-02290
            assertEquals(2290, violar(leituraDocumento, "NOME", "CONFIRMADA", "VALOR_DIFERENTE_DO_DOCUMENTO"));
            assertEquals(2290, violar(leituraDocumento, "NOME", "DIVERGENTE", null));
            assertEquals(2290, violar(leituraDocumento, "NOME", "DIVERGENTE", "TEXTO_LIVRE"));
            assertEquals(2290, violar(leituraDocumento, "NOME", "CORRIGIDA", null));
            // campo que nao existe na leitura (documento lido sem esse codigo): FK composta, ORA-02291
            assertEquals(2291, violar(leituraDocumento + 987654321L, "NOME", "CONFIRMADA", null));
            // duas decisoes para o mesmo campo: ORA-00001
            try (Connection c = banco.conexao()) {
                c.setAutoCommit(false);
                try {
                    inserir(c, leituraDocumento, "NOME", "CONFIRMADA", null);
                    try {
                        inserir(c, leituraDocumento, "NOME", "CONFIRMADA", null);
                        throw new AssertionError("a segunda decisao do mesmo campo foi aceita");
                    } catch (SQLException esperado) {
                        assertEquals(1, codigoOra(esperado));
                    }
                } finally {
                    c.rollback();
                }
            }
            assertEquals(0, contar(DECISOES_DO_PEDIDO, p.pedido()), "as violacoes nao deixaram linha");
        }
    }

    /** Tenta inserir e devolve o codigo ORA; sempre desfaz. */
    private static int violar(long leituraDocumento, String codigo, String decisao, String motivo) throws Exception {
        try (Connection c = banco.conexao()) {
            c.setAutoCommit(false);
            try {
                inserir(c, leituraDocumento, codigo, decisao, motivo);
                throw new AssertionError("o banco aceitou " + decisao + "/" + motivo);
            } catch (SQLException esperado) {
                return codigoOra(esperado);
            } finally {
                c.rollback();
            }
        }
    }

    private static void inserir(Connection c, long leituraDocumento, String codigo, String decisao, String motivo)
            throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO RC_CONFERENCIA_CAMPO "
                + "(EXTRACAO_DOCUMENTO_ID, CODIGO, DECISAO, MOTIVO) VALUES (?, ?, ?, ?)")) {
            p.setLong(1, leituraDocumento);
            p.setString(2, codigo);
            p.setString(3, decisao);
            p.setString(4, motivo);
            p.executeUpdate();
        }
    }
}
