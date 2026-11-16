package br.com.regulacheck.iot.service;

/**
 * Situacao de um sensor na consulta de telemetria (RC-22), calculada na hora a partir da ultima leitura; nao e
 * gravada.
 */
public enum SituacaoSensor {
    /** Ultima leitura atual e dentro da faixa demonstrativa (limites inclusivos). */
    DENTRO_DA_FAIXA,
    /** Ultima leitura atual e abaixo do minimo da faixa. */
    ABAIXO_DA_FAIXA,
    /** Ultima leitura atual e acima do maximo da faixa. */
    ACIMA_DA_FAIXA,
    /** Ultima leitura mais antiga que o prazo de atualidade: nao diz nada sobre a condicao de agora. */
    DESATUALIZADA,
    /** Nenhuma leitura do sensor na sessao mais recente (ou o pedido nao tem sessao). */
    SEM_LEITURA
}
