package br.com.regulacheck.extracao;

import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioConferencia.Preparo;
import org.junit.jupiter.api.Test;

/** Banco indisponivel na conferencia: 503 padrao sem detalhe tecnico e nada gravado pela metade. */
class BancoIndisponivelConferenciaTest {

    private static final ExcecaoNegocio FORA = new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
            "Servico temporariamente indisponivel. Tente novamente em instantes.");

    @Test
    void bancoForaNaConsultaDa503SemDetalheTecnicoEPassadaAFalhaFunciona() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            c.cenario.conferencias.falharNaProximaConsulta(FORA);

            Resposta r = c.consultarConferencia(c.authA, p.pedido(), p.extracao());

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains(c.raiz.toString()));
            assertFalse(r.corpo().contains("Maria Exemplo"));
            assertEquals(200, c.consultarConferencia(c.authA, p.pedido(), p.extracao()).status());
        }
    }

    @Test
    void bancoForaNoRegistroDa503NadaFicaGravadoEARepeticaoFunciona() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            c.cenario.conferencias.falharNoProximoRegistro(FORA);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.encaminhamento(), "NOME")));

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertEquals(0, c.cenario.conferencias.total());
            assertEquals(0, c.cenario.conferencias.gravacoes());
            // passada a falha, a repeticao funciona e registra
            assertEquals(200, c.decidir(c.authA, p.pedido(), p.extracao(),
                    lote(confirmar(p.encaminhamento(), "NOME"))).status());
            assertEquals(1, c.cenario.conferencias.total());
        }
    }

    @Test
    void bancoForaNaLeituraDoPedidoAntesDoRegistroDa503() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            Preparo p = CenarioConferencia.preparar(c);
            // a primeira consulta do PUT (conferir a dona antes de ler o corpo) falha
            c.cenario.conferencias.falharNaProximaConsulta(FORA);

            Resposta r = c.decidir(c.authA, p.pedido(), p.extracao(), lote(confirmar(p.encaminhamento(), "NOME")));

            assertEquals(503, r.status());
            assertEquals(0, c.cenario.conferencias.total());
        }
    }
}
