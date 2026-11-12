package br.com.regulacheck.usuario.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.usuario.entity.Perfil;
import br.com.regulacheck.usuario.entity.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Leitura de {@code RC_USUARIO} por JDBC, sempre com SQL parametrizado. */
public final class UsuarioRepositoryJdbc implements UsuarioRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioRepositoryJdbc.class);

    private static final String SELECT_BASE =
            "SELECT ID, EMAIL, NOME, SENHA_HASH, PERFIL, ATIVO FROM RC_USUARIO WHERE ";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public UsuarioRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return buscar(SELECT_BASE + "EMAIL = ?", email);
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return buscar(SELECT_BASE + "ID = ?", id);
    }

    private Optional<Usuario> buscar(String sql, Object parametro) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setObject(1, parametro);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                        resultado.getLong("ID"),
                        resultado.getString("EMAIL"),
                        resultado.getString("NOME"),
                        resultado.getString("SENHA_HASH"),
                        Perfil.valueOf(resultado.getString("PERFIL")),
                        resultado.getInt("ATIVO") == 1));
            }
        } catch (SQLException falha) {
            // Detalhe do driver so no log (sem parametros); quem chamou recebe 503 generico.
            LOG.warn("Falha ao consultar usuario. tipo={} codigoFornecedor={}",
                    falha.getClass().getSimpleName(), falha.getErrorCode());
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
    }
}
