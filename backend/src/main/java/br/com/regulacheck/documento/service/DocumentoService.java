package br.com.regulacheck.documento.service;

import br.com.regulacheck.armazenamento.ArmazenamentoPrivado;
import br.com.regulacheck.armazenamento.ArquivoAusenteException;
import br.com.regulacheck.armazenamento.ArquivoGuardado;
import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.armazenamento.conferencia.ResultadoConferencia;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.dto.EnvioDocumento;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.documento.repository.NovoDocumento;
import br.com.regulacheck.documento.repository.ResultadoRegistro;
import br.com.regulacheck.documento.repository.ResultadoRemocao;
import br.com.regulacheck.documento.repository.ResultadoSubstituicao;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso dos documentos de um pedido: enviar, listar e abrir. Aplica a regra de dona da RC-05 (o pedido
 * passa pela busca filtrada pela dona; pedido alheio e inexistente tem a mesma resposta) e reaproveita a
 * conferencia e o armazenamento da RC-06.
 *
 * <p><strong>O servico nao le nem interpreta o conteudo do arquivo.</strong> Receber um documento nao inicia
 * extracao nem analise e nao muda o estado do pedido. Este servico nao guarda dados de requisicao em campos.
 *
 * <p>Ordem do envio (ver {@code research.md} D6): conferir -> repeticao ou recusa barata -> guardar o arquivo ->
 * transacao do banco. O arquivo e guardado antes da transacao para nao prender conexao nem bloqueio durante a
 * escrita em disco; se a transacao falhar ou recusar, o arquivo e removido.
 */
public class DocumentoService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentoService.class);

    private final SolicitacaoService solicitacoes;
    private final DocumentoRepository documentos;
    private final ArmazenamentoPrivado armazenamento;
    private final ConferenciaDeArquivo conferencia;
    private final Clock relogio;

    /**
     * Resultado de um envio.
     *
     * @param documento documento registrado, ou o ja existente quando {@code repetido}
     * @param versaoConjuntoDocumental revisao do pedido apos o envio
     * @param documentosDisponiveis documentos disponiveis do pedido apos o envio
     * @param repetido {@code true} se era uma repeticao da mesma tentativa (nada novo foi gravado)
     */
    public record ResultadoEnvio(Documento documento, int versaoConjuntoDocumental, int documentosDisponiveis,
            boolean repetido) {
    }

    /**
     * Resultado de uma remocao.
     *
     * @param versaoConjuntoDocumental revisao do pedido apos a remocao
     * @param documentosDisponiveis documentos disponiveis do pedido apos a remocao
     */
    public record ResultadoRemover(int versaoConjuntoDocumental, int documentosDisponiveis) {
    }

    /**
     * Documentos de um pedido.
     *
     * @param pedido pedido da paciente
     * @param documentos documentos disponiveis, na ordem de envio
     */
    public record DocumentosDoPedido(Solicitacao pedido, List<Documento> documentos) {
    }

    /**
     * Arquivo aberto para download.
     *
     * @param documento registro do documento (formato e nome de exibicao)
     * @param conteudo fluxo do arquivo; quem recebe deve fechar
     */
    public record ArquivoAberto(Documento documento, InputStream conteudo) {
    }

    /**
     * @param solicitacoes casos de uso do pedido (busca filtrada pela dona)
     * @param documentos persistencia dos documentos
     * @param armazenamento area privada de arquivos
     * @param conferencia conferencia de tipo, tamanho e paginas
     * @param relogio relogio da aplicacao (instantes gravados e devolvidos)
     */
    public DocumentoService(SolicitacaoService solicitacoes, DocumentoRepository documentos,
            ArmazenamentoPrivado armazenamento, ConferenciaDeArquivo conferencia, Clock relogio) {
        this.solicitacoes = solicitacoes;
        this.documentos = documentos;
        this.armazenamento = armazenamento;
        this.conferencia = conferencia;
        this.relogio = relogio;
    }

    /**
     * Confere que o pedido existe e e da paciente da sessao. O controller chama antes de ler o corpo, para que um
     * corpo ruim enviado a um pedido alheio continue sendo 404.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return o pedido
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se nao existe ou e de outra paciente; 503 se o banco
     *     estiver indisponivel
     */
    public Solicitacao exigirPedido(SessaoAutenticada sessao, long solicitacaoId) {
        return solicitacoes.buscarPropria(sessao, solicitacaoId);
    }

    /**
     * Envia um arquivo ao rascunho da paciente da sessao.
     *
     * <p>O arquivo so vira documento depois de passar na conferencia e de a transacao do banco confirmar o
     * registro (documento, revisao nova e pedido atualizado juntos). Com a mesma identificacao de tentativa, o
     * mesmo conteudo e o mesmo tipo documental, repetir o envio devolve o documento existente sem gravar nada.
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param solicitacaoId pedido que recebe o documento
     * @param envio entrada ja validada
     * @return o desfecho do envio
     * @throws ExcecaoNegocio 404 se o pedido nao e da paciente; 400/413/415 se o arquivo for recusado pela
     *     conferencia; 409 {@code CONFLITO_ESTADO} se o pedido saiu de rascunho; 409
     *     {@code CHAVE_ENVIO_REUTILIZADA} se a chave foi usada com outro conteudo ou tipo; 422
     *     {@code REGRA_DE_NEGOCIO} se o pedido ja tem o maximo de documentos; 503 se o banco ou a area de arquivos
     *     estiverem indisponiveis
     */
    public ResultadoEnvio enviar(SessaoAutenticada sessao, long solicitacaoId, EnvioDocumento envio) {
        Solicitacao pedido = exigirPedido(sessao, solicitacaoId);
        ArquivoConferido arquivo = conferir(envio);
        try {
            // Recusas baratas, com o pedido ja carregado, antes de gravar qualquer arquivo. A decisao final
            // continua sendo a da transacao (outro envio pode ter mudado o pedido nesse meio tempo).
            if (pedido.estado() != EstadoSolicitacao.RASCUNHO) {
                throw conflitoDeEstado();
            }
            String hash = sha256(arquivo);
            if (envio.chaveEnvio() != null) {
                Optional<Documento> existente = documentos.buscarPorChave(solicitacaoId, sessao.usuarioId(),
                        envio.chaveEnvio());
                if (existente.isPresent()) {
                    return repeticao(existente.get(), hash, envio, pedido);
                }
            }
            if (pedido.documentosDisponiveis() >= LimitesArquivo.MAXIMO_ARQUIVOS) {
                throw limiteAtingido();
            }
            return gravar(sessao, solicitacaoId, envio, arquivo, hash);
        } finally {
            // Idempotente: guardar() ja descarta a quarentena; este descarte cobre as saidas antes dele.
            arquivo.descartar();
        }
    }

    /**
     * Confere, <strong>antes de o controller ler o corpo</strong> do {@code PUT}, que o documento do caminho existe
     * e esta disponivel no pedido da paciente: respeita a ordem 404 -> 415/413/400 e evita ler e guardar um arquivo
     * de ate 5 MB para depois responder 404. Excecao (D3): se a identificacao de tentativa ja corresponde a um
     * documento vivo do pedido, a repeticao da substituicao e reconhecida <em>sem</em> procurar o documento do
     * caminho (que, depois do sucesso, ja nao existe), e a decisao entre repeticao e conflito fica para depois da
     * leitura do corpo. A transacao continua sendo a decisao final (outra requisicao pode remover o documento
     * depois desta conferencia).
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido do caminho (ja conferido como da paciente)
     * @param documentoId documento do caminho
     * @param chaveEnvio valor bruto do cabecalho {@code X-Chave-Envio}, ou {@code null}; so serve de consulta
     * @throws ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se o documento nao existe, ja foi removido, e de outro
     *     pedido ou de outra paciente (resposta identica); 503 se o banco estiver indisponivel
     */
    public void exigirDocumentoParaSubstituir(SessaoAutenticada sessao, long solicitacaoId, long documentoId,
            String chaveEnvio) {
        if (chaveEnvio != null && documentos.buscarPorChave(solicitacaoId, sessao.usuarioId(), chaveEnvio)
                .isPresent()) {
            return;
        }
        Propriedade.exigir(documentos.buscarDoPedido(documentoId, solicitacaoId, sessao.usuarioId()), sessao);
    }

    /**
     * Remove um documento do pedido da paciente da sessao. O documento deixa de estar disponivel (lista, download
     * e contagem), a vaga e liberada e a versao do conjunto documental sobe em um, com a revisao registrada. A
     * conferencia anterior deixa de ser vigente, mas nenhuma analise e iniciada e o estado do pedido nao muda.
     *
     * <p>Politica de arquivos (RC-09): o banco muda primeiro, numa transacao so; o arquivo e apagado
     * <strong>depois</strong> do commit. Se apagar falhar, a remocao continua valendo e o arquivo fica sem
     * registro, nunca visivel ao usuario; o armazenamento registra so a classe da falha, sem nome interno.
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param solicitacaoId pedido do caminho
     * @param documentoId documento a remover
     * @return a versao do conjunto documental e a contagem de documentos apos a remocao
     * @throws ExcecaoNegocio 404 se o pedido ou o documento nao existe, e de outra paciente, e de outro pedido ou
     *     ja foi removido (resposta identica); 409 {@code CONFLITO_ESTADO} se o pedido foi submetido em simulacao;
     *     503 se o banco estiver indisponivel (nada muda e nenhum arquivo e apagado)
     */
    public ResultadoRemover remover(SessaoAutenticada sessao, long solicitacaoId, long documentoId) {
        exigirPedido(sessao, solicitacaoId);
        ResultadoRemocao resultado = documentos.remover(sessao.usuarioId(), solicitacaoId, documentoId, agora());
        return switch (resultado.situacao()) {
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
            case SUBMETIDO -> throw pedidoSubmetido();
            case REMOVIDO -> {
                apagarArquivo(resultado.documento());
                yield new ResultadoRemover(resultado.versaoConjuntoDocumental(), resultado.documentosDisponiveis());
            }
        };
    }

    /**
     * Substitui um documento do pedido da paciente da sessao por outro arquivo, numa unica operacao: o antigo sai e
     * o novo entra na mesma transacao, com <em>uma</em> revisao nova, e a contagem de documentos nao muda (nao ha
     * limite a conferir). O arquivo novo passa pela mesma conferencia do envio e e guardado antes da transacao; se
     * for recusado ou a transacao falhar, o novo e removido e o antigo continua intacto. O arquivo antigo e apagado
     * so depois do commit.
     *
     * <p>Com a mesma identificacao de tentativa, o mesmo conteudo e o mesmo tipo documental, repetir a substituicao
     * devolve o documento ja criado, sem gravar nada, e isso e reconhecido <em>antes</em> de procurar o documento
     * do caminho (que, depois do sucesso, ja nao existe).
     *
     * @param sessao paciente autenticada (dona do pedido)
     * @param solicitacaoId pedido do caminho
     * @param documentoId documento a substituir
     * @param envio arquivo novo, ja validado quanto ao contrato
     * @return o desfecho; {@code repetido} indica que nada novo foi gravado
     * @throws ExcecaoNegocio 404 se o pedido ou o documento nao existe, e de outra paciente, e de outro pedido ou
     *     ja foi removido; 400/413/415 se o arquivo for recusado pela conferencia; 409 {@code CONFLITO_ESTADO} se o
     *     pedido foi submetido em simulacao; 409 {@code CHAVE_ENVIO_REUTILIZADA}; 503 se o banco ou a area de
     *     arquivos estiverem indisponiveis
     */
    public ResultadoEnvio substituir(SessaoAutenticada sessao, long solicitacaoId, long documentoId,
            EnvioDocumento envio) {
        Solicitacao pedido = exigirPedido(sessao, solicitacaoId);
        ArquivoConferido arquivo = conferir(envio);
        try {
            if (pedido.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
                throw pedidoSubmetido();
            }
            String hash = sha256(arquivo);
            if (envio.chaveEnvio() != null) {
                Optional<Documento> existente = documentos.buscarPorChave(solicitacaoId, sessao.usuarioId(),
                        envio.chaveEnvio());
                if (existente.isPresent()) {
                    return repeticao(existente.get(), hash, envio, pedido);
                }
            }
            return gravarSubstituicao(sessao, solicitacaoId, documentoId, envio, arquivo, hash);
        } finally {
            arquivo.descartar();
        }
    }

    /**
     * Lista os documentos disponiveis do pedido da paciente da sessao, na ordem de envio.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return o pedido e seus documentos; lista vazia, e nao erro, quando nao ha documentos
     * @throws ExcecaoNegocio 404 se o pedido nao existe ou e de outra paciente; 503 se o banco estiver indisponivel
     */
    public DocumentosDoPedido listar(SessaoAutenticada sessao, long solicitacaoId) {
        Solicitacao pedido = exigirPedido(sessao, solicitacaoId);
        return new DocumentosDoPedido(pedido, documentos.listarDoPedido(solicitacaoId, sessao.usuarioId()));
    }

    /**
     * Abre o arquivo de um documento do pedido da paciente da sessao.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido ao qual o documento deve pertencer
     * @param documentoId documento
     * @return o documento e o fluxo do arquivo (quem recebe fecha)
     * @throws ExcecaoNegocio 404 se o pedido ou o documento nao existe, e de outra paciente ou e de outro pedido
     *     (resposta identica); 503 se o banco ou a area de arquivos estiverem indisponiveis, ou se o arquivo nao
     *     existir na area privada
     */
    public ArquivoAberto abrir(SessaoAutenticada sessao, long solicitacaoId, long documentoId) {
        exigirPedido(sessao, solicitacaoId);
        Documento documento = Propriedade.exigir(
                documentos.buscarDoPedido(documentoId, solicitacaoId, sessao.usuarioId()), sessao);
        if (documento.caminhoInterno() == null) {
            // Linha da carga sintetica: nunca teve arquivo real.
            throw arquivoIndisponivel();
        }
        try {
            return new ArquivoAberto(documento, armazenamento.abrir(documento.caminhoInterno()));
        } catch (ArquivoAusenteException ausente) {
            LOG.warn("Arquivo de documento ausente na area privada. documentoId={}", documentoId);
            throw arquivoIndisponivel();
        }
    }

    private ArquivoConferido conferir(EnvioDocumento envio) {
        ResultadoConferencia resultado = conferencia.conferir(envio.conteudo(), envio.nomeOriginal());
        return switch (resultado) {
            case ResultadoConferencia.Aceito aceito -> aceito.arquivo();
            case ResultadoConferencia.Recusado recusado -> throw recusado.motivo().excecao();
        };
    }

    private ResultadoEnvio repeticao(Documento existente, String hash, EnvioDocumento envio, Solicitacao pedido) {
        if (!hash.equals(existente.hashConteudo()) || envio.tipoDocumental() != existente.tipoDocumental()) {
            throw chaveReutilizada();
        }
        LOG.info("Envio repetido reconhecido. solicitacaoId={} documentoId={}", pedido.id(), existente.id());
        return new ResultadoEnvio(existente, pedido.revisaoAtual(), pedido.documentosDisponiveis(), true);
    }

    private ResultadoEnvio gravar(SessaoAutenticada sessao, long solicitacaoId, EnvioDocumento envio,
            ArquivoConferido arquivo, String hash) {
        ArquivoGuardado guardado = armazenamento.guardar(arquivo);
        boolean registrado = false;
        try {
            NovoDocumento novo = new NovoDocumento(guardado.nomeExibicao(), envio.tipoDocumental(), guardado.tipo(),
                    guardado.tamanhoBytes(), guardado.paginas(), guardado.id(), hash, envio.chaveEnvio());
            ResultadoRegistro resultado = documentos.registrar(sessao.usuarioId(), solicitacaoId, novo, agora());
            switch (resultado.situacao()) {
                case REGISTRADO -> {
                    registrado = true;
                    return new ResultadoEnvio(resultado.documento(), resultado.versaoConjuntoDocumental(),
                            resultado.documentosDisponiveis(), false);
                }
                case REPETIDO -> {
                    // Outro envio da mesma tentativa venceu a corrida: devolve o documento dele.
                    return new ResultadoEnvio(resultado.documento(), resultado.versaoConjuntoDocumental(),
                            resultado.documentosDisponiveis(), true);
                }
                case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
                case FORA_DE_RASCUNHO, SUBMETIDO -> throw conflitoDeEstado();
                case LIMITE -> throw limiteAtingido();
                case CONFLITO_CHAVE -> throw chaveReutilizada();
                default -> throw new IllegalStateException("Situacao de registro desconhecida.");
            }
        } finally {
            if (!registrado) {
                // Compensacao: nao pode ficar arquivo sem registro.
                armazenamento.remover(guardado.id());
            }
        }
    }

    private ResultadoEnvio gravarSubstituicao(SessaoAutenticada sessao, long solicitacaoId, long documentoId,
            EnvioDocumento envio, ArquivoConferido arquivo, String hash) {
        ArquivoGuardado guardado = armazenamento.guardar(arquivo);
        boolean registrado = false;
        try {
            NovoDocumento novo = new NovoDocumento(guardado.nomeExibicao(), envio.tipoDocumental(), guardado.tipo(),
                    guardado.tamanhoBytes(), guardado.paginas(), guardado.id(), hash, envio.chaveEnvio());
            ResultadoSubstituicao resultado = documentos.substituir(sessao.usuarioId(), solicitacaoId, documentoId,
                    novo, agora());
            ResultadoRegistro registro = resultado.registro();
            switch (registro.situacao()) {
                case REGISTRADO -> {
                    registrado = true;
                    apagarArquivo(resultado.substituido());
                    return new ResultadoEnvio(registro.documento(), registro.versaoConjuntoDocumental(),
                            registro.documentosDisponiveis(), false);
                }
                case REPETIDO -> {
                    // Outra tentativa igual venceu a corrida: devolve o documento dela.
                    return new ResultadoEnvio(registro.documento(), registro.versaoConjuntoDocumental(),
                            registro.documentosDisponiveis(), true);
                }
                case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
                case SUBMETIDO -> throw pedidoSubmetido();
                case CONFLITO_CHAVE -> throw chaveReutilizada();
                default -> throw new IllegalStateException("Situacao de substituicao desconhecida.");
            }
        } finally {
            if (!registrado) {
                // Compensacao: o arquivo novo nao pode ficar sem registro; o antigo nunca foi tocado.
                armazenamento.remover(guardado.id());
            }
        }
    }

    /**
     * Apaga o arquivo de um documento que deixou de estar disponivel, <strong>depois</strong> do commit. Linha da
     * carga sintetica nao tem arquivo ({@code caminhoInterno} nulo): nada a apagar, e isso nunca e falha. Nenhum
     * erro daqui desfaz a mudanca ja registrada.
     */
    private void apagarArquivo(Documento documento) {
        if (documento == null || documento.caminhoInterno() == null) {
            return;
        }
        try {
            armazenamento.remover(documento.caminhoInterno());
        } catch (RuntimeException falha) {
            // So a classe da falha: nunca o nome interno do arquivo (FR-022).
            LOG.warn("Arquivo de documento removido nao pode ser apagado. documentoId={} tipo={}", documento.id(),
                    falha.getClass().getSimpleName());
        }
    }

    private static ExcecaoNegocio pedidoSubmetido() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Este pedido já foi submetido e só pode ser consultado.");
    }

    private static String sha256(ArquivoConferido arquivo) {
        try (InputStream entrada = Files.newInputStream(arquivo.caminhoTemporario())) {
            MessageDigest resumo = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8 * 1024];
            int lidos;
            while ((lidos = entrada.read(buffer)) >= 0) {
                resumo.update(buffer, 0, lidos);
            }
            return HexFormat.of().formatHex(resumo.digest());
        } catch (IOException | NoSuchAlgorithmException falha) {
            // Nao registra caminho nem nome; so a classe da falha.
            LOG.error("Falha ao calcular o resumo do arquivo. tipo={}", falha.getClass().getSimpleName());
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
    }

    private static ExcecaoNegocio conflitoDeEstado() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Este pedido não pode mais receber documentos.");
    }

    private static ExcecaoNegocio limiteAtingido() {
        return new ExcecaoNegocio(422, "REGRA_DE_NEGOCIO", "O pedido já tem o máximo de documentos.",
                List.of(new ErroPadrao.Detalhe("documentos",
                        "Cada pedido aceita no máximo " + LimitesArquivo.MAXIMO_ARQUIVOS + " documentos.")));
    }

    private static ExcecaoNegocio chaveReutilizada() {
        return new ExcecaoNegocio(409, "CHAVE_ENVIO_REUTILIZADA",
                "Esta identificação de envio já foi usada com outro arquivo ou outro tipo de documento.");
    }

    private static ExcecaoNegocio arquivoIndisponivel() {
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "O armazenamento de arquivos está indisponível. Tente novamente mais tarde.");
    }

    /** Instante atual na precisao que o banco guarda (mesma razao de {@code SolicitacaoService}). */
    private Instant agora() {
        return relogio.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
