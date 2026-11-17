package br.com.regulacheck.analise.dto;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.DocumentoDaAnalise;
import br.com.regulacheck.analise.entity.EvidenciaGravada;
import br.com.regulacheck.analise.entity.PendenciaGravada;
import br.com.regulacheck.analise.entity.ResultadoGravado;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises;
import br.com.regulacheck.extracao.dto.OrigemResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Analise gravada como a API a publica ({@code POST} e {@code GET /solicitacoes/{id}/analises}, {@code POST .../resumo}
 * e {@code POST .../reenvio}). Sempre identificada como simulada: a leitura dos documentos e simulada e o protocolo e
 * academico e ficticio. Nao traz valor lido dos documentos (so documento, campo, situacao e origem). O resumo da RC-14
 * e a comparacao da RC-16 sao opcionais. Campos nulos ficam fora do JSON.
 *
 * @param id identificador da analise: numero decimal opaco, escrito como texto
 * @param revisao versao do conjunto documental avaliada
 * @param vigente {@code true} se vale para a versao atual dos documentos e para a conferencia atual
 * @param executadaEm data e hora de conclusao em ISO-8601 UTC
 * @param protocolo protocolo e versao aplicados
 * @param conferenciaConsiderada instante da conferencia considerada em ISO-8601 UTC; omitido nas analises da carga
 *     sintetica
 * @param dataReferencia data ({@code AAAA-MM-DD}, Salvador) usada pelas regras de data; omitida nas analises da carga
 *     sintetica
 * @param resultadoGeral {@code COM_PENDENCIAS}, {@code REQUER_CONFERENCIA} ou {@code SEM_PENDENCIAS_IDENTIFICADAS}
 * @param rotuloResultado texto correspondente ao resultado geral para apresentacao acessivel
 * @param simulada sempre {@code true}
 * @param aviso texto fixo de simulacao e de que "sem pendencias" nao e aprovacao
 * @param documentos documentos da revisao avaliada
 * @param resultados um por regra, na ordem da versao
 * @param pendencias uma por regra pendente ou nao verificavel
 * @param resumo orientacao simulada opcional desta analise
 * @param comparacao comparacao com a analise registrada imediatamente antes (RC-16); omitida na primeira analise do
 *     pedido e entre versoes de protocolo diferentes
 */
public record AnaliseResponse(String id, int revisao, boolean vigente, String executadaEm, Protocolo protocolo,
        String conferenciaConsiderada, String dataReferencia, String resultadoGeral, String rotuloResultado,
        boolean simulada, String aviso,
        List<Documento> documentos, List<ResultadoRegra> resultados, List<Pendencia> pendencias,
        Resumo resumo, Comparacao comparacao) {

    /** Aviso fixo de toda analise. */
    public static final String AVISO = "Análise simulada: leitura simulada dos documentos e protocolo acadêmico "
            + "fictício. \"Sem pendências identificadas\" não é aprovação do SUS.";

    /**
     * @param identificador nome do protocolo
     * @param versao versao aplicada
     */
    public record Protocolo(String identificador, String versao) {
    }

    /**
     * @param documentoId documento
     * @param nomeArquivo nome de exibicao saneado no envio
     * @param tipoDeclarado tipo escolhido no envio (a regra usa o tipo lido e confirmado)
     * @param disponivel {@code false} se depois foi substituido ou removido
     */
    public record Documento(String documentoId, String nomeArquivo, String tipoDeclarado, boolean disponivel) {
    }

    /**
     * @param codigoRegra codigo da regra na versao
     * @param descricao descricao da regra na versao
     * @param resultado {@code ATENDE}, {@code PENDENTE}, {@code NAO_VERIFICAVEL} ou {@code NAO_APLICAVEL}
     * @param motivo motivo do nao verificavel; omitido nos demais
     */
    public record ResultadoRegra(String codigoRegra, String descricao, String resultado, String motivo) {
    }

    /**
     * @param codigoRegra codigo da regra na versao
     * @param descricaoRegra descricao da regra na versao
     * @param tipo {@code PENDENTE} ou {@code NAO_VERIFICAVEL}
     * @param explicacao texto fixo do protocolo (sem diagnostico, urgencia ou promessa)
     * @param motivo motivo do nao verificavel; omitido se pendente
     * @param responsavel {@code PACIENTE} ou {@code UNIDADE_EMISSORA}
     * @param orientacao proxima providencia textual, sem mudar o responsavel da regra
     * @param evidencias documento, campo, situacao e origem considerados
     */
    public record Pendencia(String codigoRegra, String descricaoRegra, String tipo, String explicacao, String motivo,
            String responsavel, String orientacao, List<Evidencia> evidencias) {
    }

    /**
     * @param documentoId documento considerado
     * @param campo campo considerado
     * @param situacao situacao do campo quando a analise foi feita
     * @param origem pagina e trecho na leitura daquela revisao; omitida quando nao ha
     */
    public record Evidencia(String documentoId, String campo, String situacao, OrigemResponse origem) {
    }

    /** Resumo simulado, ligado a esta analise. Os itens usam somente pendencias verificadas. */
    public record Resumo(String texto, String origem, boolean simulado, List<ItemResumo> itens) {
    }

    /** Acao concreta e codigo da regra que originou a pendencia. */
    public record ItemResumo(String codigoRegra, String acao, String responsavel) {
    }

    /**
     * Comparacao com a analise anterior (RC-16). "Resolvida" so existe entre analises concluidas e registradas; nao e
     * aprovacao do SUS.
     *
     * @param analiseAnteriorId analise com que esta foi comparada, como texto
     * @param revisaoAnterior revisao avaliada pela anterior
     * @param executadaEmAnterior conclusao da anterior em ISO-8601 UTC
     * @param itens regras com pendencia em pelo menos uma das duas, na ordem desta analise; pode ser vazia
     */
    public record Comparacao(String analiseAnteriorId, int revisaoAnterior, String executadaEmAnterior,
            List<ItemComparacao> itens) {
    }

    /**
     * @param codigoRegra codigo da regra
     * @param descricaoRegra descricao da regra nesta analise
     * @param situacao {@code RESOLVIDA}, {@code CONTINUA} ou {@code NOVA}
     * @param resultadoAnterior resultado da regra na analise anterior
     * @param resultadoAtual resultado da regra nesta analise
     */
    public record ItemComparacao(String codigoRegra, String descricaoRegra, String situacao,
            String resultadoAnterior, String resultadoAtual) {
    }

    /**
     * @param analise analise gravada
     * @return resposta publica correspondente, sem comparacao
     */
    public static AnaliseResponse de(Analise analise) {
        return de(analise, null);
    }

    /**
     * @param analise analise gravada
     * @param comparacao comparacao com a anterior, calculada pelo servico; {@code null} se nao ha
     * @return resposta publica correspondente
     */
    public static AnaliseResponse de(Analise analise, ComparacaoDeAnalises comparacao) {
        return new AnaliseResponse(Long.toString(analise.id()), analise.revisao(), analise.vigente(),
                iso(analise.executadaEm()), new Protocolo(analise.protocoloIdentificador(), analise.protocoloVersao()),
                analise.conferenciaEm() == null ? null : iso(analise.conferenciaEm()),
                analise.dataReferencia() == null ? null : analise.dataReferencia().toString(),
                analise.resultadoGeral().name(), rotulo(analise.resultadoGeral().name()), true, AVISO,
                analise.documentos().stream().map(AnaliseResponse::documento).toList(),
                analise.resultados().stream().map(AnaliseResponse::resultado).toList(),
                analise.pendencias().stream().map(AnaliseResponse::pendencia).toList(),
                analise.resumoTexto() == null ? null : new Resumo(analise.resumoTexto(), analise.resumoOrigem(), true,
                        analise.pendencias().stream().map(p -> new ItemResumo(p.codigoRegra(), p.explicacao(),
                                p.responsavel().name())).toList()),
                comparacao == null ? null : comparacao(comparacao));
    }

    private static Comparacao comparacao(ComparacaoDeAnalises comparacao) {
        return new Comparacao(Long.toString(comparacao.analiseAnteriorId()), comparacao.revisaoAnterior(),
                iso(comparacao.executadaEmAnterior()),
                comparacao.itens().stream().map(item -> new ItemComparacao(item.codigoRegra(), item.descricaoRegra(),
                        item.situacao().name(), item.resultadoAnterior().name(), item.resultadoAtual().name()))
                        .toList());
    }

    private static Documento documento(DocumentoDaAnalise documento) {
        return new Documento(Long.toString(documento.documentoId()), documento.nomeArquivo(),
                documento.tipoDeclarado().name(), documento.disponivel());
    }

    private static ResultadoRegra resultado(ResultadoGravado resultado) {
        return new ResultadoRegra(resultado.codigoRegra(), resultado.descricaoRegra(), resultado.resultado().name(),
                resultado.motivo() == null ? null : resultado.motivo().name());
    }

    private static Pendencia pendencia(PendenciaGravada pendencia) {
        return new Pendencia(pendencia.codigoRegra(), pendencia.descricaoRegra(), pendencia.tipo().name(),
                pendencia.explicacao(), pendencia.motivo() == null ? null : pendencia.motivo().name(),
                pendencia.responsavel().name(), OrientacaoDaPendencia.de(pendencia),
                pendencia.evidencias().stream().map(AnaliseResponse::evidencia).toList());
    }

    private static String rotulo(String resultado) {
        return switch (resultado) {
            case "COM_PENDENCIAS" -> "Com pendências";
            case "REQUER_CONFERENCIA" -> "Confira a leitura";
            case "SEM_PENDENCIAS_IDENTIFICADAS" -> "Sem pendências identificadas";
            default -> throw new IllegalArgumentException("Resultado geral desconhecido");
        };
    }

    private static Evidencia evidencia(EvidenciaGravada evidencia) {
        OrigemResponse origem = evidencia.pagina() == null && evidencia.trecho() == null ? null
                : new OrigemResponse(evidencia.pagina(), evidencia.trecho());
        return new Evidencia(Long.toString(evidencia.documentoId()), evidencia.codigo().name(),
                evidencia.situacao().name(), origem);
    }

    static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
