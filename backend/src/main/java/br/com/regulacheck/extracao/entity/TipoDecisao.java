package br.com.regulacheck.extracao.entity;

/**
 * O que a paciente decidiu sobre um campo lido. So existem duas decisoes: ela nunca digita valor nem corrige o
 * documento (CLAUDE.md, secao 11).
 */
public enum TipoDecisao {

    /** A paciente confirma que o valor lido confere com o documento. */
    CONFIRMADA,

    /** A paciente aponta que a leitura nao confere com o documento (ver {@link MotivoDivergencia}). */
    DIVERGENTE
}
