package br.com.regulacheck.validacao.regra;

import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Veredito;

/**
 * Logica de uma regra do protocolo (Strategy), indexada pelo codigo que a regra tem nas versoes do protocolo. Os
 * textos e o responsavel ficam na versao ({@code RC_REGRA}); aqui fica so como decidir o resultado.
 *
 * <p>Toda implementacao e deterministica e pura: nao le relogio, banco, arquivo nem rede, e trata valores e
 * trechos dos documentos apenas como dados. Uma evidencia nao verificavel nunca leva a {@code ATENDE} nem a
 * {@code PENDENTE} quando a decisao depende dela.
 */
public interface RegraDocumental {

    /** @return codigo da regra nas versoes do protocolo, ex.: {@code RC-R01} */
    String codigo();

    /** @return natureza esperada; o motor recusa a versao se a natureza cadastrada for outra */
    NaturezaRegra natureza();

    /**
     * Avalia a regra.
     *
     * @param entrada documentos e data de referencia
     * @return resultado, motivo (se nao verificavel) e evidencias; nunca {@code null}
     */
    Veredito avaliar(EntradaAvaliacao entrada);
}
