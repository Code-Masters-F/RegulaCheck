package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import java.util.List;
import java.util.Map;

/**
 * Leitura de um documento, com as decisoes da paciente, como a conferencia a publica. Nao expoe caminho, hash nem
 * nome interno. Documento nao lido traz o motivo e a orientacao de reenvio da leitura (RC-10).
 *
 * @param documentoId documento lido: numero decimal opaco, escrito como texto
 * @param tipoDocumental tipo informado pela paciente no envio
 * @param situacao {@code LIDO}, {@code PROTEGIDO} ou {@code LEITURA_VAZIA}
 * @param motivo por que o documento nao foi lido; omitido quando lido
 * @param reenviar {@code true} quando a paciente deve enviar outro arquivo; omitido quando lido
 * @param orientacao o que fazer; omitida quando lido
 * @param campos campos com decisao e orientacao; vazio quando o documento nao foi lido
 */
public record DocumentoConferidoResponse(String documentoId, String tipoDocumental, String situacao, String motivo,
        Boolean reenviar, String orientacao, List<CampoConferidoResponse> campos) {

    /**
     * @param documento leitura do documento
     * @param decisoes decisoes vigentes da leitura inteira (so as deste documento e campo sao usadas)
     * @return resposta publica correspondente, com mensagens fixas (sem eco de conteudo)
     */
    public static DocumentoConferidoResponse de(ExtracaoDocumento documento, List<DecisaoConferencia> decisoes) {
        Map<CodigoCampo, DecisaoConferencia> doDocumento = new java.util.EnumMap<>(CodigoCampo.class);
        for (DecisaoConferencia decisao : decisoes) {
            if (decisao.documentoId() == documento.documentoId()) {
                doDocumento.put(decisao.codigo(), decisao);
            }
        }
        List<CampoConferidoResponse> campos = documento.campos().stream()
                .map(campo -> CampoConferidoResponse.de(campo, doDocumento.get(campo.codigo()))).toList();
        DocumentoLidoResponse base = DocumentoLidoResponse.de(documento);
        return new DocumentoConferidoResponse(base.documentoId(), base.tipoDocumental(), base.situacao(),
                base.motivo(), base.reenviar(), base.orientacao(), campos);
    }
}
