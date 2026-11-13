package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@code GET /solicitacoes} e {@code GET /solicitacoes/{id}} pela API real com repositorios em memoria. */
class ListarReabrirEndpointTest {

    static final String CORPO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";

    @Test
    void cadaPacienteVeSoOsSeusPedidosDoMaisRecenteAoMaisAntigo() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            Map<String, String> authB = bearer(api.entrar(EMAIL_B, SENHA));
            String a1 = api.postJson("/solicitacoes", CORPO, authA).json().get("id").asText();
            cenario.relogio.avancar(Duration.ofSeconds(1));
            String b1 = api.postJson("/solicitacoes", CORPO, authB).json().get("id").asText();
            cenario.relogio.avancar(Duration.ofSeconds(1));
            String a2 = api.postJson("/solicitacoes", CORPO, authA).json().get("id").asText();

            JsonNode listaA = api.get("/solicitacoes", authA).json();
            JsonNode listaB = api.get("/solicitacoes", authB).json();

            assertEquals(50, listaA.get("limite").asInt());
            assertEquals(List.of(a2, a1), ids(listaA));
            assertEquals(List.of(b1), ids(listaB));
            assertEquals(List.of("id", "protocoloId", "servico", "estado", "revisao", "criadaEm", "atualizadaEm",
                    "simulacao"), ServidorDeTeste.campos(listaA.get("itens").get(0)));
        }
    }

    @Test
    void semPedidosAListaEVaziaComSucesso() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Resposta r = api.get("/solicitacoes", bearer(api.entrar(EMAIL_A, SENHA)));
            assertEquals(200, r.status());
            assertEquals(0, r.json().get("itens").size());
        }
    }

    @Test
    void o404EIdenticoParaAlheioInexistenteEIdInvalido() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            Map<String, String> authB = bearer(api.entrar(EMAIL_B, SENHA));
            String deA = api.postJson("/solicitacoes", CORPO, authA).cabecalho("Location");

            Resposta alheio = api.get(deA, authB);
            assertEquals(404, alheio.status());
            assertEquals("NAO_ENCONTRADO", alheio.json().get("codigo").asText());
            List<String> variantes = new ArrayList<>(List.of("/solicitacoes/999999", "/solicitacoes/abc",
                    "/solicitacoes/0", "/solicitacoes/-1", "/solicitacoes/01", "/solicitacoes/99999999999999999999",
                    "/solicitacoes/1.5", deA + "/qualquer", deA + "/documentos"));
            for (String caminho : variantes) {
                Resposta r = api.get(caminho, authB);
                assertEquals(404, r.status(), caminho);
                assertEquals(alheio.corpo(), r.corpo(), "corpo diferente para " + caminho);
            }
        }
    }

    @Test
    void rascunhosContinuamIguaisDepoisDeUmNovoLogin() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> primeira = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CORPO, primeira);
            assertEquals(204, api.postJson("/auth/logout", "", primeira).status());
            assertEquals(401, api.get("/solicitacoes", primeira).status());

            Map<String, String> segunda = bearer(api.entrar(EMAIL_A, SENHA));

            assertEquals(criada.json(), api.get(criada.cabecalho("Location"), segunda).json());
            assertEquals(1, api.get("/solicitacoes", segunda).json().get("itens").size());
        }
    }

    @Test
    void metodosNaoPrevistosEm405() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            String local = api.postJson("/solicitacoes", CORPO, auth).cabecalho("Location");

            assertEquals(405, api.enviar("PUT", "/solicitacoes", CORPO, auth).status());
            assertEquals(405, api.enviar("DELETE", "/solicitacoes", null, auth).status());
            assertEquals(405, api.enviar("POST", local, CORPO, auth).status());
            assertTrue(api.enviar("POST", local, CORPO, auth).cabecalho("Allow").contains("GET"));
        }
    }

    private static List<String> ids(JsonNode lista) {
        List<String> ids = new ArrayList<>();
        lista.get("itens").forEach(n -> ids.add(n.get("id").asText()));
        return ids;
    }
}
