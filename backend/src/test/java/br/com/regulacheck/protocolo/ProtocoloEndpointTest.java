package br.com.regulacheck.protocolo;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@code GET /protocolos} pela API real (Jetty) com repositorios em memoria. */
class ProtocoloEndpointTest {

    @Test
    void listaOCatalogoComOsCamposDoContratoEOAvisoFicticio() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), false);
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(EMAIL_A, SENHA);
            Resposta r = api.get("/protocolos", bearer(token));

            assertEquals(200, r.status());
            JsonNode itens = r.json().get("itens");
            assertEquals(1, itens.size());
            JsonNode item = itens.get(0);
            assertEquals(List.of("protocoloId", "identificador", "versao", "localidade", "especialidade",
                    "avisoFicticio", "simulacao"), ServidorDeTeste.campos(item));
            assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", item.get("protocoloId").asText());
            assertEquals("Município Exemplo", item.get("localidade").asText());
            assertTrue(item.get("avisoFicticio").asText().contains("fictício"));
            assertTrue(item.get("simulacao").asBoolean());
            assertTrue(!r.corpo().contains("\"id\""), "o identificador interno nao pode sair pela API");
        }
    }

    @Test
    void catalogoVazioEUmSucesso() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.protocolos.limpar();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Resposta r = api.get("/protocolos", bearer(api.entrar(EMAIL_A, SENHA)));
            assertEquals(200, r.status());
            assertEquals(0, r.json().get("itens").size());
        }
    }

    @Test
    void exigeSessaoEPerfilDePaciente() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            assertEquals(401, api.get("/protocolos", Map.of()).status());
            assertEquals(403, api.get("/protocolos", bearer(api.entrar(EMAIL_ADMIN, SENHA))).status());
        }
    }

    @Test
    void soAceitaGet() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(EMAIL_A, SENHA);
            Resposta r = api.enviar("POST", "/protocolos", "{}", bearer(token));
            assertEquals(405, r.status());
            assertEquals("METODO_NAO_PERMITIDO", r.json().get("codigo").asText());
        }
    }
}
