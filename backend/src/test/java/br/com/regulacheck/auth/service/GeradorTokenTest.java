package br.com.regulacheck.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifica formato, unicidade e hash do token de sessao. */
class GeradorTokenTest {

    private final GeradorToken gerador = new GeradorToken();

    @Test
    void tokenTem43CaracteresBase64UrlSemPreenchimento() {
        String token = gerador.novo();

        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertTrue(gerador.formatoValido(token));
    }

    @Test
    void tokensSaoDistintos() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            assertTrue(vistos.add(gerador.novo()));
        }
    }

    @Test
    void hashEoSha256EmHexadecimalMinusculo() {
        // Vetor de teste conhecido do SHA-256 (FIPS 180-2): "abc".
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", gerador.hash("abc"));
    }

    @Test
    void hashTem64CaracteresEMudaComOToken() {
        String a = gerador.hash(gerador.novo());
        String b = gerador.hash(gerador.novo());

        assertEquals(64, a.length());
        assertTrue(a.matches("[0-9a-f]{64}"));
        assertNotEquals(a, b);
    }

    @Test
    void hashNaoContemOToken() {
        String token = gerador.novo();

        assertFalse(gerador.hash(token).contains(token));
    }

    @Test
    void formatoInvalidoERecusado() {
        assertFalse(gerador.formatoValido(null));
        assertFalse(gerador.formatoValido(""));
        assertFalse(gerador.formatoValido("curto"));
        assertFalse(gerador.formatoValido("a".repeat(44)));
        assertFalse(gerador.formatoValido("!".repeat(43)));
        assertFalse(gerador.formatoValido("a".repeat(42) + " "));
    }
}
