package br.com.regulacheck.solicitacao.dto;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Corpo de {@code POST /solicitacoes} e {@code PUT /solicitacoes/{id}}: so a escolha do servico.
 *
 * <p>Outros campos enviados pelo cliente (dono, estado, revisao) nao existem neste tipo e portanto nunca sao
 * lidos. {@code protocoloId} chega como {@link JsonNode} porque o mapeador do projeto converte numeros e
 * booleanos em texto sem avisar; aqui so um valor JSON do tipo texto e aceito.
 *
 * @param protocoloId servico escolhido ({@code identificador-versao}); ausente, nulo ou nao textual e invalido
 */
public record SolicitacaoRequest(JsonNode protocoloId) {

    /** Tamanho maximo aceito; maior que {@code IDENTIFICADOR (60) + "-" + VERSAO (20)} de {@code RC_PROTOCOLO}. */
    public static final int PROTOCOLO_ID_MAXIMO = 100;

    /**
     * Valida o corpo e devolve o servico escolhido.
     *
     * @return o texto de {@code protocoloId}, ja aparado
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} com detalhe no campo {@code protocoloId} se estiver
     *     ausente, nulo, em branco, grande demais ou nao for texto
     */
    public String validar() {
        String problema = null;
        String valor = null;
        if (protocoloId == null || protocoloId.isNull() || protocoloId.isMissingNode()) {
            problema = "Informe o serviço.";
        } else if (!protocoloId.isTextual()) {
            problema = "O serviço deve ser informado como texto.";
        } else {
            valor = protocoloId.asText().strip();
            if (valor.isEmpty()) {
                problema = "Informe o serviço.";
            } else if (valor.length() > PROTOCOLO_ID_MAXIMO) {
                problema = "O serviço deve ter no máximo " + PROTOCOLO_ID_MAXIMO + " caracteres.";
            }
        }
        if (problema != null) {
            throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "Verifique os campos informados.",
                    List.of(new ErroPadrao.Detalhe("protocoloId", problema)));
        }
        return valor;
    }
}
