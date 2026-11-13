package br.com.regulacheck.validacao;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.validacao.model.CampoAvaliado;
import br.com.regulacheck.validacao.model.DocumentoAvaliado;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Montagem legivel das entradas do motor nos testes (dados sinteticos). Um documento comeca com os seis campos
 * "nao localizados" e cada metodo troca um campo. Os valores-base seguem o gabarito de {@code samples/gabarito.md}.
 */
public final class EntradasDeTeste {

    /** Data de referencia dos testes (envio em 13/11/2026, horario de Salvador). */
    public static final LocalDate REFERENCIA = LocalDate.of(2026, 11, 13);
    /** Nome sintetico da paciente. */
    public static final String NOME = "Maria Exemplo da Silva";
    /** Nascimento de paciente adulta. */
    public static final String NASCIMENTO_ADULTA = "1990-04-12";
    /** Nascimento de paciente com 15 anos na data de referencia. */
    public static final String NASCIMENTO_15_ANOS = "2011-05-01";
    /** Motivo sintetico do encaminhamento. */
    public static final String MOTIVO = "Avaliacao cardiologica ambulatorial (texto sintetico)";
    /** Emissao dentro da validade (24 dias antes da referencia). */
    public static final String EMISSAO = "2026-10-20";
    /** Trecho de origem sintetico usado nos campos confirmados. */
    public static final String TRECHO = "trecho sintetico";

    private EntradasDeTeste() {
    }

    /** Documento em montagem. */
    public static final class Doc {
        private final long id;
        private final Map<CodigoCampo, CampoAvaliado> campos = new EnumMap<>(CodigoCampo.class);

        private Doc(long id) {
            this.id = id;
            for (CodigoCampo codigo : CodigoCampo.values()) {
                campos.put(codigo, CampoAvaliado.semValor(codigo, SituacaoEvidencia.NAO_LOCALIZADO, null, null));
            }
        }

        /** Campo identificado e confirmado, com origem na pagina 1. */
        public Doc confirmado(CodigoCampo codigo, String valor) {
            campos.put(codigo, CampoAvaliado.confirmado(codigo, valor, 1, TRECHO));
            return this;
        }

        /** Campo em uma situacao sem valor (nao localizado, ilegivel, nao conferido, divergente). */
        public Doc com(CodigoCampo codigo, SituacaoEvidencia situacao) {
            campos.put(codigo, CampoAvaliado.semValor(codigo, situacao, null, null));
            return this;
        }

        /** @return o documento pronto */
        public DocumentoAvaliado pronto() {
            return new DocumentoAvaliado(id, new ArrayList<>(campos.values()));
        }
    }

    /** @return documento com os seis campos nao localizados */
    public static Doc documento(long id) {
        return new Doc(id);
    }

    /** @return encaminhamento conforme de paciente adulta (autorizacao nao localizada) */
    public static Doc encaminhamento(long id) {
        return new Doc(id).confirmado(CodigoCampo.TIPO_DOCUMENTO, "ENCAMINHAMENTO").confirmado(CodigoCampo.NOME, NOME)
                .confirmado(CodigoCampo.DATA_NASCIMENTO, NASCIMENTO_ADULTA)
                .confirmado(CodigoCampo.MOTIVO_ENCAMINHAMENTO, MOTIVO).confirmado(CodigoCampo.DATA_EMISSAO, EMISSAO);
    }

    /** @return documento de identificacao conforme de paciente adulta */
    public static Doc identificacao(long id) {
        return new Doc(id).confirmado(CodigoCampo.TIPO_DOCUMENTO, "IDENTIFICACAO").confirmado(CodigoCampo.NOME, NOME)
                .confirmado(CodigoCampo.DATA_NASCIMENTO, NASCIMENTO_ADULTA);
    }

    /** @return documento nao lido (protegido ou leitura vazia): os seis campos em {@code DOCUMENTO_NAO_LIDO} */
    public static DocumentoAvaliado naoLido(long id) {
        Doc doc = new Doc(id);
        for (CodigoCampo codigo : CodigoCampo.values()) {
            doc.com(codigo, SituacaoEvidencia.DOCUMENTO_NAO_LIDO);
        }
        return doc.pronto();
    }

    /** @return entrada com a data de referencia padrao */
    public static EntradaAvaliacao entrada(DocumentoAvaliado... documentos) {
        return new EntradaAvaliacao(List.of(documentos), REFERENCIA);
    }

    /** @return entrada com outra data de referencia */
    public static EntradaAvaliacao entrada(LocalDate referencia, DocumentoAvaliado... documentos) {
        return new EntradaAvaliacao(List.of(documentos), referencia);
    }
}
