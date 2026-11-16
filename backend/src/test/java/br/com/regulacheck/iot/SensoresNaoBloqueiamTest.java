package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.repository.NovaLeitura;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * RC-22, US4 cenario 3 (FR-020, SC-006): com sensores fora da faixa, desatualizados ou sem leitura, enviar,
 * substituir e remover documento e excluir o rascunho respondem igual a um pedido sem sessao de captura. A exclusao
 * do rascunho apaga a sessao e as leituras do pedido (research D7).
 */
class SensoresNaoBloqueiamTest {

    @Test
    void fluxosDoPedidoNaoDependemDosSensores() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String controle = c.criarRascunho(c.authA);
            String comSensor = c.criarRascunho(c.authA);
            String sessao = c.api.enviar("POST", "/solicitacoes/" + comSensor + "/sessoes-captura", null, c.authA)
                    .json().get("id").asText();
            Instant agora = c.cenario.relogio.instant();
            gravar(c, sessao, TipoSensor.LUMINOSIDADE, "40", agora);
            gravar(c, sessao, TipoSensor.DISTANCIA, "90", agora.minus(Duration.ofMinutes(5)));

            for (String pedido : new String[] {controle, comSensor}) {
                Resposta envio = c.enviar(c.authA, pedido, "ENCAMINHAMENTO", "a.pdf", ArquivosDeTeste.pdf(1), null);
                assertEquals(201, envio.status(), envio.corpo());
                String documento = envio.json().get("id").asText();
                Resposta outro = c.enviar(c.authA, pedido, "IDENTIFICACAO", "b.pdf", ArquivosDeTeste.pdf(2), null);
                assertEquals(201, outro.status(), outro.corpo());
                Resposta substituicao = c.substituir(c.authA, pedido, documento, "ENCAMINHAMENTO", "c.pdf",
                        ArquivosDeTeste.pdf(3), null);
                assertEquals(201, substituicao.status(), substituicao.corpo());
                Resposta remocao = c.remover(c.authA, pedido, outro.json().get("id").asText());
                assertEquals(200, remocao.status(), remocao.corpo());
            }

            assertEquals(204, c.excluir(c.authA, comSensor, "EXCLUIR").status());
            assertEquals(204, c.excluir(c.authA, controle, "EXCLUIR").status());
            assertTrue(c.cenario.capturas.sessoes().isEmpty(), "sessao apagada com o rascunho");
            assertTrue(c.cenario.capturas.leituras().isEmpty(), "leituras apagadas com o rascunho");
        }
    }

    private static void gravar(CenarioDocumento c, String sessao, TipoSensor tipo, String valor, Instant medidaEm) {
        c.cenario.capturas.gravarLeitura(new NovaLeitura(Long.parseLong(sessao), tipo, new BigDecimal(valor), medidaEm,
                medidaEm)).orElseThrow();
    }
}
