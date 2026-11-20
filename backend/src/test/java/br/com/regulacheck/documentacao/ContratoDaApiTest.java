package br.com.regulacheck.documentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * RC-28: o mapa de rotas ({@code CONTRATO_API.md}) e o contrato OpenAPI ({@code docs/api/openapi.yaml}) listam o
 * mesmo conjunto de pares metodo + caminho, e toda operacao do contrato declara seu grupo ({@code tags}).
 *
 * <p>O contrato e lido por indentacao, sem biblioteca de YAML: caminhos com 2 espacos sob {@code paths:} e metodos
 * com 4, que e o formato do arquivo escrito a mao. Se o formato mudar, este teste falha, em vez de passar sem ver nada.
 */
class ContratoDaApiTest {

    private static final Pattern LINHA_DO_MAPA = Pattern.compile("^\\|\\s*(GET|POST|PUT|DELETE)\\s*\\|\\s*(/\\S*)\\s*\\|");
    private static final Pattern CAMINHO = Pattern.compile("^  (/\\S*):$");
    private static final Pattern METODO = Pattern.compile("^    (get|post|put|delete):$");

    private static List<String> linhas(String... partes) throws IOException {
        return Files.readAllLines(Path.of("..", partes), StandardCharsets.UTF_8);
    }

    private static Set<String> rotasDoMapa() throws IOException {
        Set<String> rotas = new TreeSet<>();
        for (String linha : linhas("CONTRATO_API.md")) {
            Matcher m = LINHA_DO_MAPA.matcher(linha);
            if (m.find()) {
                rotas.add(m.group(1) + " " + m.group(2));
            }
        }
        return rotas;
    }

    /** Pares metodo + caminho do bloco {@code paths} e, para cada um, se a linha seguinte declara {@code tags}. */
    private static List<String[]> operacoesDoContrato() throws IOException {
        List<String> contrato = linhas("docs", "api", "openapi.yaml");
        List<String[]> operacoes = new ArrayList<>();
        boolean emPaths = false;
        String caminho = null;
        for (int i = 0; i < contrato.size(); i++) {
            String linha = contrato.get(i);
            if (linha.equals("paths:")) {
                emPaths = true;
                continue;
            }
            if (emPaths && !linha.isEmpty() && !linha.startsWith(" ")) {
                break;
            }
            Matcher c = CAMINHO.matcher(linha);
            if (emPaths && c.matches()) {
                caminho = c.group(1);
            }
            Matcher m = METODO.matcher(linha);
            if (emPaths && m.matches()) {
                boolean comTag = i + 1 < contrato.size() && contrato.get(i + 1).startsWith("      tags: [");
                operacoes.add(new String[] {m.group(1).toUpperCase() + " " + caminho, Boolean.toString(comTag)});
            }
        }
        return operacoes;
    }

    @Test
    void mapaDeRotasEContratoTemOsMesmosParesMetodoECaminho() throws IOException {
        Set<String> doContrato = new TreeSet<>();
        operacoesDoContrato().forEach(o -> doContrato.add(o[0]));
        Set<String> doMapa = rotasDoMapa();

        assertTrue(doMapa.size() >= 31, "mapa de rotas lido: " + doMapa.size());
        assertEquals(doMapa, doContrato);
    }

    @Test
    void todaOperacaoDoContratoDeclaraSeuGrupo() throws IOException {
        List<String[]> operacoes = operacoesDoContrato();
        assertTrue(operacoes.size() >= 31, "operacoes lidas: " + operacoes.size());
        for (String[] operacao : operacoes) {
            assertEquals("true", operacao[1], operacao[0] + " sem tags");
        }
    }

    @Test
    void contratoDeclaraQue202NaoEUsadoEQueOServidorPadraoEAMesmaOrigem() throws IOException {
        String contrato = String.join("\n", linhas("docs", "api", "openapi.yaml"));

        assertTrue(contrato.contains("**Nenhuma rota usa 202:**"));
        assertTrue(!contrato.contains("\"202\":"), "nenhuma resposta 202 documentada");
        assertTrue(contrato.contains("servers:\n  - url: /\n"));
    }
}
