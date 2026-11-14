package br.com.regulacheck.analise.controller;

import br.com.regulacheck.analise.dto.AnaliseResponse;
import br.com.regulacheck.analise.dto.HistoricoAnalisesResponse;
import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import br.com.regulacheck.analise.service.AnaliseEmAndamentoException;
import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.analise.service.AnaliseService.ResultadoAnalise;
import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes/{id}/analises}: pedir a analise do pedido ({@code POST}) e consultar o historico
 * ({@code GET}). Rota privada de {@code PACIENTE}; autenticacao e perfil ja foram conferidos por
 * {@code RotasPrivadas}. A leitura dos documentos e <strong>simulada</strong> e o protocolo e academico e ficticio.
 * Este controller so traduz HTTP e delega ao {@link AnaliseService}.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que
 * entrega a este controller o subcaminho de analises. O {@code POST} nao tem corpo: o corpo e ignorado.
 */
public class AnaliseController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient AnaliseService servico;

    /** @param servico casos de uso das analises */
    public AnaliseController(AnaliseService servico) {
        super("GET", "POST");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e de analises, ou seja, se o terceiro segmento e
     * {@code analises}. A validade do id e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length >= 3 && "analises".equals(segmentos[2]);
    }

    /**
     * Atende a requisicao entregue pelo {@code SolicitacaoController}, com a mesma traducao de erros e o mesmo
     * controle de metodos de qualquer controller.
     *
     * @param requisicao requisicao ja autenticada e autorizada por perfil
     * @param resposta resposta HTTP
     * @throws ServletException se o conteiner falhar ao despachar
     * @throws IOException se a escrita na resposta falhar
     */
    public void atender(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        service(requisicao, resposta);
    }

    /**
     * Pede a analise: 201 com a analise gravada agora, 200 com a vigente que ja valia. Em andamento: 409 com
     * {@code Retry-After} (escrito aqui porque a traducao generica de erros limpa os cabecalhos da resposta).
     */
    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        ResultadoAnalise resultado;
        try {
            resultado = servico.analisar(sessao, id);
        } catch (AnaliseEmAndamentoException emAndamento) {
            resposta.setHeader("Retry-After", Long.toString(emAndamento.segundosRestantes()));
            RespostaErro.tratar(resposta, emAndamento);
            return;
        }
        int status = resultado.criada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK;
        responderJson(resposta, status, AnaliseResponse.de(resultado.analise()));
    }

    /** Consulta o historico: todas as analises, a vigente marcada e a analise em andamento, se houver. */
    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        HistoricoDeAnalises historico = servico.consultar(sessao, id);
        boolean interrompida = historico.analiseIniciadaEm() != null
                && servico.interrompida(historico.analiseIniciadaEm());
        responderJson(resposta, HttpServletResponse.SC_OK, HistoricoAnalisesResponse.de(id, historico,
                AnaliseService.LIMITE_EM_ANDAMENTO, interrompida, AnaliseService.LIMITE_HISTORICO));
    }

    /**
     * Interpreta {@code /{id}/analises}. Qualquer outra forma (id invalido, segmento a mais) e o 404 padrao, sem
     * consultar o banco, igual a um pedido inexistente.
     */
    private static long idDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        if (caminho == null) {
            throw Erros.naoEncontrado();
        }
        String[] segmentos = caminho.split("/", -1);
        if (segmentos.length != 3 || !ID.matcher(segmentos[1]).matches()) {
            throw Erros.naoEncontrado();
        }
        return Long.parseLong(segmentos[1]);
    }
}
