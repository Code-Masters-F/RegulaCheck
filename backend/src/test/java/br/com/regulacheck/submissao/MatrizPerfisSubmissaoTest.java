package br.com.regulacheck.submissao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static br.com.regulacheck.reenvio.CenarioReenvio.analisado;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.reenvio.CenarioReenvio.Pedido;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matriz identidade x rota da submissao simulada (RC-17, FR-001, FR-009; SC-005; ordem de {@code docs/api/erros.md}):
 * so a dona conclui e consulta; administrador e simulador recebem 403 antes de qualquer consulta; pedido alheio,
 * inexistente ou id ilegivel tem o mesmo 404.
 */
class MatrizPerfisSubmissaoTest {

    private static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    @Test
    void anonimoRecebe401() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            for (String metodo : List.of("POST", "GET")) {
                Resposta r = c.api.enviar(metodo, "/solicitacoes/1/submissoes-simuladas", null, Map.of());
                assertEquals(401, r.status(), metodo);
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
        }
    }

    @Test
    void administradorESimuladorRecebem403SemRegistrarNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, bearer(api.entrar(EMAIL_A, SENHA)));

            for (String metodo : List.of("POST", "GET")) {
                for (Map<String, String> identidade : List.of(admin, simulador)) {
                    Resposta r = api.enviar(metodo, "/solicitacoes/1/submissoes-simuladas", null, identidade);
                    assertEquals(403, r.status(), metodo + " " + r.corpo());
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals("RASCUNHO", cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().estado().name());
            assertEquals(0, cenario.submissoes.total());
        }
    }

    @Test
    void pedidoAlheioInexistenteOuIlegivelTemOMesmo404() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Pedido p = analisado(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
            String referencia = c.api.enviar("POST", "/solicitacoes/999999/submissoes-simuladas", null, c.authB)
                    .corpo();

            for (String metodo : List.of("POST", "GET")) {
                for (String caminho : List.of("/solicitacoes/" + p.id() + "/submissoes-simuladas",
                        "/solicitacoes/999999/submissoes-simuladas", "/solicitacoes/abc/submissoes-simuladas",
                        "/solicitacoes/" + p.id() + "/submissoes-simuladas/1")) {
                    Resposta r = c.api.enviar(metodo, caminho, null, c.authB);
                    assertEquals(404, r.status(), metodo + " " + caminho);
                    assertEquals(referencia, r.corpo(), "corpo identico: " + metodo + " " + caminho);
                }
            }
            assertEquals(0, c.cenario.submissoes.total(), "a paciente B nao submeteu o pedido de A");
            assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", c.cenario.solicitacoes.buscarDaPaciente(p.numero(), 1)
                    .orElseThrow().estado().name());
        }
    }
}
