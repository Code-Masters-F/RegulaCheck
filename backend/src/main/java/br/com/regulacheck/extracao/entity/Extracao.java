package br.com.regulacheck.extracao.entity;

import java.time.Instant;
import java.util.List;

/**
 * Leitura (simulada) dos documentos de um pedido em uma versao do conjunto documental. Modelo interno: nunca e
 * serializado como resposta publica (usar {@code ExtracaoResponse}).
 *
 * @param id identificador da leitura
 * @param solicitacaoId pedido lido
 * @param revisao versao do conjunto documental (revisao do pedido) em que a leitura foi feita
 * @param criadaEm instante da leitura, em UTC
 * @param documentos leitura de cada documento disponivel naquela versao
 */
public record Extracao(long id, long solicitacaoId, int revisao, Instant criadaEm,
        List<ExtracaoDocumento> documentos) {

    /** Copia defensiva. */
    public Extracao {
        documentos = documentos == null ? List.of() : List.copyOf(documentos);
    }
}
