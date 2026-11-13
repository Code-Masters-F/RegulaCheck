package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/** US4: a leitura vale para a versao dos documentos em que foi feita; sem duplicar; submetido so consulta. */
class ExtracaoDesatualizadaTest {

    @Test
    void substituirOuRemoverDocumentoDeixaALeituraAnteriorDesatualizada() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = c.criarRascunho(c.authA);
            String doc = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            JsonNode primeira = c.lerDocumentos(c.authA, pedido).json();
            assertEquals("ATUAL", primeira.get("situacao").asText());

            // Substituir por outro arquivo (RC-09): a leitura anterior deixa de valer.
            var troca = c.substituir(c.authA, pedido, doc, "ENCAMINHAMENTO", AmostrasSinteticas.SEM_MOTIVO.nome(),
                    AmostrasSinteticas.SEM_MOTIVO.bytes(), null);
            assertEquals(201, troca.status());
            Resposta consulta = c.consultarLeitura(c.authA, pedido);
            assertEquals(200, consulta.status());
            assertEquals("DESATUALIZADA", consulta.json().get("situacao").asText());
            assertEquals(primeira.get("versaoConjuntoDocumental").asInt(),
                    consulta.json().get("versaoConjuntoDocumental").asInt());

            // Nova leitura: reflete os documentos atuais e e a atual.
            Resposta nova = c.lerDocumentos(c.authA, pedido);
            assertEquals(201, nova.status());
            assertEquals("ATUAL", nova.json().get("situacao").asText());
            assertNotEquals(primeira.get("versaoConjuntoDocumental").asInt(),
                    nova.json().get("versaoConjuntoDocumental").asInt());
            JsonNode motivo = null;
            for (JsonNode campo : nova.json().get("documentos").get(0).get("campos")) {
                if (campo.get("codigo").asText().equals("MOTIVO_ENCAMINHAMENTO")) {
                    motivo = campo;
                }
            }
            assertEquals("NAO_LOCALIZADO", motivo.get("situacao").asText(), "dados do arquivo novo");
            assertEquals(2, c.cenario.extracoes.totalDoPedido(Long.parseLong(pedido)));
            assertEquals("ATUAL", c.consultarLeitura(c.authA, pedido).json().get("situacao").asText());

            // Remover o documento: a leitura volta a ficar desatualizada e nao ha o que ler.
            String novoDoc = nova.json().get("documentos").get(0).get("documentoId").asText();
            assertEquals(200, c.remover(c.authA, pedido, novoDoc).status());
            assertEquals("DESATUALIZADA", c.consultarLeitura(c.authA, pedido).json().get("situacao").asText());
            assertEquals(422, c.lerDocumentos(c.authA, pedido).status());
        }
    }

    @Test
    void enviarDocumentoNovoTambemDesatualizaALeitura() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.lerDocumentos(c.authA, pedido);

            CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.IDENTIFICACAO);

            assertEquals("DESATUALIZADA", c.consultarLeitura(c.authA, pedido).json().get("situacao").asText());
            Resposta nova = c.lerDocumentos(c.authA, pedido);
            assertEquals(2, nova.json().get("documentos").size());
        }
    }

    @Test
    void leituraRepetidaNaMesmaVersaoNaoDuplicaEMantemUmResultadoAtual() throws Exception {
        LeitorFalso leitor = new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            JsonNode a = c.lerDocumentos(c.authA, pedido).json();
            JsonNode b = c.lerDocumentos(c.authA, pedido).json();
            JsonNode d = c.lerDocumentos(c.authA, pedido).json();

            assertEquals(a, b);
            assertEquals(a, d);
            assertEquals(1, c.cenario.extracoes.totalDoPedido(Long.parseLong(pedido)));
            assertEquals(1, leitor.chamadas());
        }
    }

    @Test
    void pedidoSubmetidoRecusaNovaLeituraMasPermiteConsultar() throws Exception {
        LeitorFalso leitor = new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            long id = Long.parseLong(pedido);
            c.cenario.solicitacoes.forcarEstado(id, EstadoSolicitacao.SUBMETIDA_SIMULACAO);

            Resposta sem = c.lerDocumentos(c.authA, pedido);
            assertEquals(409, sem.status());
            assertEquals("CONFLITO_ESTADO", sem.json().get("codigo").asText());
            assertEquals("Este pedido já foi submetido e só pode ser consultado.", sem.json().get("mensagem").asText());
            assertEquals(0, leitor.chamadas());
            assertEquals(0, c.cenario.extracoes.total());
            assertEquals(404, c.consultarLeitura(c.authA, pedido).status(), "nunca houve leitura");
        }
    }

    @Test
    void pedidoSubmetidoComLeituraExistenteContinua409NoPostEPermiteOGet() throws Exception {
        LeitorFalso leitor = new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            assertEquals(201, c.lerDocumentos(c.authA, pedido).status());
            int chamadasAntes = leitor.chamadas();
            c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.SUBMETIDA_SIMULACAO);

            Resposta post = c.lerDocumentos(c.authA, pedido);
            Resposta get = c.consultarLeitura(c.authA, pedido);

            assertEquals(409, post.status(), "estado antes de idempotencia");
            assertEquals(chamadasAntes, leitor.chamadas());
            assertEquals(200, get.status());
            assertTrue(get.json().get("simulada").asBoolean());
            assertFalse(get.corpo().isBlank());
        }
    }

    @Test
    void leituraNaoMudaOEstadoDeOutrosEstadosDoPedido() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            long id = Long.parseLong(pedido);
            c.cenario.solicitacoes.forcarEstado(id, EstadoSolicitacao.COM_PENDENCIAS);

            assertEquals(201, c.lerDocumentos(c.authA, pedido).status());

            assertEquals(EstadoSolicitacao.COM_PENDENCIAS,
                    c.cenario.solicitacoes.buscarDaPaciente(id, 1).orElseThrow().estado());
        }
    }
}
