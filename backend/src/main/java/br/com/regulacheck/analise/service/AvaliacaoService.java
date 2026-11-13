package br.com.regulacheck.analise.service;

import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.extracao.repository.ConferenciaRegistrada;
import br.com.regulacheck.extracao.repository.ConferenciaRepository;
import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import br.com.regulacheck.extracao.repository.ExtracaoRepository;
import br.com.regulacheck.extracao.service.ConferenciaService;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import br.com.regulacheck.validacao.model.AvaliacaoDeRegras;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import br.com.regulacheck.validacao.motor.ProtocoloNaoSuportadoException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monta a avaliacao dos criterios de um pedido da paciente (RC-12): aplica as regras da versao do protocolo do
 * pedido a leitura atual dos documentos, combinada com as decisoes de conferencia.
 *
 * <p><strong>Somente leitura:</strong> nao grava analise, nao muda estado nem revisao do pedido, nao altera
 * documentos, leitura ou decisoes e nao chama IA. Cada consulta abre e fecha a propria conexao, sem transacao nem
 * bloqueio; por isso o retrato e conferido (revisao da leitura, leitura ainda atual e documentos da leitura iguais
 * aos disponiveis) e, se algo mudou no meio, a montagem e recusada em vez de devolver um resultado misturado. Quem
 * publicar o resultado (RC-13) confere de novo sob o bloqueio do pedido.
 *
 * <p>Sem rota nesta historia (decisao do responsavel, 13/11/2026): a RC-13 instancia este servico na composicao
 * junto com {@code /solicitacoes/{id}/analises}. Nao guarda dados de requisicao em campos.
 */
public class AvaliacaoService {

    /** Fuso da "data do envio" do protocolo (horario de Salvador). */
    public static final ZoneId FUSO_DE_REFERENCIA = ZoneId.of("America/Bahia");

    private static final Logger LOG = LoggerFactory.getLogger(AvaliacaoService.class);

    private final SolicitacaoRepository solicitacoes;
    private final ExtracaoRepository extracoes;
    private final ConferenciaRepository conferencias;
    private final DocumentoRepository documentos;
    private final ProtocoloRepository protocolos;
    private final MotorDeRegras motor;

    /**
     * @param solicitacoes pedidos (dona, versao do protocolo, revisao atual)
     * @param extracoes leituras dos documentos
     * @param conferencias decisoes de conferencia
     * @param documentos documentos disponiveis e instante de envio
     * @param protocolos regras de cada versao do protocolo
     * @param motor motor de regras
     */
    public AvaliacaoService(SolicitacaoRepository solicitacoes, ExtracaoRepository extracoes,
            ConferenciaRepository conferencias, DocumentoRepository documentos, ProtocoloRepository protocolos,
            MotorDeRegras motor) {
        this.solicitacoes = solicitacoes;
        this.extracoes = extracoes;
        this.conferencias = conferencias;
        this.documentos = documentos;
        this.protocolos = protocolos;
        this.motor = motor;
    }

    /**
     * Avalia os criterios do pedido da paciente da sessao. Ordem das verificacoes: dona, leitura, regras da versao,
     * avaliacao.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a avaliacao, com a revisao, a leitura e a conferencia que ela usou
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 409 {@code CONFLITO_ESTADO} se o pedido nunca foi lido ou se a leitura nao e a da versao atual
     *     dos documentos (inclusive se algo mudou durante a montagem); 503 se o banco estiver indisponivel
     * @throws ProtocoloNaoSuportadoException se a versao do protocolo do pedido nao puder ser aplicada por inteiro
     *     (nenhum resultado parcial)
     */
    public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
        long pacienteId = sessao.usuarioId();
        Solicitacao pedido = Propriedade.exigir(solicitacoes.buscarDaPaciente(solicitacaoId, pacienteId), sessao);
        ExtracaoRegistrada maisRecente = extracoes.buscarMaisRecente(pacienteId, solicitacaoId)
                .orElseThrow(AvaliacaoService::semLeitura);
        if (!maisRecente.atual() || maisRecente.extracao().revisao() != pedido.revisaoAtual()) {
            throw leituraDesatualizada();
        }
        // Leitura e decisoes vem da mesma consulta, para que as decisoes sejam as da leitura avaliada.
        ConferenciaRegistrada conferencia = conferencias.buscar(pacienteId, solicitacaoId,
                maisRecente.extracao().id()).orElseThrow(() -> Propriedade.naoEncontrada(sessao));
        Extracao leitura = conferencia.leitura().extracao();
        if (!conferencia.leitura().atual() || leitura.revisao() != pedido.revisaoAtual()) {
            throw leituraDesatualizada();
        }
        LocalDate referencia = dataDeReferencia(leitura, documentos.listarDoPedido(solicitacaoId, pacienteId));
        List<RegraDoProtocolo> regras = protocolos.listarRegras(pedido.protocolo().id());
        EntradaAvaliacao entrada = new EntradaAvaliacao(
                EvidenciasDaLeitura.montar(leitura, conferencia.decisoes()), referencia);
        AvaliacaoDeRegras avaliacao;
        try {
            avaliacao = motor.avaliar(regras, entrada);
        } catch (ProtocoloNaoSuportadoException falha) {
            LOG.warn("Versao do protocolo nao suportada. solicitacaoId={} protocoloId={} motivo={} regra={}",
                    solicitacaoId, pedido.protocolo().protocoloId(), falha.motivo(), falha.codigoRegra());
            throw falha;
        }
        Instant conferenciaAtualizadaEm = ConferenciaService.visao(conferencia).atualizadaEm();
        LOG.info("Criterios avaliados. solicitacaoId={} revisao={} protocoloId={} resultadoGeral={} pendentes={} "
                + "naoVerificaveis={}", solicitacaoId, leitura.revisao(), pedido.protocolo().protocoloId(),
                avaliacao.resultadoGeral(), contar(avaliacao, Resultado.PENDENTE),
                contar(avaliacao, Resultado.NAO_VERIFICAVEL));
        return new AvaliacaoDoPedido(solicitacaoId, leitura.revisao(), pedido.protocolo(), leitura.id(),
                conferenciaAtualizadaEm, referencia, avaliacao);
    }

    /**
     * Data do envio mais recente entre os documentos da leitura, no horario de Salvador. Os documentos da leitura
     * precisam ser exatamente os disponiveis: se um entrou ou saiu entre as consultas, a leitura ja nao e a atual.
     */
    private static LocalDate dataDeReferencia(Extracao leitura, List<Documento> disponiveis) {
        Map<Long, Instant> enviadoEm = new HashMap<>();
        for (Documento documento : disponiveis) {
            enviadoEm.put(documento.id(), documento.enviadoEm());
        }
        if (enviadoEm.size() != leitura.documentos().size()) {
            throw leituraDesatualizada();
        }
        Instant maisRecente = null;
        for (ExtracaoDocumento documento : leitura.documentos()) {
            Instant envio = enviadoEm.get(documento.documentoId());
            if (envio == null) {
                throw leituraDesatualizada();
            }
            maisRecente = maisRecente == null || envio.isAfter(maisRecente) ? envio : maisRecente;
        }
        if (maisRecente == null) {
            // Uma leitura sempre tem documento (a RC-10 recusa pedido sem documentos); sem nenhum, nao ha o que
            // avaliar.
            throw semLeitura();
        }
        return maisRecente.atZone(FUSO_DE_REFERENCIA).toLocalDate();
    }

    private static long contar(AvaliacaoDeRegras avaliacao, Resultado resultado) {
        return avaliacao.resultados().stream().filter(r -> r.resultado() == resultado).count();
    }

    private static ExcecaoNegocio semLeitura() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Leia os documentos antes de conferir os critérios.");
    }

    private static ExcecaoNegocio leituraDesatualizada() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Os documentos mudaram; leia os documentos de novo.");
    }
}
