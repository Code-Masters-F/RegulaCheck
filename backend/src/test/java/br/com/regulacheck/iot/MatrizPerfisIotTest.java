package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matriz identidade x rota da estacao de captura (FR-002, FR-008, FR-018, SC-005): so a dona chega as rotas da
 * paciente; so a chave do simulador chega a rota de leituras; pedido alheio e inexistente tem a mesma resposta.
 */
class MatrizPerfisIotTest {

    @Test
    void rotasDaPacienteRecusamAnonimoAdministradorESimulador() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            List<String[]> rotas = List.of(new String[] {"POST", "/solicitacoes/" + pedido + "/sessoes-captura"},
                    new String[] {"GET", "/solicitacoes/" + pedido + "/leituras"});
            for (String[] rota : rotas) {
                assertCodigo(c.api.enviar(rota[0], rota[1], null, Map.of()), 401, "NAO_AUTENTICADO");
                assertCodigo(c.api.enviar(rota[0], rota[1], null, c.admin), 403, "ACESSO_NEGADO");
                assertCodigo(c.api.enviar(rota[0], rota[1], null, c.simulador), 403, "ACESSO_NEGADO");
            }
            assertEquals(0, c.cenario.capturas.sessoes().size());
        }
    }

    @Test
    void telemetriaDeOutraPacienteEInexistenteTemAMesmaResposta() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, "LUMINOSIDADE", "450");
            Resposta alheio = c.telemetria(c.authB, pedido);
            Resposta inexistente = c.telemetria(c.authB, "999999");
            assertCodigo(alheio, 404, "NAO_ENCONTRADO");
            assertCodigo(inexistente, 404, "NAO_ENCONTRADO");
            assertEquals(alheio.corpo(), inexistente.corpo());
        }
    }

    @Test
    void rotaDeLeiturasSoAceitaOSimulador() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String s = c.sessaoAberta(c.criarPedido(c.authA));
            String corpo = CenarioIot.leitura(s, "LUMINOSIDADE", "450", "LUX", c.agora());
            String caminho = "/sessoes-captura/" + s + "/leituras";
            assertCodigo(c.api.postJson(caminho, corpo, Map.of()), 401, "NAO_AUTENTICADO");
            assertCodigo(c.api.postJson(caminho, corpo, c.authA), 403, "ACESSO_NEGADO");
            assertCodigo(c.api.postJson(caminho, corpo, c.authB), 403, "ACESSO_NEGADO");
            assertCodigo(c.api.postJson(caminho, corpo, c.admin), 403, "ACESSO_NEGADO");
            assertEquals(201, c.api.postJson(caminho, corpo, c.simulador).status());
            assertEquals(1, c.cenario.capturas.leituras().size());
        }
    }

    private static void assertCodigo(Resposta r, int status, String codigo) {
        assertEquals(status, r.status(), r.corpo());
        assertEquals(codigo, r.json().get("codigo").asText(), r.corpo());
    }
}
