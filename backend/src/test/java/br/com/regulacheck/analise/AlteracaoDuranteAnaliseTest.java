package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.avaliacao;
import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoComIntervalo;
import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoQueFalha;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAnalise.servico;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Alteracao durante a analise (US3; FR-017; SC-003): a paciente (em outra tela) substitui um documento ou muda uma
 * decisao entre a avaliacao e a gravacao. O resultado antigo nunca e gravado e o estado volta ao de antes.
 */
class AlteracaoDuranteAnaliseTest {

    private static final String MUDOU =
            "Os documentos ou a conferência mudaram durante a análise. Confira e peça a análise de novo.";

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    private EstadoSolicitacao estado(Pedido p) {
        return c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado();
    }

    @Test
    void substituirDocumentoDuranteAAnaliseNaoGravaEVoltaARascunho() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        AnaliseService servico = servico(c, avaliacaoComIntervalo(c, () -> {
            assertEquals(EstadoSolicitacao.PROCESSANDO, estado(p), "durante a avaliacao o pedido esta PROCESSANDO");
            var r = c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                    AmostrasSinteticas.SEM_MOTIVO.nome(), AmostrasSinteticas.SEM_MOTIVO.bytes(), null);
            assertEquals(201, r.status(), "substituir continua aceito durante PROCESSANDO");
        }));

        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> servico.analisar(PACIENTE_A, p.numero()));

        assertEquals(409, recusa.status());
        assertEquals("CONFLITO_ESTADO", recusa.codigo());
        assertEquals(MUDOU, recusa.getMessage());
        assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()));
        assertEquals(EstadoSolicitacao.RASCUNHO, estado(p), "nunca analisado: volta a RASCUNHO");
        assertTrue(c.cenario.analises.iniciadaEm(p.numero()).isEmpty());
    }

    @Test
    void mudarDecisaoDuranteAAnaliseNaoGravaEVoltaAoResultadoDaUltimaAnalise() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        assertEquals(201, pedir(c, c.authA, p.id()).status()); // REQUER_CONFERENCIA
        c.cenario.relogio.avancar(Duration.ofSeconds(30));
        confirmarTudo(c, p); // tira a vigencia
        c.cenario.relogio.avancar(Duration.ofSeconds(30));
        String encaminhamento = p.documentos().get(0);
        AnaliseService servico = servico(c, avaliacaoComIntervalo(c, () -> {
            c.cenario.relogio.avancar(Duration.ofSeconds(1));
            var r = c.decidir(c.authA, p.id(), p.leitura(),
                    lote(divergir(encaminhamento, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
            assertEquals(200, r.status(), "decidir continua aceito durante PROCESSANDO");
        }));

        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> servico.analisar(PACIENTE_A, p.numero()));

        assertEquals(409, recusa.status());
        assertEquals(MUDOU, recusa.getMessage());
        assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "so a analise anterior");
        assertEquals(0, c.cenario.analises.vigentesDoPedido(p.numero()));
        assertEquals(EstadoSolicitacao.REQUER_CONFERENCIA, estado(p), "resultado da ultima analise concluida");
    }

    @Test
    void decisaoNoMesmoInstanteOuComRelogioAtrasadoTambemDescarta() throws Exception {
        // O instante da conferencia anda a cada gravacao que muda algo, mesmo com o relogio parado (mesmo
        // microssegundo) ou atrasado (gravacoes concorrentes confirmadas fora de ordem).
        for (Duration ajuste : List.of(Duration.ZERO, Duration.ofSeconds(-10))) {
            Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            confirmarTudo(c, p);
            String encaminhamento = p.documentos().get(0);
            AnaliseService servico = servico(c, avaliacaoComIntervalo(c, () -> {
                c.cenario.relogio.avancar(ajuste);
                var r = c.decidir(c.authA, p.id(), p.leitura(),
                        lote(divergir(encaminhamento, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
                assertEquals(200, r.status());
                c.cenario.relogio.avancar(ajuste.negated());
            }));

            ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class,
                    () -> servico.analisar(PACIENTE_A, p.numero()), "ajuste " + ajuste);

            assertEquals(MUDOU, recusa.getMessage(), "ajuste " + ajuste);
            assertEquals(0, c.cenario.analises.totalDoPedido(p.numero()), "ajuste " + ajuste);
            assertEquals(EstadoSolicitacao.RASCUNHO, estado(p), "ajuste " + ajuste);
        }
    }

    @Test
    void depoisDoDescarteUmaNovaAnaliseDaVersaoAtualEGravada() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        List<String> novoDocumento = new java.util.ArrayList<>();
        AnaliseService comTroca = servico(c, avaliacaoComIntervalo(c, () -> novoDocumento.add(c.substituir(c.authA,
                p.id(), p.documentos().get(0), "ENCAMINHAMENTO", AmostrasSinteticas.SEM_MOTIVO.nome(),
                AmostrasSinteticas.SEM_MOTIVO.bytes(), null).json().get("id").asText())));
        assertThrows(ExcecaoNegocio.class, () -> comTroca.analisar(PACIENTE_A, p.numero()));

        c.cenario.relogio.avancar(Duration.ofSeconds(30));
        String leitura = c.lerDocumentos(c.authA, p.id()).json().get("id").asText();
        confirmarTudo(c, new Pedido(p.id(), List.of(p.documentos().get(1), novoDocumento.get(0)), leitura));
        c.cenario.relogio.avancar(Duration.ofSeconds(30));
        var resultado = servico(c, avaliacao(c)).analisar(PACIENTE_A, p.numero());

        assertTrue(resultado.criada());
        assertEquals(4, resultado.analise().revisao());
        assertEquals(EstadoSolicitacao.COM_PENDENCIAS, estado(p));
    }

    @Test
    void leituraRecusadaDepoisDeIniciarEDescarteNaoFalha() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        AnaliseService servico = servico(c, avaliacaoQueFalha(c,
                new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Os documentos mudaram; leia os documentos de novo.")));

        ExcecaoNegocio recusa = assertThrows(ExcecaoNegocio.class, () -> servico.analisar(PACIENTE_A, p.numero()));

        assertEquals(409, recusa.status());
        assertEquals(MUDOU, recusa.getMessage());
        assertEquals(EstadoSolicitacao.RASCUNHO, estado(p), "descarte, nao FALHA_ANALISE");
    }
}
