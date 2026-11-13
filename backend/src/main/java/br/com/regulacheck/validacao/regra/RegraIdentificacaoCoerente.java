package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Veredito;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RC-R02, identificacao coerente entre documentos. Compara nome (sem diferenciar acentos, maiusculas e espacos
 * extras) e data de nascimento <strong>confirmados</strong>:
 *
 * <ol>
 *   <li>valores confirmados diferentes entre documentos: pendente (divergencia comprovada, que nao depende de
 *       nenhum dado nao verificavel);</li>
 *   <li>documento nao lido, ou nome/data nao verificavel em documento lido: nao verificavel;</li>
 *   <li>menos de dois documentos com nome e data confirmados: nao verificavel (nao ha o que comparar);</li>
 *   <li>senao: atende.</li>
 * </ol>
 */
public final class RegraIdentificacaoCoerente implements RegraDocumental {

    /** Codigo da regra nas versoes do protocolo. */
    public static final String CODIGO = "RC-R02";

    /** A comparacao exige ao menos dois documentos legiveis com identificacao (protocolo, detalhe da RC-R02). */
    private static final int MINIMO_DE_DOCUMENTOS = 2;

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
        Set<String> nomes = new HashSet<>();
        Set<LocalDate> datas = new HashSet<>();
        List<Evidencia> nomesConfirmados = new ArrayList<>();
        List<Evidencia> datasConfirmadas = new ArrayList<>();
        List<Evidencia> comparaveis = new ArrayList<>();
        int documentosComparaveis = 0;
        MotivoNaoVerificavel motivo = null;
        List<Evidencia> naoVerificaveis = new ArrayList<>();
        for (DocumentoAvaliado documento : entrada.documentos()) {
            LeituraDosCampos.Valor<String> nome = LeituraDosCampos.texto(documento.campo(CodigoCampo.NOME));
            LeituraDosCampos.Valor<LocalDate> nascimento =
                    LeituraDosCampos.data(documento.campo(CodigoCampo.DATA_NASCIMENTO));
            if (nome.presente()) {
                nomes.add(LeituraDosCampos.nomeNormalizado(nome.valor()));
                nomesConfirmados.add(documento.evidencia(CodigoCampo.NOME));
            }
            if (nascimento.presente()) {
                datas.add(nascimento.valor());
                datasConfirmadas.add(documento.evidencia(CodigoCampo.DATA_NASCIMENTO));
            }
            if (nome.presente() && nascimento.presente()) {
                documentosComparaveis++;
                comparaveis.add(documento.evidencia(CodigoCampo.NOME));
                comparaveis.add(documento.evidencia(CodigoCampo.DATA_NASCIMENTO));
            }
            if (nome.naoVerificavel() || nascimento.naoVerificavel()) {
                motivo = motivo == null ? (nome.naoVerificavel() ? nome.motivo() : nascimento.motivo()) : motivo;
                naoVerificaveis.add(documento.evidencia(CodigoCampo.NOME));
                naoVerificaveis.add(documento.evidencia(CodigoCampo.DATA_NASCIMENTO));
            }
        }
        if (nomes.size() > 1 || datas.size() > 1) {
            List<Evidencia> divergentes = new ArrayList<>();
            if (nomes.size() > 1) {
                divergentes.addAll(nomesConfirmados);
            }
            if (datas.size() > 1) {
                divergentes.addAll(datasConfirmadas);
            }
            return Veredito.pendente(divergentes);
        }
        if (motivo != null) {
            return Veredito.naoVerificavel(motivo, naoVerificaveis);
        }
        if (documentosComparaveis < MINIMO_DE_DOCUMENTOS) {
            return Veredito.naoVerificavel(MotivoNaoVerificavel.DOCUMENTOS_INSUFICIENTES, comparaveis);
        }
        return Veredito.atende(comparaveis);
    }
}
