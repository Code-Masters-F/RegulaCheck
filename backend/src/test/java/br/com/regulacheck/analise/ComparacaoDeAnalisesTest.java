package br.com.regulacheck.analise;

import static br.com.regulacheck.validacao.model.Resultado.ATENDE;
import static br.com.regulacheck.validacao.model.Resultado.NAO_APLICAVEL;
import static br.com.regulacheck.validacao.model.Resultado.NAO_VERIFICAVEL;
import static br.com.regulacheck.validacao.model.Resultado.PENDENTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.ResultadoGravado;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises.ItemComparado;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises.Situacao;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regra pura da comparacao entre analises (RC-16, FR-007 a FR-011). As expectativas vem da tabela de
 * {@code specs/020-reenviar-versao-corrigida/data-model.md}, nao da implementacao.
 */
class ComparacaoDeAnalisesTest {

    private static final String PROTOCOLO = "PROTOCOLO-ACADEMICO-FICTICIO";
    private static final Instant ANTES = Instant.parse("2026-11-17T10:00:00Z");
    private static final Instant DEPOIS = Instant.parse("2026-11-17T11:00:00Z");

    @Test
    void classificaResolvidaContinuaENovaEOmiteRegrasSemPendenciaNasDuas() {
        Analise anterior = analise(10, 4, ANTES, "v0.1",
                ATENDE, NAO_VERIFICAVEL, PENDENTE, PENDENTE, PENDENTE, ATENDE);
        Analise atual = analise(11, 6, DEPOIS, "v0.1",
                ATENDE, PENDENTE, ATENDE, PENDENTE, NAO_APLICAVEL, NAO_VERIFICAVEL);

        ComparacaoDeAnalises comparacao = ComparacaoDeAnalises.comparar(atual, anterior);

        assertNotNull(comparacao);
        assertEquals(10, comparacao.analiseAnteriorId());
        assertEquals(4, comparacao.revisaoAnterior());
        assertEquals(ANTES, comparacao.executadaEmAnterior());
        List<ItemComparado> itens = comparacao.itens();
        assertEquals(List.of("RC-R02", "RC-R03", "RC-R04", "RC-R05", "RC-R06"),
                itens.stream().map(ItemComparado::codigoRegra).toList(), "ordem da analise atual; R01 fora");
        assertItem(itens.get(0), Situacao.CONTINUA, NAO_VERIFICAVEL, PENDENTE);
        assertItem(itens.get(1), Situacao.RESOLVIDA, PENDENTE, ATENDE);
        assertItem(itens.get(2), Situacao.CONTINUA, PENDENTE, PENDENTE);
        assertItem(itens.get(3), Situacao.RESOLVIDA, PENDENTE, NAO_APLICAVEL);
        assertItem(itens.get(4), Situacao.NOVA, ATENDE, NAO_VERIFICAVEL);
        assertEquals("Descricao RC-R03", itens.get(1).descricaoRegra());
    }

    @Test
    void naoVerificavelQuePassaANaoSeAplicarEResolvida() {
        Analise anterior = analise(1, 1, ANTES, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_VERIFICAVEL);
        Analise atual = analise(2, 2, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);

        List<ItemComparado> itens = ComparacaoDeAnalises.comparar(atual, anterior).itens();

        assertEquals(1, itens.size());
        assertItem(itens.get(0), Situacao.RESOLVIDA, NAO_VERIFICAVEL, NAO_APLICAVEL);
    }

    @Test
    void semPendenciaEmNenhumaDasDuasDaComparacaoVazia() {
        Analise anterior = analise(1, 1, ANTES, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise atual = analise(2, 2, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);

        ComparacaoDeAnalises comparacao = ComparacaoDeAnalises.comparar(atual, anterior);

        assertNotNull(comparacao, "ha analise anterior: a comparacao existe, so nao tem itens");
        assertTrue(comparacao.itens().isEmpty());
    }

    @Test
    void semAnteriorOuComVersaoDiferenteNaoHaComparacao() {
        Analise atual = analise(2, 2, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise outraVersao = analise(1, 1, ANTES, "v0.2", PENDENTE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);

        assertNull(ComparacaoDeAnalises.comparar(atual, null));
        assertNull(ComparacaoDeAnalises.comparar(atual, outraVersao));
    }

    @Test
    void regraPresenteEmSoUmaDasAnalisesNaoAparece() {
        Analise anterior = new Analise(1, 1, false, ANTES, PROTOCOLO, "v0.1", null, null,
                ResultadoGeral.COM_PENDENCIAS, List.of(), List.of(resultado("RC-R09", PENDENTE)), List.of(), null,
                null);
        Analise atual = analise(2, 2, DEPOIS, "v0.1", ATENDE, PENDENTE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);

        assertTrue(ComparacaoDeAnalises.comparar(atual, anterior).itens().isEmpty(),
                "nenhuma regra pode aparecer como resolvida ou nova sem ter sido avaliada nas duas");
    }

    @Test
    void comAnteriorUsaAImediatamenteSeguinteNoHistoricoDoMaisRecente() {
        Analise maisAntiga = analise(1, 1, ANTES, "v0.1", PENDENTE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise meio = analise(2, 2, ANTES.plusSeconds(60), "v0.1",
                ATENDE, ATENDE, PENDENTE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise recente = analise(3, 3, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        List<Analise> historico = List.of(recente, meio, maisAntiga);

        ComparacaoDeAnalises daRecente = ComparacaoDeAnalises.comAnterior(recente, historico);
        ComparacaoDeAnalises doMeio = ComparacaoDeAnalises.comAnterior(meio, historico);

        assertEquals(2, daRecente.analiseAnteriorId());
        assertItem(daRecente.itens().get(0), Situacao.RESOLVIDA, PENDENTE, ATENDE);
        assertEquals(1, doMeio.analiseAnteriorId());
        assertEquals(List.of(Situacao.RESOLVIDA, Situacao.NOVA),
                doMeio.itens().stream().map(ItemComparado::situacao).toList());
        assertNull(ComparacaoDeAnalises.comAnterior(maisAntiga, historico), "a mais antiga nao tem anterior");
        assertNull(ComparacaoDeAnalises.comAnterior(
                analise(99, 9, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL), historico),
                "analise fora do historico nao e comparada");
    }

    @Test
    void compararNaoAlteraAsAnalises() {
        Analise anterior = analise(1, 1, ANTES, "v0.1", PENDENTE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise atual = analise(2, 2, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);
        Analise copiaAnterior = analise(1, 1, ANTES, "v0.1", PENDENTE, ATENDE, ATENDE, ATENDE, ATENDE,
                NAO_APLICAVEL);
        Analise copiaAtual = analise(2, 2, DEPOIS, "v0.1", ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, NAO_APLICAVEL);

        ComparacaoDeAnalises.comparar(atual, anterior);

        assertEquals(copiaAnterior, anterior);
        assertEquals(copiaAtual, atual);
    }

    private static void assertItem(ItemComparado item, Situacao situacao, Resultado antes, Resultado agora) {
        assertEquals(situacao, item.situacao(), item.codigoRegra());
        assertEquals(antes, item.resultadoAnterior(), item.codigoRegra());
        assertEquals(agora, item.resultadoAtual(), item.codigoRegra());
    }

    /** Analise com os resultados de R01 a R06, na ordem do protocolo. */
    private static Analise analise(long id, int revisao, Instant executadaEm, String versao,
            Resultado... resultados) {
        List<ResultadoGravado> gravados = new ArrayList<>();
        for (int i = 0; i < resultados.length; i++) {
            gravados.add(resultado("RC-R0" + (i + 1), resultados[i]));
        }
        boolean pendente = gravados.stream().anyMatch(r -> r.resultado() == PENDENTE);
        boolean naoVerificavel = gravados.stream().anyMatch(r -> r.resultado() == NAO_VERIFICAVEL);
        ResultadoGeral geral = pendente ? ResultadoGeral.COM_PENDENCIAS
                : naoVerificavel ? ResultadoGeral.REQUER_CONFERENCIA : ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS;
        return new Analise(id, revisao, false, executadaEm, PROTOCOLO, versao, executadaEm,
                LocalDate.parse("2026-11-17"), geral, List.of(), gravados, List.of(), null, null);
    }

    private static ResultadoGravado resultado(String codigo, Resultado resultado) {
        return new ResultadoGravado(codigo, "Descricao " + codigo, resultado, null);
    }
}
