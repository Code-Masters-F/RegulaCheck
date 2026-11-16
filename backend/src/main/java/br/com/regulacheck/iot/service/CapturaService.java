package br.com.regulacheck.iot.service;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.SessaoCaptura;
import br.com.regulacheck.iot.repository.AberturaSessao;
import br.com.regulacheck.iot.repository.CapturaRepository;
import br.com.regulacheck.iot.repository.NovaLeitura;
import br.com.regulacheck.iot.repository.TelemetriaDoPedido;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso da estacao de captura simulada (RC-21 + RC-22): abrir a sessao de captura, registrar a leitura
 * enviada pelo simulador e consultar a telemetria do pedido.
 *
 * <p>Autorizacao por recurso: abrir sessao e consultar telemetria usam a paciente da sessao autenticada e a regra
 * de dona da RC-05 (pedido de outra paciente e pedido inexistente tem a mesma resposta). O registro de leitura e
 * chamado so depois que o {@code SimuladorFiltro} conferiu a chave do simulador; a chave e unica por ambiente e
 * nao identifica a paciente, entao o vinculo verificado e o da sessao de captura existente e aberta.
 *
 * <p>Nada aqui muda documentos, revisao, leitura, analise ou estado do pedido: a telemetria e apoio a captura e
 * nao bloqueia nenhum outro fluxo (FR-020). Logs so com ids, tipo e codigos; nunca o valor medido nem a chave.
 * Este servico nao guarda dados de requisicao em campos.
 */
public class CapturaService {

    private static final Logger LOG = LoggerFactory.getLogger(CapturaService.class);

    private final CapturaRepository capturas;
    private final Clock relogio;

    /**
     * Desfecho de {@link #abrirSessao}.
     *
     * @param sessao a sessao aberta
     * @param criada {@code true} se foi criada agora; {@code false} se ja estava aberta
     */
    public record SessaoAberta(SessaoCaptura sessao, boolean criada) {
    }

    /**
     * @param capturas persistencia das sessoes e leituras
     * @param relogio relogio da aplicacao (UTC)
     */
    public CapturaService(CapturaRepository capturas, Clock relogio) {
        this.capturas = capturas;
        this.relogio = relogio;
    }

    /**
     * Abre a sessao de captura do pedido da paciente. Idempotente: se o pedido ja tem sessao aberta, ela e
     * devolvida e nada e gravado (toque duplicado e chamadas simultaneas criam no maximo uma sessao).
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a sessao e se foi criada agora
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 409 {@code CONFLITO_ESTADO} se o pedido ja foi submetido; 503 {@code DEPENDENCIA_INDISPONIVEL}
     */
    public SessaoAberta abrirSessao(SessaoAutenticada sessao, long solicitacaoId) {
        AberturaSessao abertura = capturas.abrirSessao(solicitacaoId, sessao.usuarioId(), relogio.instant());
        return switch (abertura.situacao()) {
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
            case SUBMETIDO -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido já foi submetido e só pode ser consultado.");
            case EXISTENTE -> new SessaoAberta(abertura.sessao(), false);
            case CRIADA -> {
                LOG.info("Sessao de captura aberta. solicitacaoId={} sessaoId={}", solicitacaoId,
                        abertura.sessao().id());
                yield new SessaoAberta(abertura.sessao(), true);
            }
        };
    }

    /**
     * Confere se a sessao de captura existe e esta aberta, antes de ler o corpo da requisicao do simulador (ordem
     * 404 antes de 400). A gravacao confere de novo sob bloqueio.
     *
     * @param sessaoId sessao indicada na rota
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se a sessao nao existe ou esta encerrada (mesma resposta)
     */
    public void exigirSessaoAberta(long sessaoId) {
        if (!capturas.sessaoAbertaExiste(sessaoId)) {
            throw sessaoNaoEncontrada(sessaoId);
        }
    }

    /**
     * Grava a leitura validada. So confirma depois de gravada.
     *
     * @param nova leitura validada pelo {@code LeituraSensorRequest}
     * @return a leitura gravada
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se a sessao deixou de existir ou foi encerrada; 503
     *     {@code DEPENDENCIA_INDISPONIVEL}
     */
    public LeituraSensor registrarLeitura(NovaLeitura nova) {
        LeituraSensor gravada = capturas.gravarLeitura(nova).orElseThrow(() -> sessaoNaoEncontrada(nova.sessaoId()));
        LOG.info("Leitura de sensor gravada. sessaoId={} leituraId={} tipo={}", gravada.sessaoId(), gravada.id(),
                gravada.tipo());
        return gravada;
    }

    /**
     * Consulta a telemetria do pedido da paciente. Sem sessao ou sem leitura a resposta e de sucesso, com os
     * sensores em {@code SEM_LEITURA}: a indisponibilidade dos sensores nunca vira erro para a paciente.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return sessao mais recente e ultima leitura por sensor
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 503 {@code DEPENDENCIA_INDISPONIVEL}
     */
    public TelemetriaDoPedido consultarTelemetria(SessaoAutenticada sessao, long solicitacaoId) {
        return Propriedade.exigir(capturas.telemetriaDaPaciente(solicitacaoId, sessao.usuarioId()), sessao);
    }

    /**
     * @return instante atual do relogio da aplicacao
     */
    public Instant agora() {
        return relogio.instant();
    }

    private static ExcecaoNegocio sessaoNaoEncontrada(long sessaoId) {
        LOG.info("Sessao de captura inexistente ou encerrada. sessaoId={} codigo=NAO_ENCONTRADO", sessaoId);
        return Erros.naoEncontrado();
    }
}
