package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.integracao.extracao.CodigoCampo;

/**
 * Decisao a registrar, ja validada no formato pelo {@code ConferenciaRequest}. Nao carrega valor: so confirmar ou
 * apontar divergencia.
 *
 * @param documentoId documento a que o campo pertence
 * @param codigo campo decidido
 * @param tipo confirmada ou divergente
 * @param motivo motivo da divergencia; {@code null} quando confirmada
 */
public record DecisaoNova(long documentoId, CodigoCampo codigo, TipoDecisao tipo, MotivoDivergencia motivo) {
}
