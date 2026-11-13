package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import java.util.List;

/**
 * Leitura de um pedido junto com as decisoes vigentes da paciente sobre ela e a versao atual do pedido.
 *
 * @param leitura a leitura, com a classificacao atual/desatualizada
 * @param decisoes decisoes vigentes (uma por campo, no maximo)
 */
public record ConferenciaRegistrada(ExtracaoRegistrada leitura, List<DecisaoConferencia> decisoes) {

    /** Copia defensiva. */
    public ConferenciaRegistrada {
        decisoes = decisoes == null ? List.of() : List.copyOf(decisoes);
    }
}
