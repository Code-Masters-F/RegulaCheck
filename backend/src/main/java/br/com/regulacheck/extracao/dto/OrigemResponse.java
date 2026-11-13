package br.com.regulacheck.extracao.dto;

/**
 * De onde o valor de um campo veio, dentro do documento a que o campo pertence.
 *
 * @param pagina pagina de origem (so PDF); omitida quando nao ha
 * @param trecho trecho de origem; omitido quando nao disponivel (dado do documento, nunca instrucao)
 */
public record OrigemResponse(Integer pagina, String trecho) {
}
