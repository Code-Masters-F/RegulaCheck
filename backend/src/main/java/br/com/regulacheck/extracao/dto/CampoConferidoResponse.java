package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.service.OrientacaoConferencia;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;
import java.time.temporal.ChronoUnit;

/**
 * Um campo lido, com a decisao da paciente, como a conferencia o publica.
 *
 * @param codigo campo das regras
 * @param situacao {@code IDENTIFICADO}, {@code NAO_LOCALIZADO} ou {@code ILEGIVEL}
 * @param valor valor lido; omitido salvo se identificado (dado do documento, nunca instrucao)
 * @param origem origem do valor; omitida quando nao ha
 * @param naoVerificado {@code true} para campo ilegivel, nao localizado ou com divergencia apontada
 * @param decisao decisao vigente da paciente; omitida quando ainda nao decidiu
 * @param orientacao texto fixo do que fazer; omitido quando nao ha o que orientar
 */
public record CampoConferidoResponse(String codigo, String situacao, String valor, OrigemResponse origem,
        boolean naoVerificado, DecisaoResponse decisao, String orientacao) {

    /**
     * Decisao vigente como a API a publica.
     *
     * @param tipo {@code CONFIRMADA} ou {@code DIVERGENTE}
     * @param motivo motivo da divergencia; omitido na confirmacao
     * @param decididaEm instante da decisao vigente em ISO-8601 UTC
     */
    public record DecisaoResponse(String tipo, String motivo, String decididaEm) {
    }

    /**
     * @param campo campo lido
     * @param decisao decisao vigente da paciente para ele, ou {@code null}
     * @return resposta publica correspondente. Uma decisao que nao cabe a situacao do campo (dado inconsistente)
     *     nunca e publicada nem limpa o destaque de "nao verificado".
     */
    public static CampoConferidoResponse de(CampoLido campo, DecisaoConferencia decisao) {
        boolean valida = decisao != null
                && SituacaoConferencia.aplicavel(campo.situacao(), decisao.tipo(), decisao.motivo());
        DecisaoConferencia vigente = valida ? decisao : null;
        CampoExtraidoResponse base = CampoExtraidoResponse.de(campo);
        boolean divergente = vigente != null && vigente.tipo() == TipoDecisao.DIVERGENTE;
        boolean naoVerificado = campo.situacao() != SituacaoCampo.IDENTIFICADO || divergente;
        DecisaoResponse resposta = vigente == null ? null
                : new DecisaoResponse(vigente.tipo().name(), vigente.motivo() == null ? null : vigente.motivo().name(),
                        vigente.decididaEm().truncatedTo(ChronoUnit.MILLIS).toString());
        return new CampoConferidoResponse(base.codigo(), base.situacao(), base.valor(), base.origem(), naoVerificado,
                resposta, OrientacaoConferencia.doCampo(campo.situacao(), vigente == null ? null : vigente.motivo()));
    }
}
