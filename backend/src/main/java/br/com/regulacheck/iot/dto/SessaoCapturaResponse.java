package br.com.regulacheck.iot.dto;

import br.com.regulacheck.iot.entity.SessaoCaptura;
import java.time.temporal.ChronoUnit;

/**
 * Resposta de {@code POST /solicitacoes/{id}/sessoes-captura} (RC-21). Identificadores como texto, como no resto
 * da API.
 *
 * @param id sessao de captura, a informar ao simulador
 * @param solicitacaoId pedido
 * @param iniciadaEm instante de abertura (ISO-8601, UTC)
 * @param aberta sempre {@code true} nesta rota
 * @param criada {@code true} se esta chamada criou a sessao; {@code false} se devolveu a ja aberta
 * @param simulada sempre {@code true}: a estacao de captura e simulada
 */
public record SessaoCapturaResponse(String id, String solicitacaoId, String iniciadaEm, boolean aberta,
        boolean criada, boolean simulada) {

    /**
     * @param sessao sessao aberta
     * @param criada se foi criada agora
     * @return a resposta
     */
    public static SessaoCapturaResponse de(SessaoCaptura sessao, boolean criada) {
        return new SessaoCapturaResponse(Long.toString(sessao.id()), Long.toString(sessao.solicitacaoId()),
                sessao.iniciadaEm().truncatedTo(ChronoUnit.MILLIS).toString(), sessao.aberta(), criada, true);
    }
}
