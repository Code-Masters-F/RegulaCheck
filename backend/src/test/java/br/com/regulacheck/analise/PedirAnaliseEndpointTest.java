package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedido;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAnalise.pendencia;
import static br.com.regulacheck.analise.CenarioAnalise.resultado;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pedir a analise pela API (US1; FR-001 a FR-009, FR-021, FR-022, FR-024; SC-001, SC-005). Gabarito independente em
 * {@code specs/014-persistir-analises-historico/quickstart.md}. Relogio do cenario: 2026-11-12T20:00:00Z.
 */
class PedirAnaliseEndpointTest {

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
    void amostrasConferidasGravamAnaliseSemPendenciasEMudamOEstado() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);

        Resposta r = pedir(c, c.authA, p.id());

        assertEquals(201, r.status(), r.corpo());
        JsonNode a = r.json();
        assertTrue(a.get("vigente").asBoolean());
        assertEquals(3, a.get("revisao").asInt()); // criacao + dois envios
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO", a.get("protocolo").get("identificador").asText());
        assertEquals("v0.1", a.get("protocolo").get("versao").asText());
        assertEquals("2026-11-12T20:00:00Z", a.get("executadaEm").asText());
        assertEquals("2026-11-12T20:00:00Z", a.get("conferenciaConsiderada").asText());
        assertEquals("2026-11-12", a.get("dataReferencia").asText());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", a.get("resultadoGeral").asText());
        List<String> resultados = new ArrayList<>();
        a.get("resultados").forEach(x -> resultados.add(x.get("codigoRegra").asText() + "=" + x.get("resultado").asText()));
        assertEquals(List.of("RC-R01=ATENDE", "RC-R02=ATENDE", "RC-R03=ATENDE", "RC-R04=ATENDE", "RC-R05=ATENDE",
                "RC-R06=NAO_APLICAVEL"), resultados);
        assertEquals(0, a.get("pendencias").size());
        assertTrue(a.get("simulada").asBoolean());
        assertTrue(a.get("aviso").asText().contains("não é aprovação do SUS"));
        assertFalse(a.has("resumo"), "sem resumo nesta historia (RC-14)");
        assertEquals(2, a.get("documentos").size());
        assertEquals(p.documentos().get(0), a.get("documentos").get(0).get("documentoId").asText());
        assertEquals("encaminhamento-sintetico.pdf", a.get("documentos").get(0).get("nomeArquivo").asText());
        assertTrue(a.get("documentos").get(1).get("disponivel").asBoolean());

        JsonNode pedido = pedido(c, c.authA, p.id());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", pedido.get("estado").asText());
        assertEquals("CONCLUIDA", pedido.get("analise").get("situacao").asText());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", pedido.get("analise").get("resultadoGeral").asText());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void semConferirGravaRequerConferenciaComMotivoResponsavelEEvidencias() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);

        JsonNode a = pedir(c, c.authA, p.id()).json();

        assertEquals("REQUER_CONFERENCIA", a.get("resultadoGeral").asText());
        assertEquals(6, a.get("pendencias").size());
        for (String codigo : List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05")) {
            JsonNode pendencia = pendencia(a, codigo);
            assertEquals("NAO_VERIFICAVEL", pendencia.get("tipo").asText(), codigo);
            assertEquals("TRANSCRICAO_NAO_CONFERIDA", pendencia.get("motivo").asText(), codigo);
            assertEquals("PACIENTE", pendencia.get("responsavel").asText(), codigo);
        }
        assertEquals("CONDICAO_INDETERMINADA", pendencia(a, "RC-R06").get("motivo").asText());
        assertEquals("CONDICAO_INDETERMINADA", a.get("resultados").get(5).get("motivo").asText());
        // A evidencia aponta o documento e o campo; a origem vem da leitura daquela revisao. Nenhum valor lido. Sem
        // conferencia, ate o tipo do encaminhamento esta nao conferido (e o que R04 consulta primeiro).
        JsonNode evidencia = pendencia(a, "RC-R04").get("evidencias").get(0);
        assertEquals(p.documentos().get(0), evidencia.get("documentoId").asText());
        assertEquals("TIPO_DOCUMENTO", evidencia.get("campo").asText());
        assertEquals("NAO_CONFERIDO", evidencia.get("situacao").asText());
        assertEquals(1, evidencia.get("origem").get("pagina").asInt());
        assertEquals("ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL", evidencia.get("origem").get("trecho").asText());
        assertFalse(evidencia.has("valor"));
        assertEquals("REQUER_CONFERENCIA", pedido(c, c.authA, p.id()).get("estado").asText());
    }

    @Test
    void encaminhamentoSemMotivoGravaPendenciaDaUnidadeEmissoraApontandoODocumento() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);

        JsonNode a = pedir(c, c.authA, p.id()).json();

        assertEquals("COM_PENDENCIAS", a.get("resultadoGeral").asText());
        assertEquals(1, a.get("pendencias").size());
        JsonNode r04 = pendencia(a, "RC-R04");
        assertEquals("PENDENTE", r04.get("tipo").asText());
        assertEquals("UNIDADE_EMISSORA", r04.get("responsavel").asText());
        assertFalse(r04.has("motivo"));
        assertTrue(r04.get("explicacao").asText().startsWith("O encaminhamento não traz o motivo"));
        JsonNode evidencia = r04.get("evidencias").get(0);
        assertEquals(p.documentos().get(0), evidencia.get("documentoId").asText());
        assertEquals("NAO_LOCALIZADO", evidencia.get("situacao").asText());
        assertFalse(evidencia.has("origem"));
        assertEquals("COM_PENDENCIAS", pedido(c, c.authA, p.id()).get("estado").asText());
    }

    @Test
    void repetirSemMudancaDevolveAMesmaAnaliseSemGravarOutra() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        Resposta primeira = pedir(c, c.authA, p.id());
        c.cenario.relogio.avancar(java.time.Duration.ofSeconds(5));

        Resposta segunda = pedir(c, c.authA, p.id());

        assertEquals(201, primeira.status());
        assertEquals(200, segunda.status(), segunda.corpo());
        assertEquals(primeira.json().get("id").asText(), segunda.json().get("id").asText());
        assertEquals(primeira.json().get("executadaEm").asText(), segunda.json().get("executadaEm").asText());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals("REQUER_CONFERENCIA", pedido(c, c.authA, p.id()).get("estado").asText());
    }

    @Test
    void semLeituraOuComLeituraDesatualizadaRecusaSemGravarNemMudarEstado() throws Exception {
        String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

        Resposta semLeitura = pedir(c, c.authA, pedido);
        assertEquals(409, semLeitura.status());
        assertEquals("CONFLITO_ESTADO", semLeitura.json().get("codigo").asText());
        assertEquals("Leia os documentos antes de pedir a análise.", semLeitura.json().get("mensagem").asText());

        c.lerDocumentos(c.authA, pedido);
        CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.IDENTIFICACAO); // a leitura fica desatualizada
        Resposta desatualizada = pedir(c, c.authA, pedido);
        assertEquals(409, desatualizada.status());

        assertEquals("RASCUNHO", pedido(c, c.authA, pedido).get("estado").asText());
        assertEquals("NAO_REALIZADA", pedido(c, c.authA, pedido).get("analise").get("situacao").asText());
        assertEquals(0, c.cenario.analises.totalDoPedido(Long.parseLong(pedido)));
    }

    @Test
    void pedidoSubmetidoRecusaNovaAnaliseMasContinuaConsultavel() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        c.cenario.solicitacoes.forcarEstado(p.numero(), EstadoSolicitacao.SUBMETIDA_SIMULACAO);

        Resposta r = pedir(c, c.authA, p.id());

        assertEquals(409, r.status());
        assertEquals("Este pedido já foi submetido e só pode ser consultado.", r.json().get("mensagem").asText());
        assertEquals(200, historico(c, c.authA, p.id()).status());
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void instrucaoDentroDoDocumentoNaoMudaResultadoNemEstado() throws Exception {
        Pedido normal = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, normal);
        Pedido injecao = lido(c, AmostrasSinteticas.INJECAO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, injecao);

        JsonNode esperado = pedir(c, c.authA, normal.id()).json();
        JsonNode obtido = pedir(c, c.authA, injecao.id()).json();

        assertEquals(esperado.get("resultadoGeral").asText(), obtido.get("resultadoGeral").asText());
        for (String codigo : List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05", "RC-R06")) {
            assertEquals(resultado(esperado, codigo), resultado(obtido, codigo), codigo);
        }
        assertEquals(esperado.get("aviso").asText(), obtido.get("aviso").asText());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", pedido(c, c.authA, injecao.id()).get("estado").asText());
        assertNull(obtido.get("pendencias").get(0));
    }

    @Test
    void nenhumaAnaliseLevaOPedidoASubmissao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        pedir(c, c.authA, p.id());
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS,
                c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado());
    }
}
