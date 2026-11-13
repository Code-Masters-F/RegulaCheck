package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * US4 / SC-004: entradas invalidas em {@code POST} e {@code PUT}. Erro de contrato (400/415) e distinto de erro
 * de regra (422); o campo e identificado; nada e criado ou alterado; nenhuma resposta vaza detalhe interno nem
 * repete o que foi enviado.
 */
class ValidacaoEntradaSolicitacaoTest {

    static final String VALIDO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";
    static final String SENTINELA = "SENTINELA-NAO-PODE-VAZAR";

    /** Corpos com erro de contrato (400): nome do caso -> corpo. */
    private static Map<String, String> contrato400() {
        Map<String, String> casos = new LinkedHashMap<>();
        casos.put("campo ausente", "{}");
        casos.put("nulo", "{\"protocoloId\":null}");
        casos.put("vazio", "{\"protocoloId\":\"\"}");
        casos.put("em branco", "{\"protocoloId\":\"   \"}");
        casos.put("numero", "{\"protocoloId\":123}");
        casos.put("booleano", "{\"protocoloId\":true}");
        casos.put("objeto", "{\"protocoloId\":{\"a\":1}}");
        casos.put("lista", "{\"protocoloId\":[\"" + SENTINELA + "\"]}");
        casos.put("longo demais", "{\"protocoloId\":\"" + "X".repeat(101) + "\"}");
        return casos;
    }

    private static CenarioAuth cenario() {
        CenarioAuth cenario = new CenarioAuth();
        cenario.protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), false);
        return cenario;
    }

    @Test
    void postComErroDeContratoDa400ComOCampoENaoCriaNada() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            for (Map.Entry<String, String> caso : contrato400().entrySet()) {
                Resposta r = api.postJson("/solicitacoes", caso.getValue(), auth);

                assertEquals(400, r.status(), caso.getKey());
                assertErroDeCampo(r, "REQUISICAO_INVALIDA", caso.getKey());
            }
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void putComErroDeContratoDa400ENaoAlteraONada() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            String local = api.postJson("/solicitacoes", VALIDO, auth).cabecalho("Location");
            for (Map.Entry<String, String> caso : contrato400().entrySet()) {
                Resposta r = api.enviar("PUT", local, caso.getValue(), auth);

                assertEquals(400, r.status(), caso.getKey());
                assertErroDeCampo(r, "REQUISICAO_INVALIDA", caso.getKey());
            }
            assertEquals(1, cenario.solicitacoes.revisoesDe(1).size());
            assertEquals(1, cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().revisaoAtual());
        }
    }

    @Test
    void servicoInexistenteEInativoDaoOMesmo422() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta inexistente = api.postJson("/solicitacoes", "{\"protocoloId\":\"NAO-EXISTE-v1\"}", auth);
            Resposta inativo = api.postJson("/solicitacoes",
                    "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.2\"}", auth);

            assertEquals(422, inexistente.status());
            assertErroDeCampo(inexistente, "REGRA_DE_NEGOCIO", "inexistente");
            assertEquals(422, inativo.status());
            assertEquals(inexistente.corpo(), inativo.corpo(), "inexistente e inativo nao podem ser distinguidos");
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void corpoMalformadoVazioOuGrandeEErroDeContratoSemDetalhesDoCorpo() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            String[] corpos = {"{ruim " + SENTINELA, "", "   ", "[]", "\"" + SENTINELA + "\"",
                    "{\"protocoloId\":\"" + "Y".repeat(5000) + "\"}"};
            for (String corpo : corpos) {
                Resposta r = api.postJson("/solicitacoes", corpo, auth);

                assertEquals(400, r.status(), "corpo: " + corpo.substring(0, Math.min(10, corpo.length())));
                assertEquals("REQUISICAO_INVALIDA", r.json().get("codigo").asText());
                assertSemVazamento(r);
            }
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void tipoDeConteudoErradoE415() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta r = api.post("/solicitacoes", VALIDO, "text/plain", auth);
            Resposta semTipo = api.post("/solicitacoes", VALIDO, null, auth);

            assertEquals(415, r.status());
            assertEquals("TIPO_NAO_SUPORTADO", r.json().get("codigo").asText());
            assertEquals(415, semTipo.status());
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    private static void assertErroDeCampo(Resposta r, String codigo, String caso) {
        JsonNode corpo = r.json();
        assertEquals(codigo, corpo.get("codigo").asText(), caso);
        assertEquals("protocoloId", corpo.get("detalhes").get(0).get("campo").asText(), caso);
        assertFalse(corpo.get("detalhes").get(0).get("mensagem").asText().isBlank(), caso);
        assertSemVazamento(r);
    }

    private static void assertSemVazamento(Resposta r) {
        for (String interno : new String[] {"ORA-", "SELECT ", "Exception", "br.com.regulacheck", "\tat ", "jdbc:",
                SENTINELA, "XXXXXXXXXX", "YYYYYYYYYY"}) {
            assertFalse(r.corpo().contains(interno), "resposta expoe: " + interno);
        }
        assertTrue(r.corpo().contains("\"codigo\""));
    }
}
