package br.com.regulacheck.reenvio.service;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.service.AnaliseEmAndamentoException;
import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.analise.service.AnaliseService.ResultadoAnalise;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.service.ConferenciaService;
import br.com.regulacheck.extracao.service.ExtracaoService;
import br.com.regulacheck.extracao.service.ExtracaoService.ResultadoLeitura;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.resumo.service.ResumoService;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verificacao da versao corrigida do pedido (RC-16): depois de a paciente substituir, incluir ou remover documentos,
 * refaz para a <strong>revisao atual</strong> a leitura (RC-10), a conferencia (RC-11), a analise (RC-13) e o resumo
 * (RC-14), nessa ordem, e devolve o novo resultado com a comparacao com o anterior.
 *
 * <p><strong>Sem regra propria.</strong> Cada etapa e o caso de uso da sua historia, com as garantias dele: a leitura
 * existente da revisao e reaproveitada; a analise so e publicada se a revisao e a conferencia ainda forem as avaliadas,
 * uma de cada vez e com limite de tempo; repetir sem mudanca devolve a analise vigente; o resumo e validado e cai na
 * explicacao padrao identificada em falha do adaptador. Nada aqui altera o documento clinico, as regras ou a analise
 * gravada.
 *
 * <p><strong>Para na conferencia.</strong> Uma leitura nova (de uma revisao corrigida) tem campos que so a paciente
 * pode confirmar. Enquanto houver campo identificado sem decisao (contagem {@code camposSemDecisao} da conferencia,
 * em qualquer documento), a verificacao para antes das regras e nao registra analise: uma analise "nao conferida"
 * entraria no historico e faria criterios atendidos parecerem pendencias novas na comparacao. Sem campo pendente de
 * decisao, ilegivel, documento nao lido ou divergencia apontada ({@link SituacaoConferencia#REQUER_CONFERENCIA}) nao
 * se resolvem conferindo; a analise segue e registra os nao verificaveis com o motivo.
 *
 * <p><strong>Falha preserva o ultimo resultado.</strong> Cada etapa grava tudo ou nada, entao uma falha nunca deixa
 * resultado parcial nem altera a ultima analise concluida. A excecao devolvida mantem o status e o codigo da etapa,
 * troca a mensagem pelo que a paciente precisa repetir e informa a etapa em {@code detalhes} ({@code campo = "etapa"}).
 * Falha ao gravar o resumo <em>depois</em> da analise nao e erro: a analise registrada e devolvida sem resumo, com
 * {@link Etapa#RESUMO}.
 *
 * <p>Sem transacao propria e sem estado de requisicao em campos: cada caso de uso abre e fecha as suas. Logs so com
 * identificadores, etapa, codigos e contagens; nunca valor ou trecho de documento.
 */
public class ReenvioService {

    private static final Logger LOG = LoggerFactory.getLogger(ReenvioService.class);

    private static final String LEITURA = "LEITURA";
    private static final String CONFERENCIA = "CONFERENCIA";
    private static final String ANALISE = "ANALISE";

    /** O que a paciente precisa repetir, por etapa e codigo de erro (ver {@code contracts/api-reenvio.md}). */
    private static final Map<String, String> O_QUE_REPETIR = Map.of(
            LEITURA + "/SEM_DOCUMENTOS", "Este pedido não tem documentos para verificar. Envie o documento corrigido "
                    + "e peça a verificação de novo.",
            LEITURA + "/LEITURA_INDISPONIVEL", "A leitura da versão corrigida falhou. Seus documentos foram mantidos e "
                    + "o último resultado concluído continua no histórico. Peça a verificação de novo; não é preciso "
                    + "reenviar o arquivo.",
            LEITURA + "/CONFLITO_ESTADO", "Os documentos mudaram durante a leitura. Nada foi analisado e o último "
                    + "resultado concluído continua no histórico. Peça a verificação de novo para a versão atual.",
            LEITURA + "/DEPENDENCIA_INDISPONIVEL", "Não foi possível ler os documentos agora. Seus documentos e o "
                    + "último resultado concluído foram mantidos. Tente a verificação de novo mais tarde.",
            CONFERENCIA + "/DEPENDENCIA_INDISPONIVEL", "Não foi possível consultar a conferência agora. Nada foi "
                    + "analisado e o último resultado concluído continua no histórico. Tente a verificação de novo "
                    + "mais tarde.",
            ANALISE + "/CONFLITO_ESTADO", "A verificação não foi concluída porque os documentos ou a conferência "
                    + "mudaram, ou porque ela passou do tempo limite. Nada foi publicado e o último resultado concluído "
                    + "continua no histórico. Peça a verificação de novo.",
            ANALISE + "/ANALISE_INDISPONIVEL", "Não foi possível concluir a análise da versão corrigida. Nada foi "
                    + "publicado; seus documentos e o último resultado concluído foram mantidos. Peça a verificação "
                    + "de novo.",
            ANALISE + "/DEPENDENCIA_INDISPONIVEL", "Não foi possível concluir a verificação agora. O último resultado "
                    + "concluído continua no histórico. Consulte as análises ou tente a verificação de novo mais "
                    + "tarde.");

    private static final String EM_ANDAMENTO = "Uma verificação deste pedido já está em andamento. Aguarde alguns "
            + "instantes e consulte o resultado.";

    private final SolicitacaoService solicitacoes;
    private final ExtracaoService extracoes;
    private final ConferenciaService conferencias;
    private final AnaliseService analises;
    private final ResumoService resumos;

    /** Ate onde a verificacao chegou. */
    public enum Etapa {
        /** Parou antes das regras: ha campo lido sem decisao da paciente; nenhuma analise registrada. */
        CONFERENCIA,
        /** Analise registrada (ou vigente devolvida), mas o resumo nao pode ser gravado agora. */
        RESUMO,
        /** Analise e resumo (gerado ou explicacao padrao identificada) disponiveis. */
        CONCLUIDA
    }

    /**
     * Resultado de {@link #verificar}.
     *
     * @param etapa ate onde a verificacao chegou
     * @param revisao revisao dos documentos lida
     * @param extracaoId leitura da revisao atual
     * @param leituraCriada {@code true} se esta chamada gravou a leitura
     * @param camposSemDecisao campos identificados sem decisao da paciente ({@code 0} fora de
     *     {@link Etapa#CONFERENCIA})
     * @param analise analise registrada ou vigente; {@code null} so em {@link Etapa#CONFERENCIA}
     * @param analiseCriada {@code true} se esta chamada registrou a analise
     * @param comparacao comparacao da analise com a anterior; {@code null} sem analise, na primeira analise do pedido ou
     *     entre versoes de protocolo diferentes
     */
    public record Resultado(Etapa etapa, int revisao, long extracaoId, boolean leituraCriada, int camposSemDecisao,
            Analise analise, boolean analiseCriada, ComparacaoDeAnalises comparacao) {
    }

    /**
     * @param solicitacoes busca do pedido filtrada pela dona
     * @param extracoes leitura simulada (RC-10)
     * @param conferencias conferencia da transcricao (RC-11)
     * @param analises analises e historico (RC-13), com a comparacao (RC-16)
     * @param resumos resumo simulado (RC-14)
     */
    public ReenvioService(SolicitacaoService solicitacoes, ExtracaoService extracoes,
            ConferenciaService conferencias, AnaliseService analises, ResumoService resumos) {
        this.solicitacoes = solicitacoes;
        this.extracoes = extracoes;
        this.conferencias = conferencias;
        this.analises = analises;
        this.resumos = resumos;
    }

    /**
     * Verifica a versao atual dos documentos do pedido da paciente: le (ou reaproveita a leitura), confere, analisa e
     * resume. Repetir sem mudanca nos documentos ou na conferencia devolve a analise vigente, sem registrar outra
     * analise nem outro resumo.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return ate onde chegou: parada na conferencia, analise sem resumo ou analise concluida com resumo
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica); 409 {@code CONFLITO_ESTADO} se o pedido foi submetido; nas etapas, com {@code detalhes} da etapa:
     *     422 {@code SEM_DOCUMENTOS}, 503 {@code LEITURA_INDISPONIVEL} e 409 {@code CONFLITO_ESTADO} na leitura, 409
     *     {@link AnaliseEmAndamentoException}, 409 {@code CONFLITO_ESTADO} e 503 {@code ANALISE_INDISPONIVEL} na
     *     analise, 503 {@code DEPENDENCIA_INDISPONIVEL} em qualquer etapa
     */
    public Resultado verificar(SessaoAutenticada sessao, long solicitacaoId) {
        if (solicitacoes.buscarPropria(sessao, solicitacaoId).estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido já foi submetido e só pode ser consultado.");
        }
        ResultadoLeitura leitura = ler(sessao, solicitacaoId);
        Extracao extracao = leitura.extracao().extracao();
        SituacaoConferencia.Avaliacao conferencia = conferir(sessao, solicitacaoId, extracao.id());
        if (conferencia.situacao() == SituacaoConferencia.DESATUALIZADA) {
            // Outra tela trocou os documentos entre a leitura e a conferencia: a leitura ja nao e a da versao atual.
            throw naEtapa(new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Os documentos mudaram."), LEITURA,
                    solicitacaoId);
        }
        // Pela contagem, nao pela situacao: REQUER_CONFERENCIA (ilegivel, nao lido, divergencia) tem precedencia sobre
        // PENDENTE e esconderia campos ainda sem decisao em outro documento.
        if (conferencia.camposSemDecisao() > 0) {
            LOG.info("Verificacao parada na conferencia. solicitacaoId={} revisao={} extracaoId={} leituraCriada={} "
                    + "camposSemDecisao={}", solicitacaoId, extracao.revisao(), extracao.id(), leitura.criada(),
                    conferencia.camposSemDecisao());
            return new Resultado(Etapa.CONFERENCIA, extracao.revisao(), extracao.id(), leitura.criada(),
                    conferencia.camposSemDecisao(), null, false, null);
        }
        ResultadoAnalise analise = analisar(sessao, solicitacaoId);
        return resumir(sessao, solicitacaoId, extracao, leitura.criada(), analise);
    }

    private ResultadoLeitura ler(SessaoAutenticada sessao, long solicitacaoId) {
        try {
            return extracoes.ler(sessao, solicitacaoId);
        } catch (ExcecaoNegocio falha) {
            throw naEtapa(falha, LEITURA, solicitacaoId);
        }
    }

    private SituacaoConferencia.Avaliacao conferir(SessaoAutenticada sessao, long solicitacaoId, long extracaoId) {
        try {
            return conferencias.consultar(sessao, solicitacaoId, extracaoId).avaliacao();
        } catch (ExcecaoNegocio falha) {
            throw naEtapa(falha, CONFERENCIA, solicitacaoId);
        }
    }

    private ResultadoAnalise analisar(SessaoAutenticada sessao, long solicitacaoId) {
        try {
            return analises.analisar(sessao, solicitacaoId);
        } catch (AnaliseEmAndamentoException emAndamento) {
            LOG.info("Verificacao recusada: analise em andamento. solicitacaoId={} etapa={}", solicitacaoId, ANALISE);
            throw new AnaliseEmAndamentoException(emAndamento.segundosRestantes(), EM_ANDAMENTO, etapa(ANALISE));
        } catch (ExcecaoNegocio falha) {
            throw naEtapa(falha, ANALISE, solicitacaoId);
        }
    }

    /**
     * Gera o resumo da analise. A analise ja esta gravada: falha ao gravar o resumo (a analise deixou de ser vigente
     * durante a geracao, ou o banco caiu) nao a desfaz, so devolve a analise sem resumo em {@link Etapa#RESUMO}. Falha
     * ou saida invalida do adaptador simulado nao chega aqui: o {@link ResumoService} grava a explicacao padrao.
     */
    private Resultado resumir(SessaoAutenticada sessao, long solicitacaoId, Extracao extracao, boolean leituraCriada,
            ResultadoAnalise analise) {
        try {
            ResumoService.Resultado resumo = resumos.gerar(sessao, solicitacaoId);
            if (resumo.analise().id() == analise.analise().id()) {
                LOG.info("Verificacao concluida. solicitacaoId={} revisao={} extracaoId={} analiseId={} "
                        + "analiseCriada={} resultadoGeral={} resumoOrigem={}", solicitacaoId, extracao.revisao(),
                        extracao.id(), analise.analise().id(), analise.criada(), analise.analise().resultadoGeral(),
                        resumo.analise().resumoOrigem());
                return new Resultado(Etapa.CONCLUIDA, extracao.revisao(), extracao.id(), leituraCriada, 0,
                        resumo.analise(), analise.criada(), analise.comparacao());
            }
            // Outra verificacao publicou uma analise mais nova entre a analise e o resumo: o resumo e dela, nao desta.
            LOG.info("Resumo nao associado: outra analise ficou vigente. solicitacaoId={} analiseId={}", solicitacaoId,
                    analise.analise().id());
        } catch (ExcecaoNegocio falha) {
            LOG.warn("Resumo nao gravado depois da analise. solicitacaoId={} analiseId={} status={} codigo={}",
                    solicitacaoId, analise.analise().id(), falha.status(), falha.codigo());
        }
        return new Resultado(Etapa.RESUMO, extracao.revisao(), extracao.id(), leituraCriada, 0, analise.analise(),
                analise.criada(), analise.comparacao());
    }

    /**
     * Repassa a falha de uma etapa com o mesmo status e codigo, a mensagem do que repetir e a etapa em
     * {@code detalhes}. O 404 continua identico ao de qualquer recurso alheio ou inexistente (sem detalhe).
     */
    private static ExcecaoNegocio naEtapa(ExcecaoNegocio falha, String etapa, long solicitacaoId) {
        if (falha.status() == 404) {
            return falha;
        }
        LOG.info("Verificacao interrompida. solicitacaoId={} etapa={} status={} codigo={}", solicitacaoId, etapa,
                falha.status(), falha.codigo());
        String mensagem = O_QUE_REPETIR.getOrDefault(etapa + "/" + falha.codigo(), falha.getMessage());
        return new ExcecaoNegocio(falha.status(), falha.codigo(), mensagem, etapa(etapa));
    }

    private static List<ErroPadrao.Detalhe> etapa(String etapa) {
        return List.of(new ErroPadrao.Detalhe("etapa", etapa));
    }
}
