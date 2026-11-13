package br.com.regulacheck.protocolo.entity;

/**
 * Servico demonstrativo escolhido pela paciente: uma linha de {@code RC_PROTOCOLO} (protocolo academico
 * ficticio, com localidade e especialidade). Modelo interno: nunca e serializado como resposta publica
 * (usar os DTOs do pacote {@code dto}).
 *
 * @param id identificador interno; nao sai pela API
 * @param identificador nome do protocolo, ex.: {@code PROTOCOLO-ACADEMICO-FICTICIO}
 * @param versao versao do protocolo, ex.: {@code v0.1}
 * @param localidade localidade demonstrativa (ficticia)
 * @param especialidade especialidade demonstrativa (ficticia)
 * @param avisoFicticio aviso de que o protocolo nao e exigencia oficial
 */
public record Protocolo(long id, String identificador, String versao, String localidade, String especialidade,
        String avisoFicticio) {

    /**
     * Identificador publico do servico, o mesmo valor que o cliente envia em {@code protocoloId}.
     *
     * @return {@code identificador-versao}, ex.: {@code PROTOCOLO-ACADEMICO-FICTICIO-v0.1}
     */
    public String protocoloId() {
        return identificador + "-" + versao;
    }
}
