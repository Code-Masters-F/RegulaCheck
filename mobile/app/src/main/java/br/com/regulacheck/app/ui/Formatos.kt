package br.com.regulacheck.app.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))

/**
 * Formata um instante ISO-8601 da API ("2026-11-12T13:06:00Z") como "12/11/2026 às 10:06" no
 * fuso do aparelho. Valor ilegível é devolvido como veio (texto puro), sem falhar a tela.
 */
fun formatarDataHora(iso: String, fuso: ZoneId = ZoneId.systemDefault()): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(fuso).format(FORMATO_DATA_HORA)
} catch (e: DateTimeParseException) {
    iso
}
