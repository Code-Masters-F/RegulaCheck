package br.com.regulacheck.extracao.service;

import br.com.regulacheck.extracao.repository.ConferenciaRegistrada;
import br.com.regulacheck.extracao.service.SituacaoConferencia.Avaliacao;
import java.time.Instant;

/**
 * A conferencia de uma leitura pronta para ser publicada: a leitura com as decisoes vigentes, a situacao calculada
 * e o instante da ultima alteracao.
 *
 * @param conferencia leitura e decisoes vigentes
 * @param avaliacao situacao da conferencia e contagens
 * @param atualizadaEm instante da decisao mais recente ou, se nao ha decisao, o da leitura; serve a quem precisa
 *     saber se uma analise ficou velha frente a uma conferencia nova
 */
public record VisaoConferencia(ConferenciaRegistrada conferencia, Avaliacao avaliacao, Instant atualizadaEm) {
}
