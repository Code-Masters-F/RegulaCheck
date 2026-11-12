package br.com.regulacheck.auth.service;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_INATIVA;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.entity.Sessao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Casos de login do servico de autenticacao, com repositorios em memoria e relogio controlado. */
class AutenticacaoServiceLoginTest {

    private final CenarioAuth cenario = new CenarioAuth();

    @Test
    void loginComSucessoGravaASessaoEDevolveToken() {
        AutenticacaoService.SessaoIniciada iniciada = cenario.servico.login(EMAIL_A, SENHA);

        assertTrue(cenario.gerador.formatoValido(iniciada.token()));
        assertEquals(Instant.parse("2026-11-12T20:30:00Z"), iniciada.expiraEm());
        assertEquals("Paciente A (ficticia)", iniciada.nome());
        assertEquals(Perfil.PACIENTE, iniciada.perfil());
        List<Sessao> gravadas = cenario.sessoes.todas();
        assertEquals(1, gravadas.size());
        assertEquals(1, gravadas.get(0).usuarioId());
        assertEquals(iniciada.expiraEm(), gravadas.get(0).expiraEm());
    }

    @Test
    void soOHashDoTokenFicaGuardado() {
        AutenticacaoService.SessaoIniciada iniciada = cenario.servico.login(EMAIL_A, SENHA);

        List<String> guardados = cenario.sessoes.hashesGuardados();
        assertEquals(List.of(cenario.gerador.hash(iniciada.token())), guardados);
        assertTrue(guardados.stream().noneMatch(g -> g.contains(iniciada.token())));
    }

    @Test
    void emailComMaiusculasEEspacosEAceito() {
        AutenticacaoService.SessaoIniciada iniciada = cenario.servico.login("  Paciente.A@EXEMPLO.invalid ", SENHA);

        assertEquals("Paciente A (ficticia)", iniciada.nome());
    }

    @Test
    void quatroFalhasDeCredencialSaoIndistinguiveisENaoAbremSessao() {
        ExcecaoNegocio senhaErrada = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.login(EMAIL_A, "senha-errada"));
        ExcecaoNegocio emailInexistente = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.login("ninguem@exemplo.invalid", SENHA));
        ExcecaoNegocio contaInativa = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.login(EMAIL_INATIVA, SENHA));
        ExcecaoNegocio senhaLonga = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.login(EMAIL_A, "a".repeat(73)));

        for (ExcecaoNegocio erro : List.of(senhaErrada, emailInexistente, contaInativa, senhaLonga)) {
            assertEquals(401, erro.status());
            assertEquals("CREDENCIAIS_INVALIDAS", erro.codigo());
            assertEquals(senhaErrada.getMessage(), erro.getMessage());
        }
        assertEquals(0, cenario.sessoes.todas().size());
    }

    @Test
    void emailInexistenteGastaAVerificacaoDeSenhaMesmoAssim() {
        assertThrows(ExcecaoNegocio.class, () -> cenario.servico.login("ninguem@exemplo.invalid", SENHA));

        assertEquals(1, cenario.verificador.contraHashFalso);
        assertEquals(0, cenario.verificador.reais);
    }

    @Test
    void contaInativaTemASenhaVerificadaAntesDeRecusar() {
        assertThrows(ExcecaoNegocio.class, () -> cenario.servico.login(EMAIL_INATIVA, SENHA));

        assertEquals(1, cenario.verificador.reais);
    }

    @Test
    void falhaAoGravarASessaoNaoDevolveToken() {
        cenario.sessoes.falharAoCriar(true);

        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> cenario.servico.login(EMAIL_A, SENHA));

        assertEquals(503, erro.status());
        assertEquals(0, cenario.sessoes.todas().size());
    }

    @Test
    void doisLoginsSeguidosGeramSessoesIndependentes() {
        AutenticacaoService.SessaoIniciada primeira = cenario.servico.login(EMAIL_A, SENHA);
        AutenticacaoService.SessaoIniciada segunda = cenario.servico.login(EMAIL_A, SENHA);

        assertNotEquals(primeira.token(), segunda.token());
        assertEquals(2, cenario.sessoes.todas().size());
    }

    @Test
    void senhaNuncaApareceNasMensagensDeErro() {
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.login(EMAIL_A, "segredo-que-nao-pode-vazar"));

        assertTrue(!erro.getMessage().contains("segredo-que-nao-pode-vazar"));
    }
}
