package br.com.regulacheck.reenvio;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioExtracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matriz identidade x rota da verificacao da versao corrigida (RC-16, FR-001; ordem de {@code docs/api/erros.md}):
 * so a paciente chega a {@code POST /solicitacoes/{id}/reenvio}; administrador e simulador recebem 403 antes de
 * qualquer leitura ou analise.
 */
class MatrizPerfisReenvioTest {

    private static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    @Test
    void anonimoRecebe401() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Resposta r = c.api.enviar("POST", "/solicitacoes/1/reenvio", null, Map.of());

            assertEquals(401, r.status());
            assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
        }
    }

    @Test
    void administradorESimuladorRecebem403SemLerNemAnalisar() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, bearer(api.entrar(EMAIL_A, SENHA)));

            for (Map<String, String> identidade : List.of(admin, simulador)) {
                Resposta r = api.enviar("POST", "/solicitacoes/1/reenvio", null, identidade);
                assertEquals(403, r.status(), r.corpo());
                assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
            }
            assertEquals("RASCUNHO", cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().estado().name());
            assertTrue(cenario.extracoes.buscarMaisRecente(1, 1).isEmpty(), "nada foi lido");
            assertEquals(0, cenario.analises.totalDoPedido(1));
        }
    }
}
