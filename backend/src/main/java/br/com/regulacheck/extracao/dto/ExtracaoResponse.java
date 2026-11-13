package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Leitura (simulada) dos documentos de um pedido como a API a publica. Toda resposta traz {@code simulada: true} e o
 * aviso fixo: o sistema nao le arquivos de verdade e nao afirma leitura real. Nao traz resultado de completude,
 * pendencia nem estado do pedido.
 *
 * @param id identificador da leitura: numero decimal opaco, escrito como texto (a conferencia da transcricao, RC-11,
 *     o usara)
 * @param situacao {@code ATUAL} (feita na versao atual dos documentos) ou {@code DESATUALIZADA}
 * @param versaoConjuntoDocumental versao do conjunto documental em que a leitura foi feita
 * @param simulada sempre {@code true}
 * @param aviso texto fixo que identifica a leitura como simulada
 * @param criadaEm instante da leitura em ISO-8601 UTC
 * @param documentos leitura de cada documento
 */
public record ExtracaoResponse(String id, String situacao, int versaoConjuntoDocumental, boolean simulada, String aviso,
        String criadaEm, List<DocumentoLidoResponse> documentos) {

    /** Aviso fixo de leitura simulada. */
    public static final String AVISO = "Leitura simulada com dados sintéticos; não é leitura real dos documentos.";

    /**
     * @param registrada leitura gravada e a versao atual do pedido
     * @return resposta publica correspondente
     */
    public static ExtracaoResponse de(ExtracaoRegistrada registrada) {
        return new ExtracaoResponse(Long.toString(registrada.extracao().id()), registrada.atual() ? "ATUAL" : "DESATUALIZADA",
                registrada.extracao().revisao(), true, AVISO,
                registrada.extracao().criadaEm().truncatedTo(ChronoUnit.MILLIS).toString(),
                registrada.extracao().documentos().stream().map(DocumentoLidoResponse::de).toList());
    }
}
