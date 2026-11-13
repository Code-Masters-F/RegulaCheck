package br.com.regulacheck.documento.dto;

import br.com.regulacheck.documento.entity.Documento;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Documento como a API o publica nas listas e no corpo de {@code DocumentoEnviadoResponse}. Nao expoe o nome
 * interno de armazenamento, o hash do conteudo nem a chave de envio.
 *
 * @param id identificador do documento: numero decimal opaco, escrito como texto
 * @param solicitacaoId pedido ao qual o documento pertence, escrito como texto
 * @param nomeArquivo nome de exibicao saneado
 * @param tipoDocumental tipo informado pela paciente
 * @param formato formato real do conteudo ({@code PDF}, {@code JPEG} ou {@code PNG})
 * @param tamanhoBytes tamanho em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param estadoEnvio estado do <strong>envio</strong>, nao da analise; hoje sempre {@code ENVIADO}
 * @param revisaoEntrada revisao do pedido em que o documento entrou
 * @param enviadoEm instante do envio em ISO-8601 UTC
 * @param simulacao sempre {@code true}: o documento so existe no simulador academico
 */
public record DocumentoResponse(String id, String solicitacaoId, String nomeArquivo, String tipoDocumental,
        String formato, long tamanhoBytes, int paginas, String estadoEnvio, int revisaoEntrada, String enviadoEm,
        boolean simulacao) {

    /**
     * @param documento documento lido do banco
     * @return resposta publica correspondente
     */
    public static DocumentoResponse de(Documento documento) {
        return new DocumentoResponse(Long.toString(documento.id()), Long.toString(documento.solicitacaoId()),
                documento.nomeArquivo(), documento.tipoDocumental().name(), documento.formato().name(),
                documento.tamanhoBytes(), documento.paginas(), documento.estadoEnvio().name(),
                documento.revisaoEntrada(), iso(documento.enviadoEm()), true);
    }

    private static String iso(Instant instante) {
        return instante.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
