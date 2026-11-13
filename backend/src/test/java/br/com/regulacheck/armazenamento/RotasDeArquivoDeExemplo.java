package br.com.regulacheck.armazenamento;

import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.autorizacao.RotasPrivadas;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.http.DownloadSeguro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rota de download de exemplo, so de teste, montada pelo mesmo {@link RotasPrivadas} da producao: mostra
 * o uso combinado de busca filtrada pela dona, {@link Propriedade} e {@link DownloadSeguro}. Nao e rota
 * de negocio.
 */
public final class RotasDeArquivoDeExemplo {

    /** Registro de exemplo de um documento (equivale a uma linha de RC_DOCUMENTO). */
    public record Documento(long id, long dona, ArquivoGuardado arquivo) {
    }

    /** Repositorio em memoria que conta os acessos, para provar que o 403 acontece antes dele. */
    public static final class Documentos {
        private final Map<Long, Documento> porId = new ConcurrentHashMap<>();
        private final AtomicInteger acessos = new AtomicInteger();

        public void adicionar(Documento documento) {
            porId.put(documento.id(), documento);
        }

        /** Busca ja filtrada pela dona, como fara o Repository real. */
        Optional<Documento> buscar(long id, long dona) {
            acessos.incrementAndGet();
            return Optional.ofNullable(porId.get(id)).filter(d -> d.dona() == dona);
        }

        public int acessos() {
            return acessos.get();
        }
    }

    private RotasDeArquivoDeExemplo() {
    }

    /**
     * Registra {@code GET /teste/arquivos/{id}} para pacientes.
     *
     * @param rotas registro de rotas privadas
     * @param documentos documentos de exemplo
     * @param armazenamento area privada onde estao os arquivos
     */
    public static void registrar(RotasPrivadas rotas, Documentos documentos, ArmazenamentoPrivado armazenamento) {
        rotas.registrar("/teste/arquivos/*", new DownloadServlet(documentos, armazenamento), Perfil.PACIENTE);
    }

    static final class DownloadServlet extends ServletJson {
        private static final long serialVersionUID = 1L;
        private final transient Documentos documentos;
        private final transient ArmazenamentoPrivado armazenamento;

        DownloadServlet(Documentos documentos, ArmazenamentoPrivado armazenamento) {
            super("GET");
            this.documentos = documentos;
            this.armazenamento = armazenamento;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(req);
            Documento documento = Propriedade.exigir(buscar(req, sessao), sessao);
            ArquivoGuardado arquivo = documento.arquivo();
            DownloadSeguro.enviar(resp, arquivo.tipo(), arquivo.nomeExibicao(), arquivo.tamanhoBytes(),
                    armazenamento.abrir(arquivo.id()));
        }

        private Optional<Documento> buscar(HttpServletRequest req, SessaoAutenticada sessao) {
            String info = req.getPathInfo();
            if (info == null || info.length() < 2) {
                return Optional.empty();
            }
            try {
                return documentos.buscar(Long.parseLong(info.substring(1)), sessao.usuarioId());
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
}
