package br.com.regulacheck.reenvio.dto;

import br.com.regulacheck.analise.dto.AnaliseResponse;
import br.com.regulacheck.reenvio.service.ReenvioService;
import br.com.regulacheck.reenvio.service.ReenvioService.Etapa;

/**
 * Resposta de {@code POST /solicitacoes/{id}/reenvio} (RC-16): ate onde a verificacao da versao corrigida chegou e o
 * proximo passo. Sempre identificada como simulada (leitura e resumo simulados, protocolo academico ficticio). Campos
 * nulos ficam fora do JSON. Nao traz valor lido de documento.
 *
 * @param solicitacaoId pedido, como texto
 * @param etapa {@code CONFERENCIA} (parou: confira a leitura), {@code RESUMO} (analise sem resumo: peca o resumo de
 *     novo) ou {@code CONCLUIDA}
 * @param revisao revisao dos documentos lida
 * @param extracaoId leitura da revisao atual, como texto (para a conferencia da RC-11)
 * @param leituraCriada {@code true} se esta chamada gravou a leitura
 * @param camposSemDecisao campos identificados ainda sem decisao da paciente
 * @param analiseCriada {@code true} se esta chamada registrou a analise
 * @param simulada sempre {@code true}
 * @param orientacao proximo passo, em texto, sem diagnostico nem promessa de aprovacao
 * @param analise analise com resumo e comparacao; omitida em {@code CONFERENCIA}
 */
public record ReenvioResponse(String solicitacaoId, String etapa, int revisao, String extracaoId,
        boolean leituraCriada, int camposSemDecisao, boolean analiseCriada, boolean simulada, String orientacao,
        AnaliseResponse analise) {

    /**
     * @param solicitacaoId pedido
     * @param resultado resultado da verificacao
     * @return resposta publica correspondente
     */
    public static ReenvioResponse de(long solicitacaoId, ReenvioService.Resultado resultado) {
        return new ReenvioResponse(Long.toString(solicitacaoId), resultado.etapa().name(), resultado.revisao(),
                Long.toString(resultado.extracaoId()), resultado.leituraCriada(), resultado.camposSemDecisao(),
                resultado.analiseCriada(), true, orientacao(resultado),
                resultado.analise() == null ? null : AnaliseResponse.de(resultado.analise(), resultado.comparacao()));
    }

    private static String orientacao(ReenvioService.Resultado resultado) {
        if (resultado.etapa() == Etapa.CONFERENCIA) {
            return "Confira a leitura dos documentos da revisão " + resultado.revisao() + " antes do novo resultado. "
                    + "Nenhuma pendência foi marcada como resolvida.";
        }
        if (resultado.etapa() == Etapa.RESUMO) {
            return "A análise foi registrada, mas o resumo simulado não pôde ser gerado agora. Peça o resumo de novo; "
                    + "se os documentos ou a conferência mudaram, peça a verificação de novo.";
        }
        String inicio = resultado.analiseCriada() ? "Nova verificação concluída. Compare com o resultado anterior."
                : "Nada mudou desde a última verificação; este é o resultado vigente.";
        return inicio + " \"Sem pendências identificadas\" não é aprovação do SUS.";
    }
}
