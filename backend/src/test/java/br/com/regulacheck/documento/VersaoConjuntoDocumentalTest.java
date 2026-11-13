package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * US4 (FR-006, FR-007, FR-018, FR-019, FR-024): a versao do conjunto documental e a revisao do pedido; o pedido diz
 * que nenhuma analise foi realizada; receber um documento nunca e apresentado como conferir, validar ou aprovar.
 */
class VersaoConjuntoDocumentalTest {

    private static final String CHAVE = "chave-da-tentativa-0001";

    @Test
    void aVersaoSobeACadaEnvioEOsDocumentosGuardamARevisaoEmQueEntraram() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta primeiro = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            Resposta segundo = c.enviar(c.authA, pedido, "IDENTIFICACAO", "b.png", ArquivosDeTeste.png(), null);

            assertEquals(2, primeiro.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(1, primeiro.json().get("documentosDisponiveis").asInt());
            assertEquals(3, segundo.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(2, segundo.json().get("documentosDisponiveis").asInt());
            assertEquals(2, primeiro.json().get("revisaoEntrada").asInt());
            assertEquals(3, segundo.json().get("revisaoEntrada").asInt());
            List<RevisaoRegistrada> historico = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido));
            assertEquals(List.of(MotivoRevisao.CRIACAO, MotivoRevisao.DOCUMENTO_ENVIADO,
                    MotivoRevisao.DOCUMENTO_ENVIADO), historico.stream().map(RevisaoRegistrada::motivo).toList());
        }
    }

    @Test
    void recusaERepeticaoNaoMudamAVersao() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] arquivo = ArquivosDeTeste.pdf(1);
            c.enviar(c.authA, pedido, "OUTRO", "a.pdf", arquivo, CHAVE);

            c.enviar(c.authA, pedido, "OUTRO", "a.pdf", arquivo, CHAVE);
            c.enviar(c.authA, pedido, "OUTRO", "falso.pdf", ArquivosDeTeste.texto(), null);
            c.enviar(c.authA, pedido, "INVALIDO", "a.pdf", arquivo, null);

            JsonNode lido = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            assertEquals(2, lido.get("versaoConjuntoDocumental").asInt());
            assertEquals(2, lido.get("revisao").asInt());
            assertEquals(1, lido.get("documentosDisponiveis").asInt());
        }
    }

    @Test
    void oPedidoInformaVersaoContagemEAnaliseNaoRealizadaEmTodasAsRespostas() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            c.cenario.protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
            String pedido = c.criarRascunho(c.authA);
            c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null);

            JsonNode detalhe = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            JsonNode troca = c.api.enviar("PUT", "/solicitacoes/" + pedido,
                    "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.2\"}", c.authA).json();
            JsonNode itemDaLista = c.api.get("/solicitacoes", c.authA).json().get("itens").get(0);

            for (JsonNode corpo : List.of(detalhe, itemDaLista)) {
                assertEquals(2, corpo.get("documentosDisponiveis").asInt());
            }
            assertEquals(3, detalhe.get("versaoConjuntoDocumental").asInt());
            assertEquals(4, troca.get("versaoConjuntoDocumental").asInt(), "trocar o servico tambem e uma revisao");
            assertEquals(2, troca.get("documentosDisponiveis").asInt(), "a troca de servico nao perde documentos");
            for (JsonNode corpo : List.of(detalhe, troca, itemDaLista)) {
                assertEquals("RASCUNHO", corpo.get("estado").asText());
                assertEquals("NAO_REALIZADA", corpo.get("analise").get("situacao").asText());
                assertFalse(corpo.get("analise").get("mensagem").asText().isBlank());
                assertEquals(corpo.get("revisao").asInt(), corpo.get("versaoConjuntoDocumental").asInt());
            }
        }
    }

    @Test
    void nenhumaRespostaDizQueODocumentoFoiConferidoValidadoOuAprovado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            Resposta envio = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            String docId = envio.json().get("id").asText();

            List<String> respostas = List.of(envio.corpo(),
                    c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).corpo(),
                    c.api.get("/solicitacoes/" + pedido, c.authA).corpo(), docId);

            for (String corpo : respostas) {
                String minusculo = corpo.toLowerCase(Locale.ROOT);
                for (String proibida : new String[] {"conferido", "validado", "aprovado", "verificado", "analisado"}) {
                    assertFalse(minusculo.contains(proibida), proibida + " em: " + corpo);
                }
            }
        }
    }
}
