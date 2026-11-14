package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Matriz identidade x rota das analises (FR-001, SC-007): so a dona chega as analises do pedido. */
class MatrizPerfisAnaliseTest {

    private static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    @Test
    void anonimoRecebe401() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            for (String metodo : List.of("POST", "GET")) {
                Resposta r = c.api.enviar(metodo, "/solicitacoes/1/analises", null, Map.of());
                assertEquals(401, r.status(), metodo);
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
        }
    }

    @Test
    void administradorESimuladorRecebem403SemTocarEmNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, bearer(api.entrar(EMAIL_A, SENHA)));
            for (String metodo : List.of("POST", "GET")) {
                for (Map<String, String> identidade : List.of(admin, simulador)) {
                    Resposta r = api.enviar(metodo, "/solicitacoes/1/analises", null, identidade);
                    assertEquals(403, r.status(), metodo);
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals("RASCUNHO", cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().estado().name());
            assertEquals(0, cenario.analises.totalDoPedido(1));
        }
    }

    @Test
    void pedidoDeOutraPacienteEInexistenteTemAMesmaResposta() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            pedir(c, c.authA, p.id());

            Resposta alheioPost = pedir(c, c.authB, p.id());
            Resposta inexistentePost = pedir(c, c.authB, "999999");
            Resposta alheioGet = historico(c, c.authB, p.id());
            Resposta inexistenteGet = historico(c, c.authB, "999999");

            for (Resposta r : List.of(alheioPost, inexistentePost, alheioGet, inexistenteGet)) {
                assertEquals(404, r.status());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
            }
            assertEquals(alheioPost.corpo(), inexistentePost.corpo());
            assertEquals(alheioGet.corpo(), inexistenteGet.corpo());
            assertEquals(1, c.cenario.analises.totalDoPedido(p.numero()));
        }
    }

    @Test
    void caminhoMalFormadoE404SemBancoEMetodoErradoE405() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            for (String caminho : List.of("/solicitacoes/abc/analises", "/solicitacoes/0/analises",
                    "/solicitacoes/1/analises/2", "/solicitacoes/1/analises/")) {
                Resposta r = c.api.enviar("POST", caminho, null, c.authA);
                assertEquals(404, r.status(), caminho);
            }
            Resposta put = c.api.enviar("PUT", "/solicitacoes/1/analises", "{}", c.authA);
            assertEquals(405, put.status());
            assertEquals("GET, POST", String.join(", ",
                    List.of(put.cabecalho("Allow").split(",\\s*")).stream().sorted().toList()));
        }
    }
}
