package br.com.regulacheck.protocolo.entity;

/**
 * Natureza de uma regra do protocolo, como gravada em {@code RC_REGRA.NATUREZA}.
 */
public enum NaturezaRegra {

    /** A regra se aplica a todo pedido. */
    OBRIGATORIA,

    /** A regra so se aplica quando a condicao de aplicacao dela e verdadeira (ex.: paciente menor de 18 anos). */
    CONDICIONAL
}
