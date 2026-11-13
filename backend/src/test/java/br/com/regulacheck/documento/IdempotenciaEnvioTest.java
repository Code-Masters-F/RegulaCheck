package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US3 (FR-010 a FR-014, SC-003): repetir um envio com a mesma identificacao de tentativa nao duplica; reutilizar a
 * identificacao com outro conteudo ou tipo e recusado; um envio interrompido nao deixa arquivo sem registro.
 */
class IdempotenciaEnvioTest {

    private static final String CHAVE = "7d1c1b8e-9f2a-4c3e-8a10-aa11bb22cc33";

    @Test
    void repetirOMesmoEnvioDezVezesGeraUmDocumentoEUmaRevisao() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] arquivo = ArquivosDeTeste.pdf(1);

            Resposta primeira = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", arquivo, CHAVE);
            assertEquals(201, primeira.status(), primeira.corpo());
            for (int i = 0; i < 10; i++) {
                Resposta repetida = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", arquivo, CHAVE);

                assertEquals(200, repetida.status(), repetida.corpo());
                assertEquals(primeira.json().get("id").asText(), repetida.json().get("id").asText());
                assertEquals(2, repetida.json().get("versaoConjuntoDocumental").asInt());
                assertEquals(1, repetida.json().get("documentosDisponiveis").asInt());
                assertEquals("", repetida.cabecalho("Location"), "repeticao nao anuncia recurso novo");
            }
            assertEquals(1, c.cenario.documentos.todos().size());
            assertEquals(2, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
            assertEquals(1, c.arquivosGuardados(), "a repeticao nao grava outro arquivo");
            assertEquals(0, c.arquivosNaQuarentena());
        }
    }

    @Test
    void repeticaoComOPedidoJaCheioContinuaDevolvendoODocumentoExistente() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] primeiro = ArquivosDeTeste.pdf(1);
            c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", primeiro, CHAVE);
            c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null);
            c.enviar(c.authA, pedido, "OUTRO", "c.pdf", ArquivosDeTeste.pdf(3), null);

            Resposta repetida = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", primeiro, CHAVE);

            assertEquals(200, repetida.status(), "repetir nao e o quarto documento: " + repetida.corpo());
            assertEquals(3, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void mesmaChaveComOutroConteudoOuOutroTipoEh409SemEfeitos() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", ArquivosDeTeste.pdf(1), CHAVE);

            Resposta outroConteudo = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", ArquivosDeTeste.pdf(2),
                    CHAVE);
            Resposta outroTipo = c.enviar(c.authA, pedido, "OUTRO", "enc.pdf", ArquivosDeTeste.pdf(1), CHAVE);

            assertEquals(409, outroConteudo.status());
            assertEquals("CHAVE_ENVIO_REUTILIZADA", outroConteudo.json().get("codigo").asText());
            assertEquals(409, outroTipo.status());
            assertEquals("CHAVE_ENVIO_REUTILIZADA", outroTipo.json().get("codigo").asText());
            assertEquals(1, c.cenario.documentos.todos().size());
            assertEquals(2, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
            assertEquals(1, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());
        }
    }

    @Test
    void semChaveCadaEnvioEhNovoAteOLimiteDeTres() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] arquivo = ArquivosDeTeste.pdf(1);

            for (int i = 0; i < 3; i++) {
                assertEquals(201, c.enviar(c.authA, pedido, "OUTRO", "a.pdf", arquivo, null).status());
            }
            Resposta quarto = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", arquivo, null);

            assertEquals(422, quarto.status());
            assertEquals(3, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void chaveEhIndependenteEntrePedidosEEntrePacientes() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedidoA1 = c.criarRascunho(c.authA);
            String pedidoA2 = c.criarRascunho(c.authA);
            String pedidoB = c.criarRascunho(c.authB);
            byte[] arquivo = ArquivosDeTeste.pdf(1);

            Resposta a1 = c.enviar(c.authA, pedidoA1, "OUTRO", "a.pdf", arquivo, CHAVE);
            Resposta a2 = c.enviar(c.authA, pedidoA2, "OUTRO", "a.pdf", arquivo, CHAVE);
            Resposta b = c.enviar(c.authB, pedidoB, "OUTRO", "a.pdf", arquivo, CHAVE);

            assertEquals(201, a1.status());
            assertEquals(201, a2.status());
            assertEquals(201, b.status());
            assertEquals(3, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void chaveMalformadaEh400ComDetalheENadaEhGravado() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            for (String chave : List.of("curta", "x".repeat(65), "tem espaco no meio", "com/barra-1234", "acentuação-123")) {
                Resposta r = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), chave);

                assertEquals(400, r.status(), chave);
                assertEquals("chaveEnvio", r.json().get("detalhes").get(0).get("campo").asText(), chave);
            }
            assertEquals(0, c.cenario.documentos.todos().size());
            assertEquals(0, c.arquivosGuardados());
        }
    }

    @Test
    void falhaAoRegistrarRemoveOArquivoGuardadoEPermiteRepetirComSucesso() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] arquivo = ArquivosDeTeste.pdf(1);
            c.cenario.documentos.falharNoProximoRegistro(new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes."));

            Resposta falha = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", arquivo, CHAVE);

            assertEquals(503, falha.status());
            assertEquals(0, c.arquivosGuardados(), "arquivo sem registro nao pode permanecer");
            assertEquals(0, c.arquivosNaQuarentena());
            assertEquals(0, c.cenario.documentos.todos().size());
            assertEquals(1, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());

            Resposta repetida = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "enc.pdf", arquivo, CHAVE);

            assertEquals(201, repetida.status(), "a paciente pode repetir o envio interrompido");
            assertEquals(1, c.cenario.documentos.todos().size());
            assertEquals(1, c.arquivosGuardados());
        }
    }

    @Test
    void erroInesperadoAoRegistrarTambemNaoDeixaArquivoOrfaoNemVazaDetalhe() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            c.cenario.documentos.falharNoProximoRegistro(new IllegalStateException("ORA-99999 segredo-interno"));

            Resposta falha = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);

            assertEquals(500, falha.status());
            assertFalse(falha.corpo().contains("ORA-"), falha.corpo());
            assertFalse(falha.corpo().contains("segredo-interno"), falha.corpo());
            assertEquals(0, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());
        }
    }
}
