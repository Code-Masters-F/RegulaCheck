package br.com.regulacheck.extracao.service;

import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.repository.ConferenciaRegistrada;
import br.com.regulacheck.extracao.repository.ConferenciaRepository;
import br.com.regulacheck.extracao.repository.DecisaoNova;
import br.com.regulacheck.extracao.repository.ResultadoRegistroDecisao;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso da conferencia da transcricao: a paciente ve o que foi lido (simulado) e confirma ou aponta
 * divergencia campo a campo. Aplica a regra de dona da RC-05 (pedido ou leitura de outra paciente e pedido ou
 * leitura inexistente tem a mesma resposta).
 *
 * <p><strong>A conferencia nao decide nada alem da conferencia:</strong> nao altera o estado do pedido, nao cria
 * revisao, nao altera a leitura original, nao gera analise, pendencia nem resultado de completude e nunca libera
 * submissao. A paciente nunca informa valor: so confirma ou aponta divergencia. O conteudo dos documentos e dado
 * nao confiavel: valores lidos nunca vao para log nem para mensagem de erro. Este servico nao guarda dados de
 * requisicao em campos.
 */
public class ConferenciaService {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenciaService.class);

    private final ConferenciaRepository conferencias;
    private final Clock relogio;

    /**
     * @param conferencias persistencia das decisoes
     * @param relogio relogio da aplicacao
     */
    public ConferenciaService(ConferenciaRepository conferencias, Clock relogio) {
        this.conferencias = conferencias;
        this.relogio = relogio;
    }

    /**
     * Devolve a conferencia da leitura: campos, origem, decisoes vigentes, orientacoes e a situacao calculada. Uma
     * leitura desatualizada e devolvida como {@code DESATUALIZADA} (somente leitura).
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @return a conferencia
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 404 se o pedido ou a leitura for inexistente,
     *     alheio ou de outro pedido (resposta identica); 503 se o banco estiver indisponivel
     */
    public VisaoConferencia consultar(SessaoAutenticada sessao, long solicitacaoId, long extracaoId) {
        ConferenciaRegistrada registrada = conferencias.buscar(sessao.usuarioId(), solicitacaoId, extracaoId)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
        return visao(registrada);
    }

    /**
     * Confere que o pedido e a leitura existem e sao da paciente, sem devolver nada. O controller o chama
     * <em>antes</em> de ler o corpo do {@code PUT}, para que um corpo ruim enviado a um pedido ou leitura alheios
     * nao revele que eles existem (ordem de {@code docs/api/erros.md}).
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @throws ExcecaoNegocio 404 se o pedido ou a leitura for inexistente, alheio ou de outro pedido; 503 se o
     *     banco estiver indisponivel
     */
    public void exigirLeitura(SessaoAutenticada sessao, long solicitacaoId, long extracaoId) {
        consultar(sessao, solicitacaoId, extracaoId);
    }

    /**
     * Registra as decisoes da paciente sobre os campos da leitura, de forma atomica: ou ficam todas ou nenhuma.
     * Decidir de novo substitui a decisao vigente e repetir a mesma nao altera nada. Nao altera o estado do pedido,
     * nao cria revisao, nao altera a leitura original e nunca libera submissao.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @param extracaoId leitura
     * @param decisoes decisoes ja validadas no formato (de 1 a 18, sem par repetido)
     * @return a conferencia ja com a situacao nova
     * @throws ExcecaoNegocio 404 pedido, leitura ou documento da decisao fora do pedido (resposta identica); 409
     *     pedido submetido ou leitura desatualizada (nada e gravado); 422 {@code DECISAO_NAO_APLICAVEL} se alguma
     *     decisao nao couber a situacao do campo (nada e gravado); 503 se o banco estiver indisponivel
     */
    public VisaoConferencia registrar(SessaoAutenticada sessao, long solicitacaoId, long extracaoId,
            List<DecisaoNova> decisoes) {
        ResultadoRegistroDecisao resultado = conferencias.registrar(sessao.usuarioId(), solicitacaoId, extracaoId,
                decisoes, agora());
        switch (resultado.situacao()) {
            case REGISTRADA -> {
                LOG.info("Conferencia atualizada. solicitacaoId={} extracaoId={} decisoes={}", solicitacaoId,
                        extracaoId, decisoes.size());
                return consultar(sessao, solicitacaoId, extracaoId);
            }
            case NAO_ENCONTRADO, DOCUMENTO_FORA_DA_LEITURA -> throw Propriedade.naoEncontrada(sessao);
            case SUBMETIDO -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Este pedido já foi submetido e só pode ser consultado.");
            case DESATUALIZADA -> throw new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                    "Os documentos mudaram; leia os documentos de novo.");
            case DECISAO_NAO_APLICAVEL -> throw new ExcecaoNegocio(422, "DECISAO_NAO_APLICAVEL",
                    "Uma das decisões não se aplica à situação do campo.",
                    List.of(new ErroPadrao.Detalhe("decisoes", "A decisão para o campo " + resultado.codigo().name()
                            + " do documento " + resultado.documentoId() + " não se aplica à situação do campo. "
                            + "Campo ilegível se resolve enviando um arquivo mais nítido; campo não localizado só "
                            + "admite apontar que o campo existe no documento.")));
        }
        throw new IllegalStateException("Desfecho de registro desconhecido.");
    }

    /**
     * Monta a visao publicavel: situacao calculada pela regra pura e instante da ultima alteracao. Publico para a
     * avaliacao dos criterios (RC-12) capturar o mesmo {@code atualizadaEm} que a conferencia expoe, sem recalcular
     * por conta propria.
     *
     * @param registrada leitura com as decisoes vigentes
     * @return a visao
     */
    public static VisaoConferencia visao(ConferenciaRegistrada registrada) {
        var leitura = registrada.leitura();
        Instant atualizadaEm = registrada.decisoes().stream().map(DecisaoConferencia::decididaEm)
                .max(Instant::compareTo).orElse(leitura.extracao().criadaEm());
        return new VisaoConferencia(registrada,
                SituacaoConferencia.avaliar(leitura.extracao(), leitura.atual(), registrada.decisoes()),
                atualizadaEm);
    }

    /** Instante atual na precisao que o banco guarda. */
    private Instant agora() {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
