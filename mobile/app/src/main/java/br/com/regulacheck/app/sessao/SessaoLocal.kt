package br.com.regulacheck.app.sessao

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

/** Perfil aceito pelo app (#41: conta que não é de paciente é recusada). */
const val PERFIL_PACIENTE = "PACIENTE"

/**
 * Sessão aberta no aparelho: existe entre o login e o logout, um 401 ou a expiração local.
 * Guardada só cifrada ([CofreSessaoKeystore]); o `toString` nunca mostra o token.
 *
 * @property expiraEm instante ISO-8601 devolvido pelo login.
 */
@Serializable
data class SessaoLocal(
    val token: String,
    val nome: String,
    val perfil: String,
    val expiraEm: String,
) {
    /** Primeira palavra do nome, para a saudação "Olá, {primeiro nome}". */
    val primeiroNome: String get() = nome.trim().substringBefore(' ')

    /**
     * `true` se [expiraEm] já passou em [agora]. Data ilegível não expira localmente: o servidor
     * decide e responde 401 se for o caso.
     */
    fun expirou(agora: Instant): Boolean = try {
        !agora.isBefore(OffsetDateTime.parse(expiraEm).toInstant())
    } catch (e: DateTimeParseException) {
        false
    }

    override fun toString(): String = "SessaoLocal(token=***, perfil=$perfil, expiraEm=$expiraEm)"
}
