package br.com.regulacheck.extracao.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia das decisoes da paciente sobre os campos de uma leitura (conferencia da transcricao). E uma
 * interface por ser a fronteira com o banco. Toda operacao recebe a conta dona, o pedido e a leitura e os aplica na
 * propria consulta: pedido de outra paciente, pedido inexistente e leitura de outro pedido sao indistinguiveis
 * (regra de dona da RC-05).
 */
public interface ConferenciaRepository {

    /**
     * Busca, sem bloqueio, a leitura do pedido da paciente com as decisoes vigentes e a versao atual do pedido.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @return a leitura e as decisoes, ou vazio se o pedido nao existe, nao e da paciente ou a leitura nao e dele
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<ConferenciaRegistrada> buscar(long pacienteId, long solicitacaoId, long extracaoId);

    /**
     * Registra as decisoes de forma atomica e sob o bloqueio da linha do pedido. Na mesma transacao: confere a
     * dona, a leitura e o estado ({@code SUBMETIDA_SIMULACAO} recusa), confere que a leitura ainda e a da versao
     * atual dos documentos (lida <em>sob o bloqueio</em>), confere cada decisao contra a situacao do campo e so
     * entao grava (decidir de novo substitui; repetir a mesma nao altera nada). Qualquer recusa desfaz tudo: ou
     * ficam todas as decisoes ou nenhuma. O estado, a revisao e a leitura original nao mudam.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @param decisoes decisoes ja validadas no formato (sem repeticao de par documento/campo)
     * @param agora instante das decisoes que mudarem
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoRegistroDecisao registrar(long pacienteId, long solicitacaoId, long extracaoId,
            List<DecisaoNova> decisoes, Instant agora);
}
