package br.com.regulacheck.submissao;

import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisar;
import static br.com.regulacheck.reenvio.CenarioReenvio.incluir;
import static br.com.regulacheck.reenvio.CenarioReenvio.substituir;
import static br.com.regulacheck.submissao.SubmeterEndpointTest.submeter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioConferencia;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Recusa da conclusao quando o pedido nao esta pronto (RC-17, US2; FR-002, FR-006, FR-007; SC-002): 409 com a
 * mensagem do simulador, nenhuma submissao registrada e o estado e a revisao do pedido iguais aos de antes.
 */
class RecusasDaSubmissaoTest {

    private static final String PENDENCIAS = "não aponta pendências nem critérios não verificáveis";
    private static final String SEM_VIGENTE = "Não há análise válida para a versão atual dos documentos";
    private static final String RESTRICAO = "restrição do simulador acadêmico e não impede atendimento";

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
    void analiseComPendenciaRecusaComARestricaoDoSimulador() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);

        assertRecusa(p.id(), PENDENCIAS, true);
    }

    @Test
    void analiseComNaoVerificavelRecusaComARestricaoDoSimulador() throws Exception {
        String pedido = c.criarRascunho(c.authA);
        CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.IDENTIFICACAO);
        c.lerDocumentos(c.authA, pedido);
        Resposta analise = analisar(c, pedido);
        assertEquals("REQUER_CONFERENCIA", analise.json().get("resultadoGeral").asText(), "leitura nao conferida");

        assertRecusa(pedido, PENDENCIAS, true);
    }

    @Test
    void rascunhoSemDocumentosOuNuncaAnalisadoRecusaPorFaltaDeAnaliseVigente() throws Exception {
        String vazio = c.criarRascunho(c.authA);
        String lido = c.criarRascunho(c.authA);
        CenarioExtracao.enviar(c, c.authA, lido, AmostrasSinteticas.ENCAMINHAMENTO);
        c.lerDocumentos(c.authA, lido);

        assertRecusa(vazio, SEM_VIGENTE, true);
        assertRecusa(lido, SEM_VIGENTE, true);
    }

    @Test
    void falhaDeAnaliseRecusaPorFaltaDeAnaliseVigente() throws Exception {
        String pedido = c.criarRascunho(c.authA);
        c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.FALHA_ANALISE);

        assertRecusa(pedido, SEM_VIGENTE, true);
    }

    @Test
    void analiseSemPendenciasQuePerdeuAVigenciaNaoPermiteConcluir() throws Exception {
        Pedido incluido = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        assertEquals(201, incluir(c, incluido.id(), AmostrasSinteticas.SEM_MOTIVO).status());
        Pedido substituido = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        substituir(c, substituido.id(), substituido.documentos().get(1), AmostrasSinteticas.IDENTIFICACAO);
        Pedido conferido = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        String leitura = c.consultarLeitura(c.authA, conferido.id()).json().get("id").asText();
        assertEquals(200, c.decidir(c.authA, conferido.id(), leitura, CenarioConferencia.lote(CenarioConferencia
                .divergir(conferido.documentos().get(0), "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO"))).status());

        for (Pedido p : new Pedido[] {incluido, substituido, conferido}) {
            assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, pedido(p.id()).estado(),
                    "a alteracao tira a vigencia mas nao muda o estado (RC-09/RC-13)");
            assertRecusa(p.id(), SEM_VIGENTE, true);
        }
    }

    @Test
    void analiseEmAndamentoRecusaSemRegistrar() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        c.cenario.solicitacoes.forcarEstado(p.numero(), EstadoSolicitacao.PROCESSANDO);

        assertRecusa(p.id(), "está em andamento", false);
    }

    @Test
    void pedidoJaSubmetidoSemRegistroESoConsulta() throws Exception {
        String pedido = c.criarRascunho(c.authA);
        c.cenario.solicitacoes.forcarEstado(Long.parseLong(pedido), EstadoSolicitacao.SUBMETIDA_SIMULACAO);

        assertRecusa(pedido, "Este pedido já foi submetido e só pode ser consultado.", false);
    }

    @Test
    void estadoIncoerenteComAAnaliseVigenteNaoPermiteConcluir() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        c.cenario.solicitacoes.forcarEstado(p.numero(), EstadoSolicitacao.COM_PENDENCIAS);

        assertRecusa(p.id(), SEM_VIGENTE, true);
    }

    @Test
    void bancoIndisponivelNaoDeixaRegistroParcialERepetirConclui() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        c.cenario.submissoes.falharNaProximaSubmissao(new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes."));

        Resposta falha = submeter(c, c.authA, p.id());

        assertEquals(503, falha.status(), falha.corpo());
        assertEquals(0, c.cenario.submissoes.total());
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS, pedido(p.id()).estado());
        assertEquals(201, submeter(c, c.authA, p.id()).status(), "repetir depois conclui");
    }

    /** Confere a recusa 409 e que nada mudou: nenhuma submissao, mesmo estado e mesma revisao. */
    private void assertRecusa(String pedido, String trecho, boolean comRestricao) throws Exception {
        Solicitacao antes = pedido(pedido);
        int total = c.cenario.submissoes.total();

        Resposta r = submeter(c, c.authA, pedido);

        assertEquals(409, r.status(), r.corpo());
        assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
        String mensagem = r.json().get("mensagem").asText();
        assertTrue(mensagem.contains(trecho), mensagem);
        assertEquals(comRestricao, mensagem.contains(RESTRICAO), mensagem);
        Solicitacao depois = pedido(pedido);
        assertEquals(antes.estado(), depois.estado());
        assertEquals(antes.revisaoAtual(), depois.revisaoAtual());
        assertEquals(total, c.cenario.submissoes.total(), "nenhuma submissao registrada");
        assertEquals(404, SubmeterEndpointTest.comprovante(c, c.authA, pedido).status());
    }

    private Solicitacao pedido(String id) {
        return c.cenario.solicitacoes.buscarDaPaciente(Long.parseLong(id), 1).orElseThrow();
    }
}
