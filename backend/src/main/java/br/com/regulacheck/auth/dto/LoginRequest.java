package br.com.regulacheck.auth.dto;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.util.ArrayList;
import java.util.List;

/**
 * Corpo de {@code POST /auth/login}.
 *
 * @param email e-mail da conta
 * @param senha senha em texto; trafega so aqui e nunca e registrada
 */
public record LoginRequest(String email, String senha) {

    /** Tamanho maximo do e-mail, igual ao da coluna {@code RC_USUARIO.EMAIL}. */
    public static final int EMAIL_MAXIMO = 254;

    /**
     * Valida os campos obrigatorios e tamanhos.
     *
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} com um detalhe por campo problematico
     */
    public void validar() {
        List<ErroPadrao.Detalhe> problemas = new ArrayList<>();
        if (email == null || email.isBlank()) {
            problemas.add(new ErroPadrao.Detalhe("email", "Informe o e-mail."));
        } else if (email.length() > EMAIL_MAXIMO) {
            problemas.add(new ErroPadrao.Detalhe("email", "O e-mail deve ter no máximo " + EMAIL_MAXIMO + " caracteres."));
        }
        if (senha == null || senha.isEmpty()) {
            problemas.add(new ErroPadrao.Detalhe("senha", "Informe a senha."));
        }
        if (!problemas.isEmpty()) {
            throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "Verifique os campos informados.", problemas);
        }
    }

    /** Oculta a senha para que nao vaze em logs. */
    @Override
    public String toString() {
        return "LoginRequest[email=?, senha=?]";
    }
}
