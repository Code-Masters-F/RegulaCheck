package br.com.regulacheck.auth.controller;

import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code POST /auth/logout}: encerra a sessao do token enviado. A rota fica atras do
 * {@link AutenticacaoFiltro}. Responde 204; o mesmo token passa a ser recusado imediatamente e as
 * outras sessoes da conta continuam validas.
 */
public class LogoutController extends ServletJson {

    private static final long serialVersionUID = 1L;

    private final transient AutenticacaoService autenticacao;

    /** @param autenticacao casos de uso de autenticacao */
    public LogoutController(AutenticacaoService autenticacao) {
        super("POST");
        this.autenticacao = autenticacao;
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) {
        autenticacao.encerrar(AutenticacaoFiltro.sessaoDe(requisicao));
        resposta.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
