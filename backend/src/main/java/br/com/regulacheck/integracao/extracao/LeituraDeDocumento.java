package br.com.regulacheck.integracao.extracao;

import java.util.List;

/**
 * Resultado da leitura de um documento.
 *
 * @param situacao lido, protegido ou leitura vazia
 * @param campos campos extraidos: os seis codigos quando {@code LIDO}; vazio nos demais casos
 */
public record LeituraDeDocumento(SituacaoLeitura situacao, List<CampoLido> campos) {

    /** Copia defensiva: a leitura e imutavel. */
    public LeituraDeDocumento {
        campos = campos == null ? List.of() : List.copyOf(campos);
    }

    /** @return leitura sem campos por arquivo protegido */
    public static LeituraDeDocumento protegido() {
        return new LeituraDeDocumento(SituacaoLeitura.PROTEGIDO, List.of());
    }

    /** @return leitura sem campos por nada ter sido extraido */
    public static LeituraDeDocumento vazia() {
        return new LeituraDeDocumento(SituacaoLeitura.LEITURA_VAZIA, List.of());
    }
}
