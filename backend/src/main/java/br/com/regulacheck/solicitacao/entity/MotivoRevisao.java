package br.com.regulacheck.solicitacao.entity;

/**
 * Motivo pelo qual uma revisao de pedido foi aberta. Conjunto fechado, igual ao CHECK
 * {@code RC_REVISAO_CK_MOTIVO} do banco; {@link #PROTOCOLO_ALTERADO} depende do script
 * {@code 06_motivo_revisao_protocolo.sql} em bancos criados antes da RC-07.
 */
public enum MotivoRevisao {
    /** Revisao 1, aberta na criacao do pedido. */
    CRIACAO,
    /** Documento enviado (historia de envio). */
    DOCUMENTO_ENVIADO,
    /** Documento substituido (historia de envio). */
    DOCUMENTO_SUBSTITUIDO,
    /** Documento removido (historia de envio). */
    DOCUMENTO_REMOVIDO,
    /** Conferencia da transcricao alterada (historia de extracao). */
    CONFERENCIA_ALTERADA,
    /** Servico (protocolo) do rascunho trocado pela paciente. */
    PROTOCOLO_ALTERADO
}
