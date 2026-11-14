package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoComIntervalo;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAnalise.servico;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Pedidos de analise repetidos e simultaneos (US4; FR-004, FR-015, FR-016; SC-004, SC-005). O {@code synchronized}
 * dos dobros em memoria faz o papel do {@code SELECT ... FOR UPDATE}: prova a logica do servico, nao o bloqueio de
 * linha do Oracle.
 */
class ConcorrenciaAnaliseTest {

    @Test
    void dezPedidosSimultaneosGravamUmaUnicaAnalise() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            confirmarTudo(c, p);
            ExecutorService threads = Executors.newFixedThreadPool(10);
            List<Future<Resposta>> respostas = new ArrayList<>();
            int criadas = 0;
            Set<String> ids = new HashSet<>();
            try {
                CountDownLatch largada = new CountDownLatch(1);
                for (int i = 0; i < 10; i++) {
                    respostas.add(threads.submit(() -> {
                        largada.await();
                        return pedir(c, c.authA, p.id());
                    }));
                }
                largada.countDown();
                for (Future<Resposta> futura : respostas) {
                    Resposta r = futura.get(30, TimeUnit.SECONDS);
                    assertTrue(List.of(200, 201, 409).contains(r.status()), r.status() + " " + r.corpo());
                    if (r.status() == 201) {
                        criadas++;
                    }
                    if (r.status() == 409) {
                        assertEquals("ANALISE_EM_ANDAMENTO", r.json().get("codigo").asText());
                    } else {
                        ids.add(r.json().get("id").asText());
                    }
                }
            } finally {
                encerrar(threads, respostas);
            }

            assertEquals(1, criadas);
            assertEquals(1, ids.size(), "todas as respostas de sucesso trazem a mesma analise");
            assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
            assertEquals(1, c.cenario.analises.vigentesDoPedido(p.numero()));
            assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS,
                    c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado());
        }
    }

    /** Cancela o que nao terminou (falha no meio do teste) e espera, com prazo, as threads acabarem. */
    static void encerrar(ExecutorService threads, List<? extends Future<?>> pendentes) throws InterruptedException {
        pendentes.forEach(futura -> futura.cancel(true));
        threads.shutdownNow();
        assertTrue(threads.awaitTermination(30, TimeUnit.SECONDS), "threads do teste nao terminaram");
    }

    @Test
    void segundoPedidoDuranteOPrimeiroERecusadoComoEmAndamento() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            List<Resposta> duranteOPrimeiro = new ArrayList<>();
            var primeiro = servico(c, avaliacaoComIntervalo(c, () -> duranteOPrimeiro.add(pedir(c, c.authA, p.id()))))
                    .analisar(PACIENTE_A, p.numero());

            Resposta segundo = duranteOPrimeiro.get(0);
            assertEquals(409, segundo.status());
            assertEquals("ANALISE_EM_ANDAMENTO", segundo.json().get("codigo").asText());
            assertEquals("60", segundo.cabecalho("Retry-After"), "relogio parado: falta o limite inteiro");
            assertTrue(primeiro.criada());
            assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
        }
    }
}
