package br.com.regulacheck.app.ui.leitura

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.regulacheck.app.api.DocumentoLido
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.Extracao
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoContorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.SinalTexto
import br.com.regulacheck.app.ui.componentes.SuperficieTarefa
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/**
 * E3.1 Ler documentos (quadros QxKN9, PbHSM, T1Kav, aEuZ4) e o diálogo E3.8 (Xe5GF). A conferência
 * da leitura (E3.2) fica só no desenho (#40): depois da leitura, a paciente vê o que foi lido em
 * cada documento e pode verificar os critérios.
 *
 * @param aoAnexar abre E2.2 (leitura recusada por falta de documentos).
 * @param aoVerResultado abre E3.3; chamado ao concluir a análise ou ao consultar uma em andamento.
 */
@Composable
fun LeituraTela(
    viewModel: LeituraViewModel,
    pedidoId: String,
    aoVoltar: () -> Unit,
    aoAnexar: () -> Unit,
    aoVerResultado: () -> Unit,
) {
    val leitura by viewModel.leitura.collectAsStateWithLifecycle()
    val analise by viewModel.analise.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(analise) {
        if (analise == EstadoAnalise.Concluida) aoVerResultado()
    }

    EstruturaTela(
        avisos = avisos,
        cabecalho = { CabecalhoVoltar(aoVoltar, titulo = Textos.pedidoNumero(pedidoId), simulado = true) },
    ) {
        when (val atual = leitura) {
            EstadoLeitura.Lendo -> {
                TituloTela(Textos.LENDO_TITULO, abaixo = Textos.AVISO_LEITURA_SIMULADA)
                Spacer(Modifier.height(24.dp))
                Esqueleto(Textos.LENDO, linhas = 1)
                Spacer(Modifier.height(24.dp))
                BotaoPrimario(Textos.LENDO_BOTAO, {}, carregando = true)
            }
            is EstadoLeitura.Falhou -> {
                TituloTela(Textos.LER_TITULO, abaixo = Textos.AVISO_LEITURA_SIMULADA)
                Spacer(Modifier.height(24.dp))
                FalhaLeitura(atual.erro, viewModel::ler, aoAnexar)
            }
            is EstadoLeitura.Lida -> LeituraConcluida(
                extracao = atual.extracao,
                analise = analise,
                aoVerificar = viewModel::pedirConfirmacao,
                aoAnalisarDeNovo = viewModel::analisar,
                aoConsultarDeNovo = viewModel::consultarDeNovo,
                aoLerDeNovo = viewModel::ler,
                aoVerResultado = aoVerResultado,
            )
        }
    }

    if (analise == EstadoAnalise.Confirmando) {
        DialogoVerificar(aoConfirmar = viewModel::analisar, aoVoltar = viewModel::cancelarConfirmacao)
    }
}

@Composable
private fun FalhaLeitura(erro: ErroApi, aoLer: () -> Unit, aoAnexar: () -> Unit) {
    when {
        erro == ErroApi.SessaoExpirada -> Unit
        erro is ErroApi.RequisicaoInvalida && erro.codigo == "SEM_DOCUMENTOS" -> {
            BlocoRetorno(TipoRetorno.AVISO, Textos.SEM_DOCUMENTOS_LER)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.ANEXAR_DOCUMENTO, aoAnexar)
        }
        // 409: documentos mudaram durante a leitura, ou pedido submetido (mensagem da API).
        erro is ErroApi.Conflito -> BlocoRetorno(
            TipoRetorno.AVISO,
            erro.mensagem?.takeIf { it.isNotBlank() } ?: Textos.DOCUMENTOS_MUDARAM,
            rotuloAcao = Textos.LER_DOCUMENTOS,
            aoTentar = aoLer,
        )
        erro is ErroApi.Indisponivel -> BlocoRetorno(
            TipoRetorno.ERRO,
            Textos.LEITURA_INDISPONIVEL_TITULO,
            Textos.LEITURA_INDISPONIVEL_TEXTO,
            aoTentar = aoLer,
        )
        erro == ErroApi.SemConexao -> BlocoRetorno(
            TipoRetorno.SEM_CONEXAO,
            Textos.SEM_CONEXAO_TITULO,
            Textos.SEM_CONEXAO_TEXTO,
            rotuloAcao = Textos.TENTAR_NOVAMENTE,
            aoTentar = aoLer,
        )
        // Repetir a leitura é seguro: sobre a mesma versão a API devolve a leitura existente.
        erro == ErroApi.TempoEsgotado -> BlocoRetorno(
            TipoRetorno.SEM_CONEXAO,
            Textos.CONFIRMACAO_INCERTA_TITULO,
            Textos.CONFIRMACAO_INCERTA_TEXTO,
            rotuloAcao = Textos.TENTAR_NOVAMENTE,
            aoTentar = aoLer,
        )
        erro == ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        erro == ErroApi.NaoEncontrado -> BlocoRetorno(TipoRetorno.ERRO, Textos.NAO_ENCONTRADO_TITULO, Textos.NAO_ENCONTRADO_TEXTO)
        erro is ErroApi.RequisicaoInvalida -> BlocoRetorno(
            TipoRetorno.ERRO,
            erro.mensagem?.takeIf { it.isNotBlank() } ?: Textos.LEITURA_INDISPONIVEL_TITULO,
            aoTentar = aoLer,
        )
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.LEITURA_INDISPONIVEL_TITULO, Textos.LEITURA_INDISPONIVEL_TEXTO, aoTentar = aoLer)
    }
}

@Composable
private fun LeituraConcluida(
    extracao: Extracao,
    analise: EstadoAnalise,
    aoVerificar: () -> Unit,
    aoAnalisarDeNovo: () -> Unit,
    aoConsultarDeNovo: () -> Unit,
    aoLerDeNovo: () -> Unit,
    aoVerResultado: () -> Unit,
) {
    // Aviso de simulação do backend, como texto puro.
    TituloTela(Textos.LER_TITULO, abaixo = extracao.aviso)
    Spacer(Modifier.height(24.dp))
    SuperficieTarefa(Textos.O_QUE_FOI_LIDO) {
        extracao.documentos.forEachIndexed { indice, documento ->
            if (indice > 0) HorizontalDivider(color = CoresRc.Divisor, modifier = Modifier.padding(vertical = 12.dp))
            LinhaDocumentoLido(documento)
        }
    }
    Spacer(Modifier.height(24.dp))

    when (analise) {
        is EstadoAnalise.EmAndamento -> {
            BlocoRetorno(
                TipoRetorno.AVISO,
                if (analise.retryAfterSegundos != null) {
                    Textos.analiseEmAndamento(analise.retryAfterSegundos)
                } else {
                    Textos.VERIFICACAO_NAO_TERMINOU
                },
                rotuloAcao = Textos.CONSULTAR_RESULTADO,
                aoTentar = aoVerResultado,
            )
            return
        }
        EstadoAnalise.Incerta -> {
            BlocoRetorno(
                TipoRetorno.SEM_CONEXAO,
                Textos.CONFIRMACAO_INCERTA_TITULO,
                Textos.CONFIRMACAO_INCERTA_TEXTO,
                rotuloAcao = Textos.CONSULTAR_DE_NOVO,
                aoTentar = aoConsultarDeNovo,
            )
            return
        }
        is EstadoAnalise.Falhou -> {
            FalhaAnalise(analise.erro, aoAnalisarDeNovo, aoLerDeNovo)
            return
        }
        else -> Unit
    }

    val analisando = analise == EstadoAnalise.Analisando || analise == EstadoAnalise.Concluida
    BotaoPrimario(
        if (analisando) Textos.ANALISANDO else Textos.VERIFICAR_CRITERIOS,
        aoVerificar,
        carregando = analisando,
    )
}

@Composable
private fun FalhaAnalise(erro: ErroApi, aoAnalisar: () -> Unit, aoLer: () -> Unit) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        // 409 sem leitura atual, documentos mudaram ou pedido submetido: mensagem da API.
        is ErroApi.Conflito -> BlocoRetorno(
            TipoRetorno.AVISO,
            erro.mensagem?.takeIf { it.isNotBlank() } ?: Textos.DOCUMENTOS_MUDARAM,
            rotuloAcao = Textos.LER_DOCUMENTOS,
            aoTentar = aoLer,
        )
        // O histórico confirmou que nada foi gravado: agora a paciente pode pedir de novo.
        ErroApi.TempoEsgotado, ErroApi.SemConexao -> BlocoRetorno(
            TipoRetorno.SEM_CONEXAO,
            Textos.CONFIRMACAO_INCERTA_TITULO,
            Textos.CONFIRMACAO_INCERTA_TEXTO,
            rotuloAcao = Textos.TENTAR_NOVAMENTE,
            aoTentar = aoAnalisar,
        )
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        ErroApi.NaoEncontrado -> BlocoRetorno(TipoRetorno.ERRO, Textos.NAO_ENCONTRADO_TITULO, Textos.NAO_ENCONTRADO_TEXTO)
        else -> BlocoRetorno(
            TipoRetorno.ERRO,
            Textos.FALHA_VERIFICACAO_TITULO,
            Textos.FALHA_VERIFICACAO_TEXTO,
            aoTentar = aoAnalisar,
        )
    }
}

/** Documento lido: tipo e situação; quando não lido, motivo e orientação do backend (texto puro). */
@Composable
private fun LinhaDocumentoLido(documento: DocumentoLido) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(RotulosApi.tipoDocumental(documento.tipoDocumental), style = TiposRc.Acao, color = CoresRc.Texto)
        Spacer(Modifier.height(6.dp))
        if (documento.situacao == "LIDO") {
            val identificadas = documento.campos.count { it.situacao == "IDENTIFICADO" }
            SinalTexto(IconesRc.Atende, CoresRc.Sucesso, RotulosApi.situacaoLeitura(documento.situacao))
            Spacer(Modifier.height(4.dp))
            Text(
                Textos.informacoesIdentificadas(identificadas, documento.campos.size),
                style = TiposRc.Meta,
                color = CoresRc.TextoApoio,
            )
        } else {
            SinalTexto(IconesRc.Alerta, CoresRc.Pendencia, RotulosApi.situacaoLeitura(documento.situacao))
            listOfNotNull(documento.motivo, documento.orientacao).filter { it.isNotBlank() }.forEach { texto ->
                Spacer(Modifier.height(4.dp))
                Text(texto, style = TiposRc.Meta, color = CoresRc.TextoApoio)
            }
        }
    }
}

/** `Diálogo/Confirmação` E3.8 "Verificar critérios?". */
@Composable
private fun DialogoVerificar(aoConfirmar: () -> Unit, aoVoltar: () -> Unit) {
    // Largura do quadro Xe5GF: a tela menos 16 de cada lado, e não a largura padrão do sistema.
    Dialog(onDismissRequest = aoVoltar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(CoresRc.Fundo, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Icon(IconesRc.ListaVerificacao, contentDescription = null, tint = CoresRc.Texto, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                Textos.VERIFICAR_CRITERIOS_PERGUNTA,
                style = TiposRc.Secao,
                color = CoresRc.Texto,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(Textos.DIALOGO_NAO_CONFERIDAS, style = TiposRc.Corpo, color = CoresRc.Texto)
            // Sem a frase do quadro sobre não poder anexar depois: a RC-16 passou a aceitar a versão corrigida.
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VERIFICAR_CRITERIOS, aoConfirmar)
            Spacer(Modifier.height(12.dp))
            BotaoContorno(Textos.VOLTAR, aoVoltar)
        }
    }
}
