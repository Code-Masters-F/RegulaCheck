package br.com.regulacheck.extracao;

import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Concorrencia da conferencia, com o dobro em memoria e varias threads contra a API real. Estes testes provam a
 * <strong>logica</strong> (uma decisao vigente por campo, nada gravado para leitura que ja nao era a atual, lotes
 * disjuntos sem perda); o dobro imita o {@code SELECT ... FOR UPDATE} com {@code synchronized}, entao eles
 * <strong>nao</strong> provam o bloqueio de linha do Oracle (isso e do teste opt-in contra o banco real).
 */
class ConcorrenciaConferenciaTest {

    private static List<Resposta> emParalelo(List<Callable<Resposta>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Resposta>> futuros = new ArrayList<>();
            for (Callable<Resposta> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            List<Resposta> respostas = new ArrayList<>();
            for (Future<Resposta> futuro : futuros) {
                respostas.add(futuro.get());
            }
            return respostas;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void decisoesSimultaneasDoMesmoCampoDeixamUmaSoVigenteCoerenteComUmaDasEnviadas() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
            List<Callable<Resposta>> tarefas = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                String corpo = i % 2 == 0 ? lote(confirmar(p.encaminhamento(), "NOME"))
                        : lote(divergir(p.encaminhamento(), "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO"));
                tarefas.add(() -> c.decidir(c.authA, p.pedido(), p.extracao(), corpo));
            }

            for (Resposta r : emParalelo(tarefas)) {
                assertEquals(200, r.status(), r.corpo());
            }

            List<DecisaoConferencia> vigentes = c.cenario.conferencias.decisoesDa(Long.parseLong(p.pedido()),
                    Long.parseLong(p.extracao()));
            assertEquals(1, vigentes.size(), "uma decisao vigente por campo");
            String tipo = vigentes.get(0).tipo().name();
            assertTrue(tipo.equals("CONFIRMADA") || tipo.equals("DIVERGENTE"));
            // a decisao devolvida pela API e a que ficou gravada
            assertEquals(tipo, br.com.regulacheck.extracao.CenarioConferencia.campo(
                    br.com.regulacheck.extracao.CenarioConferencia.documento(
                            c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json(), p.encaminhamento()),
                    "NOME").get("decisao").get("tipo").asText());
        }
    }

    @Test
    void lotesSimultaneosDeCamposDiferentesNaoPerdemNenhumaDecisao() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
            String[] campos = {"TIPO_DOCUMENTO", "NOME", "DATA_NASCIMENTO", "MOTIVO_ENCAMINHAMENTO", "DATA_EMISSAO"};
            List<Callable<Resposta>> tarefas = new ArrayList<>();
            for (String campo : campos) {
                tarefas.add(() -> c.decidir(c.authA, p.pedido(), p.extracao(),
                        lote(confirmar(p.encaminhamento(), campo))));
            }

            for (Resposta r : emParalelo(tarefas)) {
                assertEquals(200, r.status(), r.corpo());
            }

            assertEquals(5, c.cenario.conferencias.total());
            assertEquals("CONFERIDA",
                    c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json().get("situacao").asText());
        }
    }

    @Test
    void decisaoConcorrenteComSubstituicaoDeDocumentoValeNaEpocaOuDa409SemGravar() throws Exception {
        for (int rodada = 0; rodada < 5; rodada++) {
            try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
                Preparo p = CenarioConferencia.preparar(c);
                List<Callable<Resposta>> tarefas = new ArrayList<>();
                for (String campo : new String[] {"TIPO_DOCUMENTO", "NOME", "DATA_NASCIMENTO", "DATA_EMISSAO"}) {
                    tarefas.add(() -> c.decidir(c.authA, p.pedido(), p.extracao(),
                            lote(confirmar(p.encaminhamento(), campo))));
                }
                tarefas.add(() -> c.substituir(c.authA, p.pedido(), p.encaminhamento(), "ENCAMINHAMENTO",
                        AmostrasSinteticas.SEM_MOTIVO.nome(), AmostrasSinteticas.SEM_MOTIVO.bytes(), null));

                List<Resposta> respostas = emParalelo(tarefas);

                int aceitas = 0;
                for (int i = 0; i < 4; i++) {
                    int status = respostas.get(i).status();
                    assertTrue(status == 200 || status == 409, "status inesperado " + status);
                    if (status == 200) {
                        aceitas++;
                    }
                }
                assertEquals(201, respostas.get(4).status(), respostas.get(4).corpo());
                // so as decisoes aceitas (200) existem: nenhuma foi gravada junto com um 409
                assertEquals(aceitas, c.cenario.conferencias.total(), "decisoes gravadas = decisoes aceitas");

                // depois da substituicao a leitura nao e mais a atual: nenhuma decisao nova entra
                int antes = c.cenario.conferencias.total();
                assertEquals(409, c.decidir(c.authA, p.pedido(), p.extracao(),
                        lote(confirmar(p.encaminhamento(), "MOTIVO_ENCAMINHAMENTO"))).status());
                assertEquals(antes, c.cenario.conferencias.total());
            }
        }
    }
}
