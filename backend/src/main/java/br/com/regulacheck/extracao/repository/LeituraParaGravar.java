package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;

/**
 * Leitura de um documento, ja validada, pronta para gravar.
 *
 * @param documentoId documento lido
 * @param leitura leitura do documento
 */
public record LeituraParaGravar(long documentoId, LeituraDeDocumento leitura) {
}
