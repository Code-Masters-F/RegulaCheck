package br.com.regulacheck.validacao.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Tudo o que o motor precisa para avaliar um pedido: os documentos (com leitura e conferencia ja combinadas) e a
 * data de referencia. O motor nao le relogio, banco nem arquivo.
 *
 * @param documentos documentos disponiveis da versao avaliada (pode ser vazio)
 * @param dataReferencia "data do envio" do conjunto documental, no horario de Salvador; usada pela validade do
 *     encaminhamento e pela idade da paciente
 */
public record EntradaAvaliacao(List<DocumentoAvaliado> documentos, LocalDate dataReferencia) {

    /** Copia defensiva. */
    public EntradaAvaliacao {
        documentos = List.copyOf(documentos);
        Objects.requireNonNull(dataReferencia, "dataReferencia");
    }
}
