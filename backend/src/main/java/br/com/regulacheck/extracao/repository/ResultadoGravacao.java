package br.com.regulacheck.extracao.repository;

/**
 * Desfecho de {@link ExtracaoRepository#gravar}.
 *
 * @param situacao o que aconteceu
 * @param extracao leitura gravada ({@code GRAVADA}) ou ja existente ({@code JA_EXISTIA}); {@code null} nos demais
 */
public record ResultadoGravacao(Situacao situacao, ExtracaoRegistrada extracao) {

    /** Situacoes possiveis. */
    public enum Situacao {
        /** Leitura nova gravada. */
        GRAVADA,
        /** Ja havia leitura para a versao capturada: devolvida, nada novo foi gravado. */
        JA_EXISTIA,
        /** Os documentos mudaram depois de capturada a versao: nada foi gravado. */
        REVISAO_MUDOU,
        /** Pedido em simulacao de submissao: so consulta. */
        SUBMETIDO,
        /** Pedido inexistente ou de outra paciente. */
        NAO_ENCONTRADO
    }

    /** @return desfecho de leitura gravada */
    public static ResultadoGravacao gravada(ExtracaoRegistrada extracao) {
        return new ResultadoGravacao(Situacao.GRAVADA, extracao);
    }

    /** @return desfecho de leitura ja existente */
    public static ResultadoGravacao jaExistia(ExtracaoRegistrada extracao) {
        return new ResultadoGravacao(Situacao.JA_EXISTIA, extracao);
    }

    /** @return desfecho de versao que mudou */
    public static ResultadoGravacao revisaoMudou() {
        return new ResultadoGravacao(Situacao.REVISAO_MUDOU, null);
    }

    /** @return desfecho de pedido submetido */
    public static ResultadoGravacao submetido() {
        return new ResultadoGravacao(Situacao.SUBMETIDO, null);
    }

    /** @return desfecho de pedido nao encontrado */
    public static ResultadoGravacao naoEncontrado() {
        return new ResultadoGravacao(Situacao.NAO_ENCONTRADO, null);
    }
}
