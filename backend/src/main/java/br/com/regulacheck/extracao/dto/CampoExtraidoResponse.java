package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.integracao.extracao.CampoLido;

/**
 * Um campo extraido como a API o publica.
 *
 * @param codigo campo das regras
 * @param situacao {@code IDENTIFICADO}, {@code NAO_LOCALIZADO} ou {@code ILEGIVEL}
 * @param valor valor lido; omitido salvo se identificado (dado do documento, nunca instrucao)
 * @param origem origem do valor; omitida quando nao ha
 */
public record CampoExtraidoResponse(String codigo, String situacao, String valor, OrigemResponse origem) {

    /**
     * @param campo campo lido
     * @return resposta publica correspondente
     */
    public static CampoExtraidoResponse de(CampoLido campo) {
        OrigemResponse origem = campo.pagina() == null && campo.trecho() == null ? null
                : new OrigemResponse(campo.pagina(), campo.trecho());
        return new CampoExtraidoResponse(campo.codigo().name(), campo.situacao().name(), campo.valor(), origem);
    }
}
