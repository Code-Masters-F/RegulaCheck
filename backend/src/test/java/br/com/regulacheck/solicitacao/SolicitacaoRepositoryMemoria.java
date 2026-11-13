package br.com.regulacheck.solicitacao;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.repository.ResultadoExclusao;
import br.com.regulacheck.solicitacao.repository.ResultadoTroca;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.LongToIntFunction;

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

    private final List<java.util.function.LongConsumer> limpezas = new java.util.ArrayList<>();

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
    private LongToIntFunction contadorDeDocumentos = id -> 0;
    private LongFunction<List<IdInterno>> apagadorDeDocumentos = id -> List.of();
    private final Set<Long> pedidosComDependencias = new HashSet<>();
    private RuntimeException falhaNaExclusao;

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
        return Optional.ofNullable(pedidos.get(id)).filter(p -> p.pacienteId() == pacienteId).map(this::comContagem);
    }

    @Override
    public synchronized List<Solicitacao> listarDaPaciente(long pacienteId, int limite) {
        Comparator<Solicitacao> maisRecentePrimeiro = Comparator.comparing(Solicitacao::criadaEm)
                .thenComparing(Solicitacao::id).reversed();
        return pedidos.values().stream().filter(p -> p.pacienteId() == pacienteId)
                .sorted(maisRecentePrimeiro).limit(limite).map(this::comContagem).toList();
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
            return ResultadoTroca.semMudanca(comContagem(atual));
        }
        Solicitacao nova = new Solicitacao(id, pacienteId, novoProtocolo, atual.estado(), atual.revisaoAtual() + 1,
                atual.criadaEm(), agora);
        pedidos.put(id, nova);
        revisoes.get(id).add(new RevisaoRegistrada(nova.revisaoAtual(), MotivoRevisao.PROTOCOLO_ALTERADO, agora));
        return ResultadoTroca.alterada(comContagem(nova));
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

    /**
     * Liga o dobro de documentos a este: a contagem de documentos disponiveis de cada pedido passa a vir dele
     * (espelha a subconsulta de contagem do JDBC).
     *
     * @param contador devolve quantos documentos disponiveis o pedido tem
     */
    public synchronized void usarContadorDeDocumentos(LongToIntFunction contador) {
        this.contadorDeDocumentos = contador;
    }

    /**
     * Incrementa a revisao do pedido por causa de um documento enviado e registra a revisao com o motivo
     * {@link MotivoRevisao#DOCUMENTO_ENVIADO} (so o dobro de documentos chama; equivale ao {@code UPDATE} e ao
     * {@code INSERT} em {@code RC_REVISAO} da transacao do JDBC).
     *
     * @param id pedido existente
     * @param agora instante do envio
     * @return o pedido com a revisao nova
     */
    public synchronized Solicitacao incrementarPorDocumento(long id, Instant agora) {
        return incrementarPorDocumento(id, MotivoRevisao.DOCUMENTO_ENVIADO, agora);
    }

    /**
     * Incrementa a revisao do pedido por uma mudanca do conjunto documental (envio, substituicao ou remocao) e
     * registra a revisao com o motivo informado. O estado do pedido nao muda.
     *
     * @param id pedido existente
     * @param motivo motivo da revisao
     * @param agora instante da mudanca
     * @return o pedido com a revisao nova
     */
    public synchronized Solicitacao incrementarPorDocumento(long id, MotivoRevisao motivo, Instant agora) {
        Solicitacao atual = pedidos.get(id);
        Solicitacao nova = new Solicitacao(id, atual.pacienteId(), atual.protocolo(), atual.estado(),
                atual.revisaoAtual() + 1, atual.criadaEm(), agora);
        pedidos.put(id, nova);
        revisoes.get(id).add(new RevisaoRegistrada(nova.revisaoAtual(), motivo, agora));
        return comContagem(nova);
    }

    /**
     * Faz a proxima {@link #excluirRascunho} lancar a falha informada, antes de qualquer alteracao (simula banco
     * fora do ar na transacao de exclusao).
     *
     * @param falha excecao a lancar uma unica vez
     */
    public synchronized void falharNaProximaExclusao(RuntimeException falha) {
        this.falhaNaExclusao = falha;
    }

    @Override
    public synchronized ResultadoExclusao excluirRascunho(long id, long pacienteId) {
        if (falhaNaExclusao != null) {
            RuntimeException falha = falhaNaExclusao;
            falhaNaExclusao = null;
            throw falha;
        }
        Solicitacao atual = pedidos.get(id);
        if (atual == null || atual.pacienteId() != pacienteId) {
            return ResultadoExclusao.naoEncontrado();
        }
        if (atual.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            return ResultadoExclusao.submetido();
        }
        if (atual.estado() != EstadoSolicitacao.RASCUNHO) {
            return ResultadoExclusao.foraDeRascunho();
        }
        if (pedidosComDependencias.contains(id)) {
            return ResultadoExclusao.comDependencias();
        }
        // Documentos, revisoes e pedido saem juntos (um so "commit" em memoria).
        limpezas.forEach(limpeza -> limpeza.accept(id));
        List<IdInterno> arquivos = apagadorDeDocumentos.apply(id);
        revisoes.remove(id);
        pedidos.remove(id);
        return ResultadoExclusao.excluido(arquivos);
    }

    /**
     * Registra uma limpeza que roda ao excluir um pedido, antes dos documentos (por exemplo, apagar as leituras do
     * pedido, que referenciam os documentos).
     *
     * @param limpeza recebe o id do pedido excluido
     */
    public synchronized void registrarLimpeza(java.util.function.LongConsumer limpeza) {
        limpezas.add(limpeza);
    }

    /**
     * Liga o dobro de documentos a este: apagar o pedido apaga tambem os documentos dele e devolve os arquivos.
     *
     * @param apagador apaga os documentos (disponiveis e removidos) do pedido e devolve os arquivos
     */
    public synchronized void usarApagadorDeDocumentos(LongFunction<List<IdInterno>> apagador) {
        this.apagadorDeDocumentos = apagador;
    }

    /**
     * Simula algo ainda ligado ao pedido (chave estrangeira de uma historia futura): a exclusao passa a ser
     * recusada com {@code COM_DEPENDENCIAS}.
     *
     * @param id pedido
     */
    public synchronized void impedirExclusao(long id) {
        pedidosComDependencias.add(id);
    }

    private Solicitacao comContagem(Solicitacao pedido) {
        return pedido.comDocumentos(contadorDeDocumentos.applyAsInt(pedido.id()));
    }

    /** @return quantos pedidos existem, de qualquer paciente */
    public synchronized int total() {
        return pedidos.size();
    }
}
