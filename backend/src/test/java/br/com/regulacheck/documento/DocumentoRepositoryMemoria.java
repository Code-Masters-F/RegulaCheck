package br.com.regulacheck.documento;

import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.documento.repository.NovoDocumento;
import br.com.regulacheck.documento.repository.ResultadoRegistro;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de documentos em memoria para testes sem Oracle.
 *
 * <p>Reproduz as regras do JDBC: filtro pela dona e amarracao ao pedido em toda operacao, as seis situacoes de
 * {@link #registrar}, revisao +1 refletida no {@link SolicitacaoRepositoryMemoria} e contagem de documentos
 * disponiveis. O {@code synchronized} no monitor do repositorio de pedidos faz o papel do
 * {@code SELECT ... FOR UPDATE}: serializa envios e trocas de servico do mesmo pedido. Por isso os testes de
 * concorrencia sobre este dobro provam o Service e a logica de decisao, <strong>nao</strong> o bloqueio de linha do
 * Oracle (isso so o teste opt-in contra o banco real comprova).
 */
public final class DocumentoRepositoryMemoria implements DocumentoRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final Map<Long, Documento> porId = new LinkedHashMap<>();
    private long proximoId = 1;
    private RuntimeException falhaInjetada;

    /** @param pedidos repositorio de pedidos que compartilha o "bloqueio" e recebe a revisao nova */
    public DocumentoRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos) {
        this.pedidos = pedidos;
        pedidos.usarContadorDeDocumentos(this::disponiveis);
    }

    @Override
    public ResultadoRegistro registrar(long pacienteId, long solicitacaoId, NovoDocumento novo, Instant agora) {
        synchronized (pedidos) {
            if (falhaInjetada != null) {
                RuntimeException falha = falhaInjetada;
                falhaInjetada = null;
                throw falha;
            }
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return ResultadoRegistro.naoEncontrado();
            }
            if (pedido.get().estado() != EstadoSolicitacao.RASCUNHO) {
                return ResultadoRegistro.foraDeRascunho();
            }
            int disponiveis = disponiveis(solicitacaoId);
            if (novo.chaveEnvio() != null) {
                Optional<Documento> existente = porChave(solicitacaoId, novo.chaveEnvio());
                if (existente.isPresent()) {
                    boolean mesmaTentativa = novo.hashConteudo().equals(existente.get().hashConteudo())
                            && novo.tipoDocumental() == existente.get().tipoDocumental();
                    return mesmaTentativa
                            ? ResultadoRegistro.repetido(existente.get(), pedido.get().revisaoAtual(), disponiveis)
                            : ResultadoRegistro.conflitoChave();
                }
            }
            if (disponiveis >= LimitesArquivo.MAXIMO_ARQUIVOS) {
                return ResultadoRegistro.limite();
            }
            Solicitacao atualizado = pedidos.incrementarPorDocumento(solicitacaoId, agora);
            long id = proximoId++;
            Documento documento = new Documento(id, solicitacaoId, novo.nomeArquivo(), novo.tipoDocumental(),
                    novo.formato(), novo.tamanhoBytes(), novo.paginas(), atualizado.revisaoAtual(), agora,
                    novo.caminhoInterno(), novo.hashConteudo(), novo.chaveEnvio());
            porId.put(id, documento);
            return ResultadoRegistro.registrado(documento, atualizado.revisaoAtual(), disponiveis + 1);
        }
    }

    @Override
    public List<Documento> listarDoPedido(long solicitacaoId, long pacienteId) {
        synchronized (pedidos) {
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(porId.values().stream().filter(d -> d.solicitacaoId() == solicitacaoId).toList());
        }
    }

    @Override
    public Optional<Documento> buscarDoPedido(long documentoId, long solicitacaoId, long pacienteId) {
        synchronized (pedidos) {
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(porId.get(documentoId)).filter(d -> d.solicitacaoId() == solicitacaoId);
        }
    }

    @Override
    public Optional<Documento> buscarPorChave(long solicitacaoId, long pacienteId, String chaveEnvio) {
        synchronized (pedidos) {
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return Optional.empty();
            }
            return porChave(solicitacaoId, chaveEnvio);
        }
    }

    /**
     * Faz o proximo {@link #registrar} lancar a falha informada, antes de qualquer alteracao (simula banco fora do
     * ar ou erro na transacao).
     *
     * @param falha excecao a lancar uma unica vez
     */
    public void falharNoProximoRegistro(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaInjetada = falha;
        }
    }

    /** @return todos os documentos, de qualquer pedido, na ordem de gravacao (so para conferencias de teste) */
    public List<Documento> todos() {
        synchronized (pedidos) {
            return new ArrayList<>(porId.values());
        }
    }

    private int disponiveis(long solicitacaoId) {
        synchronized (pedidos) {
            return (int) porId.values().stream().filter(d -> d.solicitacaoId() == solicitacaoId).count();
        }
    }

    private Optional<Documento> porChave(long solicitacaoId, String chave) {
        return porId.values().stream()
                .filter(d -> d.solicitacaoId() == solicitacaoId && chave.equals(d.chaveEnvio())).findFirst();
    }
}
