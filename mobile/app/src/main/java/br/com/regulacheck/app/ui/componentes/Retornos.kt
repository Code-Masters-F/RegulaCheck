package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.regulacheck.app.ui.SinalEstado
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/** Tipo de retorno: erro (fundo de erro) ou falta de conexão/aviso (fundo informativo). */
enum class TipoRetorno { ERRO, SEM_CONEXAO, AVISO, SEM_PERMISSAO }

/**
 * `Retorno/Erro`, `Retorno/Sem conexão` e avisos: diz o que aconteceu e o próximo passo, com
 * ícone + texto e anúncio ao leitor de tela. "Tentar de novo" (contorno) só aparece com [aoTentar].
 */
@Composable
fun BlocoRetorno(
    tipo: TipoRetorno,
    titulo: String,
    texto: String? = null,
    rotuloAcao: String = Textos.TENTAR_DE_NOVO,
    aoTentar: (() -> Unit)? = null,
) {
    val (fundo, icone, corIcone) = when (tipo) {
        TipoRetorno.ERRO -> Triple(CoresRc.FundoErro, IconesRc.Falha, CoresRc.Erro)
        TipoRetorno.SEM_PERMISSAO -> Triple(CoresRc.FundoErro, IconesRc.Cadeado, CoresRc.Erro)
        TipoRetorno.SEM_CONEXAO -> Triple(CoresRc.FundoInfo, IconesRc.SemConexao, CoresRc.Texto)
        TipoRetorno.AVISO -> Triple(CoresRc.FundoInfo, IconesRc.Info, CoresRc.Texto)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(fundo, FORMA_SUPERFICIE)
            .padding(20.dp)
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(24.dp))
            Text(titulo, style = TiposRc.Acao, color = CoresRc.Texto)
        }
        if (texto != null) {
            Spacer(Modifier.height(12.dp))
            Text(texto, style = TiposRc.Corpo, color = CoresRc.Texto)
        }
        if (aoTentar != null) {
            Spacer(Modifier.height(20.dp))
            BotaoContorno(rotuloAcao, aoTentar, icone = if (rotuloAcao == Textos.TENTAR_DE_NOVO) IconesRc.Recarregar else null)
        }
    }
}

/** Ícone e cor de cada [SinalEstado] (F4): a cor reforça, o rótulo carrega o significado. */
private fun SinalEstado.icone(): Pair<ImageVector, Color> = when (this) {
    SinalEstado.RASCUNHO -> IconesRc.Lapis to CoresRc.Texto
    SinalEstado.EM_ANDAMENTO -> IconesRc.Relogio to CoresRc.Acao
    SinalEstado.CONFERIR -> IconesRc.Olho to CoresRc.Pendencia
    SinalEstado.PENDENCIA -> IconesRc.Alerta to CoresRc.Pendencia
    SinalEstado.SEM_PENDENCIA -> IconesRc.Atende to CoresRc.Sucesso
    SinalEstado.FALHA -> IconesRc.Falha to CoresRc.Erro
    SinalEstado.CONCLUIDO -> IconesRc.DocumentoConcluido to CoresRc.Texto
    SinalEstado.DESCONHECIDO -> IconesRc.Interrogacao to CoresRc.TextoApoio
}

/** Componente Estado do F4: ícone 16 na cor do significado + rótulo 14/700, lidos juntos pelo leitor de tela. */
@Composable
fun RotuloEstado(rotulo: String, sinal: SinalEstado) {
    val (icone, cor) = sinal.icone()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(rotulo, style = TiposRc.MetaForte, color = CoresRc.Texto)
    }
}

/** `Carregando/Esqueleto`: blocos estáticos (sem animação essencial) e o texto de carregamento. */
@Composable
fun Esqueleto(texto: String, linhas: Int = 3) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(CoresRc.Divisor, FORMA_SUPERFICIE),
        )
        repeat(linhas - 1) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(CoresRc.Divisor, RoundedCornerShape(8.dp)),
            )
        }
        Text(texto, style = TiposRc.Meta, color = CoresRc.TextoApoio)
    }
}
