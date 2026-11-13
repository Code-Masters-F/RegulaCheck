package br.com.regulacheck.extracao.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia das leituras (simuladas) dos documentos de um pedido. E uma interface por ser a fronteira com o
 * banco. Toda operacao recebe a conta dona e o pedido e os aplica na propria consulta: pedido de outra paciente e
 * pedido inexistente sao indistinguiveis (regra de dona da RC-05).
 */
public interface ExtracaoRepository {

    /**
     * Le, sem bloqueio, o retrato do pedido da paciente: estado, versao do conjunto documental e documentos
     * disponiveis.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @return o retrato, ou vazio se o pedido nao existe ou nao e da paciente
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<ContextoLeitura> contexto(long pacienteId, long solicitacaoId);

    /**
     * Busca a leitura mais recente do pedido, com a versao atual do pedido para classificar atual/desatualizada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @return a leitura, ou vazio se o pedido nao existe, nao e da paciente ou nunca foi lido
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<ExtracaoRegistrada> buscarMaisRecente(long pacienteId, long solicitacaoId);

    /**
     * Grava a leitura de uma versao do conjunto documental, de forma atomica e sob o bloqueio da linha do pedido.
     * Na mesma transacao: confere a dona e o estado ({@code SUBMETIDA_SIMULACAO} recusa), confere que a revisao
     * atual ainda e a capturada, procura leitura ja existente para essa versao (existe: devolve-a) e, so entao,
     * grava a leitura, a de cada documento e seus campos. O estado e a revisao do pedido nao mudam. Ou existe tudo
     * ou nada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param revisaoCapturada versao do conjunto documental em que os documentos foram lidos
     * @param leituras leitura de cada documento disponivel naquela versao, ja validada
     * @param agora instante da leitura
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoGravacao gravar(long pacienteId, long solicitacaoId, int revisaoCapturada,
            List<LeituraParaGravar> leituras, Instant agora);
}
