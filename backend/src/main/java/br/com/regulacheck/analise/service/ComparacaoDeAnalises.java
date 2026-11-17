package br.com.regulacheck.analise.service;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.ResultadoGravado;
import br.com.regulacheck.validacao.model.Resultado;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Comparacao de uma analise registrada com a analise registrada imediatamente antes dela no mesmo pedido (RC-16):
 * por regra, o que foi {@linkplain Situacao#RESOLVIDA resolvido}, o que {@linkplain Situacao#CONTINUA continua} e o
 * que e {@linkplain Situacao#NOVA novo}. Regra pura: sem HTTP, sem JDBC, sem relogio.
 *
 * <p><strong>Calculada, nunca gravada.</strong> As analises sao imutaveis (RC-13); a comparacao e derivada delas a
 * cada consulta e nao altera nenhuma das duas. Como so uma analise concluida e registrada entra aqui, enviar,
 * substituir ou remover documento, ler ou conferir nunca faz uma pendencia aparecer como resolvida: isso so acontece
 * quando ha um novo resultado.
 *
 * <p>"Resolvida" e so a leitura da regra no novo resultado simulado do protocolo academico ficticio; nao e aprovacao
 * do SUS.
 *
 * @param analiseAnteriorId analise com que a atual foi comparada
 * @param revisaoAnterior revisao do conjunto documental avaliada pela anterior
 * @param executadaEmAnterior conclusao da anterior, em UTC
 * @param itens uma entrada por regra com pendencia em pelo menos uma das duas analises, na ordem da analise atual;
 *     pode ser vazia
 */
public record ComparacaoDeAnalises(long analiseAnteriorId, int revisaoAnterior, Instant executadaEmAnterior,
        List<ItemComparado> itens) {

    /** Exige o instante e copia os itens. */
    public ComparacaoDeAnalises {
        Objects.requireNonNull(executadaEmAnterior, "executadaEmAnterior");
        itens = List.copyOf(itens);
    }

    /** Como a regra mudou entre a analise anterior e a atual. */
    public enum Situacao {
        /** Gerava pendencia ({@code PENDENTE}/{@code NAO_VERIFICAVEL}) e agora atende ou nao se aplica. */
        RESOLVIDA,
        /** Gera pendencia nas duas analises, mesmo que o tipo tenha mudado. */
        CONTINUA,
        /** Nao gerava pendencia e agora gera. */
        NOVA
    }

    /**
     * Uma regra na comparacao.
     *
     * @param codigoRegra codigo da regra na versao do protocolo
     * @param descricaoRegra descricao da regra na analise atual
     * @param situacao resolvida, continua ou nova
     * @param resultadoAnterior resultado da regra na analise anterior
     * @param resultadoAtual resultado da regra na analise atual
     */
    public record ItemComparado(String codigoRegra, String descricaoRegra, Situacao situacao,
            Resultado resultadoAnterior, Resultado resultadoAtual) {

        /** Exige codigo, situacao e os dois resultados. */
        public ItemComparado {
            Objects.requireNonNull(codigoRegra, "codigoRegra");
            Objects.requireNonNull(situacao, "situacao");
            Objects.requireNonNull(resultadoAnterior, "resultadoAnterior");
            Objects.requireNonNull(resultadoAtual, "resultadoAtual");
        }
    }

    /**
     * Compara duas analises do mesmo pedido.
     *
     * <p>Sem analise anterior, ou com protocolo ou versao diferentes, nao ha comparacao: os codigos de regra de
     * versoes diferentes nao sao garantidamente o mesmo criterio. Regra que existe em so uma das analises nao e
     * comparada (na mesma versao do protocolo cada analise tem exatamente um resultado por regra, RC-13).
     *
     * @param atual analise mais recente
     * @param anterior analise registrada imediatamente antes; {@code null} se nao ha
     * @return a comparacao, ou {@code null} quando nao ha comparacao aplicavel
     */
    public static ComparacaoDeAnalises comparar(Analise atual, Analise anterior) {
        Objects.requireNonNull(atual, "atual");
        if (anterior == null || !mesmaVersao(atual, anterior)) {
            return null;
        }
        Map<String, Resultado> resultadosAnteriores = new HashMap<>();
        for (ResultadoGravado resultado : anterior.resultados()) {
            resultadosAnteriores.put(resultado.codigoRegra(), resultado.resultado());
        }
        List<ItemComparado> itens = new ArrayList<>();
        for (ResultadoGravado resultado : atual.resultados()) {
            Resultado antes = resultadosAnteriores.get(resultado.codigoRegra());
            if (antes == null) {
                continue;
            }
            Situacao situacao = situacao(antes, resultado.resultado());
            if (situacao != null) {
                itens.add(new ItemComparado(resultado.codigoRegra(), resultado.descricaoRegra(), situacao, antes,
                        resultado.resultado()));
            }
        }
        return new ComparacaoDeAnalises(anterior.id(), anterior.revisao(), anterior.executadaEm(), itens);
    }

    /**
     * Compara a analise com a que veio imediatamente antes dela num historico ordenado da mais recente para a mais
     * antiga (a ordem de {@code GET .../analises}).
     *
     * @param atual analise a comparar (procurada no historico pelo id)
     * @param historicoDoMaisRecente analises do pedido, da mais recente para a mais antiga
     * @return a comparacao, ou {@code null} se a analise nao esta no historico, se e a mais antiga dele ou se nao ha
     *     comparacao aplicavel
     */
    public static ComparacaoDeAnalises comAnterior(Analise atual, List<Analise> historicoDoMaisRecente) {
        Objects.requireNonNull(atual, "atual");
        for (int i = 0; i < historicoDoMaisRecente.size(); i++) {
            if (historicoDoMaisRecente.get(i).id() == atual.id()) {
                return i + 1 < historicoDoMaisRecente.size()
                        ? comparar(atual, historicoDoMaisRecente.get(i + 1)) : null;
            }
        }
        return null;
    }

    /** @return a situacao, ou {@code null} se a regra nao gera pendencia em nenhuma das duas analises */
    private static Situacao situacao(Resultado anterior, Resultado atual) {
        if (anterior.geraPendencia()) {
            return atual.geraPendencia() ? Situacao.CONTINUA : Situacao.RESOLVIDA;
        }
        return atual.geraPendencia() ? Situacao.NOVA : null;
    }

    private static boolean mesmaVersao(Analise atual, Analise anterior) {
        return atual.protocoloIdentificador().equals(anterior.protocoloIdentificador())
                && atual.protocoloVersao().equals(anterior.protocoloVersao());
    }
}
