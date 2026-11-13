package br.com.regulacheck.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.RelogioFixo;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * FR-012 / SC-005 sobre o <strong>dobro em memoria</strong>: prova a logica de decisao do Service e do contrato do
 * repositorio sob varias threads. Nao prova o bloqueio de linha ({@code FOR UPDATE}) do Oracle: isso e papel do
 * teste opt-in {@code SolicitacaoOracleTest}.
 */
class ConcorrenciaTrocaServicoTest {

    static final String V01 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final String V02 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.2";
    static final SessaoAutenticada A =
            new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE, Instant.parse("2026-11-12T22:00:00Z"));

    @Test
    void trocasSimultaneasNaoGeramSaltoNemRepeticaoDeRevisao() throws Exception {
        ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
        SolicitacaoRepositoryMemoria repositorio = new SolicitacaoRepositoryMemoria();
        SolicitacaoService servico = new SolicitacaoService(new ProtocoloService(protocolos), repositorio,
                new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z")));
        Solicitacao rascunho = servico.criar(A, V01);

        int threads = 16;
        int tentativasPorThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger conflitos = new AtomicInteger();
        List<Future<?>> trabalhos = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int alvoInicial = t;
            trabalhos.add(pool.submit(() -> {
                largada.await();
                for (int i = 0; i < tentativasPorThread; i++) {
                    String alvo = (alvoInicial + i) % 2 == 0 ? V01 : V02;
                    try {
                        servico.trocarProtocolo(A, rascunho.id(), alvo);
                    } catch (ExcecaoNegocio e) {
                        assertEquals(409, e.status(), "unico erro admitido depois que o pedido sai de rascunho");
                        conflitos.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        largada.countDown();
        Thread.sleep(5);
        repositorio.forcarEstado(rascunho.id(), EstadoSolicitacao.PROCESSANDO);
        int revisaoAoSairDeRascunho = servico.buscarPropria(A, rascunho.id()).revisaoAtual();
        for (Future<?> trabalho : trabalhos) {
            trabalho.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        Solicitacao aoFinal = servico.buscarPropria(A, rascunho.id());
        List<RevisaoRegistrada> historico = repositorio.revisoesDe(rascunho.id());
        // Sem salto nem repeticao: 1, 2, 3, ... n, exatamente uma linha por numero.
        for (int i = 0; i < historico.size(); i++) {
            assertEquals(i + 1, historico.get(i).numero());
        }
        assertEquals(aoFinal.revisaoAtual(), historico.size());
        assertEquals(MotivoRevisao.CRIACAO, historico.get(0).motivo());
        historico.stream().skip(1).forEach(r -> assertEquals(MotivoRevisao.PROTOCOLO_ALTERADO, r.motivo()));
        // Nenhuma troca foi aplicada depois que o pedido deixou de ser rascunho.
        assertEquals(revisaoAoSairDeRascunho, aoFinal.revisaoAtual());
        assertTrue(aoFinal.revisaoAtual() >= 1);
        assertTrue(conflitos.get() > 0 || aoFinal.revisaoAtual() > 1, "o cenario precisa ter exercitado a concorrencia");
    }
}
