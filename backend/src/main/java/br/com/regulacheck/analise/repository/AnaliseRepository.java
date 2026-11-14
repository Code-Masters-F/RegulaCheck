package br.com.regulacheck.analise.repository;

import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Persistencia das analises de um pedido e da analise em andamento. E uma interface por ser a fronteira com o
 * banco (JDBC em producao, memoria nos testes). Toda operacao recebe a conta dona e o pedido e os aplica na propria
 * consulta: pedido de outra paciente e pedido inexistente sao indistinguiveis (regra de dona da RC-05).
 *
 * <p>As operacoes de escrita sao transacoes curtas sob o bloqueio da linha do pedido (o mesmo usado pelas escritas
 * de documentos, leitura e conferencia), sem chamada externa: a avaliacao acontece entre {@link #iniciar} e
 * {@link #publicar}/{@link #encerrar}, sem conexao aberta. O instante de inicio gravado em {@link #iniciar}
 * identifica a analise em andamento: so quem o tem pode publicar ou encerrar.
 */
public interface AnaliseRepository {

    /**
     * Comeca uma analise, se puder. Sob o bloqueio do pedido, na ordem: dona; {@code SUBMETIDA_SIMULACAO} recusa;
     * {@code PROCESSANDO} dentro do limite recusa (passado o limite, a analise anterior e encerrada como falha e esta
     * continua); analise vigente da revisao atual e devolvida; sem leitura da revisao atual recusa; senao grava
     * {@code ESTADO = PROCESSANDO} e o instante de inicio.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param agora instante atual (vira o inicio, se iniciar)
     * @param limite tempo maximo de uma analise em andamento
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel ou sem o
     *     script {@code 10_}
     */
    InicioAnalise iniciar(long pacienteId, long solicitacaoId, Instant agora, Duration limite);

    /**
     * Grava a analise, se a entrada ainda for a avaliada. Sob o bloqueio do pedido: confere que a analise em
     * andamento ainda e esta ({@code iniciadaEm}; senao nada muda); confere que ela nao passou do {@code limite}
     * (senao o resultado tardio nao e publicado e o pedido vai a {@code FALHA_ANALISE}); confere que a revisao atual
     * e o instante atual da conferencia sao os avaliados (senao encerra como {@link Encerramento#DESCARTE}); entao,
     * na mesma transacao, tira a
     * vigencia da anterior, grava a analise, um resultado por regra, as pendencias com motivo e evidencias, poe o
     * pedido no resultado geral e devolve a analise lida de volta. Ou existe tudo ou nada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param iniciadaEm instante de inicio devolvido por {@link #iniciar}
     * @param analise avaliacao e a entrada que ela considerou
     * @param agora data de execucao
     * @param limite tempo maximo de uma analise em andamento; o resultado so e publicado antes de
     *     {@code iniciadaEm + limite}
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    PublicacaoAnalise publicar(long pacienteId, long solicitacaoId, Instant iniciadaEm, AnaliseParaGravar analise,
            Instant agora, Duration limite);

    /**
     * Encerra sem resultado a analise em andamento, se ainda for esta ({@code iniciadaEm}); senao nao faz nada.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param iniciadaEm instante de inicio devolvido por {@link #iniciar}
     * @param tipo falha ({@code FALHA_ANALISE}) ou descarte (estado da analise concluida mais recente ou
     *     {@code RASCUNHO})
     * @param agora instante do encerramento
     * @return {@code true} se encerrou; {@code false} se a analise em andamento ja nao era esta
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    boolean encerrar(long pacienteId, long solicitacaoId, Instant iniciadaEm, Encerramento tipo, Instant agora);

    /**
     * Le, sem bloqueio e num unico retrato do banco, o historico de analises do pedido da paciente.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido
     * @param limite quantas analises, das mais recentes, devolver
     * @return o historico, ou vazio se o pedido nao existe ou nao e da paciente
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel ou sem o
     *     script {@code 10_}
     */
    Optional<HistoricoDeAnalises> historico(long pacienteId, long solicitacaoId, int limite);
}
