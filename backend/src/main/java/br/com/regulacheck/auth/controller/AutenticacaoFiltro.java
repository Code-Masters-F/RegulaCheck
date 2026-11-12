package br.com.regulacheck.auth.controller;

import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.RespostaErro;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

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
 * perfil e isolamento por paciente sao da RC-05 e ficam nos servicos.
 */
public class AutenticacaoFiltro implements Filter {

    /** Nome do atributo da requisicao que carrega a {@link SessaoAutenticada}. */
    public static final String ATRIBUTO = "br.com.regulacheck.sessaoAutenticada";

    private static final String ESQUEMA = "bearer ";

    private final AutenticacaoService autenticacao;

    /** @param autenticacao casos de uso de autenticacao */
    public AutenticacaoFiltro(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
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
            SessaoAutenticada sessao = autenticacao.autenticar(token(http.getHeader("Authorization")));
            http.setAttribute(ATRIBUTO, sessao);
        } catch (RuntimeException falha) {
            // 401 (previsto), 503 (banco) e falhas inesperadas (500) saem no formato padrao; a cadeia nao segue.
            RespostaErro.tratar(respostaHttp, falha);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    /** Extrai o token do cabecalho, ou {@code null} se o esquema nao for Bearer. */
    private static String token(String cabecalho) {
        if (cabecalho == null || cabecalho.length() <= ESQUEMA.length()
                || !cabecalho.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())) {
            return null;
        }
        return cabecalho.substring(ESQUEMA.length()).trim();
    }
}
