package br.com.regulacheck.documento.dto;

/**
 * Resposta da remocao de um documento: so o estado novo do conjunto documental do pedido. Nao devolve o documento
 * removido (ele deixou de existir para a paciente) nem qualquer dado interno.
 *
 * @param versaoConjuntoDocumental versao do conjunto de documentos do pedido apos a remocao
 * @param documentosDisponiveis quantos documentos disponiveis o pedido tem apos a remocao
 */
public record RemocaoResponse(int versaoConjuntoDocumental, int documentosDisponiveis) {
}
