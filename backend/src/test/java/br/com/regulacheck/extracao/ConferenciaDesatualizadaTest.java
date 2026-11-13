package br.com.regulacheck.extracao;

import static br.com.regulacheck.extracao.CenarioConferencia.campo;
import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.confirmarEncaminhamento;
import static br.com.regulacheck.extracao.CenarioConferencia.documento;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/** US5: a conferencia acompanha a leitura; leitura desatualizada so consulta; pedido submetido so consulta. */
class ConferenciaDesatualizadaTest {

    private static final String MENSAGEM_DESATUALIZADA = "Os documentos mudaram; leia os documentos de novo.";

    private static Resposta substituirEncaminhamento(CenarioDocumento c, Preparo p) throws Exception {
        return c.substituir(c.authA, p.pedido(), p.encaminhamento(), "ENCAMINHAMENTO",
                AmostrasSinteticas.SEM_MOTIVO.nome(), AmostrasSinteticas.SEM_MOTIVO.bytes(), null);
    }

    @Test
    void substituirUmDocumentoDesatualizaAConferenciaQueContinuaVisivelESoParaLeitura() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
            assertEquals(1, c.cenario.conferencias.total());

            assertEquals(201, substituirEncaminhamento(c, p).status());

            Resposta get = c.consultarConferencia(c.authA, p.pedido(), p.extracao());
            assertEquals(200, get.status());
            assertEquals("DESATUALIZADA", get.json().get("situacao").asText());
            assertFalse(get.json().get("liberadoParaSubmissao").asBoolean());
            // as decisoes anteriores continuam visiveis, so para consulta
            assertEquals("CONFIRMADA", campo(documento(get.json(), p.encaminhamento()), "NOME").get("decisao")
                    .get("tipo").asText());

            Resposta put = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "DATA_EMISSAO")));
            assertEquals(409, put.status(), put.corpo());
            assertEquals("CONFLITO_ESTADO", put.json().get("codigo").asText());
            assertEquals(MENSAGEM_DESATUALIZADA, put.json().get("mensagem").asText());
            assertEquals(1, c.cenario.conferencias.total(), "nada foi gravado");
        }
    }

    @Test
    void removerEEnviarDocumentoNovoTambemDesatualizam() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            assertEquals(200, c.remover(c.authA, p.pedido(), p.vazio()).status());
            assertEquals("DESATUALIZADA",
                    c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json().get("situacao").asText());
            assertEquals(409, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
        }
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            // um documento so, para ainda haver vaga (limite de 3 por pedido) para o envio de um novo
            Preparo p = CenarioConferencia.prepararSoEncaminhamento(c);
            CenarioExtracao.enviar(c, c.authA, p.pedido(), AmostrasSinteticas.IDENTIFICACAO);
            assertEquals("DESATUALIZADA",
                    c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json().get("situacao").asText());
            assertEquals(409, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
            assertEquals(0, c.cenario.conferencias.total());
        }
    }

    @Test
    void novaLeituraNasceSemDecisoesEAAnteriorContinuaConsultavelPeloIdAntigo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    confirmarEncaminhamento(p.encaminhamento())).status());
            assertEquals(5, c.cenario.conferencias.total());
            assertEquals(201, substituirEncaminhamento(c, p).status());

            Resposta nova = c.lerDocumentos(c.authA, p.pedido());
            assertEquals(201, nova.status());
            String extracaoNova = nova.json().get("id").asText();
            assertNotEquals(p.extracao(), extracaoNova);

            JsonNode conferenciaNova = c.consultarConferencia(c.authA, p.pedido(), extracaoNova).json();
            assertNotEquals("DESATUALIZADA", conferenciaNova.get("situacao").asText());
            assertEquals(0, conferenciaNova.get("resumo").get("divergencias").asInt());
            for (JsonNode doc : conferenciaNova.get("documentos")) {
                for (JsonNode campo : doc.get("campos")) {
                    assertFalse(campo.has("decisao"), "decisao herdada em " + campo.get("codigo"));
                }
            }
            // a leitura anterior continua la, desatualizada, com as decisoes que a paciente tomou
            JsonNode antiga = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();
            assertEquals("DESATUALIZADA", antiga.get("situacao").asText());
            assertEquals("CONFIRMADA", campo(documento(antiga, p.encaminhamento()), "NOME").get("decisao")
                    .get("tipo").asText());
            assertEquals(5, c.cenario.conferencias.total(), "as decisoes antigas ficam como historico");

            // a nova leitura aceita decisoes novas, e so ela
            String docNovo = nova.json().get("documentos").get(0).get("documentoId").asText();
            assertEquals(200, c.decidir(c.authA, p.pedido(), extracaoNova, lote(confirmar(docNovo, "NOME")))
                    .status());
            assertEquals(409, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "DATA_EMISSAO"))).status());
        }
    }

    @Test
    void pedidoSubmetidoSoConsultaOPutRecebe409EOGetRecebe200() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
            c.cenario.solicitacoes.forcarEstado(Long.parseLong(p.pedido()), EstadoSolicitacao.SUBMETIDA_SIMULACAO);

            Resposta put = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "DATA_EMISSAO")));
            assertEquals(409, put.status());
            assertEquals("CONFLITO_ESTADO", put.json().get("codigo").asText());
            assertEquals("Este pedido já foi submetido e só pode ser consultado.", put.json().get("mensagem").asText());

            Resposta get = c.consultarConferencia(c.authA, p.pedido(), p.extracao());
            assertEquals(200, get.status());
            assertTrue(get.json().get("simulada").asBoolean());
            assertEquals(1, c.cenario.conferencias.total());
        }
    }

    @Test
    void documentoRemovidoNaoDeixaDecisaoComEfeitoNaLeituraNova() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    confirmarEncaminhamento(p.encaminhamento())).status());
            assertEquals(200, c.remover(c.authA, p.pedido(), p.encaminhamento()).status());

            String extracaoNova = c.lerDocumentos(c.authA, p.pedido()).json().get("id").asText();
            JsonNode nova = c.consultarConferencia(c.authA, p.pedido(), extracaoNova).json();

            assertEquals(2, nova.get("documentos").size(), "o documento removido nao esta na leitura nova");
            for (JsonNode doc : nova.get("documentos")) {
                assertNotEquals(p.encaminhamento(), doc.get("documentoId").asText());
            }
            // decidir o documento removido na leitura nova nao e possivel: ele nao faz parte dela
            assertEquals(404, c.decidir(c.authA, p.pedido(), extracaoNova,
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
        }
    }
}
