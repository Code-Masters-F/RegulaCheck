package br.com.regulacheck.auth.service;

import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Instant;

/**
 * Resultado de uma validacao de sessao: quem e a conta autenticada. E o que as proximas historias
 * leem para identificar a paciente (nunca por identificador enviado no corpo da requisicao).
 *
 * @param sessaoId identificador interno da sessao
 * @param usuarioId conta autenticada
 * @param nome nome de exibicao da conta
 * @param perfil perfil da conta (as regras por perfil sao da RC-05)
 * @param expiraEm instante em que a sessao deixa de valer
 */
public record SessaoAutenticada(long sessaoId, long usuarioId, String nome, Perfil perfil, Instant expiraEm) {
}
