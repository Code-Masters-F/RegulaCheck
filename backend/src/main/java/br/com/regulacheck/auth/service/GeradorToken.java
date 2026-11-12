package br.com.regulacheck.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Gera os tokens de sessao e calcula o hash que vai para o banco.
 *
 * <p>O token tem 256 bits aleatorios ({@link SecureRandom}) em Base64 URL sem preenchimento (43
 * caracteres). Por ter entropia alta, SHA-256 e suficiente para guarda-lo: diferente de senhas, nao
 * existe lista de candidatos a adivinhar, entao nao se usa hash lento. O banco guarda so o hash;
 * quem ler a tabela nao consegue usar as sessoes.
 */
public class GeradorToken {

    private static final int BYTES_ALEATORIOS = 32;
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final SecureRandom aleatorio = new SecureRandom();

    /** @return um token novo, imprevisivel */
    public String novo() {
        byte[] bytes = new byte[BYTES_ALEATORIOS];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calcula o hash guardado no banco.
     *
     * @param token token em texto
     * @return SHA-256 do token em hexadecimal minusculo (64 caracteres)
     */
    public String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e obrigatorio em toda JVM; chegar aqui indica instalacao corrompida.
            throw new IllegalStateException("SHA-256 indisponivel na JVM.", e);
        }
    }

    /**
     * Confere apenas o formato, para recusar de imediato (sem consultar o banco) o que nunca foi emitido.
     *
     * @param token valor recebido da cliente; pode ser {@code null}
     * @return {@code true} se tem o formato de um token emitido por {@link #novo()}
     */
    public boolean formatoValido(String token) {
        return token != null && FORMATO.matcher(token).matches();
    }
}
