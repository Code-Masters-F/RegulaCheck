package br.com.regulacheck.submissao;

import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.historico;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioConferencia;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Conclusao da simulacao pela rota real (RC-17, US1 e US3; FR-001 a FR-005, FR-008, FR-009, FR-011; SC-001, SC-003,
 * SC-004), com repositorios em memoria e o caso sintetico 1 (encaminhamento completo e identificacao, tudo conferido e
 * analisado sem pendencias).
 */
class SubmeterEndpointTest {

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    static Resposta submeter(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.enviar("POST", "/solicitacoes/" + pedido + "/submissoes-simuladas", null, auth);
    }

    static Resposta comprovante(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.get("/solicitacoes/" + pedido + "/submissoes-simuladas", auth);
    }

    @Test
    void pedidoSemPendenciasEConcluidoComComprovanteCompleto() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode analise = historico(c, p.id()).get("analises").get(0);
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", analise.get("resultadoGeral").asText());

        Resposta r = submeter(c, c.authA, p.id());

        assertEquals(201, r.status(), r.corpo());
        JsonNode corpo = r.json();
        assertEquals(p.id(), corpo.get("solicitacaoId").asText());
        assertEquals(String.format("SIM-2026-%06d", p.numero()), corpo.get("referencia").asText());
        assertEquals("2026-11-12T20:00:00Z", corpo.get("submetidaEm").asText(), "relogio do cenario, em UTC");
        assertTrue(corpo.get("simulada").asBoolean());
        assertEquals("SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", corpo.get("destaque").asText());
        assertEquals("Município Exemplo", corpo.get("servico").get("localidade").asText());
        assertEquals("Consulta ambulatorial em cardiologia", corpo.get("servico").get("especialidade").asText());
        assertTrue(corpo.get("servico").has("avisoFicticio"));
        JsonNode versao = corpo.get("versaoAnalisada");
        assertEquals(analise.get("id").asText(), versao.get("analiseId").asText());
        assertEquals(analise.get("revisao").asInt(), versao.get("revisao").asInt());
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO", versao.get("protocolo").get("identificador").asText());
        assertEquals("v0.1", versao.get("protocolo").get("versao").asText());
        assertTrue(corpo.get("nota").asText().contains("não é protocolo oficial"));
        assertTrue(corpo.get("nota").asText().contains("não coloca você em fila real"));
        assertEquals(EstadoSolicitacao.SUBMETIDA_SIMULACAO,
                c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado());
        assertEquals("SUBMETIDA_SIMULACAO", c.api.get("/solicitacoes/" + p.id(), c.authA).json().get("estado")
                .asText());
        assertEquals(1, c.cenario.submissoes.total());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "concluir nao cria analise");
    }

    @Test
    void depoisDaSubmissaoOPedidoESoConsulta() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        String leitura = c.consultarLeitura(c.authA, p.id()).json().get("id").asText();
        assertEquals(201, submeter(c, c.authA, p.id()).status());
        String base = "/solicitacoes/" + p.id();

        List<Resposta> alteracoes = List.of(
                c.enviar(c.authA, p.id(), "OUTRO", "n.pdf", ArquivosDeTeste.pdf(1), null),
                c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO", "n.pdf",
                        ArquivosDeTeste.pdf(1), null),
                c.remover(c.authA, p.id(), p.documentos().get(1)),
                c.lerDocumentos(c.authA, p.id()),
                c.decidir(c.authA, p.id(), leitura,
                        CenarioConferencia.lote(CenarioConferencia.confirmar(p.documentos().get(0), "NOME"))),
                c.api.enviar("POST", base + "/analises", null, c.authA),
                c.api.enviar("POST", base + "/reenvio", null, c.authA),
                c.api.enviar("POST", base + "/sessoes-captura", null, c.authA));

        for (Resposta r : alteracoes) {
            assertEquals(409, r.status(), r.corpo());
            assertEquals("Este pedido já foi submetido e só pode ser consultado.", r.json().get("mensagem").asText());
        }
        for (String consulta : List.of(base, base + "/documentos", base + "/extracoes", base + "/analises",
                base + "/submissoes-simuladas")) {
            assertEquals(200, c.api.get(consulta, c.authA).status(), consulta);
        }
        assertEquals(1, c.cenario.submissoes.total());
    }

    @Test
    void repetirDevolveOMesmoComprovanteEAConsultaTambem() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        Resposta primeira = submeter(c, c.authA, p.id());
        c.cenario.relogio.avancar(Duration.ofMinutes(5));

        Resposta repetida = submeter(c, c.authA, p.id());
        Resposta consultada = comprovante(c, c.authA, p.id());

        assertEquals(201, primeira.status());
        assertEquals(200, repetida.status(), repetida.corpo());
        assertEquals(primeira.json(), repetida.json(), "mesma referencia e mesma data");
        assertEquals(200, consultada.status(), consultada.corpo());
        assertEquals(primeira.json(), consultada.json());
        assertEquals(1, c.cenario.submissoes.total());
    }

    @Test
    void consultarAntesDaSubmissaoEOMesmo404DePedidoInexistente() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);

        Resposta naoSubmetido = comprovante(c, c.authA, p.id());
        Resposta inexistente = comprovante(c, c.authA, "999999");

        assertEquals(404, naoSubmetido.status());
        assertEquals(inexistente.corpo(), naoSubmetido.corpo());
    }

    @Test
    void corpoEnviadoNaConclusaoEIgnorado() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        String corpo = "{\"referencia\":\"PROTOCOLO-OFICIAL-123\",\"pacienteId\":\"2\",\"estado\":\"APROVADO\"}";

        Resposta r = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/submissoes-simuladas", corpo, c.authA);

        assertEquals(201, r.status(), r.corpo());
        assertEquals(String.format("SIM-2026-%06d", p.numero()), r.json().get("referencia").asText());
    }

    @Test
    void dezConclusoesSimultaneasRegistramUmaSoSubmissao() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        ExecutorService threads = Executors.newFixedThreadPool(10);
        List<Future<Resposta>> respostas = new ArrayList<>();
        int criadas = 0;
        Set<String> referencias = new HashSet<>();
        try {
            CountDownLatch largada = new CountDownLatch(1);
            for (int i = 0; i < 10; i++) {
                respostas.add(threads.submit(() -> {
                    largada.await();
                    return submeter(c, c.authA, p.id());
                }));
            }
            largada.countDown();
            for (Future<Resposta> futura : respostas) {
                Resposta r = futura.get(30, TimeUnit.SECONDS);
                assertTrue(List.of(200, 201).contains(r.status()), r.status() + " " + r.corpo());
                criadas += r.status() == 201 ? 1 : 0;
                referencias.add(r.json().get("referencia").asText());
            }
        } finally {
            respostas.forEach(futura -> futura.cancel(true));
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(30, TimeUnit.SECONDS), "threads do teste nao terminaram");
        }

        assertEquals(1, criadas);
        assertEquals(1, referencias.size(), "todas as respostas trazem a mesma referencia");
        assertEquals(1, c.cenario.submissoes.total());
    }
}
