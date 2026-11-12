package br.com.regulacheck.common.http;

import br.com.regulacheck.common.exception.RespostaErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Atende qualquer caminho sem controller proprio e responde 404 no formato padrao, sem expor a
 * pagina de erro do servidor.
 */
public class RotaNaoEncontradaController extends ServletJson {

    private static final long serialVersionUID = 1L;

    /** Aceita os metodos usuais para que a resposta seja sempre 404 e nao 405. */
    public RotaNaoEncontradaController() {
        super("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    }

    @Override
    protected void service(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        RespostaErro.escrever(resposta, HttpServletResponse.SC_NOT_FOUND, "NAO_ENCONTRADO",
                "Recurso nao encontrado.");
    }
}
