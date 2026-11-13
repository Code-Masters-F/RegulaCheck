package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@code PUT /solicitacoes/{id}} pela API real com repositorios em memoria. */
class TrocarServicoEndpointTest {

    static final String CRIAR = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";
    static final String V02 = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.2\"}";

    private static CenarioAuth cenario() {
        CenarioAuth cenario = new CenarioAuth();
        cenario.protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
        return cenario;
    }

    @Test
    void trocaDeServicoDevolve200ComRevisaoDois() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CRIAR, auth);
            String local = criada.cabecalho("Location");
            cenario.relogio.avancar(Duration.ofMinutes(1));

            Resposta trocada = api.enviar("PUT", local, V02, auth);

            assertEquals(200, trocada.status());
            assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.2", trocada.json().get("protocoloId").asText());
            assertEquals(2, trocada.json().get("revisao").asInt());
            assertEquals("RASCUNHO", trocada.json().get("estado").asText());
            assertEquals(criada.json().get("criadaEm"), trocada.json().get("criadaEm"));
            assertTrue(!trocada.json().get("atualizadaEm").equals(criada.json().get("atualizadaEm")));
            assertEquals(trocada.json(), api.get(local, auth).json(), "a leitura devolve o que foi gravado");
        }
    }

    @Test
    void mesmoServicoDevolve200SemMudarARevisao() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CRIAR, auth);
            cenario.relogio.avancar(Duration.ofMinutes(1));

            Resposta repetida = api.enviar("PUT", criada.cabecalho("Location"), CRIAR, auth);

            assertEquals(200, repetida.status());
            assertEquals(criada.json(), repetida.json());
        }
    }

    @Test
    void foraDeRascunhoE409() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CRIAR, auth);
            cenario.solicitacoes.forcarEstado(criada.json().get("id").asLong(), EstadoSolicitacao.COM_PENDENCIAS);

            Resposta r = api.enviar("PUT", criada.cabecalho("Location"), V02, auth);

            assertEquals(409, r.status());
            assertEquals("CONFLITO_ESTADO", r.json().get("codigo").asText());
        }
    }

    @Test
    void servicoInativoE422ComOCampo() throws Exception {
        CenarioAuth cenario = cenario();
        cenario.protocolos.desativar("PROTOCOLO-ACADEMICO-FICTICIO-v0.2");
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            Resposta criada = api.postJson("/solicitacoes", CRIAR, auth);

            Resposta r = api.enviar("PUT", criada.cabecalho("Location"), V02, auth);

            assertEquals(422, r.status());
            assertEquals("REGRA_DE_NEGOCIO", r.json().get("codigo").asText());
            assertEquals("protocoloId", r.json().get("detalhes").get(0).get("campo").asText());
        }
    }

    @Test
    void donaVemAntesDaValidacaoDoCorpo() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            Map<String, String> authB = bearer(api.entrar(EMAIL_B, SENHA));
            String local = api.postJson("/solicitacoes", CRIAR, authA).cabecalho("Location");

            // Corpo invalido em pedido alheio ou inexistente: 404 (nunca 400, que revelaria que o pedido existe).
            Resposta alheio = api.enviar("PUT", local, "{ruim", authB);
            Resposta inexistente = api.enviar("PUT", "/solicitacoes/999999", "{ruim", authB);
            Resposta alheioValido = api.enviar("PUT", local, V02, authB);
            // O dono, com o mesmo corpo invalido, recebe 400.
            Resposta dono = api.enviar("PUT", local, "{ruim", authA);

            assertEquals(404, alheio.status());
            assertEquals(alheio.corpo(), inexistente.corpo());
            assertEquals(alheio.corpo(), alheioValido.corpo());
            assertEquals(400, dono.status());
            assertEquals(1, cenario.solicitacoes.revisoesDe(Long.parseLong(local.substring("/solicitacoes/".length()))).size(),
                    "nada foi alterado");
        }
    }

    @Test
    void semSessaoAdministradorEMetodosNaoPrevistos() throws Exception {
        CenarioAuth cenario = cenario();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));
            String local = api.postJson("/solicitacoes", CRIAR, auth).cabecalho("Location");

            assertEquals(401, api.enviar("PUT", local, V02, Map.of()).status());
            assertEquals(403, api.enviar("PUT", local, V02, bearer(api.entrar(EMAIL_ADMIN, SENHA))).status());
            // RC-09: DELETE no pedido passou a existir (exclusao com confirmacao); PATCH segue sem rota.
            Resposta patch = api.enviar("PATCH", local, null, auth);
            assertEquals(405, patch.status());
            assertEquals("METODO_NAO_PERMITIDO", patch.json().get("codigo").asText());
        }
    }
}
