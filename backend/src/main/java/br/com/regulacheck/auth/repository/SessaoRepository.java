package br.com.regulacheck.auth.repository;

import br.com.regulacheck.auth.entity.Sessao;
import java.time.Instant;
import java.util.Optional;

/**
 * Persistencia das sessoes de login. Interface por ser a fronteira com o banco (testes sem Oracle).
 * Todas as operacoes recebem somente o <em>hash</em> do token, nunca o token.
 */
public interface SessaoRepository {

    /**
     * Grava uma nova sessao.
     *
     * @param usuarioId conta dona da sessao
     * @param tokenHash SHA-256 do token, em hexadecimal minusculo (64 caracteres)
     * @param criadaEm instante do login
     * @param expiraEm instante de expiracao, posterior a {@code criadaEm}
     * @return a sessao gravada
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Sessao criar(long usuarioId, String tokenHash, Instant criadaEm, Instant expiraEm);

    /**
     * Busca uma sessao pelo hash do token, aberta ou nao.
     *
     * @param tokenHash SHA-256 do token em hexadecimal minusculo
     * @return a sessao, ou vazio se desconhecida
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Sessao> buscarPorTokenHash(String tokenHash);

    /**
     * Marca a sessao como encerrada. Nao altera sessao ja encerrada nem outras sessoes da conta.
     *
     * @param sessaoId identificador da sessao
     * @param quando instante do encerramento
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    void encerrar(long sessaoId, Instant quando);
}
