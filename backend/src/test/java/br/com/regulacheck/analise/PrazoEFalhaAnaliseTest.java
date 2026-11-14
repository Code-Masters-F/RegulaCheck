package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoComIntervalo;
import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoQueFalha;
import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedido;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAnalise.servico;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.analise.repository.InicioAnalise;
import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Limite de tempo, falha recuperavel e nova tentativa (US4; FR-016, FR-018 a FR-020; SC-006). Uma analise "travada"
 * e simulada iniciando-a direto pelo repositorio (como uma requisicao que caiu depois de gravar o inicio).
 */
class PrazoEFalhaAnaliseTest {

    private CenarioDocumento c;
    private Pedido p;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
        p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    private EstadoSolicitacao estado() {
        return c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado();
    }

    /** Simula uma requisicao que gravou o inicio e caiu. */
    private Instant analiseTravada() {
        Instant agora = c.cenario.relogio.instant();
        InicioAnalise inicio = c.cenario.analises.iniciar(1, p.numero(), agora, AnaliseService.LIMITE_EM_ANDAMENTO);
        assertEquals(InicioAnalise.Situacao.INICIADA, inicio.situacao());
        return agora;
    }

    @Test
    void dentroDoLimiteNovoPedidoERecusadoComRetryAfterEAConsultaMostraEmAndamento() throws Exception {
        Instant inicio = analiseTravada();
        c.cenario.relogio.avancar(Duration.ofSeconds(10));

        Resposta r = pedir(c, c.authA, p.id());

        assertEquals(409, r.status());
        assertEquals("ANALISE_EM_ANDAMENTO", r.json().get("codigo").asText());
        assertEquals("50", r.cabecalho("Retry-After"));
        JsonNode pedido = pedido(c, c.authA, p.id());
        assertEquals("PROCESSANDO", pedido.get("estado").asText());
        assertEquals("EM_ANDAMENTO", pedido.get("analise").get("situacao").asText());
        JsonNode execucao = historico(c, c.authA, p.id()).json().get("execucao");
        assertEquals("EM_ANDAMENTO", execucao.get("situacao").asText());
        assertEquals(inicio.toString(), execucao.get("iniciadaEm").asText());
        assertEquals(inicio.plusSeconds(60).toString(), execucao.get("limiteEm").asText());
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void depoisDoLimiteAConsultaMostraInterrompidaENovoPedidoGrava() throws Exception {
        analiseTravada();
        c.cenario.relogio.avancar(Duration.ofSeconds(61));

        assertEquals("INTERROMPIDA",
                historico(c, c.authA, p.id()).json().get("execucao").get("situacao").asText());
        Resposta r = pedir(c, c.authA, p.id());

        assertEquals(201, r.status(), r.corpo());
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, estado());
        assertTrue(c.cenario.analises.iniciadaEm(p.numero()).isEmpty());
        assertTrue(!historico(c, c.authA, p.id()).json().has("execucao"));
    }

    @Test
    void resultadoQueChegaDepoisDoLimiteEDescartadoQuandoOutraAnaliseAssumiu() throws Exception {
        AnaliseService lenta = servico(c, avaliacaoComIntervalo(c, () -> {
            c.cenario.relogio.avancar(Duration.ofSeconds(61));
            assertEquals(201, pedir(c, c.authA, p.id()).status(), "outra requisicao assume e grava");
        }));

        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> lenta.analisar(PACIENTE_A, p.numero()));

        assertEquals(409, recusa.status());
        assertEquals("A análise passou do tempo limite e não foi publicada. Consulte as análises ou peça de novo.",
                recusa.getMessage());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "so a da requisicao que assumiu");
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, estado());
    }

    @Test
    void versaoDoProtocoloNaoAplicavelVaiAFalhaAnaliseENovaTentativaGrava() throws Exception {
        c.cenario.protocolos.definirRegras(1, List.of());

        Resposta falha = pedir(c, c.authA, p.id());

        assertEquals(503, falha.status());
        assertEquals("ANALISE_INDISPONIVEL", falha.json().get("codigo").asText());
        assertEquals(EstadoSolicitacao.FALHA_ANALISE, estado());
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals("NAO_REALIZADA", pedido(c, c.authA, p.id()).get("analise").get("situacao").asText());

        c.cenario.protocolos.definirRegras(1, ProtocoloRepositoryMemoria.regrasV01());
        Resposta novaTentativa = pedir(c, c.authA, p.id());

        assertEquals(201, novaTentativa.status(), novaTentativa.corpo());
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, estado());
    }

    @Test
    void erroInesperadoNaAvaliacaoVaiAFalhaAnaliseSemGravar() {
        AnaliseService servico = servico(c, avaliacaoQueFalha(c, new IllegalStateException("falha simulada")));

        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> servico.analisar(PACIENTE_A, p.numero()));

        assertEquals(503, recusa.status());
        assertEquals("ANALISE_INDISPONIVEL", recusa.codigo());
        assertEquals(EstadoSolicitacao.FALHA_ANALISE, estado());
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
    }

    @Test
    void bancoIndisponivelAoGravarNaoDeixaAnaliseENaoPrendeOPedido() throws Exception {
        c.cenario.analises.falharNaProximaPublicacao(indisponivel());

        Resposta r = pedir(c, c.authA, p.id());

        assertEquals(503, r.status());
        assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
        assertEquals(EstadoSolicitacao.FALHA_ANALISE, estado(), "o banco voltou a tempo de registrar a falha");
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals(201, pedir(c, c.authA, p.id()).status());
    }

    @Test
    void semConseguirRegistrarAFalhaOLimiteLiberaOPedido() throws Exception {
        c.cenario.analises.falharNaProximaPublicacao(indisponivel());
        c.cenario.analises.falharNoProximoEncerramento(indisponivel());

        assertEquals(503, pedir(c, c.authA, p.id()).status());
        assertEquals(EstadoSolicitacao.PROCESSANDO, estado(), "nem a falha pode ser gravada");
        assertEquals(409, pedir(c, c.authA, p.id()).status());

        c.cenario.relogio.avancar(Duration.ofSeconds(60));
        Resposta depoisDoLimite = pedir(c, c.authA, p.id());

        assertEquals(201, depoisDoLimite.status(), depoisDoLimite.corpo());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
    }

    private static ExcecaoNegocio indisponivel() {
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
