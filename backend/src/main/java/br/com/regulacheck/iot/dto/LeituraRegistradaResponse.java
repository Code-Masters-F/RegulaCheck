package br.com.regulacheck.iot.dto;

import br.com.regulacheck.iot.entity.LeituraSensor;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * Resposta de {@code POST /sessoes-captura/{id}/leituras} (RC-21): a leitura como foi gravada.
 *
 * @param id leitura
 * @param sessaoId sessao de captura
 * @param tipo {@code LUMINOSIDADE} ou {@code DISTANCIA}
 * @param valor valor gravado (3 casas)
 * @param unidade {@code LUX} ou {@code CM}
 * @param medidaEm instante da medicao informado pelo simulador (ISO-8601, UTC)
 * @param recebidaEm instante do servidor (ISO-8601, UTC)
 */
public record LeituraRegistradaResponse(String id, String sessaoId, String tipo, BigDecimal valor, String unidade,
        String medidaEm, String recebidaEm) {

    /**
     * @param leitura leitura gravada
     * @return a resposta
     */
    public static LeituraRegistradaResponse de(LeituraSensor leitura) {
        return new LeituraRegistradaResponse(Long.toString(leitura.id()), Long.toString(leitura.sessaoId()),
                leitura.tipo().name(), leitura.valor(), leitura.tipo().unidade(),
                leitura.medidaEm().truncatedTo(ChronoUnit.MILLIS).toString(),
                leitura.recebidaEm().truncatedTo(ChronoUnit.MILLIS).toString());
    }
}
