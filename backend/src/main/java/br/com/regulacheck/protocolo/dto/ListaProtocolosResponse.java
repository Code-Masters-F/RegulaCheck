package br.com.regulacheck.protocolo.dto;

import br.com.regulacheck.protocolo.entity.Protocolo;
import java.util.List;

/**
 * Resposta de {@code GET /protocolos}.
 *
 * @param itens servicos ativos; vazio quando nao ha nenhum disponivel
 */
public record ListaProtocolosResponse(List<ProtocoloResponse> itens) {

    /**
     * @param protocolos protocolos ativos
     * @return resposta publica correspondente
     */
    public static ListaProtocolosResponse de(List<Protocolo> protocolos) {
        return new ListaProtocolosResponse(protocolos.stream().map(ProtocoloResponse::de).toList());
    }
}
