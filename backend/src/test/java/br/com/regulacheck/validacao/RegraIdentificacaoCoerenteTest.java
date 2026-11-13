package br.com.regulacheck.validacao;

import static br.com.regulacheck.validacao.EntradasDeTeste.documento;
import static br.com.regulacheck.validacao.EntradasDeTeste.encaminhamento;
import static br.com.regulacheck.validacao.EntradasDeTeste.entrada;
import static br.com.regulacheck.validacao.EntradasDeTeste.identificacao;
import static br.com.regulacheck.validacao.EntradasDeTeste.naoLido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import br.com.regulacheck.validacao.model.Veredito;
import br.com.regulacheck.validacao.regra.RegraIdentificacaoCoerente;
import org.junit.jupiter.api.Test;

/** RC-R02, identificacao coerente entre documentos (FR-013). */
class RegraIdentificacaoCoerenteTest {

    private final RegraIdentificacaoCoerente r02 = new RegraIdentificacaoCoerente();

    @Test
    void atendeComNomeEDataIguaisEmDoisDocumentos() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(), identificacao(2).pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
        assertEquals(4, v.evidencias().size());
    }

    @Test
    void acentoCaixaEEspacosExtrasNaoSaoDivergencia() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).confirmado(CodigoCampo.NOME, "  MARIA  Exemplo da   Sílva ").pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
    }

    @Test
    void letrasDiferentesSaoDivergenciaComprovada() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).confirmado(CodigoCampo.NOME, "Mariana Exemplo Souza").pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
        assertEquals(2, v.evidencias().size());
        assertTrue(v.evidencias().stream().allMatch(e -> e.codigo() == CodigoCampo.NOME));
    }

    @Test
    void datasDeNascimentoDiferentesSaoDivergencia() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).confirmado(CodigoCampo.DATA_NASCIMENTO, "1990-04-13").pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
        assertTrue(v.evidencias().stream().allMatch(e -> e.codigo() == CodigoCampo.DATA_NASCIMENTO));
    }

    @Test
    void divergenciaComprovadaPrevaleceSobreCampoIlegivel() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).confirmado(CodigoCampo.NOME, "Mariana Exemplo Souza").pronto(),
                documento(3).com(CodigoCampo.NOME, SituacaoEvidencia.ILEGIVEL).pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
    }

    @Test
    void umSoDocumentoComparavelENaoVerificavel() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(), documento(2).pronto()));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.DOCUMENTOS_INSUFICIENTES, v.motivo());
    }

    @Test
    void documentoNaoLidoImpedeAfirmarCoerencia() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(), identificacao(2).pronto(), naoLido(3)));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.DOCUMENTO_NAO_LIDO, v.motivo());
    }

    @Test
    void nomeNaoConferidoOuContestadoNaoServeDeEvidencia() {
        assertEquals(MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA, r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).com(CodigoCampo.NOME, SituacaoEvidencia.NAO_CONFERIDO).pronto())).motivo());
        assertEquals(MotivoNaoVerificavel.DIVERGENCIA_APONTADA, r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).com(CodigoCampo.NOME, SituacaoEvidencia.DIVERGENCIA_APONTADA).pronto())).motivo());
    }

    @Test
    void dataConfirmadaEmFormatoInesperadoNaoEInterpretada() {
        Veredito v = r02.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).confirmado(CodigoCampo.DATA_NASCIMENTO, "12/04/1990").pronto()));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.VALOR_NAO_INTERPRETAVEL, v.motivo());
    }
}
