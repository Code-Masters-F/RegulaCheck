package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segunda barreira de uma rota privada: confere se o <strong>perfil</strong> da conta e permitido
 * naquela rota.
 *
 * <p>Ordem de verificacao do projeto (ver {@code docs/api/autorizacao.md}):
 * <ol>
 *   <li>{@link AutenticacaoFiltro}: sem identidade valida, 401;</li>
 *   <li>este filtro: identidade valida, mas perfil fora da lista da rota, 403;</li>
 *   <li>o Service do recurso: dona do recurso, 404 ({@link Propriedade});</li>
 *   <li>validacao do pedido (400) e regra de negocio.</li>
 * </ol>
 *
 * <p>O perfil vem da {@link SessaoAutenticada}, que o {@code AutenticacaoService} le da conta a cada
 * requisicao; por isso uma mudanca de perfil no banco vale na chamada seguinte. Este filtro nao consulta
 * o banco. Nao e criado diretamente: use {@link RotasPrivadas}.
 */
public class AutorizacaoFiltro implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(AutorizacaoFiltro.class);

    private final Set<Perfil> permitidos;

    /**
     * @param permitidos perfis que podem usar a rota; nunca vazio
     * @throws IllegalArgumentException se a lista for vazia (rota sem perfil nao pode existir)
     */
    public AutorizacaoFiltro(Set<Perfil> permitidos) {
        if (permitidos == null || permitidos.isEmpty()) {
            throw new IllegalArgumentException("Toda rota privada precisa declarar ao menos um perfil permitido.");
        }
        this.permitidos = Set.copyOf(permitidos);
    }

    @Override
    public void doFilter(ServletRequest requisicao, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) requisicao;
        HttpServletResponse respostaHttp = (HttpServletResponse) resposta;
        try {
            SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(http);
            if (!permitidos.contains(sessao.perfil())) {
                // Registra so identificadores tecnicos: nunca token, chave nem conteudo do recurso.
                LOG.warn("Acesso negado. usuarioId={} perfil={} metodo={} caminho={} codigo=ACESSO_NEGADO",
                        sessao.usuarioId(), sessao.perfil(), http.getMethod(), http.getRequestURI());
                RespostaErro.tratar(respostaHttp, Erros.acessoNegado());
                return;
            }
        } catch (RuntimeException falha) {
            // Inclui a rota montada sem AutenticacaoFiltro (erro de montagem): sai como 500 generico.
            RespostaErro.tratar(respostaHttp, falha);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
