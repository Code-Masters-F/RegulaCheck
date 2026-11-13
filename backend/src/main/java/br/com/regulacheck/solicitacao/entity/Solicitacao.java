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
 * @param revisaoAtual numero da revisao vigente, a partir de 1; tambem e a versao do conjunto documental (RC-08)
 * @param criadaEm instante de criacao, em UTC
 * @param atualizadaEm instante da ultima alteracao relevante, em UTC
 * @param documentosDisponiveis quantos documentos disponiveis o pedido tem (RC-08); {@code 0} num pedido novo
 */
public record Solicitacao(long id, long pacienteId, Protocolo protocolo, EstadoSolicitacao estado,
        int revisaoAtual, Instant criadaEm, Instant atualizadaEm, int documentosDisponiveis) {

    /**
     * Cria o pedido sem documentos (contagem {@code 0}), como na criacao do rascunho.
     *
     * @param id identificador interno do pedido
     * @param pacienteId conta dona do pedido
     * @param protocolo servico escolhido
     * @param estado estado atual
     * @param revisaoAtual numero da revisao vigente
     * @param criadaEm instante de criacao
     * @param atualizadaEm instante da ultima alteracao relevante
     */
    public Solicitacao(long id, long pacienteId, Protocolo protocolo, EstadoSolicitacao estado, int revisaoAtual,
            Instant criadaEm, Instant atualizadaEm) {
        this(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm, 0);
    }

    /**
     * @param quantidade novo total de documentos disponiveis
     * @return copia do pedido com a contagem atualizada
     */
    public Solicitacao comDocumentos(int quantidade) {
        return new Solicitacao(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm, quantidade);
    }
}
