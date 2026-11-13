package br.com.regulacheck.extracao.entity;

import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import java.util.List;

/**
 * Leitura de um documento dentro de uma {@link Extracao}. Modelo interno: nunca e serializado como resposta
 * publica (usar {@code DocumentoLidoResponse}).
 *
 * @param documentoId documento lido
 * @param tipoDocumental tipo informado pela paciente no envio (nao e reconciliado com o campo lido)
 * @param situacao lido, protegido ou leitura vazia
 * @param campos campos extraidos; vazio quando o documento nao foi lido
 */
public record ExtracaoDocumento(long documentoId, TipoDocumental tipoDocumental, SituacaoLeitura situacao,
        List<CampoLido> campos) {

    /** Copia defensiva. */
    public ExtracaoDocumento {
        campos = campos == null ? List.of() : List.copyOf(campos);
    }
}
