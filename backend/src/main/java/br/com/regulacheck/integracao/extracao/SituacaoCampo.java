package br.com.regulacheck.integracao.extracao;

/**
 * Situacao de um campo extraido. Um campo esta em exatamente uma delas; "ilegivel" nunca equivale a "ausente".
 */
public enum SituacaoCampo {

    /** O campo foi lido: tem valor e, quando disponivel, origem. */
    IDENTIFICADO,

    /** O documento foi lido com clareza e o campo nao aparece nele. Sem valor. */
    NAO_LOCALIZADO,

    /** O campo (ou o trecho onde estaria) nao pode ser lido. Sem valor. */
    ILEGIVEL
}
