package br.com.regulacheck.extracao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.CenarioDocumento;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Matriz identidade x rota da conferencia: so a paciente chega aos dados dos documentos. */
class MatrizPerfisConferenciaTest {

    private static final String CHAVE = "chave-ficticia-do-simulador-0123456789";
    private static final String CORPO = CenarioConferencia.lote(CenarioConferencia.confirmar("1", "NOME"));

    private record Chamada(String metodo, String caminho, String corpo) {
    }

    private static List<Chamada> chamadas() {
        String existente = "/solicitacoes/1/extracoes/1/conferencia";
        String inexistente = "/solicitacoes/999999/extracoes/999999/conferencia";
        return List.of(new Chamada("GET", existente, null), new Chamada("PUT", existente, CORPO),
                new Chamada("GET", inexistente, null), new Chamada("PUT", inexistente, CORPO));
    }

    @Test
    void anonimoRecebe401EmTodasAsChamadasSemTocarEmNada() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            for (Chamada chamada : chamadas()) {
                Resposta r = c.api.enviar(chamada.metodo(), chamada.caminho(), chamada.corpo(), Map.of());
                assertEquals(401, r.status(), chamada.metodo() + " " + chamada.caminho());
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
            assertEquals(0, c.cenario.conferencias.total());
        }
    }

    @Test
    void administradorESimuladorRecebem403SemTocarEmNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, authA);

            for (Chamada chamada : chamadas()) {
                for (Map<String, String> identidade : List.of(admin, simulador)) {
                    Resposta r = api.enviar(chamada.metodo(), chamada.caminho(), chamada.corpo(), identidade);
                    assertEquals(403, r.status(), chamada.metodo() + " " + chamada.caminho());
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals(0, cenario.conferencias.total());
            assertEquals(1, cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().revisaoAtual());
        }
    }

    @Test
    void pacienteSemLeituraRecebe404NasDuasRotasEMetodosNaoPrevistosRecebem405ComAllow() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, authA);

            for (Chamada chamada : chamadas()) {
                Resposta r = api.enviar(chamada.metodo(), chamada.caminho(), chamada.corpo(), authA);
                assertEquals(404, r.status(), chamada.metodo() + " " + chamada.caminho());
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText());
            }
            for (String metodo : new String[] {"POST", "DELETE", "PATCH"}) {
                Resposta r = api.enviar(metodo, "/solicitacoes/1/extracoes/1/conferencia", null, authA);
                assertEquals(405, r.status(), metodo);
                assertEquals(Set.of("GET", "PUT"), Set.of(r.cabecalho("Allow").split(",\\s*")), metodo);
            }
            // formas de caminho fora do contrato continuam 404
            for (String caminho : new String[] {"/solicitacoes/1/extracoes/1/conferencia/x",
                    "/solicitacoes/1/conferencia", "/solicitacoes/1/extracoes/conferencia"}) {
                assertEquals(404, api.enviar("GET", caminho, null, authA).status(), caminho);
            }
            assertEquals(0, cenario.conferencias.total());
        }
    }
}
