package br.com.regulacheck.saude;

/**
 * Corpo da resposta de {@code GET /health}.
 *
 * @param status estado tecnico; hoje somente {@code "UP"}
 */
public record SaudeResposta(String status) {
}
