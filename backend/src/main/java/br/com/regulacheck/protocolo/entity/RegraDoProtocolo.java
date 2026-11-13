package br.com.regulacheck.protocolo.entity;

/**
 * Uma regra de uma versao do protocolo academico ficticio: uma linha de {@code RC_REGRA}. Os textos e o
 * responsavel pertencem a versao (mudar uma regra gera nova versao do protocolo); a logica de avaliacao fica no
 * motor ({@code validacao}), indexada pelo {@link #codigo()}. Modelo interno: nunca e serializado como resposta
 * publica.
 *
 * @param codigo codigo da regra na versao, ex.: {@code RC-R01}; unico dentro da versao
 * @param descricao descricao curta do criterio
 * @param evidenciaExigida evidencia que o criterio exige, em texto para pessoas
 * @param natureza obrigatoria ou condicional
 * @param condicaoAplicacao condicao de aplicacao em texto para pessoas; {@code null} quando obrigatoria
 * @param mensagem mensagem a paciente quando a regra fica pendente
 * @param responsavel quem provavelmente corrige quando a regra fica pendente
 * @param ordem ordem de avaliacao e de apresentacao dentro da versao
 */
public record RegraDoProtocolo(String codigo, String descricao, String evidenciaExigida, NaturezaRegra natureza,
        String condicaoAplicacao, String mensagem, ResponsavelCorrecao responsavel, int ordem) {
}
