package br.com.regulacheck.documentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.reenvio.CenarioReenvio;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RC-28: executa, pelas rotas reais da API (repositorios em memoria, leitor SIMULADO de producao e as amostras
 * sinteticas da raiz de {@code samples/}), o mesmo percurso de {@code docs/api/roteiro-avaliacao.md}, conferindo o
 * status e os campos que o roteiro manda observar em cada passo. Nao usa Oracle; a execucao com o banco real e
 * opt-in e manual (roteiro).
 */
class RoteiroAvaliacaoTest {

    private static final Path ENCAMINHAMENTO = Path.of("..", "samples", "encaminhamento-sintetico.pdf");
    private static final Path IDENTIFICACAO = Path.of("..", "samples", "identificacao-sintetica.png");

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioDocumento.iniciar();
    }

    @AfterEach
    void parar() throws Exception {
        c.close();
    }

    private static void exigir(Resposta r, int status, String passo) {
        assertEquals(status, r.status(), passo + ": " + r.corpo());
    }

    private Map<String, String> entrar(String email) throws Exception {
        Resposta login = c.api.postJson("/auth/login",
                "{\"email\":\"" + email + "\",\"senha\":\"" + CenarioAuth.SENHA + "\"}", Map.of());
        exigir(login, 200, "1 login");
        assertEquals("Bearer", login.json().get("tipo").asText());
        return Map.of("Authorization", "Bearer " + login.json().get("token").asText());
    }

    private Resposta enviar(Map<String, String> auth, String pedido, String tipo, Path arquivo) throws Exception {
        return c.enviar(auth, pedido, tipo, arquivo.getFileName().toString(), Files.readAllBytes(arquivo), null);
    }

    @Test
    void percursoDoRoteiroDoLoginAoComprovanteSimulado() throws Exception {
        Map<String, String> paciente = entrar(CenarioAuth.EMAIL_A);

        Resposta servicos = c.api.get("/protocolos", paciente);
        exigir(servicos, 200, "2 servicos");
        String protocoloId = servicos.json().get("itens").get(0).get("protocoloId").asText();
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", protocoloId);

        Resposta rascunho = c.api.postJson("/solicitacoes", "{\"protocoloId\":\"" + protocoloId + "\"}", paciente);
        exigir(rascunho, 201, "3 rascunho");
        assertEquals("RASCUNHO", rascunho.json().get("estado").asText());
        String pedido = rascunho.json().get("id").asText();
        assertEquals("/solicitacoes/" + pedido, rascunho.cabecalho("Location"));

        Resposta envio = enviar(paciente, pedido, "ENCAMINHAMENTO", ENCAMINHAMENTO);
        exigir(envio, 201, "4 envio");
        assertEquals(1, envio.json().get("documentosDisponiveis").asInt());
        exigir(c.api.get("/solicitacoes/" + pedido + "/documentos", paciente), 200, "4 lista");

        Resposta leitura = c.lerDocumentos(paciente, pedido);
        exigir(leitura, 201, "5 leitura");
        assertTrue(leitura.json().get("simulada").asBoolean());
        String extracao = leitura.json().get("id").asText();

        Resposta conferencia = c.consultarConferencia(paciente, pedido, extracao);
        exigir(conferencia, 200, "6 conferencia");
        assertFalse(conferencia.json().get("liberadoParaSubmissao").asBoolean());
        CenarioReenvio.confirmarTudo(c, pedido, extracao);
        assertEquals("CONFERIDA",
                c.consultarConferencia(paciente, pedido, extracao).json().get("situacao").asText());

        Resposta analise = c.api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, paciente);
        exigir(analise, 201, "7 analise");
        assertEquals("COM_PENDENCIAS", analise.json().get("resultadoGeral").asText());
        assertTrue(codigos(analise.json().get("pendencias")).contains("RC-R03"), "falta a identificacao");

        Resposta historico = c.api.get("/solicitacoes/" + pedido + "/analises", paciente);
        exigir(historico, 200, "8 historico");
        assertEquals(analise.json().get("id").asText(), historico.json().get("vigenteId").asText());
        assertEquals("COM_PENDENCIAS",
                c.api.get("/solicitacoes/" + pedido, paciente).json().get("estado").asText());

        Resposta resumo = c.api.enviar("POST", "/solicitacoes/" + pedido + "/resumo", null, paciente);
        exigir(resumo, 201, "9 resumo");
        assertTrue(resumo.json().get("resumo").get("simulado").asBoolean());

        exigir(enviar(paciente, pedido, "IDENTIFICACAO", IDENTIFICACAO), 201, "10 correcao");

        Resposta parada = CenarioReenvio.reenviar(c, paciente, pedido);
        exigir(parada, 200, "11 reenvio na conferencia");
        assertEquals("CONFERENCIA", parada.json().get("etapa").asText());
        CenarioReenvio.confirmarTudo(c, pedido, parada.json().get("extracaoId").asText());

        Resposta verificada = CenarioReenvio.reenviar(c, paciente, pedido);
        exigir(verificada, 201, "12 reenvio concluido");
        assertEquals("CONCLUIDA", verificada.json().get("etapa").asText());
        JsonNode nova = verificada.json().get("analise");
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", nova.get("resultadoGeral").asText());
        assertTrue(nova.get("comparacao").get("itens").toString().contains("\"RESOLVIDA\""));

        Resposta submissao = c.api.enviar("POST", "/solicitacoes/" + pedido + "/submissoes-simuladas", null,
                paciente);
        exigir(submissao, 201, "13 submissao");
        String referencia = submissao.json().get("referencia").asText();
        assertTrue(referencia.matches("SIM-[0-9]{4}-[0-9]{6,}"), referencia);
        assertEquals("SUBMETIDA_SIMULACAO",
                c.api.get("/solicitacoes/" + pedido, paciente).json().get("estado").asText());

        Resposta comprovante = c.api.get("/solicitacoes/" + pedido + "/submissoes-simuladas", paciente);
        exigir(comprovante, 200, "14 comprovante");
        assertEquals(referencia, comprovante.json().get("referencia").asText());
    }

    @Test
    void exclusaoDoRascunhoDescartavelEIndicadoresDoAdministrador() throws Exception {
        Map<String, String> paciente = entrar(CenarioAuth.EMAIL_A);
        String descartavel = c.criarRascunho(paciente);
        Map<String, String> confirmacao = new HashMap<>(paciente);
        confirmacao.put("X-Confirmar-Exclusao", "EXCLUIR");

        exigir(c.api.enviar("DELETE", "/solicitacoes/" + descartavel, null, confirmacao), 204, "15 exclusao");
        exigir(c.api.get("/solicitacoes/" + descartavel, paciente), 404, "15 depois da exclusao");

        Map<String, String> administrador = entrar(CenarioAuth.EMAIL_ADMIN);
        Resposta indicadores = c.api.get("/admin/indicadores", administrador);
        exigir(indicadores, 200, "16 indicadores");
        assertTrue(indicadores.json().get("dadosSinteticos").asBoolean());
        exigir(c.api.get("/admin/indicadores", paciente), 403, "16 paciente na rota administrativa");
    }

    private static List<String> codigos(JsonNode pendencias) {
        List<String> codigos = new ArrayList<>();
        pendencias.forEach(p -> codigos.add(p.get("codigoRegra").asText()));
        return codigos;
    }
}
