package br.com.regulacheck.iot.repository;

import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.SessaoCaptura;
import br.com.regulacheck.iot.entity.TipoSensor;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * O que a consulta de telemetria precisa de um pedido: a sessao de captura mais recente (aberta ou nao) e, para
 * cada sensor, a leitura de instante de medicao mais recente dessa sessao.
 *
 * @param sessao sessao mais recente do pedido; {@code null} se o pedido nunca teve sessao
 * @param ultimas ultima leitura por tipo; tipos sem leitura ficam fora do mapa
 */
public record TelemetriaDoPedido(SessaoCaptura sessao, Map<TipoSensor, LeituraSensor> ultimas) {

    /**
     * Copia defensiva do mapa.
     */
    public TelemetriaDoPedido {
        EnumMap<TipoSensor, LeituraSensor> copia = new EnumMap<>(TipoSensor.class);
        copia.putAll(ultimas);
        ultimas = Collections.unmodifiableMap(copia);
    }

    /** @return telemetria de pedido sem sessao */
    public static TelemetriaDoPedido semSessao() {
        return new TelemetriaDoPedido(null, Map.of());
    }

    /**
     * @param tipo sensor
     * @return a ultima leitura do sensor, se houver
     */
    public Optional<LeituraSensor> ultima(TipoSensor tipo) {
        return Optional.ofNullable(ultimas.get(tipo));
    }
}
