package br.com.regulacheck.solicitacao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.ServidorDeTeste.RespostaBinaria;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US4 (FR-016, SC-006): pedido em submissao simulada e so para consulta; remover, substituir e excluir respondem o
 * conflito documentado e nada muda. <strong>Limitacao:</strong> nenhuma rota produz a submissao ainda, entao o
 * estado e montado direto no dobro em memoria; isto nao e verificavel de ponta a ponta no Oracle.
 */
class PedidoSubmetidoSomenteConsultaTest {

    private static final String MENSAGEM_SUBMETIDO = "Este pedido já foi submetido e só pode ser consultado.";

    @Test
    void removerSubstituirEExcluirSaoConflitoENadaMudaEAConsultaContinua() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] conteudo = ArquivosDeTeste.pdf(1);
            String doc = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", conteudo, null).json().get("id").asText();
            c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null);
            long id = Long.parseLong(pedido);
            c.cenario.solicitacoes.forcarEstado(id, EstadoSolicitacao.SUBMETIDA_SIMULACAO);
            int revisoes = c.cenario.solicitacoes.revisoesDe(id).size();

            Resposta remover = c.remover(c.authA, pedido, doc);
            Resposta substituir = c.substituir(c.authA, pedido, doc, "OUTRO", "x.pdf", ArquivosDeTeste.pdf(3), null);
            Resposta excluir = c.excluir(c.authA, pedido, "EXCLUIR");

            for (Resposta r : List.of(remover, substituir, excluir)) {
                assertEquals(409, r.status(), r.corpo());
                assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
                assertEquals(MENSAGEM_SUBMETIDO, r.json().get("mensagem").asText());
            }
            // Confirmacao ausente continua sendo erro de contrato (400) antes da regra de estado; nada e apagado.
            assertEquals(400, c.excluir(c.authA, pedido, null).status());

            Resposta pedidoLido = c.api.get("/solicitacoes/" + pedido, c.authA);
            assertEquals(200, pedidoLido.status());
            assertEquals("SUBMETIDA_SIMULACAO", pedidoLido.json().get("estado").asText());
            assertEquals(3, pedidoLido.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(2, pedidoLido.json().get("documentosDisponiveis").asInt());
            assertEquals(2, c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA).json().get("total").asInt());
            RespostaBinaria baixa = c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + doc, c.authA);
            assertEquals(200, baixa.status());
            assertArrayEquals(conteudo, baixa.corpo());
            assertEquals(revisoes, c.cenario.solicitacoes.revisoesDe(id).size(), "nenhuma revisao nova");
            assertEquals(2, c.arquivosGuardados(), "nenhum arquivo apagado nem acrescentado");
            assertEquals(0, c.arquivosNaQuarentena());
            c.conferirIntegridade();
        }
    }

    @Test
    void emOutrosEstadosExcluirEhConflitoMasRemoverESubstituirSaoAceitosSemMudarOEstado() throws Exception {
        for (EstadoSolicitacao estado : List.of(EstadoSolicitacao.PROCESSANDO, EstadoSolicitacao.REQUER_CONFERENCIA,
                EstadoSolicitacao.COM_PENDENCIAS, EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS,
                EstadoSolicitacao.FALHA_ANALISE)) {
            try (CenarioDocumento c = CenarioDocumento.iniciar()) {
                String pedido = c.criarRascunho(c.authA);
                String d1 = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                        .get("id").asText();
                String d2 = c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null).json()
                        .get("id").asText();
                c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), estado);

                Resposta excluir = c.excluir(c.authA, pedido, "EXCLUIR");
                Resposta substituir = c.substituir(c.authA, pedido, d1, "OUTRO", "n.pdf", ArquivosDeTeste.pdf(3),
                        null);
                Resposta remover = c.remover(c.authA, pedido, d2);

                assertEquals(409, excluir.status(), estado + ": " + excluir.corpo());
                assertEquals("Somente rascunhos podem ser excluídos.", excluir.json().get("mensagem").asText());
                assertEquals(201, substituir.status(), estado + ": " + substituir.corpo());
                assertEquals(200, remover.status(), estado + ": " + remover.corpo());
                Resposta lido = c.api.get("/solicitacoes/" + pedido, c.authA);
                assertEquals(estado.name(), lido.json().get("estado").asText(), "o estado nao muda");
                assertEquals(1, lido.json().get("documentosDisponiveis").asInt());
                assertEquals(5, lido.json().get("versaoConjuntoDocumental").asInt(),
                        "1 criacao + 2 envios + troca + remocao");
                c.conferirIntegridade();
            }
        }
    }
}
