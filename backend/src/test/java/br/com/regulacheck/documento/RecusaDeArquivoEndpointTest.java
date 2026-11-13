package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US3 (FR-008, FR-009, SC-002): arquivo invalido e recusado antes de qualquer persistencia. Em cada caso a lista
 * de documentos, a revisao do pedido, a area privada e a quarentena permanecem exatamente como estavam.
 */
class RecusaDeArquivoEndpointTest {

    /** Arquivo sintetico invalido, o nome que o cliente declara e o erro esperado. */
    private record Caso(String descricao, String nome, byte[] conteudo, int status, String codigo, String campo) {
    }

    private static List<Caso> casos() {
        return List.of(
                new Caso("texto renomeado .pdf", "a.pdf", ArquivosDeTeste.texto(), 415, "TIPO_NAO_SUPORTADO", null),
                new Caso("executavel com nome .pdf", "a.pdf", ArquivosDeTeste.executavel(), 415,
                        "TIPO_NAO_SUPORTADO", null),
                new Caso("PDF com extensao .png", "a.png", ArquivosDeTeste.pdf(1), 415, "EXTENSAO_INCOMPATIVEL",
                        null),
                new Caso("PDF sem extensao", "documento", ArquivosDeTeste.pdf(1), 415, "EXTENSAO_INCOMPATIVEL",
                        null),
                new Caso("PDF com 6 paginas", "a.pdf", ArquivosDeTeste.pdf(6), 400, "REQUISICAO_INVALIDA", "arquivo"),
                new Caso("PDF protegido por senha", "a.pdf", ArquivosDeTeste.pdfProtegido(), 400,
                        "REQUISICAO_INVALIDA", "arquivo"),
                new Caso("PDF truncado", "a.pdf", ArquivosDeTeste.pdfTruncado(), 400, "REQUISICAO_INVALIDA",
                        "arquivo"),
                new Caso("arquivo vazio", "a.pdf", new byte[0], 400, "REQUISICAO_INVALIDA", "arquivo"),
                new Caso("PNG truncado", "a.png", ArquivosDeTeste.pngTruncado(), 400, "REQUISICAO_INVALIDA",
                        "arquivo"),
                new Caso("imagem com dimensao extrema", "a.png", ArquivosDeTeste.pngDimensaoExtrema(), 400,
                        "REQUISICAO_INVALIDA", "arquivo"),
                new Caso("acima de 5 MB", "a.pdf",
                        ArquivosDeTeste.pdfComTamanho((int) LimitesArquivo.MAXIMO_BYTES + 1024), 413,
                        "ARQUIVO_MUITO_GRANDE", null));
    }

    @Test
    void cadaArquivoInvalidoEhRecusadoSemDeixarRastro() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            for (Caso caso : casos()) {
                Resposta r = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", caso.nome(), caso.conteudo(), null);

                assertEquals(caso.status(), r.status(), caso.descricao() + " -> " + r.corpo());
                assertEquals(caso.codigo(), r.json().get("codigo").asText(), caso.descricao());
                if (caso.campo() != null) {
                    assertEquals(caso.campo(), r.json().get("detalhes").get(0).get("campo").asText(),
                            caso.descricao());
                }
                assertFalse(r.corpo().contains(c.raiz.toString()), "nenhum caminho na resposta: " + caso.descricao());
                assertEquals(0, c.arquivosGuardados(), caso.descricao());
                assertEquals(0, c.arquivosNaQuarentena(), caso.descricao());
                assertEquals(0, c.cenario.documentos.todos().size(), caso.descricao());
                assertEquals(1, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size(), caso.descricao());
            }
            Resposta pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA);
            assertEquals(1, pedidoLido.json().get("revisao").asInt(), "o pedido esta como antes de todos os envios");
            assertEquals(0, pedidoLido.json().get("documentosDisponiveis").asInt());
            assertEquals(0, c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).json().get("total").asInt());
        }
    }

    @Test
    void nomeComCaminhoEmbutidoEhSaneadoEONomeInternoNuncaEhOEnviado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta r = c.enviar(c.authA, pedido, "OUTRO", "..\\..\\pasta/secreta/encaminhamento.pdf",
                    ArquivosDeTeste.pdf(1), null);

            assertEquals(201, r.status(), r.corpo());
            String nome = r.json().get("nomeArquivo").asText();
            assertFalse(nome.contains("/") || nome.contains("\\") || nome.contains(".."), nome);
            assertEquals(1, c.arquivosGuardados());
            assertFalse(r.corpo().contains("pasta"), "o caminho enviado nao volta pela API");
        }
    }
}
