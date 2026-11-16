package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.iot.dto.LeituraSensorRequest;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.repository.NovaLeitura;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * RC-21 (FR-004, FR-005): validacao do corpo da leitura nas bordas dos limites fisicos (0 a 200000 lux; maior que
 * 0 e ate 500 cm), arredondamento a 3 casas e recusa de campo fora do contrato.
 */
class LeituraSensorRequestTest {

    private static final Instant AGORA = Instant.parse("2026-11-16T14:00:00Z");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void bordasDosLimitesFisicosSaoAceitas() throws Exception {
        assertEquals(new BigDecimal("0.000"), validar("LUMINOSIDADE", "0", "LUX").valor());
        assertEquals(new BigDecimal("200000.000"), validar("LUMINOSIDADE", "200000", "LUX").valor());
        assertEquals(new BigDecimal("0.001"), validar("DISTANCIA", "0.001", "CM").valor());
        assertEquals(new BigDecimal("500.000"), validar("DISTANCIA", "500", "CM").valor());
    }

    @Test
    void arredondaATresCasasEGuardaOInstanteDoServidor() throws Exception {
        NovaLeitura nova = validar("DISTANCIA", "30.12351", "CM");
        assertEquals(new BigDecimal("30.124"), nova.valor());
        assertEquals(TipoSensor.DISTANCIA, nova.tipo());
        assertEquals(7, nova.sessaoId());
        assertEquals(AGORA, nova.recebidaEm());
        assertEquals(AGORA.minusSeconds(3), nova.medidaEm());
    }

    @Test
    void campoForaDoContratoERecusado() throws Exception {
        JsonNode corpo = JSON.readTree("{\"sessaoId\":7,\"tipo\":\"DISTANCIA\",\"valor\":30,\"unidade\":\"CM\","
                + "\"medidaEm\":\"" + AGORA + "\",\"extra\":1}");
        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> LeituraSensorRequest.validar(corpo, 7, AGORA));
        assertEquals(400, recusa.status());
        assertEquals("corpo", recusa.detalhes().get(0).campo());
    }

    @Test
    void instanteComOutroFusoEConvertidoParaUtc() throws Exception {
        JsonNode corpo = JSON.readTree("{\"sessaoId\":7,\"tipo\":\"DISTANCIA\",\"valor\":30,\"unidade\":\"CM\","
                + "\"medidaEm\":\"2026-11-16T10:59:50-03:00\"}");
        assertEquals(Instant.parse("2026-11-16T13:59:50Z"), LeituraSensorRequest.validar(corpo, 7, AGORA).medidaEm());
    }

    private static NovaLeitura validar(String tipo, String valor, String unidade) throws Exception {
        JsonNode corpo = JSON.readTree("{\"sessaoId\":7,\"tipo\":\"" + tipo + "\",\"valor\":" + valor
                + ",\"unidade\":\"" + unidade + "\",\"medidaEm\":\"" + AGORA.minusSeconds(3) + "\"}");
        return LeituraSensorRequest.validar(corpo, 7, AGORA);
    }
}
