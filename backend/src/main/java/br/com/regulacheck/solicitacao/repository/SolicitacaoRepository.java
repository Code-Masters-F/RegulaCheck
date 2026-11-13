package br.com.regulacheck.solicitacao.repository;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia das solicitacoes da paciente. E uma interface por ser a fronteira com o banco.
 *
 * <p>Toda leitura e escrita recebe a conta dona e a aplica na propria consulta: um pedido de outra paciente e
 * indistinguivel de um pedido inexistente (regra de dona da RC-05). Os instantes recebidos devem vir do relogio
 * da aplicacao, ja na precisao que o banco guarda.
 */
public interface SolicitacaoRepository {

    /**
     * Cria um pedido em rascunho, na revisao 1, junto da revisao inicial com motivo {@code CRIACAO}. Os dois
     * registros sao gravados na mesma transacao: ou existem os dois ou nenhum.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param protocolo servico ativo escolhido
     * @param agora instante de criacao e de atualizacao
     * @return o pedido criado
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Solicitacao criar(long pacienteId, Protocolo protocolo, Instant agora);

    /**
     * Busca um pedido da paciente.
     *
     * @param id identificador do pedido
     * @param pacienteId conta dona (a da sessao)
     * @return o pedido, ou vazio se nao existir ou for de outra paciente
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Solicitacao> buscarDaPaciente(long id, long pacienteId);

    /**
     * Lista os pedidos da paciente, do mais recente para o mais antigo (criacao, depois id).
     *
     * @param pacienteId conta dona (a da sessao)
     * @param limite maximo de itens devolvidos
     * @return pedidos da paciente; vazio se nao tiver nenhum
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    List<Solicitacao> listarDaPaciente(long pacienteId, int limite);

    /**
     * Troca o servico de um rascunho de forma atomica e segura sob concorrencia: bloqueia a linha do pedido,
     * confere dona e estado, e so entao decide. Mudou de servico: incrementa a revisao, atualiza a data e
     * registra a nova revisao com motivo {@code PROTOCOLO_ALTERADO}, tudo na mesma transacao. Mesmo servico: nada
     * muda. Alteracoes simultaneas do mesmo pedido sao serializadas, sem salto nem repeticao de revisao.
     *
     * @param id identificador do pedido
     * @param pacienteId conta dona (a da sessao)
     * @param novoProtocolo servico ativo desejado
     * @param agora instante da alteracao
     * @return o que aconteceu e, quando existe, o pedido no estado final
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoTroca trocarProtocolo(long id, long pacienteId, Protocolo novoProtocolo, Instant agora);

    /**
     * Exclui um rascunho da paciente por inteiro, de forma atomica e sob o bloqueio da linha do pedido: confere
     * dona e estado (so {@code RASCUNHO} pode ser excluido), le os nomes internos dos arquivos de todos os
     * documentos (disponiveis e removidos) e apaga {@code RC_DOCUMENTO}, {@code RC_REVISAO} e
     * {@code RC_SOLICITACAO}, nessa ordem, por causa das chaves estrangeiras. Se algo ainda ligado ao pedido
     * impedir a exclusao, desfaz tudo e informa. Apagar os arquivos e tarefa do Service, <strong>depois</strong>
     * do commit.
     *
     * @param id identificador do pedido
     * @param pacienteId conta dona (a da sessao)
     * @return o desfecho e, se excluido, os arquivos a apagar
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoExclusao excluirRascunho(long id, long pacienteId);
}
