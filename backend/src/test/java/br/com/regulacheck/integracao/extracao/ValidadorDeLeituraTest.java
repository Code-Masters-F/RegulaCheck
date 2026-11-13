package br.com.regulacheck.integracao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.integracao.extracao.ValidadorDeLeitura.Motivo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Contrato fechado da saida de um leitor (D3): aceita o que respeita, rejeita o resto sem eco do conteudo. */
class ValidadorDeLeituraTest {

    private static List<CampoLido> seisNaoLocalizados() {
        return Arrays.stream(CodigoCampo.values()).map(CampoLido::naoLocalizado).toList();
    }

    private static LeituraDeDocumento lida(List<CampoLido> campos) {
        return new LeituraDeDocumento(SituacaoLeitura.LIDO, campos);
    }

    private static List<CampoLido> comCampo(CampoLido trocado) {
        List<CampoLido> campos = new ArrayList<>(seisNaoLocalizados());
        campos.removeIf(c -> c.codigo() == trocado.codigo());
        campos.add(trocado);
        return campos;
    }

    private static Motivo motivo(LeituraDeDocumento leitura, TipoArquivo tipo, int paginas) {
        return ValidadorDeLeitura.verificar(leitura, tipo, paginas).orElse(null);
    }

    @Test
    void aceitaLidoComOsSeisCodigosEmQualquerSituacao() {
        List<CampoLido> campos = new ArrayList<>(seisNaoLocalizados());
        campos.set(0, CampoLido.identificado(CodigoCampo.TIPO_DOCUMENTO, "ENCAMINHAMENTO", 1, "trecho"));
        campos.set(1, CampoLido.ilegivel(CodigoCampo.NOME));
        assertTrue(ValidadorDeLeitura.verificar(lida(campos), TipoArquivo.PDF, 1).isEmpty());
    }

    @Test
    void aceitaProtegidoEVazioSemCampos() {
        assertTrue(ValidadorDeLeitura.verificar(LeituraDeDocumento.protegido(), TipoArquivo.PDF, 1).isEmpty());
        assertTrue(ValidadorDeLeitura.verificar(LeituraDeDocumento.vazia(), TipoArquivo.PNG, 1).isEmpty());
    }

    @Test
    void rejeitaCampoRepetidoOuFaltando() {
        List<CampoLido> repetido = new ArrayList<>(seisNaoLocalizados());
        repetido.add(CampoLido.ilegivel(CodigoCampo.NOME));
        assertEquals(Motivo.CAMPO_REPETIDO, motivo(lida(repetido), TipoArquivo.PDF, 1));
        List<CampoLido> faltando = new ArrayList<>(seisNaoLocalizados());
        faltando.remove(0);
        assertEquals(Motivo.CAMPOS_INCOMPLETOS, motivo(lida(faltando), TipoArquivo.PDF, 1));
        assertEquals(Motivo.CAMPOS_INCOMPLETOS, motivo(lida(List.of()), TipoArquivo.PDF, 1));
    }

    @Test
    void rejeitaValorEmCampoSemValor() {
        assertEquals(Motivo.VALOR_EM_CAMPO_SEM_VALOR, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.NAO_LOCALIZADO, "inventado", null, null))),
                TipoArquivo.PDF, 1));
        assertEquals(Motivo.VALOR_EM_CAMPO_SEM_VALOR, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.ILEGIVEL, "chute", null, null))), TipoArquivo.PDF, 1));
    }

    @Test
    void rejeitaIdentificadoSemValor() {
        assertEquals(Motivo.VALOR_AUSENTE, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.IDENTIFICADO, null, null, null))), TipoArquivo.PDF, 1));
        assertEquals(Motivo.VALOR_AUSENTE, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.IDENTIFICADO, "  ", null, null))),
                TipoArquivo.PDF, 1));
    }

    @Test
    void rejeitaOrigemEmCampoNaoIdentificado() {
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.ILEGIVEL, null, 1, null))), TipoArquivo.PDF, 1));
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                new CampoLido(CodigoCampo.NOME, SituacaoCampo.NAO_LOCALIZADO, null, null, "trecho"))),
                TipoArquivo.PDF, 1));
    }

    @Test
    void rejeitaPaginaInvalidaOuEmImagem() {
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", 0, null))), TipoArquivo.PDF, 3));
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", 4, null))), TipoArquivo.PDF, 3));
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", 1, null))), TipoArquivo.PNG, 1));
        assertTrue(ValidadorDeLeitura.verificar(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", null, null))), TipoArquivo.PNG, 1).isEmpty());
    }

    @Test
    void rejeitaValorOuTrechoGrandeOuComCaracterDeControle() {
        String grande = "x".repeat(ValidadorDeLeitura.TAMANHO_MAXIMO + 1);
        assertEquals(Motivo.VALOR_INVALIDO, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, grande, null, null))), TipoArquivo.PNG, 1));
        assertEquals(Motivo.VALOR_INVALIDO, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "a\nb", null, null))), TipoArquivo.PNG, 1));
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", null, grande))), TipoArquivo.PNG, 1));
        assertEquals(Motivo.ORIGEM_INVALIDA, motivo(lida(comCampo(
                CampoLido.identificado(CodigoCampo.NOME, "A", null, "t\u0000x"))), TipoArquivo.PNG, 1));
    }

    @Test
    void rejeitaNaoLidoComCamposEleituraAusente() {
        assertEquals(Motivo.CAMPOS_EM_LEITURA_NAO_LIDA, motivo(new LeituraDeDocumento(SituacaoLeitura.PROTEGIDO,
                seisNaoLocalizados()), TipoArquivo.PDF, 1));
        assertEquals(Motivo.CAMPOS_EM_LEITURA_NAO_LIDA, motivo(new LeituraDeDocumento(SituacaoLeitura.LEITURA_VAZIA,
                List.of(CampoLido.naoLocalizado(CodigoCampo.NOME))), TipoArquivo.PDF, 1));
        assertEquals(Motivo.LEITURA_AUSENTE, motivo(null, TipoArquivo.PDF, 1));
        assertEquals(Motivo.LEITURA_AUSENTE, motivo(new LeituraDeDocumento(null, List.of()), TipoArquivo.PDF, 1));
    }

    @Test
    void mensagemDaRejeicaoNaoReproduzOConteudo() {
        String segredo = "CONTEUDO-DO-DOCUMENTO-NAO-PODE-VAZAR";
        var excecao = assertThrows(LeituraIndisponivelException.class, () -> ValidadorDeLeitura.exigir(
                lida(comCampo(new CampoLido(CodigoCampo.NOME, SituacaoCampo.NAO_LOCALIZADO, segredo, null, null))),
                TipoArquivo.PDF, 1));
        assertFalse(excecao.getMessage().contains(segredo));
        assertTrue(excecao.getMessage().contains("VALOR_EM_CAMPO_SEM_VALOR"));
    }
}
