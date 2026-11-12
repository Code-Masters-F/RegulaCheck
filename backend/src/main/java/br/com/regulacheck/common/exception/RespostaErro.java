package br.com.regulacheck.common.exception;

import br.com.regulacheck.common.Json;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escreve respostas de erro no formato padrao.
 *
 * <p>O detalhe tecnico de falhas inesperadas vai somente para o log, junto do
 * {@code idRastreamento} devolvido a quem chamou; a resposta nunca expoe stack trace, SQL ou
 * mensagens internas.
 */
public final class RespostaErro {

    private static final Logger LOG = LoggerFactory.getLogger(RespostaErro.class);

    private RespostaErro() {
    }

    /**
     * Escreve um erro previsto.
     *
     * @param resposta resposta HTTP
     * @param status status HTTP
     * @param codigo codigo estavel do erro
     * @param mensagem texto compreensivel para a pessoa
     * @throws IOException se a escrita na resposta falhar
     */
    public static void escrever(HttpServletResponse resposta, int status, String codigo, String mensagem)
            throws IOException {
        ErroPadrao erro = new ErroPadrao(codigo, mensagem, null, null);
        corpo(resposta, status, erro);
    }

    /**
     * Trata uma falha capturada: previstas viram o erro correspondente; as demais viram 500
     * generico com identificador de rastreamento, e o detalhe e registrado no log.
     *
     * @param resposta resposta HTTP
     * @param falha falha capturada
     * @throws IOException se a escrita na resposta falhar
     */
    public static void tratar(HttpServletResponse resposta, Throwable falha) throws IOException {
        if (falha instanceof ExcecaoNegocio negocio) {
            corpo(resposta, negocio.status(),
                    new ErroPadrao(negocio.codigo(), negocio.getMessage(), negocio.detalhes(), null));
            return;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        LOG.error("Falha inesperada. idRastreamento={}", id, falha);
        corpo(resposta, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, new ErroPadrao(
                "ERRO_INTERNO", "Ocorreu um erro inesperado. Tente novamente.", null, id));
    }

    private static void corpo(HttpServletResponse resposta, int status, ErroPadrao erro) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write(Json.escrever(erro));
    }
}
