package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.service.ClassificadorDeLeitura;
import br.com.regulacheck.iot.service.ClassificadorDeLeitura.Classificacao;
import br.com.regulacheck.iot.service.Orientacao;
import br.com.regulacheck.iot.service.SituacaoSensor;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * RC-22 (FR-013 a FR-016): faixas demonstrativas de 300 a 1000 lux e 20 a 40 cm, limites inclusivos, e leitura
 * desatualizada depois de 60 s. Os valores esperados vem da spec (clarify de 16/11/2026), nao do codigo.
 */
class ClassificadorDeLeituraTest {

    private static final Instant AGORA = Instant.parse("2026-11-16T14:00:00Z");

    @Test
    void luminosidadeNasBordasDaFaixa() {
        assertClassifica(TipoSensor.LUMINOSIDADE, "299.999", AGORA, SituacaoSensor.ABAIXO_DA_FAIXA,
                Orientacao.MELHORAR_ILUMINACAO);
        assertClassifica(TipoSensor.LUMINOSIDADE, "300", AGORA, SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
        assertClassifica(TipoSensor.LUMINOSIDADE, "1000", AGORA, SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
        assertClassifica(TipoSensor.LUMINOSIDADE, "1000.001", AGORA, SituacaoSensor.ACIMA_DA_FAIXA,
                Orientacao.REDUZIR_ILUMINACAO_OU_REFLEXO);
    }

    @Test
    void distanciaNasBordasDaFaixa() {
        assertClassifica(TipoSensor.DISTANCIA, "19.999", AGORA, SituacaoSensor.ABAIXO_DA_FAIXA,
                Orientacao.AFASTAR_DOCUMENTO);
        assertClassifica(TipoSensor.DISTANCIA, "20", AGORA, SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
        assertClassifica(TipoSensor.DISTANCIA, "40", AGORA, SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
        assertClassifica(TipoSensor.DISTANCIA, "40.001", AGORA, SituacaoSensor.ACIMA_DA_FAIXA,
                Orientacao.APROXIMAR_DOCUMENTO);
    }

    @Test
    void prazoDeAtualidadeDe60Segundos() {
        assertClassifica(TipoSensor.LUMINOSIDADE, "500", AGORA.minusSeconds(60), SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
        assertClassifica(TipoSensor.LUMINOSIDADE, "500", AGORA.minus(Duration.ofMillis(60_001)),
                SituacaoSensor.DESATUALIZADA, Orientacao.VERIFICAR_ESTACAO);
        // Fora da faixa, mas antiga: nao vira orientacao de correcao.
        assertClassifica(TipoSensor.DISTANCIA, "80", AGORA.minusSeconds(120), SituacaoSensor.DESATUALIZADA,
                Orientacao.VERIFICAR_ESTACAO);
        // Medida um pouco no futuro (diferenca de relogio aceita no envio) conta como atual.
        assertClassifica(TipoSensor.DISTANCIA, "30", AGORA.plusSeconds(10), SituacaoSensor.DENTRO_DA_FAIXA,
                Orientacao.CONDICAO_ADEQUADA);
    }

    @Test
    void semLeitura() {
        Classificacao c = ClassificadorDeLeitura.classificar(TipoSensor.LUMINOSIDADE, Optional.empty(), AGORA);
        assertEquals(SituacaoSensor.SEM_LEITURA, c.situacao());
        assertEquals(Orientacao.CAPTURA_SEGUE_SEM_SENSOR, c.orientacao());
    }

    private static void assertClassifica(TipoSensor tipo, String valor, Instant medidaEm, SituacaoSensor situacao,
            Orientacao orientacao) {
        LeituraSensor leitura = new LeituraSensor(1, 1, tipo, new BigDecimal(valor), medidaEm, medidaEm);
        Classificacao c = ClassificadorDeLeitura.classificar(tipo, Optional.of(leitura), AGORA);
        assertEquals(situacao, c.situacao(), tipo + " " + valor);
        assertEquals(orientacao, c.orientacao(), tipo + " " + valor);
    }
}
