package br.com.regulacheck.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfiguracaoDeCanalTest {

    private static Configuracao com(String urlPublica) {
        Map<String, String> variaveis = new HashMap<>();
        if (urlPublica != null) {
            variaveis.put("API_PUBLIC_URL", urlPublica);
        }
        return Configuracao.deMapa(variaveis);
    }

    @Test
    void ausenteOuVazioPassa() {
        assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com(null)));
        assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com("")));
        assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com("   ")));
    }

    @Test
    void httpsPassaEmQualquerHost() {
        assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com("https://demo.exemplo.org")));
        assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com("https://192.168.0.10:8443/api")));
        assertTrue(ConfiguracaoDeCanal.ehHttps(com("https://demo.exemplo.org")));
        assertFalse(ConfiguracaoDeCanal.ehHttps(com("http://localhost:8080")));
        assertFalse(ConfiguracaoDeCanal.ehHttps(com(null)));
    }

    @Test
    void httpSoEmDesenvolvimentoLocal() {
        for (String local : new String[] {"http://localhost:8080", "http://127.0.0.1:8080", "http://[::1]:8080",
                "http://10.0.2.2:8080", "HTTP://LOCALHOST:8080"}) {
            assertDoesNotThrow(() -> ConfiguracaoDeCanal.validar(com(local)), local);
        }
    }

    @Test
    void httpRemotoEhRecusadoComMensagemAcionavel() {
        for (String remoto : new String[] {"http://exemplo.com", "http://192.168.0.10:8080", "http://demo.exemplo.org/api"}) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> ConfiguracaoDeCanal.validar(com(remoto)), remoto);
            assertTrue(erro.getMessage().contains("https"));
            assertFalse(erro.getMessage().contains(remoto), "a mensagem nao deve repetir a URL");
        }
    }

    @Test
    void urlInvalidaOuComEsquemaEstranhoEhRecusada() {
        for (String ruim : new String[] {"nao e url", "ftp://exemplo.com", "//exemplo.com", "https://", "javascript:alert(1)"}) {
            assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDeCanal.validar(com(ruim)), ruim);
        }
    }

    @Test
    void urlComUsuarioESenhaEhRecusadaSemEcoarACredencial() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ConfiguracaoDeCanal.validar(com("https://pessoa:minhaSenha@exemplo.org")));
        assertFalse(erro.getMessage().contains("minhaSenha"));
        assertFalse(erro.getMessage().contains("pessoa"));
    }

    @Test
    void configuracaoNaoMostraAUrlNoToString() {
        String texto = com("https://pessoa:minhaSenha@exemplo.org").toString();
        assertFalse(texto.contains("minhaSenha"));
        assertEquals(-1, texto.indexOf("exemplo.org"));
    }

    @Test
    void servidorRecusaSubirComUrlPublicaRemotaSemHttps() {
        Configuracao insegura = com("http://exemplo.com");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Servidor.criar(new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, null, null,
                        insegura.urlPublica())));
        assertTrue(erro.getMessage().contains("https"));
    }
}
