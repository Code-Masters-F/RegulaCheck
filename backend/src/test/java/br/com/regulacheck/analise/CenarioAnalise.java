package br.com.regulacheck.analise;

import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.analise.service.AvaliacaoDoPedido;
import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Atalhos dos testes das analises (RC-13): a API em memoria (envio, leitura simulada, conferencia e analises pelas
 * rotas reais) e o {@link AnaliseService} montado sobre os mesmos repositorios, com uma avaliacao que pode provocar
 * alteracoes ou falhas entre avaliar e gravar.
 */
final class CenarioAnalise {

    private CenarioAnalise() {
    }

    /** Algo que o teste faz depois da avaliacao e antes da gravacao. */
    @FunctionalInterface
    interface Intervalo {
        void executar() throws Exception;
    }

    /** POST /solicitacoes/{id}/analises (sem corpo). */
    static Resposta pedir(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, auth);
    }

    /** GET /solicitacoes/{id}/analises. */
    static Resposta historico(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.get("/solicitacoes/" + pedido + "/analises", auth);
    }

    /** GET /solicitacoes/{id}. */
    static JsonNode pedido(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.get("/solicitacoes/" + pedido, auth).json();
    }

    /** @return o resultado da regra na analise publicada */
    static String resultado(JsonNode analise, String codigoRegra) {
        for (JsonNode resultado : analise.get("resultados")) {
            if (codigoRegra.equals(resultado.get("codigoRegra").asText())) {
                return resultado.get("resultado").asText();
            }
        }
        throw new IllegalArgumentException("Regra ausente: " + codigoRegra);
    }

    /** @return a pendencia da regra na analise publicada */
    static JsonNode pendencia(JsonNode analise, String codigoRegra) {
        for (JsonNode pendencia : analise.get("pendencias")) {
            if (codigoRegra.equals(pendencia.get("codigoRegra").asText())) {
                return pendencia;
            }
        }
        throw new IllegalArgumentException("Pendencia ausente: " + codigoRegra);
    }

    /** @return servico de analises sobre os repositorios do cenario, com a avaliacao dada */
    static AnaliseService servico(CenarioDocumento c, AvaliacaoService avaliacao) {
        return new AnaliseService(c.cenario.analises, avaliacao, c.cenario.relogio);
    }

    /** @return avaliacao real (RC-12) sobre os repositorios do cenario */
    static AvaliacaoService avaliacao(CenarioDocumento c) {
        CenarioAuth a = c.cenario;
        return new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias, a.documentos, a.protocolos,
                MotorDeRegras.doProtocoloAcademico());
    }

    /**
     * @return avaliacao real que, depois de avaliar e antes de devolver, executa o intervalo (por exemplo, substituir
     *     um documento ou mudar uma decisao, como outra tela da paciente faria durante a analise)
     */
    static AvaliacaoService avaliacaoComIntervalo(CenarioDocumento c, Intervalo intervalo) {
        CenarioAuth a = c.cenario;
        return new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias, a.documentos, a.protocolos,
                MotorDeRegras.doProtocoloAcademico()) {
            @Override
            public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                AvaliacaoDoPedido avaliada = super.avaliar(sessao, solicitacaoId);
                try {
                    intervalo.executar();
                } catch (Exception falha) {
                    throw new IllegalStateException("Intervalo do teste falhou", falha);
                }
                return avaliada;
            }
        };
    }

    /** @return avaliacao que sempre lanca a falha dada */
    static AvaliacaoService avaliacaoQueFalha(CenarioDocumento c, RuntimeException falha) {
        CenarioAuth a = c.cenario;
        return new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias, a.documentos, a.protocolos,
                MotorDeRegras.doProtocoloAcademico()) {
            @Override
            public AvaliacaoDoPedido avaliar(SessaoAutenticada sessao, long solicitacaoId) {
                throw falha;
            }
        };
    }
}
