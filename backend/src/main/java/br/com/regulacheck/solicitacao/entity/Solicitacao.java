package br.com.regulacheck.solicitacao.entity;

import br.com.regulacheck.protocolo.entity.Protocolo;
import java.time.Instant;

/**
 * Pedido de uma paciente, ja lido do banco junto com o servico (protocolo) escolhido. Modelo interno: nunca e
 * serializado como resposta publica (usar {@code SolicitacaoResponse}).
 *
 * @param id identificador interno do pedido
 * @param pacienteId conta dona do pedido; vem sempre da sessao autenticada
 * @param protocolo servico escolhido
 * @param estado estado atual (na RC-07, sempre {@link EstadoSolicitacao#RASCUNHO} ao criar)
 * @param revisaoAtual numero da revisao vigente, a partir de 1
 * @param criadaEm instante de criacao, em UTC
 * @param atualizadaEm instante da ultima alteracao relevante, em UTC
 */
public record Solicitacao(long id, long pacienteId, Protocolo protocolo, EstadoSolicitacao estado,
        int revisaoAtual, Instant criadaEm, Instant atualizadaEm) {
}
