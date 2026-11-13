package br.com.regulacheck.extracao.entity;

/**
 * Por que a paciente apontou divergencia. Lista fechada: texto livre poderia carregar dado clinico ou instrucao.
 */
public enum MotivoDivergencia {

    /** O campo foi identificado, mas o valor lido nao confere com o documento. */
    VALOR_DIFERENTE_DO_DOCUMENTO,

    /** O campo nao foi localizado na leitura, mas a paciente diz que ele consta no documento. */
    CAMPO_EXISTE_E_NAO_FOI_LIDO
}
