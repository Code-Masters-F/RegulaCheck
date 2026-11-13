package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.extracao.dto.ConferenciaRequest;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.repository.DecisaoNova;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Corpo fechado do {@code PUT} da conferencia (D1): a paciente so confirma ou aponta divergencia, nunca digita
 * valor.
 */
class ConferenciaRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String texto) throws Exception {
        return MAPPER.readTree(texto);
    }

    private static ExcecaoNegocio recusa(String texto) throws Exception {
        JsonNode corpo = json(texto);
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> ConferenciaRequest.validar(corpo));
        assertEquals(400, erro.status());
        assertEquals("REQUISICAO_INVALIDA", erro.codigo());
        assertNotNull(erro.detalhes());
        assertFalse(erro.detalhes().isEmpty());
        return erro;
    }

    private static String lista(String... itens) {
        return "{\"decisoes\":[" + String.join(",", itens) + "]}";
    }

    private static String item(String doc, String codigo, String decisao, String motivo) {
        return "{\"documentoId\":\"" + doc + "\",\"codigo\":\"" + codigo + "\",\"decisao\":\"" + decisao + "\""
                + (motivo == null ? "" : ",\"motivo\":\"" + motivo + "\"") + "}";
    }

    @Test
    void aceitaConfirmacaoEDivergenciaComMotivoDaLista() throws Exception {
        List<DecisaoNova> decisoes = ConferenciaRequest.validar(json(lista(
                item("41", "NOME", "CONFIRMADA", null),
                item("41", "DATA_EMISSAO", "DIVERGENTE", "VALOR_DIFERENTE_DO_DOCUMENTO"),
                item("42", "AUTORIZACAO_ASSINADA", "DIVERGENTE", "CAMPO_EXISTE_E_NAO_FOI_LIDO"))));
        assertEquals(3, decisoes.size());
        assertEquals(new DecisaoNova(41, CodigoCampo.NOME, TipoDecisao.CONFIRMADA, null), decisoes.get(0));
        assertEquals(TipoDecisao.DIVERGENTE, decisoes.get(1).tipo());
        assertEquals(MotivoDivergencia.VALOR_DIFERENTE_DO_DOCUMENTO, decisoes.get(1).motivo());
        assertEquals(42, decisoes.get(2).documentoId());
        assertEquals(MotivoDivergencia.CAMPO_EXISTE_E_NAO_FOI_LIDO, decisoes.get(2).motivo());
    }

    @Test
    void aceitaOLimiteDeDezoitoDecisoes() throws Exception {
        String[] itens = new String[18];
        int i = 0;
        for (long doc = 1; doc <= 3; doc++) {
            for (CodigoCampo codigo : CodigoCampo.values()) {
                itens[i++] = item(Long.toString(doc), codigo.name(), "CONFIRMADA", null);
            }
        }
        assertEquals(18, ConferenciaRequest.validar(json(lista(itens))).size());
    }

    @Test
    void recusaListaAusenteVaziaOuAcimaDoLimite() throws Exception {
        recusa("{}");
        recusa("{\"decisoes\":null}");
        recusa("{\"decisoes\":[]}");
        recusa("{\"decisoes\":\"x\"}");
        recusa("{\"decisoes\":{}}");
        recusa("[]");
        // 19 decisoes: 19 documentos diferentes para nao cair na regra de par repetido
        String[] itens = IntStream.rangeClosed(1, 19)
                .mapToObj(n -> item(Integer.toString(n), "NOME", "CONFIRMADA", null)).toArray(String[]::new);
        recusa(lista(itens));
    }

    @Test
    void recusaItemQueNaoEObjeto() throws Exception {
        recusa("{\"decisoes\":[\"NOME\"]}");
        recusa("{\"decisoes\":[null]}");
        recusa("{\"decisoes\":[[]]}");
    }

    @Test
    void recusaQualquerChaveDesconhecidaInclusiveValor() throws Exception {
        for (String chave : List.of("valor", "texto", "observacao", "correcao", "novoValor")) {
            recusa(lista("{\"documentoId\":\"41\",\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\",\"" + chave
                    + "\":\"Fulana Inventada\"}"));
        }
        // chave desconhecida tambem na raiz
        recusa("{\"decisoes\":[" + item("41", "NOME", "CONFIRMADA", null) + "],\"valor\":\"x\"}");
        // proprietario, estado e revisao nao sao lidos: tambem sao recusados, nao ignorados em silencio
        recusa("{\"pacienteId\":\"1\",\"decisoes\":[" + item("41", "NOME", "CONFIRMADA", null) + "]}");
    }

    @Test
    void recusaDecisaoCodigoEMotivoForaDaLista() throws Exception {
        recusa(lista(item("41", "NOME", "CORRIGIDA", null)));
        recusa(lista(item("41", "NOME", "confirmada", null)));
        recusa(lista(item("41", "CPF", "CONFIRMADA", null)));
        recusa(lista(item("41", "nome", "CONFIRMADA", null)));
        recusa(lista(item("41", "NOME", "DIVERGENTE", "TEXTO_LIVRE")));
        recusa(lista("{\"documentoId\":\"41\",\"decisao\":\"CONFIRMADA\"}"));
        recusa(lista("{\"documentoId\":\"41\",\"codigo\":\"NOME\"}"));
        recusa(lista("{\"documentoId\":\"41\",\"codigo\":\"NOME\",\"decisao\":null}"));
        recusa(lista("{\"documentoId\":\"41\",\"codigo\":1,\"decisao\":\"CONFIRMADA\"}"));
    }

    @Test
    void divergenteExigeMotivoEConfirmadaNaoPodeTer() throws Exception {
        recusa(lista(item("41", "NOME", "DIVERGENTE", null)));
        recusa(lista("{\"documentoId\":\"41\",\"codigo\":\"NOME\",\"decisao\":\"DIVERGENTE\",\"motivo\":null}"));
        recusa(lista(item("41", "NOME", "CONFIRMADA", "VALOR_DIFERENTE_DO_DOCUMENTO")));
    }

    @Test
    void recusaParRepetidoNoMesmoCorpo() throws Exception {
        recusa(lista(item("41", "NOME", "CONFIRMADA", null),
                item("41", "NOME", "DIVERGENTE", "VALOR_DIFERENTE_DO_DOCUMENTO")));
        // mesmo codigo em documentos diferentes nao e repeticao
        assertEquals(2, ConferenciaRequest.validar(json(lista(item("41", "NOME", "CONFIRMADA", null),
                item("42", "NOME", "CONFIRMADA", null)))).size());
    }

    @Test
    void recusaDocumentoIdMalformadoOuNaoTextual() throws Exception {
        for (String ruim : List.of("0", "-1", "01", "4 1", "abc", "", "1e3", "9999999999999999999", "41.0")) {
            recusa(lista(item(ruim, "NOME", "CONFIRMADA", null)));
        }
        recusa(lista("{\"documentoId\":41,\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\"}"));
        recusa(lista("{\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\"}"));
    }

    @Test
    void mensagensNuncaReproduzemOConteudoRecebido() throws Exception {
        String segredo = "SEGREDO-INVENTADO-ZZ";
        String[] corpos = {
                lista("{\"documentoId\":\"41\",\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\",\"" + segredo
                        + "\":\"" + segredo + "\"}"),
                lista(item(segredo, "NOME", "CONFIRMADA", null)),
                lista(item("41", segredo, "CONFIRMADA", null)),
                lista(item("41", "NOME", segredo, null)),
                lista(item("41", "NOME", "DIVERGENTE", segredo))
        };
        for (String corpo : corpos) {
            ExcecaoNegocio erro = recusa(corpo);
            String tudo = erro.getMessage() + erro.detalhes().stream()
                    .map(d -> d.campo() + "|" + d.mensagem()).collect(Collectors.joining(";"));
            assertFalse(tudo.contains(segredo), tudo);
        }
    }

    @Test
    void detalheApontaOCampoDoItemSemRepetirValores() throws Exception {
        ExcecaoNegocio erro = recusa(lista(item("41", "NOME", "CONFIRMADA", null),
                item("41", "DATA_EMISSAO", "DIVERGENTE", null)));
        assertTrue(erro.detalhes().get(0).campo().startsWith("decisoes[1]"), erro.detalhes().get(0).campo());
        assertNull(erro.detalhes().stream().filter(d -> d.campo().startsWith("decisoes[0]")).findFirst()
                .orElse(null));
    }
}
