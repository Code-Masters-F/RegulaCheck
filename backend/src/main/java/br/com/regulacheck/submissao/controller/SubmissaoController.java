package br.com.regulacheck.submissao.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.submissao.dto.SubmissaoResponse;
import br.com.regulacheck.submissao.service.SubmissaoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes/{id}/submissoes-simuladas}: concluir a simulacao ({@code POST}) e consultar o comprovante
 * ({@code GET}) (RC-17). Rota privada de {@code PACIENTE}; autenticacao e perfil ja foram conferidos por
 * {@code RotasPrivadas}. Este controller so traduz HTTP e delega ao {@link SubmissaoService}. O corpo do {@code POST}
 * e ignorado. A submissao e simulada: nada e enviado ao SUS.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que entrega
 * a este controller o subcaminho {@code /{id}/submissoes-simuladas}.
 */
public class SubmissaoController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient SubmissaoService servico;

    /** @param servico casos de uso da submissao simulada */
    public SubmissaoController(SubmissaoService servico) {
        super("GET", "POST");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e da submissao simulada, ou seja, se o terceiro
     * segmento e {@code submissoes-simuladas}. A validade do id e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length >= 3 && "submissoes-simuladas".equals(segmentos[2]);
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

    /** Conclui: 201 quando esta chamada registrou a submissao; 200 com a submissao ja existente. */
    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SubmissaoService.Resultado resultado = servico.submeter(AutenticacaoFiltro.sessaoDe(requisicao), id);
        int status = resultado.criada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK;
        responderJson(resposta, status, SubmissaoResponse.de(resultado.submissao()));
    }

    /** Consulta o comprovante: 200; pedido nao submetido tem o mesmo 404 de pedido inexistente. */
    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        responderJson(resposta, HttpServletResponse.SC_OK,
                SubmissaoResponse.de(servico.consultar(AutenticacaoFiltro.sessaoDe(requisicao), id)));
    }

    /**
     * Interpreta {@code /{id}/submissoes-simuladas}. Qualquer outra forma (id invalido, segmento a mais) e o 404
     * padrao, sem consultar o banco, igual a um pedido inexistente.
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
