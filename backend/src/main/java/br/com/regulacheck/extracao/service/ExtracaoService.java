package br.com.regulacheck.extracao.service;

import br.com.regulacheck.armazenamento.ArmazenamentoPrivado;
import br.com.regulacheck.armazenamento.ArquivoAusenteException;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.extracao.repository.ContextoLeitura;
import br.com.regulacheck.extracao.repository.DocumentoParaLeitura;
import br.com.regulacheck.extracao.repository.ExtracaoRegistrada;
import br.com.regulacheck.extracao.repository.ExtracaoRepository;
import br.com.regulacheck.extracao.repository.LeituraParaGravar;
import br.com.regulacheck.extracao.repository.ResultadoGravacao;
import br.com.regulacheck.integracao.extracao.ArquivoParaLeitura;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.LeituraIndisponivelException;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import br.com.regulacheck.integracao.extracao.ValidadorDeLeitura;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso da leitura (simulada) dos documentos de um pedido: ler e consultar. Aplica a regra de dona da RC-05
 * (pedido de outra paciente e pedido inexistente tem a mesma resposta).
 *
 * <p><strong>A leitura e simulada e nao decide nada:</strong> nao altera o estado do pedido, nao cria revisao, nao
 * gera analise, pendencia nem resultado de completude. O conteudo dos documentos e dado nao confiavel: valores
 * extraidos nunca vao para log nem para mensagem de erro. Este servico nao guarda dados de requisicao em campos.
 *
 * <p>Ordem de {@link #ler}: dona (404) -> estado (409) -> documentos (422) -> leitura ja existente para a versao
 * (devolvida, sem chamar o leitor) -> abrir os arquivos e chamar o leitor <em>sem conexao aberta</em> -> validar a
 * saida -> gravar numa transacao curta que so vale se a versao capturada ainda for a atual.
 */
public class ExtracaoService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtracaoService.class);

    private final ExtracaoRepository extracoes;
    private final LeitorDeDocumento leitor;
    private final ArmazenamentoPrivado armazenamento;
    private final Clock relogio;

    /**
     * Resultado de {@link #ler}.
     *
     * @param extracao leitura gravada ou ja existente, com a classificacao atual/desatualizada
     * @param criada {@code true} se esta chamada gravou a leitura; {@code false} se ela ja existia
     */
    public record ResultadoLeitura(ExtracaoRegistrada extracao, boolean criada) {
    }

    /**
     * @param extracoes persistencia das leituras
     * @param leitor leitor de documentos (simulado hoje)
     * @param armazenamento area privada de arquivos (so leitura)
     * @param relogio relogio da aplicacao
     */
    public ExtracaoService(ExtracaoRepository extracoes, LeitorDeDocumento leitor,
            ArmazenamentoPrivado armazenamento, Clock relogio) {
        this.extracoes = extracoes;
        this.leitor = leitor;
        this.armazenamento = armazenamento;
        this.relogio = relogio;
    }

    /**
     * Le os documentos disponiveis da versao atual do pedido da paciente e guarda o resultado. Repetir sobre a
     * mesma versao devolve a leitura existente.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a leitura e se foi criada agora
     * @throws ExcecaoNegocio 404 pedido inexistente ou alheio; 409 pedido submetido ou documentos que mudaram
     *     durante a leitura; 422 pedido sem documentos; 503 {@code LEITURA_INDISPONIVEL} se o leitor falhar ou
     *     responder fora do contrato (nada e gravado); 503 {@code DEPENDENCIA_INDISPONIVEL} se o banco ou a area
     *     de arquivos estiver indisponivel
     */
    public ResultadoLeitura ler(SessaoAutenticada sessao, long solicitacaoId) {
        ContextoLeitura contexto = extracoes.contexto(sessao.usuarioId(), solicitacaoId)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
        if (contexto.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            throw submetido();
        }
        if (contexto.documentos().isEmpty()) {
            throw new ExcecaoNegocio(422, "SEM_DOCUMENTOS",
                    "Este pedido ainda não tem documentos para ler. Envie um documento primeiro.");
        }
        var existente = extracoes.buscarMaisRecente(sessao.usuarioId(), solicitacaoId);
        if (existente.isPresent() && existente.get().extracao().revisao() == contexto.revisaoAtual()) {
            return new ResultadoLeitura(existente.get(), false);
        }
        List<LeituraParaGravar> leituras = lerDocumentos(contexto);
        ResultadoGravacao resultado = extracoes.gravar(sessao.usuarioId(), solicitacaoId, contexto.revisaoAtual(),
                leituras, agora());
        return switch (resultado.situacao()) {
            case GRAVADA -> {
                LOG.info("Leitura simulada gravada. solicitacaoId={} revisao={} documentos={}", solicitacaoId,
                        contexto.revisaoAtual(), leituras.size());
                yield new ResultadoLeitura(resultado.extracao(), true);
            }
            case JA_EXISTIA -> new ResultadoLeitura(resultado.extracao(), false);
            case REVISAO_MUDOU -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Os documentos mudaram; tente de novo.");
            case SUBMETIDO -> throw submetido();
            case NAO_ENCONTRADO -> throw Propriedade.naoEncontrada(sessao);
        };
    }

    /**
     * Devolve a leitura mais recente do pedido, classificada como atual ou desatualizada.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return a leitura mais recente
     * @throws ExcecaoNegocio 404 se o pedido for inexistente ou alheio, ou se nunca houve leitura (resposta
     *     identica); 503 se o banco estiver indisponivel
     */
    public ExtracaoRegistrada consultar(SessaoAutenticada sessao, long solicitacaoId) {
        return extracoes.buscarMaisRecente(sessao.usuarioId(), solicitacaoId)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
    }

    /**
     * Abre cada arquivo e chama o leitor, sem conexao de banco aberta. Falha do leitor ou saida fora do contrato
     * em qualquer documento aborta tudo: nenhum resultado parcial e gravado.
     */
    private List<LeituraParaGravar> lerDocumentos(ContextoLeitura contexto) {
        List<LeituraParaGravar> leituras = new ArrayList<>();
        for (DocumentoParaLeitura documento : contexto.documentos()) {
            LeituraDeDocumento leitura;
            if (documento.arquivo() == null) {
                // Linha da carga sintetica: nunca teve arquivo, entao nao ha o que ler.
                leitura = LeituraDeDocumento.vazia();
            } else {
                byte[] conteudo = abrir(documento);
                try {
                    leitura = leitor.ler(new ArquivoParaLeitura(documento.formato(), documento.paginas(), conteudo));
                    ValidadorDeLeitura.exigir(leitura, documento.formato(), documento.paginas());
                } catch (LeituraIndisponivelException falha) {
                    // Sem a mensagem da falha no log: ela pode refletir o conteudo de um documento.
                    LOG.warn("Leitor indisponivel ou fora do contrato. documentoId={} tipo={}",
                            documento.documentoId(), falha.getClass().getSimpleName());
                    throw new ExcecaoNegocio(503, "LEITURA_INDISPONIVEL",
                            "A leitura dos documentos está indisponível. Tente novamente em instantes.");
                }
            }
            leituras.add(new LeituraParaGravar(documento.documentoId(), leitura));
        }
        return leituras;
    }

    private byte[] abrir(DocumentoParaLeitura documento) {
        try (InputStream fluxo = armazenamento.abrir(documento.arquivo())) {
            return fluxo.readAllBytes();
        } catch (ArquivoAusenteException | IOException ausente) {
            LOG.warn("Arquivo de documento indisponivel para leitura. documentoId={}", documento.documentoId());
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "O armazenamento de arquivos está indisponível. Tente novamente mais tarde.");
        }
    }

    private static ExcecaoNegocio submetido() {
        return new ExcecaoNegocio(409, "CONFLITO_ESTADO", "Este pedido já foi submetido e só pode ser consultado.");
    }

    /** Instante atual na precisao que o banco guarda. */
    private Instant agora() {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
