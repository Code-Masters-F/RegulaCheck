package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** US3: protegido, leitura vazia e falha do leitor: motivo, reenvio e nenhum resultado de completude. */
class LeituraMalSucedidaTest {

    private static void semResultadoDeCompletude(String corpo) {
        for (String proibido : new String[] {"resultadoGeral", "pendencia", "COM_PENDENCIAS", "REQUER_CONFERENCIA",
                "SEM_PENDENCIAS", "NAO_VERIFICAVEL", "faltou"}) {
            assertFalse(corpo.contains(proibido), proibido);
        }
    }

    @Test
    void arquivoProtegidoTemMotivoOrientacaoDeReenvioESemCampos() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciar(LeitorFalso.protegido())) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(201, r.status());
            JsonNode doc = r.json().get("documentos").get(0);
            assertEquals("PROTEGIDO", doc.get("situacao").asText());
            assertTrue(doc.get("reenviar").asBoolean());
            assertTrue(doc.get("motivo").asText().contains("protegido"));
            assertTrue(doc.get("orientacao").asText().contains("sem proteção"));
            assertEquals(0, doc.get("campos").size());
            semResultadoDeCompletude(r.corpo());
        }
    }

    @Test
    void arquivoDesconhecidoViraLeituraVaziaSemDeclararNenhumCampoNaoLocalizado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = c.criarRascunho(c.authA);
            c.enviar(c.authA, pedido, "OUTRO", "qualquer.pdf", ArquivosDeTeste.pdf(1), null);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(201, r.status());
            JsonNode doc = r.json().get("documentos").get(0);
            assertEquals("LEITURA_VAZIA", doc.get("situacao").asText());
            assertTrue(doc.get("reenviar").asBoolean());
            assertTrue(doc.get("orientacao").asText().contains("nítido"));
            assertEquals(0, doc.get("campos").size());
            assertFalse(r.corpo().contains("NAO_LOCALIZADO"));
            semResultadoDeCompletude(r.corpo());
        }
    }

    @Test
    void umDocumentoLidoEOutroNaoLidoCadaUmComOSeuResultado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = c.criarRascunho(c.authA);
            String lido = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            String naoLido = c.enviar(c.authA, pedido, "OUTRO", "x.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();

            JsonNode corpo = c.lerDocumentos(c.authA, pedido).json();

            for (JsonNode doc : corpo.get("documentos")) {
                if (doc.get("documentoId").asText().equals(lido)) {
                    assertEquals("LIDO", doc.get("situacao").asText());
                    assertEquals(6, doc.get("campos").size());
                } else {
                    assertEquals(naoLido, doc.get("documentoId").asText());
                    assertEquals("LEITURA_VAZIA", doc.get("situacao").asText());
                    assertEquals(0, doc.get("campos").size());
                }
            }
        }
    }

    @Test
    void falhaDoLeitorDa503SemDetalheENadaEGravado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciar(LeitorFalso.queFalha())) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("LEITURA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains("Leitor indisponivel"), "sem detalhe tecnico");
            assertEquals(0, c.cenario.extracoes.total());
            assertEquals(0, c.cenario.extracoes.gravacoes());
            assertEquals(404, c.consultarLeitura(c.authA, pedido).status());
        }
    }

    @Test
    void falhaSoNoSegundoDocumentoNaoDeixaResultadoParcialEARepeticaoFunciona() throws Exception {
        AtomicInteger chamada = new AtomicInteger();
        LeitorFalso leitor = new LeitorFalso(arquivo -> chamada.incrementAndGet() == 2
                ? LeitorFalso.falhar() : LeituraDeDocumento.vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO,
                    AmostrasSinteticas.IDENTIFICACAO);

            Resposta falhou = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, falhou.status());
            assertEquals(2, leitor.chamadas(), "leu o primeiro e falhou no segundo");
            assertEquals(0, c.cenario.extracoes.total(), "nenhum resultado parcial");

            Resposta repetida = c.lerDocumentos(c.authA, pedido);

            assertEquals(201, repetida.status());
            assertEquals(2, repetida.json().get("documentos").size());
            assertEquals(1, c.cenario.extracoes.total());
        }
    }

    @Test
    void saidaForaDoContratoVira503ENadaEGravado() throws Exception {
        LeituraDeDocumento invalida = new LeituraDeDocumento(SituacaoLeitura.LIDO,
                List.of(new CampoLido(CodigoCampo.NOME, br.com.regulacheck.integracao.extracao.SituacaoCampo
                        .NAO_LOCALIZADO, "valor inventado", null, null)));
        try (CenarioDocumento c = CenarioExtracao.iniciar(new LeitorFalso(arquivo -> invalida))) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("LEITURA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains("valor inventado"));
            assertEquals(0, c.cenario.extracoes.total());
        }
    }

    @Test
    void consultaDepoisDeLeituraComNaoLidoMantemMotivoEReenvio() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciar(LeitorFalso.vazio())) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.lerDocumentos(c.authA, pedido);

            Resposta r = c.consultarLeitura(c.authA, pedido);

            assertEquals(200, r.status());
            JsonNode doc = r.json().get("documentos").get(0);
            assertEquals("LEITURA_VAZIA", doc.get("situacao").asText());
            assertTrue(doc.get("reenviar").asBoolean());
            assertTrue(doc.has("motivo"));
            semResultadoDeCompletude(r.corpo());
        }
    }

    @Test
    void leituraMalSucedidaNaoAlteraEstadoDoPedido() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciar(LeitorFalso.protegido())) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            JsonNode antes = c.api.get("/solicitacoes/" + pedido, c.authA).json();

            c.lerDocumentos(c.authA, pedido);

            assertEquals(antes, c.api.get("/solicitacoes/" + pedido, c.authA).json());
        }
    }
}
