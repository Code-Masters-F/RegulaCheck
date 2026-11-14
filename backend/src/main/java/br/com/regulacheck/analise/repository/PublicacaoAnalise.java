package br.com.regulacheck.analise.repository;

import br.com.regulacheck.analise.entity.Analise;

/**
 * Desfecho de {@link AnaliseRepository#publicar}.
 *
 * @param situacao o que aconteceu
 * @param analise analise gravada ({@code PUBLICADA}); {@code null} nos demais
 */
public record PublicacaoAnalise(Situacao situacao, Analise analise) {

    /** Situacoes possiveis. */
    public enum Situacao {
        /** Analise gravada, vigente, e pedido no resultado geral. */
        PUBLICADA,
        /**
         * A revisao ou a conferencia mudou durante a analise: nada foi gravado e o pedido voltou ao estado de antes
         * (resultado da analise concluida mais recente ou {@code RASCUNHO}).
         */
        ENTRADA_MUDOU,
        /**
         * Esta analise ja nao e a que esta em andamento (passou do limite e outra assumiu, ou ja foi encerrada):
         * nada foi gravado nem alterado.
         */
        SUBSTITUIDA,
        /** Pedido inexistente ou de outra paciente. */
        NAO_ENCONTRADO
    }

    /** @return desfecho de analise gravada */
    public static PublicacaoAnalise publicada(Analise analise) {
        return new PublicacaoAnalise(Situacao.PUBLICADA, analise);
    }

    /** @return desfecho de entrada que mudou */
    public static PublicacaoAnalise entradaMudou() {
        return new PublicacaoAnalise(Situacao.ENTRADA_MUDOU, null);
    }

    /** @return desfecho de analise substituida */
    public static PublicacaoAnalise substituida() {
        return new PublicacaoAnalise(Situacao.SUBSTITUIDA, null);
    }

    /** @return desfecho de pedido nao encontrado */
    public static PublicacaoAnalise naoEncontrado() {
        return new PublicacaoAnalise(Situacao.NAO_ENCONTRADO, null);
    }
}
