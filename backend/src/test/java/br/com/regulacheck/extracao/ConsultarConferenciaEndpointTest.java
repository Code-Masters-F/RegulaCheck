package br.com.regulacheck.extracao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static br.com.regulacheck.extracao.CenarioConferencia.campo;
import static br.com.regulacheck.extracao.CenarioConferencia.documento;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** US1: ver o que foi lido, com a origem, e o que nao pode ser verificado (conferencia, so leitura). */
class ConsultarConferenciaEndpointTest {

    private static final String ORIENTACAO_NAO_LOCALIZADO = "Esta informação não consta no documento lido. Se ela "
            + "for necessária, peça um documento corrigido à unidade emissora. Não é possível preencher aqui.";
    private static final String ORIENTACAO_ILEGIVEL = "Não foi possível ler este campo. Envie um arquivo mais nítido.";

    private static Set<String> metodosDe(String allow) {
        return Set.of(allow.split(",\\s*"));
    }

    @Test
    void devolveCamposOrigemDestaquesEOrientacoesMarcadosComoSimulados() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);

            Resposta r = c.consultarConferencia(c.authA, p.pedido(), p.extracao());

            assertEquals(200, r.status());
            JsonNode corpo = r.json();
            assertEquals(p.extracao(), corpo.get("extracaoId").asText());
            assertEquals("REQUER_CONFERENCIA", corpo.get("situacao").asText());
            assertFalse(corpo.get("liberadoParaSubmissao").asBoolean());
            assertTrue(corpo.get("simulada").asBoolean());
            assertTrue(corpo.get("aviso").asText().contains("simulada"));
            // sem decisao, a conferencia "atualizou" quando a leitura foi feita
            JsonNode leitura = c.consultarLeitura(c.authA, p.pedido()).json();
            assertEquals(leitura.get("criadaEm").asText(), corpo.get("atualizadaEm").asText());
            // 1 nao localizado no encaminhamento + 1 ilegivel e 3 nao localizados na identificacao
            assertEquals(5, corpo.get("resumo").get("camposNaoVerificados").asInt());
            assertEquals(7, corpo.get("resumo").get("camposSemDecisao").asInt());
            assertEquals(0, corpo.get("resumo").get("divergencias").asInt());
            assertEquals(3, corpo.get("documentos").size());

            JsonNode enc = documento(corpo, p.encaminhamento());
            assertEquals("LIDO", enc.get("situacao").asText());
            assertEquals("ENCAMINHAMENTO", enc.get("tipoDocumental").asText());
            JsonNode nome = campo(enc, "NOME");
            assertEquals("IDENTIFICADO", nome.get("situacao").asText());
            assertEquals("Maria Exemplo da Silva", nome.get("valor").asText());
            assertEquals(1, nome.get("origem").get("pagina").asInt());
            assertEquals("Paciente: Maria Exemplo da Silva", nome.get("origem").get("trecho").asText());
            assertFalse(nome.get("naoVerificado").asBoolean());
            assertFalse(nome.has("orientacao"));
            assertFalse(nome.has("decisao"));

            JsonNode autorizacao = campo(enc, "AUTORIZACAO_ASSINADA");
            assertEquals("NAO_LOCALIZADO", autorizacao.get("situacao").asText());
            assertTrue(autorizacao.get("naoVerificado").asBoolean());
            assertFalse(autorizacao.has("valor"));
            assertFalse(autorizacao.has("origem"));
            assertEquals(ORIENTACAO_NAO_LOCALIZADO, autorizacao.get("orientacao").asText());

            JsonNode borrada = documento(corpo, p.borrada());
            JsonNode nascimento = campo(borrada, "DATA_NASCIMENTO");
            assertEquals("ILEGIVEL", nascimento.get("situacao").asText());
            assertTrue(nascimento.get("naoVerificado").asBoolean());
            assertFalse(nascimento.has("valor"));
            assertFalse(nascimento.has("origem"));
            assertEquals(ORIENTACAO_ILEGIVEL, nascimento.get("orientacao").asText());

            JsonNode vazio = documento(corpo, p.vazio());
            assertEquals("LEITURA_VAZIA", vazio.get("situacao").asText());
            assertTrue(vazio.get("reenviar").asBoolean());
            assertTrue(vazio.has("motivo"));
            assertTrue(vazio.get("orientacao").asText().contains("nítido"));
            assertEquals(0, vazio.get("campos").size());
        }
    }

    @Test
    void aLeituraOriginalApareceIdenticaAConsultaDaRC10() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            JsonNode leitura = c.consultarLeitura(c.authA, p.pedido()).json();
            JsonNode conferencia = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();

            assertEquals(leitura.get("documentos").size(), conferencia.get("documentos").size());
            for (JsonNode doc : leitura.get("documentos")) {
                JsonNode outro = documento(conferencia, doc.get("documentoId").asText());
                assertEquals(doc.get("situacao"), outro.get("situacao"));
                assertEquals(doc.get("tipoDocumental"), outro.get("tipoDocumental"));
                assertEquals(doc.get("campos").size(), outro.get("campos").size());
                for (JsonNode campo : doc.get("campos")) {
                    JsonNode igual = campo(outro, campo.get("codigo").asText());
                    assertEquals(campo.get("situacao"), igual.get("situacao"));
                    assertEquals(campo.get("valor"), igual.get("valor"));
                    assertEquals(campo.get("origem"), igual.get("origem"));
                }
            }
        }
    }

    @Test
    void pedidoSemLeituraLeituraInexistenteAlheiaOuDeOutroPedidoETudo404Identico() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String outroPedidoDaA = c.criarRascunho(c.authA);
            String semLeitura = c.criarRascunho(c.authA);

            List<Resposta> respostas = List.of(
                    c.consultarConferencia(c.authA, p.pedido(), "999999"),
                    c.consultarConferencia(c.authA, semLeitura, p.extracao()),
                    c.consultarConferencia(c.authA, outroPedidoDaA, p.extracao()),
                    c.consultarConferencia(c.authB, p.pedido(), p.extracao()),
                    c.consultarConferencia(c.authA, "999999", p.extracao()),
                    c.consultarConferencia(c.authA, p.pedido(), "abc"),
                    c.consultarConferencia(c.authA, p.pedido(), "0"),
                    c.consultarConferencia(c.authA, p.pedido(), "0" + p.extracao()),
                    c.consultarConferencia(c.authA, p.pedido(), "-1"));

            for (Resposta r : respostas) {
                assertEquals(404, r.status(), r.corpo());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
                assertEquals(respostas.get(0).json().get("mensagem"), r.json().get("mensagem"));
            }
        }
    }

    @Test
    void semTokenRecebe401EAdministradorRecebe403() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String caminho = "/solicitacoes/" + p.pedido() + "/extracoes/" + p.extracao() + "/conferencia";

            Resposta anonimo = c.api.get(caminho, Map.of());
            assertEquals(401, anonimo.status());
            assertEquals("NAO_AUTENTICADO", anonimo.json().get("codigo").asText());

            Map<String, String> admin = bearer(c.api.entrar(EMAIL_ADMIN, SENHA));
            Resposta negado = c.api.get(caminho, admin);
            assertEquals(403, negado.status());
            assertEquals("ACESSO_NEGADO", negado.json().get("codigo").asText());
            assertFalse(negado.corpo().contains("Maria Exemplo"));
        }
    }

    @Test
    void metodosNaoPrevistosRecebem405ComAllowDeGetEPut() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String caminho = "/solicitacoes/" + p.pedido() + "/extracoes/" + p.extracao() + "/conferencia";
            for (String metodo : new String[] {"DELETE", "PATCH"}) {
                Resposta r = c.api.enviar(metodo, caminho, null, c.authA);
                assertEquals(405, r.status(), metodo);
                assertEquals(Set.of("GET", "PUT"), metodosDe(r.cabecalho("Allow")), metodo);
            }
            // POST na conferencia tambem nao existe
            Resposta post = c.api.enviar("POST", caminho, null, c.authA);
            assertEquals(405, post.status());
        }
    }

    @Test
    void naoExpoeCaminhoHashNomeInternoNemResultadoDeRegra() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String corpo = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).corpo();

            for (String proibido : new String[] {"caminho", "hash", "sha256", "regulacheck-docs", ".pdf", ".png",
                    "resultadoGeral", "pendencia", "SEM_PENDENCIAS", "COM_PENDENCIAS", "NAO_VERIFICAVEL",
                    "\"estado\"", "\"revisao\""}) {
                assertFalse(corpo.contains(proibido), proibido);
            }
        }
    }

    @Test
    void variasConsultasNaoMudamEstadoRevisaoNemLeitura() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            long pedido = Long.parseLong(p.pedido());
            var antes = c.cenario.solicitacoes.buscarDaPaciente(pedido, 1).orElseThrow();
            int leituras = c.cenario.extracoes.total();
            int gravacoes = c.cenario.extracoes.gravacoes();

            for (int i = 0; i < 5; i++) {
                assertEquals(200, c.consultarConferencia(c.authA, p.pedido(), p.extracao()).status());
            }

            var depois = c.cenario.solicitacoes.buscarDaPaciente(pedido, 1).orElseThrow();
            assertEquals(antes, depois);
            assertEquals(leituras, c.cenario.extracoes.total());
            assertEquals(gravacoes, c.cenario.extracoes.gravacoes());
            assertEquals(0, c.cenario.conferencias.total());
        }
    }
}
