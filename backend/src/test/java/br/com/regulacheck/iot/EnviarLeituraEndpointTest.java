package br.com.regulacheck.iot;

import static br.com.regulacheck.iot.CenarioIot.leitura;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.TipoSensor;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * RC-21, US1 e US2 (FR-001 a FR-007, SC-002): o simulador com a chave grava leituras validas; credencial, sessao
 * ou leitura invalida sao recusadas sem gravar nada, na ordem 401/403 → 404 → 415/400.
 */
class EnviarLeituraEndpointTest {

    @Test
    void leiturasValidasSaoGravadasComInstanteDoServidor() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            Instant medida = c.agora().minusSeconds(5);

            Resposta luz = c.enviar(sessao, leitura(sessao, "LUMINOSIDADE", "350", "LUX", medida));
            assertEquals(201, luz.status(), luz.corpo());
            JsonNode corpo = luz.json();
            assertEquals(sessao, corpo.get("sessaoId").asText());
            assertEquals("LUMINOSIDADE", corpo.get("tipo").asText());
            assertEquals("LUX", corpo.get("unidade").asText());
            assertEquals(0, new BigDecimal("350").compareTo(corpo.get("valor").decimalValue()));
            assertEquals(medida.toString(), corpo.get("medidaEm").asText());
            assertEquals(c.agora().toString(), corpo.get("recebidaEm").asText());

            Resposta distancia = c.enviar(sessao, leitura(sessao, "DISTANCIA", "30.12349", "CM", c.agora()));
            assertEquals(201, distancia.status(), distancia.corpo());

            List<LeituraSensor> gravadas = c.cenario.capturas.leituras();
            assertEquals(2, gravadas.size());
            assertEquals(TipoSensor.DISTANCIA, gravadas.get(1).tipo());
            assertEquals(new BigDecimal("30.123"), gravadas.get(1).valor());
            assertEquals(c.agora(), gravadas.get(0).recebidaEm());
        }
    }

    @Test
    void foraDaFaixaDemonstrativaETolerancaDeRelogioSaoAceitos() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            assertEquals(201, c.enviar(sessao, leitura(sessao, "LUMINOSIDADE", "0", "LUX", c.agora())).status());
            assertEquals(201, c.enviar(sessao, leitura(sessao, "LUMINOSIDADE", "200000", "LUX", c.agora())).status());
            assertEquals(201, c.enviar(sessao, leitura(sessao, "DISTANCIA", "500", "CM", c.agora())).status());
            assertEquals(201, c.enviar(sessao,
                    leitura(sessao, "DISTANCIA", "5", "CM", c.agora().plusSeconds(29))).status());
            assertEquals(4, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void credencialInvalidaOuDeContaERecusada() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            String corpo = leitura(sessao, "LUMINOSIDADE", "350", "LUX", c.agora());
            String caminho = "/sessoes-captura/" + sessao + "/leituras";

            assertRecusa(c.api.postJson(caminho, corpo, Map.of()), 401, "NAO_AUTENTICADO");
            assertRecusa(c.api.postJson(caminho, corpo, Map.of("X-Chave-Simulador", "chave-errada-ficticia")), 401,
                    "NAO_AUTENTICADO");
            assertRecusa(c.api.postJson(caminho, corpo, c.authA), 403, "ACESSO_NEGADO");
            assertRecusa(c.api.postJson(caminho, corpo, c.admin), 403, "ACESSO_NEGADO");
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void servidorSemChaveConfiguradaRecusaTodoEnvio() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar(null)) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            assertRecusa(c.enviar(sessao, "LUMINOSIDADE", "350"), 401, "NAO_AUTENTICADO");
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void sessaoInexistenteOuEncerradaTemAMesmaResposta() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            long encerrada = c.cenario.capturas.adicionarSessao(Long.parseLong(pedido), c.agora().minusSeconds(60),
                    c.agora().minusSeconds(30)).id();
            Resposta r1 = c.enviar("999", leitura("999", "LUMINOSIDADE", "350", "LUX", c.agora()));
            Resposta r2 = c.enviar(Long.toString(encerrada),
                    leitura(Long.toString(encerrada), "LUMINOSIDADE", "350", "LUX", c.agora()));
            Resposta r3 = c.api.postJson("/sessoes-captura/abc/leituras", "{}", c.simulador);
            for (Resposta r : List.of(r1, r2, r3)) {
                assertRecusa(r, 404, "NAO_ENCONTRADO");
            }
            assertEquals(r1.corpo(), r2.corpo());
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void sessaoInexistenteVemAntesDoCorpoInvalido() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            assertRecusa(c.enviar("999", "{malformado"), 404, "NAO_ENCONTRADO");
            assertRecusa(c.api.post("/sessoes-captura/999/leituras", "x", "text/plain", c.simulador), 404,
                    "NAO_ENCONTRADO");
        }
    }

    @Test
    void leituraInvalidaERecusadaComOCampo() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String s = c.sessaoAberta(c.criarPedido(c.authA));
            Instant agora = c.agora();
            Map<String, String> casos = new LinkedHashMap<>();
            casos.put("{\"sessaoId\":1234,\"tipo\":\"LUMINOSIDADE\",\"valor\":350,\"unidade\":\"LUX\",\"medidaEm\":\""
                    + agora + "\"}", "sessaoId");
            casos.put("{\"tipo\":\"LUMINOSIDADE\",\"valor\":350,\"unidade\":\"LUX\",\"medidaEm\":\"" + agora + "\"}",
                    "sessaoId");
            casos.put(leitura(s, "TEMPERATURA", "20", "LUX", agora), "tipo");
            casos.put(leitura(s, "DISTANCIA", "30", "LUX", agora), "unidade");
            casos.put(leitura(s, "LUMINOSIDADE", "350", "lux", agora), "unidade");
            casos.put("{\"sessaoId\":" + s + ",\"tipo\":\"LUMINOSIDADE\",\"unidade\":\"LUX\",\"medidaEm\":\"" + agora
                    + "\"}", "valor");
            casos.put(leitura(s, "LUMINOSIDADE", "\"350\"", "LUX", agora), "valor");
            casos.put(leitura(s, "LUMINOSIDADE", "-1", "LUX", agora), "valor");
            casos.put(leitura(s, "LUMINOSIDADE", "200001", "LUX", agora), "valor");
            casos.put(leitura(s, "DISTANCIA", "0", "CM", agora), "valor");
            casos.put(leitura(s, "DISTANCIA", "501", "CM", agora), "valor");
            casos.put("{\"sessaoId\":" + s + ",\"tipo\":\"DISTANCIA\",\"valor\":30,\"unidade\":\"CM\"}", "medidaEm");
            casos.put("{\"sessaoId\":" + s + ",\"tipo\":\"DISTANCIA\",\"valor\":30,\"unidade\":\"CM\","
                    + "\"medidaEm\":\"2026-11-12T20:00:00\"}", "medidaEm");
            casos.put(leitura(s, "DISTANCIA", "30", "CM", agora.plusSeconds(31)), "medidaEm");
            casos.put("{\"sessaoId\":" + s + ",\"tipo\":\"DISTANCIA\",\"valor\":30,\"unidade\":\"CM\",\"medidaEm\":\""
                    + agora + "\",\"pacienteId\":2}", "corpo");
            casos.put("[1,2]", "corpo");
            for (Map.Entry<String, String> caso : casos.entrySet()) {
                Resposta r = c.enviar(s, caso.getKey());
                assertRecusa(r, 400, "REQUISICAO_INVALIDA");
                assertEquals(caso.getValue(), r.json().get("detalhes").get(0).get("campo").asText(), caso.getKey());
            }
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void corpoForaDoFormatoERecusado() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String s = c.sessaoAberta(c.criarPedido(c.authA));
            String caminho = "/sessoes-captura/" + s + "/leituras";
            assertRecusa(c.api.post(caminho, leitura(s, "DISTANCIA", "30", "CM", c.agora()), "text/plain",
                    c.simulador), 415, "TIPO_NAO_SUPORTADO");
            assertRecusa(c.enviar(s, "{malformado"), 400, "REQUISICAO_INVALIDA");
            assertRecusa(c.enviar(s, "{\"x\":\"" + "a".repeat(5000) + "\"}"), 400, "REQUISICAO_INVALIDA");
            assertEquals(405, c.api.get(caminho, c.simulador).status());
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void bancoForaResponde503SemGravar() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String s = c.sessaoAberta(c.criarPedido(c.authA));
            c.cenario.capturas.bancoFora(true);
            assertRecusa(c.enviar(s, "LUMINOSIDADE", "350"), 503, "DEPENDENCIA_INDISPONIVEL");
            c.cenario.capturas.bancoFora(false);
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    private static void assertRecusa(Resposta r, int status, String codigo) {
        assertEquals(status, r.status(), r.corpo());
        assertEquals(codigo, r.json().get("codigo").asText(), r.corpo());
    }
}
