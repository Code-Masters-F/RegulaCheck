package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * US5 (FR-013 a FR-015): remover ou substituir torna a conferencia anterior nao vigente, nao inicia analise e nao
 * muda o estado do pedido. <strong>Limitacao:</strong> nenhuma rota produz analise nem os estados de analise, entao
 * a analise vigente e o estado sao montados no dobro em memoria; no Oracle so o rascunho existe e o
 * {@code UPDATE RC_ANALISE} afeta zero linhas (nao verificavel de ponta a ponta).
 */
class ConferenciaInvalidadaTest {

    @Test
    void noRascunhoAVersaoSobeEARespostaDizQueNaoHaAnaliseParaANovaVersao() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String d1 = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();

            assertEquals(200, c.remover(c.authA, pedido, d1).status());
            JsonNode aposRemover = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            Resposta novo = c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null);
            Resposta troca = c.substituir(c.authA, pedido, novo.json().get("id").asText(), "OUTRO", "c.pdf",
                    ArquivosDeTeste.pdf(3), null);
            JsonNode aposTrocar = c.api.get("/solicitacoes/" + pedido, c.authA).json();

            assertEquals(3, aposRemover.get("versaoConjuntoDocumental").asInt());
            assertEquals("NAO_REALIZADA", aposRemover.get("analise").get("situacao").asText());
            assertEquals(201, troca.status());
            assertEquals(5, aposTrocar.get("versaoConjuntoDocumental").asInt());
            assertEquals("NAO_REALIZADA", aposTrocar.get("analise").get("situacao").asText());
            assertEquals("RASCUNHO", aposTrocar.get("estado").asText());
        }
    }

    @Test
    void comAnaliseVigenteRemoverEsubstituirANaoVigenteSemIniciarAnaliseNemMudarOEstado() throws Exception {
        for (EstadoSolicitacao estado : new EstadoSolicitacao[] {EstadoSolicitacao.COM_PENDENCIAS,
                EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, EstadoSolicitacao.REQUER_CONFERENCIA}) {
            try (CenarioDocumento c = CenarioDocumento.iniciar()) {
                String pedido = c.criarRascunho(c.authA);
                long id = Long.parseLong(pedido);
                String d1 = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                        .get("id").asText();
                String d2 = c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null).json()
                        .get("id").asText();
                c.cenario.solicitacoes.forcarEstado(id, estado);
                c.cenario.documentos.montarAnaliseVigente(id);
                assertTrue(c.cenario.documentos.temAnaliseVigente(id));

                Resposta troca = c.substituir(c.authA, pedido, d1, "OUTRO", "n.pdf", ArquivosDeTeste.pdf(3), null);
                assertEquals(201, troca.status(), troca.corpo());
                assertFalse(c.cenario.documentos.temAnaliseVigente(id), estado + ": a troca invalida a conferencia");

                c.cenario.documentos.montarAnaliseVigente(id);
                assertEquals(200, c.remover(c.authA, pedido, d2).status());
                assertFalse(c.cenario.documentos.temAnaliseVigente(id), estado + ": a remocao invalida a conferencia");

                JsonNode lido = c.api.get("/solicitacoes/" + pedido, c.authA).json();
                assertEquals(estado.name(), lido.get("estado").asText(), "o estado nao muda e nada e processado");
                assertEquals(5, lido.get("versaoConjuntoDocumental").asInt());
                assertEquals(1, lido.get("documentosDisponiveis").asInt());
                c.conferirIntegridade();
            }
        }
    }

    @Test
    void remocaoOuTrocaRecusadaNaoInvalidaAConferencia() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            long id = Long.parseLong(pedido);
            String d1 = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();
            c.cenario.solicitacoes.forcarEstado(id, EstadoSolicitacao.COM_PENDENCIAS);
            c.cenario.documentos.montarAnaliseVigente(id);

            assertEquals(404, c.remover(c.authA, pedido, "999999").status());
            assertEquals(415, c.substituir(c.authA, pedido, d1, "OUTRO", "x.pdf", ArquivosDeTeste.texto(), null)
                    .status());
            assertEquals(404, c.remover(c.authB, pedido, d1).status());

            assertTrue(c.cenario.documentos.temAnaliseVigente(id), "so uma mudanca aplicada invalida a conferencia");
            c.cenario.solicitacoes.forcarEstado(id, EstadoSolicitacao.SUBMETIDA_SIMULACAO);
            assertEquals(409, c.remover(c.authA, pedido, d1).status());
            assertTrue(c.cenario.documentos.temAnaliseVigente(id), "pedido submetido: nada muda");
        }
    }
}
