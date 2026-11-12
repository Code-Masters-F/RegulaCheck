package br.com.regulacheck.common.http;

import br.com.regulacheck.common.Json;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.IOException;
import java.io.InputStream;

/**
 * Leitura segura do corpo JSON de uma requisicao: exige {@code Content-Type: application/json},
 * limita o tamanho e traduz qualquer problema em erro padrao, sem reproduzir o conteudo recebido
 * (o corpo pode conter senha).
 */
public final class CorpoJson {

    /** Tamanho maximo aceito para o corpo, em bytes. */
    public static final int LIMITE_BYTES = 4096;

    private CorpoJson() {
    }

    /**
     * Le o corpo como o DTO informado.
     *
     * @param corpo fluxo de entrada da requisicao
     * @param contentType valor do cabecalho {@code Content-Type}; pode ser {@code null}
     * @param tipo classe do DTO de destino
     * @param <T> tipo do DTO
     * @return DTO preenchido
     * @throws ExcecaoNegocio 415 {@code TIPO_NAO_SUPORTADO} se o tipo de conteudo nao for JSON; 400
     *     {@code REQUISICAO_INVALIDA} se o corpo for vazio, grande demais ou ilegivel
     */
    public static <T> T ler(InputStream corpo, String contentType, Class<T> tipo) {
        if (!eJson(contentType)) {
            throw new ExcecaoNegocio(415, "TIPO_NAO_SUPORTADO", "Envie o corpo como application/json.");
        }
        byte[] bytes;
        try {
            // Le um byte alem do limite para detectar corpo grande sem carregar tudo na memoria.
            bytes = corpo.readNBytes(LIMITE_BYTES + 1);
        } catch (IOException falha) {
            throw invalido();
        }
        if (bytes.length > LIMITE_BYTES || new String(bytes, java.nio.charset.StandardCharsets.UTF_8).isBlank()) {
            throw invalido();
        }
        try {
            return Json.ler(bytes, tipo);
        } catch (IllegalArgumentException falha) {
            throw invalido();
        }
    }

    private static boolean eJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String tipo = contentType.split(";", 2)[0].trim();
        return tipo.equalsIgnoreCase("application/json");
    }

    private static ExcecaoNegocio invalido() {
        return new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "O corpo da requisição é inválido.");
    }
}
