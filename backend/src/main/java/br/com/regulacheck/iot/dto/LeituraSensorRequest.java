package br.com.regulacheck.iot.dto;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.repository.NovaLeitura;
import br.com.regulacheck.iot.service.FaixasDemonstrativas;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Validacao do corpo de {@code POST /sessoes-captura/{id}/leituras} (RC-21, contrato em
 * {@code specs/016-iot-simulada/contracts/api-iot.md}). O corpo vem do simulador e e tratado como dado nao
 * confiavel: so os cinco campos do contrato sao aceitos, e nenhum valor recebido volta nas mensagens de erro.
 *
 * <p>Regras (research D4): {@code sessaoId} igual ao do caminho; {@code tipo} {@code LUMINOSIDADE} ou
 * {@code DISTANCIA}; {@code unidade} exatamente a do tipo ({@code LUX} ou {@code CM}), sem conversao; {@code valor}
 * numerico dentro do limite fisico ({@link FaixasDemonstrativas#fisicamentePlausivel}), arredondado a 3 casas;
 * {@code medidaEm} instante ISO-8601 com fuso, no maximo {@link FaixasDemonstrativas#TOLERANCIA_FUTURO} a frente
 * do relogio do servidor. Fora da faixa demonstrativa nao e erro: a leitura e gravada e so muda a orientacao.
 */
public final class LeituraSensorRequest {

    private static final Set<String> CAMPOS = Set.of("sessaoId", "tipo", "valor", "unidade", "medidaEm");

    private LeituraSensorRequest() {
    }

    /**
     * @param corpo JSON ja lido (limite de tamanho e tipo de conteudo conferidos pelo {@code CorpoJson})
     * @param sessaoIdDoCaminho sessao indicada na rota
     * @param agora instante do servidor; vira {@code recebidaEm}
     * @return a leitura validada
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} com o campo recusado
     */
    public static NovaLeitura validar(JsonNode corpo, long sessaoIdDoCaminho, Instant agora) {
        if (corpo == null || !corpo.isObject()) {
            throw recusa("corpo", "Envie um objeto com a leitura do sensor.");
        }
        corpo.fieldNames().forEachRemaining(campo -> {
            if (!CAMPOS.contains(campo)) {
                throw recusa("corpo", "Há um campo não permitido. Só são aceitos sessaoId, tipo, valor, unidade e "
                        + "medidaEm.");
            }
        });
        JsonNode sessao = corpo.get("sessaoId");
        if (sessao == null || !sessao.canConvertToLong() || !sessao.isIntegralNumber()
                || sessao.asLong() != sessaoIdDoCaminho) {
            throw recusa("sessaoId", "Informe o identificador da sessão de captura indicada no endereço.");
        }
        TipoSensor tipo = tipo(corpo.get("tipo"));
        JsonNode unidade = corpo.get("unidade");
        if (unidade == null || !unidade.isTextual() || !tipo.unidade().equals(unidade.asText())) {
            throw recusa("unidade", "Use LUX para luminosidade e CM para distância.");
        }
        BigDecimal valor = valor(corpo.get("valor"), tipo);
        Instant medidaEm = medidaEm(corpo.get("medidaEm"), agora);
        return new NovaLeitura(sessaoIdDoCaminho, tipo, valor, medidaEm, agora);
    }

    private static TipoSensor tipo(JsonNode tipo) {
        if (tipo != null && tipo.isTextual()) {
            for (TipoSensor candidato : TipoSensor.values()) {
                if (candidato.name().equals(tipo.asText())) {
                    return candidato;
                }
            }
        }
        throw recusa("tipo", "Use LUMINOSIDADE ou DISTANCIA.");
    }

    private static BigDecimal valor(JsonNode valor, TipoSensor tipo) {
        if (valor == null || !valor.isNumber()) {
            throw recusa("valor", "Informe o valor medido como número.");
        }
        BigDecimal numero = valor.decimalValue().setScale(FaixasDemonstrativas.CASAS_DECIMAIS, RoundingMode.HALF_UP);
        if (!FaixasDemonstrativas.fisicamentePlausivel(tipo, numero)) {
            throw recusa("valor", tipo == TipoSensor.LUMINOSIDADE
                    ? "A luminosidade deve estar entre 0 e 200000 lux."
                    : "A distância deve ser maior que 0 e no máximo 500 cm.");
        }
        return numero;
    }

    private static Instant medidaEm(JsonNode medidaEm, Instant agora) {
        if (medidaEm == null || !medidaEm.isTextual()) {
            throw recusa("medidaEm", "Informe o instante da medição no formato ISO-8601 com fuso.");
        }
        Instant instante;
        try {
            instante = OffsetDateTime.parse(medidaEm.asText()).toInstant();
        } catch (DateTimeParseException invalido) {
            throw recusa("medidaEm", "Informe o instante da medição no formato ISO-8601 com fuso.");
        }
        if (instante.isAfter(agora.plus(FaixasDemonstrativas.TOLERANCIA_FUTURO))) {
            throw recusa("medidaEm", "O instante da medição está no futuro. Confira o relógio do simulador.");
        }
        return instante;
    }

    private static ExcecaoNegocio recusa(String campo, String mensagem) {
        return new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "Verifique os campos informados.",
                List.of(new ErroPadrao.Detalhe(campo, mensagem)));
    }
}
