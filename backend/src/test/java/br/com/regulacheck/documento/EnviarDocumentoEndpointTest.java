package br.com.regulacheck.documento;

import static br.com.regulacheck.documento.CenarioDocumento.CORPO_RASCUNHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US1 (FR-001 a FR-007): enviar um documento ao rascunho, pela API real, com arquivos sinteticos gerados em memoria.
 */
class EnviarDocumentoEndpointTest {

    @Test
    void envioValidoDevolve201ComCorpoDoContratoEAumentaARevisao() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta r = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "encaminhamento-sintetico.pdf",
                    ArquivosDeTeste.pdf(1), null);

            assertEquals(201, r.status(), r.corpo());
            JsonNode corpo = r.json();
            assertEquals(List.of("id", "solicitacaoId", "nomeArquivo", "tipoDocumental", "formato", "tamanhoBytes",
                    "paginas", "estadoEnvio", "revisaoEntrada", "enviadoEm", "simulacao",
                    "versaoConjuntoDocumental", "documentosDisponiveis"), ServidorDeTeste.campos(corpo));
            assertEquals(pedido, corpo.get("solicitacaoId").asText());
            assertEquals("encaminhamento-sintetico.pdf", corpo.get("nomeArquivo").asText());
            assertEquals("ENCAMINHAMENTO", corpo.get("tipoDocumental").asText());
            assertEquals("PDF", corpo.get("formato").asText());
            assertEquals(1, corpo.get("paginas").asInt());
            assertEquals("ENVIADO", corpo.get("estadoEnvio").asText());
            assertEquals(2, corpo.get("revisaoEntrada").asInt());
            assertEquals(2, corpo.get("versaoConjuntoDocumental").asInt());
            assertEquals(1, corpo.get("documentosDisponiveis").asInt());
            assertTrue(corpo.get("simulacao").asBoolean());
            assertEquals("/solicitacoes/" + pedido + "/documentos/" + corpo.get("id").asText(),
                    r.cabecalho("Location"));
            // Nada interno sai pela API: nem nome de armazenamento, nem hash, nem chave.
            assertFalse(r.corpo().contains("caminho"), r.corpo());
            assertFalse(r.corpo().contains("hash"), r.corpo());
            assertEquals(1, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());

            JsonNode pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            assertEquals(2, pedidoLido.get("revisao").asInt());
            assertEquals(2, pedidoLido.get("versaoConjuntoDocumental").asInt());
            assertEquals(1, pedidoLido.get("documentosDisponiveis").asInt());
            assertEquals("RASCUNHO", pedidoLido.get("estado").asText(), "enviar nao inicia analise");
            assertEquals("NAO_REALIZADA", pedidoLido.get("analise").get("situacao").asText());
        }
    }

    @Test
    void jpegEPngTambemSaoAceitos() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta png = c.enviar(c.authA, pedido, "IDENTIFICACAO", "identificacao.png", ArquivosDeTeste.png(), null);
            Resposta jpeg = c.enviar(c.authA, pedido, "OUTRO", "foto.jpg", ArquivosDeTeste.jpeg(), null);

            assertEquals(201, png.status(), png.corpo());
            assertEquals("PNG", png.json().get("formato").asText());
            assertEquals(201, jpeg.status(), jpeg.corpo());
            assertEquals("JPEG", jpeg.json().get("formato").asText());
            assertEquals(3, jpeg.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(2, jpeg.json().get("documentosDisponiveis").asInt());
        }
    }

    @Test
    void camposForjadosNoCorpoSaoIgnorados() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", "OUTRO")
                    .texto("pacienteId", "2").texto("estado", "PROCESSANDO").texto("revisao", "99")
                    .texto("caminhoInterno", "../../etc/passwd").texto("solicitacaoId", "999")
                    .arquivo("arquivo", "a.pdf", "application/pdf", ArquivosDeTeste.pdf(1));

            Resposta r = c.api.postBinario("/solicitacoes/" + pedido + "/documentos", corpo.corpo(),
                    corpo.contentType(), c.authA);

            assertEquals(201, r.status(), r.corpo());
            assertEquals(pedido, r.json().get("solicitacaoId").asText());
            assertEquals(2, r.json().get("revisaoEntrada").asInt());
            assertEquals("RASCUNHO", c.api.get("/solicitacoes/" + pedido, c.authA).json().get("estado").asText());
            // O documento pertence ao pedido do caminho e a paciente A, nao a quem o corpo disse.
            assertEquals(1, c.cenario.documentos.listarDoPedido(Long.parseLong(pedido), 1).size());
            assertEquals(0, c.cenario.documentos.listarDoPedido(999, 2).size());
        }
    }

    @Test
    void tipoDocumentalAusenteEmBrancoOuInvalidoEh400ComDetalheENadaEhGravado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            for (String tipo : new String[] {null, "", "   ", "laudo", "encaminhamento", "LAUDO_INVENTADO"}) {
                Resposta r = c.enviar(c.authA, pedido, tipo, "a.pdf", ArquivosDeTeste.pdf(1), null);

                assertEquals(400, r.status(), "tipo=" + tipo);
                assertEquals("REQUISICAO_INVALIDA", r.json().get("codigo").asText());
                assertEquals("tipoDocumental", r.json().get("detalhes").get(0).get("campo").asText());
                assertFalse(r.json().get("detalhes").get(0).get("mensagem").asText().isBlank());
            }
            assertEquals(0, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());
            assertEquals(1, c.api.get("/solicitacoes/" + pedido, c.authA).json().get("revisao").asInt());
        }
    }

    @Test
    void semParteDeArquivoOuComDoisArquivosEh400() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            MultipartTeste semArquivo = new MultipartTeste().texto("tipoDocumental", "OUTRO");
            MultipartTeste doisArquivos = new MultipartTeste().texto("tipoDocumental", "OUTRO")
                    .arquivo("arquivo", "a.pdf", "application/pdf", ArquivosDeTeste.pdf(1))
                    .arquivo("outro", "b.pdf", "application/pdf", ArquivosDeTeste.pdf(1));

            Resposta r1 = c.api.postBinario("/solicitacoes/" + pedido + "/documentos", semArquivo.corpo(),
                    semArquivo.contentType(), c.authA);
            Resposta r2 = c.api.postBinario("/solicitacoes/" + pedido + "/documentos", doisArquivos.corpo(),
                    doisArquivos.contentType(), c.authA);

            assertEquals(400, r1.status());
            assertEquals("arquivo", r1.json().get("detalhes").get(0).get("campo").asText());
            assertEquals(400, r2.status());
            assertEquals("arquivos", r2.json().get("detalhes").get(0).get("campo").asText());
            assertEquals(0, c.arquivosGuardados());
            assertEquals(1, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
        }
    }

    @Test
    void corpoQueNaoEhMultipartEh415() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta r = c.api.postJson("/solicitacoes/" + pedido + "/documentos", CORPO_RASCUNHO, c.authA);

            assertEquals(415, r.status());
            assertEquals("TIPO_NAO_SUPORTADO", r.json().get("codigo").asText());
        }
    }

    @Test
    void corpoRuimEmPedidoAlheioOuInexistenteContinuaSendo404Identico() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedidoDeA = c.criarRascunho(c.authA);

            Resposta alheio = c.api.postJson("/solicitacoes/" + pedidoDeA + "/documentos", "isto nao e multipart",
                    c.authB);
            Resposta inexistente = c.api.postJson("/solicitacoes/999999/documentos", "isto nao e multipart", c.authB);
            Resposta alheioValido = c.enviar(c.authB, pedidoDeA, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);

            assertEquals(404, alheio.status());
            assertEquals(alheio.corpo(), inexistente.corpo());
            assertEquals(404, alheioValido.status());
            assertEquals(0, c.arquivosGuardados(), "nada e gravado para quem nao e dono");
            assertEquals(0, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void pedidoForaDeRascunhoEh409SemGravarNada() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.PROCESSANDO);

            Resposta r = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", ArquivosDeTeste.pdf(1), null);

            assertEquals(409, r.status());
            assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
            assertEquals(0, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());
            assertEquals(1, c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(pedido), 1).orElseThrow()
                    .revisaoAtual());
        }
    }

    @Test
    void quartoDocumentoEh422PelaRegraDeNegocioSemGravarNada() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            for (int i = 0; i < 3; i++) {
                assertEquals(201, c.enviar(c.authA, pedido, "OUTRO", "d" + i + ".pdf", ArquivosDeTeste.pdf(i + 1),
                        null).status());
            }

            Resposta r = c.enviar(c.authA, pedido, "OUTRO", "quarto.pdf", ArquivosDeTeste.pdf(4), null);

            assertEquals(422, r.status(), r.corpo());
            assertEquals("REGRA_DE_NEGOCIO", r.json().get("codigo").asText());
            assertEquals("documentos", r.json().get("detalhes").get(0).get("campo").asText());
            assertEquals(3, c.arquivosGuardados());
            assertEquals(4, c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(pedido), 1).orElseThrow()
                    .revisaoAtual(), "a revisao so subiu nos tres envios aceitos");
        }
    }
}
