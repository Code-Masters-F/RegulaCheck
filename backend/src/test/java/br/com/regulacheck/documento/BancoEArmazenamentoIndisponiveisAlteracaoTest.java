package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.ServidorDeTeste.RespostaBinaria;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.documento.repository.NovoDocumento;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * FR-020: banco ou area de arquivos indisponiveis em remover, substituir e excluir respondem o 503 padrao sem
 * detalhe tecnico, nao deixam mudanca pela metade, nao apagam arquivo de quem nao foi removido e nao deixam arquivo
 * novo orfao.
 */
class BancoEArmazenamentoIndisponiveisAlteracaoTest {

    private static final ExcecaoNegocio FORA = new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
            "Servico temporariamente indisponivel. Tente novamente em instantes.");

    @Test
    void bancoForaNoRemoverESubstituirDa503NenhumArquivoEApagadoENadaMuda() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] original = ArquivosDeTeste.pdf(1);
            String doc = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", original, null).json().get("id").asText();

            c.cenario.documentos.falharNaProximaAlteracao(FORA);
            Resposta remover = c.remover(c.authA, pedido, doc);
            c.cenario.documentos.falharNaProximaAlteracao(FORA);
            Resposta substituir = c.substituir(c.authA, pedido, doc, "OUTRO", "n.pdf", ArquivosDeTeste.pdf(2), null);

            for (Resposta r : new Resposta[] {remover, substituir}) {
                assertEquals(503, r.status(), r.corpo());
                assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
                assertFalse(r.corpo().contains(c.raiz.toString()));
            }
            assertEquals(1, c.arquivosGuardados(), "o arquivo antigo ficou e o novo nao ficou orfao");
            assertEquals(0, c.arquivosNaQuarentena());
            RespostaBinaria baixa = c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + doc, c.authA);
            assertArrayEquals(original, baixa.corpo(), "o documento antigo continua abrindo");
            assertEquals(2, c.api.get("/solicitacoes/" + pedido, c.authA).json().get("revisao").asInt());
            c.conferirIntegridade();
        }
    }

    @Test
    void bancoForaNaExclusaoDa503ENenhumArquivoEApagado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null);

            c.cenario.solicitacoes.falharNaProximaExclusao(FORA);
            Resposta r = c.excluir(c.authA, pedido, "EXCLUIR");

            assertEquals(503, r.status(), r.corpo());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertEquals(200, c.api.get("/solicitacoes/" + pedido, c.authA).status(), "o pedido segue existindo");
            assertEquals(2, c.arquivosGuardados(), "nenhum arquivo foi apagado");
            c.conferirIntegridade();
            // Tentar de novo, com o banco de volta, funciona.
            assertEquals(204, c.excluir(c.authA, pedido, "EXCLUIR").status());
            assertEquals(0, c.arquivosGuardados());
        }
    }

    @Test
    void semAreaDeArquivosSubstituirDa503EOAntigoFicaEnquantoRemoverEExcluirTemSucessoNoBanco() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar(new CenarioAuth(), false)) {
            String pedido = c.criarRascunho(c.authA);
            long id = Long.parseLong(pedido);
            // Sem area de arquivos nao ha como enviar: os documentos sao registrados direto, com nome interno.
            long d1 = registrarDireto(c, id, "d1");
            long d2 = registrarDireto(c, id, "d2");

            Resposta substituir = c.substituir(c.authA, pedido, String.valueOf(d1), "OUTRO", "n.pdf",
                    ArquivosDeTeste.pdf(1), null);

            assertEquals(503, substituir.status(), substituir.corpo());
            assertEquals("DEPENDENCIA_INDISPONIVEL", substituir.json().get("codigo").asText());
            assertEquals(2, c.cenario.documentos.todos().size(), "o antigo continua disponivel");
            assertEquals(0, c.cenario.documentos.removidos().size());
            assertEquals(3, c.api.get("/solicitacoes/" + pedido, c.authA).json().get("revisao").asInt());

            // O banco muda mesmo sem poder apagar o arquivo: a remocao e a exclusao nao respondem 503.
            Resposta remover = c.remover(c.authA, pedido, String.valueOf(d2));
            assertEquals(200, remover.status(), remover.corpo());
            assertEquals(204, c.excluir(c.authA, pedido, "EXCLUIR").status());
        }
    }

    private static long registrarDireto(CenarioDocumento c, long pedido, String nome) {
        return c.cenario.documentos.registrar(1, pedido, new NovoDocumento(nome + ".pdf", TipoDocumental.OUTRO,
                TipoArquivo.PDF, 10, 1, IdInterno.novo(), "0".repeat(64), null), Instant.now()).documento().id();
    }
}
