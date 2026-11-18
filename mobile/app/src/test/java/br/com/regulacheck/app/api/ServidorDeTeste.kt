package br.com.regulacheck.app.api

import java.io.ByteArrayInputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/*
 * Apoio dos testes da RC-19: cliente apontado para um MockWebServer, respostas de erro no formato
 * de docs/api/erros.md e corpos sintéticos do contrato (docs/api/openapi.yaml, versão com a RC-15).
 */

/** API do app apontada para [servidor], com token fixo de teste. */
fun apiDeTeste(
    servidor: MockWebServer,
    http: OkHttpClient = ApiClient.criarHttpPadrao(),
    aoSessaoExpirar: () -> Unit = {},
): RegulaCheckApi = RegulaCheckApi(
    ApiClient(
        urlBase = servidor.url("/").toString().trimEnd('/'),
        provedorToken = { "token-de-teste" },
        aoSessaoExpirar = aoSessaoExpirar,
        http = http,
    ),
)

/** Resposta JSON com [status]. */
fun json(status: Int, corpo: String): MockResponse =
    MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(corpo)

/** Resposta de erro padronizada. */
fun erroApi(status: Int, codigo: String, mensagem: String = "Mensagem."): MockResponse =
    json(status, """{"codigo":"$codigo","mensagem":"$mensagem"}""")

/** Arquivo em memória; [falhar] simula a permissão do seletor perdida. */
class FonteDeTeste(
    override val nome: String = "encaminhamento-sintetico.pdf",
    override val tipoMidia: String? = "application/pdf",
    private val conteudo: ByteArray = "%PDF-1.4 conteudo sintetico".toByteArray(),
    private val falhar: Boolean = false,
) : FonteArquivo {
    var aberturas = 0
        private set

    override fun abrir(): InputStream {
        aberturas++
        if (falhar) throw SecurityException("permissão perdida")
        return ByteArrayInputStream(conteudo)
    }
}

/** Documento enviado (201/200 de `POST …/documentos`). */
fun documentoEnviado(id: String = "41", disponiveis: Int = 1) = """
    {"id":"$id","solicitacaoId":"12","nomeArquivo":"encaminhamento-sintetico.pdf","tipoDocumental":"ENCAMINHAMENTO",
     "formato":"PDF","tamanhoBytes":188416,"paginas":2,"estadoEnvio":"ENVIADO","revisaoEntrada":2,
     "enviadoEm":"2026-11-17T12:00:00Z","simulacao":true,"versaoConjuntoDocumental":2,"documentosDisponiveis":$disponiveis}
""".trimIndent()

/** Pedido em [estado] com [documentos] disponíveis. */
fun solicitacao(id: String = "12", estado: String = "RASCUNHO", documentos: Int = 0) = """
    {"id":"$id","protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
     "servico":{"localidade":"Santa Aurora (município fictício)","especialidade":"Consulta ambulatorial em cardiologia",
                "avisoFicticio":"Documento acadêmico fictício."},
     "estado":"$estado","revisao":1,"criadaEm":"2026-11-17T12:00:00Z","atualizadaEm":"2026-11-17T12:00:00Z",
     "simulacao":true,"versaoConjuntoDocumental":1,"documentosDisponiveis":$documentos,
     "analise":{"situacao":"NAO_REALIZADA","mensagem":"Ainda não analisado."}}
""".trimIndent()

/** Análise gravada com uma pendência da unidade emissora (RC-R04) e evidência com origem. */
fun analiseGravada(id: String = "31", vigente: Boolean = true, resumo: String? = null) = """
    {"id":"$id","revisao":3,"vigente":$vigente,"executadaEm":"2026-11-17T12:30:00Z",
     "protocolo":{"identificador":"PROTOCOLO-ACADEMICO-FICTICIO","versao":"v0.1"},
     "resultadoGeral":"COM_PENDENCIAS","rotuloResultado":"Com pendências","simulada":true,
     "aviso":"Análise simulada; sem pendências não é aprovação.",
     "documentos":[{"documentoId":"41","nomeArquivo":"encaminhamento-sintetico.pdf","tipoDeclarado":"ENCAMINHAMENTO","disponivel":true}],
     "resultados":[{"codigoRegra":"RC-R01","descricao":"Encaminhamento presente","resultado":"ATENDE"},
                   {"codigoRegra":"RC-R04","descricao":"Motivo do encaminhamento informado","resultado":"PENDENTE"}],
     "pendencias":[{"codigoRegra":"RC-R04","descricaoRegra":"Motivo do encaminhamento informado","tipo":"PENDENTE",
                    "explicacao":"O encaminhamento não traz o motivo.","responsavel":"UNIDADE_EMISSORA",
                    "orientacao":"Solicite à unidade emissora a revisão do encaminhamento.",
                    "evidencias":[{"documentoId":"41","campo":"MOTIVO_ENCAMINHAMENTO","situacao":"NAO_LOCALIZADO",
                                   "origem":{"pagina":1,"trecho":"Motivo: <a> & b"}}]}]
     ${if (resumo != null) ""","resumo":$resumo""" else ""}}
""".trimIndent()

/** Histórico de análises. */
fun historico(
    analises: String = "",
    vigenteId: String? = null,
    estado: String = "COM_PENDENCIAS",
    execucao: String? = null,
) = """
    {"solicitacaoId":"12","estado":"$estado","versaoConjuntoDocumental":3,"limite":50,
     ${if (vigenteId != null) """"vigenteId":"$vigenteId",""" else ""}
     ${if (execucao != null) """"execucao":{"situacao":"$execucao","iniciadaEm":"2026-11-17T12:29:00Z","limiteEm":"2026-11-17T12:30:00Z"},""" else ""}
     "analises":[$analises]}
""".trimIndent()

/** Leitura simulada com um documento lido. */
fun extracao(status: String = "ATUAL") = """
    {"id":"7","situacao":"$status","versaoConjuntoDocumental":2,"simulada":true,
     "aviso":"Leitura simulada com dados sintéticos; não é leitura real dos documentos.","criadaEm":"2026-11-17T12:10:00Z",
     "documentos":[{"documentoId":"41","tipoDocumental":"ENCAMINHAMENTO","situacao":"LIDO",
                    "campos":[{"codigo":"NOME","situacao":"IDENTIFICADO","valor":"Ana"},
                              {"codigo":"MOTIVO_ENCAMINHAMENTO","situacao":"NAO_LOCALIZADO"}]}]}
""".trimIndent()
