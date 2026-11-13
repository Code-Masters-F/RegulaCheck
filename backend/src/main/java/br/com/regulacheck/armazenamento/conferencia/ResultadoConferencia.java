package br.com.regulacheck.armazenamento.conferencia;

/**
 * Resultado da conferencia de um arquivo: aceito (com o arquivo ainda em quarentena) ou recusado
 * por um motivo da lista fechada {@link MotivoRecusa}.
 */
public sealed interface ResultadoConferencia {

    /**
     * Arquivo aceito.
     *
     * @param arquivo arquivo em quarentena, pronto para ser guardado
     */
    record Aceito(ArquivoConferido arquivo) implements ResultadoConferencia {
    }

    /**
     * Arquivo recusado; nada permanece gravado.
     *
     * @param motivo motivo da recusa
     */
    record Recusado(MotivoRecusa motivo) implements ResultadoConferencia {
    }
}
