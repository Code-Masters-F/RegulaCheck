package br.com.regulacheck.config;

import java.util.Map;

/**
 * Configuracao de execucao lida de variaveis de ambiente.
 *
 * <p>Nenhuma variavel e obrigatoria nesta etapa: sem valor, valem os padroes. As variaveis do
 * banco ({@code DB_*}), de armazenamento e do simulador entram quando seus recursos existirem
 * (historias posteriores) e nunca sao exigidas para iniciar a fundacao. O arquivo {@code .env}
 * nao e lido automaticamente; as variaveis devem ser exportadas no shell ou na IDE.
 *
 * @param host endereco de escuta; {@code 0.0.0.0} permite acesso do emulador e de aparelhos na rede
 * @param porta porta HTTP; {@code 0} escolhe uma porta livre (usado em testes)
 */
public record Configuracao(String host, int porta) {

    /** Porta padrao quando {@code SERVER_PORT} nao e informada. */
    public static final int PORTA_PADRAO = 8080;

    /** Endereco padrao de escuta quando {@code SERVER_HOST} nao e informado. */
    public static final String HOST_PADRAO = "0.0.0.0";

    /**
     * Le a configuracao do ambiente do processo.
     *
     * @return configuracao com os padroes aplicados as variaveis ausentes
     * @throws IllegalArgumentException se {@code SERVER_PORT} nao for um inteiro entre 0 e 65535
     */
    public static Configuracao deAmbiente() {
        return deMapa(System.getenv());
    }

    /**
     * Le a configuracao de um mapa de variaveis (facilita testes sem alterar o ambiente real).
     *
     * @param variaveis mapa nome-valor das variaveis
     * @return configuracao com os padroes aplicados
     * @throws IllegalArgumentException se {@code SERVER_PORT} for invalida
     */
    public static Configuracao deMapa(Map<String, String> variaveis) {
        String host = valorOuPadrao(variaveis.get("SERVER_HOST"), HOST_PADRAO);
        String portaTexto = variaveis.get("SERVER_PORT");
        int porta = PORTA_PADRAO;
        if (portaTexto != null && !portaTexto.isBlank()) {
            try {
                porta = Integer.parseInt(portaTexto.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SERVER_PORT deve ser um numero inteiro entre 0 e 65535.");
            }
            if (porta < 0 || porta > 65535) {
                throw new IllegalArgumentException("SERVER_PORT deve ser um numero inteiro entre 0 e 65535.");
            }
        }
        return new Configuracao(host, porta);
    }

    private static String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
