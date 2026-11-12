package br.com.regulacheck.auth.controller;

import br.com.regulacheck.auth.dto.LoginRequest;
import br.com.regulacheck.auth.dto.LoginResponse;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code POST /auth/login}: autentica a conta e abre uma sessao. Rota publica. Apenas traduz HTTP:
 * valida o contrato do corpo e delega ao {@link AutenticacaoService}. Erros saem no formato padrao
 * (400, 401 {@code CREDENCIAIS_INVALIDAS}, 415, 503) pela classe base.
 */
public class LoginController extends ServletJson {

    private static final long serialVersionUID = 1L;

    private final transient AutenticacaoService autenticacao;

    /** @param autenticacao casos de uso de autenticacao */
    public LoginController(AutenticacaoService autenticacao) {
        super("POST");
        this.autenticacao = autenticacao;
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        LoginRequest corpo = CorpoJson.ler(requisicao.getInputStream(), requisicao.getContentType(),
                LoginRequest.class);
        corpo.validar();

        LoginResponse resultado = LoginResponse.de(autenticacao.login(corpo.email(), corpo.senha()));

        // A resposta contem o token: nao deve ser guardada por navegadores nem intermediarios.
        resposta.setHeader("Cache-Control", "no-store");
        responderJson(resposta, HttpServletResponse.SC_OK, resultado);
    }
}
