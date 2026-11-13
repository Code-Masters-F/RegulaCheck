package br.com.regulacheck.integracao.ia;

/**
 * Texto produzido pelo resumo simulado (a IA e mockada no MVP), pronto para ser devolvido pela API.
 *
 * <p>{@code simulado} e sempre {@code true}: o resumo nunca e apresentado como integracao real. {@code formato}
 * declara que o texto deve ser exibido como <strong>texto puro</strong>, sem interpretar HTML; alem disso o
 * serializador JSON da aplicacao escapa {@code <}, {@code >}, {@code &} e {@code '}, de modo que a resposta
 * nunca carrega marcacao literal.
 *
 * @param texto conteudo do resumo; e dado, nunca marcacao
 * @param simulado sempre {@code true} enquanto a IA for mockada
 * @param formato sempre {@link #FORMATO_TEXTO_PURO}
 */
public record TextoSimulado(String texto, boolean simulado, String formato) {

    /** Valor de {@code formato}: o app deve exibir como texto, nao como HTML. */
    public static final String FORMATO_TEXTO_PURO = "texto_puro";

    /**
     * @param texto conteudo do resumo simulado
     * @return DTO identificado como simulado e em texto puro
     */
    public static TextoSimulado de(String texto) {
        return new TextoSimulado(texto, true, FORMATO_TEXTO_PURO);
    }
}
