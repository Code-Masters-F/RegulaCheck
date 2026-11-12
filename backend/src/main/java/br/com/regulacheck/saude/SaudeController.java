package br.com.regulacheck.saude;

import br.com.regulacheck.common.Json;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller de {@code GET /health}: expoe apenas o estado tecnico, sem autenticacao e sem
 * detalhes internos (versoes, caminhos, componentes).
 */
public class SaudeController extends ServletJson {

    private static final long serialVersionUID = 1L;

    private final transient SaudeService servico;

    /**
     * @param servico caso de uso que fornece o estado
     */
    public SaudeController(SaudeService servico) {
        super("GET");
        this.servico = servico;
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_OK);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write(Json.escrever(servico.consultar()));
    }
}
