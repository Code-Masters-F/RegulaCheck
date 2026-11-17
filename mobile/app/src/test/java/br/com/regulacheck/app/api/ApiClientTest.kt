package br.com.regulacheck.app.api

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Mapeamentos de erro e garantias de rede exigidos pela RC-18 (#41), contra um servidor HTTP local
 * (MockWebServer). Corpos de erro seguem `docs/api/erros.md`.
 */
class ApiClientTest {

    private lateinit var servidor: MockWebServer
    private var expiracoes = 0
    private var token: String? = "token-de-teste"

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        expiracoes = 0
    }

    @After
    fun encerrar() {
        servidor.shutdown()
    }

    private fun api(http: okhttp3.OkHttpClient = ApiClient.criarHttpPadrao()) = RegulaCheckApi(cliente(http))

    private fun cliente(http: okhttp3.OkHttpClient = ApiClient.criarHttpPadrao()) = ApiClient(
        urlBase = servidor.url("/").toString().trimEnd('/'),
        provedorToken = { token },
        aoSessaoExpirar = { expiracoes++ },
        http = http,
    )

    private fun erro(status: Int, codigo: String, mensagem: String = "Mensagem.") =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
            .setBody("""{"codigo":"$codigo","mensagem":"$mensagem"}""")

    private fun <T> falha(resultado: Resultado<T>): ErroApi {
        assertTrue("esperava falha, veio $resultado", resultado is Resultado.Falha)
        return (resultado as Resultado.Falha).erro
    }

    @Test
    fun `login envia JSON sem token e devolve a sessao`() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """{"token":"abc","tipo":"Bearer","expiraEm":"2026-11-17T22:30:00Z",
                   "usuario":{"nome":"Paciente A (fictícia)","perfil":"PACIENTE"},"campoNovo":1}""",
            ),
        )

        val resultado = api().entrar("paciente.a@exemplo.invalid", "senha-de-teste")

        val login = (resultado as Resultado.Sucesso).valor
        assertEquals("abc", login.token)
        assertEquals("PACIENTE", login.usuario.perfil)
        val requisicao = servidor.takeRequest()
        assertEquals("POST", requisicao.method)
        assertEquals("/auth/login", requisicao.path)
        assertNull(requisicao.getHeader("Authorization"))
        assertEquals("application/json", requisicao.getHeader("Content-Type"))
        assertTrue(requisicao.body.readUtf8().contains("\"email\":\"paciente.a@exemplo.invalid\""))
    }

    @Test
    fun `400 vira requisicao invalida com detalhes`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"codigo":"REQUISICAO_INVALIDA","mensagem":"Corpo inválido.",
                   "detalhes":[{"campo":"email","mensagem":"Obrigatório"}]}""",
            ),
        )

        val erro = falha(api().entrar("", "x")) as ErroApi.RequisicaoInvalida

        assertEquals("REQUISICAO_INVALIDA", erro.codigo)
        assertEquals(listOf(DetalheErro("email", "Obrigatório")), erro.detalhes)
    }

    @Test
    fun `401 no login e credencial invalida e nao encerra sessao`() = runTest {
        servidor.enqueue(erro(401, "CREDENCIAIS_INVALIDAS"))

        assertEquals(ErroApi.CredenciaisInvalidas, falha(api().entrar("a@b.invalid", "errada")))
        assertEquals(0, expiracoes)
    }

    @Test
    fun `401 em rota privada e sessao expirada e chama o callback`() = runTest {
        servidor.enqueue(erro(401, "NAO_AUTENTICADO"))

        assertEquals(ErroApi.SessaoExpirada, falha(api().listarSolicitacoes()))
        assertEquals(1, expiracoes)
    }

    @Test
    fun `401 no logout nao chama o callback`() = runTest {
        servidor.enqueue(erro(401, "NAO_AUTENTICADO"))

        assertEquals(ErroApi.SessaoExpirada, falha(api().sair("token-antigo")))
        assertEquals(0, expiracoes)
        assertEquals("Bearer token-antigo", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `403 e sem permissao e nao encerra sessao`() = runTest {
        servidor.enqueue(erro(403, "ACESSO_NEGADO"))

        assertEquals(ErroApi.SemPermissao, falha(api().listarSolicitacoes()))
        assertEquals(0, expiracoes)
    }

    @Test
    fun `404 e nao encontrado`() = runTest {
        servidor.enqueue(erro(404, "NAO_ENCONTRADO"))

        assertEquals(ErroApi.NaoEncontrado, falha(api().obterSolicitacao("999")))
        assertEquals("/solicitacoes/999", servidor.takeRequest().path)
    }

    @Test
    fun `409 e conflito com Retry-After em segundos`() = runTest {
        servidor.enqueue(erro(409, "ANALISE_EM_ANDAMENTO", "Aguarde.").setHeader("Retry-After", "42"))

        val erro = falha(api().obterSolicitacao("1")) as ErroApi.Conflito

        assertEquals("ANALISE_EM_ANDAMENTO", erro.codigo)
        assertEquals("Aguarde.", erro.mensagem)
        assertEquals(42L, erro.retryAfterSegundos)
    }

    @Test
    fun `409 sem Retry-After fica sem prazo`() = runTest {
        servidor.enqueue(erro(409, "CONFLITO_ESTADO"))

        assertNull((falha(api().obterSolicitacao("1")) as ErroApi.Conflito).retryAfterSegundos)
    }

    @Test
    fun `413 e 415 sao arquivo recusado`() = runTest {
        servidor.enqueue(erro(413, "ARQUIVO_MUITO_GRANDE", "O arquivo excede 5 MB."))
        servidor.enqueue(erro(415, "TIPO_NAO_SUPORTADO"))

        val muitoGrande = falha(api().listarSolicitacoes()) as ErroApi.ArquivoRecusado
        val tipo = falha(api().listarSolicitacoes()) as ErroApi.ArquivoRecusado

        assertEquals("O arquivo excede 5 MB.", muitoGrande.mensagem)
        assertEquals("TIPO_NAO_SUPORTADO", tipo.codigo)
    }

    @Test
    fun `503 e indisponivel mesmo sem corpo`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(503))

        assertEquals(ErroApi.Indisponivel(503), falha(api().listarSolicitacoes()))
    }

    @Test
    fun `servidor fora do ar e sem conexao`() = runTest {
        val cliente = api()
        servidor.shutdown()

        assertEquals(ErroApi.SemConexao, falha(cliente.listarSolicitacoes()))
    }

    @Test
    fun `resposta que nao chega e tempo esgotado`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val rapido = ApiClient.criarHttpPadrao().newBuilder().readTimeout(300, TimeUnit.MILLISECONDS).build()

        assertEquals(ErroApi.TempoEsgotado, falha(api(rapido).listarSolicitacoes()))
    }

    @Test
    fun `falha de conexao nao e repetida automaticamente`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(MockResponse().setBody("""{"itens":[]}"""))

        assertEquals(ErroApi.SemConexao, falha(api().listarSolicitacoes()))
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `cada chamada usa conexao nova para nao herdar conexao ociosa fechada`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"itens":[]}"""))
        servidor.enqueue(MockResponse().setBody("""{"itens":[]}"""))
        val cliente = api()

        cliente.listarSolicitacoes()
        cliente.listarSolicitacoes()

        // sequenceNumber 0 = primeira requisição na conexão: a segunda abriu conexão nova.
        assertEquals(0, servidor.takeRequest().sequenceNumber)
        assertEquals(0, servidor.takeRequest().sequenceNumber)
    }

    @Test
    fun `rota privada envia o token da sessao`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"itens":[],"limite":50}"""))

        api().listarSolicitacoes()

        assertEquals("Bearer token-de-teste", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `JSON fora do contrato vira resposta inesperada`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"itens":[{"id":"1"}]}"""))

        assertEquals(ErroApi.RespostaInesperada(200), falha(api().listarSolicitacoes()))
    }

    @Test
    fun `estado desconhecido chega como texto sem falhar`() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """{"itens":[{"id":"7","servico":{"localidade":"L","especialidade":"E"},
                   "estado":"ESTADO_FUTURO","revisao":1,"criadaEm":"2026-11-12T13:00:00Z",
                   "atualizadaEm":"2026-11-12T13:06:00Z","documentosDisponiveis":0}],"limite":50}""",
            ),
        )

        val lista = (api().listarSolicitacoes() as Resultado.Sucesso).valor

        assertEquals("ESTADO_FUTURO", lista.itens.single().estado)
    }

    @Test
    fun `tempos e politica de retentativa seguem a issue 40`() {
        val padrao = ApiClient.criarHttpPadrao()

        assertEquals(10_000, padrao.connectTimeoutMillis)
        assertEquals(30_000, padrao.readTimeoutMillis)
        assertFalse(padrao.retryOnConnectionFailure)
        assertFalse(padrao.followRedirects)
        assertTrue(padrao.interceptors.isEmpty())
        assertEquals(70_000, cliente(padrao).httpLeituraLonga.readTimeoutMillis)
    }

    @Test
    fun `toString de login e sessao nao expoe segredo`() {
        val texto = LoginRequisicao("paciente.a@exemplo.invalid", "senha-secreta").toString() +
            LoginResposta("token-secreto", "2026-11-17T22:30:00Z", UsuarioResumo("Paciente A", "PACIENTE")).toString()

        assertFalse(texto.contains("senha-secreta"))
        assertFalse(texto.contains("token-secreto"))
        assertFalse(texto.contains("paciente.a@"))
    }
}
