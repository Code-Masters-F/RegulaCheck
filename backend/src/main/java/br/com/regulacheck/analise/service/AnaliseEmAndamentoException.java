package br.com.regulacheck.analise.service;

import br.com.regulacheck.common.exception.ExcecaoNegocio;

/**
 * Recusa de um pedido de analise porque outra analise do mesmo pedido esta em andamento dentro do limite de tempo
 * (409 {@code ANALISE_EM_ANDAMENTO}). Carrega quantos segundos faltam para o limite, que o controller publica no
 * cabecalho {@code Retry-After}.
 */
public class AnaliseEmAndamentoException extends ExcecaoNegocio {

    private static final long serialVersionUID = 1L;

    private final long segundosRestantes;

    /** @param segundosRestantes segundos ate o limite da analise em andamento; no minimo 1 */
    public AnaliseEmAndamentoException(long segundosRestantes) {
        super(409, "ANALISE_EM_ANDAMENTO",
                "A análise deste pedido já está em andamento. Aguarde alguns instantes e consulte o resultado.");
        this.segundosRestantes = Math.max(1, segundosRestantes);
    }

    /** @return segundos ate o limite da analise em andamento (no minimo 1) */
    public long segundosRestantes() {
        return segundosRestantes;
    }
}
