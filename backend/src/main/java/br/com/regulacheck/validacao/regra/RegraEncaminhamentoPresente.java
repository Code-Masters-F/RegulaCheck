package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Veredito;
import java.util.List;

/**
 * RC-R01, encaminhamento presente. Atende se algum documento foi lido e confirmado como encaminhamento. Sem ele,
 * fica nao verificavel se algum documento tem tipo nao verificavel (pode ser o encaminhamento que falta) e pendente
 * so quando todos os documentos tem tipo conhecido e nenhum e encaminhamento (inclui pedido sem documentos).
 */
public final class RegraEncaminhamentoPresente implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R01";

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
        if (!encaminhamentos.isEmpty()) {
            return Veredito.atende(LeituraDosCampos.evidencias(encaminhamentos, CodigoCampo.TIPO_DOCUMENTO));
        }
        List<DocumentoAvaliado> desconhecidos = LeituraDosCampos.deTipoDesconhecido(entrada);
        if (!desconhecidos.isEmpty()) {
            return Veredito.naoVerificavel(LeituraDosCampos.tipo(desconhecidos.get(0)).motivo(),
                    LeituraDosCampos.evidencias(desconhecidos, CodigoCampo.TIPO_DOCUMENTO));
        }
        return Veredito.pendente(LeituraDosCampos.evidencias(entrada.documentos(), CodigoCampo.TIPO_DOCUMENTO));
    }
}
