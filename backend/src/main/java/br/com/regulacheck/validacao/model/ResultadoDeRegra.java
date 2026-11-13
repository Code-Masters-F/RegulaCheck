package br.com.regulacheck.validacao.model;

import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;

/**
 * Uma regra da versao do protocolo e o veredito dela para o pedido.
 *
 * @param regra regra da versao (codigo, textos, natureza, responsavel)
 * @param veredito resultado, motivo e evidencias
 */
public record ResultadoDeRegra(RegraDoProtocolo regra, Veredito veredito) {

    /** @return o resultado da regra */
    public Resultado resultado() {
        return veredito.resultado();
    }
}
