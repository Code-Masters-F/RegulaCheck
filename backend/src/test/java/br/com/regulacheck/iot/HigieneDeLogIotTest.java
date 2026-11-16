package br.com.regulacheck.iot;

import static br.com.regulacheck.iot.CenarioIot.leitura;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * RC-21, US2 cenario 6 (FR-007): envios aceitos e recusados nao deixam no log nem nas respostas de erro a chave do
 * simulador, o valor medido nem o corpo; so ids, tipo e codigos.
 */
class HigieneDeLogIotTest {

    private static final String VALOR_MARCADO = "123.456";
    private static final String CHAVE_ERRADA = "chave-ficticia-errada-987654";

    @Test
    void chaveECorpoNaoVaoParaOLog() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> respostas = new ArrayList<>();
        try (CenarioIot c = CenarioIot.iniciar()) {
            String s = c.sessaoAberta(c.criarPedido(c.authA));
            respostas.add(c.enviar(s, leitura(s, "LUMINOSIDADE", VALOR_MARCADO, "LUX", c.agora())).corpo());
            respostas.add(c.enviar(s, leitura(s, "DISTANCIA", VALOR_MARCADO, "LUX", c.agora())).corpo());
            respostas.add(c.enviar(s, leitura(s, "LUMINOSIDADE", "-" + VALOR_MARCADO, "LUX", c.agora())).corpo());
            respostas.add(c.enviar("999", leitura("999", "LUMINOSIDADE", VALOR_MARCADO, "LUX", c.agora())).corpo());
            respostas.add(c.api.postJson("/sessoes-captura/" + s + "/leituras",
                    leitura(s, "LUMINOSIDADE", VALOR_MARCADO, "LUX", c.agora()),
                    Map.of("X-Chave-Simulador", CHAVE_ERRADA)).corpo());
            respostas.add(c.api.postJson("/sessoes-captura/" + s + "/leituras",
                    leitura(s, "LUMINOSIDADE", VALOR_MARCADO, "LUX", c.agora()), c.authA).corpo());
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Leitura de sensor gravada"), "a gravacao deve deixar rastro tecnico");
        assertTrue(log.contains("Sessao de captura aberta"), "a abertura deve deixar rastro tecnico");
        for (String proibido : List.of(CenarioIot.CHAVE, CHAVE_ERRADA, VALOR_MARCADO, "\"medidaEm\"")) {
            assertFalse(log.contains(proibido), "o log vazou: " + proibido);
            for (String resposta : respostas.subList(1, respostas.size())) {
                assertFalse(resposta.contains(proibido), "erro vazou: " + proibido);
            }
        }
        assertFalse(log.contains("\tat "), "sem stack trace no log de recusas esperadas");
    }
}
