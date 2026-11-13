package br.com.regulacheck.solicitacao.controller;

import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.CorpoJson;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.documento.controller.DocumentoController;
import br.com.regulacheck.extracao.controller.ExtracaoController;
import br.com.regulacheck.solicitacao.dto.ListaSolicitacoesResponse;
import br.com.regulacheck.solicitacao.dto.SolicitacaoRequest;
import br.com.regulacheck.solicitacao.dto.SolicitacaoResponse;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes} e {@code /solicitacoes/{id}}: rascunho da solicitacao da paciente (criar, reabrir, listar,
 * trocar o servico e excluir com confirmacao). Rota privada de
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

    /** Cabecalho que confirma, de forma deliberada, a exclusao irreversivel de um rascunho. */
    public static final String CABECALHO_CONFIRMACAO = "X-Confirmar-Exclusao";

    /** Valor exato exigido no cabecalho de confirmacao. */
    public static final String VALOR_CONFIRMACAO = "EXCLUIR";

    private final transient SolicitacaoService servico;
    private final transient DocumentoController documentos;
    private final transient ExtracaoController extracao;

    /**
     * @param servico casos de uso da solicitacao
     * @param documentos controller dos documentos do pedido (RC-08), ao qual este entrega os subcaminhos
     *     {@code /{id}/documentos[/{docId}]}
     * @param extracao controller da leitura (simulada) dos documentos (RC-10), ao qual este entrega o subcaminho
     *     {@code /{id}/extracoes}
     */
    public SolicitacaoController(SolicitacaoService servico, DocumentoController documentos,
            ExtracaoController extracao) {
        super("GET", "POST", "PUT", "DELETE");
        this.servico = servico;
        this.documentos = documentos;
        this.extracao = extracao;
    }

    /**
     * Entrega ao {@link DocumentoController} os subcaminhos de documentos e ao {@link ExtracaoController} o de
     * extracao; o resto segue o fluxo normal. A RC-05
     * so aceita um registro por caminho, e {@code /solicitacoes/*} e este.
     */
    @Override
    protected void service(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        if (DocumentoController.reconhece(requisicao.getPathInfo())) {
            documentos.atender(requisicao, resposta);
            return;
        }
        if (ExtracaoController.reconhece(requisicao.getPathInfo())) {
            extracao.atender(requisicao, resposta);
            return;
        }
        super.service(requisicao, resposta);
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        if (idDe(requisicao) != SEM_ID) {
            metodoNaoPermitido(resposta, "GET, PUT, DELETE");
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

    /**
     * Exclui o rascunho (RC-09). Ordem: dona (404) -> confirmacao (400) -> estado (409). A confirmacao e o
     * cabecalho {@link #CABECALHO_CONFIRMACAO} com o valor exato {@link #VALOR_CONFIRMACAO}; sem ela nada e apagado.
     */
    @Override
    protected void doDelete(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        long id = idDe(requisicao);
        if (id == SEM_ID) {
            metodoNaoPermitido(resposta, "GET, POST");
            return;
        }
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        // Primeiro a dona (404): confirmacao ausente em pedido alheio continua 404 e nao revela que ele existe.
        servico.buscarPropria(sessao, id);
        if (!VALOR_CONFIRMACAO.equals(requisicao.getHeader(CABECALHO_CONFIRMACAO))) {
            throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "A exclusão precisa ser confirmada.",
                    List.of(new ErroPadrao.Detalhe("confirmacao", "Envie o cabeçalho " + CABECALHO_CONFIRMACAO
                            + " com o valor " + VALOR_CONFIRMACAO + " para confirmar a exclusão.")));
        }
        servico.excluirRascunho(sessao, id);
        resposta.setStatus(HttpServletResponse.SC_NO_CONTENT);
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
