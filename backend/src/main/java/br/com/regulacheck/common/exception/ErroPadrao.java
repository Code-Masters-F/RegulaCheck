package br.com.regulacheck.common.exception;

import java.util.List;

/**
 * Corpo padrao de toda resposta de erro (ver {@code docs/api/erros.md}).
 *
 * <p>Nunca carrega stack trace, SQL, caminhos, chaves ou conteudo de documentos.
 *
 * @param codigo identificador estavel do erro (ex.: {@code NAO_ENCONTRADO})
 * @param mensagem texto compreensivel para a pessoa
 * @param detalhes erros por campo de entrada; {@code null} quando nao se aplica
 * @param idRastreamento identificador para localizar o registro tecnico no log; opcional
 */
public record ErroPadrao(String codigo, String mensagem, List<Detalhe> detalhes, String idRastreamento) {

    /**
     * Erro ligado a um campo da entrada.
     *
     * @param campo nome do campo
     * @param mensagem o que esta errado
     */
    public record Detalhe(String campo, String mensagem) {
    }
}
