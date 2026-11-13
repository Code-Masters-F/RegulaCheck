package br.com.regulacheck.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.service.EvidenciasDaLeitura;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import br.com.regulacheck.validacao.model.CampoAvaliado;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Combinacao leitura + conferencia (research D2, FR-007 a FR-010), sem HTTP nem banco. */
class EvidenciasDaLeituraTest {

    private static final Instant T0 = Instant.parse("2026-11-13T10:00:00Z");
    private static final long DOC = 41;

    /** Documento lido: tipo e nome identificados, nascimento ilegivel, demais nao localizados. */
    private static ExtracaoDocumento lido(long id) {
        return new ExtracaoDocumento(id, TipoDocumental.ENCAMINHAMENTO, SituacaoLeitura.LIDO, List.of(
                CampoLido.identificado(CodigoCampo.TIPO_DOCUMENTO, "ENCAMINHAMENTO", 1, "ENCAMINHAMENTO"),
                CampoLido.identificado(CodigoCampo.NOME, "Maria Exemplo", 1, "Paciente: Maria Exemplo"),
                CampoLido.ilegivel(CodigoCampo.DATA_NASCIMENTO),
                CampoLido.naoLocalizado(CodigoCampo.MOTIVO_ENCAMINHAMENTO),
                CampoLido.naoLocalizado(CodigoCampo.DATA_EMISSAO),
                CampoLido.naoLocalizado(CodigoCampo.AUTORIZACAO_ASSINADA)));
    }

    private static DecisaoConferencia confirmada(long doc, CodigoCampo codigo) {
        return new DecisaoConferencia(doc, codigo, TipoDecisao.CONFIRMADA, null, T0);
    }

    private static DecisaoConferencia divergente(long doc, CodigoCampo codigo, MotivoDivergencia motivo) {
        return new DecisaoConferencia(doc, codigo, TipoDecisao.DIVERGENTE, motivo, T0);
    }

    private static DocumentoAvaliado montarUm(ExtracaoDocumento documento, DecisaoConferencia... decisoes) {
        return EvidenciasDaLeitura.montar(new Extracao(1, 7, 3, T0, List.of(documento)), List.of(decisoes)).get(0);
    }

    @Test
    void identificadoSemDecisaoNaoEConferidoEMantemAOrigemSemValor() {
        CampoAvaliado nome = montarUm(lido(DOC)).campo(CodigoCampo.NOME);
        assertEquals(SituacaoEvidencia.NAO_CONFERIDO, nome.situacao());
        assertNull(nome.valor());
        assertEquals(1, nome.pagina());
        assertEquals("Paciente: Maria Exemplo", nome.trecho());
    }

    @Test
    void identificadoConfirmadoTrazOValor() {
        CampoAvaliado nome = montarUm(lido(DOC), confirmada(DOC, CodigoCampo.NOME)).campo(CodigoCampo.NOME);
        assertEquals(SituacaoEvidencia.CONFIRMADO, nome.situacao());
        assertEquals("Maria Exemplo", nome.valor());
    }

    @Test
    void identificadoComValorDiferenteApontadoNaoServeDeEvidencia() {
        CampoAvaliado nome = montarUm(lido(DOC),
                divergente(DOC, CodigoCampo.NOME, MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO))
                .campo(CodigoCampo.NOME);
        assertEquals(SituacaoEvidencia.DIVERGENCIA_APONTADA, nome.situacao());
        assertNull(nome.valor());
    }

    @Test
    void naoLocalizadoContaComoAusenteSalvoSeAPacienteDizQueOCampoExiste() {
        DocumentoAvaliado semDecisao = montarUm(lido(DOC));
        assertEquals(SituacaoEvidencia.NAO_LOCALIZADO, semDecisao.campo(CodigoCampo.MOTIVO_ENCAMINHAMENTO).situacao());
        DocumentoAvaliado contestado = montarUm(lido(DOC), divergente(DOC, CodigoCampo.MOTIVO_ENCAMINHAMENTO,
                MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO));
        assertEquals(SituacaoEvidencia.DIVERGENCIA_APONTADA,
                contestado.campo(CodigoCampo.MOTIVO_ENCAMINHAMENTO).situacao());
    }

    @Test
    void ilegivelContinuaIlegivelQualquerQueSejaADecisao() {
        DocumentoAvaliado documento = montarUm(lido(DOC), confirmada(DOC, CodigoCampo.DATA_NASCIMENTO));
        assertEquals(SituacaoEvidencia.ILEGIVEL, documento.campo(CodigoCampo.DATA_NASCIMENTO).situacao());
    }

    @Test
    void decisaoQueNaoCabeASituacaoDoCampoEIgnorada() {
        // Confirmar um nao localizado ou divergir de um identificado com o motivo errado nao "limpa" nada.
        DocumentoAvaliado documento = montarUm(lido(DOC), confirmada(DOC, CodigoCampo.MOTIVO_ENCAMINHAMENTO),
                divergente(DOC, CodigoCampo.NOME, MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO));
        assertEquals(SituacaoEvidencia.NAO_LOCALIZADO, documento.campo(CodigoCampo.MOTIVO_ENCAMINHAMENTO).situacao());
        assertEquals(SituacaoEvidencia.NAO_CONFERIDO, documento.campo(CodigoCampo.NOME).situacao());
    }

    @Test
    void decisaoDeOutroDocumentoNaoValeParaEste() {
        DocumentoAvaliado documento = montarUm(lido(DOC), confirmada(DOC + 1, CodigoCampo.NOME));
        assertEquals(SituacaoEvidencia.NAO_CONFERIDO, documento.campo(CodigoCampo.NOME).situacao());
    }

    @Test
    void documentoProtegidoOuDeLeituraVaziaTemOsSeisCamposNaoLidos() {
        for (SituacaoLeitura situacao : new SituacaoLeitura[] {SituacaoLeitura.PROTEGIDO,
                SituacaoLeitura.LEITURA_VAZIA}) {
            DocumentoAvaliado documento = montarUm(
                    new ExtracaoDocumento(DOC, TipoDocumental.IDENTIFICACAO, situacao, List.of()));
            assertTrue(documento.naoLido());
            assertTrue(documento.campos().stream()
                    .allMatch(c -> c.situacao() == SituacaoEvidencia.DOCUMENTO_NAO_LIDO && c.valor() == null));
        }
    }

    @Test
    void mantemAOrdemEOsIdsDosDocumentosDaLeitura() {
        List<DocumentoAvaliado> documentos = EvidenciasDaLeitura.montar(new Extracao(1, 7, 3, T0,
                List.of(lido(5), lido(3))), List.of());
        assertEquals(List.of(5L, 3L), documentos.stream().map(DocumentoAvaliado::documentoId).toList());
    }
}
