package br.com.regulacheck.auth.service;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Validacao de sessao, logout e expiracao no servico de autenticacao. */
class AutenticacaoServiceSessaoTest {

    private final CenarioAuth cenario = new CenarioAuth();

    private static void exigeNaoAutenticado(ExcecaoNegocio erro) {
        assertEquals(401, erro.status());
        assertEquals("NAO_AUTENTICADO", erro.codigo());
    }

    @Test
    void sessaoValidaDevolveIdentidadeEExpiracao() {
        String token = cenario.servico.login(EMAIL_A, SENHA).token();

        SessaoAutenticada sessao = cenario.servico.autenticar(token);

        assertEquals("Paciente A (ficticia)", sessao.nome());
        assertEquals(Perfil.PACIENTE, sessao.perfil());
        assertEquals(1, sessao.usuarioId());
        assertEquals(Instant.parse("2026-11-12T20:30:00Z"), sessao.expiraEm());
    }

    @Test
    void logoutInvalidaOTokenNaHora() {
        String token = cenario.servico.login(EMAIL_A, SENHA).token();
        SessaoAutenticada sessao = cenario.servico.autenticar(token);

        cenario.servico.encerrar(sessao);

        exigeNaoAutenticado(assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(token)));
    }

    @Test
    void tokenAusenteMalformadoDesconhecidoDaoOMesmoErro() {
        ExcecaoNegocio ausente = assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(null));
        ExcecaoNegocio vazio = assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(""));
        ExcecaoNegocio malformado = assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar("lixo"));
        ExcecaoNegocio desconhecido = assertThrows(ExcecaoNegocio.class,
                () -> cenario.servico.autenticar(cenario.gerador.novo()));

        for (ExcecaoNegocio erro : new ExcecaoNegocio[] {ausente, vazio, malformado, desconhecido}) {
            exigeNaoAutenticado(erro);
            assertEquals(ausente.getMessage(), erro.getMessage());
        }
    }

    @Test
    void sessaoExpiradaEhRecusada() {
        String token = cenario.servico.login(EMAIL_A, SENHA).token();

        cenario.relogio.avancar(Duration.ofMinutes(29).plusSeconds(59));
        cenario.servico.autenticar(token); // ainda vale um segundo antes

        cenario.relogio.avancar(Duration.ofSeconds(1));
        exigeNaoAutenticado(assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(token)));
    }

    @Test
    void contaDesativadaDepoisDoLoginPerdeASessao() {
        String token = cenario.servico.login(EMAIL_A, SENHA).token();

        cenario.usuarios.desativar(1);

        exigeNaoAutenticado(assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(token)));
    }

    @Test
    void encerrarUmaSessaoNaoAfetaAsOutrasDaMesmaConta() {
        String primeiro = cenario.servico.login(EMAIL_A, SENHA).token();
        String segundo = cenario.servico.login(EMAIL_A, SENHA).token();
        String deOutraConta = cenario.servico.login(EMAIL_B, SENHA).token();

        cenario.servico.encerrar(cenario.servico.autenticar(primeiro));

        exigeNaoAutenticado(assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(primeiro)));
        assertEquals(1, cenario.servico.autenticar(segundo).usuarioId());
        assertEquals(2, cenario.servico.autenticar(deOutraConta).usuarioId());
    }

    @Test
    void logoutRepetidoNaoQuebraENaoReabreASessao() {
        String token = cenario.servico.login(EMAIL_A, SENHA).token();
        SessaoAutenticada sessao = cenario.servico.autenticar(token);

        cenario.servico.encerrar(sessao);
        cenario.servico.encerrar(sessao);

        exigeNaoAutenticado(assertThrows(ExcecaoNegocio.class, () -> cenario.servico.autenticar(token)));
    }
}
