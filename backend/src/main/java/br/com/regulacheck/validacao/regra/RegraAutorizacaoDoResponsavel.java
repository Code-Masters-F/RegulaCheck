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
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RC-R06, autorizacao do responsavel legal (condicional: paciente com menos de 18 anos na data de referencia).
 *
 * <p><strong>Condicao:</strong> a data de nascimento vem <em>so</em> do documento de identificacao (protocolo,
 * detalhe da RC-R06). Se ela nao pode ser determinada de forma unica (sem identificacao confirmada, data ausente,
 * nao verificavel, posterior a data de referencia ou diferente entre identificacoes), a condicao e desconhecida e
 * o resultado e nao verificavel ({@code CONDICAO_INDETERMINADA}): a idade nunca e presumida, nem a partir de outro
 * documento. Com 18 anos ou mais (inclusive no dia do aniversario), a regra nao se aplica.
 *
 * <p><strong>Evidencia (menor de 18):</strong> atende se algum documento traz autorizacao assinada confirmada como
 * {@code SIM}; sem isso, nao verificavel se algum documento nao foi lido ou tem a autorizacao nao verificavel;
 * senao, pendente.
 */
public final class RegraAutorizacaoDoResponsavel implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R06";

    /** Idade a partir da qual a autorizacao do responsavel deixa de ser exigida. */
    public static final int MAIORIDADE = 18;

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public NaturezaRegra natureza() {
        return NaturezaRegra.CONDICIONAL;
    }

    @Override
    public Veredito avaliar(EntradaAvaliacao entrada) {
        List<DocumentoAvaliado> identificacoes = LeituraDosCampos.doTipo(entrada, TipoDocumental.IDENTIFICACAO);
        List<Evidencia> evidenciasDaCondicao = identificacoes.isEmpty()
                ? LeituraDosCampos.evidencias(LeituraDosCampos.deTipoDesconhecido(entrada), CodigoCampo.TIPO_DOCUMENTO)
                : LeituraDosCampos.evidencias(identificacoes, CodigoCampo.DATA_NASCIMENTO);
        LocalDate nascimento = nascimentoUnico(identificacoes);
        if (nascimento == null || nascimento.isAfter(entrada.dataReferencia())) {
            return Veredito.naoVerificavel(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, evidenciasDaCondicao);
        }
        if (Period.between(nascimento, entrada.dataReferencia()).getYears() >= MAIORIDADE) {
            return Veredito.naoAplicavel(evidenciasDaCondicao);
        }
        List<Evidencia> autorizacoes = new ArrayList<>(evidenciasDaCondicao);
        MotivoNaoVerificavel motivo = null;
        for (DocumentoAvaliado documento : entrada.documentos()) {
            LeituraDosCampos.Valor<Boolean> assinada =
                    LeituraDosCampos.simNao(documento.campo(CodigoCampo.AUTORIZACAO_ASSINADA));
            if (Boolean.TRUE.equals(assinada.valor())) {
                return Veredito.atende(List.of(evidenciasDaCondicao.get(0),
                        documento.evidencia(CodigoCampo.AUTORIZACAO_ASSINADA)));
            }
            if (assinada.naoVerificavel()) {
                motivo = motivo == null ? assinada.motivo() : motivo;
            }
            autorizacoes.add(documento.evidencia(CodigoCampo.AUTORIZACAO_ASSINADA));
        }
        if (motivo != null) {
            return Veredito.naoVerificavel(motivo, autorizacoes);
        }
        return Veredito.pendente(autorizacoes);
    }

    /**
     * @return a data de nascimento confirmada, igual em todas as identificacoes; {@code null} se nao ha
     *     identificacao, se alguma nao traz a data confirmada e interpretavel, ou se as datas diferem
     */
    private static LocalDate nascimentoUnico(List<DocumentoAvaliado> identificacoes) {
        Set<LocalDate> datas = new HashSet<>();
        for (DocumentoAvaliado identificacao : identificacoes) {
            LeituraDosCampos.Valor<LocalDate> data =
                    LeituraDosCampos.data(identificacao.campo(CodigoCampo.DATA_NASCIMENTO));
            if (!data.presente()) {
                return null;
            }
            datas.add(data.valor());
        }
        return datas.size() == 1 ? datas.iterator().next() : null;
    }
}
