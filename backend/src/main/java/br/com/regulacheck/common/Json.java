package br.com.regulacheck.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

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
            .build();

    private Json() {
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
