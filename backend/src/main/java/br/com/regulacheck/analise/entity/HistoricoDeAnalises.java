package br.com.regulacheck.analise.entity;

import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Historico das analises de um pedido da paciente, junto com o retrato do pedido para a consulta dizer se ha
 * analise em andamento.
 *
 * @param estado estado atual do pedido
 * @param revisaoAtual versao atual do conjunto documental
 * @param analiseIniciadaEm inicio da analise em andamento; {@code null} fora de {@code PROCESSANDO}
 * @param analises analises gravadas, da mais recente para a mais antiga (ate o limite pedido)
 */
public record HistoricoDeAnalises(EstadoSolicitacao estado, int revisaoAtual, Instant analiseIniciadaEm,
        List<Analise> analises) {

    /** Exige o estado e copia a lista. */
    public HistoricoDeAnalises {
        Objects.requireNonNull(estado, "estado");
        analises = List.copyOf(analises);
    }
}
