package br.com.regulacheck.iot.entity;

/**
 * Os dois sensores simulados da estacao de captura (RC-21), cada um com a unica unidade aceita.
 *
 * <p>A unidade e amarrada ao tipo aqui e no banco ({@code RC_LEITURA_SENSOR_CK_UNIDADE}): nao ha conversao de
 * unidades, uma leitura com unidade diferente da do tipo e recusada.
 */
public enum TipoSensor {

    /** Iluminacao sobre o documento, em lux. */
    LUMINOSIDADE("LUX"),

    /** Distancia entre a camera e o documento, em centimetros. */
    DISTANCIA("CM");

    private final String unidade;

    TipoSensor(String unidade) {
        this.unidade = unidade;
    }

    /**
     * @return a unidade aceita para o tipo ({@code LUX} ou {@code CM})
     */
    public String unidade() {
        return unidade;
    }
}
