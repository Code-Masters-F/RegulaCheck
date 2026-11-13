package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Veredito;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * RC-R05, encaminhamento dentro da validade demonstrativa de 180 dias, contados da data de emissao ate a data de
 * referencia (a "data do envio"). Considera todos os encaminhamentos confirmados: algum sem data de emissao (lido
 * com clareza), com mais de 180 dias ou com emissao posterior a data de referencia leva a pendente; data nao
 * verificavel ou nao interpretavel leva a nao verificavel; todos dentro da validade atendem. Sem encaminhamento
 * confirmado: nao verificavel, com o mesmo motivo da RC-R04.
 */
public final class RegraValidadeDoEncaminhamento implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R05";

    /** Validade demonstrativa do protocolo academico v0.1 (nao vem de norma oficial). */
    public static final long VALIDADE_EM_DIAS = 180;

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public NaturezaRegra natureza() {
        return NaturezaRegra.OBRIGATORIA;
    }

    @Override
    public Veredito avaliar(EntradaAvaliacao entrada) {
        List<DocumentoAvaliado> encaminhamentos = LeituraDosCampos.doTipo(entrada, TipoDocumental.ENCAMINHAMENTO);
        if (encaminhamentos.isEmpty()) {
            return Veredito.naoVerificavel(LeituraDosCampos.motivoSemDocumentoDoTipo(entrada),
                    LeituraDosCampos.evidencias(LeituraDosCampos.deTipoDesconhecido(entrada),
                            CodigoCampo.TIPO_DOCUMENTO));
        }
        List<Evidencia> foraDaValidade = new ArrayList<>();
        List<Evidencia> naoVerificaveis = new ArrayList<>();
        MotivoNaoVerificavel motivo = null;
        for (DocumentoAvaliado encaminhamento : encaminhamentos) {
            LeituraDosCampos.Valor<LocalDate> emissao =
                    LeituraDosCampos.data(encaminhamento.campo(CodigoCampo.DATA_EMISSAO));
            if (emissao.ausente() || (emissao.presente() && !dentroDaValidade(emissao.valor(),
                    entrada.dataReferencia()))) {
                foraDaValidade.add(encaminhamento.evidencia(CodigoCampo.DATA_EMISSAO));
            } else if (emissao.naoVerificavel()) {
                motivo = motivo == null ? emissao.motivo() : motivo;
                naoVerificaveis.add(encaminhamento.evidencia(CodigoCampo.DATA_EMISSAO));
            }
        }
        if (!foraDaValidade.isEmpty()) {
            return Veredito.pendente(foraDaValidade);
        }
        if (motivo != null) {
            return Veredito.naoVerificavel(motivo, naoVerificaveis);
        }
        return Veredito.atende(LeituraDosCampos.evidencias(encaminhamentos, CodigoCampo.DATA_EMISSAO));
    }

    /**
     * Emissao posterior a data de referencia nao e "valida por antecipacao": e um documento com data impossivel,
     * que precisa ser reemitido, entao conta como fora da validade.
     */
    private static boolean dentroDaValidade(LocalDate emissao, LocalDate referencia) {
        long dias = ChronoUnit.DAYS.between(emissao, referencia);
        return dias >= 0 && dias <= VALIDADE_EM_DIAS;
    }
}
