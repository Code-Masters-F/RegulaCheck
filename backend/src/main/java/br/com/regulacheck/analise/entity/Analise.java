package br.com.regulacheck.analise.entity;

import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Analise gravada de um pedido: uma execucao concluida da avaliacao da RC-12, como ficou registrada. Nunca e
 * reescrita; a unica mudanca possivel e deixar de ser vigente. Modelo interno: nunca e serializado como resposta
 * publica (usar {@code AnaliseResponse}).
 *
 * <p>A leitura e sempre simulada no MVP ({@code RC_ANALISE.SIMULADO} e sempre 1).
 *
 * @param id identificador da analise
 * @param revisao revisao do pedido (versao do conjunto documental) avaliada
 * @param vigente {@code true} se vale para a versao atual dos documentos e para a conferencia atual
 * @param executadaEm instante em que a execucao foi concluida e gravada, em UTC
 * @param protocoloIdentificador identificador do protocolo aplicado, ex.: {@code PROTOCOLO-ACADEMICO-FICTICIO}
 * @param protocoloVersao versao do protocolo aplicada, ex.: {@code v0.1}
 * @param conferenciaEm instante da conferencia considerada; {@code null} so nas analises da carga sintetica
 * @param dataReferencia data usada pelas regras de data; {@code null} so nas analises da carga sintetica
 * @param resultadoGeral resultado geral pela precedencia do protocolo
 * @param documentos documentos da revisao avaliada, na ordem de envio
 * @param resultados um por regra da versao, na ordem da versao
 * @param pendencias uma por regra pendente ou nao verificavel, na ordem da versao
 */
public record Analise(long id, int revisao, boolean vigente, Instant executadaEm, String protocoloIdentificador,
        String protocoloVersao, Instant conferenciaEm, LocalDate dataReferencia, ResultadoGeral resultadoGeral,
        List<DocumentoDaAnalise> documentos, List<ResultadoGravado> resultados, List<PendenciaGravada> pendencias) {

    /** Exige os campos obrigatorios e copia as listas. */
    public Analise {
        Objects.requireNonNull(executadaEm, "executadaEm");
        Objects.requireNonNull(protocoloIdentificador, "protocoloIdentificador");
        Objects.requireNonNull(protocoloVersao, "protocoloVersao");
        Objects.requireNonNull(resultadoGeral, "resultadoGeral");
        documentos = List.copyOf(documentos);
        resultados = List.copyOf(resultados);
        pendencias = List.copyOf(pendencias);
    }
}
