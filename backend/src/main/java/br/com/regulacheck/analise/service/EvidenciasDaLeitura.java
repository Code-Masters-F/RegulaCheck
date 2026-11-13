package br.com.regulacheck.analise.service;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import br.com.regulacheck.validacao.model.CampoAvaliado;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combina a leitura (RC-10) com as decisoes de conferencia da paciente (RC-11) e produz os documentos que o motor
 * avalia. Regra pura: sem HTTP, banco ou relogio.
 *
 * <p><strong>So vale como evidencia o que foi identificado e confirmado.</strong> Campo identificado sem decisao
 * vira {@code NAO_CONFERIDO}; com divergencia apontada vira {@code DIVERGENCIA_APONTADA}; ilegivel continua
 * ilegivel; nao localizado conta como ausente, salvo se a paciente apontou que ele existe no documento; documento
 * protegido ou de leitura vazia tem os seis campos em {@code DOCUMENTO_NAO_LIDO}. Uma decisao que nao cabe a
 * situacao do campo (dado inconsistente) e ignorada, como na situacao da conferencia: nunca "limpa" um campo.
 */
public final class EvidenciasDaLeitura {

    private EvidenciasDaLeitura() {
    }

    /**
     * @param leitura leitura atual do pedido
     * @param decisoes decisoes vigentes da paciente sobre essa leitura
     * @return um documento avaliado por documento da leitura, na mesma ordem
     */
    public static List<DocumentoAvaliado> montar(Extracao leitura, List<DecisaoConferencia> decisoes) {
        Map<String, DecisaoConferencia> porCampo = new HashMap<>();
        for (DecisaoConferencia decisao : decisoes) {
            porCampo.put(chave(decisao.documentoId(), decisao.codigo()), decisao);
        }
        List<DocumentoAvaliado> documentos = new ArrayList<>();
        for (ExtracaoDocumento documento : leitura.documentos()) {
            documentos.add(avaliar(documento, porCampo));
        }
        return documentos;
    }

    private static DocumentoAvaliado avaliar(ExtracaoDocumento documento, Map<String, DecisaoConferencia> porCampo) {
        List<CampoAvaliado> campos = new ArrayList<>();
        for (CodigoCampo codigo : CodigoCampo.values()) {
            if (documento.situacao() != SituacaoLeitura.LIDO) {
                campos.add(CampoAvaliado.semValor(codigo, SituacaoEvidencia.DOCUMENTO_NAO_LIDO, null, null));
            } else {
                CampoLido campo = campoLido(documento, codigo);
                campos.add(avaliar(campo, porCampo.get(chave(documento.documentoId(), codigo))));
            }
        }
        return new DocumentoAvaliado(documento.documentoId(), campos);
    }

    private static CampoAvaliado avaliar(CampoLido campo, DecisaoConferencia decisao) {
        boolean valida = decisao != null
                && SituacaoConferencia.aplicavel(campo.situacao(), decisao.tipo(), decisao.motivo());
        boolean divergente = valida && decisao.tipo() == TipoDecisao.DIVERGENTE;
        return switch (campo.situacao()) {
            case ILEGIVEL -> CampoAvaliado.semValor(campo.codigo(), SituacaoEvidencia.ILEGIVEL, null, null);
            case NAO_LOCALIZADO -> CampoAvaliado.semValor(campo.codigo(),
                    divergente ? SituacaoEvidencia.DIVERGENCIA_APONTADA : SituacaoEvidencia.NAO_LOCALIZADO, null, null);
            case IDENTIFICADO -> {
                if (!valida) {
                    yield CampoAvaliado.semValor(campo.codigo(), SituacaoEvidencia.NAO_CONFERIDO, campo.pagina(),
                            campo.trecho());
                }
                yield divergente
                        ? CampoAvaliado.semValor(campo.codigo(), SituacaoEvidencia.DIVERGENCIA_APONTADA,
                                campo.pagina(), campo.trecho())
                        : CampoAvaliado.confirmado(campo.codigo(), campo.valor(), campo.pagina(), campo.trecho());
            }
        };
    }

    /** O validador da leitura (RC-10) garante os seis campos em documento lido; a falta e erro de dados. */
    private static CampoLido campoLido(ExtracaoDocumento documento, CodigoCampo codigo) {
        for (CampoLido campo : documento.campos()) {
            if (campo.codigo() == codigo) {
                return campo;
            }
        }
        throw new IllegalStateException("Leitura sem o campo " + codigo + " no documento " + documento.documentoId());
    }

    private static String chave(long documentoId, CodigoCampo codigo) {
        return documentoId + "/" + codigo.name();
    }
}
