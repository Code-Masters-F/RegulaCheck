package br.com.regulacheck.app.ui.documentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.Documento
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.FonteArquivo
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import br.com.regulacheck.app.api.Solicitacao
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pedido e documentos confirmados pela API: só eles aparecem na lista. */
data class DadosDocumentos(val pedido: Solicitacao, val documentos: List<Documento>) {
    val rascunho: Boolean get() = pedido.estado == "RASCUNHO"
    val podeAnexar: Boolean get() = rascunho && documentos.size < MAXIMO_DOCUMENTOS
}

/** Limite de documentos por pedido (RC-08). */
const val MAXIMO_DOCUMENTOS = 3

/** Situação do arquivo escolhido que ainda não foi confirmado (data-model, E2.2/E2.3). */
sealed interface SituacaoEnvio {
    /** Requisição em andamento: a linha mostra "Enviando…" e as ações ficam bloqueadas. */
    data object Enviando : SituacaoEnvio

    /** Rede, tempo ou serviço: "Não enviado" + "Tentar de novo" com o mesmo arquivo e a mesma chave. */
    data class NaoEnviado(val erro: ErroApi) : SituacaoEnvio

    /** A API recusou o arquivo (400/413/415/422): [mensagem] da API; nada foi salvo. */
    data class Recusado(val mensagem: String) : SituacaoEnvio

    /** O arquivo não abre mais, ou a chave foi usada com outro conteúdo: escolher de novo. */
    data object EscolherDeNovo : SituacaoEnvio

    /** O pedido deixou de aceitar documentos (409 `CONFLITO_ESTADO`). */
    data class PedidoFechado(val mensagem: String) : SituacaoEnvio
}

/**
 * Arquivo escolhido e ainda não confirmado.
 *
 * @property chave `X-Chave-Envio`, gerada na escolha e reutilizada em toda tentativa deste arquivo
 * (research D2): repetir devolve 200 com o documento já gravado, sem duplicar.
 * @property fonte `null` quando o arquivo nem pôde ser consultado (só resta escolher de novo).
 */
data class EnvioEmCurso(
    val tipoDocumental: String,
    val nome: String,
    val fonte: FonteArquivo?,
    val chave: String,
    val situacao: SituacaoEnvio,
)

/**
 * E2.2 Seus documentos: consulta o pedido e os documentos e envia o arquivo escolhido
 * (`POST …/documentos`). Um documento só entra na lista depois de 201 ou 200 (#42).
 *
 * @param gerarChave fonte da chave de envio; os testes a fixam.
 */
class DocumentosViewModel(
    private val api: RegulaCheckApi,
    private val pedidoId: String,
    private val gerarChave: () -> String = { UUID.randomUUID().toString().replace("-", "") },
) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoTela<DadosDocumentos>>(EstadoTela.Carregando)
    val estado: StateFlow<EstadoTela<DadosDocumentos>> = estadoMutavel.asStateFlow()

    private val envioMutavel = MutableStateFlow<EnvioEmCurso?>(null)

    /** Arquivo escolhido ainda não confirmado; `null` quando não há nenhum. */
    val envio: StateFlow<EnvioEmCurso?> = envioMutavel.asStateFlow()

    private var carga: Job? = null

    /**
     * Consulta pedido e documentos. Ignorada durante outra consulta ou um envio (o resultado do envio
     * atualiza a lista). Com dados exibidos, atualiza sem voltar ao esqueleto.
     */
    fun carregar() {
        if (carga?.isActive == true || envioMutavel.value?.situacao == SituacaoEnvio.Enviando) return
        if (estadoMutavel.value !is EstadoTela.Sucesso) estadoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch {
            val pedido = when (val resultado = api.obterSolicitacao(pedidoId)) {
                is Resultado.Sucesso -> resultado.valor
                is Resultado.Falha -> {
                    estadoMutavel.value = EstadoTela.Falha(resultado.erro)
                    return@launch
                }
            }
            estadoMutavel.value = when (val resultado = api.listarDocumentos(pedidoId)) {
                is Resultado.Sucesso -> EstadoTela.Sucesso(DadosDocumentos(pedido, resultado.valor.documentos))
                is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
            }
        }
    }

    /**
     * A paciente escolheu [fonte] como [tipoDocumental]: gera a chave e envia. Ignorado durante outro
     * envio (os botões também ficam bloqueados).
     */
    fun arquivoEscolhido(tipoDocumental: String, fonte: FonteArquivo) {
        if (envioMutavel.value?.situacao == SituacaoEnvio.Enviando) return
        envioMutavel.value = EnvioEmCurso(tipoDocumental, fonte.nome, fonte, gerarChave(), SituacaoEnvio.Enviando)
        enviar()
    }

    /** O arquivo escolhido nem pôde ser consultado (permissão do seletor perdida). */
    fun arquivoInacessivel(tipoDocumental: String) {
        if (envioMutavel.value?.situacao == SituacaoEnvio.Enviando) return
        envioMutavel.value = EnvioEmCurso(tipoDocumental, "", null, "", SituacaoEnvio.EscolherDeNovo)
    }

    /** "Tentar de novo" de um envio interrompido: mesmo arquivo, mesma chave (não duplica). */
    fun tentarDeNovo() {
        val atual = envioMutavel.value ?: return
        if (atual.situacao !is SituacaoEnvio.NaoEnviado || atual.fonte == null) return
        envioMutavel.value = atual.copy(situacao = SituacaoEnvio.Enviando)
        enviar()
    }

    private fun enviar() {
        val atual = envioMutavel.value ?: return
        val fonte = atual.fonte ?: return
        viewModelScope.launch {
            when (val resultado = api.enviarDocumento(pedidoId, fonte, atual.tipoDocumental, atual.chave)) {
                is Resultado.Sucesso -> {
                    val documento = resultado.valor.comoDocumento()
                    val dados = (estadoMutavel.value as? EstadoTela.Sucesso)?.dados
                    if (dados != null) {
                        // 200 devolve o documento já gravado: substitui pelo id, nunca duplica a linha.
                        val lista = dados.documentos.filterNot { it.id == documento.id } + documento
                        estadoMutavel.value = EstadoTela.Sucesso(
                            dados.copy(
                                pedido = dados.pedido.copy(
                                    documentosDisponiveis = resultado.valor.documentosDisponiveis ?: lista.size,
                                ),
                                documentos = lista,
                            ),
                        )
                    }
                    envioMutavel.value = null
                }
                is Resultado.Falha -> {
                    envioMutavel.value = if (resultado.erro == ErroApi.SessaoExpirada) {
                        null
                    } else {
                        atual.copy(situacao = situacaoDaFalha(resultado.erro))
                    }
                    if (resultado.erro is ErroApi.Conflito) carregar()
                }
            }
        }
    }
}

/** Converte a falha de um envio na situação mostrada (contrato consumido, linha 4). */
internal fun situacaoDaFalha(erro: ErroApi): SituacaoEnvio = when (erro) {
    is ErroApi.ArquivoRecusado -> SituacaoEnvio.Recusado(
        erro.mensagem?.takeIf { it.isNotBlank() }
            ?: if (erro.codigo == "ARQUIVO_MUITO_GRANDE") Textos.RECUSA_TAMANHO else Textos.RECUSA_FORMATO,
    )
    is ErroApi.RequisicaoInvalida -> SituacaoEnvio.Recusado(
        erro.mensagem?.takeIf { it.isNotBlank() }
            ?: erro.detalhes.firstOrNull()?.mensagem
            ?: if (erro.codigo == "REGRA_DE_NEGOCIO") Textos.RECUSA_LIMITE else Textos.RECUSA_ILEGIVEL,
    )
    is ErroApi.Conflito ->
        if (erro.codigo == "CHAVE_ENVIO_REUTILIZADA") {
            SituacaoEnvio.EscolherDeNovo
        } else {
            SituacaoEnvio.PedidoFechado(erro.mensagem?.takeIf { it.isNotBlank() } ?: Textos.PEDIDO_NAO_RECEBE)
        }
    ErroApi.ArquivoInacessivel -> SituacaoEnvio.EscolherDeNovo
    else -> SituacaoEnvio.NaoEnviado(erro)
}
