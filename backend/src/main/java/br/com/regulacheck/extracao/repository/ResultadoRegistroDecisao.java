package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.integracao.extracao.CodigoCampo;

/**
 * Desfecho de {@link ConferenciaRepository#registrar}.
 *
 * @param situacao o que aconteceu
 * @param documentoId documento da decisao recusada ({@code DECISAO_NAO_APLICAVEL}); {@code null} nos demais
 * @param codigo campo da decisao recusada ({@code DECISAO_NAO_APLICAVEL}); {@code null} nos demais
 */
public record ResultadoRegistroDecisao(Situacao situacao, Long documentoId, CodigoCampo codigo) {

    /** Situacoes possiveis. */
    public enum Situacao {
        /** Todas as decisoes foram registradas. */
        REGISTRADA,
        /** Pedido ou leitura inexistente, de outra paciente ou de outro pedido. */
        NAO_ENCONTRADO,
        /** Pedido em simulacao de submissao: so consulta. */
        SUBMETIDO,
        /** A leitura nao e a da versao atual dos documentos. */
        DESATUALIZADA,
        /** Alguma decisao aponta para documento que nao faz parte da leitura. */
        DOCUMENTO_FORA_DA_LEITURA,
        /** Alguma decisao nao cabe a situacao do campo (ou o campo nao existe na leitura). */
        DECISAO_NAO_APLICAVEL
    }

    /** @return desfecho de lote registrado */
    public static ResultadoRegistroDecisao registrada() {
        return new ResultadoRegistroDecisao(Situacao.REGISTRADA, null, null);
    }

    /** @return desfecho de pedido ou leitura nao encontrados */
    public static ResultadoRegistroDecisao naoEncontrado() {
        return new ResultadoRegistroDecisao(Situacao.NAO_ENCONTRADO, null, null);
    }

    /** @return desfecho de pedido submetido */
    public static ResultadoRegistroDecisao submetido() {
        return new ResultadoRegistroDecisao(Situacao.SUBMETIDO, null, null);
    }

    /** @return desfecho de leitura desatualizada */
    public static ResultadoRegistroDecisao desatualizada() {
        return new ResultadoRegistroDecisao(Situacao.DESATUALIZADA, null, null);
    }

    /** @return desfecho de decisao para documento fora da leitura */
    public static ResultadoRegistroDecisao documentoForaDaLeitura() {
        return new ResultadoRegistroDecisao(Situacao.DOCUMENTO_FORA_DA_LEITURA, null, null);
    }

    /**
     * @param documentoId documento da decisao recusada
     * @param codigo campo da decisao recusada
     * @return desfecho de decisao que nao cabe ao campo
     */
    public static ResultadoRegistroDecisao naoAplicavel(long documentoId, CodigoCampo codigo) {
        return new ResultadoRegistroDecisao(Situacao.DECISAO_NAO_APLICAVEL, documentoId, codigo);
    }
}
