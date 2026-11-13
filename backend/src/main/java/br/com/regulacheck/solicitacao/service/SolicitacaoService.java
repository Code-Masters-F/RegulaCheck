package br.com.regulacheck.solicitacao.service;

import br.com.regulacheck.armazenamento.ArmazenamentoPrivado;
import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.repository.ResultadoExclusao;
import br.com.regulacheck.solicitacao.repository.ResultadoTroca;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso da solicitacao em rascunho (RC-07): criar, reabrir, listar e trocar o servico escolhido.
 *
 * <p>A paciente e sempre a da sessao autenticada, nunca um identificador vindo do cliente. Todo acesso a um
 * pedido passa pela busca ja filtrada pela dona; pedido alheio e pedido inexistente tem a mesma resposta
 * (regra de dona da RC-05). Este servico nao guarda dados de requisicao em campos.
 */
public class SolicitacaoService {

    /** Maximo de pedidos devolvidos pela listagem (sem paginacao nesta historia). */
    public static final int LIMITE_LISTA = 50;

    private static final Logger LOG = LoggerFactory.getLogger(SolicitacaoService.class);

    private final ProtocoloService protocolos;
    private final SolicitacaoRepository solicitacoes;
    private final ArmazenamentoPrivado armazenamento;
    private final Clock relogio;

    /**
     * @param protocolos catalogo de servicos
     * @param solicitacoes persistencia dos pedidos
     * @param armazenamento area privada de arquivos (infraestrutura comum; usada so para apagar os arquivos dos
     *     documentos de um rascunho excluido)
     * @param relogio relogio da aplicacao (instantes gravados e devolvidos)
     */
    public SolicitacaoService(ProtocoloService protocolos, SolicitacaoRepository solicitacoes,
            ArmazenamentoPrivado armazenamento, Clock relogio) {
        this.protocolos = protocolos;
        this.solicitacoes = solicitacoes;
        this.armazenamento = armazenamento;
        this.relogio = relogio;
    }

    /**
     * Cria um rascunho da paciente da sessao, na revisao 1, ligado ao servico escolhido. O pedido e a revisao
     * inicial sao gravados juntos; o retorno so acontece depois da gravacao confirmada.
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param protocoloId servico escolhido ({@code identificador-versao}), ja validado quanto ao formato
     * @return o pedido criado
     * @throws ExcecaoNegocio 422 {@code REGRA_DE_NEGOCIO} se o servico nao existe ou esta inativo; 503 se o
     *     banco estiver indisponivel
     */
    public Solicitacao criar(SessaoAutenticada sessao, String protocoloId) {
        Protocolo protocolo = protocolos.exigirAtivo(protocoloId);
        return solicitacoes.criar(sessao.usuarioId(), protocolo, agora());
    }

    /**
     * Busca um pedido da paciente da sessao.
     *
     * @param sessao paciente autenticada
     * @param id identificador do pedido
     * @return o pedido persistido
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente (resposta
     *     identica nos dois casos); 503 se o banco estiver indisponivel
     */
    public Solicitacao buscarPropria(SessaoAutenticada sessao, long id) {
        return Propriedade.exigir(solicitacoes.buscarDaPaciente(id, sessao.usuarioId()), sessao);
    }

    /**
     * Lista os pedidos da paciente da sessao, do mais recente para o mais antigo, ate {@link #LIMITE_LISTA}.
     *
     * @param sessao paciente autenticada
     * @return pedidos da paciente (so os dela); lista vazia, e nao erro, quando nao tem nenhum
     * @throws ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    public List<Solicitacao> listar(SessaoAutenticada sessao) {
        return solicitacoes.listarDaPaciente(sessao.usuarioId(), LIMITE_LISTA);
    }

    /**
     * Troca o servico de um rascunho da paciente da sessao. Se o servico mudou, a revisao do pedido aumenta em
     * um e a nova revisao fica registrada com o motivo da troca (resultados produzidos com o servico anterior
     * deixam de ser vigentes); se e o mesmo servico, nada muda. A verificacao do estado e a gravacao acontecem
     * sob o mesmo bloqueio da linha do pedido, entao alteracoes simultaneas sao serializadas.
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param id identificador do pedido
     * @param protocoloId servico desejado ({@code identificador-versao}), ja validado quanto ao formato
     * @return o pedido no estado final
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe ou e de outra paciente; 409
     *     {@code CONFLITO_ESTADO} se o pedido ja saiu de rascunho; 422 {@code REGRA_DE_NEGOCIO} se o servico nao
     *     existe ou esta inativo; 503 se o banco estiver indisponivel
     */
    public Solicitacao trocarProtocolo(SessaoAutenticada sessao, long id, String protocoloId) {
        Protocolo protocolo = protocolos.exigirAtivo(protocoloId);
        ResultadoTroca resultado = solicitacoes.trocarProtocolo(id, sessao.usuarioId(), protocolo, agora());
        return switch (resultado.situacao()) {
            case NAO_ENCONTRADA -> throw Propriedade.naoEncontrada(sessao);
            case FORA_DE_RASCUNHO -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido não pode mais ser alterado.");
            case SEM_MUDANCA, ALTERADA -> resultado.solicitacao();
        };
    }

    /**
     * Exclui um rascunho da paciente da sessao, com o pedido, o historico de revisoes, os documentos e os
     * arquivos guardados. A confirmacao explicita ja foi exigida pelo controller; aqui valem a dona, o estado e a
     * atomicidade.
     *
     * <p>Politica de arquivos (RC-09): o banco muda primeiro, numa transacao so; os arquivos sao apagados
     * <strong>depois</strong> do commit. Se apagar um arquivo falhar, a exclusao continua valendo (o pedido ja nao
     * existe) e o arquivo remanescente fica sem registro, sem nunca ser visivel ao usuario; o risco e registrado so
     * em log tecnico, com a contagem e sem nome interno.
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param id identificador do pedido
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o pedido nao existe, ja foi excluido ou e de outra
     *     paciente; 409 {@code CONFLITO_ESTADO} se o pedido foi submetido em simulacao, se nao e rascunho ou se
     *     algo ainda ligado a ele impede a exclusao; 503 se o banco estiver indisponivel (nada e apagado)
     */
    public void excluirRascunho(SessaoAutenticada sessao, long id) {
        ResultadoExclusao resultado = solicitacoes.excluirRascunho(id, sessao.usuarioId());
        switch (resultado.situacao()) {
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
            case SUBMETIDO -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido já foi submetido e só pode ser consultado.");
            case FORA_DE_RASCUNHO -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Somente rascunhos podem ser excluídos.");
            case COM_DEPENDENCIAS -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido não pode ser excluído.");
            case EXCLUIDO -> apagarArquivos(id, resultado.arquivos());
        }
    }

    /** Depois do commit: apaga cada arquivo sem interromper nos erros; so a contagem de falhas vai ao log. */
    private void apagarArquivos(long id, List<IdInterno> arquivos) {
        int falhas = 0;
        for (IdInterno arquivo : arquivos) {
            try {
                armazenamento.remover(arquivo);
            } catch (RuntimeException falha) {
                falhas++;
            }
        }
        if (falhas > 0) {
            LOG.warn("Arquivos de rascunho excluido nao puderam ser apagados. solicitacaoId={} arquivos={} falhas={}",
                    id, arquivos.size(), falhas);
        }
    }

    /**
     * Instante atual na precisao que o banco guarda. O Oracle arredonda para 6 casas decimais e o relogio do
     * sistema tem mais; truncar na origem faz a resposta da criacao ser igual a uma leitura posterior.
     */
    private Instant agora() {
        return relogio.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
