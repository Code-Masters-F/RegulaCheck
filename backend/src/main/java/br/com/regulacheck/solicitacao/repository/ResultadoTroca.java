package br.com.regulacheck.solicitacao.repository;

import br.com.regulacheck.solicitacao.entity.Solicitacao;

/**
 * Resultado da troca de servico de um rascunho, decidido pelo repositorio sob o bloqueio da linha do pedido
 * (ver {@link SolicitacaoRepository#trocarProtocolo}). O Service traduz cada situacao em resposta de negocio.
 *
 * @param situacao o que aconteceu
 * @param solicitacao pedido no estado final quando {@code situacao} e {@link Situacao#SEM_MUDANCA} ou
 *     {@link Situacao#ALTERADA}; {@code null} nas demais
 */
public record ResultadoTroca(Situacao situacao, Solicitacao solicitacao) {

    /** Situacoes possiveis da troca. */
    public enum Situacao {
        /** Nao existe pedido com esse id para essa paciente (inexistente ou de outra paciente). */
        NAO_ENCONTRADA,
        /** O pedido ja saiu de rascunho; nada foi alterado. */
        FORA_DE_RASCUNHO,
        /** O servico pedido ja era o atual; revisao e data de atualizacao permanecem. */
        SEM_MUDANCA,
        /** Servico trocado: revisao incrementada e revisao nova registrada. */
        ALTERADA
    }

    /** @return resultado sem pedido: nao encontrado */
    public static ResultadoTroca naoEncontrada() {
        return new ResultadoTroca(Situacao.NAO_ENCONTRADA, null);
    }

    /** @return resultado sem pedido: fora de rascunho */
    public static ResultadoTroca foraDeRascunho() {
        return new ResultadoTroca(Situacao.FORA_DE_RASCUNHO, null);
    }

    /**
     * @param solicitacao pedido inalterado
     * @return resultado de troca sem mudanca
     */
    public static ResultadoTroca semMudanca(Solicitacao solicitacao) {
        return new ResultadoTroca(Situacao.SEM_MUDANCA, solicitacao);
    }

    /**
     * @param solicitacao pedido ja atualizado
     * @return resultado de troca aplicada
     */
    public static ResultadoTroca alterada(Solicitacao solicitacao) {
        return new ResultadoTroca(Situacao.ALTERADA, solicitacao);
    }
}
