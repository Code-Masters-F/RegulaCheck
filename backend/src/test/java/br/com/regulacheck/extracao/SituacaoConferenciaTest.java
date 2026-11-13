package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.extracao.service.SituacaoConferencia.Avaliacao;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regra pura da situacao da conferencia (D4), sem HTTP nem banco. */
class SituacaoConferenciaTest {

    private static final Instant T0 = Instant.parse("2026-11-13T10:00:00Z");
    private static final long DOC = 41;

    private static Extracao leitura(ExtracaoDocumento... documentos) {
        return new Extracao(1, 7, 1, T0, List.of(documentos));
    }

    private static ExtracaoDocumento lido(CampoLido... campos) {
        return new ExtracaoDocumento(DOC, TipoDocumental.ENCAMINHAMENTO, SituacaoLeitura.LIDO, List.of(campos));
    }

    private static ExtracaoDocumento naoLido(long id, SituacaoLeitura situacao) {
        return new ExtracaoDocumento(id, TipoDocumental.OUTRO, situacao, List.of());
    }

    private static CampoLido nome() {
        return CampoLido.identificado(CodigoCampo.NOME, "Maria Exemplo", 1, "Paciente: Maria Exemplo");
    }

    private static CampoLido emissao() {
        return CampoLido.identificado(CodigoCampo.DATA_EMISSAO, "2026-11-01", 1, "Emitido em 2026-11-01");
    }

    private static DecisaoConferencia confirmada(CodigoCampo codigo) {
        return new DecisaoConferencia(DOC, codigo, TipoDecisao.CONFIRMADA, null, T0.plusSeconds(5));
    }

    private static DecisaoConferencia divergente(CodigoCampo codigo, MotivoDivergencia motivo) {
        return new DecisaoConferencia(DOC, codigo, TipoDecisao.DIVERGENTE, motivo, T0.plusSeconds(6));
    }

    @Test
    void campoIdentificadoSemDecisaoEPendente() {
        Avaliacao a = SituacaoConferencia.avaliar(leitura(lido(nome(), emissao())), true, List.of());
        assertEquals(SituacaoConferencia.PENDENTE, a.situacao());
        assertEquals(2, a.camposSemDecisao());
        assertEquals(0, a.divergencias());
    }

    @Test
    void todosOsIdentificadosConfirmadosSaoConferidos() {
        Avaliacao a = SituacaoConferencia.avaliar(leitura(lido(nome(), emissao())), true,
                List.of(confirmada(CodigoCampo.NOME), confirmada(CodigoCampo.DATA_EMISSAO)));
        assertEquals(SituacaoConferencia.CONFERIDA, a.situacao());
        assertEquals(0, a.camposSemDecisao());
    }

    @Test
    void decisaoDivergenteExigeConferencia() {
        Avaliacao a = SituacaoConferencia.avaliar(leitura(lido(nome(), emissao())), true,
                List.of(confirmada(CodigoCampo.NOME),
                        divergente(CodigoCampo.DATA_EMISSAO, MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO)));
        assertEquals(SituacaoConferencia.REQUER_CONFERENCIA, a.situacao());
        assertEquals(1, a.divergencias());
    }

    @Test
    void campoIlegivelEmDocumentoLidoExigeConferenciaMesmoComTudoConfirmado() {
        Avaliacao a = SituacaoConferencia.avaliar(
                leitura(lido(nome(), CampoLido.ilegivel(CodigoCampo.DATA_NASCIMENTO))), true,
                List.of(confirmada(CodigoCampo.NOME)));
        assertEquals(SituacaoConferencia.REQUER_CONFERENCIA, a.situacao());
        assertEquals(1, a.camposNaoVerificados());
    }

    @Test
    void decisaoInjetadaEmCampoIlegivelNaoLimpaARequisicaoDeConferencia() {
        Avaliacao a = SituacaoConferencia.avaliar(
                leitura(lido(CampoLido.ilegivel(CodigoCampo.DATA_NASCIMENTO))), true,
                List.of(confirmada(CodigoCampo.DATA_NASCIMENTO)));
        assertEquals(SituacaoConferencia.REQUER_CONFERENCIA, a.situacao());
    }

    @Test
    void documentoNaoLidoExigeConferencia() {
        for (SituacaoLeitura situacao : List.of(SituacaoLeitura.PROTEGIDO, SituacaoLeitura.LEITURA_VAZIA)) {
            Avaliacao a = SituacaoConferencia.avaliar(leitura(lido(nome()), naoLido(42, situacao)), true,
                    List.of(confirmada(CodigoCampo.NOME)));
            assertEquals(SituacaoConferencia.REQUER_CONFERENCIA, a.situacao(), situacao.name());
        }
    }

    @Test
    void naoLocalizadoSemDivergenciaNaoPesa() {
        Avaliacao a = SituacaoConferencia.avaliar(
                leitura(lido(nome(), CampoLido.naoLocalizado(CodigoCampo.AUTORIZACAO_ASSINADA))), true,
                List.of(confirmada(CodigoCampo.NOME)));
        assertEquals(SituacaoConferencia.CONFERIDA, a.situacao());
        assertEquals(1, a.camposNaoVerificados());
    }

    @Test
    void naoLocalizadoComDivergenciaExigeConferencia() {
        Avaliacao a = SituacaoConferencia.avaliar(
                leitura(lido(nome(), CampoLido.naoLocalizado(CodigoCampo.AUTORIZACAO_ASSINADA))), true,
                List.of(confirmada(CodigoCampo.NOME),
                        divergente(CodigoCampo.AUTORIZACAO_ASSINADA, MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO)));
        assertEquals(SituacaoConferencia.REQUER_CONFERENCIA, a.situacao());
        assertEquals(1, a.divergencias());
    }

    @Test
    void leituraDesatualizadaTemPrecedenciaSobreTudo() {
        Avaliacao a = SituacaoConferencia.avaliar(leitura(lido(nome())), false,
                List.of(divergente(CodigoCampo.NOME, MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO)));
        assertEquals(SituacaoConferencia.DESATUALIZADA, a.situacao());
    }

    @Test
    void contagensRefletemALeitura() {
        Avaliacao a = SituacaoConferencia.avaliar(
                leitura(lido(nome(), emissao(), CampoLido.ilegivel(CodigoCampo.DATA_NASCIMENTO),
                        CampoLido.naoLocalizado(CodigoCampo.MOTIVO_ENCAMINHAMENTO))),
                true, List.of(divergente(CodigoCampo.NOME, MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO)));
        // ilegivel + nao localizado + o identificado que a paciente apontou como divergente (contrato: naoVerificado)
        assertEquals(3, a.camposNaoVerificados());
        assertEquals(1, a.camposSemDecisao());
        assertEquals(1, a.divergencias());
    }

    @Test
    void aplicabilidadeDaDecisaoPorSituacaoDoCampo() {
        // identificado: confirmar, ou divergir por valor diferente
        assertTrue(SituacaoConferencia.aplicavel(SituacaoCampo.IDENTIFICADO, TipoDecisao.CONFIRMADA, null));
        assertTrue(SituacaoConferencia.aplicavel(SituacaoCampo.IDENTIFICADO, TipoDecisao.DIVERGENTE,
                MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO));
        assertFalse(SituacaoConferencia.aplicavel(SituacaoCampo.IDENTIFICADO, TipoDecisao.DIVERGENTE,
                MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO));
        // nao localizado: so "o campo existe"
        assertTrue(SituacaoConferencia.aplicavel(SituacaoCampo.NAO_LOCALIZADO, TipoDecisao.DIVERGENTE,
                MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO));
        assertFalse(SituacaoConferencia.aplicavel(SituacaoCampo.NAO_LOCALIZADO, TipoDecisao.CONFIRMADA, null));
        assertFalse(SituacaoConferencia.aplicavel(SituacaoCampo.NAO_LOCALIZADO, TipoDecisao.DIVERGENTE,
                MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO));
        // ilegivel: nenhuma
        assertFalse(SituacaoConferencia.aplicavel(SituacaoCampo.ILEGIVEL, TipoDecisao.CONFIRMADA, null));
        assertFalse(SituacaoConferencia.aplicavel(SituacaoCampo.ILEGIVEL, TipoDecisao.DIVERGENTE,
                MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO));
    }
}
