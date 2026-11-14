package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.avaliacaoComIntervalo;
import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAnalise.servico;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FR-022 e FR-023 (SC-008): pedir, repetir, consultar, recusar pedido alheio, descartar por alteracao e falhar nao
 * deixam no log nem nas mensagens de erro valor ou trecho dos documentos (incluindo o que tenta dar ordens ao
 * sistema); ficam so identificadores tecnicos, codigos e contagens.
 */
class HigieneDeLogAnaliseTest {

    /** Conteudo das amostras sinteticas que nunca pode aparecer em log ou erro. */
    private static final List<String> CONTEUDO = List.of("Maria Exemplo", "1990-04-12", "ignore as regras",
            "Paciente:", "Nascimento:", "Motivo:", "Emissao:", "encaminhamento-injecao", "identificacao-sintetica");

    @Test
    void analisesNaoLevamConteudoDoDocumentoAoLogNemAosErros() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> erros = new ArrayList<>();
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = lido(c, AmostrasSinteticas.INJECAO, AmostrasSinteticas.IDENTIFICACAO);
            pedir(c, c.authA, p.id());
            pedir(c, c.authA, p.id());
            historico(c, c.authA, p.id());
            Resposta alheio = pedir(c, c.authB, p.id());
            erros.add(alheio.corpo());

            c.cenario.relogio.avancar(Duration.ofSeconds(30));
            confirmarTudo(c, p);
            c.cenario.relogio.avancar(Duration.ofSeconds(30));
            erros.add(assertThrows(ExcecaoNegocio.class, () -> servico(c, avaliacaoComIntervalo(c,
                    () -> c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                            AmostrasSinteticas.ENCAMINHAMENTO.nome(), AmostrasSinteticas.ENCAMINHAMENTO.bytes(),
                            null))).analisar(PACIENTE_A, p.numero())).getMessage());

            c.cenario.relogio.avancar(Duration.ofSeconds(30));
            c.lerDocumentos(c.authA, p.id());
            c.cenario.protocolos.definirRegras(1, List.of());
            erros.add(pedir(c, c.authA, p.id()).corpo());
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Analise gravada"), "a gravacao deve deixar rastro tecnico");
        assertTrue(log.contains("Analise descartada"), "o descarte deve deixar rastro tecnico");
        assertTrue(log.contains("Analise encerrada sem resultado"), "a falha deve deixar rastro tecnico");
        for (String trecho : CONTEUDO) {
            assertFalse(log.contains(trecho), "o log vazou: " + trecho);
            for (String erro : erros) {
                assertFalse(erro.contains(trecho), "erro vazou: " + trecho);
            }
        }
        assertFalse(log.contains("\tat "), "sem stack trace no log de recusas e falhas esperadas");
    }
}
