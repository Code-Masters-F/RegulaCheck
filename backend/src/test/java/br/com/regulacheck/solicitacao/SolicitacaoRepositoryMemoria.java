package br.com.regulacheck.solicitacao;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.repository.ResultadoTroca;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de solicitacoes em memoria para testes sem Oracle.
 *
 * <p>Reproduz as regras do JDBC: filtro pela dona em toda operacao, as quatro situacoes de
 * {@link #trocarProtocolo} e historico de revisoes. O {@code synchronized} faz o papel do
 * {@code SELECT ... FOR UPDATE}: serializa as operacoes. Por isso os testes de concorrencia sobre este dobro
 * provam o Service e a logica de decisao, <strong>nao</strong> o bloqueio de linha do Oracle (isso so o teste
 * opt-in contra o banco real comprova).
 */
public final class SolicitacaoRepositoryMemoria implements SolicitacaoRepository {

    /**
     * Revisao registrada no historico.
     *
     * @param numero numero da revisao
     * @param motivo motivo da revisao
     * @param quando instante da revisao
     */
    public record RevisaoRegistrada(int numero, MotivoRevisao motivo, Instant quando) {
    }

    private final Map<Long, Solicitacao> pedidos = new LinkedHashMap<>();
    private final Map<Long, List<RevisaoRegistrada>> revisoes = new LinkedHashMap<>();
    private long proximoId = 1;

    @Override
    public synchronized Solicitacao criar(long pacienteId, Protocolo protocolo, Instant agora) {
        long id = proximoId++;
        Solicitacao pedido = new Solicitacao(id, pacienteId, protocolo, EstadoSolicitacao.RASCUNHO, 1, agora, agora);
        pedidos.put(id, pedido);
        List<RevisaoRegistrada> historico = new ArrayList<>();
        historico.add(new RevisaoRegistrada(1, MotivoRevisao.CRIACAO, agora));
        revisoes.put(id, historico);
        return pedido;
    }

    @Override
    public synchronized Optional<Solicitacao> buscarDaPaciente(long id, long pacienteId) {
        return Optional.ofNullable(pedidos.get(id)).filter(p -> p.pacienteId() == pacienteId);
    }

    @Override
    public synchronized List<Solicitacao> listarDaPaciente(long pacienteId, int limite) {
        Comparator<Solicitacao> maisRecentePrimeiro = Comparator.comparing(Solicitacao::criadaEm)
                .thenComparing(Solicitacao::id).reversed();
        return pedidos.values().stream().filter(p -> p.pacienteId() == pacienteId)
                .sorted(maisRecentePrimeiro).limit(limite).toList();
    }

    @Override
    public synchronized ResultadoTroca trocarProtocolo(long id, long pacienteId, Protocolo novoProtocolo,
            Instant agora) {
        Solicitacao atual = pedidos.get(id);
        if (atual == null || atual.pacienteId() != pacienteId) {
            return ResultadoTroca.naoEncontrada();
        }
        if (atual.estado() != EstadoSolicitacao.RASCUNHO) {
            return ResultadoTroca.foraDeRascunho();
        }
        if (atual.protocolo().id() == novoProtocolo.id()) {
            return ResultadoTroca.semMudanca(atual);
        }
        Solicitacao nova = new Solicitacao(id, pacienteId, novoProtocolo, atual.estado(), atual.revisaoAtual() + 1,
                atual.criadaEm(), agora);
        pedidos.put(id, nova);
        revisoes.get(id).add(new RevisaoRegistrada(nova.revisaoAtual(), MotivoRevisao.PROTOCOLO_ALTERADO, agora));
        return ResultadoTroca.alterada(nova);
    }

    /**
     * Muda o estado do pedido, como se outra historia o tivesse processado (so para testes).
     *
     * @param id pedido
     * @param estado novo estado
     */
    public synchronized void forcarEstado(long id, EstadoSolicitacao estado) {
        Solicitacao p = pedidos.get(id);
        pedidos.put(id, new Solicitacao(p.id(), p.pacienteId(), p.protocolo(), estado, p.revisaoAtual(),
                p.criadaEm(), p.atualizadaEm()));
    }

    /**
     * @param id pedido
     * @return copia do historico de revisoes do pedido, na ordem de gravacao
     */
    public synchronized List<RevisaoRegistrada> revisoesDe(long id) {
        return List.copyOf(revisoes.getOrDefault(id, List.of()));
    }

    /** @return quantos pedidos existem, de qualquer paciente */
    public synchronized int total() {
        return pedidos.size();
    }
}
