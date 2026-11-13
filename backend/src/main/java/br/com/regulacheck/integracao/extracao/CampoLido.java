package br.com.regulacheck.integracao.extracao;

/**
 * Um campo extraido de um documento.
 *
 * @param codigo campo das regras
 * @param situacao identificado, nao localizado ou ilegivel
 * @param valor valor lido; {@code null} salvo se {@code IDENTIFICADO}
 * @param pagina pagina de origem (so PDF); {@code null} se nao ha ou nao se aplica
 * @param trecho trecho de origem; {@code null} se nao disponivel
 */
public record CampoLido(CodigoCampo codigo, SituacaoCampo situacao, String valor, Integer pagina, String trecho) {

    /**
     * @param codigo campo
     * @param valor valor lido
     * @param pagina pagina de origem, ou {@code null}
     * @param trecho trecho de origem, ou {@code null}
     * @return campo identificado
     */
    public static CampoLido identificado(CodigoCampo codigo, String valor, Integer pagina, String trecho) {
        return new CampoLido(codigo, SituacaoCampo.IDENTIFICADO, valor, pagina, trecho);
    }

    /**
     * @param codigo campo
     * @return campo nao localizado em documento lido com clareza (sem valor nem origem)
     */
    public static CampoLido naoLocalizado(CodigoCampo codigo) {
        return new CampoLido(codigo, SituacaoCampo.NAO_LOCALIZADO, null, null, null);
    }

    /**
     * @param codigo campo
     * @return campo ilegivel (sem valor nem origem)
     */
    public static CampoLido ilegivel(CodigoCampo codigo) {
        return new CampoLido(codigo, SituacaoCampo.ILEGIVEL, null, null, null);
    }

    /** O valor e o trecho vem de documento: nunca aparecem em texto de log por descuido. */
    @Override
    public String toString() {
        return "CampoLido[codigo=" + codigo + ", situacao=" + situacao + "]";
    }
}
