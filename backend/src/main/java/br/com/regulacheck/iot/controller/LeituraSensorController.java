package br.com.regulacheck.iot.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.iot.dto.LeituraRegistradaResponse;
import br.com.regulacheck.iot.dto.LeituraSensorRequest;
import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.repository.NovaLeitura;
import br.com.regulacheck.iot.service.CapturaService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code POST /sessoes-captura/{id}/leituras}: o simulador envia uma leitura por requisicao (RC-21). Publicada por
 * {@code RotasPrivadas.registrarTelemetria}, entao o {@code SimuladorFiltro} ja conferiu a chave
 * {@code X-Chave-Simulador} antes de chegar aqui.
 *
 * <p>Ordem das conferencias (contrato e {@code docs/api/erros.md}):
 * <ol>
 *   <li>401 sem chave ou chave errada; 403 conta de paciente/administrador (filtro);</li>
 *   <li>404 sessao inexistente, encerrada ou com id malformado (mesma resposta), conferida <em>antes</em> de ler o
 *       corpo, para que um corpo invalido nao revele nada sobre a sessao;</li>
 *   <li>415/400 de formato do corpo ({@code CorpoJson}: tipo de conteudo, tamanho, JSON);</li>
 *   <li>400 de conteudo ({@link LeituraSensorRequest});</li>
 *   <li>gravacao sob bloqueio, que confere a sessao de novo (404 se ela sumiu nesse intervalo); 201.</li>
 * </ol>
 * O corpo e dado nao confiavel e nunca vai para o log.
 */
public class LeituraSensorController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient CapturaService servico;

    /**
     * @param servico casos de uso da estacao de captura
     */
    public LeituraSensorController(CapturaService servico) {
        super("POST");
        this.servico = servico;
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long sessaoId = sessaoIdDe(requisicao);
        servico.exigirSessaoAberta(sessaoId);
        JsonNode corpo = CorpoJson.ler(requisicao.getInputStream(), requisicao.getContentType(), JsonNode.class);
        NovaLeitura nova = LeituraSensorRequest.validar(corpo, sessaoId, servico.agora());
        LeituraSensor gravada = servico.registrarLeitura(nova);
        responderJson(resposta, HttpServletResponse.SC_CREATED, LeituraRegistradaResponse.de(gravada));
    }

    private static long sessaoIdDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        String[] segmentos = caminho == null ? new String[0] : caminho.split("/", -1);
        if (segmentos.length != 3 || !ID.matcher(segmentos[1]).matches() || !"leituras".equals(segmentos[2])) {
            throw Erros.naoEncontrado();
        }
        return Long.parseLong(segmentos[1]);
    }
}
