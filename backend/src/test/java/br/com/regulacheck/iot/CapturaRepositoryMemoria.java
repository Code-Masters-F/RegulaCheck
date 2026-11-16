package br.com.regulacheck.iot;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.SessaoCaptura;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.repository.AberturaSessao;
import br.com.regulacheck.iot.repository.CapturaRepository;
import br.com.regulacheck.iot.repository.NovaLeitura;
import br.com.regulacheck.iot.repository.TelemetriaDoPedido;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dobro em memoria de {@link CapturaRepository} com as mesmas regras do JDBC: dona do pedido, pedido submetido,
 * no maximo uma sessao aberta por pedido, leitura so em sessao aberta. Usa o monitor do
 * {@link SolicitacaoRepositoryMemoria} como o "bloqueio do pedido", entao abertura, gravacao e exclusao do rascunho
 * ficam serializadas como no Oracle. A exclusao do rascunho apaga sessoes e leituras do pedido.
 */
public final class CapturaRepositoryMemoria implements CapturaRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final Map<Long, SessaoCaptura> sessoes = new LinkedHashMap<>();
    private final List<LeituraSensor> leituras = new ArrayList<>();
    private long proximaSessao = 1;
    private long proximaLeitura = 1;
    private boolean bancoFora;

    /**
     * @param pedidos dobro dos pedidos, cujo monitor serializa as operacoes
     */
    public CapturaRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos) {
        this.pedidos = pedidos;
        pedidos.registrarLimpeza(solicitacaoId -> {
            List<Long> doPedido = sessoes.values().stream().filter(s -> s.solicitacaoId() == solicitacaoId)
                    .map(SessaoCaptura::id).toList();
            leituras.removeIf(l -> doPedido.contains(l.sessaoId()));
            doPedido.forEach(sessoes::remove);
        });
    }

    @Override
    public AberturaSessao abrirSessao(long solicitacaoId, long pacienteId, Instant agora) {
        synchronized (pedidos) {
            exigirBanco();
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return AberturaSessao.naoEncontrado();
            }
            if (pedido.get().estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return AberturaSessao.submetido();
            }
            Optional<SessaoCaptura> aberta = sessoes.values().stream()
                    .filter(s -> s.solicitacaoId() == solicitacaoId && s.aberta()).findFirst();
            if (aberta.isPresent()) {
                return AberturaSessao.existente(aberta.get());
            }
            SessaoCaptura nova = new SessaoCaptura(proximaSessao++, solicitacaoId, agora, null);
            sessoes.put(nova.id(), nova);
            return AberturaSessao.criada(nova);
        }
    }

    @Override
    public boolean sessaoAbertaExiste(long sessaoId) {
        synchronized (pedidos) {
            exigirBanco();
            SessaoCaptura sessao = sessoes.get(sessaoId);
            return sessao != null && sessao.aberta();
        }
    }

    @Override
    public Optional<LeituraSensor> gravarLeitura(NovaLeitura nova) {
        synchronized (pedidos) {
            exigirBanco();
            SessaoCaptura sessao = sessoes.get(nova.sessaoId());
            if (sessao == null || !sessao.aberta()) {
                return Optional.empty();
            }
            LeituraSensor gravada = new LeituraSensor(proximaLeitura++, nova.sessaoId(), nova.tipo(), nova.valor(),
                    nova.medidaEm(), nova.recebidaEm());
            leituras.add(gravada);
            return Optional.of(gravada);
        }
    }

    @Override
    public Optional<TelemetriaDoPedido> telemetriaDaPaciente(long solicitacaoId, long pacienteId) {
        synchronized (pedidos) {
            exigirBanco();
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return Optional.empty();
            }
            Optional<SessaoCaptura> recente = sessoes.values().stream()
                    .filter(s -> s.solicitacaoId() == solicitacaoId)
                    .max(Comparator.comparing(SessaoCaptura::iniciadaEm).thenComparing(SessaoCaptura::id));
            if (recente.isEmpty()) {
                return Optional.of(TelemetriaDoPedido.semSessao());
            }
            long sessaoId = recente.get().id();
            Map<TipoSensor, LeituraSensor> ultimas = new EnumMap<>(TipoSensor.class);
            for (TipoSensor tipo : TipoSensor.values()) {
                leituras.stream().filter(l -> l.sessaoId() == sessaoId && l.tipo() == tipo)
                        .max(Comparator.comparing(LeituraSensor::medidaEm).thenComparing(LeituraSensor::id))
                        .ifPresent(l -> ultimas.put(tipo, l));
            }
            return Optional.of(new TelemetriaDoPedido(recente.get(), ultimas));
        }
    }

    /**
     * Cria uma sessao diretamente (como a carga do banco faria), aberta ou encerrada.
     *
     * @param solicitacaoId pedido
     * @param iniciadaEm inicio
     * @param encerradaEm fim; {@code null} para aberta
     * @return a sessao criada
     */
    public SessaoCaptura adicionarSessao(long solicitacaoId, Instant iniciadaEm, Instant encerradaEm) {
        synchronized (pedidos) {
            SessaoCaptura sessao = new SessaoCaptura(proximaSessao++, solicitacaoId, iniciadaEm, encerradaEm);
            sessoes.put(sessao.id(), sessao);
            return sessao;
        }
    }

    /**
     * Encerra a sessao (nenhuma rota faz isso nesta entrega; so a carga).
     *
     * @param sessaoId sessao
     * @param quando instante de encerramento
     */
    public void encerrar(long sessaoId, Instant quando) {
        synchronized (pedidos) {
            SessaoCaptura s = sessoes.get(sessaoId);
            sessoes.put(sessaoId, new SessaoCaptura(s.id(), s.solicitacaoId(), s.iniciadaEm(), quando));
        }
    }

    /** @param fora {@code true} para simular o banco indisponivel em todas as operacoes */
    public void bancoFora(boolean fora) {
        synchronized (pedidos) {
            this.bancoFora = fora;
        }
    }

    /** @return todas as sessoes, na ordem de criacao */
    public List<SessaoCaptura> sessoes() {
        synchronized (pedidos) {
            return List.copyOf(sessoes.values());
        }
    }

    /** @return todas as leituras gravadas, na ordem de gravacao */
    public List<LeituraSensor> leituras() {
        synchronized (pedidos) {
            return List.copyOf(leituras);
        }
    }

    private void exigirBanco() {
        if (bancoFora) {
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
    }
}
