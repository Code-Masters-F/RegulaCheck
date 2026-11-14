package br.com.regulacheck.extracao.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Instante gravado para as decisoes que mudam a conferencia de uma leitura (RC-13). O instante da conferencia (a
 * decisao mais recente ou, sem decisao, a criacao da leitura) e o que a analise grava e confere antes de publicar;
 * ele precisa mudar a cada gravacao que muda o conteudo da conferencia.
 */
public final class InstanteDaConferencia {

    private InstanteDaConferencia() {
    }

    /**
     * Garante o avanco do instante da conferencia: devolve {@code agora} se ele ja e posterior ao ultimo instante;
     * senao, o ultimo instante mais 1 microssegundo (a precisao do banco). Chamado sob o bloqueio do pedido.
     *
     * @param ultimo instante atual da conferencia (ultima decisao ou criacao da leitura)
     * @param agora instante capturado antes do bloqueio, ja na precisao do banco
     * @return instante a gravar, sempre posterior a {@code ultimo}
     */
    public static Instant depoisDe(Instant ultimo, Instant agora) {
        return agora.isAfter(ultimo) ? agora : ultimo.plus(1, ChronoUnit.MICROS);
    }
}
