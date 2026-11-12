package br.com.regulacheck.auth.autorizacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Chave do simulador de sensores ({@code IOT_API_KEY}), mantida so como resumo SHA-256.
 *
 * <p>A comparacao e em tempo constante: os dois valores sao reduzidos a resumos do mesmo tamanho e
 * comparados com {@link MessageDigest#isEqual}, de modo que o tempo de resposta nao revela quantos
 * caracteres iniciais estavam certos nem o tamanho da chave. Sem chave configurada, nenhuma chave
 * apresentada e aceita.
 */
public final class ChaveSimulador {

    /** Nome do cabecalho HTTP que carrega a chave. */
    public static final String CABECALHO = "X-Chave-Simulador";

    private final byte[] resumo;

    /**
     * @param chave valor configurado; {@code null} ou vazio significa "sem chave" (nada e aceito)
     */
    public ChaveSimulador(String chave) {
        this.resumo = chave == null || chave.isBlank() ? null : sha256(chave);
    }

    /**
     * @param apresentada valor recebido no cabecalho; pode ser {@code null}
     * @return {@code true} somente se ha chave configurada e o valor e igual a ela
     */
    public boolean confere(String apresentada) {
        if (resumo == null || apresentada == null) {
            return false;
        }
        return MessageDigest.isEqual(resumo, sha256(apresentada));
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM.", e);
        }
    }

    /** Nunca mostra a chave nem o resumo. */
    @Override
    public String toString() {
        return "ChaveSimulador[" + (resumo == null ? "ausente" : "informada") + "]";
    }
}
