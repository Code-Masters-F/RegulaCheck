package br.com.regulacheck.validacao.model;

/**
 * Resultado de uma regra do protocolo para um pedido (protocolo academico, secao "Resultados por regra").
 * Documento ilegivel nunca equivale a requisito ausente: falta de evidencia legivel e {@link #NAO_VERIFICAVEL}.
 */
public enum Resultado {

    /** A regra se aplica e a evidencia foi encontrada, confirmada e esta conforme. */
    ATENDE,

    /** A regra se aplica e a evidencia nao esta em documento lido com clareza, ou ha divergencia comprovada. */
    PENDENTE,

    /** A regra se aplica (ou a aplicabilidade e desconhecida), mas a evidencia nao pode ser verificada. */
    NAO_VERIFICAVEL,

    /** A condicao da regra condicional nao se aplica a este pedido; nao gera pendencia. */
    NAO_APLICAVEL;

    /** @return {@code true} se o resultado gera pendencia ({@code PENDENTE} ou {@code NAO_VERIFICAVEL}) */
    public boolean geraPendencia() {
        return this == PENDENTE || this == NAO_VERIFICAVEL;
    }
}
