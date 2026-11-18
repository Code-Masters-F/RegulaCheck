package br.com.regulacheck.submissao.repository;

import br.com.regulacheck.submissao.entity.Submissao;
import java.time.Instant;
import java.util.Optional;

/**
 * Persistencia da submissao simulada (RC-17). E uma interface por ser a fronteira com o banco (JDBC em producao,
 * memoria nos testes). Toda operacao recebe a conta dona e a aplica na propria consulta: pedido de outra paciente e
 * pedido inexistente sao indistinguiveis (regra de dona da RC-05).
 */
public interface SubmissaoRepository {

    /**
     * Conclui a simulacao do pedido, se ele estiver pronto. Numa transacao curta, sob o bloqueio da linha do pedido (o
     * mesmo das alteracoes de documento, conferencia e analise), nesta ordem: dona; submissao ja existente (devolvida,
     * sem gravar outra); pedido em {@code SUBMETIDA_SIMULACAO} sem registro (somente consulta); {@code PROCESSANDO}
     * (em andamento); analise vigente da revisao atual (senao, sem analise vigente); resultado geral
     * {@code SEM_PENDENCIAS_IDENTIFICADAS} e nenhum resultado {@code PENDENTE} ou {@code NAO_VERIFICAVEL} nessa analise
     * (senao, com pendencias); pedido em {@code SEM_PENDENCIAS_IDENTIFICADAS} (senao, sem analise vigente: estado
     * incoerente com a vigente). So entao grava a submissao ligada a essa analise
     * e poe o pedido em {@code SUBMETIDA_SIMULACAO}, na mesma transacao. Ou existe tudo ou nada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param referencia referencia interna calculada pelo Service; so e usada se a submissao for gravada agora
     * @param agora instante da conclusao (relogio da aplicacao, na precisao do banco)
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoSubmissao submeter(long pacienteId, long solicitacaoId, String referencia, Instant agora);

    /**
     * Le a submissao do pedido da paciente, com a revisao e o protocolo da analise usada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @return a submissao, ou vazio se o pedido nao existe, e de outra paciente ou ainda nao foi submetido
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Submissao> buscar(long pacienteId, long solicitacaoId);
}
