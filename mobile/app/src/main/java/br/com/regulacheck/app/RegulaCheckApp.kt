package br.com.regulacheck.app

import android.app.Application
import android.content.Context
import br.com.regulacheck.app.api.ApiClient
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.sessao.CofreSessaoKeystore
import br.com.regulacheck.app.sessao.GerenciadorSessao
import br.com.regulacheck.app.sessao.MotivoEncerramento
import br.com.regulacheck.app.sessao.ServicoSessao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contêiner simples das dependências do app (sem biblioteca de injeção, #40). Uma instância por
 * processo, criada em [RegulaCheckApp].
 */
class Dependencias(contexto: Context) {

    val gerenciadorSessao = GerenciadorSessao(CofreSessaoKeystore(contexto))

    /** Escopo do processo: o logout remoto continua mesmo depois que a tela de origem sai. */
    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val api = RegulaCheckApi(
        ApiClient(
            urlBase = BuildConfig.API_BASE_URL,
            provedorToken = gerenciadorSessao::tokenAtual,
            aoSessaoExpirar = { gerenciadorSessao.encerrar(MotivoEncerramento.EXPIROU) },
        ),
    )

    val servicoSessao = ServicoSessao(api, gerenciadorSessao, escopoApp)
}

/** Aplicação RegulaCheck: cria as [Dependencias] uma única vez. */
class RegulaCheckApp : Application() {

    lateinit var dependencias: Dependencias
        private set

    override fun onCreate() {
        super.onCreate()
        dependencias = Dependencias(this)
    }
}
