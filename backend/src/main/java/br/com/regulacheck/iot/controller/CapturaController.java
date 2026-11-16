package br.com.regulacheck.iot.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.iot.dto.SessaoCapturaResponse;
import br.com.regulacheck.iot.dto.TelemetriaResponse;
import br.com.regulacheck.iot.service.CapturaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Rotas da paciente na estacao de captura (RC-21/RC-22), entregues pelo {@code SolicitacaoController} (a rota
 * {@code /solicitacoes/*} ja e registrada em {@code RotasPrivadas} com perfil {@code PACIENTE}):
 * <ul>
 *   <li>{@code POST /solicitacoes/{id}/sessoes-captura}: abre a sessao (201) ou devolve a ja aberta (200);</li>
 *   <li>{@code GET /solicitacoes/{id}/leituras}: telemetria dos dois sensores com orientacao (200).</li>
 * </ul>
 * O id do pedido malformado tem a mesma resposta de pedido inexistente (404).
 */
public class CapturaController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");
    private static final String SESSOES = "sessoes-captura";
    private static final String LEITURAS = "leituras";

    private final transient CapturaService servico;

    /**
     * @param servico casos de uso da estacao de captura
     */
    public CapturaController(CapturaService servico) {
        super("GET", "POST");
        this.servico = servico;
    }

    /**
     * @param pathInfo caminho depois de {@code /solicitacoes}
     * @return {@code true} se a rota e {@code /{id}/sessoes-captura} ou {@code /{id}/leituras}
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length == 3 && (SESSOES.equals(segmentos[2]) || LEITURAS.equals(segmentos[2]));
    }

    /**
     * Atende a requisicao entregue pelo {@code SolicitacaoController}, com a mesma traducao de erros e o mesmo
     * controle de metodos de qualquer controller.
     *
     * @param requisicao requisicao HTTP
     * @param resposta resposta HTTP
     * @throws ServletException erro do container
     * @throws IOException falha de escrita
     */
    public void atender(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        service(requisicao, resposta);
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        if (!SESSOES.equals(recurso(requisicao))) {
            metodoNaoPermitido(resposta, "GET");
            return;
        }
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        CapturaService.SessaoAberta aberta = servico.abrirSessao(sessao, id);
        resposta.setHeader("Location", "/solicitacoes/" + id + "/" + LEITURAS);
        responderJson(resposta, aberta.criada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK,
                SessaoCapturaResponse.de(aberta.sessao(), aberta.criada()));
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        if (!LEITURAS.equals(recurso(requisicao))) {
            metodoNaoPermitido(resposta, "POST");
            return;
        }
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        responderJson(resposta, HttpServletResponse.SC_OK,
                TelemetriaResponse.de(id, servico.consultarTelemetria(sessao, id), servico.agora()));
    }

    private static String recurso(HttpServletRequest requisicao) {
        return requisicao.getPathInfo().split("/", -1)[2];
    }

    private static long idDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        String[] segmentos = caminho == null ? new String[0] : caminho.split("/", -1);
        if (segmentos.length != 3 || !ID.matcher(segmentos[1]).matches()) {
            throw Erros.naoEncontrado();
        }
        return Long.parseLong(segmentos[1]);
    }

    private static void metodoNaoPermitido(HttpServletResponse resposta, String permitidos) throws IOException {
        resposta.setHeader("Allow", permitidos);
        RespostaErro.escrever(resposta,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "Metodo HTTP nao permitido para este recurso.");
    }
}
