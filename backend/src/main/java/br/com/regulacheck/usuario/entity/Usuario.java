package br.com.regulacheck.usuario.entity;

/**
 * Conta de usuario persistida em {@code RC_USUARIO}. Modelo interno: nunca e serializada como
 * resposta publica (usar DTOs).
 *
 * @param id identificador interno
 * @param email e-mail unico, guardado em minusculas
 * @param nome nome de exibicao
 * @param senhaHash hash BCrypt da senha; nunca a senha
 * @param perfil perfil da conta
 * @param ativo {@code false} impede o login e invalida sessoes existentes
 */
public record Usuario(long id, String email, String nome, String senhaHash, Perfil perfil, boolean ativo) {

    /** Oculta e-mail e hash para que nao vazem em logs. */
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", perfil=" + perfil + ", ativo=" + ativo + "]";
    }
}
