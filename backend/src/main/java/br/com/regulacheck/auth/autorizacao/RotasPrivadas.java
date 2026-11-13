package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.ServletMapping;

/**
 * Unico caminho para publicar uma rota privada: declara o caminho com os perfis permitidos e
 * instala, na ordem certa, a autenticacao (401) e a autorizacao por perfil (403).
 *
 * <p><strong>Negar por omissao.</strong> Nao ha como registrar rota privada sem perfil
 * ({@link AutorizacaoFiltro} recusa lista vazia) e {@link #exigirTudoDeclarado()}, chamado ao final
 * da montagem da aplicacao, impede a subida do servidor se algum caminho tiver sido publicado
 * direto no contexto, fora deste registro. Rotas publicas sao apenas as de {@link #PUBLICAS}.
 *
 * <p>Como usar numa historia nova (ver {@code docs/api/autorizacao.md}):
 * <pre>{@code
 * rotas.registrar("/solicitacoes/*", new SolicitacaoController(servico), Perfil.PACIENTE);
 * }</pre>
 * O Service do recurso identifica a paciente pela sessao e passa a busca ja filtrada pela dona por
 * {@link Propriedade#exigir}.
 */
public final class RotasPrivadas {

    /**
     * Caminhos que dispensam sessao: saude tecnica, login e a rota coringa que responde 404 padrao.
     * Qualquer outro caminho publicado precisa estar declarado neste registro.
     */
    public static final Set<String> PUBLICAS = Set.of("/health", "/auth/login", "/*");

    private final ServletContextHandler contexto;
    private final AutenticacaoFiltro autenticacao;
    private final SimuladorFiltro simulador;
    private final Set<String> declaradas = new LinkedHashSet<>();

    /**
     * @param contexto contexto de servlets onde as rotas serao publicadas
     * @param autenticacao filtro que identifica a conta (401)
     * @param simulador filtro da credencial do simulador, usado so por {@link #registrarTelemetria}
     */
    public RotasPrivadas(ServletContextHandler contexto, AutenticacaoFiltro autenticacao, SimuladorFiltro simulador) {
        this.contexto = contexto;
        this.autenticacao = autenticacao;
        this.simulador = simulador;
    }

    /**
     * Publica uma rota privada de conta.
     *
     * @param caminho caminho do servlet (ex.: {@code /solicitacoes/*})
     * @param servlet controller da rota
     * @param perfis perfis que podem usar a rota; ao menos um
     * @throws IllegalArgumentException se nao houver perfil ou o caminho ja estiver declarado ou for publico
     */
    public void registrar(String caminho, HttpServlet servlet, Perfil... perfis) {
        registrar(caminho, servlet, null, perfis);
    }

    /**
     * Publica uma rota privada de conta cujo servlet recebe {@code multipart/form-data} (envio de arquivos, RC-08).
     * A autenticacao e a autorizacao por perfil sao as mesmas de {@link #registrar(String, HttpServlet, Perfil...)};
     * a configuracao multipart so vale para requisicoes multipart e define os limites e a pasta temporaria
     * privada onde o conteiner grava as partes.
     *
     * @param caminho caminho do servlet (ex.: {@code /solicitacoes/*})
     * @param servlet controller da rota
     * @param multipart limites e pasta temporaria das partes; {@code null} para uma rota sem multipart
     * @param perfis perfis que podem usar a rota; ao menos um
     * @throws IllegalArgumentException se nao houver perfil ou o caminho ja estiver declarado ou for publico
     */
    public void registrar(String caminho, HttpServlet servlet, MultipartConfigElement multipart, Perfil... perfis) {
        if (perfis == null || perfis.length == 0) {
            throw new IllegalArgumentException("A rota " + caminho + " precisa declarar ao menos um perfil.");
        }
        declarar(caminho);
        EnumSet<DispatcherType> tipos = EnumSet.of(DispatcherType.REQUEST);
        contexto.addFilter(new FilterHolder(autenticacao), caminho, tipos);
        contexto.addFilter(new FilterHolder(new AutorizacaoFiltro(Set.of(perfis))), caminho, tipos);
        ServletHolder suporte = new ServletHolder(servlet);
        if (multipart != null) {
            suporte.getRegistration().setMultipartConfig(multipart);
        }
        contexto.addServlet(suporte, caminho);
    }

    /**
     * Publica a rota de telemetria, a unica que aceita a credencial do simulador (e recusa contas).
     *
     * @param caminho caminho do servlet
     * @param servlet controller da rota
     * @throws IllegalArgumentException se o caminho ja estiver declarado ou for publico
     */
    public void registrarTelemetria(String caminho, HttpServlet servlet) {
        declarar(caminho);
        contexto.addFilter(new FilterHolder(simulador), caminho, EnumSet.of(DispatcherType.REQUEST));
        contexto.addServlet(new ServletHolder(servlet), caminho);
    }

    /** @return caminhos privados declarados ate agora (somente leitura) */
    public Set<String> declaradas() {
        return Collections.unmodifiableSet(declaradas);
    }

    /**
     * Confere que todo servlet publicado no contexto e publico conhecido ou foi declarado aqui.
     *
     * @throws IllegalStateException se houver caminho publicado fora do registro (rota privada
     *     esquecida); a mensagem lista os caminhos, sem dados sensiveis
     */
    public void exigirTudoDeclarado() {
        ServletHandler tratador = contexto.getServletHandler();
        Set<String> fora = new LinkedHashSet<>();
        for (ServletMapping mapeamento : tratador.getServletMappings()) {
            for (String caminho : mapeamento.getPathSpecs()) {
                if (!PUBLICAS.contains(caminho) && !declaradas.contains(caminho)) {
                    fora.add(caminho);
                }
            }
        }
        if (!fora.isEmpty()) {
            throw new IllegalStateException("Rotas publicadas fora de RotasPrivadas (sem perfil declarado): " + fora);
        }
    }

    private void declarar(String caminho) {
        if (PUBLICAS.contains(caminho)) {
            throw new IllegalArgumentException("O caminho " + caminho + " e publico e nao pode ser declarado privado.");
        }
        if (!declaradas.add(caminho)) {
            throw new IllegalArgumentException("O caminho " + caminho + " ja foi declarado.");
        }
    }
}
