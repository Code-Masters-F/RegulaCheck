package br.com.regulacheck.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import br.com.regulacheck.integracao.ValidadorDeSaidaEstruturada.Motivo;
import br.com.regulacheck.integracao.ValidadorDeSaidaEstruturada.Resultado;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SaidaForaDoContratoTest {

    private final ValidadorDeSaidaEstruturada validador = new ValidadorDeSaidaEstruturada(
            Set.of("R01", "R02"), ValidadorDeSaidaEstruturada.ESTADOS_PADRAO, 50);

    private static String item(String regra, String estado, String texto) {
        return "{\"codigoRegra\":\"" + regra + "\",\"estado\":\"" + estado + "\",\"texto\":\"" + texto + "\"}";
    }

    private static String saida(String... itens) {
        return "{\"itens\":[" + String.join(",", itens) + "]}";
    }

    private void assertInvalida(Motivo esperado, String json) {
        Resultado r = validador.validar(json);
        assertEquals(esperado, assertInstanceOf(Resultado.Invalida.class, r).motivo(), json);
    }

    @Test
    void saidaDentroDoContratoEAceita() {
        Resultado r = validador.validar(saida(item("R01", "PENDENTE", "Falta o CNS."), item("R02", "ATENDE", "ok")));
        List<ValidadorDeSaidaEstruturada.Item> itens = assertInstanceOf(Resultado.Valida.class, r).itens();
        assertEquals(2, itens.size());
        assertEquals("R01", itens.get(0).codigoRegra());
        assertEquals("Falta o CNS.", itens.get(0).texto());
        assertInstanceOf(Resultado.Valida.class, validador.validar("{\"itens\":[]}"));
    }

    @Test
    void campoDesconhecidoEmQualquerNivelInvalidaTudo() {
        assertInvalida(Motivo.CAMPO_DESCONHECIDO, "{\"itens\":[],\"perfil\":\"ADMINISTRADOR\"}");
        assertInvalida(Motivo.CAMPO_DESCONHECIDO, "{\"itens\":[],\"resultadoGeral\":\"SEM_PENDENCIAS\"}");
        assertInvalida(Motivo.CAMPO_DESCONHECIDO,
                "{\"itens\":[{\"codigoRegra\":\"R01\",\"estado\":\"ATENDE\",\"texto\":\"x\",\"estadoSolicitacao\":\"APROVADA\"}]}");
        assertInvalida(Motivo.CAMPO_DESCONHECIDO,
                "{\"itens\":[{\"codigoRegra\":\"R01\",\"estado\":\"ATENDE\",\"texto\":\"x\",\"perfil\":\"ADMINISTRADOR\"}]}");
    }

    @Test
    void codigoDeRegraForaDaListaDoProtocoloEhRejeitado() {
        assertInvalida(Motivo.CODIGO_DE_REGRA_DESCONHECIDO, saida(item("R99", "ATENDE", "x")));
        assertInvalida(Motivo.CODIGO_DE_REGRA_DESCONHECIDO, saida(item("r01", "ATENDE", "x")));
        assertInvalida(Motivo.CODIGO_DE_REGRA_DESCONHECIDO, saida(item("EXECUTAR_FERRAMENTA", "ATENDE", "x")));
    }

    @Test
    void estadoForaDaListaEhRejeitado() {
        assertInvalida(Motivo.ESTADO_INVALIDO, saida(item("R01", "APROVADO", "x")));
        assertInvalida(Motivo.ESTADO_INVALIDO, saida(item("R01", "atende", "x")));
    }

    @Test
    void textoGrandeDemaisEhRejeitado() {
        assertInvalida(Motivo.TEXTO_INVALIDO, saida(item("R01", "ATENDE", "x".repeat(51))));
    }

    @Test
    void estruturaInvalidaEJsonInvalidoSaoRejeitados() {
        assertInvalida(Motivo.JSON_INVALIDO, "isto nao e json");
        assertInvalida(Motivo.JSON_INVALIDO, "");
        assertInvalida(Motivo.JSON_INVALIDO, null);
        assertInvalida(Motivo.JSON_INVALIDO, "{\"itens\":[]} {\"itens\":[]}");
        assertInvalida(Motivo.JSON_INVALIDO, "{\"itens\":[],\"itens\":[]}");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA, "[]");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA, "{\"itens\":\"texto\"}");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA, "{\"outro\":[]}");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA, "{\"itens\":[\"texto\"]}");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA, "{\"itens\":[{\"codigoRegra\":\"R01\",\"estado\":\"ATENDE\"}]}");
        assertInvalida(Motivo.ESTRUTURA_INVALIDA,
                "{\"itens\":[{\"codigoRegra\":1,\"estado\":\"ATENDE\",\"texto\":\"x\"}]}");
    }

    @Test
    void itensDemaisSaoRejeitados() {
        String[] muitos = new String[51];
        java.util.Arrays.fill(muitos, item("R01", "ATENDE", "x"));
        assertInvalida(Motivo.ITENS_DEMAIS, saida(muitos));
    }

    @Test
    void motivoDeRejeicaoNaoReproduzOConteudoRecebido() {
        Resultado r = validador.validar(saida(item("R01", "SEGREDO-NO-ESTADO-123", "x")));
        assertFalse(r.toString().contains("SEGREDO-NO-ESTADO-123"));
    }
}
