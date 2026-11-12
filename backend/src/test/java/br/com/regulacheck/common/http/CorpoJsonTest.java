package br.com.regulacheck.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica a leitura limitada e validada de corpos JSON. */
class CorpoJsonTest {

    record Exemplo(String nome) {
    }

    private static ByteArrayInputStream corpo(String texto) {
        return new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void leJsonValido() {
        Exemplo lido = CorpoJson.ler(corpo("{\"nome\":\"abc\"}"), "application/json", Exemplo.class);

        assertEquals("abc", lido.nome());
    }

    @Test
    void aceitaContentTypeComCharset() {
        Exemplo lido = CorpoJson.ler(corpo("{\"nome\":\"ç\"}"), "application/json; charset=UTF-8", Exemplo.class);

        assertEquals("ç", lido.nome());
    }

    @Test
    void contentTypeDiferenteDevolve415() {
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                () -> CorpoJson.ler(corpo("{}"), "text/plain", Exemplo.class));

        assertEquals(415, erro.status());
        assertEquals("TIPO_NAO_SUPORTADO", erro.codigo());
    }

    @Test
    void semContentTypeDevolve415() {
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                () -> CorpoJson.ler(corpo("{}"), null, Exemplo.class));

        assertEquals(415, erro.status());
    }

    @Test
    void jsonIlegivelEVazioDevolvem400() {
        for (String ruim : new String[] {"", "   ", "{nome:", "[1,2]", "nulo"}) {
            ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                    () -> CorpoJson.ler(corpo(ruim), "application/json", Exemplo.class), ruim);

            assertEquals(400, erro.status(), ruim);
            assertEquals("REQUISICAO_INVALIDA", erro.codigo(), ruim);
        }
    }

    @Test
    void corpoMaiorQueOLimiteDevolve400SemLerTudo() {
        String grande = "{\"nome\":\"" + "a".repeat(CorpoJson.LIMITE_BYTES + 10) + "\"}";

        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                () -> CorpoJson.ler(corpo(grande), "application/json", Exemplo.class));

        assertEquals(400, erro.status());
        assertEquals("REQUISICAO_INVALIDA", erro.codigo());
    }

    @Test
    void mensagemDeErroNaoVazaConteudoDoCorpo() {
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class,
                () -> CorpoJson.ler(corpo("{\"senha\":\"segredo-ficticio\" lixo"), "application/json", Exemplo.class));

        assertEquals(false, erro.getMessage().contains("segredo-ficticio"));
    }
}
