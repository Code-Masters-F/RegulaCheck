package br.com.regulacheck.validacao;

import static br.com.regulacheck.validacao.EntradasDeTeste.documento;
import static br.com.regulacheck.validacao.EntradasDeTeste.encaminhamento;
import static br.com.regulacheck.validacao.EntradasDeTeste.entrada;
import static br.com.regulacheck.validacao.EntradasDeTeste.identificacao;
import static br.com.regulacheck.validacao.EntradasDeTeste.naoLido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import br.com.regulacheck.validacao.model.Veredito;
import br.com.regulacheck.validacao.regra.RegraDocumentoDeIdentificacao;
import br.com.regulacheck.validacao.regra.RegraEncaminhamentoPresente;
import org.junit.jupiter.api.Test;

/** RC-R01 (encaminhamento presente) e RC-R03 (documento de identificacao): FR-010, FR-012, FR-014. */
class RegraEncaminhamentoEIdentificacaoTest {

    private final RegraEncaminhamentoPresente r01 = new RegraEncaminhamentoPresente();
    private final RegraDocumentoDeIdentificacao r03 = new RegraDocumentoDeIdentificacao();

    @Test
    void r01AtendeComEncaminhamentoConfirmadoMesmoComOutroDocumentoNaoLido() {
        Veredito v = r01.avaliar(entrada(naoLido(1), encaminhamento(2).pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
        assertEquals(new Evidencia(2, CodigoCampo.TIPO_DOCUMENTO, SituacaoEvidencia.CONFIRMADO, 1,
                EntradasDeTeste.TRECHO), v.evidencias().get(0));
    }

    @Test
    void r01PendenteSemEncaminhamentoEntreDocumentosDeTipoConhecido() {
        Veredito v = r01.avaliar(entrada(identificacao(1).pronto(), documento(2).pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
        assertEquals(2, v.evidencias().size(), "mostra o tipo lido de cada documento");
    }

    @Test
    void r01PendenteEmPedidoSemDocumentos() {
        Veredito v = r01.avaliar(entrada());
        assertEquals(Resultado.PENDENTE, v.resultado());
        assertTrue(v.evidencias().isEmpty());
    }

    @Test
    void r01NaoVerificavelQuandoUmDocumentoNaoFoiLido() {
        Veredito v = r01.avaliar(entrada(identificacao(1).pronto(), naoLido(2)));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.DOCUMENTO_NAO_LIDO, v.motivo());
        assertEquals(2, v.evidencias().get(0).documentoId());
    }

    @Test
    void r01NaoVerificavelComTipoNaoConferidoContestadoOuIlegivel() {
        assertEquals(MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA, r01.avaliar(entrada(encaminhamento(1)
                .com(CodigoCampo.TIPO_DOCUMENTO, SituacaoEvidencia.NAO_CONFERIDO).pronto())).motivo());
        assertEquals(MotivoNaoVerificavel.DIVERGENCIA_APONTADA, r01.avaliar(entrada(encaminhamento(1)
                .com(CodigoCampo.TIPO_DOCUMENTO, SituacaoEvidencia.DIVERGENCIA_APONTADA).pronto())).motivo());
        assertEquals(MotivoNaoVerificavel.CAMPO_ILEGIVEL, r01.avaliar(entrada(encaminhamento(1)
                .com(CodigoCampo.TIPO_DOCUMENTO, SituacaoEvidencia.ILEGIVEL).pronto())).motivo());
    }

    @Test
    void r01TipoNaoInterpretavelNaoEAdivinhado() {
        Veredito v = r01.avaliar(entrada(encaminhamento(1).confirmado(CodigoCampo.TIPO_DOCUMENTO, "Encaminhamento?")
                .pronto()));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.VALOR_NAO_INTERPRETAVEL, v.motivo());
    }

    @Test
    void valeOTipoLidoEConfirmadoNaoOInformadoNoEnvio() {
        // Documento enviado como "encaminhamento", mas lido e confirmado como identificacao: conta como identificacao.
        var documento = identificacao(1).pronto();
        assertEquals(Resultado.PENDENTE, r01.avaliar(entrada(documento)).resultado());
        assertEquals(Resultado.ATENDE, r03.avaliar(entrada(documento)).resultado());
    }

    @Test
    void r03AtendeComIdentificacaoComNomeEDataConfirmados() {
        Veredito v = r03.avaliar(entrada(encaminhamento(1).pronto(), identificacao(2).pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
        assertEquals(3, v.evidencias().size());
        assertTrue(v.evidencias().stream().allMatch(e -> e.documentoId() == 2));
    }

    @Test
    void r03NaoVerificavelComDataDeNascimentoIlegivel() {
        Veredito v = r03.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).com(CodigoCampo.DATA_NASCIMENTO, SituacaoEvidencia.ILEGIVEL).pronto()));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.CAMPO_ILEGIVEL, v.motivo());
    }

    @Test
    void r03NaoVerificavelComIdentificacaoNaoLida() {
        Veredito v = r03.avaliar(entrada(encaminhamento(1).pronto(), naoLido(2)));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.DOCUMENTO_NAO_LIDO, v.motivo());
    }

    @Test
    void r03PendenteSemIdentificacaoOuComIdentificacaoSemNome() {
        assertEquals(Resultado.PENDENTE, r03.avaliar(entrada(encaminhamento(1).pronto())).resultado());
        assertEquals(Resultado.PENDENTE, r03.avaliar(entrada(encaminhamento(1).pronto(),
                identificacao(2).com(CodigoCampo.NOME, SituacaoEvidencia.NAO_LOCALIZADO).pronto())).resultado());
    }

    @Test
    void r03AtendeSeUmaDasIdentificacoesEstaCompleta() {
        Veredito v = r03.avaliar(entrada(
                identificacao(1).com(CodigoCampo.NOME, SituacaoEvidencia.ILEGIVEL).pronto(), identificacao(2).pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
    }
}
