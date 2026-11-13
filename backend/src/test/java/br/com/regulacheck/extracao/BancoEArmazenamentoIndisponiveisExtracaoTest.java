package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Banco ou area de arquivos indisponiveis: 503 padrao sem detalhe tecnico e nada gravado. */
class BancoEArmazenamentoIndisponiveisExtracaoTest {

    private static final ExcecaoNegocio FORA = new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
            "Servico temporariamente indisponivel. Tente novamente em instantes.");

    @Test
    void bancoForaNaConsultaDoContextoDa503() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.cenario.extracoes.falharNaProximaConsulta(FORA);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains(c.raiz.toString()));
            assertEquals(0, c.cenario.extracoes.total());
        }
    }

    @Test
    void bancoForaNaGravacaoDa503ENadaFicaGravado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.cenario.extracoes.falharNaProximaGravacao(FORA);

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertEquals(0, c.cenario.extracoes.total());
            // Passada a falha, a repeticao funciona.
            assertEquals(201, c.lerDocumentos(c.authA, pedido).status());
        }
    }

    @Test
    void bancoForaNaConsultaDaLeituraDa503() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            c.lerDocumentos(c.authA, pedido);
            c.cenario.extracoes.falharNaProximaConsulta(FORA);

            Resposta r = c.consultarLeitura(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
        }
    }

    @Test
    void arquivoDeDocumentoAusenteNaAreaPrivadaDa503ENadaEGravado() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            try (Stream<Path> itens = Files.list(c.raiz)) {
                for (Path arquivo : itens.filter(Files::isRegularFile).toList()) {
                    Files.delete(arquivo);
                }
            }

            Resposta r = c.lerDocumentos(c.authA, pedido);

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains(c.raiz.toString()));
            assertEquals(0, c.cenario.extracoes.total());
        }
    }
}
