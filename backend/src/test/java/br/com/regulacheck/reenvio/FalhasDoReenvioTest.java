package br.com.regulacheck.reenvio;

import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static br.com.regulacheck.reenvio.CenarioReenvio.PACIENTE_A;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.avaliacao;
import static br.com.regulacheck.reenvio.CenarioReenvio.confirmarTudo;
import static br.com.regulacheck.reenvio.CenarioReenvio.historico;
import static br.com.regulacheck.reenvio.CenarioReenvio.reenviar;
import static br.com.regulacheck.reenvio.CenarioReenvio.servico;
import static br.com.regulacheck.reenvio.CenarioReenvio.substituir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.service.AvaliacaoDoPedido;
import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.extracao.LeitoresDeTeste;
import br.com.regulacheck.integracao.extracao.ArquivoParaLeitura;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.LeituraIndisponivelException;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.reenvio.service.ReenvioService;
import br.com.regulacheck.reenvio.service.ReenvioService.Etapa;
import br.com.regulacheck.resumo.service.GeradorResumo;
import br.com.regulacheck.resumo.service.GeradorResumoMockado;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Falhas da verificacao da versao corrigida (RC-16, US4; FR-014 a FR-017; SC-005): cada falha diz a etapa e o que
 * repetir, nao grava resultado parcial e nao altera a ultima analise concluida; repetir so a etapa indicada conclui.
 * Os substitutos de teste (leitor, avaliacao e gerador de resumo) provocam as falhas sobre os repositorios em memoria.
 */
class FalhasDoReenvioTest {

    @Test
    void leituraIndisponivelPreservaDocumentosEUltimoResultadoEPermiteRepetirSemReenviarArquivo() throws Exception {
        LeitorAlternavel leitor = new LeitorAlternavel();
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
            JsonNode antes = historico(c, p.id()).get("analises").get(0);
            String novo = substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.ENCAMINHAMENTO);
            int leituraAntes = revisaoDaUltimaLeitura(c, p);
            leitor.indisponivel = true;

            Resposta r = reenviar(c, c.authA, p.id());

            assertEquals(503, r.status(), r.corpo());
            assertEquals("LEITURA_INDISPONIVEL", r.json().get("codigo").asText());
            assertEtapa(r, "LEITURA");
            assertTrue(r.json().get("mensagem").asText().contains("não é preciso reenviar o arquivo"));
            assertEquals(leituraAntes, revisaoDaUltimaLeitura(c, p), "nada da leitura nova foi gravado");
            JsonNode depois = historico(c, p.id()).get("analises");
            assertEquals(1, depois.size());
            assertEquals(antes.get("resultados"), depois.get(0).get("resultados"), "ultimo resultado intacto");
            assertEquals(200, c.api.getBinario("/solicitacoes/" + p.id() + "/documentos/" + novo, c.authA).status(),
                    "o arquivo corrigido foi mantido");

            leitor.indisponivel = false;
            Resposta repetida = reenviar(c, c.authA, p.id());
            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals("CONFERENCIA", repetida.json().get("etapa").asText());
        }
    }

    @Test
    void avaliacaoQueFalhaDeixaFalhaRecuperavelSemResultadoParcialERepetirConclui() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = prontoParaVerificar(c);
            AtomicBoolean falhar = new AtomicBoolean(true);
            AvaliacaoService instavel = new AvaliacaoService(c.cenario.solicitacoes, c.cenario.extracoes,
                    c.cenario.conferencias, c.cenario.documentos, c.cenario.protocolos,
                    MotorDeRegras.doProtocoloAcademico()) {
                @Override
                public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                    if (falhar.getAndSet(false)) {
                        throw new IllegalStateException("falha simulada da avaliacao");
                    }
                    return super.avaliar(sessao, solicitacaoId);
                }
            };
            ReenvioService servico = servico(c, LeitoresDeTeste.simuladoComTodasAsAmostras(), instavel,
                    new GeradorResumoMockado());

            ExcecaoNegocio falha = assertThrows(ExcecaoNegocio.class, () -> servico.verificar(PACIENTE_A,
                    p.numero()));

            assertEquals(503, falha.status());
            assertEquals("ANALISE_INDISPONIVEL", falha.codigo());
            assertEquals("ANALISE", falha.detalhes().get(0).mensagem());
            assertTrue(falha.getMessage().contains("Peça a verificação de novo"));
            assertEquals(EstadoSolicitacao.FALHA_ANALISE, estado(c, p));
            assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()), "nenhum resultado parcial");

            ReenvioService.Resultado repetido = servico.verificar(PACIENTE_A, p.numero());

            assertEquals(Etapa.CONCLUIDA, repetido.etapa());
            assertTrue(repetido.analiseCriada());
            assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, estado(c, p));
        }
    }

    @Test
    void documentoTrocadoDuranteAAnaliseNaoPublicaResultado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = prontoParaVerificar(c);
            String atual = c.api.get("/solicitacoes/" + p.id() + "/documentos", c.authA).json().get("documentos")
                    .get(0).get("id").asText();
            AvaliacaoService comTroca = new AvaliacaoService(c.cenario.solicitacoes, c.cenario.extracoes,
                    c.cenario.conferencias, c.cenario.documentos, c.cenario.protocolos,
                    MotorDeRegras.doProtocoloAcademico()) {
                @Override
                public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                    AvaliacaoDoPedido avaliada = super.avaliar(sessao, solicitacaoId);
                    try {
                        substituir(c, p.id(), atual, AmostrasSinteticas.SEM_MOTIVO);
                    } catch (Exception erro) {
                        throw new IllegalStateException(erro);
                    }
                    return avaliada;
                }
            };

            ExcecaoNegocio falha = assertThrows(ExcecaoNegocio.class, () -> servico(c,
                    LeitoresDeTeste.simuladoComTodasAsAmostras(), comTroca, new GeradorResumoMockado())
                    .verificar(PACIENTE_A, p.numero()));

            assertEquals(409, falha.status());
            assertEquals("CONFLITO_ESTADO", falha.codigo());
            assertEquals("ANALISE", falha.detalhes().get(0).mensagem());
            assertTrue(falha.getMessage().contains("Nada foi publicado"));
            assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
            assertEquals(0, c.cenario.analises.vigentesDoPedido(p.numero()));
            assertEquals(EstadoSolicitacao.COM_PENDENCIAS, estado(c, p), "volta ao resultado da ultima concluida");
        }
    }

    @Test
    void resumoQueNaoPodeSerGravadoDevolveAAnaliseRegistradaNaEtapaResumo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = prontoParaVerificar(c);
            JsonNode leitura = c.consultarLeitura(c.authA, p.id()).json();
            String documento = leitura.get("documentos").get(0).get("documentoId").asText();
            GeradorResumo mudaAConferencia = analise -> {
                try {
                    c.decidir(c.authA, p.id(), leitura.get("id").asText(),
                            lote(divergir(documento, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
                } catch (Exception erro) {
                    throw new IllegalStateException(erro);
                }
                return new GeradorResumoMockado().gerar(analise);
            };

            ReenvioService.Resultado resultado = servico(c, LeitoresDeTeste.simuladoComTodasAsAmostras(),
                    avaliacao(c), mudaAConferencia).verificar(PACIENTE_A, p.numero());

            assertEquals(Etapa.RESUMO, resultado.etapa());
            assertTrue(resultado.analiseCriada(), "a analise registrada nao e desfeita");
            assertNull(resultado.analise().resumoTexto());
            assertEquals(2, c.cenario.analises.totalDoPedido(p.numero()));
            assertFalse(historico(c, p.id()).get("analises").get(0).has("resumo"));
        }
    }

    @Test
    void geradorQueFalhaUsaAExplicacaoPadraoIdentificadaEConclui() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = prontoParaVerificar(c);
            GeradorResumo quebrado = analise -> {
                throw new IllegalStateException("falha simulada do resumo");
            };

            ReenvioService.Resultado resultado = servico(c, LeitoresDeTeste.simuladoComTodasAsAmostras(),
                    avaliacao(c), quebrado).verificar(PACIENTE_A, p.numero());

            assertEquals(Etapa.CONCLUIDA, resultado.etapa());
            assertEquals("PADRAO", resultado.analise().resumoOrigem());
            assertTrue(resultado.analise().resumoTexto().contains("Resumo simulado indisponível"));
        }
    }

    @Test
    void verificacaoDuranteOutraAnaliseERecusadaComRetryAfterEEtapa() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = prontoParaVerificar(c);
            List<Resposta> duranteAPrimeira = new ArrayList<>();
            AvaliacaoService comSegundaVerificacao = new AvaliacaoService(c.cenario.solicitacoes,
                    c.cenario.extracoes, c.cenario.conferencias, c.cenario.documentos, c.cenario.protocolos,
                    MotorDeRegras.doProtocoloAcademico()) {
                @Override
                public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                    try {
                        duranteAPrimeira.add(reenviar(c, c.authA, p.id()));
                    } catch (Exception erro) {
                        throw new IllegalStateException(erro);
                    }
                    return super.avaliar(sessao, solicitacaoId);
                }
            };

            ReenvioService.Resultado primeira = servico(c, LeitoresDeTeste.simuladoComTodasAsAmostras(),
                    comSegundaVerificacao, new GeradorResumoMockado()).verificar(PACIENTE_A, p.numero());

            Resposta segunda = duranteAPrimeira.get(0);
            assertEquals(409, segunda.status(), segunda.corpo());
            assertEquals("ANALISE_EM_ANDAMENTO", segunda.json().get("codigo").asText());
            assertEquals("60", segunda.cabecalho("Retry-After"), "relogio parado: falta o limite inteiro");
            assertEtapa(segunda, "ANALISE");
            assertEquals(Etapa.CONCLUIDA, primeira.etapa());
            assertEquals(2, c.cenario.analises.totalDoPedido(p.numero()));
        }
    }

    /**
     * Encaminhamento sem motivo analisado (R04 pendente), substituido pelo completo e com a leitura nova conferida:
     * a proxima verificacao chega as regras.
     */
    private static Pedido prontoParaVerificar(CenarioDocumento c) throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.ENCAMINHAMENTO);
        Resposta parada = reenviar(c, c.authA, p.id());
        confirmarTudo(c, p.id(), parada.json().get("extracaoId").asText());
        return p;
    }

    private static void assertEtapa(Resposta r, String etapa) {
        JsonNode detalhe = r.json().get("detalhes").get(0);
        assertEquals("etapa", detalhe.get("campo").asText());
        assertEquals(etapa, detalhe.get("mensagem").asText());
    }

    private static EstadoSolicitacao estado(CenarioDocumento c, Pedido p) {
        return c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado();
    }

    /** Revisao da leitura mais recente do pedido (a leitura so muda quando uma nova e gravada). */
    private static int revisaoDaUltimaLeitura(CenarioDocumento c, Pedido p) {
        return c.cenario.extracoes.buscarMaisRecente(1, p.numero()).orElseThrow().extracao().revisao();
    }

    /** Leitor simulado das amostras que pode ficar indisponivel, para provar a falha e a nova tentativa. */
    private static final class LeitorAlternavel implements LeitorDeDocumento {

        private final LeitorDeDocumento real = LeitoresDeTeste.simuladoComTodasAsAmostras();
        private volatile boolean indisponivel;

        @Override
        public LeituraDeDocumento ler(ArquivoParaLeitura arquivo) throws LeituraIndisponivelException {
            if (indisponivel) {
                throw new LeituraIndisponivelException("Leitor indisponivel (simulado no teste).");
            }
            return real.ler(arquivo);
        }
    }
}
