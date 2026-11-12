package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.service.SessaoAutenticada;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Regra de dona do recurso: so a dona enxerga o que e dela, e quem pergunta por um recurso alheio
 * recebe a mesma resposta de um recurso que nao existe.
 *
 * <p>Como usar: o Repository do recurso expoe uma busca <strong>ja filtrada pela dona</strong>
 * (junta ate {@code RC_SOLICITACAO.PACIENTE_ID} e filtra pela conta da sessao na mesma instrucao
 * SQL, parametrizada) e o Service passa o resultado por {@link #exigir}. Assim nao existe um passo
 * "buscar e depois comparar o dono" que alguem possa esquecer, e nao ha diferenca de caminho entre
 * "nao existe" e "nao e seu".
 */
public final class Propriedade {

    private static final Logger LOG = LoggerFactory.getLogger(Propriedade.class);

    private Propriedade() {
    }

    /**
     * Devolve o recurso ou falha com o 404 padrao.
     *
     * @param recurso resultado da busca ja filtrada pela dona da sessao
     * @param sessao conta autenticada que fez o pedido (so o identificador tecnico vai para o log)
     * @param <T> tipo do recurso
     * @return o recurso, quando existe e pertence a conta
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 404 {@code NAO_ENCONTRADO} se a busca
     *     veio vazia, seja porque o recurso nao existe ou porque pertence a outra paciente
     */
    public static <T> T exigir(Optional<T> recurso, SessaoAutenticada sessao) {
        return recurso.orElseThrow(() -> {
            LOG.info("Recurso nao encontrado ou de outra dona. usuarioId={} perfil={} codigo=NAO_ENCONTRADO",
                    sessao.usuarioId(), sessao.perfil());
            return Erros.naoEncontrado();
        });
    }
}
