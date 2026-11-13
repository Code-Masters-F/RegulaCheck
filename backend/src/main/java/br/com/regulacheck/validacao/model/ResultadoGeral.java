package br.com.regulacheck.validacao.model;

import java.util.Collection;

/**
 * Resultado geral de uma avaliacao, pela precedencia do protocolo academico: alguma regra pendente leva a
 * {@link #COM_PENDENCIAS}; senao, alguma nao verificavel leva a {@link #REQUER_CONFERENCIA}; senao,
 * {@link #SEM_PENDENCIAS_IDENTIFICADAS}. {@code NAO_APLICAVEL} nao influencia. "Sem pendencias identificadas" nao e
 * aprovacao do SUS nem autorizacao de atendimento.
 */
public enum ResultadoGeral {

    /** Ha ao menos uma regra pendente. */
    COM_PENDENCIAS,

    /** Nenhuma pendente, mas ao menos uma nao verificavel: a paciente precisa conferir ou reenviar. */
    REQUER_CONFERENCIA,

    /** Todas as regras aplicaveis foram verificadas e atendem. */
    SEM_PENDENCIAS_IDENTIFICADAS;

    /**
     * Aplica a precedencia do protocolo.
     *
     * @param resultados resultado de cada regra avaliada; nao pode ser vazio (sem regra avaliada nao ha como
     *     afirmar "sem pendencias")
     * @return o resultado geral
     * @throws IllegalArgumentException se nao houver resultado algum
     */
    public static ResultadoGeral de(Collection<Resultado> resultados) {
        if (resultados.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma regra avaliada.");
        }
        if (resultados.contains(Resultado.PENDENTE)) {
            return COM_PENDENCIAS;
        }
        if (resultados.contains(Resultado.NAO_VERIFICAVEL)) {
            return REQUER_CONFERENCIA;
        }
        return SEM_PENDENCIAS_IDENTIFICADAS;
    }
}
