package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import java.util.List;

/**
 * Leitura de um documento como a API a publica. Nao expoe caminho, hash nem nome interno.
 *
 * @param documentoId documento lido: numero decimal opaco, escrito como texto
 * @param tipoDocumental tipo informado pela paciente no envio
 * @param situacao {@code LIDO}, {@code PROTEGIDO} ou {@code LEITURA_VAZIA}
 * @param motivo por que o documento nao foi lido; omitido quando lido
 * @param reenviar {@code true} quando a paciente deve enviar outro arquivo; omitido quando lido
 * @param orientacao o que fazer; omitida quando lido
 * @param campos campos extraidos; vazio quando o documento nao foi lido
 */
public record DocumentoLidoResponse(String documentoId, String tipoDocumental, String situacao, String motivo,
        Boolean reenviar, String orientacao, List<CampoExtraidoResponse> campos) {

    /**
     * @param documento leitura do documento
     * @return resposta publica correspondente, com mensagens fixas (sem eco de conteudo)
     */
    public static DocumentoLidoResponse de(ExtracaoDocumento documento) {
        List<CampoExtraidoResponse> campos = documento.campos().stream().map(CampoExtraidoResponse::de).toList();
        String id = Long.toString(documento.documentoId());
        String tipo = documento.tipoDocumental().name();
        return switch (documento.situacao()) {
            case LIDO -> new DocumentoLidoResponse(id, tipo, SituacaoLeitura.LIDO.name(), null, null, null, campos);
            case PROTEGIDO -> new DocumentoLidoResponse(id, tipo, SituacaoLeitura.PROTEGIDO.name(),
                    "O arquivo está protegido e não pôde ser lido.", true,
                    "Envie uma versão do arquivo sem proteção (sem senha).", campos);
            case LEITURA_VAZIA -> new DocumentoLidoResponse(id, tipo, SituacaoLeitura.LEITURA_VAZIA.name(),
                    "Não foi possível extrair informações deste arquivo.", true,
                    "Isso não significa que as informações estejam faltando no documento. "
                            + "Envie um arquivo mais nítido.", campos);
        };
    }
}
