package br.com.regulacheck.analise.dto;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Historico das analises de um pedido ({@code GET /solicitacoes/{id}/analises}): todas as analises gravadas, da mais
 * recente para a mais antiga, com a vigente marcada, e a analise em andamento, se houver. Campos nulos ficam fora do
 * JSON.
 *
 * @param solicitacaoId pedido
 * @param estado estado atual do pedido
 * @param versaoConjuntoDocumental versao atual dos documentos
 * @param execucao analise em andamento; omitida fora de {@code PROCESSANDO}
 * @param vigenteId id da analise vigente; omitido quando nenhuma vale para a versao atual
 * @param limite maximo de analises devolvidas
 * @param analises analises, da mais recente para a mais antiga
 */
public record HistoricoAnalisesResponse(String solicitacaoId, String estado, int versaoConjuntoDocumental,
        Execucao execucao, String vigenteId, int limite, List<AnaliseResponse> analises) {

    /**
     * Analise em andamento.
     *
     * @param situacao {@code EM_ANDAMENTO} dentro do limite; {@code INTERROMPIDA} depois dele (pode pedir de novo)
     * @param iniciadaEm inicio em ISO-8601 UTC
     * @param limiteEm fim do limite em ISO-8601 UTC
     */
    public record Execucao(String situacao, String iniciadaEm, String limiteEm) {
    }

    /**
     * @param solicitacaoId pedido
     * @param historico historico lido
     * @param limiteEmAndamento tempo maximo de uma analise em andamento
     * @param interrompida {@code true} se a analise em andamento ja passou do limite
     * @param limite maximo de analises devolvidas
     * @return resposta publica correspondente
     */
    public static HistoricoAnalisesResponse de(long solicitacaoId, HistoricoDeAnalises historico,
            Duration limiteEmAndamento, boolean interrompida, int limite) {
        Instant iniciadaEm = historico.analiseIniciadaEm();
        Execucao execucao = iniciadaEm == null ? null : new Execucao(interrompida ? "INTERROMPIDA" : "EM_ANDAMENTO",
                AnaliseResponse.iso(iniciadaEm), AnaliseResponse.iso(iniciadaEm.plus(limiteEmAndamento)));
        String vigenteId = historico.analises().stream().filter(Analise::vigente).findFirst()
                .map(a -> Long.toString(a.id())).orElse(null);
        return new HistoricoAnalisesResponse(Long.toString(solicitacaoId), historico.estado().name(),
                historico.revisaoAtual(), execucao, vigenteId, limite,
                historico.analises().stream().map(AnaliseResponse::de).toList());
    }
}
