package br.com.regulacheck.auth.dto;

/**
 * Dados basicos da conta autenticada, os unicos que a resposta publica mostra: nunca hash de senha nem
 * identificador interno.
 *
 * @param nome nome de exibicao
 * @param perfil {@code PACIENTE} ou {@code ADMINISTRADOR}
 */
public record UsuarioResumo(String nome, String perfil) {
}
