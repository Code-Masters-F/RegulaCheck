package br.com.regulacheck.app.sessao

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import br.com.regulacheck.app.api.JsonApi
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Armazenamento persistente da [SessaoLocal]. É a fronteira com o Android Keystore, que não existe
 * em teste JVM; os testes usam uma implementação em memória.
 */
interface CofreSessao {
    /** Sessão guardada, ou `null` se não houver ou não puder ser lida (o dado ilegível é apagado). */
    fun ler(): SessaoLocal?

    /** Substitui a sessão guardada. @throws GeneralSecurityException se o Keystore recusar. */
    fun salvar(sessao: SessaoLocal)

    /** Apaga a sessão guardada; não falha se não houver nenhuma. */
    fun apagar()
}

/**
 * [CofreSessao] com AES-256-GCM e chave gerada no Android Keystore (research D4; #40 proíbe
 * `androidx.security:security-crypto`). A chave nunca sai do Keystore; o `SharedPreferences`
 * privado guarda só `Base64(IV + texto cifrado)` do JSON da sessão.
 */
class CofreSessaoKeystore(contexto: Context) : CofreSessao {

    private val preferencias = contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    override fun ler(): SessaoLocal? {
        val guardado = preferencias.getString(CHAVE_SESSAO, null) ?: return null
        return try {
            val bytes = Base64.decode(guardado, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, TAMANHO_IV)
            val cifrado = bytes.copyOfRange(TAMANHO_IV, bytes.size)
            val cifra = Cipher.getInstance(TRANSFORMACAO)
            cifra.init(Cipher.DECRYPT_MODE, obterChave(), GCMParameterSpec(TAMANHO_TAG_BITS, iv))
            JsonApi.decodeFromString(SessaoLocal.serializer(), String(cifra.doFinal(cifrado), Charsets.UTF_8))
        } catch (e: GeneralSecurityException) {
            descartarIlegivel()
        } catch (e: ProviderException) {
            descartarIlegivel()
        } catch (e: IllegalArgumentException) {
            // Base64 ou JSON corrompido (SerializationException herda desta classe).
            descartarIlegivel()
        } catch (e: IndexOutOfBoundsException) {
            descartarIlegivel()
        }
    }

    override fun salvar(sessao: SessaoLocal) {
        val cifra = Cipher.getInstance(TRANSFORMACAO)
        // O IV é gerado pelo próprio Cipher (o Keystore não aceita IV escolhido pelo app).
        cifra.init(Cipher.ENCRYPT_MODE, obterChave())
        val claro = JsonApi.encodeToString(SessaoLocal.serializer(), sessao).toByteArray(Charsets.UTF_8)
        val guardado = Base64.encodeToString(cifra.iv + cifra.doFinal(claro), Base64.NO_WRAP)
        preferencias.edit().putString(CHAVE_SESSAO, guardado).apply()
    }

    override fun apagar() {
        preferencias.edit().remove(CHAVE_SESSAO).apply()
    }

    /** Chave perdida (ex.: bloqueio de tela redefinido) ou dado corrompido: pede login de novo. */
    private fun descartarIlegivel(): SessaoLocal? {
        apagar()
        return null
    }

    private fun obterChave(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVEDOR).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR)
        gerador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(TAMANHO_CHAVE_BITS)
                .build(),
        )
        return gerador.generateKey()
    }

    private companion object {
        const val PROVEDOR = "AndroidKeyStore"
        const val ALIAS = "regulacheck_sessao"
        const val ARQUIVO = "sessao"
        const val CHAVE_SESSAO = "sessao_cifrada"
        const val TRANSFORMACAO = "AES/GCM/NoPadding"
        const val TAMANHO_IV = 12
        const val TAMANHO_TAG_BITS = 128
        const val TAMANHO_CHAVE_BITS = 256
    }
}
