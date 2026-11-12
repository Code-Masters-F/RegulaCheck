package br.com.regulacheck.auth.autorizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unidades do mecanismo de autorizacao: erros fixos, regra de dona, chave do simulador e configuracao. */
class UnidadesDoMecanismoTest {

    private static final SessaoAutenticada SESSAO =
            new SessaoAutenticada(1, 1, "Paciente A", Perfil.PACIENTE, Instant.parse("2026-11-12T21:00:00Z"));

    @Test
    void errosTemStatusCodigoEMensagemFixos() {
        ExcecaoNegocio negado = Erros.acessoNegado();
        ExcecaoNegocio naoEncontrado = Erros.naoEncontrado();

        assertEquals(403, negado.status());
        assertEquals("ACESSO_NEGADO", negado.codigo());
        assertEquals(404, naoEncontrado.status());
        assertEquals("NAO_ENCONTRADO", naoEncontrado.codigo());
        assertEquals(Erros.acessoNegado().getMessage(), negado.getMessage());
        assertEquals(Erros.naoEncontrado().getMessage(), naoEncontrado.getMessage());
    }

    @Test
    void propriedadeDevolveORecursoPresente() {
        String recurso = "documento";

        assertSame(recurso, Propriedade.exigir(Optional.of(recurso), SESSAO));
    }

    @Test
    void propriedadeVazioViraOMesmoNaoEncontradoParaInexistenteEAlheio() {
        // A busca filtrada pela dona devolve vazio nos dois casos; o servico nao distingue um do outro.
        ExcecaoNegocio inexistente = assertThrows(ExcecaoNegocio.class, () -> Propriedade.exigir(Optional.empty(), SESSAO));
        ExcecaoNegocio alheio = assertThrows(ExcecaoNegocio.class, () -> Propriedade.exigir(Optional.empty(), SESSAO));

        assertEquals(404, alheio.status());
        assertEquals(inexistente.codigo(), alheio.codigo());
        assertEquals(inexistente.getMessage(), alheio.getMessage());
    }

    @Test
    void chaveDoSimuladorConfereSoQuandoIgualEConfigurada() {
        ChaveSimulador chave = new ChaveSimulador("chave-ficticia");

        assertTrue(chave.confere("chave-ficticia"));
        assertFalse(chave.confere("chave-ficticia "));
        assertFalse(chave.confere("Chave-ficticia"));
        assertFalse(chave.confere(""));
        assertFalse(chave.confere(null));
        assertFalse(chave.confere("chave"));
    }

    @Test
    void semChaveConfiguradaNadaEAceito() {
        for (String ausente : new String[] {null, "", "   "}) {
            ChaveSimulador chave = new ChaveSimulador(ausente);
            assertFalse(chave.confere(ausente), "ausente=" + ausente);
            assertFalse(chave.confere("qualquer"));
        }
    }

    @Test
    void chaveNuncaApareceEmToString() {
        assertFalse(new ChaveSimulador("chave-ficticia").toString().contains("chave-ficticia"));
        Configuracao c = Configuracao.deMapa(Map.of("IOT_API_KEY", "chave-ficticia-na-config"));
        assertFalse(c.toString().contains("chave-ficticia-na-config"), c.toString());
        assertTrue(c.toString().contains("informada"));
    }

    @Test
    void configuracaoLeAChaveDoSimuladorEIgnoraBrancos() {
        assertEquals("abc", Configuracao.deMapa(Map.of("IOT_API_KEY", "  abc ")).chaveSimulador());
        assertEquals(null, Configuracao.deMapa(Map.of()).chaveSimulador());
        assertEquals(null, new Configuracao("127.0.0.1", 0).chaveSimulador());
    }
}
