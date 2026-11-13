package br.com.regulacheck.solicitacao.dto;

import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pedido da paciente como a API o publica (criacao, reabertura, troca de servico e itens da lista). Nao expoe
 * o dono, o identificador interno do protocolo nem campos do banco.
 *
 * @param id identificador do pedido: numero decimal opaco, escrito como texto
 * @param protocoloId servico escolhido ({@code identificador-versao})
 * @param servico dados de exibicao do servico, com o aviso de protocolo ficticio
 * @param estado estado do pedido (nesta historia, {@code RASCUNHO})
 * @param revisao numero da revisao vigente, a partir de 1
 * @param criadaEm instante de criacao em ISO-8601 UTC
 * @param atualizadaEm instante da ultima alteracao relevante em ISO-8601 UTC
 * @param simulacao sempre {@code true}: o pedido so existe no simulador academico
 */
public record SolicitacaoResponse(String id, String protocoloId, Servico servico, String estado, int revisao,
        String criadaEm, String atualizadaEm, boolean simulacao) {

    /**
     * Dados de exibicao do servico escolhido.
     *
     * @param localidade localidade demonstrativa (ficticia)
     * @param especialidade especialidade demonstrativa (ficticia)
     * @param avisoFicticio aviso de que o protocolo e academico e nao e exigencia oficial
     */
    public record Servico(String localidade, String especialidade, String avisoFicticio) {
    }

    /**
     * @param pedido pedido lido do banco
     * @return resposta publica correspondente
     */
    public static SolicitacaoResponse de(Solicitacao pedido) {
        return new SolicitacaoResponse(Long.toString(pedido.id()), pedido.protocolo().protocoloId(),
                new Servico(pedido.protocolo().localidade(), pedido.protocolo().especialidade(),
                        pedido.protocolo().avisoFicticio()),
                pedido.estado().name(), pedido.revisaoAtual(), iso(pedido.criadaEm()), iso(pedido.atualizadaEm()),
                true);
    }

    private static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
