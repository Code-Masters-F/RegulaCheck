package br.com.regulacheck.solicitacao.dto;

import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
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
 * @param analise situacao da analise para a versao atual dos documentos (RC-08; calculada desde a RC-13)
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
     * Situacao da analise para a versao atual dos documentos. Receber um documento nao e analisa-lo. Desde a RC-13 e
     * calculada a partir do estado e da analise vigente da revisao atual, sem mudar o significado de
     * {@code NAO_REALIZADA} (nenhuma analise vale para esta versao). "Interrompida pelo limite de tempo" so aparece na
     * consulta das analises.
     *
     * @param situacao {@code NAO_REALIZADA}, {@code EM_ANDAMENTO} (pedido em {@code PROCESSANDO}) ou
     *     {@code CONCLUIDA} (ha analise vigente da revisao atual)
     * @param mensagem texto para a paciente
     * @param resultadoGeral resultado geral da vigente; so em {@code CONCLUIDA} (omitido nos demais)
     */
    public record Analise(String situacao, String mensagem, String resultadoGeral) {

        /** Nenhuma analise vale para a versao atual dos documentos. */
        public static final Analise NAO_REALIZADA = new Analise("NAO_REALIZADA",
                "Nenhuma análise foi realizada para esta versão dos documentos.", null);

        /** A analise do pedido esta em andamento; nao equivale a concluida. */
        public static final Analise EM_ANDAMENTO = new Analise("EM_ANDAMENTO",
                "A análise está em andamento. Consulte o resultado em instantes.", null);

        /**
         * @param pedido pedido lido do banco
         * @return a situacao da analise para a versao atual dos documentos
         */
        public static Analise de(Solicitacao pedido) {
            if (pedido.estado() == EstadoSolicitacao.PROCESSANDO) {
                return EM_ANDAMENTO;
            }
            if (pedido.resultadoVigente() != null) {
                return new Analise("CONCLUIDA", "Há uma análise simulada para esta versão dos documentos.",
                        pedido.resultadoVigente().name());
            }
            return NAO_REALIZADA;
        }
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
                true, pedido.revisaoAtual(), pedido.documentosDisponiveis(), Analise.de(pedido));
    }

    private static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
