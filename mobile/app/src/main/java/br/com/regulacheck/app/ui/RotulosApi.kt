package br.com.regulacheck.app.ui

import java.util.Locale

/**
 * Rótulos dos valores fechados da API (RC-19), copiados de `specs/015-fluxo-telas-app/data-model.md`
 * §2. Os valores chegam como texto: um valor que o app não conhece recebe um rótulo neutro e
 * nunca derruba a tela nem vira resultado negativo.
 */
object RotulosApi {

    /** Tipo documental informado pela paciente (`TipoDocumental`). */
    fun tipoDocumental(valor: String): String = when (valor) {
        "ENCAMINHAMENTO" -> "Encaminhamento"
        "IDENTIFICACAO" -> "Identificação ou cartão do SUS"
        "AUTORIZACAO_RESPONSAVEL" -> "Autorização do responsável"
        "OUTRO" -> "Outro documento"
        else -> "Documento"
    }

    /** Formato decidido pela API pelo conteúdo; JPEG aparece como "JPG", como no desenho (E2.2). */
    fun formato(valor: String): String = if (valor == "JPEG") "JPG" else valor

    /** Tamanho legível: "184 KB" ou "1,2 MB" (base 1024, uma casa decimal em MB). */
    fun tamanho(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
        else -> String.format(Locale("pt", "BR"), "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** "PDF · 184 KB · 2 páginas"; páginas só em PDF (textos E2.2). */
    fun detalheArquivo(formato: String, tamanhoBytes: Long, paginas: Int): String {
        val base = "${formato(formato)} · ${tamanho(tamanhoBytes)}"
        return if (formato == "PDF") "$base · $paginas ${if (paginas == 1) "página" else "páginas"}" else base
    }

    /** Versão do protocolo sem o prefixo "v" ("v0.1" → "0.1"), como nos quadros. */
    fun versaoProtocolo(valor: String): String =
        if (valor.length > 1 && (valor[0] == 'v' || valor[0] == 'V') && valor[1].isDigit()) valor.substring(1) else valor

    /** Situação da leitura de um documento (`DocumentoLido.situacao`), RC-19. */
    fun situacaoLeitura(valor: String): String = when (valor) {
        "LIDO" -> "Lido"
        "PROTEGIDO" -> "Não lido: PDF protegido"
        "LEITURA_VAZIA" -> "Não lido"
        else -> "Situação não reconhecida"
    }

    /** Campo lido ou avaliado (`campos[].codigo`, `evidencias[].campo`), em minúsculas para frases. */
    fun campo(valor: String): String = when (valor) {
        "TIPO_DOCUMENTO" -> "tipo do documento"
        "NOME" -> "nome"
        "DATA_NASCIMENTO" -> "data de nascimento"
        "MOTIVO_ENCAMINHAMENTO" -> "motivo do encaminhamento"
        "DATA_EMISSAO" -> "data de emissão"
        "AUTORIZACAO_ASSINADA" -> "autorização assinada"
        else -> "informação"
    }

    /**
     * Situação de uma evidência, para a frase "{campo} {situação}" (ex.: "data de nascimento
     * ilegível"). Sem rótulo na spec 015; criado na RC-19.
     */
    fun situacaoEvidencia(valor: String): String = when (valor) {
        "CONFIRMADO" -> "confirmado"
        "NAO_LOCALIZADO" -> "não localizado na leitura"
        "ILEGIVEL" -> "ilegível"
        "NAO_CONFERIDO" -> "não conferido"
        "DIVERGENCIA_APONTADA" -> "com diferença apontada"
        "DOCUMENTO_NAO_LIDO" -> "não lido"
        else -> "em situação não reconhecida"
    }

    /** Resultado por regra (F4 `Critério`). */
    fun resultadoRegra(valor: String): String = when (valor) {
        "ATENDE" -> "Atende"
        "PENDENTE" -> "Pendente"
        "NAO_VERIFICAVEL" -> "Não verificável"
        "NAO_APLICAVEL" -> "Não se aplica"
        else -> "Situação não reconhecida"
    }

    /** Responsável registrado pela regra, sem reescrever o valor da API. */
    fun responsavel(valor: String): String = when (valor) {
        "PACIENTE" -> "Você"
        "UNIDADE_EMISSORA" -> "Unidade emissora"
        else -> "Não informado"
    }

    /** "Quem resolve: você" / "Quem resolve: unidade emissora" (lista "O que falta", E3.3). */
    fun quemResolve(valor: String): String = "Quem resolve: ${responsavel(valor).replaceFirstChar { it.lowercase() }}"

    /** Motivo do não verificável. */
    fun motivo(valor: String): String = when (valor) {
        "CAMPO_ILEGIVEL" -> "Uma informação está ilegível"
        "DOCUMENTO_NAO_LIDO" -> "Um documento não pôde ser lido"
        "TRANSCRICAO_NAO_CONFERIDA" -> "A leitura ainda não foi conferida"
        "DIVERGENCIA_APONTADA" -> "Você apontou uma diferença"
        "VALOR_NAO_INTERPRETAVEL" -> "Um valor não pôde ser interpretado"
        "CONDICAO_INDETERMINADA" -> "Não foi possível saber se o critério se aplica"
        "DOCUMENTOS_INSUFICIENTES" -> "Faltam documentos para comparar"
        "DEPENDE_DE_DOCUMENTO_AUSENTE" -> "Depende de um documento que não foi enviado"
        else -> "Motivo não reconhecido"
    }
}
