package br.com.regulacheck.validacao.model;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import java.util.Objects;

/**
 * Um campo de um documento como a avaliacao o enxerga. O valor so existe quando o campo foi identificado e
 * confirmado: campo nao conferido, contestado ou ilegivel nunca carrega valor para as regras.
 *
 * @param codigo campo das regras
 * @param situacao o que se sabe do campo
 * @param valor valor confirmado; {@code null} salvo em {@link SituacaoEvidencia#CONFIRMADO}
 * @param pagina pagina de origem (so PDF); {@code null} se nao ha
 * @param trecho trecho de origem; {@code null} se nao ha
 */
public record CampoAvaliado(CodigoCampo codigo, SituacaoEvidencia situacao, String valor, Integer pagina,
        String trecho) {

    /** Valida a invariante: valor presente se, e somente se, confirmado. */
    public CampoAvaliado {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(situacao, "situacao");
        if ((situacao == SituacaoEvidencia.CONFIRMADO) != (valor != null)) {
            throw new IllegalArgumentException("Valor so existe em campo confirmado.");
        }
    }

    /**
     * @param codigo campo
     * @param valor valor identificado e confirmado
     * @param pagina pagina de origem, ou {@code null}
     * @param trecho trecho de origem, ou {@code null}
     * @return campo confirmado
     */
    public static CampoAvaliado confirmado(CodigoCampo codigo, String valor, Integer pagina, String trecho) {
        return new CampoAvaliado(codigo, SituacaoEvidencia.CONFIRMADO, Objects.requireNonNull(valor, "valor"),
                pagina, trecho);
    }

    /**
     * @param codigo campo
     * @param situacao qualquer situacao diferente de confirmado
     * @param pagina pagina de origem, ou {@code null}
     * @param trecho trecho de origem, ou {@code null}
     * @return campo sem valor utilizavel
     */
    public static CampoAvaliado semValor(CodigoCampo codigo, SituacaoEvidencia situacao, Integer pagina,
            String trecho) {
        return new CampoAvaliado(codigo, situacao, null, pagina, trecho);
    }

    /** Valor e trecho vem do documento: nunca aparecem em log por descuido. */
    @Override
    public String toString() {
        return "CampoAvaliado[codigo=" + codigo + ", situacao=" + situacao + "]";
    }
}
