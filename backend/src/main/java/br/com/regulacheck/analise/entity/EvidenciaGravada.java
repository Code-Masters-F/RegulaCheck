package br.com.regulacheck.analise.entity;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.util.Objects;

/**
 * Evidencia de uma pendencia gravada: o documento, o campo e a situacao que a regra considerou no momento da
 * analise. A situacao e a gravada (a decisao de conferencia pode ter mudado depois); a origem vem da leitura da
 * mesma revisao, que nunca e alterada. Nao carrega o valor lido.
 *
 * @param documentoId documento considerado
 * @param codigo campo considerado
 * @param situacao situacao do campo quando a analise foi feita
 * @param pagina pagina de origem na leitura daquela revisao (so PDF); {@code null} se nao ha
 * @param trecho trecho de origem na leitura daquela revisao; {@code null} se nao ha
 */
public record EvidenciaGravada(long documentoId, CodigoCampo codigo, SituacaoEvidencia situacao, Integer pagina,
        String trecho) {

    /** Exige campo e situacao. */
    public EvidenciaGravada {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(situacao, "situacao");
    }

    /** O trecho vem do documento: nunca aparece em log por descuido. */
    @Override
    public String toString() {
        return "EvidenciaGravada[documentoId=" + documentoId + ", codigo=" + codigo + ", situacao=" + situacao + "]";
    }
}
