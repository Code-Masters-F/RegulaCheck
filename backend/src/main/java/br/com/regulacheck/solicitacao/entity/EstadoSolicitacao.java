package br.com.regulacheck.solicitacao.entity;

/**
 * Estado de uma solicitacao. Conjunto fechado, igual ao CHECK {@code RC_SOLICITACAO_CK_ESTADO} do banco
 * (ver {@code docs/api/estados-e-revisoes.md}).
 *
 * <p>A RC-07 so produz e altera pedidos em {@link #RASCUNHO}; os demais estados existem para que a leitura de
 * um pedido de qualquer estado seja fiel ao banco e para que a troca de servico possa recusar o que ja saiu de
 * rascunho.
 */
public enum EstadoSolicitacao {
    /** Pedido criado, documentos ainda sendo enviados; nada processado. */
    RASCUNHO,
    /** Extracao/analise em andamento; nao equivale a concluido. */
    PROCESSANDO,
    /** A paciente precisa conferir a transcricao ou reenviar. */
    REQUER_CONFERENCIA,
    /** Ha regra pendente. */
    COM_PENDENCIAS,
    /** Criterios aplicaveis atendidos; nao e aprovacao do SUS. */
    SEM_PENDENCIAS_IDENTIFICADAS,
    /** Erro de processamento; permite tentar de novo. */
    FALHA_ANALISE,
    /** Submissao concluida apenas no simulador; estado final. */
    SUBMETIDA_SIMULACAO
}
