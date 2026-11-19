package br.com.regulacheck.app.ui.documentos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.regulacheck.app.api.Documento
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.captura.CondicoesCapturaTela
import br.com.regulacheck.app.ui.captura.CondicoesCapturaViewModel
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoContorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.BotaoSeta
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.MARGEM_TELA
import br.com.regulacheck.app.ui.componentes.OpcaoEscolha
import br.com.regulacheck.app.ui.componentes.SinalTexto
import br.com.regulacheck.app.ui.componentes.SuperficieTarefa
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/** Tipos aceitos pelo seletor do sistema (#42): PDF, JPEG e PNG. */
private val TIPOS_ACEITOS = arrayOf("application/pdf", "image/jpeg", "image/png")

/** "Escolher a foto" da E4.1 (RC-22, research D2): o mesmo seletor, só com imagens. */
private val TIPOS_FOTO = arrayOf("image/jpeg", "image/png")

/**
 * E2.2 Seus documentos (quadros t31hN, hHwfV, CvQRm, WPH7O, Leg44, FEqIA), com a folha E2.7 em dois
 * passos — tipo (fwOMP) e origem (ncnpi, RC-22) — e as recusas E2.3 (mNcfi). "Fotografar" abre a
 * E4.1 Condições da captura como passo do anexar, no lugar da lista (spec 023, research D1); dela, a
 * paciente segue para o mesmo seletor do sistema. "Abrir", "Mais opções", substituir e remover não
 * aparecem: são de outras histórias ou rotas fora da #42 (spec 022, FR-030).
 *
 * @param condicoesCaptura ViewModel da E4.1 deste pedido (RC-22).
 * @param aoLer abre E3.1 (leitura simulada) deste pedido.
 * @param aoVerResultado abre E3.3 deste pedido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentosTela(
    viewModel: DocumentosViewModel,
    condicoesCaptura: CondicoesCapturaViewModel,
    pedidoId: String,
    aoVoltar: () -> Unit,
    aoLer: () -> Unit,
    aoVerResultado: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val envio by viewModel.envio.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val contexto = LocalContext.current

    // Tipo escolhido na folha, guardado enquanto o seletor do sistema está aberto (sobrevive à rotação).
    var tipoPendente by rememberSaveable { mutableStateOf<String?>(null) }
    var folhaAberta by rememberSaveable { mutableStateOf(false) }
    // Tipo escolhido no 1º passo da folha; `null` = a folha mostra o passo "tipo".
    var tipoFolha by rememberSaveable { mutableStateOf<String?>(null) }
    // E4.1 aberta para este tipo (RC-22); `null` = mostra a lista de documentos.
    var capturaTipo by rememberSaveable { mutableStateOf<String?>(null) }

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val tipo = tipoPendente
        tipoPendente = null
        if (uri == null || tipo == null) return@rememberLauncherForActivityResult
        val fonte = FonteArquivoUri.de(contexto.contentResolver, uri)
        if (fonte != null) viewModel.arquivoEscolhido(tipo, fonte) else viewModel.arquivoInacessivel(tipo)
    }
    val escolherArquivo = { tipo: String, tipos: Array<String> ->
        tipoPendente = tipo
        seletor.launch(tipos)
    }
    val fecharFolha = {
        folhaAberta = false
        tipoFolha = null
    }

    LaunchedEffect(Unit) { viewModel.carregar() }

    // Voltar do sistema na E4.1 volta à lista, não à tela anterior da pilha.
    BackHandler(enabled = capturaTipo != null) { capturaTipo = null }

    val tipoCaptura = capturaTipo
    if (tipoCaptura != null) {
        CondicoesCapturaTela(
            viewModel = condicoesCaptura,
            pedidoId = pedidoId,
            aoVoltar = { capturaTipo = null },
            aoEscolherFoto = {
                capturaTipo = null
                escolherArquivo(tipoCaptura, TIPOS_FOTO)
            },
            aoEscolherArquivo = {
                capturaTipo = null
                escolherArquivo(tipoCaptura, TIPOS_ACEITOS)
            },
            aoPedidoFechado = {
                capturaTipo = null
                viewModel.carregar()
            },
            aoVoltarParaPedidos = {
                capturaTipo = null
                aoVoltar()
            },
        )
        return
    }

    EstruturaTela(
        avisos = avisos,
        cabecalho = { CabecalhoVoltar(aoVoltar, titulo = Textos.pedidoNumero(pedidoId)) },
    ) {
        when (val atual = estado) {
            EstadoTela.Carregando, EstadoTela.Vazio -> {
                TituloTela(Textos.DOCUMENTOS_TITULO, abaixo = Textos.DOCUMENTOS_APOIO)
                Spacer(Modifier.height(24.dp))
                Esqueleto(Textos.DOCUMENTOS_CARREGANDO, linhas = 2)
            }
            is EstadoTela.Falha -> {
                TituloTela(Textos.DOCUMENTOS_TITULO)
                Spacer(Modifier.height(24.dp))
                FalhaCarga(atual.erro, aoVoltar, viewModel::carregar)
            }
            is EstadoTela.Sucesso -> ConteudoDocumentos(
                dados = atual.dados,
                envio = envio,
                aoAnexar = {
                    tipoFolha = null
                    folhaAberta = true
                },
                aoEscolherDeNovo = { tipo -> escolherArquivo(tipo, TIPOS_ACEITOS) },
                aoTentarDeNovo = viewModel::tentarDeNovo,
                aoLer = aoLer,
                aoVerResultado = aoVerResultado,
                aoVoltar = aoVoltar,
            )
        }
    }

    if (folhaAberta) {
        val folha = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = fecharFolha,
            sheetState = folha,
            containerColor = CoresRc.Fundo,
        ) {
            val tipo = tipoFolha
            if (tipo == null) {
                FolhaTipoDocumento(aoEscolher = { tipoFolha = it }, aoCancelar = fecharFolha)
            } else {
                FolhaOrigem(
                    aoFotografar = {
                        fecharFolha()
                        condicoesCaptura.novaVisita()
                        capturaTipo = tipo
                    },
                    aoEscolherArquivo = {
                        fecharFolha()
                        escolherArquivo(tipo, TIPOS_ACEITOS)
                    },
                    aoCancelar = fecharFolha,
                )
            }
        }
    }
}

@Composable
private fun FalhaCarga(erro: ErroApi, aoVoltar: () -> Unit, aoTentar: () -> Unit) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        ErroApi.NaoEncontrado -> {
            Text(Textos.NAO_ENCONTRADO_TITULO, style = TiposRc.Secao, color = CoresRc.Texto, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(Textos.NAO_ENCONTRADO_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR_PARA_PEDIDOS, aoVoltar)
        }
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.SEM_CONEXAO_TEXTO, aoTentar = aoTentar)
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.INDISPONIVEL_TITULO, Textos.DETALHE_INDISPONIVEL_TEXTO, aoTentar = aoTentar)
    }
}

@Composable
private fun ConteudoDocumentos(
    dados: DadosDocumentos,
    envio: EnvioEmCurso?,
    aoAnexar: () -> Unit,
    aoEscolherDeNovo: (String) -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoLer: () -> Unit,
    aoVerResultado: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val enviando = envio?.situacao == SituacaoEnvio.Enviando
    TituloTela(
        Textos.DOCUMENTOS_TITULO,
        abaixo = if (dados.rascunho) Textos.DOCUMENTOS_APOIO else Textos.DOCUMENTOS_APOIO_VERIFICADO,
    )
    Spacer(Modifier.height(24.dp))

    // E2.3: a recusa aparece acima da lista e o arquivo recusado não é listado.
    if (envio != null) when (val situacao = envio.situacao) {
        is SituacaoEnvio.Recusado -> {
            BlocoRetorno(
                TipoRetorno.ERRO,
                Textos.arquivoNaoEnviado(envio.nome),
                "${situacao.mensagem}\n${Textos.NADA_SALVO}",
                rotuloAcao = Textos.ESCOLHER_OUTRO_ARQUIVO,
                aoTentar = if (dados.podeAnexar) ({ aoEscolherDeNovo(envio.tipoDocumental) }) else null,
            )
            Spacer(Modifier.height(24.dp))
        }
        SituacaoEnvio.EscolherDeNovo -> {
            BlocoRetorno(
                TipoRetorno.AVISO,
                Textos.ESCOLHA_OUTRA_VEZ,
                Textos.NADA_SALVO,
                rotuloAcao = Textos.ESCOLHER_OUTRO_ARQUIVO,
                aoTentar = if (dados.podeAnexar) ({ aoEscolherDeNovo(envio.tipoDocumental) }) else null,
            )
            Spacer(Modifier.height(24.dp))
        }
        is SituacaoEnvio.PedidoFechado -> {
            BlocoRetorno(TipoRetorno.AVISO, situacao.mensagem, Textos.NADA_SALVO, rotuloAcao = Textos.VOLTAR, aoTentar = aoVoltar)
            Spacer(Modifier.height(24.dp))
        }
        else -> Unit
    }

    if (!dados.rascunho) {
        val aviso = if (dados.pedido.estado == "SUBMETIDA_SIMULACAO") Textos.PEDIDO_SUBMETIDO else Textos.PEDIDO_VERIFICADO_AVISO
        BlocoRetorno(TipoRetorno.AVISO, aviso)
        Spacer(Modifier.height(24.dp))
    }

    SuperficieTarefa(Textos.ARQUIVOS_DO_PEDIDO) {
        val linhaEnvio = envio?.takeIf { it.situacao == SituacaoEnvio.Enviando || it.situacao is SituacaoEnvio.NaoEnviado }
        if (dados.documentos.isEmpty() && linhaEnvio == null) {
            Text(Textos.NENHUM_DOCUMENTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
        }
        dados.documentos.forEachIndexed { indice, documento ->
            if (indice > 0) HorizontalDivider(color = CoresRc.Divisor, modifier = Modifier.padding(vertical = 12.dp))
            LinhaDocumento(documento)
        }
        if (linhaEnvio != null) {
            if (dados.documentos.isNotEmpty()) HorizontalDivider(color = CoresRc.Divisor, modifier = Modifier.padding(vertical = 12.dp))
            LinhaEnvio(linhaEnvio)
        }
    }

    if (envio != null) when (val situacao = envio.situacao) {
        SituacaoEnvio.Enviando -> {
            Spacer(Modifier.height(16.dp))
            Text(Textos.MANTENHA_ABERTO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        }
        is SituacaoEnvio.NaoEnviado -> {
            Spacer(Modifier.height(24.dp))
            BlocoRetorno(
                TipoRetorno.ERRO,
                Textos.envioInterrompido(envio.nome),
                when (situacao.erro) {
                    ErroApi.TempoEsgotado -> Textos.CONFIRMACAO_INCERTA_TEXTO
                    // "O que você digitou foi mantido" (E4.3) não serve para arquivo; o quadro WPH7O sim.
                    ErroApi.SemConexao -> "${Textos.SEM_CONEXAO_TITULO} ${Textos.ENVIO_INTERROMPIDO_TEXTO}"
                    else -> Textos.ENVIO_INTERROMPIDO_TEXTO
                },
                aoTentar = aoTentarDeNovo,
            )
        }
        else -> Unit
    }

    val quantidade = dados.documentos.size
    // Sem documentos (t31hN), os limites vêm antes do botão principal; com documentos (CvQRm),
    // "Anexar documento" vira contorno e os limites ficam abaixo dele.
    val limitesAntes = quantidade == 0
    if (limitesAntes) Limites(quantidade)
    if (dados.podeAnexar) {
        Spacer(Modifier.height(24.dp))
        if (quantidade == 0) {
            BotaoPrimario(Textos.ANEXAR_DOCUMENTO, aoAnexar, habilitado = !enviando)
        } else {
            BotaoContorno(Textos.ANEXAR_DOCUMENTO, aoAnexar, icone = IconesRc.Mais, habilitado = !enviando)
        }
    }
    if (!limitesAntes) Limites(quantidade)

    if (dados.rascunho && quantidade > 0) {
        Spacer(Modifier.height(32.dp))
        BotaoPrimario(Textos.LER_DOCUMENTOS, aoLer, habilitado = !enviando)
        Spacer(Modifier.height(12.dp))
        Text(
            Textos.LER_APOIO,
            style = TiposRc.Meta,
            color = CoresRc.TextoApoio,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (!dados.rascunho) {
        Spacer(Modifier.height(16.dp))
        BotaoSeta(Textos.VER_RESULTADO, aoVerResultado)
    }
}

/** Quantidade e limites visíveis (#42): 3 documentos, 5 MB cada, PDF até 5 páginas, JPEG ou PNG. */
@Composable
private fun Limites(quantidade: Int) {
    Spacer(Modifier.height(16.dp))
    Text(
        if (quantidade >= MAXIMO_DOCUMENTOS) Textos.LIMITE_ATINGIDO else Textos.contagemDocumentos(quantidade),
        style = TiposRc.Meta,
        color = CoresRc.TextoApoio,
    )
    Text(Textos.LIMITES_FORMATO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
}

/** `Documento/Linha` confirmado pela API: tipo, nome, formato · tamanho · páginas e "Enviado". */
@Composable
private fun LinhaDocumento(documento: Documento) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(RotulosApi.tipoDocumental(documento.tipoDocumental), style = TiposRc.Acao, color = CoresRc.Texto)
        Spacer(Modifier.height(4.dp))
        // Nome de exibição saneado pela API, como texto puro.
        Text(documento.nomeArquivo, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        Text(
            RotulosApi.detalheArquivo(documento.formato, documento.tamanhoBytes, documento.paginas),
            style = TiposRc.Meta,
            color = CoresRc.TextoApoio,
        )
        Spacer(Modifier.height(6.dp))
        SinalTexto(IconesRc.Atende, CoresRc.Sucesso, Textos.ENVIADO)
    }
}

/** Linha do arquivo ainda não confirmado: "Enviando…" ou "Não enviado" (texto + ícone). */
@Composable
private fun LinhaEnvio(envio: EnvioEmCurso) {
    Column(
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(RotulosApi.tipoDocumental(envio.tipoDocumental), style = TiposRc.Acao, color = CoresRc.Texto)
        Spacer(Modifier.height(4.dp))
        Text(envio.nome, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        Spacer(Modifier.height(6.dp))
        if (envio.situacao == SituacaoEnvio.Enviando) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CircularProgressIndicator(color = CoresRc.Acao, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Text(Textos.ENVIANDO, style = TiposRc.MetaForte, color = CoresRc.Texto)
            }
        } else {
            SinalTexto(IconesRc.Falha, CoresRc.Erro, Textos.NAO_ENVIADO)
        }
    }
}

/** Folha E2.7, passo 1, "Que documento você vai anexar?" (fwOMP); depois vem a origem. */
@Composable
private fun FolhaTipoDocumento(aoEscolher: (String) -> Unit, aoCancelar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MARGEM_TELA)
            .padding(bottom = 16.dp),
    ) {
        Text(
            Textos.FOLHA_TIPO_TITULO,
            style = TiposRc.Secao,
            color = CoresRc.Texto,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Textos.TIPOS_DOCUMENTO.forEachIndexed { indice, (codigo, titulo, apoio) ->
            if (indice > 0) HorizontalDivider(color = CoresRc.Divisor)
            OpcaoEscolha(titulo = titulo, apoio = apoio, selecionada = false, aoEscolher = { aoEscolher(codigo) })
        }
        HorizontalDivider(color = CoresRc.Divisor)
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = aoCancelar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(Textos.CANCELAR, style = TiposRc.Acao, color = CoresRc.Acao)
        }
    }
}

/**
 * Folha E2.7, passo 2, "Como você quer anexar?" (ncnpi, RC-22). "Fotografar" abre a E4.1; o app não
 * abre a câmera (research D2). "Escolher arquivo" vai direto ao seletor do sistema.
 */
@Composable
private fun FolhaOrigem(aoFotografar: () -> Unit, aoEscolherArquivo: () -> Unit, aoCancelar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MARGEM_TELA)
            .padding(bottom = 16.dp),
    ) {
        Text(
            Textos.FOLHA_ORIGEM_TITULO,
            style = TiposRc.Secao,
            color = CoresRc.Texto,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        OpcaoEscolha(Textos.ORIGEM_FOTOGRAFAR, Textos.ORIGEM_FOTOGRAFAR_APOIO, selecionada = false, aoEscolher = aoFotografar)
        HorizontalDivider(color = CoresRc.Divisor)
        OpcaoEscolha(Textos.ESCOLHER_ARQUIVO, Textos.ORIGEM_ARQUIVO_APOIO, selecionada = false, aoEscolher = aoEscolherArquivo)
        HorizontalDivider(color = CoresRc.Divisor)
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = aoCancelar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(Textos.CANCELAR, style = TiposRc.Acao, color = CoresRc.Acao)
        }
    }
}
