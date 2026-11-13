package br.com.regulacheck.integracao.extracao;

/** Situacao da leitura de um documento. So {@link #LIDO} traz campos. */
public enum SituacaoLeitura {

    /** Documento lido: traz os campos, cada um em uma {@link SituacaoCampo}. */
    LIDO,

    /** Arquivo protegido (por exemplo, com senha): nada foi lido. */
    PROTEGIDO,

    /** Nada foi extraido do arquivo. Nao significa que os campos estejam ausentes. */
    LEITURA_VAZIA
}
