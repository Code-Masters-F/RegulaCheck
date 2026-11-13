package br.com.regulacheck.protocolo.repository;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import java.util.List;
import java.util.Optional;

/**
 * Leitura dos servicos (protocolos) disponiveis e das regras de cada versao. E uma interface por ser a fronteira
 * com o banco, o que permite testar os servicos sem Oracle. A busca de protocolos so ve os ativos
 * ({@code ATIVO = 1}); as regras sao lidas por versao, ativa ou nao (RC-12).
 */
public interface ProtocoloRepository {

    /**
     * Lista os protocolos ativos.
     *
     * @return protocolos ativos, em ordem estavel (identificador e versao); vazio se nenhum estiver ativo
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    List<Protocolo> listarAtivos();

    /**
     * Busca um protocolo ativo pelo identificador publico.
     *
     * @param protocoloId {@code identificador-versao}, ex.: {@code PROTOCOLO-ACADEMICO-FICTICIO-v0.1}
     * @return o protocolo, ou vazio se nao existir ou estiver inativo (as duas situacoes sao indistinguiveis)
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    Optional<Protocolo> buscarAtivoPorProtocoloId(String protocoloId);

    /**
     * Lista as regras de uma versao do protocolo (RC-12). Nao filtra por {@code ATIVO}: um pedido continua sendo
     * avaliado pela versao a que esta ligado, mesmo que ela tenha saido do catalogo.
     *
     * @param protocoloId identificador interno da versao do protocolo ({@code RC_PROTOCOLO.ID})
     * @return as regras da versao, na ordem de avaliacao ({@code ORDEM}, depois codigo); vazio se a versao nao tem
     *     regras ou nao existe
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    List<RegraDoProtocolo> listarRegras(long protocoloId);
}
