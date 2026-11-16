package br.com.regulacheck.iot.service;

import br.com.regulacheck.iot.entity.TipoSensor;
import java.math.BigDecimal;
import java.time.Duration;

/**
 * Ponto unico dos limites da estacao de captura simulada (RC-21/RC-22, decisoes do clarify de 16/11/2026).
 *
 * <p><strong>Todos os valores sao demonstrativos e sem calibracao real.</strong> Servem para mostrar a orientacao
 * a paciente na demonstracao academica; nao comprovam que a foto ficou legivel e nao bloqueiam envio de documento
 * nem salvamento do rascunho. Mudar um valor aqui muda a API inteira; a documentacao
 * ({@code simulador-iot/README.md}) precisa acompanhar.
 *
 * <p>Ha dois tipos de limite, de propositos diferentes:
 * <ul>
 *   <li><strong>faixa demonstrativa</strong> (inclusiva): decide a orientacao; fora dela a leitura e aceita e
 *       gravada normalmente;</li>
 *   <li><strong>limite fisico</strong>: valor fora dele e implausivel para o sensor e a leitura e recusada (400).</li>
 * </ul>
 */
public final class FaixasDemonstrativas {

    /** Luminosidade minima da faixa demonstrativa, em lux (inclusivo). */
    public static final BigDecimal LUMINOSIDADE_MINIMA = new BigDecimal("300");
    /** Luminosidade maxima da faixa demonstrativa, em lux (inclusivo). */
    public static final BigDecimal LUMINOSIDADE_MAXIMA = new BigDecimal("1000");
    /** Distancia minima da faixa demonstrativa, em cm (inclusivo). */
    public static final BigDecimal DISTANCIA_MINIMA = new BigDecimal("20");
    /** Distancia maxima da faixa demonstrativa, em cm (inclusivo). */
    public static final BigDecimal DISTANCIA_MAXIMA = new BigDecimal("40");

    /** Idade maxima de uma leitura, contada do instante da medicao, para ainda valer como atual. */
    public static final Duration PRAZO_ATUALIDADE = Duration.ofSeconds(60);
    /** Quanto o instante da medicao pode estar a frente do relogio do servidor (diferenca de relogios). */
    public static final Duration TOLERANCIA_FUTURO = Duration.ofSeconds(30);

    /** Maior luminosidade aceita (luz do sol direta fica em torno de 100000 lux). */
    public static final BigDecimal LUMINOSIDADE_FISICA_MAXIMA = new BigDecimal("200000");
    /** Maior distancia aceita, em cm; a distancia precisa ser maior que zero. */
    public static final BigDecimal DISTANCIA_FISICA_MAXIMA = new BigDecimal("500");

    /** Casas decimais gravadas ({@code NUMBER(12,3)}). */
    public static final int CASAS_DECIMAIS = 3;

    private FaixasDemonstrativas() {
    }

    /**
     * @param tipo sensor
     * @return limite minimo da faixa demonstrativa do sensor
     */
    public static BigDecimal minimo(TipoSensor tipo) {
        return tipo == TipoSensor.LUMINOSIDADE ? LUMINOSIDADE_MINIMA : DISTANCIA_MINIMA;
    }

    /**
     * @param tipo sensor
     * @return limite maximo da faixa demonstrativa do sensor
     */
    public static BigDecimal maximo(TipoSensor tipo) {
        return tipo == TipoSensor.LUMINOSIDADE ? LUMINOSIDADE_MAXIMA : DISTANCIA_MAXIMA;
    }

    /**
     * Confere o limite fisico de plausibilidade: luminosidade de 0 a
     * 200000 lux; distancia maior que 0 e ate 500 cm.
     *
     * @param tipo sensor
     * @param valor valor recebido
     * @return {@code true} se o valor e plausivel para o sensor
     */
    public static boolean fisicamentePlausivel(TipoSensor tipo, BigDecimal valor) {
        if (tipo == TipoSensor.LUMINOSIDADE) {
            return valor.signum() >= 0 && valor.compareTo(LUMINOSIDADE_FISICA_MAXIMA) <= 0;
        }
        return valor.signum() > 0 && valor.compareTo(DISTANCIA_FISICA_MAXIMA) <= 0;
    }
}
