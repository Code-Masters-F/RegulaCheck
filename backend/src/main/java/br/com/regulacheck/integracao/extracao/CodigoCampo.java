package br.com.regulacheck.integracao.extracao;

/**
 * Campos que a leitura extrai: somente os usados pelas regras do protocolo academico (RC-R01 a RC-R06). Conjunto
 * fechado; qualquer outro codigo vindo de um leitor e rejeitado pelo {@link ValidadorDeLeitura}.
 */
public enum CodigoCampo {

    /** Natureza do documento (regras R01 e R03): {@code ENCAMINHAMENTO}, {@code IDENTIFICACAO}, etc. */
    TIPO_DOCUMENTO,

    /** Nome da paciente (regra R02). */
    NOME,

    /** Data de nascimento, {@code AAAA-MM-DD} (regras R02 e R06). */
    DATA_NASCIMENTO,

    /** Motivo ou justificativa do encaminhamento (regra R04). */
    MOTIVO_ENCAMINHAMENTO,

    /** Data de emissao, {@code AAAA-MM-DD} (regra R05). */
    DATA_EMISSAO,

    /** Presenca de autorizacao assinada do responsavel, {@code SIM} ou {@code NAO} (regra R06). */
    AUTORIZACAO_ASSINADA
}
