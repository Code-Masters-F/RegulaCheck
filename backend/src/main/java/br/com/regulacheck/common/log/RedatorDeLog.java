package br.com.regulacheck.common.log;

import java.util.regex.Pattern;

/**
 * Mascara segredos em textos e falhas antes de irem para o log.
 *
 * <p>Mensagens de excecao de bibliotecas (driver do banco, pool, servidor) podem conter a URL de
 * conexao com usuario e senha, tokens ou valores de configuracao. Este redator troca por {@code ***} os
 * padroes conhecidos. E a ultima barreira: o codigo do projeto ja evita colocar segredo em mensagens, e
 * as respostas HTTP nunca incluem mensagem de excecao.
 */
public final class RedatorDeLog {

    private static final String MASCARA = "***";

    private static final Pattern PAR_SECRETO = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|senha|secret|token|api[_-]?key|chave|authorization)(\\s*[=:]\\s*)"
                    + "(\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;&)\\]}]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern JDBC_USUARIO_SENHA = Pattern.compile("(?i)(jdbc:[a-z]+:[a-z]+:)[^/@\\s]+/[^@\\s]+@");
    private static final Pattern USUARIO_NA_URL = Pattern.compile("(?i)(://)[^/@\\s:]+:[^/@\\s]+@");
    private static final Pattern TOKEN_CONHECIDO = Pattern.compile(
            "\\b(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{12,}|sk-[A-Za-z0-9]{20,})");

    private RedatorDeLog() {
    }

    /**
     * @param texto texto que sera registrado; pode ser {@code null}
     * @return o texto com segredos conhecidos mascarados ({@code null} vira texto vazio)
     */
    public static String redigir(String texto) {
        if (texto == null) {
            return "";
        }
        String saida = JDBC_USUARIO_SENHA.matcher(texto).replaceAll("$1" + MASCARA + "/" + MASCARA + "@");
        saida = USUARIO_NA_URL.matcher(saida).replaceAll("$1" + MASCARA + "@");
        saida = BEARER.matcher(saida).replaceAll("Bearer " + MASCARA);
        saida = PAR_SECRETO.matcher(saida).replaceAll("$1$2" + MASCARA);
        return TOKEN_CONHECIDO.matcher(saida).replaceAll(MASCARA);
    }

    /**
     * Copia uma falha trocando as mensagens (dela e das causas) pela versao redigida e preservando a pilha,
     * para que o log mostre onde ocorreu sem expor segredos da mensagem original.
     *
     * @param falha falha original; pode ser {@code null}
     * @return copia redigida, ou {@code null} se {@code falha} for {@code null}
     */
    public static Throwable redigir(Throwable falha) {
        if (falha == null) {
            return null;
        }
        FalhaRedigida copia = new FalhaRedigida(falha.getClass().getName(), redigir(falha.getMessage()),
                redigir(falha.getCause()));
        copia.setStackTrace(falha.getStackTrace());
        return copia;
    }

    /** Falha com a mensagem ja redigida, no lugar da original. */
    private static final class FalhaRedigida extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String classeOriginal;

        FalhaRedigida(String classeOriginal, String mensagem, Throwable causa) {
            super(mensagem, causa);
            this.classeOriginal = classeOriginal;
        }

        @Override
        public String toString() {
            String mensagem = getLocalizedMessage();
            return mensagem == null || mensagem.isEmpty() ? classeOriginal : classeOriginal + ": " + mensagem;
        }
    }
}
