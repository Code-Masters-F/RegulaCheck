package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** RC-21, clarify Q4 (FR-008): aberturas simultaneas do mesmo pedido criam exatamente uma sessao. */
class ConcorrenciaAberturaSessaoTest {

    private static final int THREADS = 10;

    @Test
    void dezAberturasSimultaneasCriamUmaSessao() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resposta>> futuros = new ArrayList<>();
            try {
                for (int i = 0; i < THREADS; i++) {
                    futuros.add(executor.submit(() -> {
                        largada.await();
                        return c.abrirSessao(c.authA, pedido);
                    }));
                }
                largada.countDown();
                int criadas = 0;
                Set<String> ids = new HashSet<>();
                for (Future<Resposta> futuro : futuros) {
                    Resposta r = futuro.get();
                    if (r.status() == 201) {
                        criadas++;
                    } else {
                        assertEquals(200, r.status());
                    }
                    ids.add(r.json().get("id").asText());
                }
                assertEquals(1, criadas);
                assertEquals(1, ids.size());
                assertEquals(1, c.cenario.capturas.sessoes().size());
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
