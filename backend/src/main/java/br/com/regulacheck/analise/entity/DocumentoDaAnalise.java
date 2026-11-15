package br.com.regulacheck.analise.entity;

import br.com.regulacheck.documento.entity.TipoDocumental;
import java.util.Objects;

/**
 * Documento que fazia parte do pedido na revisao avaliada por uma analise, para a paciente saber a qual documento
 * cada resultado se refere mesmo depois de substitui-lo ou remove-lo.
 *
 * @param documentoId documento
 * @param nomeArquivo nome de exibicao ja saneado no envio (nunca o nome de armazenamento)
 * @param tipoDeclarado tipo escolhido pela paciente no envio (nao e prova do tipo; a regra usa o tipo lido)
 * @param disponivel {@code true} se o documento continua no pedido; {@code false} se depois foi substituido ou
 *     removido
 */
public record DocumentoDaAnalise(long documentoId, String nomeArquivo, TipoDocumental tipoDeclarado,
        boolean disponivel) {

    /** Exige nome e tipo. */
    public DocumentoDaAnalise {
        Objects.requireNonNull(nomeArquivo, "nomeArquivo");
        Objects.requireNonNull(tipoDeclarado, "tipoDeclarado");
    }
}
