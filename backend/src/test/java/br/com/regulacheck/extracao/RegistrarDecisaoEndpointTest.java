package br.com.regulacheck.extracao;

import static br.com.regulacheck.extracao.CenarioConferencia.campo;
import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.documento;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US2: confirmar ou marcar divergencia, campo a campo (a paciente nunca digita valor). */
class RegistrarDecisaoEndpointTest {

    private static String caminho(Preparo p) {
        return "/solicitacoes/" + p.pedido() + "/extracoes/" + p.extracao() + "/conferencia";
    }

    @Test
    void confirmarCampoIdentificadoDevolveADecisaoEARefleteNaConferencia() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            JsonNode antes = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();
            assertEquals(7, antes.get("resumo").get("camposSemDecisao").asInt());

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.encaminhamento(), "NOME")));

            assertEquals(200, r.status(), r.corpo());
            JsonNode nome = campo(documento(r.json(), p.encaminhamento()), "NOME");
            assertEquals("CONFIRMADA", nome.get("decisao").get("tipo").asText());
            assertFalse(nome.get("decisao").has("motivo"));
            assertTrue(nome.get("decisao").get("decididaEm").asText().endsWith("Z"));
            assertFalse(nome.get("naoVerificado").asBoolean());
            assertEquals(6, r.json().get("resumo").get("camposSemDecisao").asInt());
            assertFalse(r.json().get("liberadoParaSubmissao").asBoolean());
            // a consulta seguinte enxerga a mesma decisao
            assertEquals(r.json(), c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json());
        }
    }

    @Test
    void divergirPorValorDiferenteMarcaNaoVerificadoENaoGuardaValorNenhum() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "DATA_EMISSAO", "VALOR_DIFERENTE_DO_DOCUMENTO")));

            assertEquals(200, r.status(), r.corpo());
            JsonNode emissao = campo(documento(r.json(), p.encaminhamento()), "DATA_EMISSAO");
            assertEquals("DIVERGENTE", emissao.get("decisao").get("tipo").asText());
            assertEquals("VALOR_DIFERENTE_DO_DOCUMENTO", emissao.get("decisao").get("motivo").asText());
            assertTrue(emissao.get("naoVerificado").asBoolean());
            assertTrue(emissao.get("orientacao").asText().contains("unidade emissora"));
            // o que foi lido continua sendo o que foi lido: a paciente nao substitui valor
            assertEquals("2026-10-20", emissao.get("valor").asText());
            assertEquals("REQUER_CONFERENCIA", r.json().get("situacao").asText());
            assertEquals(1, r.json().get("resumo").get("divergencias").asInt());
        }
    }

    @Test
    void campoNaoLocalizadoAceitaSoDivergenciaCampoExisteENaoGanhaValor() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "AUTORIZACAO_ASSINADA", "CAMPO_EXISTE_E_NAO_FOI_LIDO")));

            assertEquals(200, r.status(), r.corpo());
            JsonNode autorizacao = campo(documento(r.json(), p.encaminhamento()), "AUTORIZACAO_ASSINADA");
            assertEquals("NAO_LOCALIZADO", autorizacao.get("situacao").asText());
            assertEquals("CAMPO_EXISTE_E_NAO_FOI_LIDO", autorizacao.get("decisao").get("motivo").asText());
            assertFalse(autorizacao.has("valor"));
            assertFalse(autorizacao.has("origem"));
            assertTrue(autorizacao.get("orientacao").asText().contains("arquivo mais nítido"));
        }
    }

    @Test
    void decidirDeNovoSubstituiERepetirAMesmaNaoMudaNada() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            long pedido = Long.parseLong(p.pedido());
            JsonNode inicial = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();
            // sem decisao: atualizadaEm e o instante da leitura
            c.cenario.relogio.avancar(Duration.ofSeconds(10));

            JsonNode primeira = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).json();
            String instante1 = campo(documento(primeira, p.encaminhamento()), "NOME").get("decisao")
                    .get("decididaEm").asText();
            assertFalse(instante1.equals(inicial.get("atualizadaEm").asText()), "atualizadaEm avanca com a decisao");
            assertEquals(instante1, primeira.get("atualizadaEm").asText());
            assertEquals(1, c.cenario.conferencias.gravacoes());

            // a mesma decisao de novo: no-op (mesmo instante, nenhuma gravacao nova)
            c.cenario.relogio.avancar(Duration.ofSeconds(10));
            JsonNode repetida = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).json();
            assertEquals(primeira, repetida);
            assertEquals(1, c.cenario.conferencias.gravacoes());

            // decisao diferente no mesmo campo: substitui (continua uma so) e o instante avanca
            c.cenario.relogio.avancar(Duration.ofSeconds(10));
            JsonNode trocada = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO"))).json();
            JsonNode nome = campo(documento(trocada, p.encaminhamento()), "NOME");
            assertEquals("DIVERGENTE", nome.get("decisao").get("tipo").asText());
            assertFalse(instante1.equals(nome.get("decisao").get("decididaEm").asText()));
            assertEquals(nome.get("decisao").get("decididaEm").asText(), trocada.get("atualizadaEm").asText());
            assertEquals(1, c.cenario.conferencias.total(), "uma decisao vigente por campo");
            assertEquals(2, c.cenario.conferencias.gravacoes());
            assertEquals(1, c.cenario.conferencias.decisoesDa(pedido, Long.parseLong(p.extracao())).size());
        }
    }

    @Test
    void loteComVariasDecisoesRegistraTodas() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(), CenarioConferencia.confirmarEncaminhamento(
                    p.encaminhamento()));

            assertEquals(200, r.status(), r.corpo());
            assertEquals(5, c.cenario.conferencias.total());
            assertEquals(2, r.json().get("resumo").get("camposSemDecisao").asInt(), "so os 2 da identificacao");
        }
    }

    @Test
    void loteComUmaDecisaoInvalidaNaoGravaNenhumaDasOutras() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            JsonNode antes = c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json();

            // a 1a e valida; a 2a tenta decidir um campo ilegivel
            Resposta ilegivel = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"), confirmar(p.borrada(), "DATA_NASCIMENTO")));
            assertEquals(422, ilegivel.status(), ilegivel.corpo());
            assertEquals("DECISAO_NAO_APLICAVEL", ilegivel.json().get("codigo").asText());
            assertTrue(ilegivel.corpo().contains("DATA_NASCIMENTO"));

            // a 2a e de outro documento que nao e da leitura
            Resposta fora = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"), confirmar("999999", "NOME")));
            assertEquals(404, fora.status());

            // a 2a tem chave de valor
            Resposta valor = c.decidir(c.authA, p.pedido(), p.extracao(),
                    "{\"decisoes\":[" + confirmar(p.encaminhamento(), "NOME") + ",{\"documentoId\":\""
                            + p.encaminhamento() + "\",\"codigo\":\"DATA_EMISSAO\",\"decisao\":\"CONFIRMADA\","
                            + "\"valor\":\"2026-01-01\"}]}");
            assertEquals(400, valor.status());

            assertEquals(0, c.cenario.conferencias.total());
            assertEquals(0, c.cenario.conferencias.gravacoes());
            assertEquals(antes, c.consultarConferencia(c.authA, p.pedido(), p.extracao()).json());
        }
    }

    @Test
    void recusasDeRegraDeNegocio() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);

            // confirmar campo nao localizado
            Resposta confirmarAusente = c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "AUTORIZACAO_ASSINADA")));
            assertEquals(422, confirmarAusente.status());
            assertEquals("DECISAO_NAO_APLICAVEL", confirmarAusente.json().get("codigo").asText());

            // motivo trocado: "valor diferente" em campo nao localizado e "campo existe" em identificado
            assertEquals(422, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "AUTORIZACAO_ASSINADA", "VALOR_DIFERENTE_DO_DOCUMENTO")))
                    .status());
            assertEquals(422, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "NOME", "CAMPO_EXISTE_E_NAO_FOI_LIDO"))).status());

            // qualquer decisao em campo ilegivel
            assertEquals(422, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.borrada(), "DATA_NASCIMENTO", "CAMPO_EXISTE_E_NAO_FOI_LIDO"))).status());

            // documento que nao foi lido nao tem campos
            assertEquals(422, c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.vazio(), "NOME")))
                    .status());

            // documento fora da leitura: 404 identico ao de pedido inexistente
            Resposta fora = c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar("999999", "NOME")));
            Resposta inexistente = c.decidir(c.authA, "999999", p.extracao(), lote(confirmar("999999", "NOME")));
            assertEquals(404, fora.status());
            assertEquals(inexistente.json().get("mensagem"), fora.json().get("mensagem"));
            assertEquals(inexistente.json().get("codigo"), fora.json().get("codigo"));

            assertEquals(0, c.cenario.conferencias.total());
        }
    }

    @Test
    void recusasDeFormatoDoCorpo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String item = confirmar(p.encaminhamento(), "NOME");

            // chave de valor (a paciente nao digita valor)
            Resposta valor = c.decidir(c.authA, p.pedido(), p.extracao(),
                    "{\"decisoes\":[{\"documentoId\":\"" + p.encaminhamento()
                            + "\",\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\",\"valor\":\"Fulana\"}]}");
            assertEquals(400, valor.status());
            assertEquals("REQUISICAO_INVALIDA", valor.json().get("codigo").asText());
            assertFalse(valor.corpo().contains("Fulana"));

            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(),
                    "{\"decisoes\":[{\"documentoId\":\"" + p.encaminhamento()
                            + "\",\"codigo\":\"NOME\",\"decisao\":\"CORRIGIDA\"}]}").status());
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "NOME", "TEXTO_LIVRE"))).status());
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "CPF"))).status());
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(), "{\"decisoes\":[]}").status());
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(), "").status());
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(), "{nao e json").status());
            // acima de 4 KB
            assertEquals(400, c.decidir(c.authA, p.pedido(), p.extracao(),
                    "{\"decisoes\":[" + item + "]}" + " ".repeat(5000)).status());

            // corpo que nao e application/json: 415
            Resposta texto = c.api.enviarBinario("PUT", caminho(p), item.getBytes(StandardCharsets.UTF_8),
                    "text/plain", c.authA);
            assertEquals(415, texto.status());
            assertEquals("TIPO_NAO_SUPORTADO", texto.json().get("codigo").asText());

            assertEquals(0, c.cenario.conferencias.total());
        }
    }

    @Test
    void aLeituraOriginalFicaIdenticaAntesEDepoisDeQualquerDecisao() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            JsonNode antes = c.consultarLeitura(c.authA, p.pedido()).json();

            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    CenarioConferencia.confirmarEncaminhamento(p.encaminhamento())).status());
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(divergir(p.encaminhamento(), "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO"),
                            divergir(p.encaminhamento(), "AUTORIZACAO_ASSINADA", "CAMPO_EXISTE_E_NAO_FOI_LIDO")))
                    .status());

            assertEquals(antes, c.consultarLeitura(c.authA, p.pedido()).json());
            assertEquals(1, c.cenario.extracoes.total(), "nenhuma leitura nova");
        }
    }

    @Test
    void dadosDeDonoEstadoERevisaoNoCabecalhoSaoIgnoradosENoCorpoSaoRecusados() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            Map<String, String> cabecalhos = new HashMap<>(c.authA);
            cabecalhos.put("X-Paciente-Id", "2");
            cabecalhos.put("X-Estado", "SUBMETIDA_SIMULACAO");
            cabecalhos.put("X-Revisao", "99");

            Resposta ok = c.api.enviar("PUT", caminho(p), lote(confirmar(p.encaminhamento(), "NOME")), cabecalhos);
            assertEquals(200, ok.status(), ok.corpo());

            Resposta corpo = c.api.enviar("PUT", caminho(p), "{\"pacienteId\":2,\"estado\":\"X\",\"revisao\":99,"
                    + "\"decisoes\":[" + confirmar(p.encaminhamento(), "DATA_EMISSAO") + "]}", c.authA);
            assertEquals(400, corpo.status());
            assertEquals(1, c.cenario.conferencias.total());
        }
    }

    @Test
    void pedidoOuLeituraAlheiosDao404AntesDeLerOCorpo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            String corpoInvalido = "{nao e json";

            // paciente B tenta decidir no pedido da A, com corpo invalido e com Content-Type errado: 404, nao 400/415
            Resposta alheio = c.api.enviar("PUT", caminho(p), corpoInvalido, c.authB);
            Resposta alheioTexto = c.api.enviarBinario("PUT", caminho(p), corpoInvalido.getBytes(
                    StandardCharsets.UTF_8), "text/plain", c.authB);
            Resposta leituraInexistente = c.api.enviar("PUT", "/solicitacoes/" + p.pedido()
                    + "/extracoes/999999/conferencia", corpoInvalido, c.authA);
            Resposta pedidoInexistente = c.api.enviar("PUT", "/solicitacoes/999999/extracoes/" + p.extracao()
                    + "/conferencia", corpoInvalido, c.authA);

            for (Resposta r : List.of(alheio, alheioTexto, leituraInexistente, pedidoInexistente)) {
                assertEquals(404, r.status(), r.corpo());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
            }
            // e a paciente B nao decide valido no pedido da A
            assertEquals(404, c.decidir(c.authB, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
            assertEquals(0, c.cenario.conferencias.total());
        }
    }
}
