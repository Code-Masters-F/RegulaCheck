package br.com.regulacheck.analise.repository;

/** Como encerrar uma analise em andamento que nao vai gravar resultado. */
public enum Encerramento {

    /** A avaliacao falhou: o pedido vai a {@code FALHA_ANALISE} e a paciente pode tentar de novo. */
    FALHA,

    /**
     * A entrada mudou durante a analise (documentos ou conferencia): o resultado nao vale e o pedido volta ao
     * resultado geral da analise concluida mais recente, ou a {@code RASCUNHO} se nunca houve.
     */
    DESCARTE
}
