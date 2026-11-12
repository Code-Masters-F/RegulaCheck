package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.common.exception.ExcecaoNegocio;

/**
 * Fabricas dos dois erros de autorizacao, com mensagens fixas.
 *
 * <p>A mensagem nunca varia conforme o motivo (perfil exigido, dona do recurso, existencia), para
 * que a resposta nao revele nada alem do codigo. O 404 e identico ao da rota coringa
 * ({@code RotaNaoEncontradaController}): "recurso de outra paciente" e "recurso inexistente" sao
 * indistinguiveis por quem chama.
 */
public final class Erros {

    private Erros() {
    }

    /** @return 403 {@code ACESSO_NEGADO}: identidade valida, mas sem permissao nesta rota */
    public static ExcecaoNegocio acessoNegado() {
        return new ExcecaoNegocio(403, "ACESSO_NEGADO", "Voce nao tem permissao para esta operacao.");
    }

    /** @return 404 {@code NAO_ENCONTRADO}: recurso inexistente, de outra paciente ou fora de alcance */
    public static ExcecaoNegocio naoEncontrado() {
        return new ExcecaoNegocio(404, "NAO_ENCONTRADO", "Recurso nao encontrado.");
    }
}
