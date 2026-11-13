package br.com.regulacheck.validacao.model;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Um documento do pedido como a avaliacao o enxerga: os seis campos das regras, cada um exatamente uma vez. Um
 * documento nao lido tem os seis campos em {@link SituacaoEvidencia#DOCUMENTO_NAO_LIDO}.
 *
 * @param documentoId documento
 * @param campos os seis campos (um por {@link CodigoCampo})
 */
public record DocumentoAvaliado(long documentoId, List<CampoAvaliado> campos) {

    /** Copia defensiva e conferencia de que cada codigo aparece exatamente uma vez. */
    public DocumentoAvaliado {
        campos = List.copyOf(campos);
        Map<CodigoCampo, CampoAvaliado> porCodigo = new EnumMap<>(CodigoCampo.class);
        for (CampoAvaliado campo : campos) {
            if (porCodigo.put(campo.codigo(), campo) != null) {
                throw new IllegalArgumentException("Campo repetido no documento.");
            }
        }
        if (porCodigo.size() != CodigoCampo.values().length) {
            throw new IllegalArgumentException("O documento precisa dos seis campos das regras.");
        }
    }

    /**
     * @param codigo campo desejado
     * @return o campo (sempre existe)
     */
    public CampoAvaliado campo(CodigoCampo codigo) {
        for (CampoAvaliado campo : campos) {
            if (campo.codigo() == codigo) {
                return campo;
            }
        }
        throw new IllegalStateException("Campo ausente: " + codigo);
    }

    /** @return {@code true} se o documento nao foi lido (protegido ou leitura vazia) */
    public boolean naoLido() {
        return campos.get(0).situacao() == SituacaoEvidencia.DOCUMENTO_NAO_LIDO;
    }

    /**
     * @param codigo campo
     * @return a evidencia correspondente ao campo deste documento
     */
    public Evidencia evidencia(CodigoCampo codigo) {
        return Evidencia.de(this, campo(codigo));
    }
}
