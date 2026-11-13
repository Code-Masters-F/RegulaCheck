package br.com.regulacheck.validacao.model;

/**
 * O que a avaliacao sabe de um campo de um documento, combinando a leitura (RC-10) com a conferencia da paciente
 * (RC-11). So {@link #CONFIRMADO} traz valor utilizavel como evidencia.
 */
public enum SituacaoEvidencia {

    /** Identificado na leitura e confirmado pela paciente. */
    CONFIRMADO,

    /** O documento foi lido com clareza e o campo nao aparece nele (sem contestacao da paciente). */
    NAO_LOCALIZADO,

    /** O campo foi lido como ilegivel. */
    ILEGIVEL,

    /** Identificado na leitura, mas ainda sem confirmacao da paciente. */
    NAO_CONFERIDO,

    /** A paciente apontou que a leitura nao confere com o documento. */
    DIVERGENCIA_APONTADA,

    /** O documento inteiro nao foi lido (protegido ou leitura vazia). */
    DOCUMENTO_NAO_LIDO;

    /**
     * @return {@code true} se o campo nao pode ser usado para decidir nada: ilegivel, nao conferido, contestado ou
     *     de documento nao lido
     */
    public boolean naoVerificavel() {
        return this == ILEGIVEL || this == NAO_CONFERIDO || this == DIVERGENCIA_APONTADA || this == DOCUMENTO_NAO_LIDO;
    }

    /** @return o motivo de "nao verificavel" correspondente; {@code null} para confirmado e nao localizado */
    public MotivoNaoVerificavel motivo() {
        return switch (this) {
            case ILEGIVEL -> MotivoNaoVerificavel.CAMPO_ILEGIVEL;
            case NAO_CONFERIDO -> MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA;
            case DIVERGENCIA_APONTADA -> MotivoNaoVerificavel.DIVERGENCIA_APONTADA;
            case DOCUMENTO_NAO_LIDO -> MotivoNaoVerificavel.DOCUMENTO_NAO_LIDO;
            case CONFIRMADO, NAO_LOCALIZADO -> null;
        };
    }
}
