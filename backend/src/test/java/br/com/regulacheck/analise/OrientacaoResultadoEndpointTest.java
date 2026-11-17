package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.analise.dto.OrientacaoDaPendencia;
import br.com.regulacheck.analise.entity.PendenciaGravada;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import com.fasterxml.jackson.databind.JsonNode;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.validacao.model.Resultado;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Contrato aditivo da orientacao de resultado da RC-15, com dados sinteticos. */
class OrientacaoResultadoEndpointTest {

    private CenarioDocumento c;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    @Test
    void rotulosTextuaisAcompanhamOsTresResultadosSemMudarOValorDoMotor() throws Exception {
        Pedido comPendencia = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, comPendencia);
        JsonNode pendente = pedir(c, c.authA, comPendencia.id()).json();
        assertEquals("COM_PENDENCIAS", pendente.get("resultadoGeral").asText());
        assertEquals("Com pendências", pendente.get("rotuloResultado").asText());

        Pedido conferir = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        JsonNode naoVerificavel = pedir(c, c.authA, conferir.id()).json();
        assertEquals("REQUER_CONFERENCIA", naoVerificavel.get("resultadoGeral").asText());
        assertEquals("Confira a leitura", naoVerificavel.get("rotuloResultado").asText());
        JsonNode orientacao = CenarioAnalise.pendencia(naoVerificavel, "RC-R02");
        assertTrue(orientacao.get("orientacao").asText().contains("Confira"));
        assertFalse(orientacao.get("orientacao").asText().contains("está ausente"));

        Pedido semPendencia = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, semPendencia);
        JsonNode conforme = pedir(c, c.authA, semPendencia.id()).json();
        assertEquals("SEM_PENDENCIAS_IDENTIFICADAS", conforme.get("resultadoGeral").asText());
        assertEquals("Sem pendências identificadas", conforme.get("rotuloResultado").asText());
        assertTrue(conforme.get("aviso").asText().contains("não é aprovação"));
    }

    @Test
    void orientacaoDaRegraR02CondicionaErroNoOriginal() {
        PendenciaGravada pendencia = new PendenciaGravada("RC-R02", "Identificação coerente", Resultado.PENDENTE,
                "Nome ou data de nascimento diferem.", null, ResponsavelCorrecao.PACIENTE, List.of());
        String orientacao = OrientacaoDaPendencia.de(pendencia);
        assertTrue(orientacao.contains("Se o erro estiver no documento médico original"));
        assertTrue(orientacao.contains("unidade emissora"));
        assertFalse(orientacao.contains("O documento médico está errado"));
    }

    @Test
    void orientacaoDaUnidadePreservaEvidenciasResumoERevisao() throws Exception {
        Pedido p = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, p);
        JsonNode analise = pedir(c, c.authA, p.id()).json();
        JsonNode pendencia = CenarioAnalise.pendencia(analise, "RC-R04");
        assertEquals("UNIDADE_EMISSORA", pendencia.get("responsavel").asText());
        assertTrue(pendencia.get("orientacao").asText().contains("unidade emissora"));
        assertTrue(pendencia.get("evidencias").size() > 0);
        JsonNode comResumo = c.api.enviar("POST", "/solicitacoes/" + p.id() + "/resumo", null, c.authA).json();
        assertEquals(analise.get("revisao"), comResumo.get("revisao"));
        assertEquals(analise.get("pendencias"), comResumo.get("pendencias"));
        assertTrue(comResumo.get("resumo").get("simulado").asBoolean());
    }
}
