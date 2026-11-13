package br.com.regulacheck.extracao;

import br.com.regulacheck.documento.DocumentoRepositoryMemoria;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.repository.ContextoLeitura;
import br.com.regulacheck.extracao.repository.DocumentoParaLeitura;
import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import br.com.regulacheck.extracao.repository.ExtracaoRepository;
import br.com.regulacheck.extracao.repository.LeituraParaGravar;
import br.com.regulacheck.extracao.repository.ResultadoGravacao;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Dobro de teste do {@link ExtracaoRepository}, sem banco. Aplica as mesmas regras do JDBC: dona, estado,
 * revisao capturada e uma leitura por (pedido, revisao). A exclusao atomica das etapas do JDBC (sob
 * {@code FOR UPDATE}) e imitada por {@code synchronized} no mesmo monitor dos dobros de pedido e de documento
 * ({@link SolicitacaoRepositoryMemoria}); isso prova a logica, <strong>nao</strong> o bloqueio de linha do Oracle
 * (o teste opt-in contra o banco real e quem comprova).
 */
public final class ExtracaoRepositoryMemoria implements ExtracaoRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final DocumentoRepositoryMemoria documentos;
    // pedido -> (revisao -> leitura)
    private final Map<Long, Map<Integer, Extracao>> leituras = new TreeMap<>();
    private long proximoId = 1;
    private int gravacoes;
    private RuntimeException falhaNaGravacao;
    private RuntimeException falhaNaConsulta;

    /**
     * @param pedidos dobro de pedidos (e monitor compartilhado)
     * @param documentos dobro de documentos
     */
    public ExtracaoRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos, DocumentoRepositoryMemoria documentos) {
        this.pedidos = pedidos;
        this.documentos = documentos;
        // Excluir o pedido apaga as leituras dele, como as DELETEs do JDBC (antes dos documentos).
        pedidos.registrarLimpeza(id -> {
            synchronized (pedidos) {
                leituras.remove(id);
            }
        });
    }

    @Override
    public Optional<ContextoLeitura> contexto(long pacienteId, long solicitacaoId) {
        synchronized (pedidos) {
            falharSeInjetado(true);
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return Optional.empty();
            }
            List<DocumentoParaLeitura> lista = new ArrayList<>();
            for (Documento d : documentos.listarDoPedido(solicitacaoId, pacienteId)) {
                lista.add(new DocumentoParaLeitura(d.id(), d.tipoDocumental(), d.formato(), d.paginas(),
                        d.caminhoInterno()));
            }
            return Optional.of(new ContextoLeitura(pedido.get().estado(), pedido.get().revisaoAtual(), lista));
        }
    }

    @Override
    public Optional<ExtracaoRegistrada> buscarMaisRecente(long pacienteId, long solicitacaoId) {
        synchronized (pedidos) {
            falharSeInjetado(true);
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            Map<Integer, Extracao> doPedido = leituras.get(solicitacaoId);
            if (pedido.isEmpty() || doPedido == null || doPedido.isEmpty()) {
                return Optional.empty();
            }
            Extracao maisRecente = ((TreeMap<Integer, Extracao>) doPedido).lastEntry().getValue();
            return Optional.of(new ExtracaoRegistrada(maisRecente, pedido.get().revisaoAtual()));
        }
    }

    @Override
    public Optional<ExtracaoRegistrada> buscarPorId(long pacienteId, long solicitacaoId, long extracaoId) {
        synchronized (pedidos) {
            falharSeInjetado(true);
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            Map<Integer, Extracao> doPedido = leituras.get(solicitacaoId);
            if (pedido.isEmpty() || doPedido == null) {
                return Optional.empty();
            }
            return doPedido.values().stream().filter(leitura -> leitura.id() == extracaoId).findFirst()
                    .map(leitura -> new ExtracaoRegistrada(leitura, pedido.get().revisaoAtual()));
        }
    }

    @Override
    public ResultadoGravacao gravar(long pacienteId, long solicitacaoId, int revisaoCapturada,
            List<LeituraParaGravar> paraGravar, Instant agora) {
        synchronized (pedidos) {
            falharSeInjetado(false);
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return ResultadoGravacao.naoEncontrado();
            }
            if (pedido.get().estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return ResultadoGravacao.submetido();
            }
            if (pedido.get().revisaoAtual() != revisaoCapturada) {
                return ResultadoGravacao.revisaoMudou();
            }
            Map<Integer, Extracao> doPedido = leituras.computeIfAbsent(solicitacaoId, id -> new TreeMap<>());
            Extracao existente = doPedido.get(revisaoCapturada);
            if (existente != null) {
                return ResultadoGravacao.jaExistia(new ExtracaoRegistrada(existente, pedido.get().revisaoAtual()));
            }
            List<ExtracaoDocumento> lidos = new ArrayList<>();
            for (LeituraParaGravar item : paraGravar) {
                Documento documento = documentos.listarDoPedido(solicitacaoId, pacienteId).stream()
                        .filter(d -> d.id() == item.documentoId()).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Documento da leitura nao esta disponivel."));
                lidos.add(new ExtracaoDocumento(item.documentoId(), documento.tipoDocumental(),
                        item.leitura().situacao(), item.leitura().campos()));
            }
            Extracao nova = new Extracao(proximoId++, solicitacaoId, revisaoCapturada, agora, lidos);
            doPedido.put(revisaoCapturada, nova);
            gravacoes++;
            return ResultadoGravacao.gravada(new ExtracaoRegistrada(nova, pedido.get().revisaoAtual()));
        }
    }

    /** @return quantas leituras foram efetivamente gravadas (nao conta as devolvidas por ja existirem) */
    public int gravacoes() {
        synchronized (pedidos) {
            return gravacoes;
        }
    }

    /**
     * @param solicitacaoId pedido
     * @return quantas leituras o pedido tem gravadas (uma por revisao)
     */
    public int totalDoPedido(long solicitacaoId) {
        synchronized (pedidos) {
            Map<Integer, Extracao> doPedido = leituras.get(solicitacaoId);
            return doPedido == null ? 0 : doPedido.size();
        }
    }

    /** @return o total de leituras gravadas em todos os pedidos */
    public int total() {
        synchronized (pedidos) {
            return leituras.values().stream().mapToInt(Map::size).sum();
        }
    }

    /**
     * Faz a proxima gravacao lancar a falha informada (uma unica vez), para testar banco indisponivel.
     *
     * @param falha excecao a lancar
     */
    public void falharNaProximaGravacao(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNaGravacao = falha;
        }
    }

    /**
     * Faz a proxima consulta ({@code contexto} ou {@code buscarMaisRecente}) lancar a falha informada, uma vez.
     *
     * @param falha excecao a lancar
     */
    public void falharNaProximaConsulta(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNaConsulta = falha;
        }
    }

    private void falharSeInjetado(boolean consulta) {
        if (consulta && falhaNaConsulta != null) {
            RuntimeException falha = falhaNaConsulta;
            falhaNaConsulta = null;
            throw falha;
        }
        if (!consulta && falhaNaGravacao != null) {
            RuntimeException falha = falhaNaGravacao;
            falhaNaGravacao = null;
            throw falha;
        }
    }
}
