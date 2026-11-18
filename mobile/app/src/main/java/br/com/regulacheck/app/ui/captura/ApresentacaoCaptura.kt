package br.com.regulacheck.app.ui.captura

import br.com.regulacheck.app.api.SensorTelemetria
import br.com.regulacheck.app.api.Telemetria
import br.com.regulacheck.app.ui.Textos
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

/*
 * Apresentação da telemetria SIMULADA na E4.1 (RC-22, spec 023, data-model). Funções puras: a
 * situação e a orientação vêm da API (regras da spec 016); aqui só se escolhem rótulo, valor
 * formatado, idade da leitura e a marca de "recuperada" da visita.
 */

/** Situação mostrada de um sensor; o rótulo e o ícone saem dela (texto + ícone, nunca só cor). */
enum class SituacaoSensorUi { ADEQUADA, ADEQUADA_DE_NOVO, BAIXA, ALTA, FORA_DO_IDEAL, DESATUALIZADA, SEM_LEITURA, INDISPONIVEL }

/**
 * Um sensor como aparece na tela.
 *
 * @property valor "410 lux", "31,5 cm" ou "—" sem leitura.
 * @property orientacao texto puro (da API, ou local por código se a API não mandar); `null` dentro da faixa.
 * @property linhaLeitura idade e origem da leitura ("Leitura há 5 s · sensor simulado").
 */
data class ApresentacaoSensor(
    val tipo: String,
    val nome: String,
    val valor: String,
    val situacao: SituacaoSensorUi,
    val rotulo: String,
    val orientacao: String?,
    val linhaLeitura: String,
)

/**
 * A E4.1 inteira: os dois sensores, na ordem luminosidade, distância.
 *
 * @property indisponivel sem telemetria utilizável (falha de rede/serviço ou os dois sem leitura).
 */
data class ApresentacaoCaptura(val sensores: List<ApresentacaoSensor>, val indisponivel: Boolean) {
    /**
     * A ação principal diz "mesmo assim" (quadros `Cna25` e `kaHJz`). Fora da faixa ela não muda
     * (quadros `fOrmB` e `NyDDm`): o sensor orienta, nunca bloqueia.
     */
    val mesmoAssim: Boolean
        get() = indisponivel || sensores.any {
            it.situacao == SituacaoSensorUi.DESATUALIZADA || it.situacao == SituacaoSensorUi.SEM_LEITURA
        }
}

/**
 * Memória de uma visita à E4.1, para perceber a volta à faixa (a API não guarda a transição,
 * research D5).
 *
 * @property foraDaFaixa último valor fora da faixa, por tipo de sensor.
 * @property recuperados valor anterior (fora da faixa) dos sensores que voltaram e continuam dentro.
 */
data class MemoriaCaptura(
    val foraDaFaixa: Map<String, Double> = emptyMap(),
    val recuperados: Map<String, Double> = emptyMap(),
)

/** Apresentação calculada e a memória atualizada para a próxima consulta. */
data class ResultadoApresentacao(val apresentacao: ApresentacaoCaptura, val memoria: MemoriaCaptura)

const val TIPO_LUMINOSIDADE = "LUMINOSIDADE"
const val TIPO_DISTANCIA = "DISTANCIA"

/** Os dois sensores do contrato, na ordem exibida, com a unidade esperada. */
private val SENSORES = listOf(TIPO_LUMINOSIDADE to "LUX", TIPO_DISTANCIA to "CM")

private const val SEM_VALOR = "—"

private fun nomeSensor(tipo: String) = if (tipo == TIPO_LUMINOSIDADE) Textos.LUMINOSIDADE else Textos.DISTANCIA

/**
 * Estado "indisponível" (quadro `kaHJz`): sem rede, serviço fora, sem sessão ou sem leituras. Só o
 * primeiro cartão diz que a paciente pode seguir normalmente, como no desenho.
 */
fun apresentacaoIndisponivel(): ApresentacaoCaptura = ApresentacaoCaptura(
    sensores = SENSORES.mapIndexed { indice, (tipo, _) ->
        ApresentacaoSensor(
            tipo = tipo,
            nome = nomeSensor(tipo),
            valor = SEM_VALOR,
            situacao = SituacaoSensorUi.INDISPONIVEL,
            rotulo = Textos.SENSORES_INDISPONIVEIS,
            orientacao = if (indice == 0) Textos.PODE_ANEXAR_NORMALMENTE else null,
            linhaLeitura = Textos.SEM_LEITURA_DISPONIVEL,
        )
    },
    indisponivel = true,
)

/**
 * Converte uma resposta de `GET …/leituras` na apresentação, usando e atualizando a [memoria] da
 * visita (transições em data-model.md). Os dois sensores sem leitura (sem sessão ou sem leituras)
 * viram o estado indisponível.
 */
fun apresentarTelemetria(telemetria: Telemetria, memoria: MemoriaCaptura): ResultadoApresentacao {
    val porTipo = telemetria.sensores.associateBy { it.tipo }
    val consultadaEm = instante(telemetria.consultadaEm)
    var fora = memoria.foraDaFaixa
    var recuperados = memoria.recuperados

    if (SENSORES.all { (tipo, _) -> situacaoDaApi(porTipo[tipo]) == SEM_LEITURA_API }) {
        return ResultadoApresentacao(apresentacaoIndisponivel(), MemoriaCaptura(fora, emptyMap()))
    }

    val sensores = SENSORES.map { (tipo, unidadeEsperada) ->
        val sensor = porTipo[tipo]
        val unidade = sensor?.unidade ?: unidadeEsperada
        val leitura = sensor?.ultimaLeitura
        val valor = leitura?.let { formatarValor(it.valor, unidade) } ?: SEM_VALOR
        val idade = leitura?.let { idadeSegundos(consultadaEm, it.medidaEm) }?.let(::formatarIdade)
        val orientacaoApi = sensor?.orientacao?.let { o -> o.mensagem?.takeIf { it.isNotBlank() } ?: Textos.orientacaoLocal(o.codigo) }

        fun sensorUi(situacao: SituacaoSensorUi, rotulo: String, orientacao: String?, linha: String) =
            ApresentacaoSensor(tipo, nomeSensor(tipo), valor, situacao, rotulo, orientacao, linha)

        val linhaAtual = when {
            leitura == null -> Textos.SEM_LEITURA_DISPONIVEL
            idade == null -> Textos.LEITURA_SENSOR_SIMULADO
            else -> Textos.leituraHa(idade)
        }

        when (val situacao = situacaoDaApi(sensor)) {
            "DENTRO_DA_FAIXA" -> {
                // Volta à faixa nesta visita: vale o último valor fora (ou o já registrado como recuperado).
                val antes = fora[tipo] ?: recuperados[tipo]
                if (antes == null) {
                    sensorUi(SituacaoSensorUi.ADEQUADA, Textos.SITUACAO_ADEQUADA, null, linhaAtual)
                } else {
                    fora = fora - tipo
                    recuperados = recuperados + (tipo to antes)
                    val linha = if (idade != null) Textos.leituraHaAntes(idade, formatarValor(antes, unidade)) else linhaAtual
                    sensorUi(SituacaoSensorUi.ADEQUADA_DE_NOVO, Textos.SITUACAO_ADEQUADA_DE_NOVO, Textos.CONDICOES_ADEQUADAS_DE_NOVO, linha)
                }
            }
            "ABAIXO_DA_FAIXA", "ACIMA_DA_FAIXA" -> {
                if (leitura != null) fora = fora + (tipo to leitura.valor)
                recuperados = recuperados - tipo
                val (ui, rotulo) = when {
                    tipo == TIPO_DISTANCIA -> SituacaoSensorUi.FORA_DO_IDEAL to Textos.SITUACAO_FORA_DO_IDEAL
                    situacao == "ABAIXO_DA_FAIXA" -> SituacaoSensorUi.BAIXA to Textos.SITUACAO_BAIXA
                    else -> SituacaoSensorUi.ALTA to Textos.SITUACAO_ALTA
                }
                sensorUi(ui, rotulo, orientacaoApi, linhaAtual)
            }
            "DESATUALIZADA" -> {
                // A leitura antiga não confirma nem nega a condição atual; o "esteve fora" é mantido.
                recuperados = recuperados - tipo
                val linha = if (idade != null) Textos.ultimaLeituraHa(idade) else linhaAtual
                sensorUi(SituacaoSensorUi.DESATUALIZADA, Textos.SITUACAO_DESATUALIZADA, orientacaoApi, linha)
            }
            else -> {
                recuperados = recuperados - tipo
                ApresentacaoSensor(
                    tipo, nomeSensor(tipo), SEM_VALOR, SituacaoSensorUi.SEM_LEITURA, Textos.SITUACAO_SEM_LEITURA,
                    orientacaoApi, Textos.SEM_LEITURA_DISPONIVEL,
                )
            }
        }
    }
    return ResultadoApresentacao(ApresentacaoCaptura(sensores, indisponivel = false), MemoriaCaptura(fora, recuperados))
}

private const val SEM_LEITURA_API = "SEM_LEITURA"
private val SITUACOES_CONHECIDAS = setOf("DENTRO_DA_FAIXA", "ABAIXO_DA_FAIXA", "ACIMA_DA_FAIXA", "DESATUALIZADA", SEM_LEITURA_API)

/** Situação da API; sensor ausente ou situação desconhecida (versão futura) contam como sem leitura. */
private fun situacaoDaApi(sensor: SensorTelemetria?): String =
    sensor?.situacao?.takeIf { it in SITUACOES_CONHECIDAS } ?: SEM_LEITURA_API

private val FORMATO_VALOR: NumberFormat
    get() = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }

/** "410 lux", "31,5 cm", "200.000 lux" (pt-BR, até uma casa decimal). */
fun formatarValor(valor: Double, unidade: String): String {
    val sufixo = when (unidade) {
        "LUX" -> "lux"
        "CM" -> "cm"
        else -> unidade.lowercase(Locale.ROOT)
    }
    return "${FORMATO_VALOR.format(valor)} $sufixo"
}

/** "5 s", "3 min", "2 h": idade arredondada para baixo. */
fun formatarIdade(segundos: Long): String = when {
    segundos < 60 -> "$segundos s"
    segundos < 3600 -> "${segundos / 60} min"
    else -> "${segundos / 3600} h"
}

/**
 * Idade da leitura pelo relógio do servidor (`consultadaEm − medidaEm`, research D4), nunca
 * negativa. `null` se algum instante for ilegível.
 */
fun idadeSegundos(consultadaEm: Instant?, medidaEm: String): Long? {
    val medida = instante(medidaEm) ?: return null
    val consulta = consultadaEm ?: return null
    return Duration.between(medida, consulta).seconds.coerceAtLeast(0)
}

private fun instante(iso: String): Instant? = try {
    OffsetDateTime.parse(iso).toInstant()
} catch (e: DateTimeParseException) {
    null
}
