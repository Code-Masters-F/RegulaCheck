package br.com.regulacheck.common.http;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acrescenta cabecalhos de seguranca a todas as respostas da API.
 *
 * <p>E um filtro <strong>apenas de cabecalho</strong>: nao decide acesso, nao altera status nem corpo (o 404
 * da rota coringa e os demais erros continuam como estavam). Impede que o navegador "adivinhe" o tipo do
 * conteudo ({@code nosniff}), que respostas da API sejam guardadas em cache, que sejam exibidas em quadros
 * ou carreguem recursos, e, quando a API e publicada por HTTPS, declara {@code Strict-Transport-Security}.
 */
public final class CabecalhosSegurosFiltro implements Filter {

    private static final String HSTS = "max-age=31536000";

    /** Cabecalhos que este filtro define (sem o HSTS, que depende de configuracao). */
    private static final List<String> NOMES = List.of("X-Content-Type-Options", "Cache-Control",
            "Content-Security-Policy", "Referrer-Policy", "Strict-Transport-Security");

    /**
     * Guarda os cabecalhos de seguranca ja definidos na resposta. Usado por quem precisa limpar a resposta
     * ({@code reset()}) para escrever um erro, sem perder esses cabecalhos.
     *
     * @param resposta resposta HTTP
     * @return pares nome-valor dos cabecalhos de seguranca presentes
     */
    public static Map<String, String> capturar(HttpServletResponse resposta) {
        Map<String, String> preservados = new LinkedHashMap<>();
        for (String nome : NOMES) {
            String valor = resposta.getHeader(nome);
            if (valor != null) {
                preservados.put(nome, valor);
            }
        }
        return preservados;
    }

    /**
     * Reaplica cabecalhos guardados por {@link #capturar}.
     *
     * @param resposta resposta HTTP
     * @param preservados cabecalhos a restaurar
     */
    public static void restaurar(HttpServletResponse resposta, Map<String, String> preservados) {
        preservados.forEach(resposta::setHeader);
    }

    private final boolean https;

    /**
     * @param https {@code true} quando a API e alcancada por HTTPS ({@code API_PUBLIC_URL}); habilita HSTS
     */
    public CabecalhosSegurosFiltro(boolean https) {
        this.https = https;
    }

    @Override
    public void doFilter(ServletRequest requisicao, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        if (resposta instanceof HttpServletResponse http) {
            http.setHeader("X-Content-Type-Options", "nosniff");
            http.setHeader("Cache-Control", "no-store");
            http.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
            http.setHeader("Referrer-Policy", "no-referrer");
            if (https) {
                http.setHeader("Strict-Transport-Security", HSTS);
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
