package br.com.regulacheck.submissao.repository;

import br.com.regulacheck.submissao.entity.Submissao;

/**
 * Desfecho de {@link SubmissaoRepository#submeter}, decidido sob o bloqueio da linha do pedido. O Service traduz cada
 * situacao em resposta.
 *
 * @param situacao o que aconteceu
 * @param submissao a submissao registrada agora ({@link Situacao#SUBMETIDA}) ou a ja existente
 *     ({@link Situacao#JA_SUBMETIDA}); {@code null} nas demais
 */
public record ResultadoSubmissao(Situacao situacao, Submissao submissao) {

    /** Situacoes possiveis da conclusao, na ordem em que sao conferidas. */
    public enum Situacao {
        /** Pedido inexistente ou de outra paciente; nada foi gravado. */
        NAO_ENCONTRADO,
        /** O pedido ja tinha submissao: devolve a existente, sem gravar outra. */
        JA_SUBMETIDA,
        /** Pedido em submissao simulada sem registro (dado montado fora do fluxo): so consulta; nada foi gravado. */
        SOMENTE_CONSULTA,
        /** Analise em andamento ({@code PROCESSANDO}); nada foi gravado. */
        EM_ANDAMENTO,
        /**
         * Nao ha analise vigente da revisao atual, ou ha uma sem pendencias mas o estado do pedido nao e
         * {@code SEM_PENDENCIAS_IDENTIFICADAS} (dado incoerente); nada foi gravado.
         */
        SEM_ANALISE_VIGENTE,
        /** A analise vigente aponta pendencia ou criterio nao verificavel; nada foi gravado. */
        COM_PENDENCIAS,
        /** Submissao gravada e pedido em {@code SUBMETIDA_SIMULACAO}, na mesma transacao. */
        SUBMETIDA
    }

    /**
     * @param situacao desfecho sem submissao
     * @return resultado sem submissao
     */
    public static ResultadoSubmissao sem(Situacao situacao) {
        return new ResultadoSubmissao(situacao, null);
    }

    /**
     * @param existente submissao ja registrada para o pedido
     * @return resultado de repeticao
     */
    public static ResultadoSubmissao jaSubmetida(Submissao existente) {
        return new ResultadoSubmissao(Situacao.JA_SUBMETIDA, existente);
    }

    /**
     * @param nova submissao registrada agora
     * @return resultado de sucesso
     */
    public static ResultadoSubmissao submetida(Submissao nova) {
        return new ResultadoSubmissao(Situacao.SUBMETIDA, nova);
    }
}
