package br.com.regulacheck.submissao.service;

import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.submissao.entity.Submissao;
import br.com.regulacheck.submissao.repository.ResultadoSubmissao;
import br.com.regulacheck.submissao.repository.SubmissaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso da submissao simulada (RC-17): concluir a simulacao de um pedido pronto e consultar o comprovante.
 * Aplica a regra de dona da RC-05 (pedido de outra paciente e pedido inexistente tem a mesma resposta).
 *
 * <p><strong>A submissao e simulada.</strong> Nada e enviado ao SUS, nao ha fila real nem numero oficial de protocolo;
 * a referencia e interna e identificada como simulacao. A exigencia de analise vigente sem pendencias e uma restricao
 * do simulador academico, nao regra assistencial: as recusas dizem isso a paciente.
 *
 * <p>A decisao (dona, submissao existente, estado, analise vigente da revisao atual e seus resultados) e a gravacao
 * acontecem juntas no {@link SubmissaoRepository#submeter}, sob o bloqueio do pedido; este servico so calcula a
 * referencia, traduz o desfecho e registra o log (so ids e desfechos). Nao guarda dados de requisicao em campos.
 */
public class SubmissaoService {

    /** Prefixo que identifica a referencia como simulacao (nunca formato de protocolo oficial). */
    public static final String PREFIXO_REFERENCIA = "SIM";

    private static final ZoneId SALVADOR = ZoneId.of("America/Bahia");
    private static final Logger LOG = LoggerFactory.getLogger(SubmissaoService.class);
    private static final String RESTRICAO_DO_SIMULADOR = " Esta é uma restrição do simulador acadêmico e não impede "
            + "atendimento em serviço de saúde.";

    private final SubmissaoRepository submissoes;
    private final Clock relogio;

    /**
     * Resultado de {@link #submeter}.
     *
     * @param submissao submissao registrada agora ou a ja existente
     * @param criada {@code true} se esta chamada registrou a submissao; {@code false} se devolveu a existente
     */
    public record Resultado(Submissao submissao, boolean criada) {
    }

    /**
     * @param submissoes persistencia das submissoes
     * @param relogio relogio da aplicacao (instante da conclusao e ano da referencia)
     */
    public SubmissaoService(SubmissaoRepository submissoes, Clock relogio) {
        this.submissoes = submissoes;
        this.relogio = relogio;
    }

    /**
     * Conclui a simulacao do pedido da paciente, se houver analise vigente da revisao atual sem pendencia nem
     * criterio nao verificavel. O pedido passa a {@code SUBMETIDA_SIMULACAO} (so consulta) na mesma operacao. Repetir
     * devolve a mesma submissao, sem registrar outra.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a submissao registrada agora ({@code criada}) ou a ja existente
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 409 {@code CONFLITO_ESTADO} se a analise vigente tem pendencia ou nao verificavel, se nao ha analise
     *     vigente da versao atual, se ha analise em andamento ou se o pedido ja esta so para consulta sem registro;
     *     503 {@code DEPENDENCIA_INDISPONIVEL} se o banco estiver indisponivel
     */
    public Resultado submeter(SessaoAutenticada sessao, long solicitacaoId) {
        Instant agora = agora();
        ResultadoSubmissao resultado = submissoes.submeter(sessao.usuarioId(), solicitacaoId,
                referencia(solicitacaoId, agora), agora);
        return switch (resultado.situacao()) {
            case SUBMETIDA -> {
                LOG.info("Simulacao concluida. solicitacaoId={} submissaoId={} analiseId={}", solicitacaoId,
                        resultado.submissao().id(), resultado.submissao().analiseId());
                yield new Resultado(resultado.submissao(), true);
            }
            case JA_SUBMETIDA -> {
                LOG.info("Conclusao repetida: submissao existente devolvida. solicitacaoId={} submissaoId={}",
                        solicitacaoId, resultado.submissao().id());
                yield new Resultado(resultado.submissao(), false);
            }
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
            case SOMENTE_CONSULTA -> throw recusa(solicitacaoId, resultado,
                    "Este pedido já foi submetido e só pode ser consultado.");
            case EM_ANDAMENTO -> throw recusa(solicitacaoId, resultado,
                    "A verificação deste pedido está em andamento. Aguarde o resultado antes de concluir a simulação.");
            case SEM_ANALISE_VIGENTE -> throw recusa(solicitacaoId, resultado,
                    "Não há análise válida para a versão atual dos documentos. Verifique o pedido antes de concluir a "
                            + "simulação." + RESTRICAO_DO_SIMULADOR);
            case COM_PENDENCIAS -> throw recusa(solicitacaoId, resultado,
                    "A simulação só pode ser concluída quando a análise vigente não aponta pendências nem critérios "
                            + "não verificáveis." + RESTRICAO_DO_SIMULADOR);
        };
    }

    /**
     * Devolve o comprovante da submissao do pedido da paciente.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a submissao
     * @throws ExcecaoNegocio 404 se o pedido nao existe, e de outra paciente ou ainda nao foi submetido (resposta
     *     identica); 503 se o banco estiver indisponivel
     */
    public Submissao consultar(SessaoAutenticada sessao, long solicitacaoId) {
        return submissoes.buscar(sessao.usuarioId(), solicitacaoId)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
    }

    /**
     * Referencia interna da submissao: {@code SIM-{ano em Salvador}-{id do pedido com no minimo 6 digitos}}. Como um
     * pedido tem no maximo uma submissao, o id do pedido ja a torna unica (o banco confere de novo); seis digitos
     * nunca coincidem com o formato de quatro da carga sintetica ({@code SIM-2026-0001}). Deterministica: repetir a
     * conclusao, inclusive depois de falha de rede, leva a mesma referencia.
     *
     * @param solicitacaoId pedido
     * @param instante instante da conclusao
     * @return a referencia
     */
    public static String referencia(long solicitacaoId, Instant instante) {
        return String.format(Locale.ROOT, "%s-%d-%06d", PREFIXO_REFERENCIA, instante.atZone(SALVADOR).getYear(),
                solicitacaoId);
    }

    private static ExcecaoNegocio recusa(long solicitacaoId, ResultadoSubmissao resultado, String mensagem) {
        LOG.info("Conclusao da simulacao recusada. solicitacaoId={} situacao={}", solicitacaoId,
                resultado.situacao());
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", mensagem);
    }

    /** Instante atual na precisao que o banco guarda. */
    private Instant agora() {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
