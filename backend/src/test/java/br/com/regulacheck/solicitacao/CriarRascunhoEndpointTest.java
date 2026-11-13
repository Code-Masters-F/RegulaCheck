package br.com.regulacheck.solicitacao;

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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@code POST /solicitacoes} e reabertura pelo {@code Location}, pela API real com repositorios em memoria. */
class CriarRascunhoEndpointTest {

    static final String CORPO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";
    static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    @Test
    void criaRascunhoEDevolve201ComLocationECorpoDoContrato() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));

            Resposta criada = api.postJson("/solicitacoes", CORPO, auth);

            assertEquals(201, criada.status());
            JsonNode corpo = criada.json();
            assertEquals(List.of("id", "protocoloId", "servico", "estado", "revisao", "criadaEm", "atualizadaEm",
                    "simulacao", "versaoConjuntoDocumental", "documentosDisponiveis", "analise"),
                    ServidorDeTeste.campos(corpo));
            // RC-08: pedido novo -> versao do conjunto = revisao 1, nenhum documento, nenhuma analise.
            assertEquals(1, corpo.get("versaoConjuntoDocumental").asInt());
            assertEquals(0, corpo.get("documentosDisponiveis").asInt());
            assertEquals("NAO_REALIZADA", corpo.get("analise").get("situacao").asText());
            assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", corpo.get("protocoloId").asText());
            assertEquals("RASCUNHO", corpo.get("estado").asText());
            assertEquals(1, corpo.get("revisao").asInt());
            assertTrue(corpo.get("simulacao").asBoolean());
            assertEquals(List.of("localidade", "especialidade", "avisoFicticio"),
                    ServidorDeTeste.campos(corpo.get("servico")));
            assertTrue(corpo.get("servico").get("avisoFicticio").asText().contains("fictício"));
            assertEquals("/solicitacoes/" + corpo.get("id").asText(), criada.cabecalho("Location"));
            assertTrue(!criada.corpo().contains("pacienteId"), "campos internos nao saem pela API");
        }
    }

    @Test
    void oPedidoCriadoPodeSerReabertoComOsMesmosDados() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CORPO, auth);

            Resposta reaberta = api.get(criada.cabecalho("Location"), auth);

            assertEquals(200, reaberta.status());
            assertEquals(criada.json(), reaberta.json());
            assertEquals(1, cenario.solicitacoes.total(), "so um pedido foi gravado");
        }
    }

    @Test
    void donoEEstadoEnviadosPeloClienteSaoIgnorados() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            String forjado = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\",\"pacienteId\":2,"
                    + "\"estado\":\"SUBMETIDA_SIMULACAO\",\"revisao\":9}";

            Resposta criada = api.postJson("/solicitacoes", forjado, auth);

            assertEquals(201, criada.status());
            assertEquals("RASCUNHO", criada.json().get("estado").asText());
            assertEquals(1, criada.json().get("revisao").asInt());
            long id = criada.json().get("id").asLong();
            assertEquals(1L, cenario.solicitacoes.buscarDaPaciente(id, 1).orElseThrow().pacienteId());
            assertTrue(cenario.solicitacoes.buscarDaPaciente(id, 2).isEmpty());
        }
    }

    @Test
    void semSessaoE401() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            assertEquals(401, api.postJson("/solicitacoes", CORPO, Map.of()).status());
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void administradorESimuladorSaoRecusadosSemCriarNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Resposta admin = api.postJson("/solicitacoes", CORPO, bearer(api.entrar(EMAIL_ADMIN, SENHA)));
            Resposta simulador = api.postJson("/solicitacoes", CORPO, Map.of("X-Chave-Simulador", CHAVE));

            assertEquals(403, admin.status());
            assertEquals("ACESSO_NEGADO", admin.json().get("codigo").asText());
            assertEquals(403, simulador.status());
            assertEquals(0, cenario.solicitacoes.total());
        }
    }
}
