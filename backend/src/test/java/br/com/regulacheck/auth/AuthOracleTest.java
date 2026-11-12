package br.com.regulacheck.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.repository.SessaoRepositoryJdbc;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.auth.service.VerificadorSenha;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.usuario.repository.UsuarioRepositoryJdbc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao contra o <strong>Oracle real</strong> da faculdade (opt-in).
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO.
 * Pre-requisito: scripts 01, 02 e 05 executados na conta. Usa as tres contas ficticias da carga
 * (senha de demonstracao fictícia de {@code database/scripts/README.md}) e remove ao final
 * <em>somente</em> as sessoes que criou, identificadas pelo hash do token.
 */
class AuthOracleTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";
    private static final String[] TABELAS_RC03 = {"RC_USUARIO", "RC_PROTOCOLO", "RC_REGRA", "RC_SOLICITACAO",
            "RC_REVISAO", "RC_DOCUMENTO", "RC_ANALISE", "RC_RESULTADO_REGRA", "RC_PENDENCIA", "RC_SESSAO_CAPTURA",
            "RC_LEITURA_SENSOR", "RC_SUBMISSAO"};

    private static BancoDados banco;
    private static Configuracao configuracao;
    private static final GeradorToken GERADOR = new GeradorToken();
    private static final Set<String> HASHES_CRIADOS = new HashSet<>();
    private static long linhasAntes;

    @BeforeAll
    static void preparar() throws Exception {
        Configuracao ambiente = Configuracao.deAmbiente();
        assumeTrue("SIM".equals(System.getenv("RC_TESTE_ORACLE")), "RC_TESTE_ORACLE != SIM: teste de Oracle ignorado");
        assumeTrue(ambiente.banco().configurado(), "DB_* nao configuradas: teste de Oracle ignorado");
        configuracao = new Configuracao("127.0.0.1", 0, ambiente.banco(), ambiente.ttlSessaoMinutos());
        banco = BancoDados.criar(ambiente.banco());
        linhasAntes = contarLinhasRc03();
    }

    @AfterAll
    static void limpar() throws Exception {
        if (banco == null) {
            return;
        }
        // Remove apenas as sessoes criadas por este teste (por hash do token); nunca outras linhas.
        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement("DELETE FROM RC_SESSAO_LOGIN WHERE TOKEN_HASH = ?")) {
            for (String hash : HASHES_CRIADOS) {
                p.setString(1, hash);
                p.executeUpdate();
            }
        }
        assertEquals(linhasAntes, contarLinhasRc03(), "as tabelas da RC-03 nao podem mudar");
        banco.fechar();
    }

    private static long contarLinhasRc03() throws Exception {
        long total = 0;
        try (Connection c = banco.conexao()) {
            for (String tabela : TABELAS_RC03) {
                try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM " + tabela);
                        ResultSet r = p.executeQuery()) {
                    r.next();
                    total += r.getLong(1);
                }
            }
        }
        return total;
    }

    private static String registrar(String token) {
        HASHES_CRIADOS.add(GERADOR.hash(token));
        return token;
    }

    private static AutenticacaoService servico(Clock relogio) {
        return new AutenticacaoService(new UsuarioRepositoryJdbc(banco), new SessaoRepositoryJdbc(banco),
                new VerificadorSenha(), GERADOR, relogio, Duration.ofMinutes(30));
    }

    @Test
    void asTresContasDaCargaEntramEOHashNoBancoNaoEOToken() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            for (String[] conta : new String[][] {{"paciente.a@exemplo.invalid", "PACIENTE"},
                    {"paciente.b@exemplo.invalid", "PACIENTE"}, {"admin@exemplo.invalid", "ADMINISTRADOR"}}) {
                Resposta r = api.postJson("/auth/login",
                        "{\"email\":\"" + conta[0] + "\",\"senha\":\"" + SENHA_DEMONSTRACAO + "\"}", Map.of());

                assertEquals(200, r.status(), conta[0]);
                assertEquals(conta[1], r.json().get("usuario").get("perfil").asText());
                String token = registrar(r.json().get("token").asText());
                assertEquals(200, api.get("/auth/sessao", ServidorDeTeste.bearer(token)).status());
            }
        }
    }

    @Test
    void senhaErradaEEmailInexistenteDaoOMesmo401() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            Resposta errada = api.postJson("/auth/login",
                    "{\"email\":\"paciente.a@exemplo.invalid\",\"senha\":\"senha-errada\"}", Map.of());
            Resposta inexistente = api.postJson("/auth/login",
                    "{\"email\":\"ninguem@exemplo.invalid\",\"senha\":\"" + SENHA_DEMONSTRACAO + "\"}", Map.of());

            assertEquals(401, errada.status());
            assertEquals(errada.corpo(), inexistente.corpo());
        }
    }

    @Test
    void logoutInvalidaOTokenNoBancoEOutraSessaoDaMesmaContaContinua() throws Exception {
        try (ServidorDeTeste api = ServidorDeTeste.iniciarReal(configuracao)) {
            String primeiro = registrar(api.entrar("paciente.a@exemplo.invalid", SENHA_DEMONSTRACAO));
            String segundo = registrar(api.entrar("paciente.a@exemplo.invalid", SENHA_DEMONSTRACAO));
            assertNotEquals(primeiro, segundo);

            assertEquals(204, api.postJson("/auth/logout", "", ServidorDeTeste.bearer(primeiro)).status());

            assertEquals(401, api.get("/auth/sessao", ServidorDeTeste.bearer(primeiro)).status());
            assertEquals(401, api.postJson("/auth/logout", "", ServidorDeTeste.bearer(primeiro)).status());
            assertEquals(200, api.get("/auth/sessao", ServidorDeTeste.bearer(segundo)).status());
        }
    }

    @Test
    void sessaoExpiraNoPrazoComOBancoReal() {
        RelogioFixo relogio = new RelogioFixo(Instant.now());
        AutenticacaoService servico = servico(relogio);
        String token = registrar(servico.login("paciente.b@exemplo.invalid", SENHA_DEMONSTRACAO).token());

        relogio.avancar(Duration.ofMinutes(29));
        servico.autenticar(token);

        relogio.avancar(Duration.ofMinutes(2));
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> servico.autenticar(token));
        assertEquals("NAO_AUTENTICADO", erro.codigo());
    }

    @Test
    void reinicioDoServidorNaoRessuscitaSessaoEncerradaNemDerrubaAAberta() {
        Clock relogio = Clock.systemUTC();
        String aberta = registrar(servico(relogio).login("paciente.a@exemplo.invalid", SENHA_DEMONSTRACAO).token());
        String encerrada = registrar(servico(relogio).login("paciente.a@exemplo.invalid", SENHA_DEMONSTRACAO).token());
        AutenticacaoService antes = servico(relogio);
        SessaoAutenticada sessao = antes.autenticar(encerrada);
        antes.encerrar(sessao);

        // "Reinicio": servicos novos sobre o mesmo banco, sem nenhum estado em memoria.
        AutenticacaoService depois = servico(relogio);

        assertEquals("Paciente A (fictícia)", depois.autenticar(aberta).nome());
        assertThrows(ExcecaoNegocio.class, () -> depois.autenticar(encerrada));
    }

    @Test
    void tokenEmClaroNuncaEstaNoBanco() throws Exception {
        String token = registrar(servico(Clock.systemUTC()).login("admin@exemplo.invalid", SENHA_DEMONSTRACAO).token());

        try (Connection c = banco.conexao();
                PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM RC_SESSAO_LOGIN WHERE TOKEN_HASH = ?")) {
            p.setString(1, token);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                assertEquals(0, r.getLong(1), "o token em claro nao pode ser o valor guardado");
            }
            p.setString(1, GERADOR.hash(token));
            try (ResultSet r = p.executeQuery()) {
                r.next();
                assertEquals(1, r.getLong(1));
            }
        }
        assertFalse(HASHES_CRIADOS.contains(token));
    }
}
