package br.com.regulacheck.iot.entity;

import java.time.Instant;

/**
 * Sessao de captura de um pedido ({@code RC_SESSAO_CAPTURA}): o periodo em que a estacao de captura acompanha a
 * paciente ao fotografar os documentos. Aberta pela propria paciente (RC-21); no maximo uma aberta por pedido.
 *
 * @param id identificador da sessao, informado ao simulador
 * @param solicitacaoId pedido ao qual a sessao pertence (a dona e a dona do pedido)
 * @param iniciadaEm instante de abertura, do relogio do servidor (UTC)
 * @param encerradaEm instante de encerramento; {@code null} enquanto a sessao esta aberta. Nenhuma rota encerra
 *     sessoes nesta entrega (so a carga ou um script)
 */
public record SessaoCaptura(long id, long solicitacaoId, Instant iniciadaEm, Instant encerradaEm) {

    /**
     * @return {@code true} se a sessao ainda recebe leituras
     */
    public boolean aberta() {
        return encerradaEm == null;
    }
}
