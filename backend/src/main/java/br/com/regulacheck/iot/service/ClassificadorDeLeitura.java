package br.com.regulacheck.iot.service;

import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.TipoSensor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Classifica a ultima leitura de um sensor em situacao e orientacao (RC-22). Funcao pura: nao conhece HTTP nem
 * banco, e o mesmo par (leitura, instante) sempre da o mesmo resultado.
 *
 * <p>Precedencia (research D5):
 * <ol>
 *   <li>sem leitura → {@link SituacaoSensor#SEM_LEITURA};</li>
 *   <li>medida ha mais que {@link FaixasDemonstrativas#PRAZO_ATUALIDADE} → {@link SituacaoSensor#DESATUALIZADA},
 *       mesmo que o valor esteja fora da faixa: uma leitura antiga nao autoriza dizer nada sobre a condicao de
 *       agora;</li>
 *   <li>senao, abaixo, dentro ou acima da faixa demonstrativa, com limites inclusivos.</li>
 * </ol>
 * Uma leitura medida no futuro (dentro da tolerancia aceita no envio) conta como atual.
 */
public final class ClassificadorDeLeitura {

    /**
     * Resultado da classificacao de um sensor.
     *
     * @param situacao situacao do sensor
     * @param orientacao orientacao correspondente
     */
    public record Classificacao(SituacaoSensor situacao, Orientacao orientacao) {
    }

    private ClassificadorDeLeitura() {
    }

    /**
     * @param tipo sensor
     * @param ultima ultima leitura do sensor, se houver
     * @param agora instante da consulta (relogio do servidor)
     * @return situacao e orientacao do sensor
     */
    public static Classificacao classificar(TipoSensor tipo, Optional<LeituraSensor> ultima, Instant agora) {
        if (ultima.isEmpty()) {
            return new Classificacao(SituacaoSensor.SEM_LEITURA, Orientacao.CAPTURA_SEGUE_SEM_SENSOR);
        }
        LeituraSensor leitura = ultima.get();
        if (leitura.medidaEm().plus(FaixasDemonstrativas.PRAZO_ATUALIDADE).isBefore(agora)) {
            return new Classificacao(SituacaoSensor.DESATUALIZADA, Orientacao.VERIFICAR_ESTACAO);
        }
        BigDecimal valor = leitura.valor();
        if (valor.compareTo(FaixasDemonstrativas.minimo(tipo)) < 0) {
            return new Classificacao(SituacaoSensor.ABAIXO_DA_FAIXA,
                    tipo == TipoSensor.LUMINOSIDADE ? Orientacao.MELHORAR_ILUMINACAO : Orientacao.AFASTAR_DOCUMENTO);
        }
        if (valor.compareTo(FaixasDemonstrativas.maximo(tipo)) > 0) {
            return new Classificacao(SituacaoSensor.ACIMA_DA_FAIXA, tipo == TipoSensor.LUMINOSIDADE
                    ? Orientacao.REDUZIR_ILUMINACAO_OU_REFLEXO : Orientacao.APROXIMAR_DOCUMENTO);
        }
        return new Classificacao(SituacaoSensor.DENTRO_DA_FAIXA, Orientacao.CONDICAO_ADEQUADA);
    }
}
