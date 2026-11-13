package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.documento.repository.NovoDocumento;
import br.com.regulacheck.documento.repository.ResultadoRegistro;
import br.com.regulacheck.documento.repository.ResultadoRemocao;
import br.com.regulacheck.documento.repository.ResultadoSubstituicao;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Banco fora do ar e area de arquivos inutilizavel nas rotas de documentos: erro 503 generico, sem detalhe tecnico,
 * sem arquivo orfao; e a API recusa subir com uma pasta de arquivos impossivel de preparar.
 */
class BancoEArmazenamentoIndisponiveisTest {

    /** Repositorio de documentos cujo banco "caiu": toda operacao responde o 503 padrao. */
    private static final class DocumentosQueFalham implements DocumentoRepository {

        private static ExcecaoNegocio fora() {
            return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }

        @Override
        public ResultadoRegistro registrar(long pacienteId, long solicitacaoId, NovoDocumento novo, Instant agora) {
            throw fora();
        }

        @Override
        public ResultadoRemocao remover(long pacienteId, long solicitacaoId, long documentoId, Instant agora) {
            throw fora();
        }

        @Override
        public ResultadoSubstituicao substituir(long pacienteId, long solicitacaoId, long documentoId,
                NovoDocumento novo, Instant agora) {
            throw fora();
        }

        @Override
        public List<Documento> listarDoPedido(long solicitacaoId, long pacienteId) {
            throw fora();
        }

        @Override
        public Optional<Documento> buscarDoPedido(long documentoId, long solicitacaoId, long pacienteId) {
            throw fora();
        }

        @Override
        public Optional<Documento> buscarPorChave(long solicitacaoId, long pacienteId, String chaveEnvio) {
            throw fora();
        }
    }

    @Test
    void bancoForaDoArDa503GenericoNoEnvioNaListaENoDownloadSemDeixarArquivo() throws Exception {
        CenarioAuth base = new CenarioAuth();
        Dependencias deps = new Dependencias(base.usuarios, base.sessoes, base.protocolos, base.solicitacoes,
                new DocumentosQueFalham(), base.extracoes, base.conferencias,
                new br.com.regulacheck.integracao.extracao.LeitorSimulado(), base.relogio);
        try (CenarioDocumento c = CenarioDocumento.iniciar(base, deps, true)) {
            String pedido = c.criarRascunho(c.authA);

            Resposta envio = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), "chave-0000001");
            Resposta lista = c.api.get("/solicitacoes/" + pedido + "/documentos", c.authA);
            Resposta baixa = c.api.get("/solicitacoes/" + pedido + "/documentos/1", c.authA);

            for (Resposta r : new Resposta[] {envio, lista, baixa}) {
                assertEquals(503, r.status(), r.corpo());
                assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
                assertFalse(r.corpo().contains("ORA-"), r.corpo());
                assertFalse(r.corpo().contains(c.raiz.toString()), r.corpo());
            }
            assertEquals(0, c.arquivosGuardados(), "o envio que falhou nao deixa arquivo orfao");
            assertEquals(0, c.arquivosNaQuarentena());
            // O pedido continua acessivel: so as rotas de documentos dependem do repositorio que caiu.
            assertEquals(200, c.api.get("/solicitacoes/" + pedido, c.authA).status());
        }
    }

    @Test
    void pastaDeArquivosImpossivelDePrepararImpedeASubidaSemVazarOCaminho() throws Exception {
        Path arquivoComum = Files.createTempFile("regulacheck-nao-eh-pasta-", ".tmp");
        try {
            Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, null,
                    arquivoComum.toString(), null);

            IllegalStateException falha = assertThrows(IllegalStateException.class,
                    () -> ServidorDeTeste.iniciar(config, new CenarioAuth().dependencias()));

            assertTrue(falha.getMessage().toLowerCase().contains("pasta"), falha.getMessage());
            assertFalse(falha.getMessage().contains(arquivoComum.toString()), "a mensagem nao traz o caminho");
        } finally {
            Files.deleteIfExists(arquivoComum);
        }
    }
}
