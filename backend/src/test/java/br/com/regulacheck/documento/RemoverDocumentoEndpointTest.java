package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.documento.repository.NovoDocumento;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US1 (FR-001 a FR-004, FR-017, FR-022): remover um documento do pedido pela API real, com arquivos sinteticos.
 */
class RemoverDocumentoEndpointTest {

    private static final String HASH_FALSO = "0".repeat(64);

    private static String enviarUm(CenarioDocumento c, String pedido, String nome) throws Exception {
        Resposta r = c.enviar(c.authA, pedido, "OUTRO", nome, ArquivosDeTeste.pdf(1), null);
        assertEquals(201, r.status(), r.corpo());
        return r.json().get("id").asText();
    }

    @Test
    void removerDevolve200ComAVersaoNovaLiberaAVagaEApagaOArquivo() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String d1 = enviarUm(c, pedido, "a.pdf");
            String d2 = enviarUm(c, pedido, "b.pdf");
            String d3 = enviarUm(c, pedido, "c.pdf");
            assertEquals(3, c.arquivosGuardados());
            assertEquals(422, c.enviar(c.authA, pedido, "OUTRO", "d.pdf", ArquivosDeTeste.pdf(2), null).status(),
                    "com 3 documentos o quarto e recusado");

            Resposta r = c.remover(c.authA, pedido, d2);

            assertEquals(200, r.status(), r.corpo());
            assertEquals(List.of("versaoConjuntoDocumental", "documentosDisponiveis"),
                    ServidorDeTeste.campos(r.json()));
            assertEquals(5, r.json().get("versaoConjuntoDocumental").asInt(), "1 criacao + 3 envios + 1 remocao");
            assertEquals(2, r.json().get("documentosDisponiveis").asInt());
            assertFalse(r.corpo().contains("caminho"), r.corpo());

            List<RevisaoRegistrada> revisoes = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido));
            assertEquals(MotivoRevisao.DOCUMENTO_REMOVIDO, revisoes.get(revisoes.size() - 1).motivo());
            assertEquals(5, revisoes.get(revisoes.size() - 1).numero());

            JsonNode lista = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).json();
            assertEquals(2, lista.get("documentos").size());
            assertEquals(404, c.api.get("/solicitacoes/" + pedido + "/documentos/" + d2, c.authA).status());
            assertEquals(200, c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + d1, c.authA).status());
            assertEquals(200, c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + d3, c.authA).status());

            JsonNode pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            assertEquals(5, pedidoLido.get("versaoConjuntoDocumental").asInt());
            assertEquals(2, pedidoLido.get("documentosDisponiveis").asInt());
            assertEquals("RASCUNHO", pedidoLido.get("estado").asText(), "remover nao muda o estado");
            assertEquals("NAO_REALIZADA", pedidoLido.get("analise").get("situacao").asText());

            assertEquals(2, c.arquivosGuardados(), "o arquivo removido foi apagado");
            c.conferirIntegridade();
            assertEquals(1, c.cenario.documentos.removidos().size(), "o registro fica como historico");
            assertEquals(201, c.enviar(c.authA, pedido, "OUTRO", "e.pdf", ArquivosDeTeste.pdf(2), null).status(),
                    "a vaga liberada aceita um envio novo");
            c.conferirIntegridade();
        }
    }

    @Test
    void repetirARemocaoEh404SemMudarVersaoNemContagem() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String d1 = enviarUm(c, pedido, "a.pdf");
            enviarUm(c, pedido, "b.pdf");
            assertEquals(200, c.remover(c.authA, pedido, d1).status());

            Resposta segunda = c.remover(c.authA, pedido, d1);

            assertEquals(404, segunda.status());
            assertEquals("NAO_ENCONTRADO", segunda.json().get("codigo").asText());
            JsonNode pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            assertEquals(4, pedidoLido.get("versaoConjuntoDocumental").asInt(), "1 + 2 envios + 1 remocao");
            assertEquals(1, pedidoLido.get("documentosDisponiveis").asInt(), "nao removeu um segundo documento");
            c.conferirIntegridade();
        }
    }

    @Test
    void documentoAlheioDeOutroPedidoInexistenteOuMalFormadoTemOMesmo404ENadaMuda() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedidoA = c.criarRascunho(c.authA);
            String outroPedidoA = c.criarRascunho(c.authA);
            String pedidoB = c.criarRascunho(c.authB);
            String docA = enviarUm(c, pedidoA, "a.pdf");
            String docNoOutroPedido = enviarUm(c, outroPedidoA, "o.pdf");

            Resposta alheio = c.remover(c.authB, pedidoB, docA);
            Resposta pedidoAlheio = c.remover(c.authB, pedidoA, docA);
            Resposta outroPedido = c.remover(c.authA, pedidoA, docNoOutroPedido);
            Resposta inexistente = c.remover(c.authA, pedidoA, "999999");
            Resposta malFormado = c.remover(c.authA, pedidoA, "abc");
            Resposta pedidoInexistente = c.remover(c.authA, "999999", docA);

            for (Resposta r : List.of(alheio, pedidoAlheio, outroPedido, inexistente, malFormado, pedidoInexistente)) {
                assertEquals(404, r.status(), r.corpo());
                assertEquals(inexistente.corpo(), r.corpo(), "resposta idêntica nos seis casos");
            }
            assertEquals(2, c.cenario.documentos.todos().size());
            assertTrue(c.cenario.documentos.removidos().isEmpty());
            assertEquals(2, c.api.get("/solicitacoes/" + pedidoA, c.authA).json().get("revisao").asInt());
            c.conferirIntegridade();
        }
    }

    @Test
    void camposForjadosNaRequisicaoSaoIgnorados() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String d1 = enviarUm(c, pedido, "a.pdf");

            Resposta r = c.api.enviar("DELETE", "/solicitacoes/" + pedido + "/documentos/" + d1,
                    "{\"pacienteId\":2,\"estado\":\"PROCESSANDO\",\"revisao\":99}", c.authA);

            assertEquals(200, r.status(), r.corpo());
            assertEquals(3, r.json().get("versaoConjuntoDocumental").asInt());
            assertEquals("RASCUNHO", c.api.get("/solicitacoes/" + pedido, c.authA).json().get("estado").asText());
        }
    }

    @Test
    void documentoDaCargaSinteticaSemArquivoPodeSerRemovidoSemFalhar() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            // Linha como a da carga sintetica: registro sem nome interno (nunca teve arquivo).
            var registro = c.cenario.documentos.registrar(1, Long.parseLong(pedido), new NovoDocumento("sim.pdf",
                    TipoDocumental.OUTRO, TipoArquivo.PDF, 10, 1, null, HASH_FALSO, null), Instant.now());
            String id = String.valueOf(registro.documento().id());

            Resposta r = c.remover(c.authA, pedido, id);

            assertEquals(200, r.status(), r.corpo());
            assertEquals(0, r.json().get("documentosDisponiveis").asInt());
        }
    }

    @Test
    void aChaveDeEnvioDoDocumentoRemovidoFicaLivre() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            Resposta primeiro = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), "chave-livre-01");
            assertEquals(201, primeiro.status());
            assertEquals(200, c.remover(c.authA, pedido, primeiro.json().get("id").asText()).status());

            // Mesma chave, conteudo diferente: depois da remocao nao e mais "tentativa viva", entao nao conflita.
            Resposta novo = c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(3), "chave-livre-01");

            assertEquals(201, novo.status(), novo.corpo());
            c.conferirIntegridade();
        }
    }
}
