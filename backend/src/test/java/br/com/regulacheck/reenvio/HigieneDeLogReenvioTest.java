package br.com.regulacheck.reenvio;

import static br.com.regulacheck.reenvio.CenarioReenvio.PACIENTE_A;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.reenvio.CenarioReenvio.confirmarTudo;
import static br.com.regulacheck.reenvio.CenarioReenvio.reenviar;
import static br.com.regulacheck.reenvio.CenarioReenvio.servico;
import static br.com.regulacheck.reenvio.CenarioReenvio.substituir;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import br.com.regulacheck.resumo.service.GeradorResumoMockado;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FR-019 e FR-020 (SC-007): parar na conferencia, concluir, repetir, recusar pedido alheio e falhar na analise nao
 * deixam no log nem nas respostas de erro valor ou trecho dos documentos (inclusive o que tenta dar ordens ao
 * sistema); ficam so identificadores, etapa, codigos e contagens.
 */
class HigieneDeLogReenvioTest {

    /** Conteudo das amostras sinteticas que nunca pode aparecer em log ou erro. */
    private static final List<String> CONTEUDO = List.of("Maria Exemplo", "1990-04-12", "ignore as regras",
            "Paciente:", "Nascimento:", "Motivo:", "Emissao:", "encaminhamento-injecao", "identificacao-sintetica");

    @Test
    void verificacaoNaoLevaConteudoDoDocumentoAoLogNemAosErros() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> erros = new ArrayList<>();
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
            substituir(c, p.id(), p.documentos().get(0), AmostrasSinteticas.INJECAO);
            Resposta parada = reenviar(c, c.authA, p.id());
            confirmarTudo(c, p.id(), parada.json().get("extracaoId").asText());
            erros.add(parada.corpo());
            reenviar(c, c.authA, p.id());
            reenviar(c, c.authA, p.id());
            erros.add(reenviar(c, c.authB, p.id()).corpo());

            String atual = c.api.get("/solicitacoes/" + p.id() + "/documentos", c.authA).json().get("documentos")
                    .get(0).get("id").asText();
            substituir(c, p.id(), atual, AmostrasSinteticas.ENCAMINHAMENTO);
            Resposta novaParada = reenviar(c, c.authA, p.id());
            confirmarTudo(c, p.id(), novaParada.json().get("extracaoId").asText());
            AvaliacaoService quebrada = new AvaliacaoService(c.cenario.solicitacoes, c.cenario.extracoes,
                    c.cenario.conferencias, c.cenario.documentos, c.cenario.protocolos,
                    MotorDeRegras.doProtocoloAcademico()) {
                @Override
                public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                    throw new IllegalStateException("Paciente: Maria Exemplo da Silva (nao pode ir ao log)");
                }
            };
            erros.add(assertThrows(ExcecaoNegocio.class, () -> servico(c,
                    LeitoresDeTeste.simuladoComTodasAsAmostras(), quebrada, new GeradorResumoMockado())
                    .verificar(PACIENTE_A, p.numero())).getMessage());
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Verificacao parada na conferencia"), "a parada deve deixar rastro tecnico");
        assertTrue(log.contains("Verificacao concluida"), "a conclusao deve deixar rastro tecnico");
        assertTrue(log.contains("Verificacao interrompida"), "a falha deve deixar rastro tecnico");
        for (String trecho : CONTEUDO) {
            assertFalse(log.contains(trecho), "o log vazou: " + trecho);
            for (String erro : erros) {
                assertFalse(erro.contains(trecho), "resposta vazou: " + trecho);
            }
        }
    }
}
