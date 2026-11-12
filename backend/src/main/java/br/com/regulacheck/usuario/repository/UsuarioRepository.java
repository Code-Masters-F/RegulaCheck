package br.com.regulacheck.usuario.repository;

import br.com.regulacheck.usuario.entity.Usuario;
import java.util.Optional;

/**
 * Consulta de contas. E uma interface por ser a fronteira com o banco, o que permite testar os
 * servicos sem Oracle.
 */
public interface UsuarioRepository {

    /**
     * Busca uma conta pelo e-mail.
     *
     * @param email e-mail ja normalizado (aparado e em minusculas)
     * @return a conta, ou vazio se nao existir
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Busca uma conta pelo identificador interno.
     *
     * @param id identificador da conta
     * @return a conta, ou vazio se nao existir
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Usuario> buscarPorId(long id);
}
