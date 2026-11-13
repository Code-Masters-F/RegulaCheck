package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Concorrencia da leitura. O dobro em memoria usa {@code synchronized} no monitor dos pedidos: prova a logica
 * (uma leitura por versao; leitura gravada so se a versao capturada ainda e a atual), <strong>nao</strong> o
 * bloqueio de linha do Oracle, que so o teste opt-in contra o banco real comprova.
 */
class ConcorrenciaExtracaoTest {

    private static <T> List<T> executarJuntas(List<Callable<T>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<T>> futuros = new ArrayList<>();
            for (Callable<T> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            List<T> resultados = new ArrayList<>();
            for (Future<T> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void leiturasSimultaneasDaMesmaVersaoGravamUmaSoETodasRecebemAMesmaLeitura() throws Exception {
        LeitorFalso leitor = new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO,
                    AmostrasSinteticas.IDENTIFICACAO);
            List<Callable<Resposta>> tarefas = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tarefas.add(() -> c.lerDocumentos(c.authA, pedido));
            }

            List<Resposta> respostas = executarJuntas(tarefas);

            long criadas = respostas.stream().filter(r -> r.status() == 201).count();
            long repetidas = respostas.stream().filter(r -> r.status() == 200).count();
            assertEquals(1, criadas, "so uma chamada grava");
            assertEquals(11, repetidas);
            assertEquals(1, c.cenario.extracoes.totalDoPedido(Long.parseLong(pedido)));
            assertEquals(1, c.cenario.extracoes.gravacoes());
            Set<String> corpos = new TreeSet<>();
            respostas.forEach(r -> corpos.add(r.json().get("criadaEm").asText() + r.json().get("documentos")));
            assertEquals(1, corpos.size(), "todas recebem a mesma leitura");
        }
    }

    @Test
    void leituraConcorrenteComSubstituicaoNuncaGravaLeituraDeVersaoQueJaNaoEraAAtual() throws Exception {
        for (int rodada = 0; rodada < 15; rodada++) {
            LeitorFalso leitor = new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
            try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
                String pedido = c.criarRascunho(c.authA);
                String doc = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
                long id = Long.parseLong(pedido);
                List<Callable<Integer>> tarefas = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    tarefas.add(() -> c.lerDocumentos(c.authA, pedido).status());
                }
                tarefas.add(() -> c.substituir(c.authA, pedido, doc, "ENCAMINHAMENTO",
                        AmostrasSinteticas.SEM_MOTIVO.nome(), AmostrasSinteticas.SEM_MOTIVO.bytes(), null).status());

                List<Integer> status = executarJuntas(tarefas);

                for (int i = 0; i < 4; i++) {
                    assertTrue(Set.of(200, 201, 409).contains(status.get(i)), "status " + status.get(i));
                }
                assertEquals(201, status.get(4));
                // Invariante: toda leitura gravada corresponde exatamente aos documentos da versao em que foi
                // feita; a atual (se houver) corresponde aos documentos disponiveis agora.
                int revisaoAtual = c.cenario.solicitacoes.buscarDaPaciente(id, 1).orElseThrow().revisaoAtual();
                var maisRecente = c.cenario.extracoes.buscarMaisRecente(1, id);
                if (maisRecente.isPresent()) {
                    ExtracaoRegistrada registrada = maisRecente.get();
                    assertTrue(registrada.extracao().revisao() <= revisaoAtual);
                    if (registrada.atual()) {
                        Set<Long> disponiveis = new TreeSet<>();
                        for (Documento d : c.cenario.documentos.listarDoPedido(id, 1)) {
                            disponiveis.add(d.id());
                        }
                        Set<Long> lidos = new TreeSet<>();
                        registrada.extracao().documentos().forEach(d -> lidos.add(d.documentoId()));
                        assertEquals(disponiveis, lidos, "leitura atual cobre exatamente os documentos atuais");
                    }
                }
                assertTrue(c.cenario.extracoes.totalDoPedido(id) <= 2, "no maximo uma leitura por versao");
                c.conferirIntegridade();
            }
        }
    }
}
