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
 * @param estado estado do pedido
 * @param revisao numero da revisao vigente, a partir de 1
 * @param criadaEm instante de criacao em ISO-8601 UTC
 * @param atualizadaEm instante da ultima alteracao relevante em ISO-8601 UTC
 * @param simulacao sempre {@code true}: o pedido so existe no simulador academico
 * @param versaoConjuntoDocumental versao do conjunto de documentos do pedido: e a propria revisao vigente, que
 *     aumenta a cada documento enviado (RC-08)
 * @param documentosDisponiveis quantos documentos disponiveis o pedido tem (RC-08)
 * @param analise situacao da analise para a versao atual dos documentos (RC-08)
 */
public record SolicitacaoResponse(String id, String protocoloId, Servico servico, String estado, int revisao,
        String criadaEm, String atualizadaEm, boolean simulacao, int versaoConjuntoDocumental,
        int documentosDisponiveis, Analise analise) {

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
     * Situacao da analise. Receber um documento nao e analisa-lo: enquanto nenhuma historia de analise existir,
     * toda versao dos documentos esta sem analise. Quando a analise existir, esta situacao passa a ser calculada
     * a partir do resultado vigente da revisao atual, sem mudar o significado dos valores existentes.
     *
     * @param situacao valor fechado; hoje apenas {@code NAO_REALIZADA}
     * @param mensagem texto para a paciente
     */
    public record Analise(String situacao, String mensagem) {

        /** Nenhuma analise foi realizada para a versao atual dos documentos. */
        public static final Analise NAO_REALIZADA = new Analise("NAO_REALIZADA",
                "Nenhuma análise foi realizada para esta versão dos documentos.");
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
                true, pedido.revisaoAtual(), pedido.documentosDisponiveis(), Analise.NAO_REALIZADA);
    }

    private static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
