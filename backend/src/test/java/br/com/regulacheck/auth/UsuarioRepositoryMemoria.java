package br.com.regulacheck.auth;

import br.com.regulacheck.usuario.entity.Usuario;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Repositorio de usuarios em memoria para testes sem Oracle. */
public final class UsuarioRepositoryMemoria implements UsuarioRepository {

    private final Map<Long, Usuario> porId = new LinkedHashMap<>();

    /** @param usuario conta a disponibilizar */
    public synchronized void adicionar(Usuario usuario) {
        porId.put(usuario.id(), usuario);
    }

    /** Desativa a conta, como se alguem a tivesse bloqueado depois do login. */
    public synchronized void desativar(long id) {
        Usuario u = porId.get(id);
        porId.put(id, new Usuario(u.id(), u.email(), u.nome(), u.senhaHash(), u.perfil(), false));
    }

    @Override
    public synchronized Optional<Usuario> buscarPorEmail(String email) {
        return porId.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public synchronized Optional<Usuario> buscarPorId(long id) {
        return Optional.ofNullable(porId.get(id));
    }
}
