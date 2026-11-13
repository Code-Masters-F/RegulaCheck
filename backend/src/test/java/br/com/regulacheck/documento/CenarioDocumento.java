package br.com.regulacheck.documento;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Dependencias;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Cenario dos testes de endpoint dos documentos: API real (Jetty, porta livre) com repositorios em memoria e uma
 * area privada de arquivos numa pasta temporaria propria. Cria uma pasta nova por cenario e a apaga ao fechar.
 */
public final class CenarioDocumento implements AutoCloseable {

    /** Corpo para criar um rascunho (RC-07) com o servico demonstrativo. */
    public static final String CORPO_RASCUNHO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";

    public final CenarioAuth cenario;
    public final Path raiz;
    public final ServidorDeTeste api;
    public final Map<String, String> authA;
    public final Map<String, String> authB;

    private CenarioDocumento(CenarioAuth cenario, Path raiz, ServidorDeTeste api) throws Exception {
        this.cenario = cenario;
        this.raiz = raiz;
        this.api = api;
        this.authA = bearer(api.entrar(EMAIL_A, SENHA));
        this.authB = bearer(api.entrar(EMAIL_B, SENHA));
    }

    /** Inicia a API com area de arquivos configurada numa pasta temporaria nova. */
    public static CenarioDocumento iniciar() throws Exception {
        return iniciar(new CenarioAuth(), true);
    }

    /**
     * Inicia a API com as dependencias do cenario dado.
     *
     * @param cenario cenario de autenticacao (permite trocar repositorios por dobros que falham)
     * @param comArmazenamento se {@code false}, nao configura {@code DOCUMENT_STORAGE_PATH}
     */
    public static CenarioDocumento iniciar(CenarioAuth cenario, boolean comArmazenamento) throws Exception {
        return iniciar(cenario, cenario.dependencias(), comArmazenamento);
    }

    /** Inicia a API com dependencias especificas (por exemplo, repositorio de documentos que falha). */
    public static CenarioDocumento iniciar(CenarioAuth cenario, Dependencias dependencias, boolean comArmazenamento)
            throws Exception {
        Path raiz = Files.createTempDirectory("regulacheck-docs-");
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, null,
                comArmazenamento ? raiz.toString() : null, null);
        return new CenarioDocumento(cenario, raiz, ServidorDeTeste.iniciar(config, dependencias));
    }

    /** Cria um rascunho da paciente dona do token e devolve o id. */
    public String criarRascunho(Map<String, String> auth) throws Exception {
        return api.postJson("/solicitacoes", CORPO_RASCUNHO, auth).json().get("id").asText();
    }

    /** Envia um arquivo ao pedido com tipo documental e, opcionalmente, identificacao de tentativa. */
    public Resposta enviar(Map<String, String> auth, String pedidoId, String tipoDocumental, String nomeArquivo,
            byte[] conteudo, String chaveEnvio) throws Exception {
        MultipartTeste corpo = new MultipartTeste();
        if (tipoDocumental != null) {
            corpo.texto("tipoDocumental", tipoDocumental);
        }
        corpo.arquivo("arquivo", nomeArquivo, "application/octet-stream", conteudo);
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (chaveEnvio != null) {
            cabecalhos.put("X-Chave-Envio", chaveEnvio);
        }
        return api.postBinario("/solicitacoes/" + pedidoId + "/documentos", corpo.corpo(), corpo.contentType(),
                cabecalhos);
    }

    /** Substitui um documento por outro arquivo (PUT multipart), com chave de tentativa opcional. */
    public Resposta substituir(Map<String, String> auth, String pedidoId, String documentoId, String tipoDocumental,
            String nomeArquivo, byte[] conteudo, String chaveEnvio) throws Exception {
        MultipartTeste corpo = new MultipartTeste();
        if (tipoDocumental != null) {
            corpo.texto("tipoDocumental", tipoDocumental);
        }
        corpo.arquivo("arquivo", nomeArquivo, "application/octet-stream", conteudo);
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (chaveEnvio != null) {
            cabecalhos.put("X-Chave-Envio", chaveEnvio);
        }
        return api.enviarBinario("PUT", "/solicitacoes/" + pedidoId + "/documentos/" + documentoId, corpo.corpo(),
                corpo.contentType(), cabecalhos);
    }

    /** Pede a leitura (simulada) dos documentos do pedido (POST /extracoes, RC-10). */
    public Resposta lerDocumentos(Map<String, String> auth, String pedidoId) throws Exception {
        return api.enviar("POST", "/solicitacoes/" + pedidoId + "/extracoes", null, auth);
    }

    /** Consulta a leitura mais recente do pedido (GET /extracoes, RC-10). */
    public Resposta consultarLeitura(Map<String, String> auth, String pedidoId) throws Exception {
        return api.get("/solicitacoes/" + pedidoId + "/extracoes", auth);
    }

    /** Consulta a conferencia da transcricao de uma leitura (GET /extracoes/{id}/conferencia, RC-11). */
    public Resposta consultarConferencia(Map<String, String> auth, String pedidoId, String extracaoId)
            throws Exception {
        return api.get("/solicitacoes/" + pedidoId + "/extracoes/" + extracaoId + "/conferencia", auth);
    }

    /** Registra decisoes de conferencia (PUT /extracoes/{id}/conferencia) com corpo JSON. */
    public Resposta decidir(Map<String, String> auth, String pedidoId, String extracaoId, String json)
            throws Exception {
        return api.enviar("PUT", "/solicitacoes/" + pedidoId + "/extracoes/" + extracaoId + "/conferencia", json,
                auth);
    }

    /** Remove um documento (DELETE). */
    public Resposta remover(Map<String, String> auth, String pedidoId, String documentoId) throws Exception {
        return api.enviar("DELETE", "/solicitacoes/" + pedidoId + "/documentos/" + documentoId, null, auth);
    }

    /** Exclui o rascunho (DELETE); {@code confirmacao} {@code null} nao envia o cabecalho de confirmacao. */
    public Resposta excluir(Map<String, String> auth, String pedidoId, String confirmacao) throws Exception {
        Map<String, String> cabecalhos = new HashMap<>(auth);
        if (confirmacao != null) {
            cabecalhos.put("X-Confirmar-Exclusao", confirmacao);
        }
        return api.enviar("DELETE", "/solicitacoes/" + pedidoId, null, cabecalhos);
    }

    /**
     * Confere a integridade entre registro e arquivos (SC-004): todo documento disponivel com arquivo tem o arquivo
     * na area privada, e todo arquivo na area privada pertence a um documento disponivel (nenhum orfao: nem de
     * removido, nem de substituido, nem de pedido excluido). Linhas da carga sintetica nao tem arquivo.
     */
    public void conferirIntegridade() throws IOException {
        java.util.Set<String> esperados = new java.util.TreeSet<>();
        for (br.com.regulacheck.documento.entity.Documento d : cenario.documentos.todos()) {
            if (d.caminhoInterno() != null) {
                esperados.add(d.caminhoInterno().valor());
            }
        }
        java.util.Set<String> existentes = new java.util.TreeSet<>();
        try (Stream<Path> itens = Files.list(raiz)) {
            itens.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).forEach(existentes::add);
        }
        org.junit.jupiter.api.Assertions.assertEquals(esperados, existentes,
                "arquivos na area privada devem ser exatamente os dos documentos disponiveis");
    }

    /** @return quantos arquivos comuns existem na raiz da area privada (arquivos guardados e temporarios de gravacao) */
    public long arquivosGuardados() throws IOException {
        try (Stream<Path> itens = Files.list(raiz)) {
            return itens.filter(Files::isRegularFile).count();
        }
    }

    /**
     * @return quantos arquivos restam na quarentena (devem ser zero depois de cada requisicao). O conteiner apaga
     *     as partes temporarias depois de escrever a resposta, entao espera ate 2 s pelo esvaziamento antes de
     *     contar.
     */
    public long arquivosNaQuarentena() throws Exception {
        Path quarentena = raiz.resolve(".quarentena");
        long restantes = 0;
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            if (!Files.isDirectory(quarentena)) {
                return 0;
            }
            try (Stream<Path> itens = Files.list(quarentena)) {
                restantes = itens.count();
            }
            if (restantes == 0) {
                return 0;
            }
            Thread.sleep(100);
        }
        return restantes;
    }

    @Override
    public void close() throws Exception {
        api.close();
        try (Stream<Path> arvore = Files.walk(raiz)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(caminho -> caminho.toFile().delete());
        }
    }
}
