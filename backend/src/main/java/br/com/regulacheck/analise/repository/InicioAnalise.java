package br.com.regulacheck.analise.repository;

import br.com.regulacheck.analise.entity.Analise;
import java.time.Instant;

/**
 * Desfecho de {@link AnaliseRepository#iniciar}.
 *
 * @param situacao o que aconteceu
 * @param analise analise vigente devolvida ({@code VIGENTE}); {@code null} nos demais
 * @param instante inicio da analise em andamento ({@code EM_ANDAMENTO}) ou desta ({@code INICIADA});
 *     {@code null} nos demais
 * @param interrompeuAnterior {@code true} se, para iniciar, uma analise anterior que passou do limite foi
 *     encerrada como falha
 */
public record InicioAnalise(Situacao situacao, Analise analise, Instant instante, boolean interrompeuAnterior) {

    /** Situacoes possiveis. */
    public enum Situacao {
        /** Pedido inexistente ou de outra paciente. */
        NAO_ENCONTRADO,
        /** Pedido em simulacao de submissao: so consulta. */
        SUBMETIDO,
        /** Outra analise do pedido esta em andamento dentro do limite: nada foi iniciado. */
        EM_ANDAMENTO,
        /** Ja existe analise vigente da revisao atual: devolvida, nada foi gravado. */
        VIGENTE,
        /** Nao ha leitura da versao atual dos documentos: nada foi iniciado. */
        SEM_LEITURA_ATUAL,
        /** O pedido foi para {@code PROCESSANDO} com o instante de inicio desta analise. */
        INICIADA
    }

    /** @return desfecho de pedido nao encontrado */
    public static InicioAnalise naoEncontrado() {
        return new InicioAnalise(Situacao.NAO_ENCONTRADO, null, null, false);
    }

    /** @return desfecho de pedido submetido */
    public static InicioAnalise submetido() {
        return new InicioAnalise(Situacao.SUBMETIDO, null, null, false);
    }

    /**
     * @param desde inicio da analise em andamento
     * @return desfecho de analise ja em andamento
     */
    public static InicioAnalise emAndamento(Instant desde) {
        return new InicioAnalise(Situacao.EM_ANDAMENTO, null, desde, false);
    }

    /**
     * @param analise analise vigente da revisao atual
     * @param interrompeuAnterior se uma analise vencida foi encerrada nesta chamada
     * @return desfecho de analise vigente devolvida
     */
    public static InicioAnalise vigente(Analise analise, boolean interrompeuAnterior) {
        return new InicioAnalise(Situacao.VIGENTE, analise, null, interrompeuAnterior);
    }

    /**
     * @param interrompeuAnterior se uma analise vencida foi encerrada nesta chamada
     * @return desfecho de pedido sem leitura atual
     */
    public static InicioAnalise semLeituraAtual(boolean interrompeuAnterior) {
        return new InicioAnalise(Situacao.SEM_LEITURA_ATUAL, null, null, interrompeuAnterior);
    }

    /**
     * @param iniciadaEm instante de inicio gravado (identifica esta analise)
     * @param interrompeuAnterior se uma analise vencida foi encerrada nesta chamada
     * @return desfecho de analise iniciada
     */
    public static InicioAnalise iniciada(Instant iniciadaEm, boolean interrompeuAnterior) {
        return new InicioAnalise(Situacao.INICIADA, null, iniciadaEm, interrompeuAnterior);
    }
}
