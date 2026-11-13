package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US1: ler os documentos do pedido e ver, por documento, o que foi extraido (leitura simulada). */
class ExtrairEndpointTest {

    private static JsonNode documento(JsonNode corpo, String documentoId) {
        for (JsonNode d : corpo.get("documentos")) {
            if (d.get("documentoId").asText().equals(documentoId)) {
                return d;
            }
        }
        throw new AssertionError("documento " + documentoId + " ausente da leitura");
    }

    private static JsonNode campo(JsonNode documento, String codigo) {
        for (JsonNode c : documento.get("campos")) {
            if (c.get("codigo").asText().equals(codigo)) {
                return c;
            }
        }
        throw new AssertionError("campo " + codigo + " ausente");
    }

    @Test
    void lePdfEFotoEDevolveOsCamposDoGabaritoMarcadosComoSimulados() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = c.criarRascunho(c.authA);
            String docPdf = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
            String docFoto = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.IDENTIFICACAO);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(201, r.status());
            JsonNode corpo = r.json();
            assertEquals("ATUAL", corpo.get("situacao").asText());
            assertTrue(corpo.get("id").asText().matches("[1-9][0-9]*"));
            assertTrue(corpo.get("simulada").asBoolean());
            assertTrue(corpo.get("aviso").asText().contains("simulada"));
            assertEquals(3, corpo.get("versaoConjuntoDocumental").asInt());
            assertEquals(2, corpo.get("documentos").size());

            JsonNode pdf = documento(corpo, docPdf);
            assertEquals("LIDO", pdf.get("situacao").asText());
            assertEquals("ENCAMINHAMENTO", pdf.get("tipoDocumental").asText());
            JsonNode motivo = campo(pdf, "MOTIVO_ENCAMINHAMENTO");
            assertEquals("IDENTIFICADO", motivo.get("situacao").asText());
            assertEquals("Avaliacao cardiologica ambulatorial (texto sintetico)", motivo.get("valor").asText());
            assertEquals(1, motivo.get("origem").get("pagina").asInt());
            assertEquals("2026-10-20", campo(pdf, "DATA_EMISSAO").get("valor").asText());

            JsonNode foto = documento(corpo, docFoto);
            assertEquals("IDENTIFICACAO", foto.get("tipoDocumental").asText());
            JsonNode nome = campo(foto, "NOME");
            assertEquals("Maria Exemplo da Silva", nome.get("valor").asText());
            assertFalse(nome.get("origem").has("pagina"), "foto nao tem pagina de origem");
            assertTrue(nome.get("origem").has("trecho"));
            // Cada documento traz os seus proprios campos: a foto nao tem motivo nem emissao.
            assertEquals("NAO_LOCALIZADO", campo(foto, "MOTIVO_ENCAMINHAMENTO").get("situacao").asText());
            assertEquals("NAO_LOCALIZADO", campo(foto, "DATA_EMISSAO").get("situacao").asText());
            assertEquals("ENCAMINHAMENTO", campo(pdf, "TIPO_DOCUMENTO").get("valor").asText());
            assertEquals("IDENTIFICACAO", campo(foto, "TIPO_DOCUMENTO").get("valor").asText());
        }
    }

    @Test
    void consultarDevolveAMesmaLeituraMarcadaComoSimulada() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            JsonNode criada = c.lerDocumentos(c.authA, pedido).json();

            Resposta r = c.consultarLeitura(c.authA, pedido);

            assertEquals(200, r.status());
            assertEquals(criada, r.json());
            assertTrue(r.json().get("simulada").asBoolean());
        }
    }

    @Test
    void repetirALeituraNaMesmaVersaoDevolve200SemChamarOLeitorDeNovo() throws Exception {
        LeitorFalso contador = new LeitorFalso(arquivo -> br.com.regulacheck.integracao.extracao.LeituraDeDocumento
                .vazia());
        try (CenarioDocumento c = CenarioExtracao.iniciar(contador)) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            assertEquals(201, c.lerDocumentos(c.authA, pedido).status());
            assertEquals(1, contador.chamadas());
            Resposta segunda = c.lerDocumentos(c.authA, pedido);

            assertEquals(200, segunda.status());
            assertEquals(1, contador.chamadas(), "o leitor nao e chamado de novo");
            assertEquals(1, c.cenario.extracoes.totalDoPedido(Long.parseLong(pedido)));
        }
    }

    @Test
    void oLeitorRecebeOsBytesDoArquivoGuardadoESoEles() throws Exception {
        LeitorFalso leitor = LeitorFalso.vazio();
        try (CenarioDocumento c = CenarioExtracao.iniciar(leitor)) {
            Amostra amostra = AmostrasSinteticas.ENCAMINHAMENTO;
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, amostra);

            c.lerDocumentos(c.authA, pedido);

            assertEquals(1, leitor.recebidos().size());
            assertTrue(java.util.Arrays.equals(amostra.bytes(), leitor.recebidos().get(0)));
        }
    }

    @Test
    void pedidoSemDocumentosRecebe422SemCriarNada() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = c.criarRascunho(c.authA);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(422, r.status());
            assertEquals("SEM_DOCUMENTOS", r.json().get("codigo").asText());
            assertEquals(0, c.cenario.extracoes.total());
        }
    }

    @Test
    void consultarSemLeituraPreviaRecebe404() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);

            assertEquals(404, c.consultarLeitura(c.authA, pedido).status());
        }
    }

    @Test
    void pedidoAlheioInexistenteEIdMalformadoTemRespostaIdentica() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedidoDeA = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.lerDocumentos(c.authA, pedidoDeA);

            Resposta alheioPost = c.lerDocumentos(c.authB, pedidoDeA);
            Resposta alheioGet = c.consultarLeitura(c.authB, pedidoDeA);
            Resposta inexistentePost = c.lerDocumentos(c.authB, "999999");
            Resposta inexistenteGet = c.consultarLeitura(c.authB, "999999");
            Resposta malformado = c.consultarLeitura(c.authB, "abc");

            for (Resposta r : new Resposta[] {alheioPost, alheioGet, inexistentePost, inexistenteGet, malformado}) {
                assertEquals(404, r.status());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
            }
            assertEquals(alheioGet.json().get("mensagem"), inexistenteGet.json().get("mensagem"));
            assertEquals(alheioPost.json().get("mensagem"), malformado.json().get("mensagem"));
            assertEquals(1, c.cenario.extracoes.total(), "a paciente B nao criou leitura nenhuma");
        }
    }

    @Test
    void leituraNaoAlteraEstadoRevisaoNemRespostaDoPedido() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            JsonNode antes = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            int revisoesAntes = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size();

            c.lerDocumentos(c.authA, pedido);

            JsonNode depois = c.api.get("/solicitacoes/" + pedido, c.authA).json();
            assertEquals(antes, depois);
            assertEquals("RASCUNHO", depois.get("estado").asText());
            assertEquals(revisoesAntes, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
        }
    }

    @Test
    void metodosNaoPrevistosRecebem405ComAllow() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            for (String metodo : new String[] {"PUT", "DELETE", "PATCH"}) {
                Resposta r = c.api.enviar(metodo, "/solicitacoes/" + pedido + "/extracoes", null, c.authA);
                assertEquals(405, r.status(), metodo);
                assertEquals(java.util.Set.of("GET", "POST"), metodosDe(r.cabecalho("Allow")), metodo);
            }
            assertEquals(404, c.api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes/1", null, c.authA)
                    .status());
        }
    }

    @Test
    void corpoECabecalhosDeDonoOuEstadoSaoIgnorados() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            Map<String, String> cabecalhos = new java.util.HashMap<>(c.authA);
            cabecalhos.put("X-Paciente-Id", "2");
            cabecalhos.put("X-Estado", "SUBMETIDA_SIMULACAO");
            Resposta r = c.api.enviar("POST", "/solicitacoes/" + pedido + "/extracoes",
                    "{\"pacienteId\":2,\"estado\":\"SUBMETIDA_SIMULACAO\",\"revisao\":99}", cabecalhos);
            assertEquals(201, r.status());
            assertEquals(2, r.json().get("versaoConjuntoDocumental").asInt());
        }
    }

    @Test
    void respostaNaoTrazCaminhoHashNomeInternoNemResultadoDeCompletude() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO,
                    AmostrasSinteticas.IDENTIFICACAO);
            String corpo = c.lerDocumentos(c.authA, pedido).corpo();

            for (var documento : c.cenario.documentos.todos()) {
                assertFalse(corpo.contains(documento.caminhoInterno().valor()));
                assertFalse(corpo.contains(documento.hashConteudo()));
                assertFalse(corpo.contains(documento.nomeArquivo()));
            }
            for (String proibido : new String[] {"resultadoGeral", "pendencia", "COM_PENDENCIAS", "REQUER_CONFERENCIA",
                    "SEM_PENDENCIAS", "analise"}) {
                assertFalse(corpo.contains(proibido), proibido);
            }
            assertNull(c.api.get("/solicitacoes/" + pedido, c.authA).json().get("pendencias"));
        }
    }

    @Test
    void excluirRascunhoComLeituraApagaTudoSemResiduo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.lerDocumentos(c.authA, pedido);
            assertEquals(1, c.cenario.extracoes.total());

            Resposta r = c.excluir(c.authA, pedido, "EXCLUIR");

            assertEquals(204, r.status());
            assertEquals(0, c.cenario.extracoes.total());
            assertEquals(404, c.consultarLeitura(c.authA, pedido).status());
            c.conferirIntegridade();
        }
    }

    private static java.util.Set<String> metodosDe(String allow) {
        return java.util.Arrays.stream(allow.split(",")).map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
    }
}
