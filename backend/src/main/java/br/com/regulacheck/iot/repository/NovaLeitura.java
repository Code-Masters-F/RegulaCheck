package br.com.regulacheck.iot.repository;

import br.com.regulacheck.iot.entity.TipoSensor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Leitura ja validada pela API, pronta para gravar.
 *
 * @param sessaoId sessao de captura indicada no caminho (e no corpo)
 * @param tipo sensor
 * @param valor valor arredondado a 3 casas, dentro dos limites fisicos
 * @param medidaEm instante da medicao informado pelo simulador
 * @param recebidaEm instante do servidor (UTC) em que a leitura foi recebida
 */
public record NovaLeitura(long sessaoId, TipoSensor tipo, BigDecimal valor, Instant medidaEm, Instant recebidaEm) {
}
