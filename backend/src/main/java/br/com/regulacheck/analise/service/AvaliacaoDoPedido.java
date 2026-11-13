package br.com.regulacheck.analise.service;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.validacao.model.AvaliacaoDeRegras;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Avaliacao dos criterios de um pedido, pronta para quem vai grava-la e publica-la (RC-13) ou explica-la (RC-14).
 * Modelo interno, nao gravado e nao serializado como resposta publica nesta historia.
 *
 * <p>Os campos de identificacao servem para saber se o resultado ficou velho: so pode ser publicado como vigente se
 * {@link #revisao()} ainda for a revisao do pedido e {@link #conferenciaAtualizadaEm()} ainda for o instante da
 * ultima decisao de conferencia (compensacao registrada pela RC-11).
 *
 * @param solicitacaoId pedido avaliado
 * @param revisao revisao do pedido avaliada (a mesma da leitura)
 * @param protocolo versao do protocolo aplicada
 * @param extracaoId leitura usada
 * @param conferenciaAtualizadaEm ultima decisao de conferencia ou, sem decisao, o instante da leitura
 * @param dataReferencia data (Salvador) do envio mais recente entre os documentos avaliados
 * @param avaliacao resultados por regra, pendencias e resultado geral
 */
public record AvaliacaoDoPedido(long solicitacaoId, int revisao, Protocolo protocolo, long extracaoId,
        Instant conferenciaAtualizadaEm, LocalDate dataReferencia, AvaliacaoDeRegras avaliacao) {

    /**
     * A avaliacao se baseia na leitura simulada (o unico leitor do MVP; {@code RC_EXTRACAO.SIMULADA} e sempre 1) e
     * no protocolo academico ficticio. Quem a exibir deve identifica-la como simulada.
     *
     * @return sempre {@code true}
     */
    public boolean leituraSimulada() {
        return true;
    }
}
