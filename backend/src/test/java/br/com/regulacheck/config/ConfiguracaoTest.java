package br.com.regulacheck.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifica leitura, padroes e protecao de segredos da configuracao. */
class ConfiguracaoTest {

    @Test
    void semVariaveisValemOsPadroes() {
        Configuracao c = Configuracao.deMapa(Map.of());

        assertEquals(Configuracao.PORTA_PADRAO, c.porta());
        assertEquals(Configuracao.HOST_PADRAO, c.host());
        assertEquals(30, c.ttlSessaoMinutos());
        assertFalse(c.banco().configurado());
    }

    @Test
    void leVariaveisDoBancoESessao() {
        Configuracao c = Configuracao.deMapa(Map.of(
                "DB_URL", "jdbc:oracle:thin:@host:1521:orcl",
                "DB_USERNAME", "usuario-ficticio",
                "DB_PASSWORD", "senha-ficticia",
                "SESSION_TTL_MINUTES", "45"));

        assertTrue(c.banco().configurado());
        assertEquals("jdbc:oracle:thin:@host:1521:orcl", c.banco().url());
        assertEquals(45, c.ttlSessaoMinutos());
    }

    @Test
    void bancoIncompletoNaoContaComoConfigurado() {
        Configuracao c = Configuracao.deMapa(Map.of("DB_URL", "jdbc:oracle:thin:@host:1521:orcl",
                "DB_USERNAME", "u"));

        assertFalse(c.banco().configurado());
    }

    @Test
    void ttlInvalidoImpedeIniciarComMensagemClara() {
        for (String invalido : new String[] {"0", "-5", "1441", "abc", "1.5"}) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Configuracao.deMapa(Map.of("SESSION_TTL_MINUTES", invalido)), invalido);
            assertTrue(erro.getMessage().contains("SESSION_TTL_MINUTES"));
        }
    }

    @Test
    void toStringNaoExpoeSenhaNemUsuario() {
        Configuracao c = Configuracao.deMapa(Map.of(
                "DB_URL", "jdbc:oracle:thin:@host:1521:orcl",
                "DB_USERNAME", "usuario-ficticio",
                "DB_PASSWORD", "senha-ficticia"));

        String texto = c.toString();
        assertFalse(texto.contains("senha-ficticia"));
        assertFalse(texto.contains("usuario-ficticio"));
    }

    @Test
    void areaDeArquivosEUrlPublicaSaoOpcionaisEEscondidasNoToString() {
        Configuracao sem = Configuracao.deMapa(Map.of());
        assertFalse(sem.armazenamentoConfigurado());
        assertEquals(null, sem.armazenamentoRaiz());
        assertEquals(null, sem.urlPublica());

        Configuracao vazia = Configuracao.deMapa(Map.of("DOCUMENT_STORAGE_PATH", "  ", "API_PUBLIC_URL", ""));
        assertFalse(vazia.armazenamentoConfigurado());

        Configuracao com = Configuracao.deMapa(Map.of("DOCUMENT_STORAGE_PATH", " /dados/privado-ficticio ",
                "API_PUBLIC_URL", "https://demo-ficticia.exemplo.org"));
        assertTrue(com.armazenamentoConfigurado());
        assertEquals("/dados/privado-ficticio", com.armazenamentoRaiz());
        assertEquals("https://demo-ficticia.exemplo.org", com.urlPublica());
        String texto = com.toString();
        assertFalse(texto.contains("privado-ficticio"));
        assertFalse(texto.contains("demo-ficticia"));
        assertTrue(texto.contains("armazenamento=configurado"));
    }

    @Test
    void construtorSimplesDeHostEPortaMantemOsPadroesDeSessao() {
        Configuracao c = new Configuracao("127.0.0.1", 0);

        assertEquals(0, c.porta());
        assertEquals(30, c.ttlSessaoMinutos());
        assertFalse(c.banco().configurado());
    }
}
