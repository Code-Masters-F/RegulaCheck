package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.extracao.service.VisaoConferencia;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Conferencia da transcricao de uma leitura como a API a publica. Toda resposta traz {@code simulada: true} e o
 * aviso fixo: a leitura e simulada. {@code liberadoParaSubmissao} e <strong>sempre {@code false}</strong>: a
 * conferencia nao libera submissao, nao gera resultado de regra, pendencia nem resultado geral, e nao muda o estado
 * do pedido.
 *
 * @param extracaoId identificador da leitura conferida, escrito como texto
 * @param situacao {@code PENDENTE}, {@code REQUER_CONFERENCIA}, {@code CONFERIDA} ou {@code DESATUALIZADA}
 * @param liberadoParaSubmissao sempre {@code false}
 * @param simulada sempre {@code true}
 * @param aviso texto fixo que identifica a leitura como simulada
 * @param atualizadaEm instante da decisao mais recente, ou o da leitura se nao ha decisao (ISO-8601 UTC)
 * @param resumo contagens da conferencia
 * @param documentos cada documento da leitura, com campos, decisoes e orientacoes
 */
public record ConferenciaResponse(String extracaoId, String situacao, boolean liberadoParaSubmissao,
        boolean simulada, String aviso, String atualizadaEm, ResumoResponse resumo,
        List<DocumentoConferidoResponse> documentos) {

    /**
     * Contagens da conferencia.
     *
     * @param camposNaoVerificados campos ilegiveis, nao localizados ou com divergencia apontada
     * @param camposSemDecisao campos identificados que a paciente ainda nao confirmou nem contestou
     * @param divergencias decisoes vigentes de divergencia
     */
    public record ResumoResponse(int camposNaoVerificados, int camposSemDecisao, int divergencias) {
    }

    /**
     * @param visao conferencia calculada pelo servico
     * @return resposta publica correspondente
     */
    public static ConferenciaResponse de(VisaoConferencia visao) {
        var leitura = visao.conferencia().leitura().extracao();
        var decisoes = visao.conferencia().decisoes();
        return new ConferenciaResponse(Long.toString(leitura.id()), visao.avaliacao().situacao().name(), false, true,
                ExtracaoResponse.AVISO, visao.atualizadaEm().truncatedTo(ChronoUnit.MILLIS).toString(),
                new ResumoResponse(visao.avaliacao().camposNaoVerificados(), visao.avaliacao().camposSemDecisao(),
                        visao.avaliacao().divergencias()),
                leitura.documentos().stream().map(d -> DocumentoConferidoResponse.de(d, decisoes)).toList());
    }
}
