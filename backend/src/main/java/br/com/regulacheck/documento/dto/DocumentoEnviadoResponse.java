package br.com.regulacheck.documento.dto;

import br.com.regulacheck.documento.entity.Documento;

/**
 * Resposta do envio de um documento: os dados do documento mais a versao do conjunto documental e a contagem de
 * documentos do pedido <em>no instante em que o envio foi aplicado</em>. Receber o arquivo nao e analisa-lo.
 *
 * @param id identificador do documento
 * @param solicitacaoId pedido ao qual o documento pertence
 * @param nomeArquivo nome de exibicao saneado
 * @param tipoDocumental tipo informado pela paciente
 * @param formato formato real do conteudo
 * @param tamanhoBytes tamanho em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param estadoEnvio estado do envio; hoje sempre {@code ENVIADO}
 * @param revisaoEntrada revisao do pedido em que o documento entrou
 * @param enviadoEm instante do envio em ISO-8601 UTC
 * @param simulacao sempre {@code true}
 * @param versaoConjuntoDocumental versao do conjunto de documentos do pedido (a revisao vigente)
 * @param documentosDisponiveis quantos documentos disponiveis o pedido tem
 */
public record DocumentoEnviadoResponse(String id, String solicitacaoId, String nomeArquivo, String tipoDocumental,
        String formato, long tamanhoBytes, int paginas, String estadoEnvio, int revisaoEntrada, String enviadoEm,
        boolean simulacao, int versaoConjuntoDocumental, int documentosDisponiveis) {

    /**
     * @param documento documento registrado ou ja existente
     * @param versaoConjuntoDocumental revisao do pedido apos o envio
     * @param documentosDisponiveis documentos disponiveis do pedido apos o envio
     * @return resposta publica correspondente
     */
    public static DocumentoEnviadoResponse de(Documento documento, int versaoConjuntoDocumental,
            int documentosDisponiveis) {
        DocumentoResponse base = DocumentoResponse.de(documento);
        return new DocumentoEnviadoResponse(base.id(), base.solicitacaoId(), base.nomeArquivo(),
                base.tipoDocumental(), base.formato(), base.tamanhoBytes(), base.paginas(), base.estadoEnvio(),
                base.revisaoEntrada(), base.enviadoEm(), base.simulacao(), versaoConjuntoDocumental,
                documentosDisponiveis);
    }
}
