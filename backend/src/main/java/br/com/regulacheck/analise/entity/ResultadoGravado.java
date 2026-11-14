package br.com.regulacheck.analise.entity;

import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Resultado;
import java.util.Objects;

/**
 * Resultado de uma regra numa analise gravada (uma linha de {@code RC_RESULTADO_REGRA}).
 *
 * @param codigoRegra codigo da regra na versao do protocolo da analise
 * @param descricaoRegra descricao da regra naquela versao
 * @param resultado um dos quatro resultados
 * @param motivo motivo do nao verificavel, lido da pendencia da mesma regra; {@code null} nos demais resultados e
 *     nas analises da carga sintetica
 */
public record ResultadoGravado(String codigoRegra, String descricaoRegra, Resultado resultado,
        MotivoNaoVerificavel motivo) {

    /** Exige codigo e resultado. */
    public ResultadoGravado {
        Objects.requireNonNull(codigoRegra, "codigoRegra");
        Objects.requireNonNull(resultado, "resultado");
    }
}
