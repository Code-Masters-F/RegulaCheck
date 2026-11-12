package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.common.exception.RespostaErro;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Barreira da rota de telemetria, a unica que aceita a credencial do simulador de sensores.
 *
 * <p>Respostas:
 * <ul>
 *   <li>chave do simulador valida: a requisicao segue (a rota ainda confere a sessao de captura);</li>
 *   <li>token de conta valido, sem chave valida: 403, porque pessoas nao enviam telemetria;</li>
 *   <li>nenhuma credencial valida (ou {@code IOT_API_KEY} nao configurada): 401.</li>
 * </ul>
 *
 * <p>A chave e comparada em tempo constante ({@link ChaveSimulador}) e nunca e registrada.
 */
public class SimuladorFiltro implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(SimuladorFiltro.class);

    private final ChaveSimulador chave;
    private final AutenticacaoService autenticacao;

    /**
     * @param chave chave configurada do simulador (pode estar ausente)
     * @param autenticacao usado so para reconhecer um token de conta e responder 403 em vez de 401
     */
    public SimuladorFiltro(ChaveSimulador chave, AutenticacaoService autenticacao) {
        this.chave = chave;
        this.autenticacao = autenticacao;
    }

    @Override
    public void doFilter(ServletRequest requisicao, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) requisicao;
        HttpServletResponse respostaHttp = (HttpServletResponse) resposta;
        try {
            if (chave.confere(http.getHeader(ChaveSimulador.CABECALHO))) {
                cadeia.doFilter(requisicao, resposta);
                return;
            }
            if (contaAutenticada(http)) {
                LOG.warn("Acesso negado. credencial=conta metodo={} caminho={} codigo=ACESSO_NEGADO",
                        http.getMethod(), http.getRequestURI());
                RespostaErro.tratar(respostaHttp, Erros.acessoNegado());
            } else {
                LOG.warn("Acesso nao autenticado. credencial=simulador metodo={} caminho={} codigo=NAO_AUTENTICADO",
                        http.getMethod(), http.getRequestURI());
                RespostaErro.tratar(respostaHttp, AutenticacaoService.naoAutenticado());
            }
        } catch (RuntimeException falha) {
            RespostaErro.tratar(respostaHttp, falha);
        }
    }

    private boolean contaAutenticada(HttpServletRequest http) {
        try {
            autenticacao.autenticar(AutenticacaoFiltro.tokenDe(http.getHeader("Authorization")));
            return true;
        } catch (ExcecaoNegocio naoAutenticado) {
            if (naoAutenticado.status() == HttpServletResponse.SC_UNAUTHORIZED) {
                return false;
            }
            throw naoAutenticado;
        }
    }
}
