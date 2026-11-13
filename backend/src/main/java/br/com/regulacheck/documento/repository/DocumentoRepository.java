package br.com.regulacheck.documento.repository;

import br.com.regulacheck.documento.entity.Documento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia dos documentos de um pedido. E uma interface por ser a fronteira com o banco.
 *
 * <p>Toda operacao recebe a conta dona e o pedido e os aplica na propria consulta: um documento de outra paciente,
 * de outro pedido ou inexistente e indistinguivel (regra de dona da RC-05). Os instantes recebidos devem vir do
 * relogio da aplicacao, ja na precisao que o banco guarda.
 */
public interface DocumentoRepository {

    /**
     * Registra um documento no rascunho da paciente, de forma atomica e sob o bloqueio da linha do pedido. Na
     * mesma transacao: confere a dona e o estado, procura a chave de envio, confere o limite de documentos
     * disponiveis, incrementa a revisao do pedido, registra a revisao com o motivo de documento enviado e grava o
     * documento na revisao nova. Ou existem os tres registros (documento, revisao nova, pedido atualizado) ou
     * nenhum. O bloqueio serializa envios simultaneos ao mesmo pedido, entao o limite e a numeracao de revisoes
     * valem sob concorrencia.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido que recebera o documento
     * @param novo dados do documento, com o arquivo ja guardado na area privada
     * @param agora instante do envio e da atualizacao do pedido
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoRegistro registrar(long pacienteId, long solicitacaoId, NovoDocumento novo, Instant agora);

    /**
     * Remove (logicamente) um documento disponivel do pedido, de forma atomica e sob o bloqueio da linha do
     * pedido. Na mesma transacao: confere dona e estado (so {@code SUBMETIDA_SIMULACAO} recusa), procura o
     * documento disponivel <em>do pedido</em>, incrementa a revisao do pedido, registra a revisao com o motivo de
     * documento removido, marca o documento com essa revisao e anula a chave de envio dele (a unicidade da chave
     * cobre tambem linhas removidas) e torna nao vigente a analise vigente, se houver. O estado do pedido nao
     * muda e nenhuma analise e iniciada. O registro do documento permanece como historico; apagar o arquivo e
     * tarefa do Service, <strong>depois</strong> do commit.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido do caminho
     * @param documentoId documento a remover
     * @param agora instante da remocao e da atualizacao do pedido
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoRemocao remover(long pacienteId, long solicitacaoId, long documentoId, Instant agora);

    /**
     * Substitui um documento por outro numa unica transacao, sob o bloqueio da linha do pedido. A chave de envio
     * (se houver) e procurada <strong>antes</strong> do documento do caminho: depois de uma substituicao bem
     * sucedida o documento antigo ja nao existe, e a repeticao da mesma tentativa precisa ser reconhecida em vez de
     * virar "nao encontrado". Sem repeticao: confere dona e estado (so {@code SUBMETIDA_SIMULACAO} recusa), procura
     * o documento disponivel do pedido, incrementa a revisao <em>uma</em> vez, registra a revisao com o motivo de
     * documento substituido, marca o antigo como removido nessa revisao (chave anulada), grava o novo nela e torna
     * nao vigente a analise vigente. A contagem de disponiveis nao muda, entao o limite nao se aplica.
     *
     * @param pacienteId conta dona (a da sessao)
     * @param solicitacaoId pedido do caminho
     * @param documentoId documento a substituir
     * @param novo dados do documento novo, com o arquivo ja guardado na area privada
     * @param agora instante da substituicao
     * @return o desfecho; nunca {@code null}
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    ResultadoSubstituicao substituir(long pacienteId, long solicitacaoId, long documentoId, NovoDocumento novo,
            Instant agora);

    /**
     * Lista os documentos disponiveis do pedido da paciente, na ordem de envio.
     *
     * @param solicitacaoId pedido
     * @param pacienteId conta dona
     * @return documentos disponiveis (no maximo 3); vazia se o pedido nao tem documentos ou nao e da paciente
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    List<Documento> listarDoPedido(long solicitacaoId, long pacienteId);

    /**
     * Busca um documento disponivel, amarrado ao pedido informado e a dona desse pedido.
     *
     * @param documentoId documento
     * @param solicitacaoId pedido ao qual ele deve pertencer
     * @param pacienteId conta dona do pedido
     * @return o documento, ou vazio se nao existir, for de outro pedido ou de outra paciente
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Documento> buscarDoPedido(long documentoId, long solicitacaoId, long pacienteId);

    /**
     * Procura um documento do pedido pela identificacao da tentativa de envio. Serve a leitura antecipada do
     * Service (evitar gravar arquivo numa repeticao); a decisao final e a de {@link #registrar}.
     *
     * @param solicitacaoId pedido
     * @param pacienteId conta dona
     * @param chaveEnvio identificacao da tentativa
     * @return o documento ja registrado com essa chave, ou vazio
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Documento> buscarPorChave(long solicitacaoId, long pacienteId, String chaveEnvio);
}
