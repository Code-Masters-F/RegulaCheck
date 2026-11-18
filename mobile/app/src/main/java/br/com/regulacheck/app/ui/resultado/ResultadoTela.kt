package br.com.regulacheck.app.ui.resultado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.regulacheck.app.api.AnaliseGravada
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.PendenciaGravada
import br.com.regulacheck.app.api.ResultadoRegra
import br.com.regulacheck.app.ui.EstadoPedido
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.apresentarEstado
import br.com.regulacheck.app.ui.SinalEstado
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoContorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.BotaoSeta
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.RotuloEstado
import br.com.regulacheck.app.ui.componentes.SeloSimulado
import br.com.regulacheck.app.ui.componentes.SinalTexto
import br.com.regulacheck.app.ui.componentes.SuperficieTarefa
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloSecao
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.formatarDataHora
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/**
 * E3.3 Resultado da verificação (quadros L5Keg, zMx9T, cAl1Z, EAe9t, udibQ), conforme o contrato de
 * apresentação da RC-15. "Conferir leitura" e "Concluir simulação" não aparecem: a conferência é só
 * desenho (#40) e a conclusão é da RC-17.
 *
 * @param aoAbrirPendencia abre E3.4 com os dados da mesma análise.
 * @param aoAbrirAnterior abre uma análise não vigente como "Resultado anterior".
 * @param aoLerDeNovo abre E3.1 (análise com falha: "Tentar de novo").
 */
@Composable
fun ResultadoTela(
    viewModel: ResultadoViewModel,
    pedidoId: String,
    aoVoltar: () -> Unit,
    aoVoltarParaPedidos: () -> Unit,
    aoAbrirPendencia: (DetalhePendencia) -> Unit,
    aoAbrirAnterior: (String) -> Unit,
    aoLerDeNovo: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val resumo by viewModel.resumo.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.carregar() }

    EstruturaTela(
        avisos = avisos,
        cabecalho = { CabecalhoVoltar(aoVoltar, titulo = Textos.pedidoNumero(pedidoId), simulado = true) },
    ) {
        when (val atual = estado) {
            EstadoTela.Carregando, EstadoTela.Vazio -> {
                TituloTela(Textos.RESULTADO_TITULO)
                Spacer(Modifier.height(24.dp))
                Esqueleto(Textos.RESULTADO_CARREGANDO)
            }
            is EstadoTela.Falha -> {
                TituloTela(Textos.RESULTADO_TITULO)
                Spacer(Modifier.height(24.dp))
                FalhaConsulta(atual.erro, aoVoltar, aoVoltarParaPedidos, viewModel::carregar)
            }
            is EstadoTela.Sucesso -> when (val apresentacao = atual.dados) {
                is ApresentacaoHistorico.Analise -> ResultadoDaAnalise(
                    pedidoId = pedidoId,
                    analise = apresentacao.analise,
                    vigente = apresentacao.vigente,
                    resumo = resumo,
                    aoPedirResumo = viewModel::pedirResumo,
                    aoAbrirPendencia = aoAbrirPendencia,
                )
                ApresentacaoHistorico.EmAndamento -> {
                    TituloTela(Textos.RESULTADO_TITULO)
                    Spacer(Modifier.height(16.dp))
                    apresentarEstado(EstadoPedido.PROCESSANDO, 0).let { RotuloEstado(it.rotulo, it.sinal) }
                    Spacer(Modifier.height(12.dp))
                    Text(Textos.VERIFICACAO_NAO_TERMINOU, style = TiposRc.Corpo, color = CoresRc.Texto)
                    Spacer(Modifier.height(24.dp))
                    BotaoContorno(Textos.ATUALIZAR, viewModel::carregar, icone = IconesRc.Recarregar)
                }
                is ApresentacaoHistorico.Falha -> {
                    TituloTela(Textos.RESULTADO_TITULO)
                    Spacer(Modifier.height(16.dp))
                    apresentarEstado(EstadoPedido.FALHA_ANALISE, 0).let { RotuloEstado(it.rotulo, it.sinal) }
                    Spacer(Modifier.height(16.dp))
                    BlocoRetorno(TipoRetorno.ERRO, Textos.FALHA_VERIFICACAO_TITULO, Textos.FALHA_VERIFICACAO_TEXTO)
                    Spacer(Modifier.height(24.dp))
                    BotaoPrimario(Textos.TENTAR_DE_NOVO, aoLerDeNovo)
                }
                ApresentacaoHistorico.SemAnalise -> {
                    TituloTela(Textos.RESULTADO_TITULO)
                    Spacer(Modifier.height(16.dp))
                    Text(Textos.SEM_ANALISE, style = TiposRc.Corpo, color = CoresRc.Texto)
                }
                is ApresentacaoHistorico.SemVigente -> {
                    TituloTela(Textos.RESULTADO_TITULO)
                    Spacer(Modifier.height(16.dp))
                    Text(Textos.SEM_VIGENTE, style = TiposRc.Corpo, color = CoresRc.Texto)
                    Spacer(Modifier.height(16.dp))
                    BotaoSeta(Textos.VER_RESULTADO_ANTERIOR) { aoAbrirAnterior(apresentacao.anterior.id) }
                }
            }
        }
    }
}

@Composable
private fun FalhaConsulta(erro: ErroApi, aoVoltar: () -> Unit, aoVoltarParaPedidos: () -> Unit, aoTentar: () -> Unit) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        ErroApi.SemPermissao -> {
            BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR, aoVoltar)
        }
        // 404 não revela se o pedido existe para outra paciente.
        ErroApi.NaoEncontrado -> {
            Text(Textos.NAO_ENCONTRADO_TITULO, style = TiposRc.Secao, color = CoresRc.Texto, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(Textos.NAO_ENCONTRADO_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR_PARA_PEDIDOS, aoVoltarParaPedidos)
        }
        ErroApi.SemConexao, ErroApi.TempoEsgotado -> BlocoRetorno(
            TipoRetorno.SEM_CONEXAO,
            Textos.RESULTADO_ERRO,
            Textos.SEM_CONEXAO_TEXTO,
            rotuloAcao = Textos.TENTAR_NOVAMENTE,
            aoTentar = aoTentar,
        )
        else -> BlocoRetorno(
            TipoRetorno.ERRO,
            Textos.RESULTADO_ERRO,
            Textos.DETALHE_INDISPONIVEL_TEXTO,
            rotuloAcao = Textos.TENTAR_NOVAMENTE,
            aoTentar = aoTentar,
        )
    }
}

@Composable
private fun ResultadoDaAnalise(
    pedidoId: String,
    analise: AnaliseGravada,
    vigente: Boolean,
    resumo: EstadoResumo,
    aoPedirResumo: () -> Unit,
    aoAbrirPendencia: (DetalhePendencia) -> Unit,
) {
    val versao = RotulosApi.versaoProtocolo(analise.protocolo.versao)
    TituloTela(Textos.RESULTADO_TITULO)
    Spacer(Modifier.height(8.dp))
    Text(Textos.verificadoEm(formatarDataHora(analise.executadaEm), analise.revisao), style = TiposRc.Meta, color = CoresRc.TextoApoio)
    Text(Textos.protocoloVersao(versao), style = TiposRc.Meta, color = CoresRc.TextoApoio)

    if (!vigente) {
        Spacer(Modifier.height(16.dp))
        BlocoRetorno(TipoRetorno.AVISO, Textos.RESULTADO_ANTERIOR, Textos.resultadoAnteriorTexto(analise.revisao))
    }

    Spacer(Modifier.height(16.dp))
    RotuloEstado(rotuloDoResultado(analise), sinalDoResultado(analise.resultadoGeral))
    Spacer(Modifier.height(16.dp))

    ResumoDaAnalise(analise, vigente, resumo, aoPedirResumo)

    Spacer(Modifier.height(16.dp))
    // Aviso de simulação vindo da API, como texto puro.
    Text(analise.aviso, style = TiposRc.Meta, color = CoresRc.TextoApoio)

    when (analise.resultadoGeral) {
        "REQUER_CONFERENCIA" -> {
            Spacer(Modifier.height(16.dp))
            Text(Textos.CONFIRA_TEXTO, style = TiposRc.Corpo, color = CoresRc.Texto)
            Spacer(Modifier.height(8.dp))
            Text(Textos.CONFIRA_SO_DESENHO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        }
        "SEM_PENDENCIAS_IDENTIFICADAS" -> {
            Spacer(Modifier.height(16.dp))
            BlocoRetorno(TipoRetorno.AVISO, Textos.SEM_PENDENCIAS_AVISO)
        }
    }

    val nomes = analise.documentos.associate { it.documentoId to it.nomeArquivo }
    if (analise.pendencias.isNotEmpty()) {
        TituloSecao(Textos.O_QUE_FALTA)
        analise.pendencias.forEach { pendencia ->
            ItemPendencia(pendencia, nomes) { aoAbrirPendencia(detalheDe(pedidoId, analise, vigente, pendencia)) }
        }
    }

    val codigosPendentes = analise.pendencias.map { it.codigoRegra }.toSet()
    val outros = analise.resultados.filterNot { it.codigoRegra in codigosPendentes }
    if (outros.isNotEmpty()) {
        TituloSecao(if (analise.pendencias.isEmpty()) Textos.CRITERIOS else Textos.OUTROS_CRITERIOS)
        outros.forEach { LinhaCriterio(it) }
    }

    Spacer(Modifier.height(24.dp))
    Text(Textos.AVISO_RESULTADO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
}

/** `Resumo/Simulado`: texto da RC-14 com SIMULADO; ausente ou `PADRAO` oferece "Tentar de novo". */
@Composable
private fun ResumoDaAnalise(analise: AnaliseGravada, vigente: Boolean, estado: EstadoResumo, aoPedir: () -> Unit) {
    val resumo = analise.resumo
    SuperficieTarefa(Textos.RESUMO_TITULO, acaoTitulo = { SeloSimulado() }) {
        // Texto do resumo como texto puro: `<` e `&` aparecem literalmente.
        Text(resumo?.texto ?: Textos.RESUMO_INDISPONIVEL, style = TiposRc.Corpo, color = CoresRc.Texto)
        if (resumo != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (resumo.origem == "PADRAO") Textos.RESUMO_PADRAO_NOTA else Textos.RESUMO_NOTA,
                style = TiposRc.Legenda,
                color = CoresRc.TextoApoio,
            )
        }
    }
    val podeTentar = vigente && (resumo == null || resumo.origem == "PADRAO")
    if (podeTentar) {
        Spacer(Modifier.height(16.dp))
        val pedindo = estado == EstadoResumo.Pedindo
        BotaoContorno(Textos.TENTAR_DE_NOVO, aoPedir, icone = IconesRc.Recarregar, habilitado = !pedindo)
        if (estado is EstadoResumo.Falhou) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (estado.erro == ErroApi.SemConexao || estado.erro == ErroApi.TempoEsgotado) {
                    Textos.SEM_CONEXAO_TITULO
                } else {
                    Textos.INDISPONIVEL_TITULO
                },
                style = TiposRc.Meta,
                color = CoresRc.Texto,
            )
        }
    }
}

/** `Pendência/Linha`: critério, código · tipo · arquivo, quem resolve; a linha toda abre o detalhe. */
@Composable
private fun ItemPendencia(pendencia: PendenciaGravada, nomes: Map<String, String>, aoAbrir: () -> Unit) {
    val (icone, cor) = iconeDoResultado(pendencia.tipo)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = pendencia.descricaoRegra, role = Role.Button, onClick = aoAbrir),
    ) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pendencia.descricaoRegra, style = TiposRc.TituloItem, color = CoresRc.Texto)
                Spacer(Modifier.height(4.dp))
                Text(apoioItemPendencia(pendencia, nomes), style = TiposRc.Meta, color = CoresRc.TextoApoio)
                Spacer(Modifier.height(4.dp))
                Text(RotulosApi.quemResolve(pendencia.responsavel), style = TiposRc.MetaForte, color = CoresRc.Texto)
            }
            Icon(IconesRc.SetaDireita, contentDescription = null, tint = CoresRc.TextoApoio, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}

/** `Critério/Linha`: descrição e código, resultado em texto + ícone à direita. */
@Composable
private fun LinhaCriterio(resultado: ResultadoRegra) {
    val (icone, cor) = iconeDoResultado(resultado.resultado)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(resultado.descricao, style = TiposRc.Corpo, color = CoresRc.Texto)
                Text(resultado.codigoRegra, style = TiposRc.Meta, color = CoresRc.TextoApoio)
            }
            Spacer(Modifier.width(12.dp))
            SinalTexto(icone, cor, RotulosApi.resultadoRegra(resultado.resultado))
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}

/** Ícone e cor por resultado de regra (F4 `Critério`): a cor reforça, o texto carrega o sentido. */
private fun iconeDoResultado(valor: String): Pair<ImageVector, Color> = when (valor) {
    "ATENDE" -> IconesRc.Atende to CoresRc.Sucesso
    "PENDENTE" -> IconesRc.Alerta to CoresRc.Pendencia
    "NAO_VERIFICAVEL" -> IconesRc.NaoVerificavel to CoresRc.Pendencia
    "NAO_APLICAVEL" -> IconesRc.NaoSeAplica to CoresRc.TextoApoio
    else -> IconesRc.Interrogacao to CoresRc.TextoApoio
}

/** Sinal do resultado geral (mesmos de F4). */
private fun sinalDoResultado(resultadoGeral: String): SinalEstado = when (resultadoGeral) {
    "COM_PENDENCIAS" -> SinalEstado.PENDENCIA
    "REQUER_CONFERENCIA" -> SinalEstado.CONFERIR
    "SEM_PENDENCIAS_IDENTIFICADAS" -> SinalEstado.SEM_PENDENCIA
    else -> SinalEstado.DESCONHECIDO
}
