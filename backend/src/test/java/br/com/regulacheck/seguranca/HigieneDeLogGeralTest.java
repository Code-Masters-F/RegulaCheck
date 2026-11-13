package br.com.regulacheck.seguranca;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.usuario.entity.Usuario;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * SC-005 / FR-018 / FR-019: numa sessao com sucesso e varias falhas (inclusive erro interno cuja mensagem
 * traz credencial de conexao), nem o log nem as respostas contem senha, token, chave, credencial de
 * conexao ou pilha.
 */
class HigieneDeLogGeralTest {

    private static final String CHAVE = "chave-ficticia-higiene-geral";
    private static final String SENHA_ERRADA = "senha-errada-geral-que-nao-pode-vazar";
    private static final String SENHA_DO_BANCO = "SenhaDoBancoQueNaoPodeVazar";
    private static final String URL_COM_CREDENCIAL = "jdbc:oracle:thin:usuarioficticio/" + SENHA_DO_BANCO
            + "@servidor.invalido:1521:orcl";

    /** Repositorio cuja falha traz uma mensagem com credencial, como alguns drivers fazem. */
    private static final class UsuariosQueFalham implements UsuarioRepository {
        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            throw new IllegalStateException("Nao conectou em " + URL_COM_CREDENCIAL + " password=" + SENHA_DO_BANCO);
        }

        @Override
        public Optional<Usuario> buscarPorId(long id) {
            return Optional.empty();
        }
    }

    @Test
    void logsERespostasNaoContemSegredoNenhum() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StringBuilder respostas = new StringBuilder();
        CenarioAuth cenario = new CenarioAuth();
        String token;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
            try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
                token = api.entrar(EMAIL_A, SENHA);
                Resposta[] rs = {
                    api.postJson("/auth/login", "{\"email\":\"" + EMAIL_A + "\",\"senha\":\"" + SENHA_ERRADA + "\"}",
                            Map.of()),
                    api.get("/auth/sessao", bearer("token-invalido-que-nao-pode-vazar-0123456789")),
                    api.get("/auth/sessao", Map.of("X-Chave-Simulador", CHAVE)),
                    api.get("/auth/sessao", Map.of("X-Chave-Simulador", "chave-errada-que-nao-pode-vazar")),
                    api.get("/auth/sessao", bearer(token)),
                };
                for (Resposta r : rs) {
                    respostas.append(r.corpo());
                }
            }
            Dependencias comFalha = new Dependencias(new UsuariosQueFalham(), cenario.sessoes, cenario.protocolos,
                    cenario.solicitacoes, cenario.documentos, cenario.extracoes, cenario.conferencias,
                    new br.com.regulacheck.integracao.extracao.LeitorSimulado(), cenario.relogio);
            try (ServidorDeTeste api = ServidorDeTeste.iniciar(comFalha)) {
                Resposta erro = api.postJson("/auth/login",
                        "{\"email\":\"" + EMAIL_A + "\",\"senha\":\"" + SENHA_ERRADA + "\"}", Map.of());
                assertEquals(500, erro.status());
                assertEquals("ERRO_INTERNO", erro.json().get("codigo").asText());
                respostas.append(erro.corpo());
            }
        } finally {
            System.setErr(original);
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        // Controle positivo: a captura funcionou e a falha interna foi registrada (com pilha, sem segredo).
        assertTrue(log.contains("Login resultado=SUCESSO"), "captura de log nao funcionou");
        assertTrue(log.contains("Falha inesperada"), "falha interna deveria ter sido registrada");
        assertTrue(log.contains("IllegalStateException"));

        String tudo = log + "\n" + respostas;
        for (String proibido : new String[] {SENHA, SENHA_ERRADA, token, CHAVE, "chave-errada-que-nao-pode-vazar",
                "token-invalido-que-nao-pode-vazar", SENHA_DO_BANCO, "usuarioficticio/", "$2a$"}) {
            assertFalse(tudo.contains(proibido), "vazou: " + proibido.substring(0, Math.min(8, proibido.length())) + "...");
        }
        // Respostas nunca trazem pilha, SQL ou nomes de classe internos.
        String soRespostas = respostas.toString();
        for (String interno : new String[] {"\tat ", "Exception", "br.com.regulacheck", "jdbc:", "SELECT "}) {
            assertFalse(soRespostas.contains(interno), "resposta expoe detalhe interno: " + interno);
        }
    }
}
