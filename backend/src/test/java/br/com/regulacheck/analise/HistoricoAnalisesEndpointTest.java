package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedido;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Historico e resultado vigente (US2; FR-010 a FR-015; SC-002). O relogio avanca entre os passos, como na vida real,
 * para que cada conferencia tenha um instante proprio.
 */
class HistoricoAnalisesEndpointTest {

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    private void passarTempo() {
        c.cenario.relogio.avancar(Duration.ofSeconds(30));
    }

    @Test
    void pedidoNuncaAnalisadoTemHistoricoVazioSemVigente() throws Exception {
        String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

        Resposta r = historico(c, c.authA, pedido);

        assertEquals(200, r.status());
        JsonNode h = r.json();
        assertEquals(pedido, h.get("solicitacaoId").asText());
        assertEquals("RASCUNHO", h.get("estado").asText());
        assertEquals(2, h.get("versaoConjuntoDocumental").asInt());
        assertEquals(0, h.get("analises").size());
        assertEquals(50, h.get("limite").asInt());
        assertFalse(h.has("vigenteId"));
        assertFalse(h.has("execucao"));
    }

    @Test
    void completarAConferenciaTiraAVigenciaEPermiteNovaAnaliseDaMesmaRevisao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode primeira = pedir(c, c.authA, p.id()).json();
        passarTempo();

        confirmarTudo(c, p);

        JsonNode depoisDaConferencia = historico(c, c.authA, p.id()).json();
        assertFalse(depoisDaConferencia.get("analises").get(0).get("vigente").asBoolean());
        assertFalse(depoisDaConferencia.has("vigenteId"));
        JsonNode pedido = pedido(c, c.authA, p.id());
        assertEquals("REQUER_CONFERENCIA", pedido.get("estado").asText(), "o estado nao muda so por conferir");
        assertEquals("NAO_REALIZADA", pedido.get("analise").get("situacao").asText());

        passarTempo();
        Resposta segunda = pedir(c, c.authA, p.id());
        assertEquals(201, segunda.status(), segunda.corpo());

        JsonNode h = historico(c, c.authA, p.id()).json();
        assertEquals(2, h.get("analises").size());
        JsonNode nova = h.get("analises").get(0);
        JsonNode antiga = h.get("analises").get(1);
        assertEquals(segunda.json().get("id").asText(), nova.get("id").asText());
        assertEquals(nova.get("id").asText(), h.get("vigenteId").asText());
        assertTrue(nova.get("vigente").asBoolean());
        assertFalse(antiga.get("vigente").asBoolean());
        assertEquals(nova.get("revisao").asInt(), antiga.get("revisao").asInt());
        assertNotEquals(nova.get("conferenciaConsiderada").asText(), antiga.get("conferenciaConsiderada").asText());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", nova.get("resultadoGeral").asText());
        // A antiga continua como foi gravada.
        assertEquals(primeira.get("id").asText(), antiga.get("id").asText());
        assertEquals("REQUER_CONFERENCIA", antiga.get("resultadoGeral").asText());
        assertEquals(primeira.get("pendencias"), antiga.get("pendencias"));
        assertEquals(primeira.get("executadaEm").asText(), antiga.get("executadaEm").asText());
        assertEquals(1, c.cenario.analises.vigentesDoPedido(p.numero()));
    }

    @Test
    void substituirDocumentoTiraAVigenciaSemMudarEstadoEANovaRevisaoGeraNovaAnalise() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        JsonNode primeira = pedir(c, c.authA, p.id()).json();
        passarTempo();

        Resposta troca = c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                AmostrasSinteticas.SEM_MOTIVO.nome(), AmostrasSinteticas.SEM_MOTIVO.bytes(), null);
        assertEquals(201, troca.status(), troca.corpo());
        String novoDocumento = troca.json().get("id").asText();

        JsonNode pedido = pedido(c, c.authA, p.id());
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", pedido.get("estado").asText(), "RC-09: o estado e mantido");
        assertEquals("NAO_REALIZADA", pedido.get("analise").get("situacao").asText());
        JsonNode h = historico(c, c.authA, p.id()).json();
        assertEquals(4, h.get("versaoConjuntoDocumental").asInt());
        assertFalse(h.has("vigenteId"));
        JsonNode antiga = h.get("analises").get(0);
        assertEquals(primeira.get("resultados"), antiga.get("resultados"));
        assertEquals(3, antiga.get("revisao").asInt());
        assertFalse(antiga.get("documentos").get(0).get("disponivel").asBoolean(), "substituido depois");
        assertTrue(antiga.get("documentos").get(1).get("disponivel").asBoolean());

        passarTempo();
        String leitura = c.lerDocumentos(c.authA, p.id()).json().get("id").asText();
        confirmarTudo(c, new Pedido(p.id(), java.util.List.of(novoDocumento, p.documentos().get(1)), leitura));
        passarTempo();
        JsonNode segunda = pedir(c, c.authA, p.id()).json();

        assertEquals(4, segunda.get("revisao").asInt());
        assertEquals("COM_PENDENCIAS", segunda.get("resultadoGeral").asText());
        // Documentos da revisao 4, na ordem de envio: a identificacao (mantida) e o encaminhamento novo.
        assertEquals(p.documentos().get(1), segunda.get("documentos").get(0).get("documentoId").asText());
        assertEquals(novoDocumento, segunda.get("documentos").get(1).get("documentoId").asText());
        assertEquals(2, segunda.get("documentos").size());
        JsonNode depois = historico(c, c.authA, p.id()).json();
        assertEquals(2, depois.get("analises").size());
        assertEquals(segunda.get("id").asText(), depois.get("vigenteId").asText());
        assertEquals("COM_PENDENCIAS", pedido(c, c.authA, p.id()).get("estado").asText());
    }

    @Test
    void repetirAMesmaDecisaoNaoTiraAVigenciaMasMudarUmaTira() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        String analise = pedir(c, c.authA, p.id()).json().get("id").asText();
        passarTempo();

        confirmarTudo(c, p); // mesmas decisoes: nada muda
        assertEquals(analise, historico(c, c.authA, p.id()).json().get("vigenteId").asText());

        String encaminhamento = p.documentos().get(0);
        Resposta r = c.decidir(c.authA, p.id(), p.leitura(),
                lote(divergir(encaminhamento, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
        assertEquals(200, r.status(), r.corpo());
        assertFalse(historico(c, c.authA, p.id()).json().has("vigenteId"));
        assertEquals(0, c.cenario.analises.vigentesDoPedido(p.numero()));
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "a analise continua no historico");
    }
}
