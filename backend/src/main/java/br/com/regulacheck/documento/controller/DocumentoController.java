package br.com.regulacheck.documento.controller;

import br.com.regulacheck.armazenamento.conferencia.MotivoRecusa;
import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.DownloadSeguro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.documento.dto.DocumentoEnviadoResponse;
import br.com.regulacheck.documento.dto.EnvioDocumento;
import br.com.regulacheck.documento.dto.ListaDocumentosResponse;
import br.com.regulacheck.documento.service.DocumentoService;
import br.com.regulacheck.documento.service.DocumentoService.ArquivoAberto;
import br.com.regulacheck.documento.service.DocumentoService.DocumentosDoPedido;
import br.com.regulacheck.documento.service.DocumentoService.ResultadoEnvio;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code /solicitacoes/{id}/documentos} e {@code /solicitacoes/{id}/documentos/{docId}}: envio, lista e download
 * dos documentos do pedido da paciente. Rota privada de {@code PACIENTE}; autenticacao e perfil ja foram
 * conferidos por {@code RotasPrivadas}. Este controller so traduz HTTP (caminho, multipart, status) e delega ao
 * {@link DocumentoService}.
 *
 * <p>Nao e registrado sozinho: o caminho {@code /solicitacoes/*} pertence ao {@code SolicitacaoController}, que
 * entrega a este controller os subcaminhos de documentos (a RC-05 nao aceita dois registros para o mesmo caminho).
 *
 * <p>Ordem das verificacoes (ver {@code docs/api/erros.md}): o pedido da dona e conferido antes de ler o corpo;
 * so depois vem o contrato (multipart, campos, arquivo), a conferencia do arquivo e as regras de negocio.
 */
public class DocumentoController extends ServletJson {

    private static final long serialVersionUID = 1L;

    /** Cabecalho com a identificacao da tentativa de envio, que permite repetir um envio sem duplicar. */
    public static final String CABECALHO_CHAVE = "X-Chave-Envio";

    private static final String PARTE_ARQUIVO = "arquivo";
    private static final String PARTE_TIPO = "tipoDocumental";
    private static final int MAXIMO_BYTES_TIPO = 64;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");

    private final transient DocumentoService servico;

    /** Pedido (e, se houver, documento) apontados pelo caminho. */
    private record Alvo(long solicitacaoId, Long documentoId) {
    }

    /** @param servico casos de uso dos documentos */
    public DocumentoController(DocumentoService servico) {
        super("GET", "POST");
        this.servico = servico;
    }

    /**
     * Diz se o caminho (a parte depois de {@code /solicitacoes}) e de documentos, ou seja, se o terceiro
     * segmento e {@code documentos}. A validade dos ids e das demais partes e decidida depois, aqui dentro.
     *
     * @param pathInfo valor de {@code getPathInfo()}; pode ser {@code null}
     * @return {@code true} se este controller deve atender a requisicao
     */
    public static boolean reconhece(String pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        String[] segmentos = pathInfo.split("/", -1);
        // "/12/documentos" -> ["", "12", "documentos"]
        return segmentos.length >= 3 && "documentos".equals(segmentos[2]);
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

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        Alvo alvo = alvoDe(requisicao);
        if (alvo.documentoId() != null) {
            metodoNaoPermitido(resposta, "GET");
            return;
        }
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        // Primeiro a dona (404), so depois o corpo (415/413/400): corpo ruim em pedido alheio continua 404.
        servico.exigirPedido(sessao, alvo.solicitacaoId());
        exigirMultipart(requisicao);
        Collection<Part> partes = lerPartes(requisicao);
        try {
            Part arquivo = exigirUmArquivo(partes);
            String tipoTexto = lerTexto(partes);
            try (InputStream conteudo = arquivo.getInputStream()) {
                EnvioDocumento envio = EnvioDocumento.validar(tipoTexto, requisicao.getHeader(CABECALHO_CHAVE),
                        arquivo.getSubmittedFileName(), conteudo);
                ResultadoEnvio resultado = servico.enviar(sessao, alvo.solicitacaoId(), envio);
                DocumentoEnviadoResponse corpo = DocumentoEnviadoResponse.de(resultado.documento(),
                        resultado.versaoConjuntoDocumental(), resultado.documentosDisponiveis());
                if (resultado.repetido()) {
                    responderJson(resposta, HttpServletResponse.SC_OK, corpo);
                } else {
                    resposta.setHeader("Location", "/solicitacoes/" + alvo.solicitacaoId() + "/documentos/"
                            + resultado.documento().id());
                    responderJson(resposta, HttpServletResponse.SC_CREATED, corpo);
                }
            }
        } finally {
            apagar(partes);
        }
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        Alvo alvo = alvoDe(requisicao);
        SessaoAutenticada sessao = AutenticacaoFiltro.sessaoDe(requisicao);
        if (alvo.documentoId() == null) {
            DocumentosDoPedido lista = servico.listar(sessao, alvo.solicitacaoId());
            responderJson(resposta, HttpServletResponse.SC_OK,
                    ListaDocumentosResponse.de(lista.documentos(), lista.pedido().revisaoAtual()));
            return;
        }
        ArquivoAberto aberto = servico.abrir(sessao, alvo.solicitacaoId(), alvo.documentoId());
        DownloadSeguro.enviar(resposta, aberto.documento().formato(), aberto.documento().nomeArquivo(),
                aberto.documento().tamanhoBytes(), aberto.conteudo());
    }

    /**
     * Interpreta {@code /{id}/documentos[/{docId}]}. Qualquer outra forma (id invalido, segmento a mais, id de
     * documento invalido) e o 404 padrao, sem consultar o banco, igual a um pedido inexistente.
     */
    private static Alvo alvoDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getPathInfo();
        if (caminho == null) {
            throw Erros.naoEncontrado();
        }
        String[] segmentos = caminho.split("/", -1);
        boolean colecao = segmentos.length == 3 || (segmentos.length == 4 && segmentos[3].isEmpty());
        boolean item = segmentos.length == 4 && !segmentos[3].isEmpty();
        if (!(colecao || item) || !ID.matcher(segmentos[1]).matches()
                || (item && !ID.matcher(segmentos[3]).matches())) {
            throw Erros.naoEncontrado();
        }
        return new Alvo(Long.parseLong(segmentos[1]), item ? Long.valueOf(segmentos[3]) : null);
    }

    private static void exigirMultipart(HttpServletRequest requisicao) {
        String tipo = requisicao.getContentType();
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            throw new ExcecaoNegocio(415, "TIPO_NAO_SUPORTADO", "Envie o arquivo como multipart/form-data.");
        }
    }

    /**
     * Le as partes do corpo. O Jetty entrega estouro de limite, multipart malformado e corpo interrompido como
     * {@code ServletException}; o estouro e reconhecido pela causa-raiz (ver {@code research.md} D2).
     */
    private static Collection<Part> lerPartes(HttpServletRequest requisicao) {
        try {
            return requisicao.getParts();
        } catch (ServletException | IOException falha) {
            throw traduzirFalhaDeMultipart(falha);
        }
    }

    private static ExcecaoNegocio traduzirFalhaDeMultipart(Exception falha) {
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            if (causa instanceof IllegalStateException && causa.getMessage() != null
                    && causa.getMessage().startsWith("max ")) {
                return MotivoRecusa.TAMANHO_EXCEDIDO.excecao();
            }
        }
        return new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "O corpo da requisição não pôde ser lido.",
                List.of(new ErroPadrao.Detalhe("arquivo", "Reenvie o arquivo como multipart/form-data.")));
    }

    private static Part exigirUmArquivo(Collection<Part> partes) {
        Part arquivo = null;
        int arquivos = 0;
        for (Part parte : partes) {
            if (parte.getSubmittedFileName() != null) {
                arquivos++;
                if (PARTE_ARQUIVO.equals(parte.getName())) {
                    arquivo = parte;
                }
            }
        }
        if (arquivos > 1) {
            throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "O arquivo enviado não foi aceito.",
                    List.of(new ErroPadrao.Detalhe("arquivos", "Envie um arquivo por vez.")));
        }
        if (arquivo == null) {
            throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "O arquivo enviado não foi aceito.",
                    List.of(new ErroPadrao.Detalhe("arquivo", "Envie o arquivo no campo \"arquivo\".")));
        }
        return arquivo;
    }

    /** Le o campo de texto {@code tipoDocumental}; devolve {@code null} se faltar. Valor enorme e cortado. */
    private static String lerTexto(Collection<Part> partes) throws IOException {
        for (Part parte : partes) {
            if (parte.getSubmittedFileName() == null && PARTE_TIPO.equals(parte.getName())) {
                try (InputStream entrada = parte.getInputStream()) {
                    byte[] bytes = entrada.readNBytes(MAXIMO_BYTES_TIPO + 1);
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    /** Apaga as partes temporarias mesmo em erro (o conteiner tambem as apaga; isto e a garantia local). */
    private static void apagar(Collection<Part> partes) {
        for (Part parte : partes) {
            try {
                parte.delete();
            } catch (IOException | RuntimeException ignorada) {
                // Nao ha o que fazer; o conteiner repete a limpeza ao fim da requisicao.
            }
        }
    }

    private static void metodoNaoPermitido(HttpServletResponse resposta, String permitidos) throws IOException {
        resposta.setHeader("Allow", permitidos);
        RespostaErro.escrever(resposta, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "Metodo HTTP nao permitido para este recurso.");
    }
}
