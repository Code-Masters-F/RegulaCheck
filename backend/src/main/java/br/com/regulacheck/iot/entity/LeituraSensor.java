package br.com.regulacheck.iot.entity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uma leitura gravada de um dos dois sensores simulados ({@code RC_LEITURA_SENSOR}). Leituras nao sao alteradas;
 * so sao apagadas junto com o rascunho do pedido.
 *
 * @param id identificador da leitura
 * @param sessaoId sessao de captura que recebeu a leitura
 * @param tipo sensor; a unidade e {@link TipoSensor#unidade()}
 * @param valor valor medido, com ate 3 casas decimais (lux ou cm)
 * @param medidaEm instante da medicao informado pelo simulador
 * @param recebidaEm instante em que o servidor gravou a leitura (UTC), nunca o informado pelo simulador
 */
public record LeituraSensor(long id, long sessaoId, TipoSensor tipo, BigDecimal valor, Instant medidaEm,
        Instant recebidaEm) {
}
