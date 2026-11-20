package br.com.regulacheck.app.sessao

import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Resultado de uma tentativa de entrar. */
sealed interface ResultadoEntrada {
    data class Entrou(val sessao: SessaoLocal) : ResultadoEntrada
    /** O servidor aceitou a conta, mas o perfil não é de paciente nem de administrador; nada foi guardado. */
    data object PerfilNaoSuportado : ResultadoEntrada
    data class Falhou(val erro: ErroApi) : ResultadoEntrada
}

/**
 * Casos de uso de entrar e sair (E1.1 e "Sair" de E1.2).
 *
 * @param escopoApp escopo que sobrevive às telas: o pedido de logout ao servidor continua mesmo
 * depois que a navegação descarta a tela que pediu a saída.
 */
class ServicoSessao(
    private val api: RegulaCheckApi,
    private val gerenciador: GerenciadorSessao,
    private val escopoApp: CoroutineScope,
) {

    /**
     * Autentica e abre a sessão de paciente ou de administrador (RC-20; a tela inicial sai do perfil,
     * `telaDaSessao`). Outro perfil é recusado: a sessão aberta no servidor é encerrada com o próprio
     * token (falha ignorada) e nada é guardado no aparelho.
     */
    suspend fun entrar(email: String, senha: String): ResultadoEntrada =
        when (val resposta = api.entrar(email.trim(), senha)) {
            is Resultado.Falha -> ResultadoEntrada.Falhou(resposta.erro)
            is Resultado.Sucesso -> {
                val login = resposta.valor
                if (login.usuario.perfil != PERFIL_PACIENTE && login.usuario.perfil != PERFIL_ADMINISTRADOR) {
                    api.sair(login.token)
                    ResultadoEntrada.PerfilNaoSuportado
                } else {
                    val sessao = SessaoLocal(login.token, login.usuario.nome, login.usuario.perfil, login.expiraEm)
                    gerenciador.abrir(sessao)
                    ResultadoEntrada.Entrou(sessao)
                }
            }
        }

    /**
     * Sai: apaga a sessão local **antes** e independentemente da resposta do servidor (#41), e
     * pede o encerramento no servidor em [escopoApp] com o token capturado.
     *
     * @return o trabalho do logout remoto (os testes aguardam; a tela não precisa).
     */
    fun sair(): Job {
        val token = gerenciador.tokenAtual()
        gerenciador.encerrar(MotivoEncerramento.SAIU)
        return escopoApp.launch { if (token != null) api.sair(token) }
    }
}
