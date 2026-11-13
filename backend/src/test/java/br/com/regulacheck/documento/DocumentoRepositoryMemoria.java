package br.com.regulacheck.documento;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.documento.repository.NovoDocumento;
import br.com.regulacheck.documento.repository.ResultadoRegistro;
import br.com.regulacheck.documento.repository.ResultadoRemocao;
import br.com.regulacheck.documento.repository.ResultadoSubstituicao;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de documentos em memoria para testes sem Oracle.
 *
 * <p>Reproduz as regras do JDBC: filtro pela dona e amarracao ao pedido em toda operacao, as situacoes de
 * {@link #registrar}, {@link #remover} e {@link #substituir}, revisao +1 refletida no
 * {@link SolicitacaoRepositoryMemoria} e contagem de documentos disponiveis. O {@code synchronized} no monitor do
 * repositorio de pedidos faz o papel do {@code SELECT ... FOR UPDATE}: serializa envios, remocoes, substituicoes,
 * trocas de servico e exclusoes do mesmo pedido. Por isso os testes de concorrencia sobre este dobro provam o
 * Service e a logica de decisao, <strong>nao</strong> o bloqueio de linha do Oracle (isso so o teste opt-in contra
 * o banco real comprova).
 */
public final class DocumentoRepositoryMemoria implements DocumentoRepository {

    private final SolicitacaoRepositoryMemoria pedidos;
    private final Map<Long, Documento> porId = new LinkedHashMap<>();
    /** Documentos removidos ou substituidos: ficam como historico, sem chave de envio, e nao sao disponiveis. */
    private final Map<Long, Documento> removidos = new LinkedHashMap<>();
    private final Set<Long> pedidosComAnaliseVigente = new HashSet<>();
    private long proximoId = 1;
    private RuntimeException falhaInjetada;
    private RuntimeException falhaAlteracaoInjetada;

    /** @param pedidos repositorio de pedidos que compartilha o "bloqueio" e recebe a revisao nova */
    public DocumentoRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos) {
        this.pedidos = pedidos;
        pedidos.usarContadorDeDocumentos(this::disponiveis);
        pedidos.usarApagadorDeDocumentos(this::apagarDoPedido);
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
    public ResultadoRemocao remover(long pacienteId, long solicitacaoId, long documentoId, Instant agora) {
        synchronized (pedidos) {
            lancarFalhaDeAlteracaoSeHouver();
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return ResultadoRemocao.naoEncontrado();
            }
            if (pedido.get().estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return ResultadoRemocao.submetido();
            }
            Documento vivo = porId.get(documentoId);
            if (vivo == null || vivo.solicitacaoId() != solicitacaoId) {
                return ResultadoRemocao.naoEncontrado();
            }
            Solicitacao atualizado = pedidos.incrementarPorDocumento(solicitacaoId,
                    MotivoRevisao.DOCUMENTO_REMOVIDO, agora);
            marcarRemovido(vivo);
            pedidosComAnaliseVigente.remove(solicitacaoId);
            return ResultadoRemocao.removido(vivo, atualizado.revisaoAtual(), disponiveis(solicitacaoId));
        }
    }

    @Override
    public ResultadoSubstituicao substituir(long pacienteId, long solicitacaoId, long documentoId,
            NovoDocumento novo, Instant agora) {
        synchronized (pedidos) {
            lancarFalhaDeAlteracaoSeHouver();
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty()) {
                return ResultadoSubstituicao.naoEncontrado();
            }
            if (pedido.get().estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return ResultadoSubstituicao.submetido();
            }
            if (novo.chaveEnvio() != null) {
                // Antes do documento do caminho: depois do sucesso, o antigo ja nao existe.
                Optional<Documento> existente = porChave(solicitacaoId, novo.chaveEnvio());
                if (existente.isPresent()) {
                    boolean mesmaTentativa = novo.hashConteudo().equals(existente.get().hashConteudo())
                            && novo.tipoDocumental() == existente.get().tipoDocumental();
                    return mesmaTentativa
                            ? ResultadoSubstituicao.repetido(existente.get(), pedido.get().revisaoAtual(),
                                    disponiveis(solicitacaoId))
                            : ResultadoSubstituicao.conflitoChave();
                }
            }
            Documento antigo = porId.get(documentoId);
            if (antigo == null || antigo.solicitacaoId() != solicitacaoId) {
                return ResultadoSubstituicao.naoEncontrado();
            }
            Solicitacao atualizado = pedidos.incrementarPorDocumento(solicitacaoId,
                    MotivoRevisao.DOCUMENTO_SUBSTITUIDO, agora);
            marcarRemovido(antigo);
            long id = proximoId++;
            Documento documento = new Documento(id, solicitacaoId, novo.nomeArquivo(), novo.tipoDocumental(),
                    novo.formato(), novo.tamanhoBytes(), novo.paginas(), atualizado.revisaoAtual(), agora,
                    novo.caminhoInterno(), novo.hashConteudo(), novo.chaveEnvio());
            porId.put(id, documento);
            pedidosComAnaliseVigente.remove(solicitacaoId);
            return ResultadoSubstituicao.substituido(documento, antigo, atualizado.revisaoAtual(),
                    disponiveis(solicitacaoId));
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

    /**
     * Faz o proximo {@link #remover} ou {@link #substituir} lancar a falha informada, antes de qualquer alteracao.
     *
     * @param falha excecao a lancar uma unica vez
     */
    public void falharNaProximaAlteracao(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaAlteracaoInjetada = falha;
        }
    }

    /**
     * Monta uma analise vigente para o pedido, como se uma historia futura a tivesse produzido (nenhuma rota a
     * produz hoje).
     *
     * @param solicitacaoId pedido
     */
    public void montarAnaliseVigente(long solicitacaoId) {
        synchronized (pedidos) {
            pedidosComAnaliseVigente.add(solicitacaoId);
        }
    }

    /**
     * @param solicitacaoId pedido
     * @return {@code true} se o pedido ainda tem analise vigente
     */
    public boolean temAnaliseVigente(long solicitacaoId) {
        synchronized (pedidos) {
            return pedidosComAnaliseVigente.contains(solicitacaoId);
        }
    }

    /** @return todos os documentos disponiveis, de qualquer pedido, na ordem de gravacao (so para testes) */
    public List<Documento> todos() {
        synchronized (pedidos) {
            return new ArrayList<>(porId.values());
        }
    }

    /** @return documentos removidos ou substituidos (historico, sem chave de envio) de qualquer pedido */
    public List<Documento> removidos() {
        synchronized (pedidos) {
            return new ArrayList<>(removidos.values());
        }
    }

    private void marcarRemovido(Documento documento) {
        porId.remove(documento.id());
        // Como no JDBC: a linha fica como historico e a chave de envio e anulada.
        removidos.put(documento.id(), new Documento(documento.id(), documento.solicitacaoId(),
                documento.nomeArquivo(), documento.tipoDocumental(), documento.formato(), documento.tamanhoBytes(),
                documento.paginas(), documento.revisaoEntrada(), documento.enviadoEm(), documento.caminhoInterno(),
                documento.hashConteudo(), null));
    }

    /** Usado pela exclusao do pedido: apaga documentos disponiveis e removidos e devolve todos os arquivos. */
    private List<IdInterno> apagarDoPedido(long solicitacaoId) {
        List<IdInterno> arquivos = new ArrayList<>();
        for (Map<Long, Documento> mapa : List.of(porId, removidos)) {
            mapa.values().removeIf(d -> {
                if (d.solicitacaoId() != solicitacaoId) {
                    return false;
                }
                if (d.caminhoInterno() != null) {
                    arquivos.add(d.caminhoInterno());
                }
                return true;
            });
        }
        pedidosComAnaliseVigente.remove(solicitacaoId);
        return arquivos;
    }

    private void lancarFalhaDeAlteracaoSeHouver() {
        if (falhaAlteracaoInjetada != null) {
            RuntimeException falha = falhaAlteracaoInjetada;
            falhaAlteracaoInjetada = null;
            throw falha;
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
