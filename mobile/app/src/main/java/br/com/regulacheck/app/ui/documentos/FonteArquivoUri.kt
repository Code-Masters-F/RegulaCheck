package br.com.regulacheck.app.ui.documentos

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import br.com.regulacheck.app.api.ArquivoInacessivelException
import br.com.regulacheck.app.api.FonteArquivo
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Arquivo escolhido no seletor do sistema (`content://`, research D3). O seletor concede leitura
 * temporária: sem pedir permissão de armazenamento e sem guardar a permissão. Se ela se perder, a
 * abertura falha com [ArquivoInacessivelException] e o app pede nova escolha.
 */
class FonteArquivoUri private constructor(
    private val resolver: ContentResolver,
    private val uri: Uri,
    override val nome: String,
    override val tipoMidia: String?,
) : FonteArquivo {

    override fun abrir(): InputStream = try {
        resolver.openInputStream(uri) ?: throw ArquivoInacessivelException()
    } catch (e: FileNotFoundException) {
        throw ArquivoInacessivelException(e)
    } catch (e: SecurityException) {
        throw ArquivoInacessivelException(e)
    }

    // Nunca expõe o endereço do arquivo em log ou mensagem.
    override fun toString(): String = "FonteArquivoUri(***)"

    companion object {
        /**
         * Lê nome e tipo do [uri] escolhido.
         *
         * @return `null` se o provedor já não permite consultar o arquivo.
         */
        fun de(resolver: ContentResolver, uri: Uri): FonteArquivoUri? {
            val tipo = try {
                resolver.getType(uri)
            } catch (e: SecurityException) {
                return null
            }
            val nome = try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            } catch (e: SecurityException) {
                return null
            }
            return FonteArquivoUri(resolver, uri, nome?.takeIf { it.isNotBlank() } ?: nomePadrao(tipo), tipo)
        }

        /** Nome com extensão coerente com o tipo, quando o provedor não informa o nome. */
        private fun nomePadrao(tipo: String?): String = when (tipo) {
            "application/pdf" -> "documento.pdf"
            "image/png" -> "documento.png"
            "image/jpeg" -> "documento.jpg"
            else -> "documento"
        }
    }
}
