package br.com.regulacheck.app.ui.novopedido

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.Protocolo
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.AvisoProposta
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.BotaoSeta
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.OpcaoEscolha
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.TiposRc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Situação da criação do rascunho (E2.1). */
sealed interface EstadoCriacao {
    data object Ociosa : EstadoCriacao
    data object Criando : EstadoCriacao
    data class Criado(val pedidoId: String) : EstadoCriacao
    data class Falhou(val erro: ErroApi) : EstadoCriacao
}

/**
 * E2.1 Novo pedido: consulta os serviços (`GET /protocolos`) e cria o rascunho com o primeiro
 * (`POST /solicitacoes`). A paciente só confirma o serviço demonstrativo (#42).
 */
class NovoPedidoViewModel(private val api: RegulaCheckApi) : ViewModel() {

    private val servicoMutavel = MutableStateFlow<EstadoTela<Protocolo>>(EstadoTela.Carregando)

    /** Primeiro serviço ativo; [EstadoTela.Vazio] quando a lista vem vazia. */
    val servico: StateFlow<EstadoTela<Protocolo>> = servicoMutavel.asStateFlow()

    private val criacaoMutavel = MutableStateFlow<EstadoCriacao>(EstadoCriacao.Ociosa)
    val criacao: StateFlow<EstadoCriacao> = criacaoMutavel.asStateFlow()

    private var carga: Job? = null

    init {
        carregar()
    }

    /** Consulta os serviços; ignorada com outra consulta ou uma criação em andamento. */
    fun carregar() {
        if (carga?.isActive == true || criacaoMutavel.value == EstadoCriacao.Criando) return
        criacaoMutavel.value = EstadoCriacao.Ociosa
        servicoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch {
            servicoMutavel.value = when (val resultado = api.listarProtocolos()) {
                is Resultado.Sucesso -> resultado.valor.itens.firstOrNull()?.let { EstadoTela.Sucesso(it) } ?: EstadoTela.Vazio
                is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
            }
        }
    }

    /**
     * Cria o rascunho do serviço exibido. Toques durante a criação (ou depois de criado) são
     * ignorados: a API não deduplica a criação (research D7).
     */
    fun criar() {
        val protocolo = (servicoMutavel.value as? EstadoTela.Sucesso)?.dados ?: return
        val atual = criacaoMutavel.value
        if (atual == EstadoCriacao.Criando || atual is EstadoCriacao.Criado) return
        criacaoMutavel.value = EstadoCriacao.Criando
        viewModelScope.launch {
            criacaoMutavel.value = when (val resultado = api.criarSolicitacao(protocolo.protocoloId)) {
                is Resultado.Sucesso -> EstadoCriacao.Criado(resultado.valor.id)
                is Resultado.Falha -> EstadoCriacao.Falhou(resultado.erro)
            }
        }
    }
}

/**
 * E2.1 Novo pedido (quadros c78REx, gj4eA, Zk97Z, pEOeQ).
 *
 * @param aoCriar recebe o identificador do rascunho criado; quem chama abre os documentos dele.
 */
@Composable
fun NovoPedidoTela(viewModel: NovoPedidoViewModel, aoVoltar: () -> Unit, aoCriar: (String) -> Unit) {
    val servico by viewModel.servico.collectAsStateWithLifecycle()
    val criacao by viewModel.criacao.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(criacao) {
        (criacao as? EstadoCriacao.Criado)?.let { aoCriar(it.pedidoId) }
    }

    EstruturaTela(avisos = avisos, cabecalho = { CabecalhoVoltar(aoVoltar) }) {
        TituloTela(Textos.NOVO_PEDIDO_TITULO, abaixo = Textos.NOVO_PEDIDO_APOIO)
        Spacer(Modifier.height(24.dp))
        when (val atual = servico) {
            EstadoTela.Carregando -> Esqueleto(Textos.SERVICOS_CARREGANDO, linhas = 2)
            EstadoTela.Vazio -> SemServicos(aoVoltar)
            is EstadoTela.Falha -> FalhaServicos(atual.erro, viewModel::carregar)
            is EstadoTela.Sucesso -> ServicoDemonstrativo(atual.dados, criacao, viewModel::criar, viewModel::carregar)
        }
    }
}

@Composable
private fun SemServicos(aoVoltar: () -> Unit) {
    Text(
        Textos.SEM_SERVICOS_TITULO,
        style = TiposRc.Secao,
        color = CoresRc.Texto,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(12.dp))
    Text(Textos.SEM_SERVICOS_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
    Spacer(Modifier.height(16.dp))
    BotaoSeta(Textos.VOLTAR_PARA_PEDIDOS, aoVoltar)
}

@Composable
private fun FalhaServicos(erro: ErroApi, aoTentar: () -> Unit) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.SEM_CONEXAO_TEXTO, aoTentar = aoTentar)
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.SERVICOS_ERRO_TITULO, Textos.DETALHE_INDISPONIVEL_TEXTO, aoTentar = aoTentar)
    }
}

@Composable
private fun ServicoDemonstrativo(
    protocolo: Protocolo,
    criacao: EstadoCriacao,
    aoCriar: () -> Unit,
    aoRecarregar: () -> Unit,
) {
    val criando = criacao == EstadoCriacao.Criando
    // Só um serviço: a paciente confirma; a opção aparece marcada e não muda.
    OpcaoEscolha(
        titulo = protocolo.especialidade,
        apoio = Textos.linhaServico(protocolo.localidade, RotulosApi.versaoProtocolo(protocolo.versao)),
        selecionada = true,
        aoEscolher = {},
        habilitada = !criando,
    )
    HorizontalDivider(color = CoresRc.Divisor)
    protocolo.avisoFicticio?.takeIf { it.isNotBlank() }?.let { aviso ->
        Spacer(Modifier.height(8.dp))
        // Texto do backend exibido como texto puro.
        Text(aviso, style = TiposRc.Meta, color = CoresRc.TextoApoio)
    }
    Spacer(Modifier.height(24.dp))
    AvisoProposta(titulo = Textos.PROPOSTA_NOVO_TITULO, texto = Textos.PROPOSTA_NOVO_TEXTO)
    Spacer(Modifier.height(24.dp))
    if (criacao is EstadoCriacao.Falhou) {
        FalhaCriacao(criacao.erro, aoRecarregar)
        if (criacao.erro != ErroApi.SemPermissao && criacao.erro != ErroApi.SessaoExpirada &&
            criacao.erro !is ErroApi.RequisicaoInvalida
        ) {
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.TENTAR_DE_NOVO, aoCriar)
        }
    } else {
        BotaoPrimario(
            if (criando) Textos.CRIANDO else Textos.CRIAR_RASCUNHO,
            aoCriar,
            carregando = criando,
        )
    }
}

@Composable
private fun FalhaCriacao(erro: ErroApi, aoRecarregar: () -> Unit) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        // 422/400: serviço inexistente ou inativo; a lista é consultada de novo antes de criar.
        is ErroApi.RequisicaoInvalida ->
            BlocoRetorno(TipoRetorno.ERRO, Textos.SERVICO_RECUSADO, aoTentar = aoRecarregar)
        // A API cria um pedido por chamada: o rascunho pode existir mesmo sem confirmação.
        ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.AVISO, Textos.CONFIRMACAO_INCERTA_TITULO, Textos.RASCUNHO_PODE_EXISTIR)
        ErroApi.SemConexao -> BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.CRIAR_ERRO_TEXTO)
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.CRIAR_ERRO_TITULO, Textos.CRIAR_ERRO_TEXTO)
    }
}
