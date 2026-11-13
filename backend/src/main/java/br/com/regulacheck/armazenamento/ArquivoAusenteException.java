package br.com.regulacheck.armazenamento;

/**
 * O arquivo pedido nao esta na area privada (nunca foi guardado, foi removido ou o identificador aponta
 * para fora dela). A mensagem nao contem caminho; quem trata a falha devolve o erro interno padrao e o
 * detalhe tecnico fica so no log.
 */
public final class ArquivoAusenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Cria a excecao com mensagem fixa, sem caminho nem identificador. */
    public ArquivoAusenteException() {
        super("Arquivo nao encontrado na area privada.");
    }
}
