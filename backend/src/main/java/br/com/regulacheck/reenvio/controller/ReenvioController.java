package br.com.regulacheck.reenvio.controller;

import br.com.regulacheck.analise.service.AnaliseEmAndamentoException;
import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.reenvio.dto.ReenvioResponse;
import br.com.regulacheck.reenvio.service.ReenvioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code POST /solicitacoes/{id}/reenvio}: verificar a versao corrigida do pedido (RC-16). Rota privada de
 * {@code PACIENTE}; autenticacao e perfil ja foram conferidos por {@code RotasPrivadas}. Este controller so traduz
 * HTTP e delega ao {@link ReenvioService}. O corpo e ignorado.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que entrega
 * a este controller o subcaminho {@code /{id}/reenvio}.
 */
public class ReenvioController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient ReenvioService servico;

    /** @param servico verificacao da versao corrigida */
    public ReenvioController(ReenvioService servico) {
        super("POST");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e do reenvio, ou seja, se o terceiro segmento e
     * {@code reenvio}. A validade do id e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length >= 3 && "reenvio".equals(segmentos[2]);
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
     * Verifica: 201 quando esta chamada registrou a nova analise; 200 quando parou na conferencia ou devolveu a
     * analise vigente. Analise em andamento: 409 com {@code Retry-After} (escrito aqui porque a traducao generica de
     * erros limpa os cabecalhos da resposta).
     */
    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        ReenvioService.Resultado resultado;
        try {
            resultado = servico.verificar(AutenticacaoFiltro.sessaoDe(requisicao), id);
        } catch (AnaliseEmAndamentoException emAndamento) {
            resposta.setHeader("Retry-After", Long.toString(emAndamento.segundosRestantes()));
            RespostaErro.tratar(resposta, emAndamento);
            return;
        }
        int status = resultado.analiseCriada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK;
        responderJson(resposta, status, ReenvioResponse.de(id, resultado));
    }

    /**
     * Interpreta {@code /{id}/reenvio}. Qualquer outra forma (id invalido, segmento a mais) e o 404 padrao, sem
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
