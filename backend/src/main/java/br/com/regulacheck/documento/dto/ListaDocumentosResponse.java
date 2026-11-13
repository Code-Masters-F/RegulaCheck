package br.com.regulacheck.documento.dto;

import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.documento.entity.Documento;
import java.util.List;

/**
 * Lista dos documentos disponiveis de um pedido. Sem paginacao: o pedido tem no maximo
 * {@link LimitesArquivo#MAXIMO_ARQUIVOS} documentos.
 *
 * @param documentos documentos na ordem de envio; vazia quando o pedido nao tem documentos
 * @param total quantidade de documentos listados
 * @param maximo limite de documentos disponiveis por pedido
 * @param versaoConjuntoDocumental versao atual do conjunto documental (a revisao vigente do pedido)
 */
public record ListaDocumentosResponse(List<DocumentoResponse> documentos, int total, int maximo,
        int versaoConjuntoDocumental) {

    /**
     * @param documentos documentos lidos do banco
     * @param versaoConjuntoDocumental revisao vigente do pedido
     * @return resposta publica correspondente
     */
    public static ListaDocumentosResponse de(List<Documento> documentos, int versaoConjuntoDocumental) {
        List<DocumentoResponse> itens = documentos.stream().map(DocumentoResponse::de).toList();
        return new ListaDocumentosResponse(itens, itens.size(), LimitesArquivo.MAXIMO_ARQUIVOS,
                versaoConjuntoDocumental);
    }
}
