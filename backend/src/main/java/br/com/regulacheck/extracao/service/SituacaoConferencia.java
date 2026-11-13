package br.com.regulacheck.extracao.service;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Situacao da conferencia de uma leitura, calculada (nunca gravada) a partir da leitura e das decisoes vigentes da
 * paciente. Regra pura: sem HTTP, sem JDBC e sem relogio.
 *
 * <p>Precedencia: {@link #DESATUALIZADA} > {@link #REQUER_CONFERENCIA} > {@link #PENDENTE} > {@link #CONFERIDA}.
 * {@code CONFERIDA} so diz que a paciente conferiu a leitura: <strong>nao libera submissao</strong> e nao e
 * resultado de regra, pendencia nem aprovacao (isso e das historias de analise).
 */
public enum SituacaoConferencia {

    /** Ha campo identificado sem decisao da paciente. */
    PENDENTE,

    /**
     * Algum documento nao foi lido, algum campo de documento lido esta ilegivel ou a paciente apontou
     * divergencia: a leitura nao pode ser tomada como verificada.
     */
    REQUER_CONFERENCIA,

    /** Todo campo identificado foi confirmado e nada impede de tomar a leitura como verificada. */
    CONFERIDA,

    /** A leitura nao e a da versao atual dos documentos: so consulta, e preciso ler de novo. */
    DESATUALIZADA;

    /**
     * Resultado de {@link #avaliar}.
     *
     * @param situacao situacao da conferencia
     * @param camposNaoVerificados campos ilegiveis, nao localizados ou com divergencia apontada, em documentos lidos
     * @param camposSemDecisao campos identificados que a paciente ainda nao confirmou nem contestou
     * @param divergencias decisoes vigentes de divergencia
     */
    public record Avaliacao(SituacaoConferencia situacao, int camposNaoVerificados, int camposSemDecisao,
            int divergencias) {
    }

    /**
     * Calcula a situacao e as contagens. Uma decisao so conta se o campo existe na leitura <em>e</em> se ela e
     * aplicavel a situacao dele (ver {@link #aplicavel}); uma decisao fora disso, vinda de um dado inconsistente,
     * nunca "limpa" um campo que a leitura marcou como nao verificado.
     *
     * @param leitura leitura (uma por versao dos documentos)
     * @param atual {@code true} se a leitura e a da versao atual dos documentos
     * @param decisoes decisoes vigentes da paciente para a leitura
     * @return a avaliacao; nunca {@code null}
     */
    public static Avaliacao avaliar(Extracao leitura, boolean atual, List<DecisaoConferencia> decisoes) {
        Map<String, DecisaoConferencia> porCampo = new HashMap<>();
        for (DecisaoConferencia decisao : decisoes) {
            porCampo.put(chave(decisao.documentoId(), decisao.codigo()), decisao);
        }
        boolean documentoNaoLido = false;
        boolean ilegivel = false;
        boolean divergencia = false;
        int naoVerificados = 0;
        int semDecisao = 0;
        int divergencias = 0;
        for (ExtracaoDocumento documento : leitura.documentos()) {
            if (documento.situacao() != SituacaoLeitura.LIDO) {
                documentoNaoLido = true;
                continue;
            }
            for (CampoLido campo : documento.campos()) {
                DecisaoConferencia decisao = porCampo.get(chave(documento.documentoId(), campo.codigo()));
                boolean valida = decisao != null
                        && aplicavel(campo.situacao(), decisao.tipo(), decisao.motivo());
                boolean divergente = valida && decisao.tipo() == TipoDecisao.DIVERGENTE;
                if (divergente) {
                    divergencia = true;
                    divergencias++;
                }
                switch (campo.situacao()) {
                    case ILEGIVEL -> {
                        ilegivel = true;
                        naoVerificados++;
                    }
                    case NAO_LOCALIZADO -> naoVerificados++;
                    case IDENTIFICADO -> {
                        if (!valida) {
                            semDecisao++;
                        } else if (divergente) {
                            naoVerificados++;
                        }
                    }
                }
            }
        }
        SituacaoConferencia situacao;
        if (!atual) {
            situacao = DESATUALIZADA;
        } else if (documentoNaoLido || ilegivel || divergencia) {
            situacao = REQUER_CONFERENCIA;
        } else if (semDecisao > 0) {
            situacao = PENDENTE;
        } else {
            situacao = CONFERIDA;
        }
        return new Avaliacao(situacao, naoVerificados, semDecisao, divergencias);
    }

    /**
     * Diz se uma decisao cabe a situacao do campo. Identificado: confirmar, ou divergir por valor diferente.
     * Nao localizado: so divergir por "o campo existe e nao foi lido". Ilegivel: nenhuma decisao (resolve-se com
     * nova leitura). Confirmar nunca leva motivo.
     *
     * @param situacao situacao do campo na leitura
     * @param tipo decisao
     * @param motivo motivo da divergencia; {@code null} em confirmacao
     * @return {@code true} se a decisao e aplicavel
     */
    public static boolean aplicavel(SituacaoCampo situacao, TipoDecisao tipo, MotivoDivergencia motivo) {
        return switch (situacao) {
            case IDENTIFICADO -> tipo == TipoDecisao.CONFIRMADA ? motivo == null
                    : motivo == MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO;
            case NAO_LOCALIZADO -> tipo == TipoDecisao.DIVERGENTE
                    && motivo == MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO;
            case ILEGIVEL -> false;
        };
    }

    private static String chave(long documentoId, CodigoCampo codigo) {
        return documentoId + "/" + codigo.name();
    }
}
