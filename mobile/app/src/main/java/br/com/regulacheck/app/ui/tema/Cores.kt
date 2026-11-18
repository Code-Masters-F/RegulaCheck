package br.com.regulacheck.app.ui.tema

import androidx.compose.ui.graphics.Color

/**
 * Tokens semânticos de cor do design system v1.0 (`mobile/design/design-system.md`, seção Cor).
 * Só os usados pelas telas da RC-18; contrastes conferidos em `mobile/design/acessibilidade.md`.
 * O acento laranja `rc-accent` não entra: nunca é texto nem ícone isolado.
 */
object CoresRc {
    val Fundo = Color(0xFFFFFFFF)
    val Texto = Color(0xFF112B3C) // rc-text
    val TextoApoio = Color(0xFF526774) // rc-text-muted
    val Acao = Color(0xFF205375) // rc-action / rc-focus
    val NaAcao = Color(0xFFFFFFFF) // rc-on-action
    val Pendencia = Color(0xFFB44809) // rc-pending (ícone)
    val Sucesso = Color(0xFF176448) // rc-success (ícone)
    val Erro = Color(0xFFB42335) // rc-error
    val FundoErro = Color(0xFFFDEDF0) // rc-error-bg
    val Superficie = Color(0xFFEFEFEF) // rc-surface
    val FundoInfo = Color(0xFFEAF2F8) // rc-info-bg
    val Borda = Color(0xFF82929B) // rc-border
    val Divisor = Color(0xFFDAE2E7) // rc-divider / rc-skeleton
    val Simulado = Color(0xFF8D3C0C) // rc-simulated-fg (selo SIMULADO, 6,82:1 sobre o fundo)
    val FundoSimulado = Color(0xFFFFF1E6) // rc-simulated-bg
    val Desabilitado = Color(0xFFE9EEF1)
}
