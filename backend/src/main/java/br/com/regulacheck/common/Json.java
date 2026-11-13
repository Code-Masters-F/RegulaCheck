package br.com.regulacheck.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;

/**
 * Acesso unico ao {@link ObjectMapper} da aplicacao.
 *
 * <p>O mapper e thread-safe depois de configurado; so DTOs explicitos devem ser serializados
 * (nunca entidades), e campos nulos ficam fora do JSON.
 */
public final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            // Aviso de depreciacao conhecido do Jackson 2.23: a opcao funciona e e coberta pelos testes.
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            // Entrada: campos desconhecidos sao ignorados, mas lixo depois do JSON e recusado.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    static {
        // Texto puro: a saida nunca carrega marcacao literal (<, >, &, '), mesmo que um cliente trate a
        // resposta como pagina. O cliente JSON decodifica < de volta para o texto original.
        MAPPER.getFactory().setCharacterEscapes(new EscapeDeMarcacao());
    }

    private Json() {
    }

    /** Escapa os caracteres usados em marcacao HTML nas respostas JSON. */
    private static final class EscapeDeMarcacao extends CharacterEscapes {
        private static final long serialVersionUID = 1L;
        private final int[] escapes;

        EscapeDeMarcacao() {
            escapes = CharacterEscapes.standardAsciiEscapesForJSON();
            for (char c : new char[] {'<', '>', '&', '\''}) {
                escapes[c] = CharacterEscapes.ESCAPE_STANDARD;
            }
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return null;
        }
    }

    /**
     * Le um JSON de entrada para um DTO.
     *
     * @param bytes conteudo JSON em UTF-8
     * @param tipo classe do DTO de destino
     * @param <T> tipo do DTO
     * @return DTO preenchido (nunca {@code null})
     * @throws IllegalArgumentException se o JSON for ilegivel, nao corresponder ao tipo ou for
     *     {@code null}; a mensagem nao reproduz o conteudo recebido
     */
    public static <T> T ler(byte[] bytes, Class<T> tipo) {
        try {
            T valor = MAPPER.readValue(bytes, tipo);
            if (valor == null) {
                throw new IllegalArgumentException("JSON sem conteudo.");
            }
            return valor;
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON ilegivel.");
        }
    }

    /**
     * Serializa um DTO para JSON.
     *
     * @param valor objeto a serializar
     * @return texto JSON
     * @throws IllegalStateException se a serializacao falhar (erro de programacao, nao de entrada)
     */
    public static String escrever(Object valor) {
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar resposta JSON.", e);
        }
    }
}
