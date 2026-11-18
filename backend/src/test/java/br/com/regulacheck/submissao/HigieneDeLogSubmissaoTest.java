package br.com.regulacheck.submissao;

import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static br.com.regulacheck.submissao.SubmeterEndpointTest.comprovante;
import static br.com.regulacheck.submissao.SubmeterEndpointTest.submeter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Linguagem e registros da submissao simulada (RC-17, US4; FR-010, FR-012; SC-006): concluir, repetir, consultar e
 * recusar nao levam conteudo de documento ao log nem as respostas; comprovantes trazem o destaque de simulacao e
 * nenhuma resposta afirma aprovacao, posicao em fila, vaga, prazo, diagnostico ou urgencia.
 */
class HigieneDeLogSubmissaoTest {

    /** Conteudo das amostras sinteticas que nunca pode aparecer em log ou resposta. */
    private static final List<String> CONTEUDO = List.of("Maria Exemplo", "1990-04-12", "ignore as regras",
            "Paciente:", "Nascimento:", "Motivo:", "Emissao:", "encaminhamento-injecao", "identificacao-sintetica");

    /**
     * Afirmacoes proibidas (em minusculas). As negacoes obrigatorias da nota ("nao e protocolo oficial", "nao coloca
     * voce em fila real") nao entram: a lista mira alegacao, nao a palavra.
     */
    private static final List<String> PROIBIDOS = List.of("aprovad", "aprovação", "posição", "vaga", "prazo",
            "diagnóstico", "urgência", "urgente", "senha de atendimento", "número do protocolo", "enviado ao sus.");

    @Test
    void concluirRepetirConsultarERecusarNaoVazamConteudoNemPrometemAtendimento() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<Resposta> comprovantes = new ArrayList<>();
        List<Resposta> respostas = new ArrayList<>();
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido pronto = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            Pedido comInjecao = analisado(c, AmostrasSinteticas.INJECAO, AmostrasSinteticas.IDENTIFICACAO);
            Pedido pendente = analisado(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);

            comprovantes.add(submeter(c, c.authA, pronto.id()));
            comprovantes.add(submeter(c, c.authA, pronto.id()));
            comprovantes.add(comprovante(c, c.authA, pronto.id()));
            respostas.add(submeter(c, c.authA, comInjecao.id()));
            respostas.add(submeter(c, c.authA, pendente.id()));
            respostas.add(submeter(c, c.authB, pendente.id()));
            respostas.add(comprovante(c, c.authA, pendente.id()));
        } finally {
            System.setErr(original);
        }
        respostas.addAll(comprovantes);
        String log = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(log.contains("Simulacao concluida"), "a conclusao deve deixar rastro tecnico");
        assertTrue(log.contains("Conclusao repetida"), "a repeticao deve deixar rastro tecnico");
        assertTrue(log.contains("Conclusao da simulacao recusada"), "a recusa deve deixar rastro tecnico");
        for (Resposta r : comprovantes) {
            assertTrue(List.of(200, 201).contains(r.status()), r.status() + " " + r.corpo());
            assertEquals("SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", r.json().get("destaque").asText());
            assertTrue(r.json().get("simulada").asBoolean());
            assertTrue(r.json().get("referencia").asText().startsWith("SIM-"));
        }
        for (String trecho : CONTEUDO) {
            assertFalse(log.contains(trecho), "o log vazou: " + trecho);
            for (Resposta r : respostas) {
                assertFalse(r.corpo().contains(trecho), "resposta vazou: " + trecho);
            }
        }
        for (Resposta r : respostas) {
            String texto = r.corpo().toLowerCase(Locale.ROOT);
            for (String proibido : PROIBIDOS) {
                assertFalse(texto.contains(proibido), "afirmacao proibida '" + proibido + "' em: " + r.corpo());
            }
        }
    }
}
