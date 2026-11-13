package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-022: criar, trocar, listar, reabrir e falhar nas rotas da RC-07 nao deixa no log e-mail, nome, token nem o
 * corpo recebido; ficam so identificadores tecnicos, etapa e codigos.
 */
class HigieneDeLogSolicitacaoTest {

    private static final String SENTINELA_CORPO = "CORPO-SENTINELA-QUE-NAO-PODE-VAZAR";

    @Test
    void logsDasRotasDeSolicitacaoNaoTrazemDadoPessoalNemCorpo() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CenarioAuth cenario = new CenarioAuth();
        cenario.protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
        String token;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            token = api.entrar(EMAIL_A, SENHA);
            Map<String, String> auth = bearer(token);
            String local = api.postJson("/solicitacoes", "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}",
                    auth).cabecalho("Location");
            api.enviar("PUT", local, "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.2\"}", auth);
            api.get("/solicitacoes", auth);
            api.get(local, auth);
            // Falhas: servico invalido com sentinela, corpo ilegivel com sentinela, pedido que nao existe.
            assertEquals(422, api.postJson("/solicitacoes", "{\"protocoloId\":\"" + SENTINELA_CORPO + "\"}", auth)
                    .status());
            assertEquals(400, api.postJson("/solicitacoes", "{ruim " + SENTINELA_CORPO, auth).status());
            assertEquals(404, api.get("/solicitacoes/999999", auth).status());
        } finally {
            System.setErr(original);
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Recurso nao encontrado"), "controle positivo: a captura de log funcionou");
        for (String proibido : new String[] {SENTINELA_CORPO, EMAIL_A, "Paciente A", token, SENHA}) {
            assertFalse(log.contains(proibido), "vazou no log: " + proibido.substring(0, Math.min(8, proibido.length())));
        }
    }
}
