package br.com.regulacheck.auth;

import br.com.regulacheck.auth.entity.Sessao;
import br.com.regulacheck.auth.repository.SessaoRepository;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio de sessoes em memoria para testes sem Oracle. */
public final class SessaoRepositoryMemoria implements SessaoRepository {

    private final Map<String, Sessao> porHash = new HashMap<>();
    private long proximoId = 1;
    private boolean falharAoCriar;

    /** Faz as proximas gravacoes falharem como se o banco estivesse fora do ar. */
    public synchronized void falharAoCriar(boolean falhar) {
        this.falharAoCriar = falhar;
    }

    /** @return copia das sessoes gravadas (abertas ou nao) */
    public synchronized List<Sessao> todas() {
        return new ArrayList<>(porHash.values());
    }

    /** @return hashes de token guardados, para conferir que o token em claro nao e armazenado */
    public synchronized List<String> hashesGuardados() {
        return new ArrayList<>(porHash.keySet());
    }

    @Override
    public synchronized Sessao criar(long usuarioId, String tokenHash, Instant criadaEm, Instant expiraEm) {
        if (falharAoCriar) {
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL", "Servico temporariamente indisponivel.");
        }
        Sessao sessao = new Sessao(proximoId++, usuarioId, criadaEm, expiraEm, null);
        porHash.put(tokenHash, sessao);
        return sessao;
    }

    @Override
    public synchronized Optional<Sessao> buscarPorTokenHash(String tokenHash) {
        return Optional.ofNullable(porHash.get(tokenHash));
    }

    @Override
    public synchronized void encerrar(long sessaoId, Instant quando) {
        porHash.replaceAll((hash, s) -> s.id() == sessaoId && s.encerradaEm() == null
                ? new Sessao(s.id(), s.usuarioId(), s.criadaEm(), s.expiraEm(), quando) : s);
    }
}
