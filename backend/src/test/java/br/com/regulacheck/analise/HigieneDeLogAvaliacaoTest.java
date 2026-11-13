package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_B;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.analise.CenarioAvaliacao.servico;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.analise.service.AvaliacaoDoPedido;
import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.validacao.motor.ProtocoloNaoSuportadoException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FR-011 e FR-028: avaliar (com documento que traz texto de instrucao), recusar pedido alheio, sem leitura ou com
 * versao sem regras nao deixa no log nem nas mensagens de erro valor ou trecho dos documentos; ficam so
 * identificadores tecnicos, codigos e contagens. O conteudo do documento tambem nao entra nas explicacoes.
 */
class HigieneDeLogAvaliacaoTest {

    /** Conteudo das amostras sinteticas que nunca pode aparecer em log ou erro. */
    private static final List<String> CONTEUDO = List.of("Maria Exemplo", "1990-04-12", "ignore as regras",
            "Paciente:", "Nascimento:", "Motivo:", "Emissao:", "encaminhamento-injecao", "identificacao-sintetica");

    @Test
    void avaliacaoNaoLevaConteudoDoDocumentoAoLogNemAosErros() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> mensagens = new ArrayList<>();
        AvaliacaoDoPedido resultado;
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            AvaliacaoService avaliacao = servico(c);
            Pedido pedido = lido(c, AmostrasSinteticas.INJECAO, AmostrasSinteticas.IDENTIFICACAO);
            confirmarTudo(c, pedido);
            resultado = avaliacao.avaliar(PACIENTE_A, pedido.numero());

            mensagens.add(assertThrows(ExcecaoNegocio.class,
                    () -> avaliacao.avaliar(PACIENTE_B, pedido.numero())).getMessage());
            String semLeitura = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.INJECAO);
            mensagens.add(assertThrows(ExcecaoNegocio.class,
                    () -> avaliacao.avaliar(PACIENTE_A, Long.parseLong(semLeitura))).getMessage());
            c.cenario.protocolos.definirRegras(1, List.of());
            mensagens.add(assertThrows(ProtocoloNaoSuportadoException.class,
                    () -> avaliacao.avaliar(PACIENTE_A, pedido.numero())).getMessage());
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Criterios avaliados"), "a avaliacao deve deixar rastro tecnico");
        assertTrue(log.contains("Versao do protocolo nao suportada"), "a versao recusada deve deixar rastro tecnico");
        for (String trecho : CONTEUDO) {
            assertFalse(log.contains(trecho), "o log vazou: " + trecho);
            for (String mensagem : mensagens) {
                assertFalse(mensagem.contains(trecho), "mensagem de erro vazou: " + trecho);
            }
            resultado.avaliacao().pendencias().forEach(p -> assertFalse(p.explicacao().contains(trecho),
                    "explicacao com conteudo do documento: " + trecho));
        }
        assertFalse(log.contains("\tat "), "sem stack trace no log de uma recusa esperada");
        // O texto de instrucao no motivo nao mudou nada: o caso segue o gabarito das amostras oficiais.
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", resultado.avaliacao().resultadoGeral().name());
        // Os objetos com dados do documento nao os mostram em texto (toString), caso alguem os registre.
        resultado.avaliacao().resultados().forEach(r -> r.veredito().evidencias().forEach(
                e -> assertFalse(e.toString().contains("Paciente:"), "toString da evidencia com trecho")));
    }
}
