package br.com.regulacheck.validacao.model;

import br.com.regulacheck.integracao.extracao.CodigoCampo;

/**
 * Um campo de um documento que a regra considerou, com a situacao dele e a origem quando a leitura a tem. Nao
 * carrega o valor: quem precisar mostra-lo o busca na leitura da mesma revisao.
 *
 * @param documentoId documento considerado
 * @param codigo campo considerado
 * @param situacao situacao do campo
 * @param pagina pagina de origem (so PDF); {@code null} se nao ha
 * @param trecho trecho de origem; {@code null} se nao ha
 */
public record Evidencia(long documentoId, CodigoCampo codigo, SituacaoEvidencia situacao, Integer pagina,
        String trecho) {

    /**
     * @param documento documento avaliado
     * @param campo campo do documento
     * @return a evidencia do campo
     */
    public static Evidencia de(DocumentoAvaliado documento, CampoAvaliado campo) {
        return new Evidencia(documento.documentoId(), campo.codigo(), campo.situacao(), campo.pagina(),
                campo.trecho());
    }

    /** O trecho vem do documento: nunca aparece em log por descuido. */
    @Override
    public String toString() {
        return "Evidencia[documentoId=" + documentoId + ", codigo=" + codigo + ", situacao=" + situacao + "]";
    }
}
