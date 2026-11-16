package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/** RC-21, US1 cenarios 1 e 2 (FR-008): a paciente abre a sessao de captura do proprio pedido, sem duplicar. */
class AbrirSessaoEndpointTest {

    @Test
    void abreASessaoEReaberturaDevolveAMesma() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            Solicitacao antes = c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(pedido), 1).orElseThrow();

            Resposta primeira = c.abrirSessao(c.authA, pedido);
            assertEquals(201, primeira.status());
            JsonNode corpo = primeira.json();
            assertTrue(corpo.get("criada").asBoolean());
            assertTrue(corpo.get("aberta").asBoolean());
            assertTrue(corpo.get("simulada").asBoolean());
            assertEquals(pedido, corpo.get("solicitacaoId").asText());
            assertEquals(c.agora().toString(), corpo.get("iniciadaEm").asText());
            assertEquals("/solicitacoes/" + pedido + "/leituras", primeira.cabecalho("Location"));

            Resposta segunda = c.abrirSessao(c.authA, pedido);
            assertEquals(200, segunda.status());
            assertEquals(corpo.get("id").asText(), segunda.json().get("id").asText());
            assertFalse(segunda.json().get("criada").asBoolean());
            assertEquals(1, c.cenario.capturas.sessoes().size());

            // Abrir sessao nao muda estado, revisao nem documentos do pedido.
            Solicitacao depois = c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(pedido), 1).orElseThrow();
            assertEquals(antes.estado(), depois.estado());
            assertEquals(antes.revisaoAtual(), depois.revisaoAtual());
        }
    }

    @Test
    void pedidoDeOutraPacienteEInexistenteTemAMesmaResposta() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            Resposta alheio = c.abrirSessao(c.authB, pedido);
            Resposta inexistente = c.abrirSessao(c.authB, "999999");
            Resposta malformado = c.abrirSessao(c.authB, "abc");
            for (Resposta r : new Resposta[] {alheio, inexistente, malformado}) {
                assertEquals(404, r.status());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
            }
            assertEquals(alheio.corpo(), inexistente.corpo());
            assertEquals(0, c.cenario.capturas.sessoes().size());
        }
    }

    @Test
    void pedidoSubmetidoRecebe409SemCriarSessao() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.SUBMETIDA_SIMULACAO);
            Resposta r = c.abrirSessao(c.authA, pedido);
            assertEquals(409, r.status());
            assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
            assertEquals(0, c.cenario.capturas.sessoes().size());
        }
    }

    @Test
    void abreDepoisDeUmaSessaoEncerradaEOutrosEstadosDoPedido() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            long numero = Long.parseLong(pedido);
            c.cenario.capturas.adicionarSessao(numero, c.agora().minusSeconds(600), c.agora().minusSeconds(300));
            c.cenario.solicitacoes.forcarEstado(numero, EstadoSolicitacao.COM_PENDENCIAS);
            Resposta r = c.abrirSessao(c.authA, pedido);
            assertEquals(201, r.status());
            assertEquals(2, c.cenario.capturas.sessoes().size());
        }
    }

    @Test
    void metodoErradoNaRotaDaSessaoRecebe405() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String pedido = c.criarPedido(c.authA);
            assertEquals(405, c.api.get("/solicitacoes/" + pedido + "/sessoes-captura", c.authA).status());
            assertEquals(405, c.api.enviar("POST", "/solicitacoes/" + pedido + "/leituras", null, c.authA).status());
        }
    }
}
