package br.com.regulacheck.iot.repository;

import br.com.regulacheck.iot.entity.LeituraSensor;
import java.time.Instant;
import java.util.Optional;

/**
 * Persistencia das sessoes de captura e das leituras dos sensores simulados (RC-21/RC-22).
 *
 * <p>Interface por ser a fronteira com o banco (o teste usa um dobro em memoria com as mesmas regras). Toda
 * operacao em nome da paciente filtra a dona do pedido na propria consulta: pedido de outra paciente e pedido
 * inexistente tem o mesmo desfecho. Falhas de acesso ao banco viram 503 {@code DEPENDENCIA_INDISPONIVEL}.
 */
public interface CapturaRepository {

    /**
     * Abre a sessao de captura do pedido da paciente, ou devolve a que ja esta aberta. Roda sob o mesmo bloqueio
     * do pedido usado por envio de documento, troca de servico, analise e exclusao do rascunho, entao chamadas
     * simultaneas criam no maximo uma sessao.
     *
     * @param solicitacaoId pedido
     * @param pacienteId paciente da sessao autenticada
     * @param agora instante de inicio da sessao (UTC)
     * @return {@code NAO_ENCONTRADO}, {@code SUBMETIDO}, {@code EXISTENTE} (nada gravado) ou {@code CRIADA}
     */
    AberturaSessao abrirSessao(long solicitacaoId, long pacienteId, Instant agora);

    /**
     * Consulta, sem bloqueio, se a sessao existe e esta aberta. Usada so para responder 404 antes de ler o corpo
     * da requisicao; a gravacao confere de novo sob bloqueio.
     *
     * @param sessaoId sessao indicada pelo simulador
     * @return {@code true} se existe e esta aberta
     */
    boolean sessaoAbertaExiste(long sessaoId);

    /**
     * Grava a leitura se a sessao ainda existe e esta aberta. A conferencia e o {@code INSERT} acontecem sob o
     * bloqueio do pedido dono da sessao, para que nenhuma leitura chegue no meio da exclusao do rascunho.
     *
     * @param nova leitura validada
     * @return a leitura gravada; vazio se a sessao nao existe ou esta encerrada (nada gravado)
     */
    Optional<LeituraSensor> gravarLeitura(NovaLeitura nova);

    /**
     * Le a telemetria do pedido da paciente, sem escrever nada.
     *
     * @param solicitacaoId pedido
     * @param pacienteId paciente da sessao autenticada
     * @return vazio se o pedido nao existe ou e de outra paciente; senao a sessao mais recente (ou nenhuma) e a
     *     ultima leitura de cada sensor
     */
    Optional<TelemetriaDoPedido> telemetriaDaPaciente(long solicitacaoId, long pacienteId);
}
