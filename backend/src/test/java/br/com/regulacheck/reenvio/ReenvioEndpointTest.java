package br.com.regulacheck.reenvio;

import static br.com.regulacheck.reenvio.CenarioReenvio.algumaResolvida;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.confirmarTudo;
import static br.com.regulacheck.reenvio.CenarioReenvio.historico;
import static br.com.regulacheck.reenvio.CenarioReenvio.reenviar;
import static br.com.regulacheck.reenvio.CenarioReenvio.situacoes;
import static br.com.regulacheck.reenvio.CenarioReenvio.substituir;
import static br.com.regulacheck.reenvio.CenarioReenvio.verificarConferindo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Verificacao da versao corrigida pela rota real (RC-16, US1; FR-001 a FR-006, FR-009, FR-018; SC-001, SC-002,
 * SC-004, SC-006), com repositorios em memoria e leitor simulado. Casos sinteticos do protocolo academico ficticio:
 * encaminhamento sem motivo (RC-R04 pendente) corrigido pelo encaminhamento completo.
 */
class ReenvioEndpointTest {

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    @Test
    void percursoCompletoParaNaConferenciaEDepoisPublicaANovaAnaliseComComparacao() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode antes = historico(c, p.id()).get("analises").get(0);
        assertEquals("COM_PENDENCIAS", antes.get("resultadoGeral").asText());
        assertFalse(antes.has("comparacao"), "a primeira analise do pedido nao tem comparacao");

        substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.ENCAMINHAMENTO);
        JsonNode depoisDoUpload = historico(c, p.id());
        assertFalse(depoisDoUpload.has("vigenteId"), "o upload tira a vigencia");
        assertFalse(algumaResolvida(depoisDoUpload), "o upload nunca resolve pendencia (FR-009)");

        Resposta parada = reenviar(c, c.authA, p.id());
        assertEquals(200, parada.status(), parada.corpo());
        JsonNode corpoParada = parada.json();
        assertEquals("CONFERENCIA", corpoParada.get("etapa").asText());
        assertTrue(corpoParada.get("leituraCriada").asBoolean());
        assertTrue(corpoParada.get("camposSemDecisao").asInt() > 0);
        assertFalse(corpoParada.get("analiseCriada").asBoolean());
        assertFalse(corpoParada.has("analise"));
        assertTrue(corpoParada.get("simulada").asBoolean());
        assertTrue(corpoParada.get("orientacao").asText().contains("Nenhuma pendência foi marcada como resolvida"));
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "parar na conferencia nao registra analise");

        Resposta paradaDeNovo = reenviar(c, c.authA, p.id());
        assertEquals(200, paradaDeNovo.status());
        assertFalse(paradaDeNovo.json().get("leituraCriada").asBoolean(), "a leitura da revisao e reaproveitada");
        assertEquals(corpoParada.get("extracaoId"), paradaDeNovo.json().get("extracaoId"));

        confirmarTudo(c, p.id(), corpoParada.get("extracaoId").asText());
        Resposta concluida = reenviar(c, c.authA, p.id());

        assertEquals(201, concluida.status(), concluida.corpo());
        JsonNode corpo = concluida.json();
        assertEquals("CONCLUIDA", corpo.get("etapa").asText());
        assertTrue(corpo.get("analiseCriada").asBoolean());
        assertEquals(corpoParada.get("revisao"), corpo.get("revisao"));
        JsonNode nova = corpo.get("analise");
        assertEquals(corpo.get("revisao").asInt(), nova.get("revisao").asInt());
        assertTrue(nova.get("vigente").asBoolean());
        assertTrue(nova.get("simulada").asBoolean());
        assertTrue(nova.get("aviso").asText().contains("não é aprovação do SUS"));
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", nova.get("resultadoGeral").asText());
        assertTrue(nova.get("resumo").get("simulado").asBoolean());
        assertEquals("ADAPTADOR_MOCKADO", nova.get("resumo").get("origem").asText());
        assertEquals(antes.get("id").asText(), nova.get("comparacao").get("analiseAnteriorId").asText());
        assertEquals(antes.get("revisao").asInt(), nova.get("comparacao").get("revisaoAnterior").asInt());
        assertEquals(Map.of("RC-R04", "RESOLVIDA"), situacoes(nova));
        JsonNode item = nova.get("comparacao").get("itens").get(0);
        assertEquals("PENDENTE", item.get("resultadoAnterior").asText());
        assertEquals("ATENDE", item.get("resultadoAtual").asText());

        JsonNode depois = historico(c, p.id());
        assertEquals(2, depois.get("analises").size());
        assertEquals(nova.get("id"), depois.get("vigenteId"));
        JsonNode anterior = depois.get("analises").get(1);
        assertEquals(antes.get("id"), anterior.get("id"));
        assertFalse(anterior.get("vigente").asBoolean());
        assertEquals(antes.get("pendencias"), anterior.get("pendencias"), "o historico preserva a analise anterior");
        assertEquals(antes.get("resultados"), anterior.get("resultados"));
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS,
                c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado());
    }

    @Test
    void repetirSemMudancaDevolveAMesmaAnaliseSemNovaAnaliseNemNovoResumo() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.ENCAMINHAMENTO);
        JsonNode primeira = verificarConferindo(c, p.id()).json();

        Resposta repetida = reenviar(c, c.authA, p.id());

        assertEquals(200, repetida.status(), repetida.corpo());
        JsonNode corpo = repetida.json();
        assertEquals("CONCLUIDA", corpo.get("etapa").asText());
        assertFalse(corpo.get("analiseCriada").asBoolean());
        assertTrue(corpo.get("orientacao").asText().startsWith("Nada mudou"));
        assertEquals(primeira.get("analise").get("id"), corpo.get("analise").get("id"));
        assertEquals(primeira.get("analise").get("resumo"), corpo.get("analise").get("resumo"));
        assertEquals(primeira.get("analise").get("comparacao"), corpo.get("analise").get("comparacao"));
        assertEquals(2, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals(1, c.cenario.analises.vigentesDoPedido(p.numero()));
    }

    @Test
    void conferenciaComCampoIlegivelRegistraAAnaliseComNaoVerificaveisComoNovas() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, p.id(), p.documentos().get(1), AmostrasSinteticas.BORRADA);

        Resposta r = verificarConferindo(c, p.id());

        assertEquals(201, r.status(), r.corpo());
        JsonNode nova = r.json().get("analise");
        assertEquals("REQUER_CONFERENCIA", nova.get("resultadoGeral").asText());
        Map<String, String> situacoes = situacoes(nova);
        assertFalse(situacoes.isEmpty());
        assertFalse(situacoes.containsValue("RESOLVIDA"));
        assertTrue(situacoes.values().stream().allMatch("NOVA"::equals), situacoes.toString());
        Set<String> pendencias = new HashSet<>();
        nova.get("pendencias").forEach(pendencia -> pendencias.add(pendencia.get("codigoRegra").asText()));
        assertEquals(pendencias, situacoes.keySet(), "cada pendencia nova aparece na comparacao");
        assertEquals("NAO_VERIFICAVEL", nova.get("pendencias").get(0).get("tipo").asText());
    }

    @Test
    void campoSemDecisaoEmOutroDocumentoParaMesmoComDocumentoIlegivel() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, p.id(), p.documentos().get(1), AmostrasSinteticas.BORRADA);

        Resposta r = reenviar(c, c.authA, p.id());

        assertEquals(200, r.status(), r.corpo());
        assertEquals("CONFERENCIA", r.json().get("etapa").asText(),
                "a conferencia REQUER_CONFERENCIA (ilegivel) nao pode esconder campos ainda sem decisao");
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void pedidoAlheioInexistenteOuIlegivelTemOMesmo404() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);

        Resposta alheio = reenviar(c, c.authB, p.id());
        Resposta inexistente = reenviar(c, c.authB, "999999");
        Resposta ilegivel = c.api.enviar("POST", "/solicitacoes/abc/reenvio", null, c.authA);
        Resposta segmentoAMais = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/reenvio/1", null, c.authA);

        for (Resposta r : List.of(alheio, inexistente, ilegivel, segmentoAMais)) {
            assertEquals(404, r.status(), r.corpo());
            assertEquals(alheio.corpo(), r.corpo(), "corpo identico, sem detalhe de etapa");
        }
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void pedidoSubmetidoESoConsultaSemEtapa() throws Exception {
        String pedido = c.criarRascunho(c.authA);
        CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.SUBMETIDA_SIMULACAO);

        Resposta r = reenviar(c, c.authA, pedido);

        assertEquals(409, r.status(), r.corpo());
        assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
        assertEquals("Este pedido já foi submetido e só pode ser consultado.", r.json().get("mensagem").asText());
        assertFalse(r.json().has("detalhes"));
        assertEquals(404, c.consultarLeitura(c.authA, pedido).status(), "nada foi lido");
    }

    @Test
    void pedidoSemDocumentosOrientaEnviarNaEtapaLeitura() throws Exception {
        String pedido = c.criarRascunho(c.authA);

        Resposta r = reenviar(c, c.authA, pedido);

        assertEquals(422, r.status(), r.corpo());
        assertEquals("SEM_DOCUMENTOS", r.json().get("codigo").asText());
        assertTrue(r.json().get("mensagem").asText().contains("Envie o documento corrigido"));
        assertEquals("etapa", r.json().get("detalhes").get(0).get("campo").asText());
        assertEquals("LEITURA", r.json().get("detalhes").get(0).get("mensagem").asText());
    }

    @Test
    void metodoDiferenteDePostEh405() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);

        Resposta r = c.api.get("/solicitacoes/" + p.id() + "/reenvio", c.authA);

        assertEquals(405, r.status(), r.corpo());
    }

    @Test
    void corpoEnviadoEIgnorado() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        String corpoMalicioso = "{\"pacienteId\":\"2\",\"resultadoGeral\":\"SEM_PENDENCIAS_IDENTIFICADAS\"}";

        Resposta r = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/reenvio", corpoMalicioso, c.authA);

        assertEquals(200, r.status(), r.corpo());
        assertFalse(r.json().get("analiseCriada").asBoolean(), "nada mudou: devolve a vigente");
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "o corpo nao cria analise");
    }

    @Test
    void dezVerificacoesSimultaneasRegistramNoMaximoUmaAnaliseNova() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.ENCAMINHAMENTO);
        Resposta parada = reenviar(c, c.authA, p.id());
        confirmarTudo(c, p.id(), parada.json().get("extracaoId").asText());

        ExecutorService threads = Executors.newFixedThreadPool(10);
        List<Future<Resposta>> respostas = new ArrayList<>();
        int criadas = 0;
        Set<String> ids = new HashSet<>();
        try {
            CountDownLatch largada = new CountDownLatch(1);
            for (int i = 0; i < 10; i++) {
                respostas.add(threads.submit(() -> {
                    largada.await();
                    return reenviar(c, c.authA, p.id());
                }));
            }
            largada.countDown();
            for (Future<Resposta> futura : respostas) {
                Resposta r = futura.get(30, TimeUnit.SECONDS);
                assertTrue(List.of(200, 201, 409).contains(r.status()), r.status() + " " + r.corpo());
                if (r.status() == 409) {
                    assertEquals("ANALISE_EM_ANDAMENTO", r.json().get("codigo").asText());
                    assertEquals("ANALISE", r.json().get("detalhes").get(0).get("mensagem").asText());
                    continue;
                }
                if (r.status() == 201) {
                    criadas++;
                }
                ids.add(r.json().get("analise").get("id").asText());
            }
        } finally {
            respostas.forEach(futura -> futura.cancel(true));
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(30, TimeUnit.SECONDS), "threads do teste nao terminaram");
        }

        assertEquals(1, criadas);
        assertEquals(1, ids.size(), "todas as respostas de sucesso trazem a mesma analise");
        assertEquals(2, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals(1, c.cenario.analises.vigentesDoPedido(p.numero()));
    }
}
