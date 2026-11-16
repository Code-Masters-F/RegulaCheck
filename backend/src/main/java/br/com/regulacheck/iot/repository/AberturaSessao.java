package br.com.regulacheck.iot.repository;

import br.com.regulacheck.iot.entity.SessaoCaptura;

/**
 * Desfecho de {@link CapturaRepository#abrirSessao}.
 *
 * @param situacao o que aconteceu
 * @param sessao a sessao criada ou a ja aberta; {@code null} em {@link Situacao#NAO_ENCONTRADO} e
 *     {@link Situacao#SUBMETIDO}
 */
public record AberturaSessao(Situacao situacao, SessaoCaptura sessao) {

    /** Desfechos possiveis da abertura. */
    public enum Situacao {
        /** Pedido inexistente ou de outra paciente (mesma resposta para os dois). */
        NAO_ENCONTRADO,
        /** Pedido ja submetido em simulacao: so consulta. */
        SUBMETIDO,
        /** O pedido ja tinha sessao aberta; ela e devolvida e nada e gravado. */
        EXISTENTE,
        /** Sessao nova gravada. */
        CRIADA
    }

    /** @return desfecho de pedido inexistente ou alheio */
    public static AberturaSessao naoEncontrado() {
        return new AberturaSessao(Situacao.NAO_ENCONTRADO, null);
    }

    /** @return desfecho de pedido submetido */
    public static AberturaSessao submetido() {
        return new AberturaSessao(Situacao.SUBMETIDO, null);
    }

    /**
     * @param sessao a sessao ja aberta
     * @return desfecho de reabertura
     */
    public static AberturaSessao existente(SessaoCaptura sessao) {
        return new AberturaSessao(Situacao.EXISTENTE, sessao);
    }

    /**
     * @param sessao a sessao gravada agora
     * @return desfecho de criacao
     */
    public static AberturaSessao criada(SessaoCaptura sessao) {
        return new AberturaSessao(Situacao.CRIADA, sessao);
    }
}
