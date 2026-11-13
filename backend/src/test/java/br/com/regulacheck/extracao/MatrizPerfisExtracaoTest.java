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
import org.junit.jupiter.api.Test;

/** Matriz identidade x rota da extracao: so a paciente chega aos dados dos documentos. */
class MatrizPerfisExtracaoTest {

    private static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    private record Chamada(String metodo, String caminho) {
    }

    private static List<Chamada> chamadas() {
        return List.of(new Chamada("POST", "/solicitacoes/1/extracoes"), new Chamada("GET", "/solicitacoes/1/extracoes"),
                new Chamada("POST", "/solicitacoes/999999/extracoes"),
                new Chamada("GET", "/solicitacoes/999999/extracoes"));
    }

    @Test
    void anonimoRecebe401() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            for (Chamada chamada : chamadas()) {
                Resposta r = c.api.enviar(chamada.metodo(), chamada.caminho(), null, Map.of());
                assertEquals(401, r.status(), chamada.metodo() + " " + chamada.caminho());
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
            assertEquals(0, c.cenario.extracoes.total());
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
                    Resposta r = api.enviar(chamada.metodo(), chamada.caminho(), null, identidade);
                    assertEquals(403, r.status(), chamada.metodo() + " " + chamada.caminho());
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals(0, cenario.extracoes.total());
            assertEquals(1, cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().revisaoAtual());
        }
    }
}
