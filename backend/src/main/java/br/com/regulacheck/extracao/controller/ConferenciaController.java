package br.com.regulacheck.extracao.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.extracao.dto.ConferenciaRequest;
import br.com.regulacheck.extracao.dto.ConferenciaResponse;
import br.com.regulacheck.extracao.repository.DecisaoNova;
import br.com.regulacheck.extracao.service.ConferenciaService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes/{id}/extracoes/{extracaoId}/conferencia}: consultar a conferencia da transcricao
 * ({@code GET}) e registrar as decisoes da paciente ({@code PUT}). Rota privada de {@code PACIENTE}; autenticacao e perfil ja foram conferidos por
 * {@code RotasPrivadas}. A leitura e <strong>simulada</strong> e a conferencia nunca libera submissao. Este
 * controller so traduz HTTP e delega ao {@link ConferenciaService}.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que
 * entrega a este controller o subcaminho da conferencia (antes de entregar ao {@code ExtracaoController}).
 */
public class ConferenciaController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient ConferenciaService servico;

    /** @param servico casos de uso da conferencia */
    public ConferenciaController(ConferenciaService servico) {
        super("GET", "PUT");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e de conferencia, ou seja, se tem a forma
     * {@code /{id}/extracoes/{extracaoId}/conferencia}. A validade dos ids e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        return segmentos.length == 5 && "extracoes".equals(segmentos[2]) && "conferencia".equals(segmentos[4]);
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

    /** Consulta a conferencia da leitura: atual ou desatualizada (somente leitura). */
    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long[] ids = idsDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        responderJson(resposta, HttpServletResponse.SC_OK,
                ConferenciaResponse.de(servico.consultar(sessao, ids[0], ids[1])));
    }

    /**
     * Registra as decisoes da paciente (confirmar ou apontar divergencia). Ordem: dona do pedido e da leitura
     * (404) -> tipo e formato do corpo (415/400) -> estado e versao (409) -> aplicabilidade (422). O corpo e
     * fechado: nenhum valor digitado e aceito.
     */
    @Override
    protected void doPut(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long[] ids = idsDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        // Primeiro a dona (404): corpo ruim enviado a pedido ou leitura alheios nao revela que eles existem.
        servico.exigirLeitura(sessao, ids[0], ids[1]);
        JsonNode corpo = CorpoJson.ler(requisicao.getInputStream(), requisicao.getContentType(), JsonNode.class);
        List<DecisaoNova> decisoes = ConferenciaRequest.validar(corpo);
        responderJson(resposta, HttpServletResponse.SC_OK,
                ConferenciaResponse.de(servico.registrar(sessao, ids[0], ids[1], decisoes)));
    }

    /**
     * Interpreta {@code /{id}/extracoes/{extracaoId}/conferencia}. Qualquer outra forma (id invalido ou mal
     * formado) e o 404 padrao, sem consultar o banco, igual a um pedido ou leitura inexistente.
     *
     * @return o id do pedido e o da leitura, nessa ordem
     */
    private static long[] idsDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        if (caminho == null) {
            throw Erros.naoEncontrado();
        }
        String[] segmentos = caminho.split("/", -1);
        if (segmentos.length != 5 || !ID.matcher(segmentos[1]).matches() || !ID.matcher(segmentos[3]).matches()) {
            throw Erros.naoEncontrado();
        }
        return new long[] {Long.parseLong(segmentos[1]), Long.parseLong(segmentos[3])};
    }
}
