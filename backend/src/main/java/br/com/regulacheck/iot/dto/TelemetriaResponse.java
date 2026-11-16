package br.com.regulacheck.iot.dto;

import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.SessaoCaptura;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.repository.TelemetriaDoPedido;
import br.com.regulacheck.iot.service.ClassificadorDeLeitura;
import br.com.regulacheck.iot.service.FaixasDemonstrativas;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resposta de {@code GET /solicitacoes/{id}/leituras} (RC-22): sempre os dois sensores, na ordem luminosidade,
 * distancia, cada um com a ultima leitura, a situacao e a orientacao. Campos ausentes ({@code sessao} sem sessao,
 * {@code ultimaLeitura} sem leitura) sao omitidos, como no resto da API.
 *
 * <p>Toda resposta traz {@code simulada: true} e o {@link #AVISO}: as leituras sao simuladas, os limites sao
 * demonstrativos e nada aqui comprova a legibilidade do arquivo nem impede o envio (FR-019).
 *
 * @param solicitacaoId pedido
 * @param sessao sessao mais recente do pedido; omitida se nao houver
 * @param consultadaEm instante da consulta (relogio do servidor)
 * @param prazoAtualidadeSegundos idade maxima de uma leitura para valer como atual
 * @param simulada sempre {@code true}
 * @param aviso texto fixo de limitacao
 * @param sensores os dois sensores
 */
public record TelemetriaResponse(String solicitacaoId, Sessao sessao, String consultadaEm,
        long prazoAtualidadeSegundos, boolean simulada, String aviso, List<Sensor> sensores) {

    /** Texto fixo de limitacao, presente em toda resposta. */
    public static final String AVISO = "Leituras simuladas, com limites demonstrativos e sem calibração. Elas não "
            + "comprovam a legibilidade do arquivo e não impedem o envio de documentos.";

    /**
     * @param id sessao
     * @param iniciadaEm abertura
     * @param aberta se ainda recebe leituras
     */
    public record Sessao(String id, String iniciadaEm, boolean aberta) {
    }

    /**
     * @param minimo limite minimo inclusivo
     * @param maximo limite maximo inclusivo
     */
    public record Faixa(BigDecimal minimo, BigDecimal maximo) {
    }

    /**
     * @param valor valor medido
     * @param medidaEm instante da medicao
     */
    public record UltimaLeitura(BigDecimal valor, String medidaEm) {
    }

    /**
     * @param codigo codigo estavel, contrato com a tela do app
     * @param mensagem texto fixo em portugues
     */
    public record OrientacaoResponse(String codigo, String mensagem) {
    }

    /**
     * @param tipo sensor
     * @param unidade unidade do sensor
     * @param faixa faixa demonstrativa
     * @param ultimaLeitura ultima leitura; omitida se nao houver
     * @param situacao situacao calculada
     * @param orientacao orientacao correspondente
     */
    public record Sensor(String tipo, String unidade, Faixa faixa, UltimaLeitura ultimaLeitura, String situacao,
            OrientacaoResponse orientacao) {
    }

    /**
     * @param solicitacaoId pedido
     * @param telemetria sessao e ultimas leituras
     * @param agora instante da consulta
     * @return a resposta, com a classificacao de cada sensor
     */
    public static TelemetriaResponse de(long solicitacaoId, TelemetriaDoPedido telemetria, Instant agora) {
        List<Sensor> sensores = new ArrayList<>();
        for (TipoSensor tipo : TipoSensor.values()) {
            Optional<LeituraSensor> ultima = telemetria.ultima(tipo);
            ClassificadorDeLeitura.Classificacao classificacao = ClassificadorDeLeitura.classificar(tipo, ultima, agora);
            sensores.add(new Sensor(tipo.name(), tipo.unidade(),
                    new Faixa(FaixasDemonstrativas.minimo(tipo), FaixasDemonstrativas.maximo(tipo)),
                    ultima.map(l -> new UltimaLeitura(l.valor(), iso(l.medidaEm()))).orElse(null),
                    classificacao.situacao().name(),
                    new OrientacaoResponse(classificacao.orientacao().name(), classificacao.orientacao().mensagem())));
        }
        SessaoCaptura sessao = telemetria.sessao();
        return new TelemetriaResponse(Long.toString(solicitacaoId),
                sessao == null ? null : new Sessao(Long.toString(sessao.id()), iso(sessao.iniciadaEm()), sessao.aberta()),
                iso(agora), FaixasDemonstrativas.PRAZO_ATUALIDADE.toSeconds(), true, AVISO, List.copyOf(sensores));
    }

    private static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
