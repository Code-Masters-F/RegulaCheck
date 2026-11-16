package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.resumo.service.GeradorResumo;
import br.com.regulacheck.resumo.service.GeradorResumoMockado;
import br.com.regulacheck.resumo.service.ResumoService;
import br.com.regulacheck.usuario.entity.Perfil;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Percurso RC-14 pela rota privada e falhas do contrato estruturado. */
class ResumoEndpointTest {

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    @Test
    void geraResumoPorRegraEConsultaNoHistorico() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        pedir(c, c.authA, p.id());

        Resposta primeira = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA);
        assertEquals(201, primeira.status(), primeira.corpo());
        JsonNode resumo = primeira.json().get("resumo");
        assertTrue(resumo.get("simulado").asBoolean());
        assertEquals("ADAPTADOR_MOCKADO", resumo.get("origem").asText());
        assertEquals(1, resumo.get("itens").size());
        assertEquals("RC-R04", resumo.get("itens").get(0).get("codigoRegra").asText());
        assertEquals("UNIDADE_EMISSORA", resumo.get("itens").get(0).get("responsavel").asText());
        assertEquals(1, primeira.json().get("pendencias").size());
        assertEquals(200, c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).status());
        assertEquals(resumo, historico(c, c.authA, p.id()).json().get("analises").get(0).get("resumo"));
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void semPendenciasNaoPrometeAprovacao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        pedir(c, c.authA, p.id());
        JsonNode analise = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).json();
        assertEquals(0, analise.get("resumo").get("itens").size());
        assertTrue(analise.get("resumo").get("texto").asText().contains("não significa aprovação do SUS"));
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", analise.get("resultadoGeral").asText());
    }

    @Test
    void semConferenciaOrientaConferirSemDeclararAusencia() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        pedir(c, c.authA, p.id());
        JsonNode analise = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).json();
        assertEquals("REQUER_CONFERENCIA", analise.get("resultadoGeral").asText());
        assertTrue(analise.get("resumo").get("texto").asText().contains("Confira a transcrição"));
        assertEquals(analise.get("pendencias").size(), analise.get("resumo").get("itens").size());
    }

    @Test
    void semAnaliseEOutroPacienteSaoRecusados() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        assertEquals(409, c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).status());
        pedir(c, c.authA, p.id());
        assertEquals(404, c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authB).status());
        assertEquals(404, c.api.enviar("POST", "/solicitacoes/999999/resumo", null, c.authB).status());
    }

    @Test
    void saidaInvalidaEFalhaUsamPadraoEPermitemNovaTentativa() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        pedir(c, c.authA, p.id());
        SessaoAutenticada sessao = new SessaoAutenticada(1, 1, "Paciente", Perfil.PACIENTE, Instant.MAX);
        GeradorResumo invalido = analise -> new GeradorResumo.Saida("exigência inventada", Map.of("RC-R99", "ação"));
        ResumoService servico = new ResumoService(c.cenario.analises, invalido);
        var padrao = servico.gerar(sessao, p.numero()).analise();
        assertEquals("PADRAO", padrao.resumoOrigem());
        assertEquals(1, padrao.pendencias().size());
        GeradorResumo falha = analise -> { throw new IllegalStateException("falha simulada"); };
        assertEquals("PADRAO", new ResumoService(c.cenario.analises, falha).gerar(sessao, p.numero())
                .analise().resumoOrigem());
        var recuperado = new ResumoService(c.cenario.analises, new GeradorResumoMockado())
                .gerar(sessao, p.numero()).analise();
        assertEquals("ADAPTADOR_MOCKADO", recuperado.resumoOrigem());
        assertFalse(recuperado.resumoTexto().contains("inventada"));
    }

    @Test
    void mudancaDaConferenciaDuranteGeracaoNaoPublicaResumoObsoleto() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        pedir(c, c.authA, p.id());
        SessaoAutenticada sessao = new SessaoAutenticada(1, 1, "Paciente", Perfil.PACIENTE, Instant.MAX);
        GeradorResumo gerador = analise -> {
            try {
                confirmarTudo(c, p, Map.of(p.documentos().get(0) + "/MOTIVO_ENCAMINHAMENTO",
                        divergir(p.documentos().get(0), "MOTIVO_ENCAMINHAMENTO", "VALOR_DIFERENTE_DO_DOCUMENTO")));
            } catch (Exception falha) {
                throw new IllegalStateException(falha);
            }
            return new GeradorResumoMockado().gerar(analise);
        };
        assertEquals(409, assertThrows(ExcecaoNegocio.class,
                () -> new ResumoService(c.cenario.analises, gerador).gerar(sessao, p.numero())).status());
        assertFalse(historico(c, c.authA, p.id()).json().get("analises").get(0).has("resumo"));
    }
}
