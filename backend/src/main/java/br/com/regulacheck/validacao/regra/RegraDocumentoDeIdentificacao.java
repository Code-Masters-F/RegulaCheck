package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Veredito;
import java.util.ArrayList;
import java.util.List;

/**
 * RC-R03, documento de identificacao legivel. Atende se algum documento foi confirmado como identificacao e traz
 * nome e data de nascimento confirmados. Sem isso: nao verificavel se ha documento de tipo nao verificavel ou
 * identificacao com nome/data nao verificavel; pendente se nao ha identificacao entre documentos de tipo conhecido
 * ou se a identificacao lida com clareza nao traz nome ou data.
 */
public final class RegraDocumentoDeIdentificacao implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R03";

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
        List<DocumentoAvaliado> identificacoes = LeituraDosCampos.doTipo(entrada, TipoDocumental.IDENTIFICACAO);
        for (DocumentoAvaliado documento : identificacoes) {
            if (LeituraDosCampos.texto(documento.campo(CodigoCampo.NOME)).presente()
                    && LeituraDosCampos.data(documento.campo(CodigoCampo.DATA_NASCIMENTO)).presente()) {
                return Veredito.atende(List.of(documento.evidencia(CodigoCampo.TIPO_DOCUMENTO),
                        documento.evidencia(CodigoCampo.NOME), documento.evidencia(CodigoCampo.DATA_NASCIMENTO)));
            }
        }
        MotivoNaoVerificavel motivo = null;
        List<Evidencia> naoVerificaveis = new ArrayList<>();
        for (DocumentoAvaliado documento : entrada.documentos()) {
            LeituraDosCampos.Valor<TipoDocumental> tipo = LeituraDosCampos.tipo(documento);
            if (tipo.naoVerificavel()) {
                motivo = motivo == null ? tipo.motivo() : motivo;
                naoVerificaveis.add(documento.evidencia(CodigoCampo.TIPO_DOCUMENTO));
            } else if (tipo.valor() == TipoDocumental.IDENTIFICACAO) {
                LeituraDosCampos.Valor<String> nome = LeituraDosCampos.texto(documento.campo(CodigoCampo.NOME));
                LeituraDosCampos.Valor<?> nascimento =
                        LeituraDosCampos.data(documento.campo(CodigoCampo.DATA_NASCIMENTO));
                if (nome.naoVerificavel() || nascimento.naoVerificavel()) {
                    motivo = motivo == null ? (nome.naoVerificavel() ? nome.motivo() : nascimento.motivo()) : motivo;
                    naoVerificaveis.add(documento.evidencia(CodigoCampo.NOME));
                    naoVerificaveis.add(documento.evidencia(CodigoCampo.DATA_NASCIMENTO));
                }
            }
        }
        if (motivo != null) {
            return Veredito.naoVerificavel(motivo, naoVerificaveis);
        }
        List<Evidencia> consideradas = new ArrayList<>(
                LeituraDosCampos.evidencias(entrada.documentos(), CodigoCampo.TIPO_DOCUMENTO));
        consideradas.addAll(LeituraDosCampos.evidencias(identificacoes, CodigoCampo.NOME));
        consideradas.addAll(LeituraDosCampos.evidencias(identificacoes, CodigoCampo.DATA_NASCIMENTO));
        return Veredito.pendente(consideradas);
    }
}
