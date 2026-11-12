package br.com.regulacheck.auth.dto;

import br.com.regulacheck.auth.service.AutenticacaoService.SessaoIniciada;
import java.time.temporal.ChronoUnit;

/**
 * Resposta de {@code POST /auth/login}. O token aparece somente aqui, uma vez.
 *
 * @param token credencial da sessao, para {@code Authorization: Bearer}
 * @param tipo sempre {@code Bearer}
 * @param expiraEm instante de expiracao em ISO-8601 UTC
 * @param usuario nome e perfil da conta
 */
public record LoginResponse(String token, String tipo, String expiraEm, UsuarioResumo usuario) {

    /**
     * @param iniciada sessao recem-aberta
     * @return resposta publica correspondente
     */
    public static LoginResponse de(SessaoIniciada iniciada) {
        return new LoginResponse(iniciada.token(), "Bearer",
                iniciada.expiraEm().truncatedTo(ChronoUnit.SECONDS).toString(),
                new UsuarioResumo(iniciada.nome(), iniciada.perfil().name()));
    }

    /** Oculta o token para que nao vaze em logs. */
    @Override
    public String toString() {
        return "LoginResponse[tipo=" + tipo + ", expiraEm=" + expiraEm + "]";
    }
}
