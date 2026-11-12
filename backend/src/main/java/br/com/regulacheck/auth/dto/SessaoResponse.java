package br.com.regulacheck.auth.dto;

import br.com.regulacheck.auth.service.SessaoAutenticada;
import java.time.temporal.ChronoUnit;

/**
 * Resposta de {@code GET /auth/sessao}.
 *
 * @param usuario nome e perfil da conta
 * @param expiraEm instante de expiracao em ISO-8601 UTC
 */
public record SessaoResponse(UsuarioResumo usuario, String expiraEm) {

    /**
     * @param sessao sessao validada pelo filtro de autenticacao
     * @return resposta publica correspondente
     */
    public static SessaoResponse de(SessaoAutenticada sessao) {
        return new SessaoResponse(new UsuarioResumo(sessao.nome(), sessao.perfil().name()),
                sessao.expiraEm().truncatedTo(ChronoUnit.SECONDS).toString());
    }
}
