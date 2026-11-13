package br.com.regulacheck.integracao;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Valida a saida estruturada de um adaptador de extracao ou de resumo (mockado hoje, real no futuro) contra um
 * <strong>contrato fechado</strong>; saida fora do contrato e rejeitada, nunca "corrigida".
 *
 * <p>Formato aceito: {@code {"itens":[{"codigoRegra":"...","estado":"...","texto":"..."}]}}. Qualquer outro
 * campo, em qualquer nivel (inclusive campos de controle como {@code perfil}, {@code estadoSolicitacao} ou
 * {@code resultadoGeral}), codigo de regra fora da lista do protocolo, estado fora da lista, texto grande
 * demais, JSON invalido ou duplicado invalida a saida inteira. Assim, mesmo que um documento "convenca" o
 * modelo a devolver instrucoes ou campos extras, isso nunca vira regra, perfil ou estado de solicitacao.
 *
 * <p>A mensagem de rejeicao nunca reproduz o conteudo recebido.
 */
public final class ValidadorDeSaidaEstruturada {

    /** Estados de resultado por regra (CLAUDE.md secao 11). */
    public static final Set<String> ESTADOS_PADRAO = Set.of("ATENDE", "PENDENTE", "NAO_VERIFICAVEL", "NAO_APLICAVEL");

    private static final Set<String> CAMPOS_DO_ITEM = Set.of("codigoRegra", "estado", "texto");
    private static final int MAXIMO_ITENS = 50;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /** Motivos de rejeicao (lista fechada, sem eco do conteudo). */
    public enum Motivo {
        JSON_INVALIDO, ESTRUTURA_INVALIDA, CAMPO_DESCONHECIDO, CODIGO_DE_REGRA_DESCONHECIDO, ESTADO_INVALIDO,
        TEXTO_INVALIDO, ITENS_DEMAIS
    }

    /**
     * Item aceito da saida.
     *
     * @param codigoRegra codigo de uma regra do protocolo vigente
     * @param estado estado do resultado da regra
     * @param texto explicacao ou valor citado; e dado, nunca instrucao
     */
    public record Item(String codigoRegra, String estado, String texto) {
    }

    /** Resultado da validacao. */
    public sealed interface Resultado {
        /**
         * Saida aceita.
         *
         * @param itens itens validados
         */
        record Valida(List<Item> itens) implements Resultado {
        }

        /**
         * Saida rejeitada.
         *
         * @param motivo motivo da rejeicao
         */
        record Invalida(Motivo motivo) implements Resultado {
        }
    }

    private final Set<String> codigosDeRegra;
    private final Set<String> estados;
    private final int tamanhoMaximoTexto;

    /**
     * @param codigosDeRegra codigos de regra do protocolo vigente (a unica fonte de codigos validos)
     * @param estados estados aceitos; use {@link #ESTADOS_PADRAO}
     * @param tamanhoMaximoTexto limite de caracteres do campo {@code texto}
     */
    public ValidadorDeSaidaEstruturada(Set<String> codigosDeRegra, Set<String> estados, int tamanhoMaximoTexto) {
        this.codigosDeRegra = Set.copyOf(codigosDeRegra);
        this.estados = Set.copyOf(estados);
        this.tamanhoMaximoTexto = tamanhoMaximoTexto;
    }

    /**
     * Valida uma saida.
     *
     * @param json saida do adaptador, em JSON
     * @return {@link Resultado.Valida} com os itens, ou {@link Resultado.Invalida} com o motivo
     */
    public Resultado validar(String json) {
        JsonNode raiz;
        try {
            if (json == null || json.isBlank()) {
                return new Resultado.Invalida(Motivo.JSON_INVALIDO);
            }
            try (JsonParser parser = MAPPER.createParser(json)) {
                raiz = MAPPER.readTree(parser);
            }
        } catch (java.io.IOException | RuntimeException e) {
            return new Resultado.Invalida(Motivo.JSON_INVALIDO);
        }
        if (raiz == null || !raiz.isObject() || raiz.size() != 1 || !raiz.has("itens") || !raiz.get("itens").isArray()) {
            return new Resultado.Invalida(raiz != null && raiz.isObject() && raiz.size() > 1 && raiz.has("itens")
                    ? Motivo.CAMPO_DESCONHECIDO : Motivo.ESTRUTURA_INVALIDA);
        }
        JsonNode itens = raiz.get("itens");
        if (itens.size() > MAXIMO_ITENS) {
            return new Resultado.Invalida(Motivo.ITENS_DEMAIS);
        }
        List<Item> aceitos = new ArrayList<>();
        for (JsonNode item : itens) {
            Motivo problema = conferirItem(item);
            if (problema != null) {
                return new Resultado.Invalida(problema);
            }
            aceitos.add(new Item(item.get("codigoRegra").asText(), item.get("estado").asText(),
                    item.get("texto").asText()));
        }
        return new Resultado.Valida(List.copyOf(aceitos));
    }

    private Motivo conferirItem(JsonNode item) {
        if (!item.isObject()) {
            return Motivo.ESTRUTURA_INVALIDA;
        }
        var nomes = item.fieldNames();
        while (nomes.hasNext()) {
            if (!CAMPOS_DO_ITEM.contains(nomes.next())) {
                return Motivo.CAMPO_DESCONHECIDO;
            }
        }
        for (String campo : CAMPOS_DO_ITEM) {
            if (!item.has(campo) || !item.get(campo).isTextual()) {
                return Motivo.ESTRUTURA_INVALIDA;
            }
        }
        if (!codigosDeRegra.contains(item.get("codigoRegra").asText())) {
            return Motivo.CODIGO_DE_REGRA_DESCONHECIDO;
        }
        if (!estados.contains(item.get("estado").asText())) {
            return Motivo.ESTADO_INVALIDO;
        }
        String texto = item.get("texto").asText();
        if (texto.length() > tamanhoMaximoTexto) {
            return Motivo.TEXTO_INVALIDO;
        }
        return null;
    }
}
