package br.com.regulacheck.extracao;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.repository.ConferenciaRegistrada;
import br.com.regulacheck.extracao.repository.ConferenciaRepository;
import br.com.regulacheck.extracao.repository.DecisaoNova;
import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import br.com.regulacheck.extracao.repository.ResultadoRegistroDecisao;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Dobro de teste do {@link ConferenciaRepository}, sem banco. Aplica as mesmas regras do JDBC: dona, leitura do
 * pedido, estado, versao atual, aplicabilidade da decisao por situacao do campo (a mesma regra pura
 * {@link SituacaoConferencia#aplicavel}), uma decisao vigente por campo, repeticao sem efeito e lote tudo ou nada. A
 * serializacao (o {@code SELECT ... FOR UPDATE} do JDBC) e imitada por {@code synchronized} no mesmo monitor dos
 * dobros de pedido, de documento e de leitura; isso prova a logica de concorrencia, <strong>nao</strong> o bloqueio
 * de linha do Oracle (o teste opt-in contra o banco real e quem comprova).
 */
public final class ConferenciaRepositoryMemoria implements ConferenciaRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final ExtracaoRepositoryMemoria extracoes;
    // pedido -> leitura -> ("documento/codigo" -> decisao vigente)
    private final Map<Long, Map<Long, Map<String, DecisaoConferencia>>> decisoes = new TreeMap<>();
    private int gravacoes;
    private RuntimeException falhaNoRegistro;
    private RuntimeException falhaNaConsulta;

    /**
     * @param pedidos dobro de pedidos (e monitor compartilhado)
     * @param extracoes dobro de leituras
     */
    public ConferenciaRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos, ExtracaoRepositoryMemoria extracoes) {
        this.pedidos = pedidos;
        this.extracoes = extracoes;
        // Excluir o pedido apaga as decisoes dele, como a DELETE do JDBC (antes das leituras).
        pedidos.registrarLimpeza(id -> {
            synchronized (pedidos) {
                decisoes.remove(id);
            }
        });
    }

    @Override
    public Optional<ConferenciaRegistrada> buscar(long pacienteId, long solicitacaoId, long extracaoId) {
        synchronized (pedidos) {
            falharSeInjetado(true);
            Optional<ExtracaoRegistrada> leitura = extracoes.buscarPorId(pacienteId, solicitacaoId, extracaoId);
            if (leitura.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ConferenciaRegistrada(leitura.get(), vigentes(solicitacaoId, extracaoId)));
        }
    }

    @Override
    public ResultadoRegistroDecisao registrar(long pacienteId, long solicitacaoId, long extracaoId,
            List<DecisaoNova> novas, Instant agora) {
        synchronized (pedidos) {
            falharSeInjetado(false);
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            Optional<ExtracaoRegistrada> leitura = extracoes.buscarPorId(pacienteId, solicitacaoId, extracaoId);
            if (pedido.isEmpty() || leitura.isEmpty()) {
                return ResultadoRegistroDecisao.naoEncontrado();
            }
            if (pedido.get().estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return ResultadoRegistroDecisao.submetido();
            }
            if (!leitura.get().atual()) {
                return ResultadoRegistroDecisao.desatualizada();
            }
            Map<Long, ExtracaoDocumento> documentos = new HashMap<>();
            for (ExtracaoDocumento documento : leitura.get().extracao().documentos()) {
                documentos.put(documento.documentoId(), documento);
            }
            for (DecisaoNova nova : novas) {
                if (!documentos.containsKey(nova.documentoId())) {
                    return ResultadoRegistroDecisao.documentoForaDaLeitura();
                }
            }
            for (DecisaoNova nova : novas) {
                CampoLido campo = documentos.get(nova.documentoId()).campos().stream()
                        .filter(c -> c.codigo() == nova.codigo()).findFirst().orElse(null);
                if (campo == null || !SituacaoConferencia.aplicavel(campo.situacao(), nova.tipo(), nova.motivo())) {
                    return ResultadoRegistroDecisao.naoAplicavel(nova.documentoId(), nova.codigo());
                }
            }
            Map<String, DecisaoConferencia> doPedido = decisoes.computeIfAbsent(solicitacaoId,
                    id -> new TreeMap<>()).computeIfAbsent(extracaoId, id -> new TreeMap<>());
            for (DecisaoNova nova : novas) {
                String chave = chave(nova.documentoId(), nova.codigo());
                DecisaoConferencia vigente = doPedido.get(chave);
                if (vigente != null && vigente.tipo() == nova.tipo() && vigente.motivo() == nova.motivo()) {
                    continue; // repetir a mesma decisao nao muda nada, nem o instante
                }
                doPedido.put(chave, new DecisaoConferencia(nova.documentoId(), nova.codigo(), nova.tipo(),
                        nova.motivo(), agora));
                gravacoes++;
            }
            return ResultadoRegistroDecisao.registrada();
        }
    }

    /** @return quantas decisoes foram efetivamente gravadas ou trocadas (nao conta as repetidas) */
    public int gravacoes() {
        synchronized (pedidos) {
            return gravacoes;
        }
    }

    /**
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @return as decisoes vigentes da leitura
     */
    public List<DecisaoConferencia> decisoesDa(long solicitacaoId, long extracaoId) {
        synchronized (pedidos) {
            return vigentes(solicitacaoId, extracaoId);
        }
    }

    /** @return o total de decisoes vigentes em todos os pedidos e leituras */
    public int total() {
        synchronized (pedidos) {
            return decisoes.values().stream().flatMap(m -> m.values().stream()).mapToInt(Map::size).sum();
        }
    }

    /**
     * Faz o proximo registro lancar a falha informada (uma unica vez), para testar banco indisponivel.
     *
     * @param falha excecao a lancar
     */
    public void falharNoProximoRegistro(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNoRegistro = falha;
        }
    }

    /**
     * Faz a proxima consulta lancar a falha informada (uma unica vez).
     *
     * @param falha excecao a lancar
     */
    public void falharNaProximaConsulta(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNaConsulta = falha;
        }
    }

    private List<DecisaoConferencia> vigentes(long solicitacaoId, long extracaoId) {
        Map<Long, Map<String, DecisaoConferencia>> doPedido = decisoes.get(solicitacaoId);
        if (doPedido == null || doPedido.get(extracaoId) == null) {
            return List.of();
        }
        return new ArrayList<>(doPedido.get(extracaoId).values());
    }

    private static String chave(long documentoId, CodigoCampo codigo) {
        return documentoId + "/" + codigo.name();
    }

    private void falharSeInjetado(boolean consulta) {
        if (consulta && falhaNaConsulta != null) {
            RuntimeException falha = falhaNaConsulta;
            falhaNaConsulta = null;
            throw falha;
        }
        if (!consulta && falhaNoRegistro != null) {
            RuntimeException falha = falhaNoRegistro;
            falhaNoRegistro = null;
            throw falha;
        }
    }
}
