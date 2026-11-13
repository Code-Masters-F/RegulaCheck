package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SC-003: matriz identidade x rota das rotas da RC-07. Administrador e simulador nunca chegam ao pedido (403,
 * inclusive para id inexistente); anonimo recebe 401; a paciente so alcanca o que e dela.
 */
class MatrizPerfisSolicitacaoTest {

    static final String CHAVE = "chave-ficticia-do-simulador-0123456789";
    static final String CORPO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";

    /** Metodo, caminho e corpo de cada combinacao da RC-07 (o id 1 existe para a paciente A). */
    private record Chamada(String metodo, String caminho, String corpo) {
    }

    private static List<Chamada> chamadas() {
        return List.of(new Chamada("GET", "/protocolos", null), new Chamada("GET", "/solicitacoes", null),
                new Chamada("POST", "/solicitacoes", CORPO), new Chamada("GET", "/solicitacoes/1", null),
                new Chamada("PUT", "/solicitacoes/1", CORPO), new Chamada("GET", "/solicitacoes/999999", null),
                new Chamada("PUT", "/solicitacoes/999999", CORPO));
    }

    @Test
    void anonimoRecebe401EmTodasAsRotas() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            for (Chamada c : chamadas()) {
                Resposta r = api.enviar(c.metodo(), c.caminho(), c.corpo(), Map.of());
                assertEquals(401, r.status(), c.metodo() + " " + c.caminho());
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void administradorESimuladorRecebem403EmTodasAsRotasSemTocarNoPedido() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            api.postJson("/solicitacoes", CORPO, authA);

            for (Chamada c : chamadas()) {
                for (Map<String, String> identidade : List.of(admin, simulador)) {
                    Resposta r = api.enviar(c.metodo(), c.caminho(), c.corpo(), identidade);
                    assertEquals(403, r.status(), c.metodo() + " " + c.caminho());
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals(1, cenario.solicitacoes.total(), "o administrador nao criou nem alterou nada");
            assertEquals(1, cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().revisaoAtual());
        }
    }

    @Test
    void pacienteBNaoAlcancaOPedidoDeANemPorLeituraNemPorAlteracao() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            Map<String, String> authB = bearer(api.entrar(EMAIL_B, SENHA));
            api.postJson("/solicitacoes", CORPO, authA);

            Resposta leitura = api.get("/solicitacoes/1", authB);
            Resposta alteracao = api.enviar("PUT", "/solicitacoes/1", CORPO, authB);
            Resposta inexistente = api.get("/solicitacoes/999999", authB);

            assertEquals(404, leitura.status());
            assertEquals(leitura.corpo(), alteracao.corpo());
            assertEquals(leitura.corpo(), inexistente.corpo());
            assertEquals(0, api.get("/solicitacoes", authB).json().get("itens").size());
            assertEquals(1, api.get("/solicitacoes", authA).json().get("itens").size());
        }
    }
}
