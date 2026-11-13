package br.com.regulacheck.extracao;

import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.confirmarEncaminhamento;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * US4: a situacao da conferencia e calculada corretamente, o estado e a revisao do pedido ficam intactos e a
 * conferencia nunca libera submissao nem gera resultado de regra.
 */
class SituacaoESemEstadoDoPedidoTest {

    private static Solicitacao pedidoDe(CenarioDocumento c, String pedido) {
        return c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(pedido), 1).orElseThrow();
    }

    @Test
    void campoIlegivelExigeConferenciaMesmoComTodosOsOutrosConfirmados() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.BORRADA);
            String extracao = c.lerDocumentos(c.authA, pedido).json().get("id").asText();
            String doc = c.consultarLeitura(c.authA, pedido).json().get("documentos").get(0).get("documentoId")
                    .asText();

            Resposta r = c.decidir(c.authA, pedido, extracao,
                    lote(confirmar(doc, "TIPO_DOCUMENTO"), confirmar(doc, "NOME")));

            assertEquals(200, r.status(), r.corpo());
            assertEquals("REQUER_CONFERENCIA", r.json().get("situacao").asText());
            assertEquals(0, r.json().get("resumo").get("camposSemDecisao").asInt());
            assertFalse(r.json().get("liberadoParaSubmissao").asBoolean());
        }
    }

    @Test
    void documentoNaoLidoExigeConferenciaMesmoComOOutroTodoConfirmado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciar(LeitorFalso.protegido())) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            String extracao = c.lerDocumentos(c.authA, pedido).json().get("id").asText();

            JsonNode corpo = c.consultarConferencia(c.authA, pedido, extracao).json();

            assertEquals("REQUER_CONFERENCIA", corpo.get("situacao").asText());
            assertFalse(corpo.get("liberadoParaSubmissao").asBoolean());
        }
    }

    @Test
    void semDecisoesEApenasCamposIdentificadosEPendente() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);

            JsonNode corpo = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();

            assertEquals("PENDENTE", corpo.get("situacao").asText());
            assertEquals(5, corpo.get("resumo").get("camposSemDecisao").asInt());
        }
    }

    @Test
    void todosOsIdentificadosConfirmadosSaoConferidosSemLiberarSubmissaoNemGerarResultadoDeRegra() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(), confirmarEncaminhamento(p.encaminhamento()));

            assertEquals(200, r.status(), r.corpo());
            assertEquals("CONFERIDA", r.json().get("situacao").asText());
            assertFalse(r.json().get("liberadoParaSubmissao").asBoolean());
            for (String proibido : new String[] {"resultadoGeral", "pendencias", "SEM_PENDENCIAS", "COM_PENDENCIAS",
                    "NAO_VERIFICAVEL", "ATENDE", "\"regras\"", "\"estado\"", "\"revisao\""}) {
                assertFalse(r.corpo().contains(proibido), proibido);
            }
            // nenhuma rota de submissao nasce com esta historia
            assertEquals(404, c.api.enviar("POST", "/solicitacoes/" + p.pedido() + "/submissoes-simuladas", null,
                    c.authA).status());
            assertEquals(404, c.api.enviar("POST", "/solicitacoes/" + p.pedido() + "/submissao", null, c.authA)
                    .status());
        }
    }

    @Test
    void umaDivergenciaFazConferidaVirarRequerConferenciaEConfirmarDeNovoDesfaz() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
            assertEquals("CONFERIDA", c.decidir(c.authA, p.pedido(), p.extracao(),
                    confirmarEncaminhamento(p.encaminhamento())).json().get("situacao").asText());

            Resposta diverge = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "DATA_EMISSAO", "VALOR_DIFERENTE_DO_DOCUMENTO")));
            assertEquals("REQUER_CONFERENCIA", diverge.json().get("situacao").asText());
            assertEquals(1, diverge.json().get("resumo").get("divergencias").asInt());

            Resposta confirma = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "DATA_EMISSAO")));
            assertEquals("CONFERIDA", confirma.json().get("situacao").asText());
            assertEquals(0, confirma.json().get("resumo").get("divergencias").asInt());
        }
    }

    @Test
    void naoLocalizadoSemDivergenciaNaoPesaEComDivergenciaExigeConferencia() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
            // o encaminhamento tem a autorizacao nao localizada e, mesmo assim, fica conferido
            assertEquals("CONFERIDA", c.decidir(c.authA, p.pedido(), p.extracao(),
                    confirmarEncaminhamento(p.encaminhamento())).json().get("situacao").asText());

            Resposta existe = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "AUTORIZACAO_ASSINADA", "CAMPO_EXISTE_E_NAO_FOI_LIDO")));
            assertEquals("REQUER_CONFERENCIA", existe.json().get("situacao").asText());
        }
    }

    @Test
    void estadoRevisaoEAtualizadaEmDoPedidoFicamIdenticosEmQualquerSequenciaDeDecisoes() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            Solicitacao antes = pedidoDe(c, p.pedido());
            var revisoesAntes = c.cenario.solicitacoes.revisoesDe(antes.id());

            c.decidir(c.authA, p.pedido(), p.extracao(), confirmarEncaminhamento(p.encaminhamento()));
            c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
            c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.encaminhamento(), "NOME")));
            c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.borrada(), "DATA_NASCIMENTO"))); // recusada

            assertEquals(antes, pedidoDe(c, p.pedido()));
            assertEquals(revisoesAntes, c.cenario.solicitacoes.revisoesDe(antes.id()));
        }
    }

    @Test
    void emPedidoForcadoAOutrosEstadosAConferenciaNaoMudaOEstado() throws Exception {
        for (EstadoSolicitacao estado : new EstadoSolicitacao[] {EstadoSolicitacao.COM_PENDENCIAS,
                EstadoSolicitacao.PROCESSANDO, EstadoSolicitacao.REQUER_CONFERENCIA}) {
            try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
                Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
                long id = Long.parseLong(p.pedido());
                c.cenario.solicitacoes.forcarEstado(id, estado);
                Solicitacao antes = pedidoDe(c, p.pedido());

                Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(),
                        confirmarEncaminhamento(p.encaminhamento()));

                assertEquals(200, r.status(), estado + ": " + r.corpo());
                assertEquals(antes, pedidoDe(c, p.pedido()), estado.name());
                assertEquals(estado, pedidoDe(c, p.pedido()).estado());
            }
        }
    }
}
