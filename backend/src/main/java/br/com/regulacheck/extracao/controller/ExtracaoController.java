package br.com.regulacheck.extracao.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.extracao.dto.ExtracaoResponse;
import br.com.regulacheck.extracao.service.ExtracaoService;
import br.com.regulacheck.extracao.service.ExtracaoService.ResultadoLeitura;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes/{id}/extracoes}: ler os documentos do pedido ({@code POST}) e consultar a leitura
 * ({@code GET}). Rota privada de {@code PACIENTE}; autenticacao e perfil ja foram conferidos por
 * {@code RotasPrivadas}. A leitura e <strong>simulada</strong>. Este controller so traduz HTTP e delega ao
 * {@link ExtracaoService}.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que
 * entrega a este controller o subcaminho de extracao. O {@code POST} nao tem corpo: o corpo e ignorado.
 */
public class ExtracaoController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient ExtracaoService servico;

    /** @param servico casos de uso da leitura */
    public ExtracaoController(ExtracaoService servico) {
        super("GET", "POST");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e de extracao, ou seja, se o terceiro segmento e
     * {@code extracoes}. A validade do id e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length >= 3 && "extracoes".equals(segmentos[2]);
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

    /** Le os documentos: 201 com a leitura nova, 200 se ja existia para a versao atual. */
    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        ResultadoLeitura resultado = servico.ler(sessao, id);
        int status = resultado.criada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK;
        responderJson(resposta, status, ExtracaoResponse.de(resultado.extracao()));
    }

    /** Consulta a leitura mais recente: atual ou desatualizada. */
    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        responderJson(resposta, HttpServletResponse.SC_OK, ExtracaoResponse.de(servico.consultar(sessao, id)));
    }

    /**
     * Interpreta {@code /{id}/extracoes}. Qualquer outra forma (id invalido, segmento a mais) e o 404 padrao, sem
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
