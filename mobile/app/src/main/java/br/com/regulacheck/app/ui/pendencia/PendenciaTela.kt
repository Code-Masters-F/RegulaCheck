package br.com.regulacheck.app.ui.pendencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.FORMA_SUPERFICIE
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.resultado.DetalhePendencia
import br.com.regulacheck.app.ui.resultado.linhasEvidencia
import br.com.regulacheck.app.ui.resultado.orientacoesDoApp
import br.com.regulacheck.app.ui.resultado.situacaoDaPendencia
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.TiposRc

/**
 * E3.4 O que falta resolver (quadros XCNVm, QQLpq, HK8ZN, u8AYS3), com os dados da mesma análise
 * do resultado. Mostra o critério, a versão do protocolo, a situação, o responsável registrado pela
 * regra (sem reescrever o valor da API), a orientação da RC-15 e as evidências — página e trecho só
 * quando existirem. Nenhum botão de ação: abrir documento, substituir, conferir e reenviar estão
 * fora desta versão e são explicados em texto (spec 022, FR-023).
 */
@Composable
fun PendenciaTela(detalhe: DetalhePendencia, aoVoltar: () -> Unit) {
    val avisos = remember { SnackbarHostState() }
    val pendencia = detalhe.pendencia

    EstruturaTela(
        avisos = avisos,
        cabecalho = { CabecalhoVoltar(aoVoltar, titulo = Textos.pedidoNumero(detalhe.pedidoId), simulado = true) },
    ) {
        TituloTela(Textos.PENDENCIA_TITULO, abaixo = Textos.apoioPendencia(pendencia.codigoRegra, detalhe.versaoProtocolo))

        if (!detalhe.vigente) {
            Spacer(Modifier.height(16.dp))
            BlocoRetorno(TipoRetorno.AVISO, Textos.RESULTADO_ANTERIOR, Textos.resultadoAnteriorTexto(detalhe.revisao))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            pendencia.descricaoRegra,
            style = TiposRc.Secao,
            color = CoresRc.Texto,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        // Explicação fixa do protocolo, como texto puro.
        Text(pendencia.explicacao, style = TiposRc.Corpo, color = CoresRc.Texto)

        linhasEvidencia(detalhe).forEach { linha ->
            Spacer(Modifier.height(8.dp))
            Text(linha.documento, style = TiposRc.Meta, color = CoresRc.TextoApoio)
            linha.trecho?.let { trecho ->
                // Trecho do documento: dado não confiável, exibido literalmente.
                Text(trecho, style = TiposRc.Meta, color = CoresRc.TextoApoio)
            }
        }

        Spacer(Modifier.height(24.dp))
        DadoRotulado(Textos.SITUACAO, situacaoDaPendencia(pendencia))
        Spacer(Modifier.height(16.dp))
        DadoRotulado(Textos.RESPONSAVEL_REGISTRADO, RotulosApi.responsavel(pendencia.responsavel))

        pendencia.orientacao?.takeIf { it.isNotBlank() }?.let { orientacao ->
            Spacer(Modifier.height(24.dp))
            OrientacaoPendencia(orientacao)
        }

        val orientacoes = orientacoesDoApp(pendencia)
        if (orientacoes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            BlocoRetorno(TipoRetorno.AVISO, orientacoes.first(), orientacoes.drop(1).joinToString("\n\n").ifBlank { null })
        }
    }
}

/** Rótulo pequeno e valor (Situação, Responsável registrado pela regra), lidos juntos. */
@Composable
private fun DadoRotulado(rotulo: String, valor: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(rotulo, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        Spacer(Modifier.height(2.dp))
        Text(valor, style = TiposRc.Corpo, color = CoresRc.Texto)
    }
}

/** `Pendência/Orientação` "Quem pode resolver": barra de destaque e a orientação da API (texto puro). */
@Composable
private fun OrientacaoPendencia(orientacao: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(CoresRc.Superficie, FORMA_SUPERFICIE)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        // A barra é só decorativa; o título carrega o sentido.
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(CoresRc.Pendencia),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                Textos.QUEM_PODE_RESOLVER,
                style = TiposRc.TituloItem,
                color = CoresRc.Texto,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(orientacao, style = TiposRc.Corpo, color = CoresRc.Texto)
        }
    }
}
