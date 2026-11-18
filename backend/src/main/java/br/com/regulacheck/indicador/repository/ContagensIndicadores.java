package br.com.regulacheck.indicador.repository;

import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contagens brutas lidas do banco para os indicadores do administrador (RC-20). A base das contagens segue a spec
 * 024 (Clarifications): analises e pendencias so das analises vigentes; falha pelo estado atual do pedido.
 *
 * @param totalPedidos todos os pedidos, em qualquer estado
 * @param pedidosEmFalhaAnalise pedidos cujo estado atual e {@code FALHA_ANALISE} (a analise que falha nao e gravada)
 * @param analisesVigentesPorResultado analises com {@code VIGENTE = 1} por resultado geral; um resultado sem
 *     analise pode estar ausente do mapa (quem consome completa com zero)
 * @param pendenciasPorRegra pendencias das analises vigentes agrupadas por regra; so regras com ao menos uma
 *     pendencia, sem ordem garantida
 */
public record ContagensIndicadores(long totalPedidos, long pedidosEmFalhaAnalise,
        Map<ResultadoGeral, Long> analisesVigentesPorResultado, List<PendenciasDaRegra> pendenciasPorRegra) {

    /** Copia as colecoes para que o registro nao mude depois de criado. */
    public ContagensIndicadores {
        analisesVigentesPorResultado = Map.copyOf(Objects.requireNonNull(analisesVigentesPorResultado));
        pendenciasPorRegra = List.copyOf(Objects.requireNonNull(pendenciasPorRegra));
    }

    /** @return contagens de um banco sem pedidos nem analises */
    public static ContagensIndicadores vazias() {
        return new ContagensIndicadores(0, 0, Map.of(), List.of());
    }

    /**
     * Pendencias (pendente e nao verificavel) de uma regra nas analises vigentes.
     *
     * @param codigo codigo da regra (ex.: {@code RC-R02})
     * @param descricao nome curto da regra, como gravado em {@code RC_REGRA.DESCRICAO}
     * @param pendentes quantidade de pendencias do tipo {@code PENDENTE}
     * @param naoVerificaveis quantidade de pendencias do tipo {@code NAO_VERIFICAVEL}
     */
    public record PendenciasDaRegra(String codigo, String descricao, long pendentes, long naoVerificaveis) {

        /** @return soma das duas parcelas */
        public long total() {
            return pendentes + naoVerificaveis;
        }
    }
}
