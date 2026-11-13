package br.com.regulacheck.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US3 (FR-010 a FR-012, FR-017, SC-003): excluir o rascunho so com confirmacao explicita, apagando pedido,
 * revisoes, documentos e arquivos; a ordem das verificacoes e dona (404) -> confirmacao (400) -> estado (409).
 */
class ExcluirRascunhoEndpointTest {

    private static String comDocumentos(CenarioDocumento c, int quantidade) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        for (int i = 1; i <= quantidade; i++) {
            Resposta r = c.enviar(c.authA, pedido, "OUTRO", "doc" + i + ".pdf", ArquivosDeTeste.pdf(i), null);
            assertEquals(201, r.status(), r.corpo());
        }
        return pedido;
    }

    @Test
    void comConfirmacaoDevolve204ESomeTudoInclusiveOsArquivos() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = comDocumentos(c, 3);
            String removido = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).json().get("documentos")
                    .get(0).get("id").asText();
            assertEquals(200, c.remover(c.authA, pedido, removido).status(), "um documento ja saiu antes");
            long id = Long.parseLong(pedido);
            assertEquals(2, c.arquivosGuardados());

            Resposta r = c.excluir(c.authA, pedido, "EXCLUIR");

            assertEquals(204, r.status(), r.corpo());
            assertEquals("", r.corpo());
            assertEquals(404, c.api.get("/solicitacoes/" + pedido, c.authA).status());
            assertEquals(404, c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).status());
            assertEquals(404, c.enviar(c.authA, pedido, "OUTRO", "x.pdf", ArquivosDeTeste.pdf(1), null).status());
            assertEquals(0, c.api.get("/solicitacoes", c.authA).json().get("itens").size());
            assertEquals(List.of(), c.cenario.solicitacoes.revisoesDe(id), "o historico de revisoes sai junto");
            assertTrue(c.cenario.documentos.todos().isEmpty() && c.cenario.documentos.removidos().isEmpty(),
                    "documentos disponiveis e removidos saem junto");
            assertEquals(0, c.arquivosGuardados(), "nenhum arquivo do pedido resta na area privada");
            c.conferirIntegridade();
        }
    }

    @Test
    void rascunhoSemDocumentosTambemEhExcluidoEUmNovoPedidoFuncionaDepois() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            assertEquals(204, c.excluir(c.authA, pedido, "EXCLUIR").status());

            String novo = c.criarRascunho(c.authA);
            assertEquals(200, c.api.get("/solicitacoes/" + novo, c.authA).status());
            assertEquals(201, c.enviar(c.authA, novo, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).status());
        }
    }

    @Test
    void semConfirmacaoOuComValorDiferenteEh400ENadaEhApagado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = comDocumentos(c, 2);
            int revisoes = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size();

            // "EXCLUIR " (com espaco no fim) nao entra: o HTTP remove o espaco das pontas do valor do cabecalho
            // antes de a aplicacao ve-lo (RFC 9110), entao ele chega como o valor exato.
            for (String valor : new String[] {null, "", "   ", "excluir", "Excluir", "true", "SIM", "EXCLUIRR",
                    "EXCLUIR EXCLUIR"}) {
                Resposta r = c.excluir(c.authA, pedido, valor);

                assertEquals(400, r.status(), "valor=" + valor + " -> " + r.corpo());
                assertEquals("REQUISICAO_INVALIDA", r.json().get("codigo").asText());
                assertEquals("confirmacao", r.json().get("detalhes").get(0).get("campo").asText());
            }
            assertEquals(200, c.api.get("/solicitacoes/" + pedido, c.authA).status());
            assertEquals(revisoes, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
            assertEquals(2, c.cenario.documentos.todos().size());
            assertEquals(2, c.arquivosGuardados());
            c.conferirIntegridade();
        }
    }

    @Test
    void pedidoAlheioInexistenteJaExcluidoOuMalFormadoTemOMesmo404MesmoSemConfirmacao() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedidoA = comDocumentos(c, 1);
            String excluido = c.criarRascunho(c.authA);
            assertEquals(204, c.excluir(c.authA, excluido, "EXCLUIR").status());

            Resposta alheio = c.excluir(c.authB, pedidoA, "EXCLUIR");
            Resposta alheioSemConfirmacao = c.excluir(c.authB, pedidoA, null);
            Resposta inexistente = c.excluir(c.authA, "999999", "EXCLUIR");
            Resposta jaExcluido = c.excluir(c.authA, excluido, "EXCLUIR");
            Resposta malFormado = c.excluir(c.authA, "abc", "EXCLUIR");

            for (Resposta r : List.of(alheio, alheioSemConfirmacao, inexistente, jaExcluido, malFormado)) {
                assertEquals(404, r.status(), r.corpo());
                assertEquals(inexistente.corpo(), r.corpo(), "resposta idêntica nos cinco casos");
            }
            assertEquals(200, c.api.get("/solicitacoes/" + pedidoA, c.authA).status(), "o pedido da A segue la");
            assertEquals(1, c.arquivosGuardados());
            c.conferirIntegridade();
        }
    }

    @Test
    void algoAindaLigadoAoPedidoImpedeAExclusaoComConflitoENadaEhApagado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = comDocumentos(c, 1);
            c.cenario.solicitacoes.impedirExclusao(Long.parseLong(pedido));

            Resposta r = c.excluir(c.authA, pedido, "EXCLUIR");

            assertEquals(409, r.status(), r.corpo());
            assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
            assertEquals(200, c.api.get("/solicitacoes/" + pedido, c.authA).status());
            assertEquals(1, c.arquivosGuardados());
            c.conferirIntegridade();
        }
    }

    @Test
    void excluirDeNovoDepoisDeExcluidoNaoApagaOutroPedido() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String primeiro = comDocumentos(c, 1);
            String segundo = comDocumentos(c, 1);

            assertEquals(204, c.excluir(c.authA, primeiro, "EXCLUIR").status());
            assertEquals(404, c.excluir(c.authA, primeiro, "EXCLUIR").status());

            assertEquals(200, c.api.get("/solicitacoes/" + segundo, c.authA).status());
            assertEquals(1, c.cenario.documentos.todos().size());
            assertEquals(1, c.arquivosGuardados());
            c.conferirIntegridade();
        }
    }
}
