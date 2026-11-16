package br.com.regulacheck.iot;

import static br.com.regulacheck.iot.CenarioIot.leitura;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * RC-22, US3 e US4 (FR-012 a FR-019): a paciente recebe sempre os dois sensores com situacao e orientacao; a
 * recuperacao muda o aviso; leitura antiga fica desatualizada; sem sessao ou sem leitura a resposta e de sucesso.
 */
class ConsultarTelemetriaEndpointTest {

    @Test
    void dentroDaFaixaNosDoisSensores() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, "LUMINOSIDADE", "450");
            c.enviar(s, "DISTANCIA", "30");

            Resposta r = c.telemetria(c.authA, pedido);
            assertEquals(200, r.status(), r.corpo());
            JsonNode corpo = r.json();
            assertEquals(pedido, corpo.get("solicitacaoId").asText());
            assertEquals(s, corpo.get("sessao").get("id").asText());
            assertTrue(corpo.get("sessao").get("aberta").asBoolean());
            assertTrue(corpo.get("simulada").asBoolean());
            assertEquals(60, corpo.get("prazoAtualidadeSegundos").asLong());
            assertTrue(corpo.get("aviso").asText().contains("não comprovam a legibilidade"));
            JsonNode luz = sensor(corpo, 0);
            JsonNode distancia = sensor(corpo, 1);
            assertEquals("LUMINOSIDADE", luz.get("tipo").asText());
            assertEquals("LUX", luz.get("unidade").asText());
            assertEquals(300, luz.get("faixa").get("minimo").asInt());
            assertEquals(1000, luz.get("faixa").get("maximo").asInt());
            assertEquals("DISTANCIA", distancia.get("tipo").asText());
            assertEquals(20, distancia.get("faixa").get("minimo").asInt());
            assertEquals(40, distancia.get("faixa").get("maximo").asInt());
            for (JsonNode sensor : new JsonNode[] {luz, distancia}) {
                assertEquals("DENTRO_DA_FAIXA", sensor.get("situacao").asText());
                assertEquals("CONDICAO_ADEQUADA", sensor.get("orientacao").get("codigo").asText());
                assertFalse(sensor.get("orientacao").get("mensagem").asText().isBlank());
            }
        }
    }

    @Test
    void cadaSensorTemSuaPropriaSituacao() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, "LUMINOSIDADE", "120");
            c.enviar(s, "DISTANCIA", "30");
            JsonNode corpo = c.telemetria(c.authA, pedido).json();
            assertEquals("ABAIXO_DA_FAIXA", sensor(corpo, 0).get("situacao").asText());
            assertEquals("MELHORAR_ILUMINACAO", sensor(corpo, 0).get("orientacao").get("codigo").asText());
            assertEquals("DENTRO_DA_FAIXA", sensor(corpo, 1).get("situacao").asText());
        }
    }

    @Test
    void recuperacaoMudaOAvisoEPreservaAsLeiturasAntigas() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, "LUMINOSIDADE", "80");
            c.enviar(s, "DISTANCIA", "65");
            JsonNode fora = c.telemetria(c.authA, pedido).json();
            assertEquals("MELHORAR_ILUMINACAO", sensor(fora, 0).get("orientacao").get("codigo").asText());
            assertEquals("ACIMA_DA_FAIXA", sensor(fora, 1).get("situacao").asText());
            assertEquals("APROXIMAR_DOCUMENTO", sensor(fora, 1).get("orientacao").get("codigo").asText());

            c.cenario.relogio.avancar(Duration.ofSeconds(2));
            c.enviar(s, "LUMINOSIDADE", "500");
            c.enviar(s, "DISTANCIA", "28");
            JsonNode recuperado = c.telemetria(c.authA, pedido).json();
            assertEquals("CONDICAO_ADEQUADA", sensor(recuperado, 0).get("orientacao").get("codigo").asText());
            assertEquals("CONDICAO_ADEQUADA", sensor(recuperado, 1).get("orientacao").get("codigo").asText());
            assertEquals(4, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void valeALeituraDeMedicaoMaisRecenteMesmoQueChegueAntes() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, leitura(s, "LUMINOSIDADE", "500", "LUX", c.agora()));
            c.enviar(s, leitura(s, "LUMINOSIDADE", "90", "LUX", c.agora().minusSeconds(10)));
            JsonNode corpo = c.telemetria(c.authA, pedido).json();
            assertEquals("DENTRO_DA_FAIXA", sensor(corpo, 0).get("situacao").asText());
            assertEquals(500, sensor(corpo, 0).get("ultimaLeitura").get("valor").asInt());
        }
    }

    @Test
    void valeASessaoMaisRecenteDoPedido() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            long numero = Long.parseLong(pedido);
            String antiga = Long.toString(c.cenario.capturas.adicionarSessao(numero, c.agora().minusSeconds(30),
                    null).id());
            c.enviar(antiga, "LUMINOSIDADE", "90");
            c.cenario.capturas.encerrar(Long.parseLong(antiga), c.agora().minusSeconds(1));
            String nova = c.sessaoAberta(pedido);
            c.enviar(nova, "LUMINOSIDADE", "600");
            JsonNode corpo = c.telemetria(c.authA, pedido).json();
            assertEquals(nova, corpo.get("sessao").get("id").asText());
            assertEquals("DENTRO_DA_FAIXA", sensor(corpo, 0).get("situacao").asText());
            assertEquals("SEM_LEITURA", sensor(corpo, 1).get("situacao").asText());
        }
    }

    @Test
    void leituraAntigaFicaDesatualizada() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            String s = c.sessaoAberta(pedido);
            c.enviar(s, "LUMINOSIDADE", "450");
            c.enviar(s, "DISTANCIA", "90");
            c.cenario.relogio.avancar(Duration.ofSeconds(61));
            JsonNode corpo = c.telemetria(c.authA, pedido).json();
            for (int i = 0; i < 2; i++) {
                JsonNode sensor = sensor(corpo, i);
                assertEquals("DESATUALIZADA", sensor.get("situacao").asText());
                assertEquals("VERIFICAR_ESTACAO", sensor.get("orientacao").get("codigo").asText());
                assertTrue(sensor.has("ultimaLeitura"), "mostra o instante da medicao antiga");
            }
        }
    }

    @Test
    void pedidoSemSessaoOuSessaoSemLeituraNaoEErro() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            Resposta semSessao = c.telemetria(c.authA, pedido);
            assertEquals(200, semSessao.status());
            assertNull(semSessao.json().get("sessao"), "sessao omitida");
            assertSemLeitura(semSessao.json());

            c.sessaoAberta(pedido);
            Resposta semLeitura = c.telemetria(c.authA, pedido);
            assertEquals(200, semLeitura.status());
            assertTrue(semLeitura.json().has("sessao"));
            assertSemLeitura(semLeitura.json());
        }
    }

    @Test
    void bancoForaResponde503() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            c.cenario.capturas.bancoFora(true);
            Resposta r = c.telemetria(c.authA, pedido);
            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
        }
    }

    private static void assertSemLeitura(JsonNode corpo) {
        assertEquals(2, corpo.get("sensores").size());
        for (int i = 0; i < 2; i++) {
            JsonNode sensor = sensor(corpo, i);
            assertEquals("SEM_LEITURA", sensor.get("situacao").asText());
            assertEquals("CAPTURA_SEGUE_SEM_SENSOR", sensor.get("orientacao").get("codigo").asText());
            assertFalse(sensor.has("ultimaLeitura"), "ultimaLeitura omitida");
        }
    }

    private static JsonNode sensor(JsonNode corpo, int indice) {
        return corpo.get("sensores").get(indice);
    }
}
