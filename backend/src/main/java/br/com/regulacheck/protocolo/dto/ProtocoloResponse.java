package br.com.regulacheck.protocolo.dto;

import br.com.regulacheck.protocolo.entity.Protocolo;

/**
 * Servico disponivel para escolha, como a API o publica. Nao expoe o identificador interno do banco.
 *
 * @param protocoloId identificador publico ({@code identificador-versao}) enviado pelo cliente ao criar o pedido
 * @param identificador nome do protocolo
 * @param versao versao do protocolo
 * @param localidade localidade demonstrativa (ficticia)
 * @param especialidade especialidade demonstrativa (ficticia)
 * @param avisoFicticio aviso de que o protocolo e academico e nao e exigencia oficial
 * @param simulacao sempre {@code true}: indica que o servico e demonstrativo
 */
public record ProtocoloResponse(String protocoloId, String identificador, String versao, String localidade,
        String especialidade, String avisoFicticio, boolean simulacao) {

    /**
     * @param protocolo protocolo lido do banco
     * @return resposta publica correspondente
     */
    public static ProtocoloResponse de(Protocolo protocolo) {
        return new ProtocoloResponse(protocolo.protocoloId(), protocolo.identificador(), protocolo.versao(),
                protocolo.localidade(), protocolo.especialidade(), protocolo.avisoFicticio(), true);
    }
}
