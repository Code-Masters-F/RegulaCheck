package br.com.regulacheck.protocolo.repository;

import br.com.regulacheck.protocolo.entity.Protocolo;
import java.util.List;
import java.util.Optional;

/**
 * Leitura dos servicos (protocolos) disponiveis. E uma interface por ser a fronteira com o banco, o que permite
 * testar os servicos sem Oracle. Somente protocolos ativos ({@code ATIVO = 1}) sao visiveis por este contrato.
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
}
