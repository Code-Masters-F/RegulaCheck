package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.ServidorDeTeste.RespostaBinaria;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.documento.repository.NovoDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US2 (FR-015 a FR-017): listar os documentos do pedido e abrir o arquivo, so a dona e so pelo pedido certo.
 */
class ListarAbrirDocumentoEndpointTest {

    private static final List<String> CAMPOS_ITEM = List.of("id", "solicitacaoId", "nomeArquivo", "tipoDocumental",
            "formato", "tamanhoBytes", "paginas", "estadoEnvio", "revisaoEntrada", "enviadoEm", "simulacao");

    @Test
    void listaConformeOContratoNaOrdemDeEnvioESemCamposInternos() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "primeiro.pdf", ArquivosDeTeste.pdf(1), null);
            c.enviar(c.authA, pedido, "IDENTIFICACAO", "segundo.png", ArquivosDeTeste.png(), null);

            Resposta r = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA);

            assertEquals(200, r.status(), r.corpo());
            JsonNode corpo = r.json();
            assertEquals(List.of("documentos", "total", "maximo", "versaoConjuntoDocumental"),
                    ServidorDeTeste.campos(corpo));
            assertEquals(2, corpo.get("total").asInt());
            assertEquals(3, corpo.get("maximo").asInt());
            assertEquals(3, corpo.get("versaoConjuntoDocumental").asInt());
            assertEquals(CAMPOS_ITEM, ServidorDeTeste.campos(corpo.get("documentos").get(0)));
            assertEquals("primeiro.pdf", corpo.get("documentos").get(0).get("nomeArquivo").asText());
            assertEquals("segundo.png", corpo.get("documentos").get(1).get("nomeArquivo").asText());
            assertEquals("ENVIADO", corpo.get("documentos").get(1).get("estadoEnvio").asText());
            assertEquals("IDENTIFICACAO", corpo.get("documentos").get(1).get("tipoDocumental").asText());
            assertEquals(2, corpo.get("documentos").get(0).get("revisaoEntrada").asInt());
            assertEquals(3, corpo.get("documentos").get(1).get("revisaoEntrada").asInt());
            assertFalse(r.corpo().contains("caminho"), r.corpo());
            assertFalse(r.corpo().contains("hash"), r.corpo());
        }
    }

    @Test
    void pedidoSemDocumentosListaVaziaEmSucesso() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta r = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA);

            assertEquals(200, r.status());
            assertEquals(0, r.json().get("documentos").size());
            assertEquals(0, r.json().get("total").asInt());
            assertEquals(1, r.json().get("versaoConjuntoDocumental").asInt());
        }
    }

    @Test
    void downloadDevolveOsMesmosBytesComCabecalhosDeAnexoSeguro() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] original = ArquivosDeTeste.pdf(2);
            String docId = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", original, null).json()
                    .get("id").asText();

            RespostaBinaria r = c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + docId, c.authA);

            assertEquals(200, r.status());
            assertArrayEquals(original, r.corpo());
            assertEquals("application/pdf", r.cabecalho("Content-Type"));
            assertTrue(r.cabecalho("Content-Disposition").startsWith("attachment"), r.cabecalho("Content-Disposition"));
            assertEquals("nosniff", r.cabecalho("X-Content-Type-Options"));
            assertEquals("no-store", r.cabecalho("Cache-Control"));
            assertEquals(String.valueOf(original.length), r.cabecalho("Content-Length"));
            assertFalse(r.cabecalho("Content-Disposition").contains(c.raiz.toString()));
        }
    }

    @Test
    void outraPacienteRecebe404IdenticoAoDeInexistenteNaListaENoDownload() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String docId = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();

            Resposta listaAlheia = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authB);
            Resposta listaInexistente = c.api.get("/solicitacoes/999999/documentos", c.authB);
            RespostaBinaria baixaAlheia = c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + docId,
                    c.authB);
            RespostaBinaria baixaInexistente = c.api.getBinario("/solicitacoes/999999/documentos/" + docId, c.authB);

            assertEquals(404, listaAlheia.status());
            assertEquals(listaInexistente.corpo(), listaAlheia.corpo());
            assertEquals(404, baixaAlheia.status());
            assertEquals(new String(baixaInexistente.corpo()), new String(baixaAlheia.corpo()));
            assertEquals(2, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
        }
    }

    @Test
    void documentoDeOutroPedidoDaMesmaPacienteEh404() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido1 = c.criarRascunho(c.authA);
            String pedido2 = c.criarRascunho(c.authA);
            String docDoPedido1 = c.enviar(c.authA, pedido1, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();

            RespostaBinaria r = c.api.getBinario("/solicitacoes/" + pedido2 + "/documentos/" + docDoPedido1,
                    c.authA);
            RespostaBinaria inexistente = c.api.getBinario("/solicitacoes/" + pedido2 + "/documentos/999999",
                    c.authA);

            assertEquals(404, r.status());
            assertEquals(new String(inexistente.corpo()), new String(r.corpo()));
            assertEquals(0, c.api.get("/solicitacoes/" + pedido2 + "/documentos", c.authA).json().get("total")
                    .asInt());
        }
    }

    @Test
    void arquivoAusenteDaAreaPrivadaDa503ENaoEncerraAListagem() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            // Registro cujo arquivo nao existe em disco (como as linhas da carga sintetica ou um arquivo perdido).
            c.cenario.documentos.registrar(1, Long.parseLong(pedido),
                    new NovoDocumento("fantasma.pdf", TipoDocumental.OUTRO, TipoArquivo.PDF, 10, 1, IdInterno.novo(),
                            "a".repeat(64), null),
                    Instant.parse("2026-11-12T20:00:00Z"));

            RespostaBinaria baixa = c.api.getBinario("/solicitacoes/" + pedido + "/documentos/1", c.authA);
            Resposta lista = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA);

            assertEquals(503, baixa.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(baixa.corpo()).get("codigo").asText());
            assertFalse(new String(baixa.corpo()).contains(c.raiz.toString()), "nenhum caminho na resposta de erro");
            assertEquals(200, lista.status());
            assertEquals(1, lista.json().get("total").asInt());
        }
    }

    @Test
    void semAreaDeArquivosConfiguradaEnvioEDownloadDao503AListaEOPedidoFuncionam() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar(new br.com.regulacheck.auth.CenarioAuth(), false)) {
            String pedido = c.criarRascunho(c.authA);

            Resposta envio = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            Resposta lista = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA);
            Resposta pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA);

            assertEquals(503, envio.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", envio.json().get("codigo").asText());
            assertEquals(200, lista.status());
            assertEquals(0, lista.json().get("total").asInt());
            assertEquals(200, pedidoLido.status());
            assertEquals(1, pedidoLido.json().get("revisao").asInt(), "envio que falha nao muda a revisao");
            assertEquals(0, c.cenario.documentos.todos().size());
        }
    }
}
