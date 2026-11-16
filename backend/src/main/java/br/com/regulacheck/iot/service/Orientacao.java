package br.com.regulacheck.iot.service;

/**
 * Orientacao dada a paciente para cada sensor (RC-22). O <strong>codigo</strong> e o contrato com a tela do app
 * (Epico 6); a mensagem e um texto fixo em portugues, escrito pelo projeto, que nunca afirma que a foto ficou
 * legivel nem impede o envio do documento.
 */
public enum Orientacao {

    /** Leitura atual dentro da faixa. */
    CONDICAO_ADEQUADA("Iluminação e distância adequadas para capturar o documento."),
    /** Luminosidade abaixo da faixa. */
    MELHORAR_ILUMINACAO("Ilumine melhor o documento antes de capturar."),
    /** Luminosidade acima da faixa. */
    REDUZIR_ILUMINACAO_OU_REFLEXO("Reduza a iluminação direta ou o reflexo sobre o documento."),
    /** Distancia abaixo da faixa: camera perto demais. */
    AFASTAR_DOCUMENTO("Afaste um pouco a câmera do documento."),
    /** Distancia acima da faixa: camera longe demais. */
    APROXIMAR_DOCUMENTO("Aproxime um pouco a câmera do documento."),
    /** Leitura desatualizada. */
    VERIFICAR_ESTACAO("Leitura antiga. Verifique se a estação de captura está ligada; você pode enviar o documento "
            + "mesmo assim."),
    /** Sem leitura. */
    CAPTURA_SEGUE_SEM_SENSOR("Sem leitura do sensor. Você pode capturar e enviar o documento normalmente.");

    private final String mensagem;

    Orientacao(String mensagem) {
        this.mensagem = mensagem;
    }

    /**
     * @return texto fixo mostrado a paciente
     */
    public String mensagem() {
        return mensagem;
    }
}
