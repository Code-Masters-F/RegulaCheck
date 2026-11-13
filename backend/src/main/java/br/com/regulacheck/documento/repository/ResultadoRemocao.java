package br.com.regulacheck.documento.repository;

import br.com.regulacheck.documento.entity.Documento;

/**
 * Desfecho da remocao de um documento, decidido pelo repositorio sob o bloqueio da linha do pedido (ver
 * {@link DocumentoRepository#remover}). O Service traduz cada situacao em resposta de negocio.
 *
 * @param situacao o que aconteceu
 * @param documento documento removido (para o Service apagar o arquivo depois do commit); {@code null} se nada foi
 *     removido
 * @param versaoConjuntoDocumental revisao do pedido apos a remocao; {@code 0} se nada foi removido
 * @param documentosDisponiveis documentos disponiveis apos a remocao; {@code 0} se nada foi removido
 */
public record ResultadoRemocao(Situacao situacao, Documento documento, int versaoConjuntoDocumental,
        int documentosDisponiveis) {

    /** Situacoes possiveis da remocao. */
    public enum Situacao {
        /** Pedido ou documento inexistente, de outra paciente, de outro pedido ou ja removido; nada mudou. */
        NAO_ENCONTRADO,
        /** O pedido esta em submissao simulada e so pode ser consultado; nada mudou. */
        SUBMETIDO,
        /** Documento removido, revisao do pedido incrementada e revisao registrada. */
        REMOVIDO
    }

    /** @return resultado sem alteracao: pedido ou documento nao encontrado */
    public static ResultadoRemocao naoEncontrado() {
        return new ResultadoRemocao(Situacao.NAO_ENCONTRADO, null, 0, 0);
    }

    /** @return resultado sem alteracao: pedido submetido em simulacao */
    public static ResultadoRemocao submetido() {
        return new ResultadoRemocao(Situacao.SUBMETIDO, null, 0, 0);
    }

    /**
     * @param removido documento que deixou de estar disponivel
     * @param versao revisao do pedido apos a remocao
     * @param disponiveis documentos disponiveis apos a remocao
     * @return resultado de remocao aplicada
     */
    public static ResultadoRemocao removido(Documento removido, int versao, int disponiveis) {
        return new ResultadoRemocao(Situacao.REMOVIDO, removido, versao, disponiveis);
    }
}
