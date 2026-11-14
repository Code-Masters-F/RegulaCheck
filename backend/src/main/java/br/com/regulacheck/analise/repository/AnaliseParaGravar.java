package br.com.regulacheck.analise.repository;

import br.com.regulacheck.validacao.model.AvaliacaoDeRegras;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Avaliacao pronta para ser gravada como analise, com a entrada que ela considerou. A revisao e o instante da
 * conferencia sao conferidos de novo, sob o bloqueio do pedido, antes de gravar (research D2).
 *
 * @param revisao revisao do pedido avaliada (a da leitura)
 * @param protocoloId identificador interno da versao do protocolo aplicada
 * @param extracaoId leitura usada
 * @param conferenciaEm instante da ultima decisao de conferencia ou, sem decisao, o da leitura
 * @param dataReferencia data usada pelas regras de data (R05/R06)
 * @param avaliacao resultados por regra, pendencias com evidencias e resultado geral
 */
public record AnaliseParaGravar(int revisao, long protocoloId, long extracaoId, Instant conferenciaEm,
        LocalDate dataReferencia, AvaliacaoDeRegras avaliacao) {

    /** Exige a entrada completa. */
    public AnaliseParaGravar {
        Objects.requireNonNull(conferenciaEm, "conferenciaEm");
        Objects.requireNonNull(dataReferencia, "dataReferencia");
        Objects.requireNonNull(avaliacao, "avaliacao");
    }
}
