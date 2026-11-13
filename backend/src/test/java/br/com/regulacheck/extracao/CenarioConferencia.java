package br.com.regulacheck.extracao;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.documento.CenarioDocumento;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Atalhos dos testes da conferencia (RC-11) sobre o {@link CenarioDocumento}: um pedido pronto com tres
 * documentos lidos pelo leitor simulado (um encaminhamento, uma identificacao borrada e um arquivo desconhecido que
 * vira "leitura vazia") e construtores do corpo do {@code PUT}.
 */
public final class CenarioConferencia {

    /**
     * Pedido preparado.
     *
     * @param pedido id do pedido da paciente A
     * @param extracao id da leitura
     * @param encaminhamento documento de encaminhamento (5 campos identificados e a autorizacao nao localizada)
     * @param borrada identificacao com a data de nascimento ilegivel (2 identificados e 3 nao localizados)
     * @param vazio documento desconhecido do catalogo: leitura vazia, sem campos
     */
    public record Preparo(String pedido, String extracao, String encaminhamento, String borrada, String vazio) {
    }

    private CenarioConferencia() {
    }

    /** Envia os tres documentos ao pedido da paciente A e pede a leitura (201). */
    public static Preparo preparar(CenarioDocumento c) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        String encaminhamento = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        String borrada = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.BORRADA);
        String vazio = c.enviar(c.authA, pedido, "OUTRO", "qualquer.pdf", ArquivosDeTeste.pdf(1), null).json()
                .get("id").asText();
        JsonNode leitura = c.lerDocumentos(c.authA, pedido).json();
        return new Preparo(pedido, leitura.get("id").asText(), encaminhamento, borrada, vazio);
    }

    /** Envia so o encaminhamento e pede a leitura: sem campo ilegivel e sem documento nao lido. */
    public static Preparo prepararSoEncaminhamento(CenarioDocumento c) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        String encaminhamento = CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        JsonNode leitura = c.lerDocumentos(c.authA, pedido).json();
        return new Preparo(pedido, leitura.get("id").asText(), encaminhamento, null, null);
    }

    /** Item do corpo: confirma o campo. */
    public static String confirmar(String documento, String codigo) {
        return "{\"documentoId\":\"" + documento + "\",\"codigo\":\"" + codigo + "\",\"decisao\":\"CONFIRMADA\"}";
    }

    /** Item do corpo: aponta divergencia com o motivo. */
    public static String divergir(String documento, String codigo, String motivo) {
        return "{\"documentoId\":\"" + documento + "\",\"codigo\":\"" + codigo
                + "\",\"decisao\":\"DIVERGENTE\",\"motivo\":\"" + motivo + "\"}";
    }

    /** Corpo do {@code PUT} com os itens dados. */
    public static String lote(String... itens) {
        return "{\"decisoes\":[" + String.join(",", itens) + "]}";
    }

    /** Confirma todos os campos identificados do encaminhamento (5 campos). */
    public static String confirmarEncaminhamento(String documento) {
        return lote(confirmar(documento, "TIPO_DOCUMENTO"), confirmar(documento, "NOME"),
                confirmar(documento, "DATA_NASCIMENTO"), confirmar(documento, "MOTIVO_ENCAMINHAMENTO"),
                confirmar(documento, "DATA_EMISSAO"));
    }

    /** Procura o documento pelo id na resposta da conferencia. */
    public static JsonNode documento(JsonNode corpo, String documentoId) {
        for (JsonNode d : corpo.get("documentos")) {
            if (d.get("documentoId").asText().equals(documentoId)) {
                return d;
            }
        }
        throw new AssertionError("documento " + documentoId + " ausente da conferencia");
    }

    /** Procura o campo pelo codigo no documento da resposta. */
    public static JsonNode campo(JsonNode documento, String codigo) {
        for (JsonNode c : documento.get("campos")) {
            if (c.get("codigo").asText().equals(codigo)) {
                return c;
            }
        }
        throw new AssertionError("campo " + codigo + " ausente");
    }
}
