package br.com.regulacheck.analise.service;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import br.com.regulacheck.analise.repository.AnaliseParaGravar;
import br.com.regulacheck.analise.repository.AnaliseRepository;
import br.com.regulacheck.analise.repository.Encerramento;
import br.com.regulacheck.analise.repository.InicioAnalise;
import br.com.regulacheck.analise.repository.PublicacaoAnalise;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.motor.ProtocoloNaoSuportadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso das analises do pedido da paciente (RC-13): pedir a analise e consultar o historico. Aplica a regra de
 * dona da RC-05 (pedido de outra paciente e pedido inexistente tem a mesma resposta).
 *
 * <p><strong>A analise e sincrona, em tres passos, sem transacao aberta durante a avaliacao</strong> (research D1):
 * <ol>
 *   <li>{@link AnaliseRepository#iniciar}: sob o bloqueio do pedido, recusa pedido submetido, analise em andamento
 *       dentro do {@linkplain #LIMITE_EM_ANDAMENTO limite} e pedido sem leitura atual; devolve a vigente se ela ja
 *       vale para a versao atual; senao grava {@code PROCESSANDO} e o instante de inicio, que identifica esta
 *       analise.</li>
 *   <li>{@link AvaliacaoService#avaliar}: a avaliacao da RC-12, que so le.</li>
 *   <li>{@link AnaliseRepository#publicar}: sob o bloqueio, so grava se esta ainda e a analise em andamento e se a
 *       revisao e o instante da conferencia ainda sao os avaliados; ou {@link AnaliseRepository#encerrar}, em falha
 *       ({@code FALHA_ANALISE}) ou descarte (estado da ultima analise concluida ou {@code RASCUNHO}).</li>
 * </ol>
 * Assim, um resultado calculado sobre documentos ou conferencia antigos nunca vira vigente, um segundo pedido
 * simultaneo nao inicia outra analise e uma analise que trava libera o pedido no maximo ao fim do limite.
 *
 * <p>Nada aqui chama IA nem gera resumo (RC-14). O conteudo dos documentos e dado nao confiavel: so identificadores,
 * codigos e contagens vao para o log. Este servico nao guarda dados de requisicao em campos.
 */
public class AnaliseService {

    /** Tempo maximo de uma analise em andamento; passado, ela e tratada como interrompida (falha recuperavel). */
    public static final Duration LIMITE_EM_ANDAMENTO = Duration.ofMinutes(1);

    /** Quantas analises, das mais recentes, a consulta do historico devolve. */
    public static final int LIMITE_HISTORICO = 50;

    private static final Logger LOG = LoggerFactory.getLogger(AnaliseService.class);

    private final AnaliseRepository analises;
    private final AvaliacaoService avaliacoes;
    private final Clock relogio;

    /**
     * Resultado de {@link #analisar}.
     *
     * @param analise analise gravada agora ou a vigente ja existente
     * @param criada {@code true} se esta chamada gravou a analise; {@code false} se devolveu a vigente
     */
    public record ResultadoAnalise(Analise analise, boolean criada) {
    }

    /**
     * @param analises persistencia das analises
     * @param avaliacoes avaliacao dos criterios (RC-12)
     * @param relogio relogio da aplicacao
     */
    public AnaliseService(AnaliseRepository analises, AvaliacaoService avaliacoes, Clock relogio) {
        this.analises = analises;
        this.avaliacoes = avaliacoes;
        this.relogio = relogio;
    }

    /**
     * Pede a analise da versao atual dos documentos do pedido da paciente, com a conferencia atual, e espera a
     * conclusao. Repetir sem mudanca nos documentos ou na conferencia devolve a mesma analise, sem gravar outra.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a analise gravada agora ({@code criada}) ou a vigente ja existente
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 409 {@code CONFLITO_ESTADO} se o pedido foi submetido, se nao ha leitura da versao atual, se
     *     documentos ou conferencia mudaram durante a analise ou se ela passou do limite e foi assumida por outra;
     *     409 {@link AnaliseEmAndamentoException} se outra analise do pedido esta em andamento; 503
     *     {@code ANALISE_INDISPONIVEL} se a avaliacao falhou (pedido em {@code FALHA_ANALISE}); 503
     *     {@code DEPENDENCIA_INDISPONIVEL} se o banco estiver indisponivel
     */
    public ResultadoAnalise analisar(SessaoAutenticada sessao, long solicitacaoId) {
        long pacienteId = sessao.usuarioId();
        Instant agora = agora();
        InicioAnalise inicio = analises.iniciar(pacienteId, solicitacaoId, agora, LIMITE_EM_ANDAMENTO);
        if (inicio.interrompeuAnterior()) {
            LOG.warn("Analise anterior passou do limite e foi encerrada como falha. solicitacaoId={} limiteSegundos={}",
                    solicitacaoId, LIMITE_EM_ANDAMENTO.toSeconds());
        }
        switch (inicio.situacao()) {
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
            case SUBMETIDO -> throw submetido();
            case EM_ANDAMENTO -> throw new AnaliseEmAndamentoException(
                    Duration.between(agora, inicio.instante().plus(LIMITE_EM_ANDAMENTO)).toSeconds());
            case VIGENTE -> {
                return new ResultadoAnalise(inicio.analise(), false);
            }
            case SEM_LEITURA_ATUAL -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Leia os documentos antes de pedir a análise.");
            case INICIADA -> {
                // segue para a avaliacao
            }
        }
        Instant iniciadaEm = inicio.instante();
        AvaliacaoDoPedido avaliacao = avaliarOuEncerrar(sessao, solicitacaoId, iniciadaEm);
        PublicacaoAnalise publicacao;
        try {
            publicacao = analises.publicar(pacienteId, solicitacaoId, iniciadaEm, paraGravar(avaliacao), agora());
        } catch (RuntimeException falha) {
            // Nada foi gravado (a publicacao e tudo ou nada). Se o banco voltou, o pedido nao fica preso ate o limite.
            encerrarSemEsconder(pacienteId, solicitacaoId, iniciadaEm, Encerramento.FALHA, falha);
            throw falha;
        }
        return switch (publicacao.situacao()) {
            case PUBLICADA -> {
                Analise gravada = publicacao.analise();
                LOG.info("Analise gravada. solicitacaoId={} analiseId={} revisao={} resultadoGeral={} pendentes={} "
                        + "naoVerificaveis={} duracaoMs={}", solicitacaoId, gravada.id(), gravada.revisao(),
                        gravada.resultadoGeral(), contar(gravada, Resultado.PENDENTE),
                        contar(gravada, Resultado.NAO_VERIFICAVEL),
                        Duration.between(iniciadaEm, gravada.executadaEm()).toMillis());
                yield new ResultadoAnalise(gravada, true);
            }
            case ENTRADA_MUDOU -> {
                LOG.info("Analise descartada: documentos ou conferencia mudaram durante a analise. solicitacaoId={} "
                        + "revisaoAvaliada={}", solicitacaoId, avaliacao.revisao());
                throw entradaMudou();
            }
            case SUBSTITUIDA -> {
                LOG.warn("Resultado de analise que passou do limite descartado. solicitacaoId={}", solicitacaoId);
                throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                        "A análise passou do tempo limite e não foi publicada. Consulte as análises ou peça de novo.");
            }
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
        };
    }

    /**
     * Devolve o historico de analises do pedido da paciente, da mais recente para a mais antiga (ate
     * {@link #LIMITE_HISTORICO}), com o retrato do pedido para dizer se ha analise em andamento.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return o historico (lista vazia se o pedido nunca foi analisado)
     * @throws ExcecaoNegocio 404 se o pedido nao existe ou e de outra paciente (resposta identica); 503 se o banco
     *     estiver indisponivel
     */
    public HistoricoDeAnalises consultar(SessaoAutenticada sessao, long solicitacaoId) {
        return analises.historico(sessao.usuarioId(), solicitacaoId, LIMITE_HISTORICO)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
    }

    /**
     * Diz se a analise em andamento ja passou do limite (interrompida), no relogio da aplicacao.
     *
     * @param iniciadaEm inicio da analise em andamento
     * @return {@code true} se o limite ja passou
     */
    public boolean interrompida(Instant iniciadaEm) {
        return !agora().isBefore(iniciadaEm.plus(LIMITE_EM_ANDAMENTO));
    }

    /**
     * Avalia; se a avaliacao recusar ou falhar, encerra a analise em andamento antes de responder. Recusa de
     * leitura (409) depois de iniciar so acontece se os documentos mudaram nesse meio-tempo: e descarte, nao falha.
     */
    private AvaliacaoDoPedido avaliarOuEncerrar(SessaoAutenticada sessao, long solicitacaoId, Instant iniciadaEm) {
        long pacienteId = sessao.usuarioId();
        try {
            return avaliacoes.avaliar(sessao, solicitacaoId);
        } catch (ProtocoloNaoSuportadoException naoSuportado) {
            encerrarSemEsconder(pacienteId, solicitacaoId, iniciadaEm, Encerramento.FALHA, naoSuportado);
            throw analiseIndisponivel();
        } catch (ExcecaoNegocio recusa) {
            if (recusa.status() == 409 || recusa.status() == 404) {
                encerrarSemEsconder(pacienteId, solicitacaoId, iniciadaEm, Encerramento.DESCARTE, recusa);
                throw recusa.status() == 404 ? recusa : entradaMudou();
            }
            encerrarSemEsconder(pacienteId, solicitacaoId, iniciadaEm, Encerramento.FALHA, recusa);
            throw recusa;
        } catch (RuntimeException inesperada) {
            // Sem a mensagem da falha no log: ela pode refletir o conteudo de um documento.
            LOG.error("Falha inesperada na avaliacao. solicitacaoId={} tipo={}", solicitacaoId,
                    inesperada.getClass().getSimpleName());
            encerrarSemEsconder(pacienteId, solicitacaoId, iniciadaEm, Encerramento.FALHA, inesperada);
            throw analiseIndisponivel();
        }
    }

    /**
     * Encerra a analise em andamento sem trocar a falha original por outra: se nem o encerramento puder ser gravado
     * (banco fora do ar), o limite de tempo libera o pedido.
     */
    private void encerrarSemEsconder(long pacienteId, long solicitacaoId, Instant iniciadaEm, Encerramento tipo,
            RuntimeException original) {
        try {
            boolean encerrou = analises.encerrar(pacienteId, solicitacaoId, iniciadaEm, tipo, agora());
            LOG.info("Analise encerrada sem resultado. solicitacaoId={} encerramento={} aplicado={} causa={}",
                    solicitacaoId, tipo, encerrou, original.getClass().getSimpleName());
        } catch (RuntimeException falhaAoEncerrar) {
            LOG.warn("Nao foi possivel encerrar a analise; o limite de tempo liberara o pedido. solicitacaoId={} "
                    + "tipo={}", solicitacaoId, falhaAoEncerrar.getClass().getSimpleName());
        }
    }

    private static AnaliseParaGravar paraGravar(AvaliacaoDoPedido avaliacao) {
        return new AnaliseParaGravar(avaliacao.revisao(), avaliacao.protocolo().id(), avaliacao.extracaoId(),
                avaliacao.conferenciaAtualizadaEm(), avaliacao.dataReferencia(), avaliacao.avaliacao());
    }

    private static long contar(Analise analise, Resultado resultado) {
        return analise.resultados().stream().filter(r -> r.resultado() == resultado).count();
    }

    private static ExcecaoNegocio submetido() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Este pedido já foi submetido e só pode ser consultado.");
    }

    private static ExcecaoNegocio entradaMudou() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                "Os documentos ou a conferência mudaram durante a análise. Confira e peça a análise de novo.");
    }

    private static ExcecaoNegocio analiseIndisponivel() {
        return new ExcecaoNegocio(503, "ANALISE_INDISPONIVEL",
                "Não foi possível concluir a análise agora. Nada foi alterado nos seus documentos; tente novamente.");
    }

    /** Instante atual na precisao que o banco guarda. */
    private Instant agora() {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
