package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comparacao entre analises nas rotas existentes (RC-16, US2; FR-007 a FR-011; SC-002 a SC-004): {@code POST} e
 * {@code GET .../analises} e {@code POST .../resumo}. Nada aparece como resolvido antes de um novo resultado, e o
 * historico continua com as analises como foram registradas.
 */
class ComparacaoEndpointTest {

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
    void primeiraAnaliseNaoTemComparacaoESubstituirNaoResolveNada() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        Resposta primeira = pedir(c, c.authA, p.id());
        assertEquals(201, primeira.status());
        assertFalse(primeira.json().has("comparacao"));

        c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                AmostrasSinteticas.ENCAMINHAMENTO.nome(), AmostrasSinteticas.ENCAMINHAMENTO.bytes(), null);

        JsonNode depois = historico(c, c.authA, p.id()).json();
        JsonNode anterior = depois.get("analises").get(0);
        assertFalse(anterior.get("vigente").asBoolean(), "resultado anterior, nao vigente");
        assertFalse(anterior.has("comparacao"));
        assertEquals(primeira.json().get("pendencias"), anterior.get("pendencias"));
        assertEquals(409, c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).status(),
                "sem analise vigente nao ha resumo novo");
    }

    @Test
    void novaAnaliseComparaComAAnteriorEOResumoTrazAMesmaComparacao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        JsonNode primeira = pedir(c, c.authA, p.id()).json();
        c.substituir(c.authA, p.id(), p.documentos().get(0), "ENCAMINHAMENTO",
                AmostrasSinteticas.ENCAMINHAMENTO.nome(), AmostrasSinteticas.ENCAMINHAMENTO.bytes(), null);
        confirmarTudo(c, releitura(p));

        Resposta segunda = pedir(c, c.authA, p.id());

        assertEquals(201, segunda.status(), segunda.corpo());
        JsonNode comparacao = segunda.json().get("comparacao");
        assertEquals(primeira.get("id"), comparacao.get("analiseAnteriorId"));
        assertEquals(primeira.get("revisao"), comparacao.get("revisaoAnterior"));
        assertEquals(primeira.get("executadaEm"), comparacao.get("executadaEmAnterior"));
        assertEquals(Map.of("RC-R04", "RESOLVIDA"), situacoes(segunda.json()));
        Resposta resumo = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA);
        assertEquals(201, resumo.status(), resumo.corpo());
        assertEquals(comparacao, resumo.json().get("comparacao"));
        assertEquals(comparacao, historico(c, c.authA, p.id()).json().get("analises").get(0).get("comparacao"));
        assertEquals(comparacao, pedir(c, c.authA, p.id()).json().get("comparacao"), "repetir devolve a mesma");
    }

    @Test
    void mudancaSoDaConferenciaTambemComparaComAAnteriorDaMesmaRevisao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode naoConferida = pedir(c, c.authA, p.id()).json();
        assertEquals("REQUER_CONFERENCIA", naoConferida.get("resultadoGeral").asText());
        confirmarTudo(c, p);

        JsonNode conferida = pedir(c, c.authA, p.id()).json();

        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", conferida.get("resultadoGeral").asText());
        assertEquals(naoConferida.get("revisao"), conferida.get("comparacao").get("revisaoAnterior"));
        Map<String, String> situacoes = situacoes(conferida);
        assertEquals(naoConferida.get("pendencias").size(), situacoes.size());
        assertTrue(situacoes.values().stream().allMatch("RESOLVIDA"::equals), situacoes.toString());
    }

    @Test
    void remocaoGeraPendenciaNovaEOHistoricoComparaCadaAnaliseComAImediatamenteAnterior() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        JsonNode a1 = pedir(c, c.authA, p.id()).json();
        assertEquals(200, c.remover(c.authA, p.id(), p.documentos().get(1)).status());
        confirmarTudo(c, releitura(p));
        JsonNode a2 = pedir(c, c.authA, p.id()).json();
        assertEquals("NOVA", situacoes(a2).get("RC-R03"));
        assertEquals("ATENDE", item(a2, "RC-R03").get("resultadoAnterior").asText());
        assertEquals("PENDENTE", item(a2, "RC-R03").get("resultadoAtual").asText());

        // RC-16: o documento que faltava pode ser incluido depois da analise (enviar falha se nao vier 201).
        CenarioExtracao.enviar(c, c.authA, p.id(), AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, releitura(p));
        JsonNode a3 = pedir(c, c.authA, p.id()).json();
        assertEquals("RESOLVIDA", situacoes(a3).get("RC-R03"));

        List<JsonNode> analises = lista(historico(c, c.authA, p.id()).json().get("analises"));
        assertEquals(List.of(a3.get("id"), a2.get("id"), a1.get("id")),
                analises.stream().map(a -> a.get("id")).toList());
        assertEquals(a2.get("id"), analises.get(0).get("comparacao").get("analiseAnteriorId"));
        assertEquals(a1.get("id"), analises.get(1).get("comparacao").get("analiseAnteriorId"));
        assertFalse(analises.get(2).has("comparacao"));
        assertEquals(a1.get("resultados"), analises.get(2).get("resultados"), "a mais antiga continua intacta");
        assertEquals(a2.get("pendencias"), analises.get(1).get("pendencias"));
    }

    /** Le de novo os documentos (revisao atual) e devolve o pedido apontando a leitura nova. */
    private Pedido releitura(Pedido p) throws Exception {
        Resposta leitura = c.lerDocumentos(c.authA, p.id());
        assertEquals(201, leitura.status(), leitura.corpo());
        return new Pedido(p.id(), p.documentos(), leitura.json().get("id").asText());
    }

    private static Map<String, String> situacoes(JsonNode analise) {
        Map<String, String> situacoes = new LinkedHashMap<>();
        for (JsonNode item : analise.get("comparacao").get("itens")) {
            situacoes.put(item.get("codigoRegra").asText(), item.get("situacao").asText());
        }
        return situacoes;
    }

    private static JsonNode item(JsonNode analise, String codigoRegra) {
        for (JsonNode item : analise.get("comparacao").get("itens")) {
            if (codigoRegra.equals(item.get("codigoRegra").asText())) {
                return item;
            }
        }
        throw new IllegalArgumentException("Regra fora da comparacao: " + codigoRegra);
    }

    private static List<JsonNode> lista(JsonNode array) {
        List<JsonNode> itens = new ArrayList<>();
        array.forEach(itens::add);
        return itens;
    }
}
