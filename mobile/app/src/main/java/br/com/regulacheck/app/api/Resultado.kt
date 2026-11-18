package br.com.regulacheck.app.api

/**
 * Resultado de uma chamada à API: o valor desserializado ou um [ErroApi]. Nenhuma chamada lança
 * exceção de rede para a tela.
 */
sealed interface Resultado<out T> {
    data class Sucesso<T>(val valor: T) : Resultado<T>
    data class Falha(val erro: ErroApi) : Resultado<Nothing>
}
