package br.com.regulacheck.app.ui.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.sessao.ResultadoEntrada
import br.com.regulacheck.app.sessao.ServicoSessao
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.AvisoEntrada
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.CabecalhoMarca
import br.com.regulacheck.app.ui.componentes.CampoSenha
import br.com.regulacheck.app.ui.componentes.CampoTexto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Falhas que a tela de login distingue (E1.1). */
enum class FalhaLogin { CREDENCIAIS_INVALIDAS, INDISPONIVEL, SEM_CONEXAO, CONTA_NAO_PACIENTE, DADOS_RECUSADOS }

/**
 * Estado do formulário. A senha só vive aqui, em memória: não vai para `SavedState`, log ou disco.
 * O `toString` nunca a mostra.
 */
data class EstadoLogin(
    val email: String = "",
    val senha: String = "",
    val enviando: Boolean = false,
    val falha: FalhaLogin? = null,
) {
    val podeEnviar: Boolean get() = !enviando && email.isNotBlank() && senha.isNotEmpty()

    override fun toString(): String = "EstadoLogin(enviando=$enviando, falha=$falha)"
}

/**
 * Lógica de E1.1. A navegação para "Meus pedidos" acontece pela mudança de sessão
 * (`GerenciadorSessao`), não por esta classe.
 */
class LoginViewModel(private val servico: ServicoSessao) : ViewModel() {

    private val estadoMutavel = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = estadoMutavel.asStateFlow()

    fun mudarEmail(valor: String) = estadoMutavel.update { it.copy(email = valor, falha = null) }

    fun mudarSenha(valor: String) = estadoMutavel.update { it.copy(senha = valor, falha = null) }

    /**
     * Envia o login. Ignorado durante um envio (toques repetidos). Em 401 mantém o e-mail e limpa a
     * senha; em indisponibilidade ou rede preserva os dois para "Tentar de novo".
     */
    fun entrar() {
        val atual = estadoMutavel.value
        if (!atual.podeEnviar) return
        estadoMutavel.value = atual.copy(enviando = true, falha = null)
        viewModelScope.launch {
            val resultado = servico.entrar(atual.email, atual.senha)
            estadoMutavel.update {
                when (resultado) {
                    is ResultadoEntrada.Entrou -> it.copy(enviando = false, senha = "")
                    ResultadoEntrada.ContaNaoPaciente ->
                        it.copy(enviando = false, senha = "", falha = FalhaLogin.CONTA_NAO_PACIENTE)
                    is ResultadoEntrada.Falhou -> when (resultado.erro) {
                        ErroApi.CredenciaisInvalidas ->
                            it.copy(enviando = false, senha = "", falha = FalhaLogin.CREDENCIAIS_INVALIDAS)
                        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
                            it.copy(enviando = false, falha = FalhaLogin.SEM_CONEXAO)
                        is ErroApi.RequisicaoInvalida, is ErroApi.ArquivoRecusado ->
                            it.copy(enviando = false, falha = FalhaLogin.DADOS_RECUSADOS)
                        else -> it.copy(enviando = false, falha = FalhaLogin.INDISPONIVEL)
                    }
                }
            }
        }
    }
}

/** E1.1 Login (quadros KulXG, ht04w, o1mx3h, r5kJX, V9Q1Ku) com o aviso de E1.4 quando a sessão expirou. */
@Composable
fun LoginTela(viewModel: LoginViewModel, sessaoExpirada: Boolean) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val avisos = remember { SnackbarHostState() }
    val repetir = estado.falha == FalhaLogin.INDISPONIVEL || estado.falha == FalhaLogin.SEM_CONEXAO

    EstruturaTela(avisos = avisos, cabecalho = { CabecalhoMarca() }) {
        if (sessaoExpirada) {
            Spacer(Modifier.height(24.dp))
            BlocoRetorno(TipoRetorno.AVISO, Textos.SESSAO_EXPIRADA_TITULO, Textos.SESSAO_EXPIRADA_TEXTO)
        }
        TituloTela(Textos.LOGIN_TITULO, abaixo = Textos.LOGIN_APOIO)
        Spacer(Modifier.height(24.dp))
        AvisoEntrada()
        Spacer(Modifier.height(24.dp))
        CampoTexto(
            valor = estado.email,
            aoMudar = viewModel::mudarEmail,
            rotulo = Textos.CAMPO_EMAIL,
            habilitado = !estado.enviando,
            teclado = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
        )
        Spacer(Modifier.height(16.dp))
        CampoSenha(
            valor = estado.senha,
            aoMudar = viewModel::mudarSenha,
            visivel = senhaVisivel,
            aoAlternarVisibilidade = { senhaVisivel = !senhaVisivel },
            habilitado = !estado.enviando,
            mensagemErro = when (estado.falha) {
                FalhaLogin.CREDENCIAIS_INVALIDAS -> Textos.CREDENCIAIS_INVALIDAS
                FalhaLogin.DADOS_RECUSADOS -> Textos.LOGIN_DADOS_RECUSADOS
                FalhaLogin.CONTA_NAO_PACIENTE -> Textos.CONTA_NAO_PACIENTE
                else -> null
            },
            teclado = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            acoesTeclado = KeyboardActions(onDone = {
                foco.clearFocus()
                viewModel.entrar()
            }),
        )
        when (estado.falha) {
            FalhaLogin.INDISPONIVEL -> {
                Spacer(Modifier.height(24.dp))
                BlocoRetorno(TipoRetorno.ERRO, Textos.INDISPONIVEL_TITULO, Textos.LOGIN_INDISPONIVEL_TEXTO)
            }
            FalhaLogin.SEM_CONEXAO -> {
                Spacer(Modifier.height(24.dp))
                BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.SEM_CONEXAO_TEXTO)
            }
            else -> Unit
        }
        Spacer(Modifier.height(24.dp))
        BotaoPrimario(
            texto = when {
                estado.enviando -> Textos.BOTAO_ENTRANDO
                repetir -> Textos.TENTAR_DE_NOVO
                else -> Textos.BOTAO_ENTRAR
            },
            aoTocar = {
                foco.clearFocus()
                viewModel.entrar()
            },
            habilitado = estado.podeEnviar,
            carregando = estado.enviando,
        )
    }
}
