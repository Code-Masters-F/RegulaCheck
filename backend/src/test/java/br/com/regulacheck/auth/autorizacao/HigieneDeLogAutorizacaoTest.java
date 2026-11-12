package br.com.regulacheck.auth.autorizacao;

import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.CHAVE;
import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.caminho;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-017 / FR-021 / SC-006: as recusas 401, 403 e 404 e o uso da chave do simulador deixam no log so
 * identificadores tecnicos; nenhum token, chave, senha ou conteudo de recurso aparece no log nem nas
 * respostas. Mesma tecnica do {@code HigieneDeLogTest} da RC-04 (captura de {@code System.err}).
 */
class HigieneDeLogAutorizacaoTest {

    @Test
    void recusasNaoVazamTokenChaveSenhaNemConteudo() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StringBuilder respostas = new StringBuilder();
        String tokenA;
        String tokenAdmin;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            tokenA = amb.tokenA;
            tokenAdmin = amb.tokenAdmin;
            Resposta[] geradas = {
                amb.api.get("/auth/sessao", Map.of()),
                amb.api.get("/auth/sessao", Map.of("Authorization", "Bearer invalido-invalido-invalido-invalido-123")),
                amb.chamar("GET", "/teste/admin/indicadores", amb.tokenA, null),
                amb.chamar("GET", caminho(Tipo.SOLICITACAO, 1), amb.tokenAdmin, null),
                amb.chamar("GET", caminho(Tipo.DOCUMENTO, 2), amb.tokenA, null),
                amb.api.get("/teste/solicitacoes/101", Map.of(ChaveSimulador.CABECALHO, CHAVE)),
                amb.api.postJson("/teste/telemetria/901", "{}", Map.of(ChaveSimulador.CABECALHO, "chave-errada-no-teste")),
                amb.chamar("POST", "/teste/telemetria/901", amb.tokenA, "{}"),
            };
            for (Resposta r : geradas) {
                respostas.append(r.corpo());
            }
        } finally {
            System.setErr(original);
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        // Controle positivo: a captura funcionou e as recusas foram registradas.
        assertTrue(log.contains("codigo=NAO_AUTENTICADO"), "captura de log nao funcionou");
        assertTrue(log.contains("codigo=ACESSO_NEGADO"));
        assertTrue(log.contains("codigo=NAO_ENCONTRADO"));

        String tudo = log + "\n" + respostas;
        for (String proibido : new String[] {CHAVE, "chave-errada-no-teste", tokenA, tokenAdmin, CenarioAuth.SENHA,
                CenarioAuth.EMAIL_A, CenarioAuth.EMAIL_ADMIN, "invalido-invalido", "$2a$"}) {
            assertFalse(tudo.contains(proibido), "vazou: " + proibido.substring(0, Math.min(6, proibido.length())) + "...");
        }
    }
}
