package br.com.regulacheck.armazenamento;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Identificador interno de um arquivo guardado: 32 caracteres hexadecimais minusculos aleatorios.
 *
 * <p>E o unico texto que chega ao sistema de arquivos. Como o formato e fixo e validado no construtor,
 * {@code ..}, separadores de pasta, {@code :}, nomes reservados e caracteres estranhos nao tem como
 * entrar num caminho (protecao por construcao contra travessia de diretorio).
 *
 * @param valor 32 caracteres hexadecimais minusculos
 */
public record IdInterno(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[0-9a-f]{32}");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /**
     * @param valor identificador no formato esperado
     * @throws IllegalArgumentException se o texto nao tiver 32 caracteres hexadecimais minusculos; a
     *     mensagem nao reproduz o valor recebido
     */
    public IdInterno {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("Identificador interno de arquivo invalido.");
        }
    }

    /** @return novo identificador aleatorio */
    public static IdInterno novo() {
        byte[] bytes = new byte[16];
        ALEATORIO.nextBytes(bytes);
        return new IdInterno(HexFormat.of().formatHex(bytes));
    }

    /**
     * Interpreta um texto vindo de fora (por exemplo, do banco) como identificador.
     *
     * @param texto texto a validar
     * @return o identificador
     * @throws IllegalArgumentException se o formato for invalido
     */
    public static IdInterno de(String texto) {
        return new IdInterno(texto);
    }

    @Override
    public String toString() {
        return valor;
    }
}
