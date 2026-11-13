package br.com.regulacheck.extracao.entity;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import java.time.Instant;

/**
 * Decisao vigente da paciente sobre um campo de uma leitura. Modelo interno: nunca e serializado como resposta
 * publica. Nao carrega valor: a decisao e so confirmar ou apontar divergencia.
 *
 * @param documentoId documento a que o campo pertence
 * @param codigo campo decidido
 * @param tipo confirmada ou divergente
 * @param motivo motivo da divergencia; {@code null} quando confirmada
 * @param decididaEm instante em que a decisao vigente foi tomada, em UTC
 */
public record DecisaoConferencia(long documentoId, CodigoCampo codigo, TipoDecisao tipo, MotivoDivergencia motivo,
        Instant decididaEm) {

    /** Sem dados do documento: so ids e codigos. */
    @Override
    public String toString() {
        return "DecisaoConferencia[documentoId=" + documentoId + ", codigo=" + codigo + ", tipo=" + tipo + "]";
    }
}
