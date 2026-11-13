package br.com.regulacheck.validacao.model;

/**
 * Por que uma regra ficou nao verificavel. Lista fechada: acompanha a mensagem padrao do protocolo para que a
 * orientacao seja especifica sem usar texto livre (que poderia carregar conteudo do documento).
 */
public enum MotivoNaoVerificavel {

    /** Um campo necessario foi lido como ilegivel. */
    CAMPO_ILEGIVEL,

    /** Um documento que pode conter a evidencia nao foi lido (protegido ou leitura vazia). */
    DOCUMENTO_NAO_LIDO,

    /** Um campo necessario foi identificado, mas a paciente ainda nao confirmou a transcricao. */
    TRANSCRICAO_NAO_CONFERIDA,

    /** A paciente apontou que a leitura de um campo necessario nao confere com o documento. */
    DIVERGENCIA_APONTADA,

    /** Um valor confirmado nao pode ser interpretado (formato inesperado); o sistema nao adivinha. */
    VALOR_NAO_INTERPRETAVEL,

    /** A condicao de uma regra condicional nao pode ser determinada (ex.: idade desconhecida). */
    CONDICAO_INDETERMINADA,

    /** Nao ha documentos legiveis suficientes para a comparacao que a regra exige. */
    DOCUMENTOS_INSUFICIENTES,

    /** A regra depende de um documento que nao foi encontrado (cobrado por outra regra). */
    DEPENDE_DE_DOCUMENTO_AUSENTE
}
