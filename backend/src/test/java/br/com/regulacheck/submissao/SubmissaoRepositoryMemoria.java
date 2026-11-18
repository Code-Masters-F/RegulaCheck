package br.com.regulacheck.submissao;

import br.com.regulacheck.analise.AnaliseRepositoryMemoria;
import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.submissao.entity.Submissao;
import br.com.regulacheck.submissao.repository.ResultadoSubmissao;
import br.com.regulacheck.submissao.repository.ResultadoSubmissao.Situacao;
import br.com.regulacheck.submissao.repository.SubmissaoRepository;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de submissoes em memoria para testes sem Oracle (RC-17).
 *
 * <p>Reproduz as decisoes do {@code SubmissaoRepositoryJdbc}, na mesma ordem, sobre os dobros de pedido e de analise.
 * O {@code synchronized} no monitor do repositorio de pedidos faz o papel do {@code SELECT ... FOR UPDATE} (o mesmo
 * monitor dos dobros de documento, conferencia e analise): os testes de concorrencia sobre este dobro provam a logica
 * do servico, <strong>nao</strong> o bloqueio de linha do Oracle (isso so o {@code SubmissaoOracleTest} comprova).
 */
public final class SubmissaoRepositoryMemoria implements SubmissaoRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final AnaliseRepositoryMemoria analises;
    private final Map<Long, Submissao> porPedido = new HashMap<>();
    private long proximoId = 1;
    private RuntimeException falhaInjetada;

    /**
     * @param pedidos dobro dos pedidos (monitor compartilhado e estado)
     * @param analises dobro das analises (vigente e resultados)
     */
    public SubmissaoRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos, AnaliseRepositoryMemoria analises) {
        this.pedidos = pedidos;
        this.analises = analises;
    }

    @Override
    public ResultadoSubmissao submeter(long pacienteId, long solicitacaoId, String referencia, Instant agora) {
        synchronized (pedidos) {
            if (falhaInjetada != null) {
                RuntimeException falha = falhaInjetada;
                falhaInjetada = null;
                throw falha;
            }
            Optional<Solicitacao> lido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (lido.isEmpty()) {
                return ResultadoSubmissao.sem(Situacao.NAO_ENCONTRADO);
            }
            Solicitacao pedido = lido.get();
            Submissao existente = porPedido.get(solicitacaoId);
            if (existente != null) {
                return ResultadoSubmissao.jaSubmetida(existente);
            }
            if (pedido.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return ResultadoSubmissao.sem(Situacao.SOMENTE_CONSULTA);
            }
            if (pedido.estado() == EstadoSolicitacao.PROCESSANDO) {
                return ResultadoSubmissao.sem(Situacao.EM_ANDAMENTO);
            }
            Optional<Analise> vigente = analises.historico(pacienteId, solicitacaoId, 1)
                    .flatMap(historico -> historico.analises().stream()
                            .filter(a -> a.vigente() && a.revisao() == pedido.revisaoAtual()).findFirst());
            if (vigente.isEmpty()) {
                return ResultadoSubmissao.sem(Situacao.SEM_ANALISE_VIGENTE);
            }
            Analise analise = vigente.get();
            if (analise.resultadoGeral() != ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS
                    || analise.resultados().stream().anyMatch(r -> r.resultado().geraPendencia())) {
                return ResultadoSubmissao.sem(Situacao.COM_PENDENCIAS);
            }
            if (pedido.estado() != EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS) {
                return ResultadoSubmissao.sem(Situacao.SEM_ANALISE_VIGENTE);
            }
            Submissao nova = new Submissao(proximoId++, solicitacaoId, analise.id(), referencia, agora,
                    analise.revisao(), analise.protocoloIdentificador(), analise.protocoloVersao(),
                    pedido.protocolo().localidade(), pedido.protocolo().especialidade(),
                    pedido.protocolo().avisoFicticio());
            porPedido.put(solicitacaoId, nova);
            pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.SUBMETIDA_SIMULACAO, agora);
            return ResultadoSubmissao.submetida(nova);
        }
    }

    @Override
    public Optional<Submissao> buscar(long pacienteId, long solicitacaoId) {
        synchronized (pedidos) {
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(porPedido.get(solicitacaoId));
        }
    }

    /**
     * Faz a proxima conclusao falhar com a excecao dada, antes de decidir qualquer coisa (banco indisponivel).
     *
     * @param falha excecao a lancar
     */
    public void falharNaProximaSubmissao(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaInjetada = falha;
        }
    }

    /** @return quantas submissoes existem em todos os pedidos */
    public int total() {
        synchronized (pedidos) {
            return porPedido.size();
        }
    }
}
