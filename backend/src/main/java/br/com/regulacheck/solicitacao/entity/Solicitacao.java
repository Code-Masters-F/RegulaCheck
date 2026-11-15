package br.com.regulacheck.solicitacao.entity;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.validacao.model.ResultadoGeral;
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
 * @param resultadoVigente resultado geral da analise vigente <strong>da revisao atual</strong> (RC-13); {@code null}
 *     quando nenhuma analise vale para a versao atual dos documentos (nunca analisado, em andamento, ou a vigente
 *     deixou de valer porque documentos ou conferencia mudaram)
 */
public record Solicitacao(long id, long pacienteId, Protocolo protocolo, EstadoSolicitacao estado,
        int revisaoAtual, Instant criadaEm, Instant atualizadaEm, int documentosDisponiveis,
        ResultadoGeral resultadoVigente) {

    /**
     * Cria o pedido sem analise vigente da revisao atual, como em todas as operacoes que nao leem analises.
     *
     * @param id identificador interno do pedido
     * @param pacienteId conta dona do pedido
     * @param protocolo servico escolhido
     * @param estado estado atual
     * @param revisaoAtual numero da revisao vigente
     * @param criadaEm instante de criacao
     * @param atualizadaEm instante da ultima alteracao relevante
     * @param documentosDisponiveis quantos documentos disponiveis o pedido tem
     */
    public Solicitacao(long id, long pacienteId, Protocolo protocolo, EstadoSolicitacao estado, int revisaoAtual,
            Instant criadaEm, Instant atualizadaEm, int documentosDisponiveis) {
        this(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm, documentosDisponiveis, null);
    }

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
        this(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm, 0, null);
    }

    /**
     * @param quantidade novo total de documentos disponiveis
     * @return copia do pedido com a contagem atualizada
     */
    public Solicitacao comDocumentos(int quantidade) {
        return new Solicitacao(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm, quantidade,
                resultadoVigente);
    }

    /**
     * @param resultado resultado geral da analise vigente da revisao atual, ou {@code null} se nao ha
     * @return copia do pedido com o resultado vigente informado
     */
    public Solicitacao comResultadoVigente(ResultadoGeral resultado) {
        return new Solicitacao(id, pacienteId, protocolo, estado, revisaoAtual, criadaEm, atualizadaEm,
                documentosDisponiveis, resultado);
    }
}
