package br.com.regulacheck.auth.entity;

import java.time.Instant;

/**
 * Sessao de login persistida em {@code RC_SESSAO_LOGIN}. Nao confundir com a sessao de captura dos
 * sensores. O token entregue a paciente nao fica aqui nem no banco: guarda-se apenas seu SHA-256.
 *
 * @param id identificador interno
 * @param usuarioId conta dona da sessao
 * @param criadaEm instante do login (UTC)
 * @param expiraEm instante em que a sessao deixa de valer (UTC)
 * @param encerradaEm instante do logout; {@code null} enquanto a sessao nao foi encerrada
 */
public record Sessao(long id, long usuarioId, Instant criadaEm, Instant expiraEm, Instant encerradaEm) {

    /**
     * @param agora instante de referencia (relogio da aplicacao)
     * @return {@code true} se a sessao nao foi encerrada e ainda nao expirou
     */
    public boolean aberta(Instant agora) {
        return encerradaEm == null && agora.isBefore(expiraEm);
    }
}
