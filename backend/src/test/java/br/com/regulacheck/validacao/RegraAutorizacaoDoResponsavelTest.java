package br.com.regulacheck.validacao;

import static br.com.regulacheck.validacao.EntradasDeTeste.NASCIMENTO_15_ANOS;
import static br.com.regulacheck.validacao.EntradasDeTeste.REFERENCIA;
import static br.com.regulacheck.validacao.EntradasDeTeste.documento;
import static br.com.regulacheck.validacao.EntradasDeTeste.encaminhamento;
import static br.com.regulacheck.validacao.EntradasDeTeste.entrada;
import static br.com.regulacheck.validacao.EntradasDeTeste.identificacao;
import static br.com.regulacheck.validacao.EntradasDeTeste.naoLido;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.EntradasDeTeste.Doc;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import br.com.regulacheck.validacao.model.Veredito;
import br.com.regulacheck.validacao.regra.RegraAutorizacaoDoResponsavel;
import org.junit.jupiter.api.Test;

/** RC-R06, autorizacao do responsavel (condicional): FR-017 e US2. */
class RegraAutorizacaoDoResponsavelTest {

    private final RegraAutorizacaoDoResponsavel r06 = new RegraAutorizacaoDoResponsavel();

    private static Doc menor(Doc doc) {
        return doc.confirmado(CodigoCampo.DATA_NASCIMENTO, NASCIMENTO_15_ANOS);
    }

    @Test
    void adultaNaoPrecisaDeAutorizacao() {
        Veredito v = r06.avaliar(entrada(encaminhamento(1).pronto(), identificacao(2).pronto()));
        assertEquals(Resultado.NAO_APLICAVEL, v.resultado());
    }

    @Test
    void menorSemAutorizacaoEmDocumentosLegiveisFicaPendente() {
        Veredito v = r06.avaliar(entrada(menor(encaminhamento(1)).pronto(), menor(identificacao(2)).pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
    }

    @Test
    void menorComAutorizacaoAssinadaConfirmadaAtende() {
        Veredito v = r06.avaliar(entrada(menor(encaminhamento(1)).pronto(), menor(identificacao(2)).pronto(),
                documento(3).confirmado(CodigoCampo.TIPO_DOCUMENTO, "AUTORIZACAO_RESPONSAVEL")
                        .confirmado(CodigoCampo.AUTORIZACAO_ASSINADA, "SIM").pronto()));
        assertEquals(Resultado.ATENDE, v.resultado());
        assertEquals(3, v.evidencias().get(1).documentoId());
    }

    @Test
    void menorComAutorizacaoConfirmadaComoNaoFicaPendente() {
        Veredito v = r06.avaliar(entrada(menor(identificacao(1)).pronto(),
                documento(2).confirmado(CodigoCampo.AUTORIZACAO_ASSINADA, "NAO").pronto()));
        assertEquals(Resultado.PENDENTE, v.resultado());
    }

    @Test
    void menorComDocumentoNaoLidoENenhumaAutorizacaoENaoVerificavel() {
        Veredito v = r06.avaliar(entrada(menor(identificacao(1)).pronto(), naoLido(2)));
        assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado());
        assertEquals(MotivoNaoVerificavel.DOCUMENTO_NAO_LIDO, v.motivo());
    }

    @Test
    void menorComAutorizacaoIlegivelENaoVerificavel() {
        Veredito v = r06.avaliar(entrada(menor(identificacao(1)).pronto(),
                documento(2).com(CodigoCampo.AUTORIZACAO_ASSINADA, SituacaoEvidencia.ILEGIVEL).pronto()));
        assertEquals(MotivoNaoVerificavel.CAMPO_ILEGIVEL, v.motivo());
    }

    @Test
    void dataDeNascimentoIlegivelNaoConferidaOuContestadaDeixaACondicaoIndeterminada() {
        for (SituacaoEvidencia situacao : new SituacaoEvidencia[] {SituacaoEvidencia.ILEGIVEL,
                SituacaoEvidencia.NAO_CONFERIDO, SituacaoEvidencia.DIVERGENCIA_APONTADA}) {
            // O encaminhamento traz uma data legivel, mas a condicao usa so a do documento de identificacao.
            Veredito v = r06.avaliar(entrada(encaminhamento(1).pronto(),
                    identificacao(2).com(CodigoCampo.DATA_NASCIMENTO, situacao).pronto()));
            assertEquals(Resultado.NAO_VERIFICAVEL, v.resultado(), situacao.name());
            assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, v.motivo(), situacao.name());
        }
    }

    @Test
    void semDocumentoDeIdentificacaoACondicaoEIndeterminada() {
        Veredito v = r06.avaliar(entrada(encaminhamento(1).pronto()));
        assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, v.motivo());
    }

    @Test
    void identificacoesComDatasDiferentesDeixamACondicaoIndeterminada() {
        Veredito v = r06.avaliar(entrada(identificacao(1).pronto(), menor(identificacao(2)).pronto()));
        assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, v.motivo());
    }

    @Test
    void nascimentoPosteriorADataDeReferenciaNaoEPresumido() {
        Veredito v = r06.avaliar(entrada(identificacao(1)
                .confirmado(CodigoCampo.DATA_NASCIMENTO, REFERENCIA.plusDays(1).toString()).pronto()));
        assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, v.motivo());
    }

    @Test
    void quemCompleta18AnosNaDataDeReferenciaEAdulta() {
        String aniversario = REFERENCIA.minusYears(18).toString();
        assertEquals(Resultado.NAO_APLICAVEL, r06.avaliar(entrada(identificacao(1)
                .confirmado(CodigoCampo.DATA_NASCIMENTO, aniversario).pronto())).resultado());
        String vespera = REFERENCIA.minusYears(18).plusDays(1).toString();
        assertEquals(Resultado.PENDENTE, r06.avaliar(entrada(identificacao(1)
                .confirmado(CodigoCampo.DATA_NASCIMENTO, vespera).pronto())).resultado());
    }
}
