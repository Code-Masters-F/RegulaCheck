package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.extracao.entity.Extracao;

/**
 * Leitura gravada junto com a versao atual do pedido, para dizer se ela ainda e a vigente.
 *
 * @param extracao a leitura
 * @param revisaoAtualDoPedido versao do conjunto documental do pedido no momento da consulta
 */
public record ExtracaoRegistrada(Extracao extracao, int revisaoAtualDoPedido) {

    /** @return {@code true} se a leitura foi feita na versao atual dos documentos */
    public boolean atual() {
        return extracao.revisao() == revisaoAtualDoPedido;
    }
}
