package br.com.regulacheck.auth;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-011 / SC-004: durante login (com sucesso e falha), consulta de sessao, logout e falha de banco,
 * nada que identifique segredo vai para o log ou para as respostas de erro.
 *
 * <p>O {@code slf4j-simple} escreve em {@code System.err} a cada chamada (saida nao armazenada em
 * cache), entao o teste troca {@code System.err} por um buffer enquanto exercita a API. O controle
 * positivo no fim garante que a captura de fato funcionou.
 */
class HigieneDeLogTest {

    private static final String SENHA_ERRADA = "senha-errada-que-nao-pode-vazar";

    @Test
    void logsERespostasNaoContemSenhaHashTokenNemEmail() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String token;
        String hashDoToken;
        StringBuilder respostas = new StringBuilder();
        CenarioAuth cenario = new CenarioAuth();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            token = api.entrar(EMAIL_A, SENHA);
            hashDoToken = cenario.gerador.hash(token);

            Resposta falha1 = api.postJson("/auth/login",
                    "{\"email\":\"" + EMAIL_A + "\",\"senha\":\"" + SENHA_ERRADA + "\"}", Map.of());
            Resposta falha2 = api.postJson("/auth/login",
                    "{\"email\":\"ninguem@exemplo.invalid\",\"senha\":\"" + SENHA_ERRADA + "\"}", Map.of());
            Resposta corpoRuim = api.postJson("/auth/login", "{\"email\":\"" + EMAIL_A + "\" " + SENHA_ERRADA, Map.of());
            Resposta sessao = api.get("/auth/sessao", bearer(token));
            Resposta saida = api.postJson("/auth/logout", "", bearer(token));
            Resposta recusada = api.get("/auth/sessao", bearer(token));
            for (Resposta r : new Resposta[] {falha1, falha2, corpoRuim, sessao, saida, recusada}) {
                respostas.append(r.corpo());
            }
        } finally {
            System.setErr(original);
        }

        // Banco indisponivel tambem nao pode vazar nada (fora da captura acima para simplificar a leitura).
        try (ServidorDeTeste semBanco = ServidorDeTeste.iniciarReal(new Configuracao("127.0.0.1", 0))) {
            Resposta r = semBanco.postJson("/auth/login",
                    "{\"email\":\"" + EMAIL_A + "\",\"senha\":\"" + SENHA_ERRADA + "\"}", Map.of());
            assertEquals(503, r.status());
            respostas.append(r.corpo());
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        // Controle positivo: a captura funcionou e os eventos esperados foram registrados.
        assertTrue(log.contains("Login resultado=SUCESSO"), "captura de log nao funcionou");
        assertTrue(log.contains("Login resultado=FALHA"));
        assertTrue(log.contains("Logout resultado=SUCESSO"));

        String tudo = log + "\n" + respostas;
        for (String proibido : new String[] {SENHA, SENHA_ERRADA, token, hashDoToken, EMAIL_A,
                "ninguem@exemplo.invalid", "$2a$"}) {
            assertFalse(tudo.contains(proibido), "vazou: " + proibido.substring(0, Math.min(6, proibido.length())) + "...");
        }
    }
}
