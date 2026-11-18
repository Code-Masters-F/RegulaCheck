package br.com.regulacheck.app.api

import java.io.IOException
import java.io.InputStream
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

/**
 * Arquivo escolhido pela paciente para envio (RC-19). Fronteira entre a API e o Android: a
 * implementação do app lê um `content://` do seletor do sistema; os testes usam bytes em memória.
 *
 * O conteúdo nunca é registrado em log nem guardado pelo app: só é lido enquanto a requisição é
 * escrita.
 */
interface FonteArquivo {
    /** Nome de exibição (com extensão), enviado como `filename`; a API decide o formato pelo conteúdo. */
    val nome: String

    /** Tipo informado pelo provedor do arquivo, ou `null` se desconhecido. */
    val tipoMidia: String?

    /**
     * Abre um fluxo novo do início do arquivo; cada tentativa de envio abre o seu.
     *
     * @throws ArquivoInacessivelException se o arquivo não puder mais ser lido.
     */
    fun abrir(): InputStream
}

/**
 * O arquivo escolhido deixou de estar acessível. É uma [IOException] para que uma falha dentro da
 * escrita da requisição chegue ao cliente como erro de envio — e não como exceção não tratada na
 * thread do OkHttp, que encerraria o processo.
 */
class ArquivoInacessivelException(causa: Throwable? = null) : IOException("Arquivo inacessível", causa)

/**
 * Corpo `multipart/form-data` de `POST /solicitacoes/{id}/documentos`: partes `arquivo` e
 * `tipoDocumental` (`docs/api/openapi.yaml`).
 *
 * O arquivo é lido em streaming na escrita (research D1): não ocupa memória com o arquivo inteiro
 * e cada tentativa relê a [FonteArquivo] desde o início.
 */
fun corpoEnvioDocumento(fonte: FonteArquivo, tipoDocumental: String): MultipartBody =
    MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("arquivo", fonte.nome, CorpoArquivo(fonte))
        .addFormDataPart("tipoDocumental", tipoDocumental)
        .build()

/** Parte do arquivo, escrita a partir de um fluxo aberto na hora. */
private class CorpoArquivo(private val fonte: FonteArquivo) : RequestBody() {

    override fun contentType(): MediaType? =
        (fonte.tipoMidia ?: "application/octet-stream").toMediaTypeOrNull()

    // Sem tamanho declarado: o OkHttp envia em partes (chunked). O tamanho informado pelo
    // provedor pode divergir do conteúdo real, e a divergência derrubaria o envio no meio; a API
    // mede o arquivo e recusa acima de 5 MB (413) sem confiar em tamanho declarado.
    override fun contentLength(): Long = -1L

    override fun writeTo(sink: BufferedSink) {
        val fluxo = try {
            fonte.abrir()
        } catch (e: IOException) {
            throw e as? ArquivoInacessivelException ?: ArquivoInacessivelException(e)
        } catch (e: RuntimeException) {
            // SecurityException do ContentResolver quando a permissão do seletor se perdeu.
            throw ArquivoInacessivelException(e)
        }
        fluxo.source().use { sink.writeAll(it) }
    }
}
