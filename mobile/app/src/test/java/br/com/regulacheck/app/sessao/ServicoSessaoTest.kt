package br.com.regulacheck.app.sessao

import br.com.regulacheck.app.api.ApiClient
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.RegulaCheckApi
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Cofre em memória: o Android Keystore não existe em teste JVM. */
private class CofreEmMemoria(var guardada: SessaoLocal? = null) : CofreSessao {
    override fun ler() = guardada
    override fun salvar(sessao: SessaoLocal) { guardada = sessao }
    override fun apagar() { guardada = null }
}

/** Entrar, recusar conta não paciente, sair e 401 — sessão local x servidor (#41). */
class ServicoSessaoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var cofre: CofreEmMemoria
    private lateinit var gerenciador: GerenciadorSessao
    private lateinit var escopo: CoroutineScope
    private lateinit var api: RegulaCheckApi
    private lateinit var servico: ServicoSessao

    private val agora = Instant.parse("2026-11-17T12:00:00Z")
    private val sessaoValida = SessaoLocal("token-paciente", "Paciente A (fictícia)", PERFIL_PACIENTE, "2026-11-17T12:30:00Z")

    private fun montar(guardada: SessaoLocal? = null) {
        cofre = CofreEmMemoria(guardada)
        gerenciador = GerenciadorSessao(cofre) { agora }
        api = RegulaCheckApi(
            ApiClient(
                urlBase = servidor.url("/").toString().trimEnd('/'),
                provedorToken = gerenciador::tokenAtual,
                aoSessaoExpirar = { gerenciador.encerrar(MotivoEncerramento.EXPIROU) },
            ),
        )
        servico = ServicoSessao(api, gerenciador, escopo)
    }

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun encerrar() {
        escopo.cancel()
        servidor.shutdown()
    }

    private fun loginComPerfil(perfil: String) = MockResponse().setBody(
        """{"token":"token-$perfil","tipo":"Bearer","expiraEm":"2026-11-17T12:30:00Z",
           "usuario":{"nome":"Conta fictícia","perfil":"$perfil"}}""",
    )

    @Test
    fun `paciente entra e a sessao fica guardada`() = runTest {
        montar()
        servidor.enqueue(loginComPerfil("PACIENTE"))

        val resultado = servico.entrar("  paciente.a@exemplo.invalid ", "senha")

        assertTrue(resultado is ResultadoEntrada.Entrou)
        assertEquals("token-PACIENTE", cofre.guardada?.token)
        assertEquals("token-PACIENTE", gerenciador.tokenAtual())
        assertTrue(servidor.takeRequest().body.readUtf8().contains("\"email\":\"paciente.a@exemplo.invalid\""))
    }

    @Test
    fun `conta que nao e de paciente e recusada e encerrada no servidor`() = runTest {
        montar()
        servidor.enqueue(loginComPerfil("ADMINISTRADOR"))
        servidor.enqueue(MockResponse().setResponseCode(204))

        val resultado = servico.entrar("admin@exemplo.invalid", "senha")

        assertEquals(ResultadoEntrada.ContaNaoPaciente, resultado)
        assertNull(cofre.guardada)
        assertNull(gerenciador.tokenAtual())
        servidor.takeRequest()
        val logout = servidor.takeRequest()
        assertEquals("/auth/logout", logout.path)
        assertEquals("Bearer token-ADMINISTRADOR", logout.getHeader("Authorization"))
    }

    @Test
    fun `credencial invalida nao abre sessao`() = runTest {
        montar()
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"codigo":"CREDENCIAIS_INVALIDAS","mensagem":"x"}"""))

        assertEquals(ResultadoEntrada.Falhou(ErroApi.CredenciaisInvalidas), servico.entrar("a@b.invalid", "errada"))
        assertNull(cofre.guardada)
    }

    @Test
    fun `sair apaga a sessao mesmo com o servidor indisponivel`() = runTest {
        montar(sessaoValida)
        servidor.enqueue(MockResponse().setResponseCode(503))

        servico.sair().join()

        assertNull(cofre.guardada)
        assertEquals(EstadoSessao.Encerrada(MotivoEncerramento.SAIU), gerenciador.estado.value)
        assertEquals("Bearer token-paciente", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `sair apaga a sessao mesmo sem rede`() = runTest {
        montar(sessaoValida)
        servidor.shutdown()

        servico.sair().join()

        assertNull(cofre.guardada)
        assertEquals(EstadoSessao.Encerrada(MotivoEncerramento.SAIU), gerenciador.estado.value)
    }

    @Test
    fun `401 em rota privada apaga a sessao como expirada`() = runTest {
        montar(sessaoValida)
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"codigo":"NAO_AUTENTICADO","mensagem":"x"}"""))

        api.listarSolicitacoes()

        assertNull(cofre.guardada)
        assertEquals(EstadoSessao.Encerrada(MotivoEncerramento.EXPIROU), gerenciador.estado.value)
    }

    @Test
    fun `sessao guardada ja vencida nao e reaproveitada`() {
        montar(sessaoValida.copy(expiraEm = "2026-11-17T11:59:59Z"))

        assertEquals(EstadoSessao.Encerrada(MotivoEncerramento.EXPIROU), gerenciador.estado.value)
        assertNull(cofre.guardada)
    }

    @Test
    fun `sessao guardada valida abre direto`() {
        montar(sessaoValida)

        assertEquals(EstadoSessao.Ativa(sessaoValida), gerenciador.estado.value)
        assertEquals("Paciente", sessaoValida.primeiroNome)
    }
}
