package br.com.regulacheck.protocolo.entity;

/**
 * Quem provavelmente precisa agir para resolver uma pendencia, como gravado em {@code RC_REGRA.RESPONSAVEL}.
 * Informacao clinica ausente ou incorreta no documento e sempre da unidade emissora: a paciente nunca altera o
 * documento original.
 */
public enum ResponsavelCorrecao {

    /** A propria paciente (enviar documento, arquivo mais nitido ou conferir a transcricao). */
    PACIENTE,

    /** A unidade de saude que emitiu o documento (completar ou reemitir). */
    UNIDADE_EMISSORA
}
