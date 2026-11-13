package br.com.regulacheck.solicitacao.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import java.util.List;

/**
 * Desfecho da exclusao de um rascunho (ver {@link SolicitacaoRepository#excluirRascunho}).
 *
 * @param situacao o que aconteceu
 * @param arquivos nomes internos dos arquivos de <em>todos</em> os documentos do pedido (disponiveis e removidos),
 *     que o Service apaga <strong>depois</strong> do commit; vazia se nada foi excluido
 */
public record ResultadoExclusao(Situacao situacao, List<IdInterno> arquivos) {

    /** Situacoes possiveis da exclusao. */
    public enum Situacao {
        /** Pedido inexistente, de outra paciente ou ja excluido; nada mudou. */
        NAO_ENCONTRADO,
        /** O pedido esta em submissao simulada e so pode ser consultado; nada foi apagado. */
        SUBMETIDO,
        /** O pedido nao esta em rascunho (e nao esta submetido); nada foi apagado. */
        FORA_DE_RASCUNHO,
        /** Algo ainda ligado ao pedido impediu a exclusao (restricao de chave estrangeira); nada foi apagado. */
        COM_DEPENDENCIAS,
        /** Documentos, revisoes e pedido apagados na mesma transacao. */
        EXCLUIDO
    }

    /** @return resultado sem alteracao: pedido nao encontrado */
    public static ResultadoExclusao naoEncontrado() {
        return new ResultadoExclusao(Situacao.NAO_ENCONTRADO, List.of());
    }

    /** @return resultado sem alteracao: pedido submetido em simulacao */
    public static ResultadoExclusao submetido() {
        return new ResultadoExclusao(Situacao.SUBMETIDO, List.of());
    }

    /** @return resultado sem alteracao: pedido fora de rascunho */
    public static ResultadoExclusao foraDeRascunho() {
        return new ResultadoExclusao(Situacao.FORA_DE_RASCUNHO, List.of());
    }

    /** @return resultado sem alteracao: dependencias impedem a exclusao */
    public static ResultadoExclusao comDependencias() {
        return new ResultadoExclusao(Situacao.COM_DEPENDENCIAS, List.of());
    }

    /**
     * @param arquivos arquivos dos documentos do pedido, a apagar depois do commit
     * @return exclusao aplicada
     */
    public static ResultadoExclusao excluido(List<IdInterno> arquivos) {
        return new ResultadoExclusao(Situacao.EXCLUIDO, List.copyOf(arquivos));
    }
}
