package br.com.regulacheck.auth.controller;

import br.com.regulacheck.auth.autorizacao.ChaveSimulador;
import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.SessaoAutenticada;
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
 * Ponto unico de autenticacao das rotas protegidas.
 *
 * <p>Le o cabecalho {@code Authorization: Bearer <token>}, valida a sessao no
 * {@link AutenticacaoService} e, se valida, guarda a identidade como atributo da requisicao
 * ({@link #ATRIBUTO}) para o controller ler com {@link #sessaoDe}. Sem sessao valida responde 401
 * {@code NAO_AUTENTICADO} no formato padrao, sempre com a mesma mensagem.
 *
 * <p>Como as proximas historias o usam: registrar este filtro no caminho de cada rota protegida em
 * {@code Composicao} (ex.: {@code /solicitacoes/*}). O filtro so identifica quem chamou; regras por
 * perfil sao do {@code AutorizacaoFiltro} (registrado por {@code RotasPrivadas}) e a dona do recurso
 * e verificada nos servicos com {@code Propriedade}.
 *
 * <p>Credencial do simulador: se a rota e de conta e quem chama apresenta a chave do simulador valida,
 * a resposta e 403 (identidade reconhecida, sem permissao aqui) e nao 401.
 */
public class AutenticacaoFiltro implements Filter {

    /** Nome do atributo da requisicao que carrega a {@link SessaoAutenticada}. */
    public static final String ATRIBUTO = "br.com.regulacheck.sessaoAutenticada";

    private static final Logger LOG = LoggerFactory.getLogger(AutenticacaoFiltro.class);
    private static final String ESQUEMA = "bearer ";

    private final AutenticacaoService autenticacao;
    private final ChaveSimulador chaveSimulador;

    /**
     * @param autenticacao casos de uso de autenticacao
     * @param chaveSimulador chave do simulador, usada so para recusar com 403 quem a apresenta aqui
     */
    public AutenticacaoFiltro(AutenticacaoService autenticacao, ChaveSimulador chaveSimulador) {
        this.autenticacao = autenticacao;
        this.chaveSimulador = chaveSimulador;
    }

    /**
     * Le a identidade que o filtro gravou na requisicao.
     *
     * @param requisicao requisicao que passou pelo filtro
     * @return a sessao autenticada
     * @throws IllegalStateException se a rota nao foi registrada atras do filtro (erro de montagem)
     */
    public static SessaoAutenticada sessaoDe(HttpServletRequest requisicao) {
        Object valor = requisicao.getAttribute(ATRIBUTO);
        if (valor instanceof SessaoAutenticada sessao) {
            return sessao;
        }
        throw new IllegalStateException("Rota protegida sem AutenticacaoFiltro registrado.");
    }

    @Override
    public void doFilter(ServletRequest requisicao, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) requisicao;
        HttpServletResponse respostaHttp = (HttpServletResponse) resposta;
        try {
            SessaoAutenticada sessao = autenticacao.autenticar(tokenDe(http.getHeader("Authorization")));
            http.setAttribute(ATRIBUTO, sessao);
        } catch (ExcecaoNegocio naoAutenticado) {
            if (naoAutenticado.status() == HttpServletResponse.SC_UNAUTHORIZED
                    && chaveSimulador.confere(http.getHeader(ChaveSimulador.CABECALHO))) {
                LOG.warn("Acesso negado. credencial=simulador metodo={} caminho={} codigo=ACESSO_NEGADO",
                        http.getMethod(), http.getRequestURI());
                RespostaErro.tratar(respostaHttp, Erros.acessoNegado());
                return;
            }
            if (naoAutenticado.status() == HttpServletResponse.SC_UNAUTHORIZED) {
                LOG.warn("Acesso nao autenticado. metodo={} caminho={} codigo=NAO_AUTENTICADO",
                        http.getMethod(), http.getRequestURI());
            }
            RespostaErro.tratar(respostaHttp, naoAutenticado);
            return;
        } catch (RuntimeException falha) {
            // 401 (previsto), 503 (banco) e falhas inesperadas (500) saem no formato padrao; a cadeia nao segue.
            RespostaErro.tratar(respostaHttp, falha);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    /**
     * Extrai o token do cabecalho {@code Authorization}.
     *
     * @param cabecalho valor do cabecalho; pode ser {@code null}
     * @return o token, ou {@code null} se o esquema nao for Bearer
     */
    public static String tokenDe(String cabecalho) {
        if (cabecalho == null || cabecalho.length() <= ESQUEMA.length()
                || !cabecalho.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())) {
            return null;
        }
        return cabecalho.substring(ESQUEMA.length()).trim();
    }
}
