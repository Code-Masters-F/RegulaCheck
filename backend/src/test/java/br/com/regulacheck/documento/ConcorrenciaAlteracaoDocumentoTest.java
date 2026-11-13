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
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.service.DocumentoService;
import br.com.regulacheck.documento.service.DocumentoService.ResultadoEnvio;
import br.com.regulacheck.documento.service.DocumentoService.ResultadoRemover;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

/**
 * FR-019 / SC-004 / SC-005 sobre o <strong>dobro em memoria</strong>: remover, substituir, enviar e excluir ao
 * mesmo tempo no mesmo pedido, com arquivos reais numa pasta temporaria. Prova a logica de decisao do Service, a
 * compensacao de arquivos e o contrato do repositorio sob varias threads; <strong>nao</strong> prova o bloqueio de
 * linha do Oracle (papel do teste opt-in {@code AlteracaoDocumentoOracleTest}).
 */
class ConcorrenciaAlteracaoDocumentoTest {

    static final String V01 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final SessaoAutenticada A =
            new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE, Instant.parse("2026-11-12T22:00:00Z"));
    static final int THREADS = 20;

    private Path raiz;
    private SolicitacaoRepositoryMemoria pedidos;
    private DocumentoRepositoryMemoria documentos;
    private SolicitacaoService solicitacoes;
    private DocumentoService servico;
    private Solicitacao rascunho;
    private final List<Long> docs = new ArrayList<>();
    /** Gerado uma vez: o PDFBox grava datas no arquivo, entao dois pdf(1) nao tem os mesmos bytes. */
    private byte[] pdfNovo;

    @BeforeEach
    void preparar() throws Exception {
        raiz = Files.createTempDirectory("regulacheck-conc-alt-");
        ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        pedidos = new SolicitacaoRepositoryMemoria();
        documentos = new DocumentoRepositoryMemoria(pedidos);
        RelogioFixo relogio = new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z"));
        ArmazenamentoEmDisco armazenamento = new ArmazenamentoEmDisco(raiz);
        solicitacoes = new SolicitacaoService(new ProtocoloService(protocolos), pedidos, armazenamento, relogio);
        servico = new DocumentoService(solicitacoes, documentos, armazenamento, ConferenciaDeArquivo.para(raiz),
                relogio);
        rascunho = solicitacoes.criar(A, V01);
        for (int i = 1; i <= 3; i++) {
            ResultadoEnvio r = servico.enviar(A, rascunho.id(),
                    EnvioDocumento.validar("OUTRO", null, "d" + i + ".pdf", new ByteArrayInputStream(
                            ArquivosDeTeste.pdf(i))));
            docs.add(r.documento().id());
        }
        pdfNovo = ArquivosDeTeste.pdf(4);
    }

    @AfterEach
    void limpar() throws Exception {
        try (Stream<Path> arvore = Files.walk(raiz)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private ResultadoEnvio substituir(long documentoId) {
        return servico.substituir(A, rascunho.id(), documentoId,
                EnvioDocumento.validar("OUTRO", null, "n.pdf", new ByteArrayInputStream(pdfNovo)));
    }

    private ResultadoEnvio enviar() {
        return servico.enviar(A, rascunho.id(),
                EnvioDocumento.validar("OUTRO", null, "e.pdf", new ByteArrayInputStream(pdfNovo)));
    }

    /** Executa a tarefa em {@code THREADS} threads que largam juntas; devolve os resultados na ordem das threads. */
    private List<Object> largarJuntas(IntFunction<Callable<Object>> fabrica) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> trabalhos = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Callable<Object> tarefa = fabrica.apply(t);
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

    private static long contar(List<Object> resultados, Class<?> tipo) {
        return resultados.stream().filter(tipo::isInstance).count();
    }

    private void assertRevisoesSemSaltoNemRepeticao() {
        List<RevisaoRegistrada> historico = pedidos.revisoesDe(rascunho.id());
        for (int i = 0; i < historico.size(); i++) {
            assertEquals(i + 1, historico.get(i).numero(), "revisoes 1..n, sem salto nem repeticao");
        }
    }

    /** Arquivos na pasta privada = exatamente os dos documentos disponiveis (nenhum orfao). */
    private void assertSemOrfaos() throws Exception {
        Set<String> esperados = new TreeSet<>();
        for (Documento d : documentos.todos()) {
            esperados.add(d.caminhoInterno().valor());
        }
        Set<String> existentes = new TreeSet<>();
        try (Stream<Path> itens = Files.list(raiz)) {
            itens.filter(Files::isRegularFile).forEach(p -> existentes.add(p.getFileName().toString()));
        }
        assertEquals(esperados, existentes);
    }

    @RepeatedTest(3)
    void vinteRemocoesDoMesmoDocumentoRemovemUmaVezESemSaltoNaRevisao() throws Exception {
        long alvo = docs.get(0);

        List<Object> resultados = largarJuntas(t -> () -> servico.remover(A, rascunho.id(), alvo));

        assertEquals(1, contar(resultados, ResultadoRemover.class), "so uma remocao vale");
        resultados.stream().filter(r -> r instanceof ExcecaoNegocio)
                .forEach(r -> assertEquals(404, ((ExcecaoNegocio) r).status()));
        assertEquals(2, documentos.todos().size());
        assertEquals(5, pedidos.revisoesDe(rascunho.id()).size(), "criacao + 3 envios + 1 remocao");
        assertRevisoesSemSaltoNemRepeticao();
        assertSemOrfaos();
    }

    @RepeatedTest(3)
    void removerESubstituirOMesmoDocumentoAoMesmoTempoExatamenteUmaVence() throws Exception {
        long alvo = docs.get(0);

        List<Object> resultados = largarJuntas(
                t -> t % 2 == 0 ? () -> servico.remover(A, rascunho.id(), alvo) : () -> substituir(alvo));

        long vitorias = contar(resultados, ResultadoRemover.class) + contar(resultados, ResultadoEnvio.class);
        assertEquals(1, vitorias, "exatamente uma operacao altera o documento");
        resultados.stream().filter(r -> r instanceof ExcecaoNegocio)
                .forEach(r -> assertEquals(404, ((ExcecaoNegocio) r).status(), "a perdedora recebe 404"));
        assertEquals(5, pedidos.revisoesDe(rascunho.id()).size(), "uma unica revisao a mais");
        assertRevisoesSemSaltoNemRepeticao();
        assertTrue(documentos.todos().size() == 2 || documentos.todos().size() == 3);
        assertSemOrfaos();
    }

    @RepeatedTest(3)
    void removerEEnviarPertoDoLimiteNuncaPassaDeTresDocumentos() throws Exception {
        AtomicBoolean fim = new AtomicBoolean(false);
        AtomicInteger maximoVisto = new AtomicInteger();
        Thread observador = new Thread(() -> {
            while (!fim.get()) {
                maximoVisto.accumulateAndGet(documentos.todos().size(), Math::max);
            }
        });
        observador.start();
        try {
            largarJuntas(t -> t % 2 == 0 ? () -> enviar() : () -> servico.remover(A, rascunho.id(),
                    docs.get(t / 2 % docs.size())));
        } finally {
            fim.set(true);
            observador.join();
        }

        assertTrue(maximoVisto.get() <= 3, "nunca mais de 3 disponiveis; visto " + maximoVisto.get());
        assertTrue(documentos.todos().size() <= 3);
        assertRevisoesSemSaltoNemRepeticao();
        assertSemOrfaos();
    }

    @RepeatedTest(3)
    void substituirEExcluirOMesmoPedidoDeixaTudoLimpoSemArquivoOrfao() throws Exception {
        long alvo = docs.get(0);

        List<Object> resultados = largarJuntas(
                t -> t % 2 == 0 ? () -> {
                    solicitacoes.excluirRascunho(A, rascunho.id());
                    return "excluido";
                } : () -> substituir(alvo));

        assertEquals(1, resultados.stream().filter("excluido"::equals).count(), "a exclusao vale uma vez");
        assertTrue(contar(resultados, ResultadoEnvio.class) <= 1, "no maximo uma troca ocorreu antes da exclusao");
        assertTrue(pedidos.buscarDaPaciente(rascunho.id(), A.usuarioId()).isEmpty(), "o pedido nao existe mais");
        assertTrue(documentos.todos().isEmpty() && documentos.removidos().isEmpty());
        assertSemOrfaos();
        try (Stream<Path> itens = Files.list(raiz)) {
            assertEquals(0, itens.filter(Files::isRegularFile).count(), "nenhum arquivo do pedido restou");
        }
    }
}
