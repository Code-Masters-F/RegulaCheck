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
 * RC-R04, motivo do encaminhamento informado. Olha o campo de motivo de todos os documentos confirmados como
 * encaminhamento: algum lido com clareza sem o motivo leva a pendente (a unidade emissora completa); motivo nao
 * verificavel leva a nao verificavel; todos com motivo confirmado atendem. O conteudo do motivo nao e avaliado
 * (as regras nao julgam conteudo clinico, e texto do documento e so dado). Sem encaminhamento confirmado, a regra
 * fica nao verificavel: com o motivo do documento de tipo nao verificavel, se houver (ele pode ser o
 * encaminhamento), ou {@code DEPENDE_DE_DOCUMENTO_AUSENTE}. Nunca pendente nesse caso: a falta do encaminhamento e
 * cobrada pela RC-R01, e pedir a unidade que "complete o documento" seria orientacao errada.
 */
public final class RegraMotivoInformado implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R04";

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
        List<Evidencia> ausentes = new ArrayList<>();
        List<Evidencia> naoVerificaveis = new ArrayList<>();
        MotivoNaoVerificavel motivo = null;
        for (DocumentoAvaliado encaminhamento : encaminhamentos) {
            LeituraDosCampos.Valor<String> valor =
                    LeituraDosCampos.texto(encaminhamento.campo(CodigoCampo.MOTIVO_ENCAMINHAMENTO));
            if (valor.ausente()) {
                ausentes.add(encaminhamento.evidencia(CodigoCampo.MOTIVO_ENCAMINHAMENTO));
            } else if (valor.naoVerificavel()) {
                motivo = motivo == null ? valor.motivo() : motivo;
                naoVerificaveis.add(encaminhamento.evidencia(CodigoCampo.MOTIVO_ENCAMINHAMENTO));
            }
        }
        if (!ausentes.isEmpty()) {
            return Veredito.pendente(ausentes);
        }
        if (motivo != null) {
            return Veredito.naoVerificavel(motivo, naoVerificaveis);
        }
        return Veredito.atende(LeituraDosCampos.evidencias(encaminhamentos, CodigoCampo.MOTIVO_ENCAMINHAMENTO));
    }
}
