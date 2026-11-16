package br.com.regulacheck.analise;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.DocumentoDaAnalise;
import br.com.regulacheck.analise.entity.EvidenciaGravada;
import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import br.com.regulacheck.analise.entity.PendenciaGravada;
import br.com.regulacheck.analise.entity.ResultadoGravado;
import br.com.regulacheck.analise.repository.AnaliseParaGravar;
import br.com.regulacheck.analise.repository.AnaliseRepository;
import br.com.regulacheck.analise.repository.Encerramento;
import br.com.regulacheck.analise.repository.InicioAnalise;
import br.com.regulacheck.analise.repository.PublicacaoAnalise;
import br.com.regulacheck.documento.DocumentoRepositoryMemoria;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.extracao.ConferenciaRepositoryMemoria;
import br.com.regulacheck.extracao.ExtracaoRepositoryMemoria;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.Pendencia;
import br.com.regulacheck.validacao.model.ResultadoDeRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de analises em memoria para testes sem Oracle.
 *
 * <p>Reproduz as regras do {@code AnaliseRepositoryJdbc}: dona aplicada em toda operacao; {@code PROCESSANDO} com
 * instante de inicio (aqui num mapa, la em {@code RC_SOLICITACAO.ANALISE_INICIADA_EM}); limite de tempo com tomada da
 * trava vencida como falha; conferencia da revisao e do instante da conferencia na publicacao; descarte para o
 * estado da analise concluida mais recente ou {@code RASCUNHO}; no maximo uma vigente; gravacao completa ou nada. O
 * {@code synchronized} no monitor do repositorio de pedidos faz o papel do {@code SELECT ... FOR UPDATE}: os testes
 * de concorrencia sobre este dobro provam o servico e a logica de decisao, <strong>nao</strong> o bloqueio de linha
 * do Oracle (isso so o {@code AnaliseOracleTest} comprova).
 */
public final class AnaliseRepositoryMemoria implements AnaliseRepository {

    /** Analise gravada; so {@code vigente} muda (de verdadeiro para falso), como {@code RC_ANALISE.VIGENTE}. */
    private static final class Gravada {
        final long id;
        final long solicitacaoId;
        final int revisao;
        boolean vigente;
        final Instant executadaEm;
        final String protocoloIdentificador;
        final String protocoloVersao;
        final Instant conferenciaEm;
        final LocalDate dataReferencia;
        final ResultadoGeral resultadoGeral;
        final List<Long> documentos;
        final List<ResultadoGravado> resultados;
        final List<PendenciaGravada> pendencias;
        String resumoTexto;
        String resumoOrigem;

        Gravada(long id, long solicitacaoId, int revisao, Instant executadaEm, String protocoloIdentificador,
                String protocoloVersao, Instant conferenciaEm, LocalDate dataReferencia,
                ResultadoGeral resultadoGeral, List<Long> documentos, List<ResultadoGravado> resultados,
                List<PendenciaGravada> pendencias) {
            this.id = id;
            this.solicitacaoId = solicitacaoId;
            this.revisao = revisao;
            this.vigente = true;
            this.executadaEm = executadaEm;
            this.protocoloIdentificador = protocoloIdentificador;
            this.protocoloVersao = protocoloVersao;
            this.conferenciaEm = conferenciaEm;
            this.dataReferencia = dataReferencia;
            this.resultadoGeral = resultadoGeral;
            this.documentos = List.copyOf(documentos);
            this.resultados = List.copyOf(resultados);
            this.pendencias = List.copyOf(pendencias);
        }
    }

    private final SolicitacaoRepositoryMemoria pedidos;
    private final ExtracaoRepositoryMemoria extracoes;
    private final ConferenciaRepositoryMemoria conferencias;
    private final DocumentoRepositoryMemoria documentos;
    private final Map<Long, List<Gravada>> porPedido = new HashMap<>();
    private final Map<Long, Instant> emAndamento = new HashMap<>();
    private long proximoId = 1;
    private RuntimeException falhaNaPublicacao;
    private RuntimeException falhaNoEncerramento;
    private RuntimeException falhaNaConsulta;

    /**
     * @param pedidos repositorio de pedidos (bloqueio compartilhado, estado e revisao)
     * @param extracoes leituras (leitura da revisao atual)
     * @param conferencias decisoes (instante da conferencia)
     * @param documentos documentos (nome, tipo e disponibilidade dos documentos avaliados)
     */
    public AnaliseRepositoryMemoria(SolicitacaoRepositoryMemoria pedidos, ExtracaoRepositoryMemoria extracoes,
            ConferenciaRepositoryMemoria conferencias, DocumentoRepositoryMemoria documentos) {
        this.pedidos = pedidos;
        this.extracoes = extracoes;
        this.conferencias = conferencias;
        this.documentos = documentos;
        pedidos.usarConsultaDeVigente(this::resultadoVigenteDaRevisaoAtual);
        pedidos.aoInvalidarAnalise(this::invalidar);
        pedidos.registrarLimpeza(id -> {
            synchronized (pedidos) {
                porPedido.remove(id);
                emAndamento.remove(id);
            }
        });
    }

    @Override
    public InicioAnalise iniciar(long pacienteId, long solicitacaoId, Instant agora, Duration limite) {
        synchronized (pedidos) {
            Optional<Solicitacao> encontrado = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (encontrado.isEmpty()) {
                return InicioAnalise.naoEncontrado();
            }
            Solicitacao pedido = encontrado.get();
            if (pedido.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                return InicioAnalise.submetido();
            }
            boolean interrompeu = false;
            if (pedido.estado() == EstadoSolicitacao.PROCESSANDO) {
                Instant desde = emAndamento.get(solicitacaoId);
                if (desde != null && agora.isBefore(desde.plus(limite))) {
                    return InicioAnalise.emAndamento(desde);
                }
                emAndamento.remove(solicitacaoId);
                pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.FALHA_ANALISE, agora);
                interrompeu = true;
            }
            Optional<Gravada> vigente = vigenteDaRevisao(solicitacaoId, pedido.revisaoAtual());
            if (vigente.isPresent()) {
                return InicioAnalise.vigente(montar(vigente.get()), interrompeu);
            }
            if (extracoes.daRevisao(solicitacaoId, pedido.revisaoAtual()).isEmpty()) {
                return InicioAnalise.semLeituraAtual(interrompeu);
            }
            emAndamento.put(solicitacaoId, agora);
            pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.PROCESSANDO, agora);
            return InicioAnalise.iniciada(agora, interrompeu);
        }
    }

    @Override
    public PublicacaoAnalise publicar(long pacienteId, long solicitacaoId, Instant iniciadaEm,
            AnaliseParaGravar analise, Instant agora, Duration limite) {
        synchronized (pedidos) {
            if (falhaNaPublicacao != null) {
                RuntimeException falha = falhaNaPublicacao;
                falhaNaPublicacao = null;
                throw falha;
            }
            Optional<Solicitacao> encontrado = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (encontrado.isEmpty()) {
                return PublicacaoAnalise.naoEncontrado();
            }
            Solicitacao pedido = encontrado.get();
            if (pedido.estado() != EstadoSolicitacao.PROCESSANDO
                    || !iniciadaEm.equals(emAndamento.get(solicitacaoId))) {
                return PublicacaoAnalise.substituida();
            }
            if (!agora.isBefore(iniciadaEm.plus(limite))) {
                emAndamento.remove(solicitacaoId);
                pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.FALHA_ANALISE, agora);
                return PublicacaoAnalise.prazoEsgotado();
            }
            Optional<Extracao> leitura = extracoes.daRevisao(solicitacaoId, analise.revisao());
            if (pedido.revisaoAtual() != analise.revisao() || leitura.isEmpty()
                    || leitura.get().id() != analise.extracaoId()
                    || !conferenciaEm(solicitacaoId, leitura.get()).equals(analise.conferenciaEm())) {
                descartar(solicitacaoId, agora);
                return PublicacaoAnalise.entradaMudou();
            }
            List<Gravada> doPedido = porPedido.computeIfAbsent(solicitacaoId, id -> new ArrayList<>());
            for (Gravada existente : doPedido) {
                if (existente.revisao == analise.revisao() && analise.conferenciaEm().equals(existente.conferenciaEm)) {
                    // Equivale a RC_ANALISE_UK_CONFERENCIA: nao deveria acontecer sob a trava.
                    throw new IllegalStateException("Analise repetida para a mesma revisao e conferencia.");
                }
            }
            doPedido.forEach(existente -> existente.vigente = false);
            List<Long> avaliados = new ArrayList<>();
            for (ExtracaoDocumento documento : leitura.get().documentos()) {
                avaliados.add(documento.documentoId());
            }
            Gravada gravada = new Gravada(proximoId++, solicitacaoId, analise.revisao(), agora,
                    pedido.protocolo().identificador(), pedido.protocolo().versao(), analise.conferenciaEm(),
                    analise.dataReferencia(), analise.avaliacao().resultadoGeral(), avaliados,
                    resultados(analise.avaliacao().resultados()), pendencias(analise.avaliacao().pendencias()));
            doPedido.add(gravada);
            emAndamento.remove(solicitacaoId);
            pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.valueOf(gravada.resultadoGeral.name()), agora);
            return PublicacaoAnalise.publicada(montar(gravada));
        }
    }

    @Override
    public boolean encerrar(long pacienteId, long solicitacaoId, Instant iniciadaEm, Encerramento tipo,
            Instant agora) {
        synchronized (pedidos) {
            if (falhaNoEncerramento != null) {
                RuntimeException falha = falhaNoEncerramento;
                falhaNoEncerramento = null;
                throw falha;
            }
            Optional<Solicitacao> pedido = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (pedido.isEmpty() || pedido.get().estado() != EstadoSolicitacao.PROCESSANDO
                    || !iniciadaEm.equals(emAndamento.get(solicitacaoId))) {
                return false;
            }
            if (tipo == Encerramento.FALHA) {
                emAndamento.remove(solicitacaoId);
                pedidos.alterarEstado(solicitacaoId, EstadoSolicitacao.FALHA_ANALISE, agora);
            } else {
                descartar(solicitacaoId, agora);
            }
            return true;
        }
    }

    @Override
    public Optional<HistoricoDeAnalises> historico(long pacienteId, long solicitacaoId, int limite) {
        synchronized (pedidos) {
            if (falhaNaConsulta != null) {
                RuntimeException falha = falhaNaConsulta;
                falhaNaConsulta = null;
                throw falha;
            }
            Optional<Solicitacao> encontrado = pedidos.buscarDaPaciente(solicitacaoId, pacienteId);
            if (encontrado.isEmpty()) {
                return Optional.empty();
            }
            Solicitacao pedido = encontrado.get();
            List<Analise> analises = new ArrayList<>();
            List<Gravada> doPedido = porPedido.getOrDefault(solicitacaoId, List.of());
            for (int i = doPedido.size() - 1; i >= 0 && analises.size() < limite; i--) {
                analises.add(montar(doPedido.get(i)));
            }
            Instant iniciada = pedido.estado() == EstadoSolicitacao.PROCESSANDO ? emAndamento.get(solicitacaoId) : null;
            return Optional.of(new HistoricoDeAnalises(pedido.estado(), pedido.revisaoAtual(), iniciada, analises));
        }
    }

    @Override
    public Optional<Analise> guardarResumo(long pacienteId, long solicitacaoId, long analiseId, String texto,
            String origem) {
        synchronized (pedidos) {
            if (pedidos.buscarDaPaciente(solicitacaoId, pacienteId).isEmpty()) {
                return Optional.empty();
            }
            for (Gravada gravada : porPedido.getOrDefault(solicitacaoId, List.of())) {
                if (gravada.id == analiseId && gravada.vigente) {
                    if (!"ADAPTADOR_MOCKADO".equals(gravada.resumoOrigem)) {
                        gravada.resumoTexto = texto;
                        gravada.resumoOrigem = origem;
                    }
                    return Optional.of(montar(gravada));
                }
            }
            return Optional.empty();
        }
    }

    /** @param falha excecao que a proxima {@link #publicar} lanca antes de qualquer alteracao */
    public void falharNaProximaPublicacao(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNaPublicacao = falha;
        }
    }

    /** @param falha excecao que o proximo {@link #encerrar} lanca antes de qualquer alteracao */
    public void falharNoProximoEncerramento(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNoEncerramento = falha;
        }
    }

    /** @param falha excecao que a proxima {@link #historico} lanca */
    public void falharNaProximaConsulta(RuntimeException falha) {
        synchronized (pedidos) {
            this.falhaNaConsulta = falha;
        }
    }

    /**
     * @param solicitacaoId pedido
     * @return quantas analises o pedido tem gravadas (vigentes ou nao)
     */
    public int totalDoPedido(long solicitacaoId) {
        synchronized (pedidos) {
            return porPedido.getOrDefault(solicitacaoId, List.of()).size();
        }
    }

    /**
     * @param solicitacaoId pedido
     * @return quantas analises vigentes o pedido tem (o invariante e no maximo uma)
     */
    public long vigentesDoPedido(long solicitacaoId) {
        synchronized (pedidos) {
            return porPedido.getOrDefault(solicitacaoId, List.of()).stream().filter(g -> g.vigente).count();
        }
    }

    /**
     * @param solicitacaoId pedido
     * @return inicio da analise em andamento, se houver
     */
    public Optional<Instant> iniciadaEm(long solicitacaoId) {
        synchronized (pedidos) {
            return Optional.ofNullable(emAndamento.get(solicitacaoId));
        }
    }

    private void invalidar(long solicitacaoId) {
        synchronized (pedidos) {
            porPedido.getOrDefault(solicitacaoId, List.of()).forEach(g -> g.vigente = false);
        }
    }

    private ResultadoGeral resultadoVigenteDaRevisaoAtual(long solicitacaoId) {
        // Chamado de dentro do repositorio de pedidos (mesmo monitor), com o pedido ja lido.
        return porPedido.getOrDefault(solicitacaoId, List.of()).stream().filter(g -> g.vigente)
                .filter(g -> g.revisao == revisaoAtual(solicitacaoId)).map(g -> g.resultadoGeral).findFirst()
                .orElse(null);
    }

    private int revisaoAtual(long solicitacaoId) {
        return pedidos.revisoesDe(solicitacaoId).stream().mapToInt(SolicitacaoRepositoryMemoria.RevisaoRegistrada::numero)
                .max().orElse(0);
    }

    private Optional<Gravada> vigenteDaRevisao(long solicitacaoId, int revisao) {
        return porPedido.getOrDefault(solicitacaoId, List.of()).stream().filter(g -> g.vigente && g.revisao == revisao)
                .findFirst();
    }

    /** Mesma definicao de {@code ConferenciaService.visao}: ultima decisao ou, sem decisao, o instante da leitura. */
    private Instant conferenciaEm(long solicitacaoId, Extracao leitura) {
        return conferencias.decisoesDa(solicitacaoId, leitura.id()).stream().map(DecisaoConferencia::decididaEm)
                .max(Instant::compareTo).orElse(leitura.criadaEm());
    }

    private void descartar(long solicitacaoId, Instant agora) {
        emAndamento.remove(solicitacaoId);
        List<Gravada> doPedido = porPedido.getOrDefault(solicitacaoId, List.of());
        EstadoSolicitacao estado = doPedido.isEmpty() ? EstadoSolicitacao.RASCUNHO
                : EstadoSolicitacao.valueOf(doPedido.get(doPedido.size() - 1).resultadoGeral.name());
        pedidos.alterarEstado(solicitacaoId, estado, agora);
    }

    private Analise montar(Gravada gravada) {
        Map<Long, Documento> disponiveis = new LinkedHashMap<>();
        documentos.todos().forEach(d -> disponiveis.put(d.id(), d));
        Map<Long, Documento> removidos = new LinkedHashMap<>();
        documentos.removidos().forEach(d -> removidos.put(d.id(), d));
        List<DocumentoDaAnalise> avaliados = new ArrayList<>();
        for (long id : gravada.documentos) {
            Documento documento = disponiveis.containsKey(id) ? disponiveis.get(id) : removidos.get(id);
            avaliados.add(new DocumentoDaAnalise(id, documento.nomeArquivo(), documento.tipoDocumental(),
                    disponiveis.containsKey(id)));
        }
        return new Analise(gravada.id, gravada.revisao, gravada.vigente, gravada.executadaEm,
                gravada.protocoloIdentificador, gravada.protocoloVersao, gravada.conferenciaEm,
                gravada.dataReferencia, gravada.resultadoGeral, avaliados, gravada.resultados, gravada.pendencias,
                gravada.resumoTexto, gravada.resumoOrigem);
    }

    private static List<ResultadoGravado> resultados(List<ResultadoDeRegra> resultados) {
        List<ResultadoGravado> gravados = new ArrayList<>();
        for (ResultadoDeRegra resultado : resultados) {
            gravados.add(new ResultadoGravado(resultado.regra().codigo(), resultado.regra().descricao(),
                    resultado.resultado(), resultado.veredito().motivo()));
        }
        return gravados;
    }

    private static List<PendenciaGravada> pendencias(List<Pendencia> pendencias) {
        List<PendenciaGravada> gravadas = new ArrayList<>();
        for (Pendencia pendencia : pendencias) {
            List<EvidenciaGravada> evidencias = new ArrayList<>();
            for (Evidencia evidencia : pendencia.evidencias()) {
                evidencias.add(new EvidenciaGravada(evidencia.documentoId(), evidencia.codigo(), evidencia.situacao(),
                        evidencia.pagina(), evidencia.trecho()));
            }
            gravadas.add(new PendenciaGravada(pendencia.codigoRegra(), pendencia.descricaoRegra(), pendencia.tipo(),
                    pendencia.explicacao(), pendencia.motivo(), pendencia.responsavel(), evidencias));
        }
        return gravadas;
    }
}
