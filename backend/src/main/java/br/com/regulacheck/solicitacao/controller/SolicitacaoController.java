package br.com.regulacheck.solicitacao.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.documento.controller.DocumentoController;
import br.com.regulacheck.solicitacao.dto.ListaSolicitacoesResponse;
import br.com.regulacheck.solicitacao.dto.SolicitacaoRequest;
import br.com.regulacheck.solicitacao.dto.SolicitacaoResponse;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes} e {@code /solicitacoes/{id}}: rascunho da solicitacao da paciente. Rota privada de
 * {@code PACIENTE}; autenticacao e perfil ja foram conferidos por {@code RotasPrivadas}. Este controller so
 * traduz HTTP (roteamento, corpo, status) e delega ao {@link SolicitacaoService}.
 *
 * <p>Qualquer caminho que nao seja a colecao ou {@code /{id}} com id numerico valido responde o 404 padrao,
 * sem consultar o banco, igual a um pedido inexistente.
 */
public class SolicitacaoController extends ServletJson {

    private static final long serialVersionUID = 1L;

    /** Numero decimal positivo sem zeros a esquerda e dentro do alcance de {@code long}. */
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient SolicitacaoService servico;
    private final transient DocumentoController documentos;

    /**
     * @param servico casos de uso da solicitacao
     * @param documentos controller dos documentos do pedido (RC-08), ao qual este entrega os subcaminhos
     *     {@code /{id}/documentos[/{docId}]}
     */
    public SolicitacaoController(SolicitacaoService servico, DocumentoController documentos) {
        super("GET", "POST", "PUT");
        this.servico = servico;
        this.documentos = documentos;
    }

    /**
     * Entrega ao {@link DocumentoController} os subcaminhos de documentos; o resto segue o fluxo normal. A RC-05
     * so aceita um registro por caminho, e {@code /solicitacoes/*} e este.
     */
    @Override
    protected void service(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        if (DocumentoController.reconhece(requisicao.getPathInfo())) {
            documentos.atender(requisicao, resposta);
            return;
        }
        super.service(requisicao, resposta);
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        if (idDe(requisicao) != SEM_ID) {
            metodoNaoPermitido(resposta, "GET, PUT");
            return;
        }
        SolicitacaoRequest corpo = CorpoJson.ler(requisicao.getInputStream(), requisicao.getContentType(),
                SolicitacaoRequest.class);
        String protocoloId = corpo.validar();
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);

        Solicitacao criada = servico.criar(sessao, protocoloId);

        resposta.setHeader("Location", "/solicitacoes/" + criada.id());
        responderJson(resposta, HttpServletResponse.SC_CREATED, SolicitacaoResponse.de(criada));
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        if (id == SEM_ID) {
            responderJson(resposta, HttpServletResponse.SC_OK,
                    ListaSolicitacoesResponse.de(servico.listar(sessao), SolicitacaoService.LIMITE_LISTA));
            return;
        }
        responderJson(resposta, HttpServletResponse.SC_OK, SolicitacaoResponse.de(servico.buscarPropria(sessao, id)));
    }

    @Override
    protected void doPut(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        if (id == SEM_ID) {
            metodoNaoPermitido(resposta, "GET, POST");
            return;
        }
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        // Ordem de docs/api/erros.md: primeiro a dona (404), so depois o corpo (400/415). Assim um corpo ruim
        // enviado para um pedido alheio nao revela que o pedido existe.
        servico.buscarPropria(sessao, id);
        SolicitacaoRequest corpo = CorpoJson.ler(requisicao.getInputStream(), requisicao.getContentType(),
                SolicitacaoRequest.class);
        String protocoloId = corpo.validar();

        Solicitacao atualizada = servico.trocarProtocolo(sessao, id, protocoloId);

        responderJson(resposta, HttpServletResponse.SC_OK, SolicitacaoResponse.de(atualizada));
    }

    /** Marca "o caminho e a colecao, sem id". Zero nunca e um id valido. */
    private static final long SEM_ID = 0;

    /**
     * @return o id do caminho, {@link #SEM_ID} se o caminho e a colecao
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 404 padrao se o caminho tem id invalido ou
     *     subcaminho desconhecido
     */
    private static long idDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        if (caminho == null || caminho.equals("/")) {
            return SEM_ID;
        }
        String texto = caminho.startsWith("/") ? caminho.substring(1) : caminho;
        if (!ID.matcher(texto).matches()) {
            throw Erros.naoEncontrado();
        }
        return Long.parseLong(texto);
    }

    private static void metodoNaoPermitido(HttpServletResponse resposta, String permitidos) throws IOException {
        resposta.setHeader("Allow", permitidos);
        RespostaErro.escrever(resposta, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "Metodo HTTP nao permitido para este recurso.");
    }
}
