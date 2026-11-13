package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.model.CampoAvaliado;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Interpretacao dos campos usada pelas regras. Segue as convencoes da leitura simulada (datas {@code AAAA-MM-DD},
 * tipo com os nomes de {@link TipoDocumental}, autorizacao {@code SIM}/{@code NAO}); qualquer outro formato e
 * "nao interpretavel": o sistema nunca adivinha um valor.
 */
public final class LeituraDosCampos {

    private LeituraDosCampos() {
    }

    /**
     * O que se sabe de um campo depois de interpretado: um valor, ausente no documento lido, ou nao verificavel
     * (com o motivo). Exatamente um dos tres.
     *
     * @param valor valor interpretado; {@code null} se ausente ou nao verificavel
     * @param motivo motivo de nao verificavel; {@code null} se ha valor ou se ausente
     * @param <T> tipo do valor
     */
    public record Valor<T>(T valor, MotivoNaoVerificavel motivo) {

        /** @return {@code true} se ha valor interpretado (campo confirmado e no formato esperado) */
        public boolean presente() {
            return valor != null;
        }

        /** @return {@code true} se o campo nao aparece no documento lido com clareza */
        public boolean ausente() {
            return valor == null && motivo == null;
        }

        /** @return {@code true} se o campo nao pode ser usado para decidir */
        public boolean naoVerificavel() {
            return motivo != null;
        }

        static <T> Valor<T> de(T valor) {
            return new Valor<>(valor, null);
        }

        static <T> Valor<T> semValor() {
            return new Valor<>(null, null);
        }

        static <T> Valor<T> naoVerificavel(MotivoNaoVerificavel motivo) {
            return new Valor<>(null, motivo);
        }
    }

    /**
     * Valor de texto: confirmado e nao vazio. O conteudo nao e avaliado (as regras nao julgam conteudo clinico).
     *
     * @param campo campo avaliado
     * @return o texto, ausente ou nao verificavel
     */
    public static Valor<String> texto(CampoAvaliado campo) {
        return interpretar(campo, valor -> valor.isBlank() ? null : valor);
    }

    /**
     * Data no formato {@code AAAA-MM-DD}, conferida de forma estrita (sem 30 de fevereiro).
     *
     * @param campo campo avaliado
     * @return a data, ausente ou nao verificavel
     */
    public static Valor<LocalDate> data(CampoAvaliado campo) {
        return interpretar(campo, valor -> {
            try {
                return LocalDate.parse(valor.strip());
            } catch (DateTimeParseException formatoInesperado) {
                return null;
            }
        });
    }

    /**
     * Sim ou nao ({@code SIM}/{@code NAO}, sem diferenciar maiusculas).
     *
     * @param campo campo avaliado
     * @return {@code true}/{@code false}, ausente ou nao verificavel
     */
    public static Valor<Boolean> simNao(CampoAvaliado campo) {
        return interpretar(campo, valor -> switch (valor.strip().toUpperCase(Locale.ROOT)) {
            case "SIM" -> Boolean.TRUE;
            case "NAO" -> Boolean.FALSE;
            default -> null;
        });
    }

    /**
     * Tipo do documento <strong>lido e confirmado</strong>; o tipo escolhido pela paciente no envio nunca e usado.
     * Documento lido sem tipo identificado ({@code NAO_LOCALIZADO}) conta como {@link TipoDocumental#OUTRO}: nao e
     * nenhum dos tipos exigidos.
     *
     * @param documento documento avaliado
     * @return o tipo ou nao verificavel (nunca ausente)
     */
    public static Valor<TipoDocumental> tipo(DocumentoAvaliado documento) {
        CampoAvaliado campo = documento.campo(CodigoCampo.TIPO_DOCUMENTO);
        if (campo.situacao() == SituacaoEvidencia.NAO_LOCALIZADO) {
            return Valor.de(TipoDocumental.OUTRO);
        }
        return interpretar(campo, valor -> {
            try {
                return TipoDocumental.valueOf(valor.strip());
            } catch (IllegalArgumentException tipoDesconhecido) {
                return null;
            }
        });
    }

    /**
     * Nome para comparacao: sem acentos, sem diferenca de maiusculas e com espacos extras removidos. Letras
     * diferentes continuam diferentes.
     *
     * @param nome nome confirmado
     * @return nome normalizado
     */
    public static String nomeNormalizado(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    /**
     * @param entrada entrada da avaliacao
     * @param tipo tipo desejado
     * @return documentos cujo tipo lido e confirmado e o desejado, na ordem da entrada
     */
    public static List<DocumentoAvaliado> doTipo(EntradaAvaliacao entrada, TipoDocumental tipo) {
        List<DocumentoAvaliado> lista = new ArrayList<>();
        for (DocumentoAvaliado documento : entrada.documentos()) {
            if (tipo(documento).valor() == tipo) {
                lista.add(documento);
            }
        }
        return lista;
    }

    /**
     * @param entrada entrada da avaliacao
     * @return documentos cujo tipo nao pode ser verificado (nao lidos, tipo ilegivel, nao conferido, contestado ou
     *     nao interpretavel), na ordem da entrada
     */
    public static List<DocumentoAvaliado> deTipoDesconhecido(EntradaAvaliacao entrada) {
        List<DocumentoAvaliado> lista = new ArrayList<>();
        for (DocumentoAvaliado documento : entrada.documentos()) {
            if (tipo(documento).naoVerificavel()) {
                lista.add(documento);
            }
        }
        return lista;
    }

    /**
     * Motivo de "nao verificavel" de uma regra que depende de um documento de certo tipo quando nenhum documento
     * confirmado tem esse tipo: se algum documento tem tipo nao verificavel, ele pode ser o que falta, e o motivo e
     * o dele (ex.: transcricao nao conferida); se todos os tipos sao conhecidos, o documento falta de fato.
     *
     * @param entrada entrada da avaliacao
     * @return o motivo do primeiro documento de tipo nao verificavel, ou {@code DEPENDE_DE_DOCUMENTO_AUSENTE}
     */
    public static MotivoNaoVerificavel motivoSemDocumentoDoTipo(EntradaAvaliacao entrada) {
        List<DocumentoAvaliado> desconhecidos = deTipoDesconhecido(entrada);
        return desconhecidos.isEmpty() ? MotivoNaoVerificavel.DEPENDE_DE_DOCUMENTO_AUSENTE
                : tipo(desconhecidos.get(0)).motivo();
    }

    /**
     * @param documentos documentos
     * @param codigo campo
     * @return a evidencia do campo em cada documento, na mesma ordem
     */
    public static List<Evidencia> evidencias(List<DocumentoAvaliado> documentos, CodigoCampo codigo) {
        List<Evidencia> lista = new ArrayList<>();
        for (DocumentoAvaliado documento : documentos) {
            lista.add(documento.evidencia(codigo));
        }
        return lista;
    }

    /**
     * Interpretacao comum: confirmado -> valor (ou nao interpretavel); nao localizado -> ausente; demais situacoes ->
     * nao verificavel com o motivo da situacao.
     */
    private static <T> Valor<T> interpretar(CampoAvaliado campo, Function<String, T> conversor) {
        return switch (campo.situacao()) {
            case CONFIRMADO -> {
                T valor = conversor.apply(campo.valor());
                yield valor == null ? Valor.naoVerificavel(MotivoNaoVerificavel.VALOR_NAO_INTERPRETAVEL)
                        : Valor.de(valor);
            }
            case NAO_LOCALIZADO -> Valor.semValor();
            case ILEGIVEL, NAO_CONFERIDO, DIVERGENCIA_APONTADA, DOCUMENTO_NAO_LIDO ->
                    Valor.naoVerificavel(campo.situacao().motivo());
        };
    }
}
