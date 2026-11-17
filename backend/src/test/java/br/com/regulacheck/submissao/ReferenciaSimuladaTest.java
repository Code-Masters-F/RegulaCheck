package br.com.regulacheck.submissao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.submissao.service.SubmissaoService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Referencia interna da submissao simulada (RC-17, FR-004): prefixo de simulacao, ano em Salvador e id do pedido com
 * no minimo seis digitos. Expectativas escritas a partir do research D4, nao da implementacao.
 */
class ReferenciaSimuladaTest {

    @Test
    void usaPrefixoDeSimulacaoAnoEIdComSeisDigitos() {
        assertEquals("SIM-2026-000012", SubmissaoService.referencia(12, Instant.parse("2026-11-17T15:00:00Z")));
    }

    @Test
    void anoEODeSalvadorNaViradaDoAnoEmUtc() {
        // 02:00 UTC de 01/01/2027 ainda e 23:00 de 31/12/2026 em Salvador (UTC-3).
        assertEquals("SIM-2026-000007", SubmissaoService.referencia(7, Instant.parse("2027-01-01T02:00:00Z")));
        assertEquals("SIM-2027-000007", SubmissaoService.referencia(7, Instant.parse("2027-01-01T03:00:00Z")));
    }

    @Test
    void idMaiorQueSeisDigitosNaoETruncado() {
        assertEquals("SIM-2026-1234567", SubmissaoService.referencia(1_234_567, Instant.parse("2026-11-17T15:00:00Z")));
    }

    @Test
    void nuncaCoincideComOFormatoDaCargaSintetica() {
        String referencia = SubmissaoService.referencia(1, Instant.parse("2026-11-12T11:45:00Z"));

        assertNotEquals("SIM-2026-0001", referencia, "a carga 02_ usa quatro digitos");
        assertTrue(referencia.matches("^SIM-\\d{4}-\\d{6,}$"), referencia);
    }
}
