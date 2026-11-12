package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Recurso;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Rotas de exemplo, so de teste, montadas pelo mesmo {@link RotasPrivadas} da producao. Servem para
 * provar o mecanismo; nao sao rotas de negocio.
 */
public final class RotasDeExemplo {

    private RotasDeExemplo() {
    }

    /** Corpo aceito pelo PUT de exemplo; qualquer campo de dona enviado e ignorado. */
    public record Alteracao(String nome) {
    }

    /**
     * Registra as rotas de exemplo.
     *
     * @param rotas registro de rotas privadas
     * @param recursos dados de exemplo
     */
    public static void registrar(RotasPrivadas rotas, RecursosDeExemplo recursos) {
        for (Tipo tipo : Tipo.values()) {
            rotas.registrar("/teste/" + tipo.caminho() + "/*", new RecursoServlet(tipo, recursos), Perfil.PACIENTE);
        }
        rotas.registrar("/teste/admin/indicadores", new IndicadoresServlet(recursos), Perfil.ADMINISTRADOR);
        rotas.registrarTelemetria("/teste/telemetria/*", new TelemetriaServlet(recursos));
    }

    /** GET/PUT/DELETE de um recurso da paciente e listagem; sempre pela regra de dona. */
    static final class RecursoServlet extends ServletJson {
        private static final long serialVersionUID = 1L;
        private final transient Tipo tipo;
        private final transient RecursosDeExemplo recursos;

        RecursoServlet(Tipo tipo, RecursosDeExemplo recursos) {
            super("GET", "PUT", "DELETE");
            this.tipo = tipo;
            this.recursos = recursos;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(req);
            if (req.getPathInfo() == null || req.getPathInfo().equals("/")) {
                responderJson(resp, 200, Map.of("ids", recursos.listar(tipo, sessao.usuarioId())));
                return;
            }
            Recurso recurso = localizar(req, sessao);
            responderJson(resp, 200, Map.of("id", recurso.id(), "tipo", tipo.name(), "versao", recurso.versao()));
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(req);
            Recurso recurso = localizar(req, sessao);
            // Dona antes da validacao do corpo: corpo ruim em recurso alheio continua sendo 404.
            Alteracao corpo = CorpoJson.ler(req.getInputStream(), req.getContentType(), Alteracao.class);
            if (corpo.nome() == null || corpo.nome().isBlank()) {
                throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "O corpo da requisição é inválido.");
            }
            recursos.alterar(recurso);
            responderJson(resp, 200, Map.of("id", recurso.id(), "versao", recurso.versao()));
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
            Recurso recurso = localizar(req, AutenticacaoFiltro.sessaoDe(req));
            recursos.remover(recurso);
            resp.setStatus(204);
        }

        private Recurso localizar(HttpServletRequest req, SessaoAutenticada sessao) {
            String[] partes = req.getPathInfo() == null ? new String[0] : req.getPathInfo().substring(1).split("/");
            Optional<Recurso> achado = Optional.empty();
            try {
                if (tipo == Tipo.DOCUMENTO && partes.length == 2) {
                    achado = recursos.buscar(tipo, Long.parseLong(partes[1]), Long.parseLong(partes[0]),
                            sessao.usuarioId());
                } else if (tipo != Tipo.DOCUMENTO && partes.length == 1) {
                    achado = recursos.buscar(tipo, Long.parseLong(partes[0]), null, sessao.usuarioId());
                }
            } catch (NumberFormatException idInvalido) {
                // Identificador ilegivel responde como "nao encontrado", igual a um que nao existe.
            }
            return Propriedade.exigir(achado, sessao);
        }
    }

    /** Indicadores agregados: so totais, sem dado identificavel. */
    static final class IndicadoresServlet extends ServletJson {
        private static final long serialVersionUID = 1L;
        private final transient RecursosDeExemplo recursos;

        IndicadoresServlet(RecursosDeExemplo recursos) {
            super("GET");
            this.recursos = recursos;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            responderJson(resp, 200, recursos.totais());
        }
    }

    /** Telemetria: aceita a chave do simulador; a sessao de captura precisa existir e estar aberta. */
    static final class TelemetriaServlet extends ServletJson {
        private static final long serialVersionUID = 1L;
        private final transient RecursosDeExemplo recursos;

        TelemetriaServlet(RecursosDeExemplo recursos) {
            super("POST");
            this.recursos = recursos;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            String info = req.getPathInfo();
            long id;
            try {
                id = Long.parseLong(info == null ? "" : info.substring(1));
            } catch (NumberFormatException invalido) {
                throw Erros.naoEncontrado();
            }
            if (!recursos.capturaAberta(id)) {
                throw Erros.naoEncontrado();
            }
            responderJson(resp, 200, Map.of("recebida", true));
        }
    }
}
