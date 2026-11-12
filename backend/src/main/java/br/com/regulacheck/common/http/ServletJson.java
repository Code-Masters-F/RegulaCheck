package br.com.regulacheck.common.http;

import br.com.regulacheck.common.exception.RespostaErro;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Base dos controllers HTTP: garante que erros saiam sempre no formato padrao em JSON.
 *
 * <p>Metodos HTTP fora dos permitidos viram 405 {@code METODO_NAO_PERMITIDO}, e qualquer falha
 * durante o atendimento e traduzida por {@link RespostaErro#tratar}. Subclasses nao devem guardar
 * dados de uma requisicao em campos (os servlets sao compartilhados entre requisicoes).
 */
public abstract class ServletJson extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Set<String> metodosPermitidos;

    /**
     * @param metodosPermitidos metodos HTTP aceitos por este controller (ex.: {@code "GET"})
     */
    protected ServletJson(String... metodosPermitidos) {
        this.metodosPermitidos = Set.of(metodosPermitidos);
    }

    @Override
    protected void service(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        try {
            if (!metodosPermitidos.contains(requisicao.getMethod())) {
                resposta.setHeader("Allow", String.join(", ", metodosPermitidos));
                RespostaErro.escrever(resposta, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "METODO_NAO_PERMITIDO", "Metodo HTTP nao permitido para este recurso.");
                return;
            }
            super.service(requisicao, resposta);
        } catch (RuntimeException falha) {
            if (!resposta.isCommitted()) {
                resposta.reset();
            }
            RespostaErro.tratar(resposta, falha);
        }
    }
}
