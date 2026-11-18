package br.com.regulacheck.app.api

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Como a requisição se autentica; define também o significado de um 401 (research D5). */
sealed interface Autenticacao {
    /** Sem token (login). Um 401 aqui significa credenciais inválidas. */
    data object Nenhuma : Autenticacao

    /** Token da sessão atual. Um 401 apaga a sessão local pelo callback de expiração. */
    data object Sessao : Autenticacao

    /**
     * Token informado pela chamada (logout, ou encerrar a sessão de uma conta recusada). Um 401
     * não dispara o callback: quem chama já está encerrando essa sessão.
     */
    class TokenExplicito(val token: String) : Autenticacao {
        override fun toString(): String = "TokenExplicito(***)"
    }
}

/**
 * Requisição à API.
 *
 * @property segmentos caminho em segmentos já separados (cada um é codificado na URL).
 * @property corpoJson corpo em JSON; `null` envia corpo vazio em POST/PUT.
 * @property leituraLonga usa o tempo de leitura de 70 s (ler documentos e analisar, RC-19).
 * @property corpo corpo já montado (multipart do envio de documento, RC-19); exclui [corpoJson].
 * @property cabecalhos cabeçalhos extras não sensíveis (ex.: `X-Chave-Envio`).
 */
data class RequisicaoApi(
    val metodo: String,
    val segmentos: List<String>,
    val corpoJson: String? = null,
    val autenticacao: Autenticacao = Autenticacao.Sessao,
    val leituraLonga: Boolean = false,
    val corpo: RequestBody? = null,
    val cabecalhos: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = "RequisicaoApi($metodo /${segmentos.joinToString("/")})"
}

/**
 * Cliente HTTP da API do RegulaCheck sobre OkHttp.
 *
 * Garantias exigidas pela #40/#41:
 * - tempos de conexão 10 s e leitura 30 s (70 s em [RequisicaoApi.leituraLonga]);
 * - **nenhuma retentativa automática** (`retryOnConnectionFailure(false)`, sem interceptor de
 *   repetição): repetir é sempre decisão da pessoa ("Tentar de novo");
 * - **nenhum log**: não há interceptor de log; token, senha, cabeçalhos e corpos nunca são
 *   registrados;
 * - todo 401 de rota com [Autenticacao.Sessao] chama [aoSessaoExpirar] antes de devolver
 *   [ErroApi.SessaoExpirada].
 *
 * @param urlBase origem da API, sem caminho (ex.: `http://10.0.2.2:8080`).
 * @param provedorToken token da sessão atual, ou `null` sem sessão.
 * @param aoSessaoExpirar chamado uma vez por 401 de rota privada; deve apagar a sessão local.
 * @param http cliente OkHttp; o padrão é [criarHttpPadrao]. Testes passam tempos menores.
 */
class ApiClient(
    urlBase: String,
    private val provedorToken: () -> String?,
    private val aoSessaoExpirar: () -> Unit,
    private val http: OkHttpClient = criarHttpPadrao(),
    private val json: Json = JsonApi,
) {
    private val base: HttpUrl = urlBase.toHttpUrl()

    /** Mesmo cliente (mesmo pool), com leitura de 70 s; o servidor limita a análise a 1 min. */
    val httpLeituraLonga: OkHttpClient =
        http.newBuilder().readTimeout(TEMPO_LEITURA_LONGA_S, TimeUnit.SECONDS).build()

    /**
     * Executa [requisicao] e converte a resposta.
     *
     * @param decodificar converte o corpo de uma resposta 2xx; uma exceção de formato vira
     * [ErroApi.RespostaInesperada].
     * @return [Resultado.Sucesso] só para 2xx com corpo válido; qualquer outro caso é
     * [Resultado.Falha] com a categoria de [ErroApi].
     */
    suspend fun <T> executar(
        requisicao: RequisicaoApi,
        decodificar: (String) -> T,
    ): Resultado<T> {
        val chamada = (if (requisicao.leituraLonga) httpLeituraLonga else http)
            .newCall(montar(requisicao))
        val resposta = try {
            chamada.aguardar()
        } catch (e: IOException) {
            return Resultado.Falha(mapearFalhaDeRede(e))
        }
        return resposta.use { r ->
            val corpo = try {
                withContext(Dispatchers.IO) { r.body?.string() }
            } catch (e: IOException) {
                return@use Resultado.Falha(mapearFalhaDeRede(e))
            }
            if (r.isSuccessful) {
                try {
                    Resultado.Sucesso(decodificar(corpo.orEmpty()))
                } catch (e: IllegalArgumentException) {
                    // SerializationException herda de IllegalArgumentException: JSON fora do contrato.
                    Resultado.Falha(ErroApi.RespostaInesperada(r.code))
                }
            } else {
                val erro = mapearErroHttp(
                    status = r.code,
                    corpo = corpo,
                    retryAfter = r.header("Retry-After"),
                    autenticacao = requisicao.autenticacao,
                    json = json,
                )
                if (erro == ErroApi.SessaoExpirada && requisicao.autenticacao == Autenticacao.Sessao) {
                    aoSessaoExpirar()
                }
                Resultado.Falha(erro)
            }
        }
    }

    private fun montar(requisicao: RequisicaoApi): Request {
        val url = base.newBuilder().apply { requisicao.segmentos.forEach { addPathSegment(it) } }.build()
        val corpo: RequestBody? = when {
            requisicao.corpo != null -> requisicao.corpo
            requisicao.corpoJson != null ->
                // Bytes, e não String, para o OkHttp não acrescentar "; charset=utf-8" ao tipo.
                requisicao.corpoJson.toByteArray(Charsets.UTF_8).toRequestBody(TIPO_JSON)
            requisicao.metodo == "POST" || requisicao.metodo == "PUT" -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        val token = when (val auth = requisicao.autenticacao) {
            Autenticacao.Nenhuma -> null
            Autenticacao.Sessao -> provedorToken()
            is Autenticacao.TokenExplicito -> auth.token
        }
        return Request.Builder()
            .url(url)
            .method(requisicao.metodo, corpo)
            .header("Accept", "application/json")
            .apply { requisicao.cabecalhos.forEach { (nome, valor) -> header(nome, valor) } }
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
    }

    companion object {
        const val TEMPO_CONEXAO_S = 10L
        const val TEMPO_LEITURA_S = 30L
        const val TEMPO_LEITURA_LONGA_S = 70L
        private val TIPO_JSON = "application/json".toMediaType()

        /**
         * Cliente com os tempos da #40, sem retentativa, sem reaproveitar conexão ociosa e sem
         * seguir redirecionamentos.
         */
        fun criarHttpPadrao(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TEMPO_CONEXAO_S, TimeUnit.SECONDS)
            .readTimeout(TEMPO_LEITURA_S, TimeUnit.SECONDS)
            .writeTimeout(TEMPO_LEITURA_S, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            // Sem retentativa, uma conexão ociosa já fechada pelo servidor (o Jetty fecha após 30 s)
            // faria a próxima consulta falhar como "sem conexão". Visto no emulador em 17/11/2026:
            // abrir um pedido 1 min depois da lista falhava. Cada chamada usa conexão nova.
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            // O backend não redireciona; não seguir evita reenviar o token para outro destino.
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}

/**
 * Converte uma resposta de erro HTTP em [ErroApi] (tabela em
 * `specs/017-app-android-login-pedidos/contracts/api-consumida.md`). Corpo ilegível não impede a
 * classificação pelo status.
 */
internal fun mapearErroHttp(
    status: Int,
    corpo: String?,
    retryAfter: String?,
    autenticacao: Autenticacao,
    json: Json = JsonApi,
): ErroApi {
    val erro = corpo
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.decodeFromString(ErroCorpo.serializer(), it) }.getOrNull() }
    return when (status) {
        400, 422 -> ErroApi.RequisicaoInvalida(erro?.codigo, erro?.mensagem, erro?.detalhes.orEmpty())
        401 -> if (autenticacao == Autenticacao.Nenhuma) ErroApi.CredenciaisInvalidas else ErroApi.SessaoExpirada
        403 -> ErroApi.SemPermissao
        404 -> ErroApi.NaoEncontrado
        409 -> ErroApi.Conflito(
            codigo = erro?.codigo,
            mensagem = erro?.mensagem,
            retryAfterSegundos = retryAfter?.trim()?.toLongOrNull()?.takeIf { it >= 0 },
        )
        413, 415 -> ErroApi.ArquivoRecusado(erro?.codigo, erro?.mensagem)
        in 500..599 -> ErroApi.Indisponivel(status, erro?.codigo)
        else -> ErroApi.RespostaInesperada(status)
    }
}

/**
 * Tempo esgotado (conexão, leitura ou chamada), arquivo escolhido que não pôde ser aberto (envio de
 * documento) ou qualquer outra falha de rede.
 */
internal fun mapearFalhaDeRede(e: IOException): ErroApi = when {
    e is ArquivoInacessivelException || e.cause is ArquivoInacessivelException -> ErroApi.ArquivoInacessivel
    e is SocketTimeoutException || (e is InterruptedIOException && e.message == "timeout") -> ErroApi.TempoEsgotado
    else -> ErroApi.SemConexao
}

/**
 * Executa a chamada de forma assíncrona; cancelar a corrotina cancela a requisição. O `resume` com
 * `onCancellation` fecha a resposta que chegar depois do cancelamento (API marcada experimental).
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.aguardar(): Response = suspendCancellableCoroutine { continuacao ->
    continuacao.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuacao.resume(response) { _ -> response.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            continuacao.resumeWithException(e)
        }
    })
}
