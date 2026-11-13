package br.com.regulacheck.validacao.model;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de aplicar uma versao do protocolo a uma entrada: um resultado por regra, as pendencias e o resultado
 * geral. Produzido pelo motor; nao e gravado pela RC-12.
 *
 * @param resultados um por regra da versao, na ordem da versao
 * @param pendencias uma por regra pendente ou nao verificavel, na mesma ordem
 * @param resultadoGeral resultado geral pela precedencia do protocolo
 */
public record AvaliacaoDeRegras(List<ResultadoDeRegra> resultados, List<Pendencia> pendencias,
        ResultadoGeral resultadoGeral) {

    /** Copias defensivas. */
    public AvaliacaoDeRegras {
        resultados = List.copyOf(resultados);
        pendencias = List.copyOf(pendencias);
        Objects.requireNonNull(resultadoGeral, "resultadoGeral");
    }

    /**
     * @param codigoRegra codigo da regra na versao
     * @return o resultado dessa regra
     * @throws IllegalArgumentException se a regra nao faz parte da avaliacao
     */
    public ResultadoDeRegra resultadoDe(String codigoRegra) {
        return resultados.stream().filter(r -> r.regra().codigo().equals(codigoRegra)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Regra fora da avaliacao: " + codigoRegra));
    }
}
