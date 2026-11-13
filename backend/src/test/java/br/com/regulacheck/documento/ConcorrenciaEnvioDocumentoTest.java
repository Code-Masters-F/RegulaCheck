package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArmazenamentoEmDisco;
import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.auth.RelogioFixo;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.dto.EnvioDocumento;
import br.com.regulacheck.documento.service.DocumentoService;
import br.com.regulacheck.documento.service.DocumentoService.ResultadoEnvio;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import br.com.regulacheck.usuario.entity.Perfil;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FR-012 / SC-004 sobre o <strong>dobro em memoria</strong>: prova a logica de decisao do Service e o contrato do
 * repositorio sob varias threads (arquivos reais numa pasta temporaria). Nao prova o bloqueio de linha do Oracle:
 * isso e papel do teste opt-in {@code DocumentoOracleTest}.
 */
class ConcorrenciaEnvioDocumentoTest {

    static final String V01 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final String V02 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.2";
    static final SessaoAutenticada A =
            new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE, Instant.parse("2026-11-12T22:00:00Z"));
    static final int THREADS = 20;

    private Path raiz;
    private SolicitacaoRepositoryMemoria pedidos;
    private DocumentoRepositoryMemoria documentos;
    private SolicitacaoService solicitacoes;
    private DocumentoService servico;
    private Solicitacao rascunho;
    /** Gerado uma vez: o PDFBox grava datas no arquivo, entao dois pdf(1) nao tem os mesmos bytes (nem o mesmo hash). */
    private byte[] pdf;

    @BeforeEach
    void preparar() throws Exception {
        raiz = Files.createTempDirectory("regulacheck-conc-");
        ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
        pedidos = new SolicitacaoRepositoryMemoria();
        documentos = new DocumentoRepositoryMemoria(pedidos);
        RelogioFixo relogio = new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z"));
        solicitacoes = new SolicitacaoService(new ProtocoloService(protocolos), pedidos, relogio);
        servico = new DocumentoService(solicitacoes, documentos, new ArmazenamentoEmDisco(raiz),
                ConferenciaDeArquivo.para(raiz), relogio);
        rascunho = solicitacoes.criar(A, V01);
        pdf = ArquivosDeTeste.pdf(1);
    }

    @AfterEach
    void limpar() throws Exception {
        try (Stream<Path> arvore = Files.walk(raiz)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private ResultadoEnvio enviar(String chave) {
        EnvioDocumento envio = EnvioDocumento.validar("OUTRO", chave, "a.pdf",
                new ByteArrayInputStream(pdf));
        return servico.enviar(A, rascunho.id(), envio);
    }

    private long arquivosGuardados() throws Exception {
        try (Stream<Path> itens = Files.list(raiz)) {
            return itens.filter(Files::isRegularFile).count();
        }
    }

    /** Executa a tarefa em {@code THREADS} threads que largam juntas; devolve os resultados na ordem das threads. */
    private List<Object> largarJuntas(java.util.function.IntFunction<java.util.concurrent.Callable<Object>> fabrica)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> trabalhos = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            java.util.concurrent.Callable<Object> tarefa = fabrica.apply(t);
            trabalhos.add(pool.submit(() -> {
                largada.await();
                try {
                    return tarefa.call();
                } catch (ExcecaoNegocio negocio) {
                    return negocio;
                }
            }));
        }
        largada.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<Object> trabalho : trabalhos) {
            resultados.add(trabalho.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        return resultados;
    }

    @Test
    void vinteEnviosSimultaneosNuncaPassamDeTresDocumentosESemSaltoNaRevisao() throws Exception {
        List<Object> resultados = largarJuntas(t -> () -> enviar(null));

        long aceitos = resultados.stream().filter(r -> r instanceof ResultadoEnvio).count();
        List<ExcecaoNegocio> recusas = resultados.stream().filter(r -> r instanceof ExcecaoNegocio)
                .map(r -> (ExcecaoNegocio) r).toList();
        assertEquals(3, aceitos, "exatamente tres envios entram");
        assertEquals(THREADS - 3, recusas.size());
        recusas.forEach(e -> assertEquals(422, e.status(), "unica recusa admitida: limite de documentos"));
        assertEquals(3, documentos.todos().size());
        assertEquals(3, arquivosGuardados(), "so ficam os arquivos dos tres aceitos; os demais foram removidos");
        List<RevisaoRegistrada> historico = pedidos.revisoesDe(rascunho.id());
        for (int i = 0; i < historico.size(); i++) {
            assertEquals(i + 1, historico.get(i).numero(), "revisoes 1..n, sem salto nem repeticao");
        }
        assertEquals(4, historico.size());
        assertEquals(MotivoRevisao.CRIACAO, historico.get(0).motivo());
        historico.stream().skip(1).forEach(r -> assertEquals(MotivoRevisao.DOCUMENTO_ENVIADO, r.motivo()));
    }

    @Test
    void vinteEnviosSimultaneosComAMesmaChaveGeramUmDocumentoEUmaRevisao() throws Exception {
        List<Object> resultados = largarJuntas(t -> () -> enviar("chave-compartilhada-0001"));

        List<ResultadoEnvio> envios = resultados.stream().filter(r -> r instanceof ResultadoEnvio)
                .map(r -> (ResultadoEnvio) r).toList();
        assertEquals(THREADS, envios.size(), "toda tentativa recebe resposta de sucesso");
        assertEquals(1, envios.stream().filter(e -> !e.repetido()).count(), "so um envio grava");
        assertEquals(1, envios.stream().map(e -> e.documento().id()).distinct().count(), "todos veem o mesmo documento");
        assertEquals(1, documentos.todos().size());
        assertEquals(2, pedidos.revisoesDe(rascunho.id()).size());
        assertEquals(1, arquivosGuardados(), "os arquivos das tentativas repetidas foram removidos");
    }

    @Test
    void enviosSimultaneosContraTrocasDeServicoMantemRevisoesCoerentes() throws Exception {
        AtomicInteger contador = new AtomicInteger();
        List<Object> resultados = largarJuntas(t -> () -> {
            if (t % 2 == 0) {
                return enviar(null);
            }
            return solicitacoes.trocarProtocolo(A, rascunho.id(), contador.getAndIncrement() % 2 == 0 ? V02 : V01);
        });

        assertEquals(THREADS, resultados.size());
        List<RevisaoRegistrada> historico = pedidos.revisoesDe(rascunho.id());
        for (int i = 0; i < historico.size(); i++) {
            assertEquals(i + 1, historico.get(i).numero(), "revisoes 1..n, sem salto nem repeticao");
        }
        Solicitacao fim = solicitacoes.buscarPropria(A, rascunho.id());
        assertEquals(fim.revisaoAtual(), historico.size());
        assertTrue(documentos.todos().size() <= 3);
        assertEquals(fim.documentosDisponiveis(), documentos.todos().size());
        assertEquals(documentos.todos().size(), arquivosGuardados());
    }
}
