package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.util.List;

/**
 * Retrato do pedido da paciente no instante em que a leitura foi pedida: estado, versao do conjunto documental
 * (revisao atual) e documentos disponiveis. A revisao capturada aqui e a unica para a qual a leitura pode ser
 * gravada.
 *
 * @param estado estado atual do pedido
 * @param revisaoAtual versao do conjunto documental
 * @param documentos documentos disponiveis, na ordem de envio
 */
public record ContextoLeitura(EstadoSolicitacao estado, int revisaoAtual, List<DocumentoParaLeitura> documentos) {

    /** Copia defensiva. */
    public ContextoLeitura {
        documentos = List.copyOf(documentos);
    }
}
