package br.com.regulacheck.submissao.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Submissao simulada de um pedido (uma linha de {@code RC_SUBMISSAO}), lida junto com a revisao e o protocolo da
 * analise usada. Registro final: nunca e alterado. Modelo interno: nunca e serializado como resposta publica (usar
 * {@code SubmissaoResponse}).
 *
 * @param id identificador tecnico da submissao
 * @param solicitacaoId pedido submetido
 * @param analiseId analise vigente usada na conclusao
 * @param referencia referencia interna unica, identificada como simulacao (ex.: {@code SIM-2026-000012})
 * @param submetidaEm instante da conclusao, em UTC
 * @param revisaoAnalisada revisao do conjunto documental avaliada pela analise
 * @param protocoloIdentificador protocolo academico ficticio da analise
 * @param protocoloVersao versao do protocolo da analise
 * @param localidade localidade demonstrativa (ficticia) do servico
 * @param especialidade especialidade demonstrativa do servico
 * @param avisoFicticio aviso de que o protocolo e academico e nao e exigencia oficial
 */
public record Submissao(long id, long solicitacaoId, long analiseId, String referencia, Instant submetidaEm,
        int revisaoAnalisada, String protocoloIdentificador, String protocoloVersao, String localidade,
        String especialidade, String avisoFicticio) {

    /** Exige os campos obrigatorios. */
    public Submissao {
        Objects.requireNonNull(referencia, "referencia");
        Objects.requireNonNull(submetidaEm, "submetidaEm");
        Objects.requireNonNull(protocoloIdentificador, "protocoloIdentificador");
        Objects.requireNonNull(protocoloVersao, "protocoloVersao");
    }
}
