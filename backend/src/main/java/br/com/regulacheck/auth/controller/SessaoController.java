package br.com.regulacheck.auth.controller;

import br.com.regulacheck.auth.dto.SessaoResponse;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code GET /auth/sessao}: mostra a identidade da sessao atual. A rota fica atras do
 * {@link AutenticacaoFiltro}, que ja validou o token; aqui so se le a identidade do atributo da
 * requisicao (nunca do corpo).
 */
public class SessaoController extends ServletJson {

    private static final long serialVersionUID = 1L;

    /** Cria o controller; nao precisa de servico porque a validacao e do filtro. */
    public SessaoController() {
        super("GET");
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        responderJson(resposta, HttpServletResponse.SC_OK, SessaoResponse.de(AutenticacaoFiltro.sessaoDe(requisicao)));
    }
}
