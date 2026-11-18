package br.com.regulacheck.documento;

import static br.com.regulacheck.reenvio.CenarioReenvio.algumaResolvida;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.historico;
import static br.com.regulacheck.reenvio.CenarioReenvio.incluir;
import static br.com.regulacheck.reenvio.CenarioReenvio.situacoes;
import static br.com.regulacheck.reenvio.CenarioReenvio.verificarConferindo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Inclusao do documento que faltava depois da analise (RC-16, US3; FR-012, FR-013, FR-015; SC-001, SC-002): aceita em
 * pedido nao submetido, com a mesma conferencia de arquivo, o mesmo limite e a mesma chave de tentativa do primeiro
 * envio; cria a revisao seguinte, tira a vigencia da analise e nao muda o estado nem resolve pendencia.
 */
class InclusaoAposAnaliseTest {

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
    void inclusaoDepoisDaAnaliseCriaRevisaoTiraAVigenciaEMantemOEstado() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO);
        JsonNode analise = historico(c, p.id()).get("analises").get(0);
        assertEquals("COM_PENDENCIAS", analise.get("resultadoGeral").asText(), "so o encaminhamento: R03 pendente");
        int revisaoAntes = c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().revisaoAtual();

        Resposta inclusao = incluir(c, p.id(), AmostrasSinteticas.IDENTIFICACAO);

        assertEquals(201, inclusao.status(), inclusao.corpo());
        assertEquals(revisaoAntes + 1, inclusao.json().get("versaoConjuntoDocumental").asInt());
        List<RevisaoRegistrada> revisoes = c.cenario.solicitacoes.revisoesDe(p.numero());
        assertEquals(MotivoRevisao.DOCUMENTO_ENVIADO, revisoes.get(revisoes.size() - 1).motivo());
        JsonNode pedido = c.api.get("/solicitacoes/" + p.id(), c.authA).json();
        assertEquals("COM_PENDENCIAS", pedido.get("estado").asText(), "incluir nao muda o estado");
        assertEquals("NAO_REALIZADA", pedido.get("analise").get("situacao").asText());
        JsonNode depois = historico(c, p.id());
        assertFalse(depois.has("vigenteId"));
        assertEquals(1, depois.get("analises").size());
        assertEquals(analise.get("pendencias"), depois.get("analises").get(0).get("pendencias"));
        assertFalse(algumaResolvida(depois), "incluir o arquivo nao resolve pendencia");
        assertEquals(0, c.cenario.analises.vigentesDoPedido(p.numero()));
    }

    @Test
    void documentoQueFaltavaIncluidoEVerificadoResolveAsPendencias() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO);
        incluir(c, p.id(), AmostrasSinteticas.IDENTIFICACAO);

        Resposta verificacao = verificarConferindo(c, p.id());

        assertEquals(201, verificacao.status(), verificacao.corpo());
        JsonNode nova = verificacao.json().get("analise");
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", nova.get("resultadoGeral").asText());
        Map<String, String> situacoes = situacoes(nova);
        assertEquals("RESOLVIDA", situacoes.get("RC-R03"));
        assertTrue(situacoes.values().stream().allMatch("RESOLVIDA"::equals), situacoes.toString());
        assertEquals(EstadoSolicitacao.SEM_PENDENCIAS_IDENTIFICADAS,
                c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().estado());
    }

    @Test
    void limiteDeTresERepeticaoDaTentativaValemDepoisDaAnalise() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        byte[] terceiro = ArquivosDeTeste.pdf(2);
        Resposta primeira = c.enviar(c.authA, p.id(), "OUTRO", "terceiro.pdf", terceiro, "tentativa-rc16");
        assertEquals(201, primeira.status(), primeira.corpo());
        int revisao = primeira.json().get("versaoConjuntoDocumental").asInt();

        Resposta repetida = c.enviar(c.authA, p.id(), "OUTRO", "terceiro.pdf", terceiro, "tentativa-rc16");
        Resposta quarto = c.enviar(c.authA, p.id(), "OUTRO", "quarto.pdf", ArquivosDeTeste.pdf(3), null);

        assertEquals(200, repetida.status(), "a mesma tentativa nao duplica");
        assertEquals(primeira.json().get("id"), repetida.json().get("id"));
        assertEquals(422, quarto.status(), quarto.corpo());
        assertEquals("REGRA_DE_NEGOCIO", quarto.json().get("codigo").asText());
        assertEquals(revisao, c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().revisaoAtual());
        assertEquals(3, c.api.get("/solicitacoes/" + p.id() + "/documentos", c.authA).json().get("total").asInt());
    }

    @Test
    void arquivoInvalidoNaInclusaoOuNaSubstituicaoMantemAAnaliseVigenteEARevisao() throws Exception {
        Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode antes = historico(c, p.id());
        int revisao = c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().revisaoAtual();

        Resposta inclusao = c.enviar(c.authA, p.id(), "OUTRO", "falso.pdf", ArquivosDeTeste.texto(), null);
        Resposta substituicao = c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                "protegido.pdf", ArquivosDeTeste.pdfProtegido(), null);

        for (Resposta r : List.of(inclusao, substituicao)) {
            assertTrue(r.status() == 400 || r.status() == 415, r.status() + " " + r.corpo());
        }
        assertEquals(antes.get("vigenteId"), historico(c, p.id()).get("vigenteId"), "a analise continua vigente");
        assertEquals(1, c.cenario.analises.vigentesDoPedido(p.numero()));
        assertEquals(revisao, c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1).orElseThrow().revisaoAtual());
        assertEquals(2, c.api.get("/solicitacoes/" + p.id() + "/documentos", c.authA).json().get("total").asInt());
        assertEquals(0, c.arquivosNaQuarentena());
        c.conferirIntegridade();
    }
}
