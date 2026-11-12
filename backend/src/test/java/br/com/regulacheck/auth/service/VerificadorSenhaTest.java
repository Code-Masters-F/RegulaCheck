package br.com.regulacheck.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.Test;

/** Verifica a conferencia de senha contra hashes BCrypt, inclusive os casos de borda. */
class VerificadorSenhaTest {

    private static final String SENHA = "Senha#Ficticia-2026";

    private final VerificadorSenha verificador = new VerificadorSenha();

    /** Custo 4 so para o teste ser rapido; o custo 12 real e coberto pelo teste de compatibilidade da RC-03. */
    private static String hash(String senha) {
        return new String(BCrypt.withDefaults().hash(4, senha.toCharArray()));
    }

    @Test
    void senhaCorretaConfere() {
        assertTrue(verificador.verificar(SENHA, hash(SENHA)));
    }

    @Test
    void senhaErradaNaoConfere() {
        assertFalse(verificador.verificar("outra-senha", hash(SENHA)));
    }

    @Test
    void senhaVaziaOuNulaNaoConfere() {
        String hash = hash(SENHA);
        assertFalse(verificador.verificar("", hash));
        assertFalse(verificador.verificar(null, hash));
    }

    @Test
    void senhaComAcentosEEspeciaisConfere() {
        String senha = "Ação#São-José_ß€";

        assertTrue(verificador.verificar(senha, hash(senha)));
        assertFalse(verificador.verificar("Acao#Sao-Jose_ss", hash(senha)));
    }

    @Test
    void hashEmFormatoInvalidoNaoConfereENaoLanca() {
        assertDoesNotThrow(() -> assertFalse(verificador.verificar(SENHA, "isto-nao-e-um-hash")));
        assertFalse(verificador.verificar(SENHA, ""));
        assertFalse(verificador.verificar(SENHA, null));
    }

    @Test
    void senhaAcimaDe72BytesERecusadaSemExcecao() {
        String longa = "a".repeat(73);

        assertDoesNotThrow(() -> assertFalse(verificador.verificar(longa, hash("a".repeat(72)))));
        assertFalse(verificador.excedeLimite("a".repeat(72)));
        assertTrue(verificador.excedeLimite(longa));
    }

    @Test
    void limiteEmBytesConsideraCaracteresMultibyte() {
        // 37 caracteres de 2 bytes = 74 bytes, embora tenha menos de 72 caracteres.
        assertTrue(verificador.excedeLimite("é".repeat(37)));
        assertFalse(verificador.excedeLimite("é".repeat(36)));
    }

    @Test
    void verificacaoContraHashFalsoNuncaConfere() {
        assertFalse(verificador.verificarContraHashFalso(SENHA));
        assertFalse(verificador.verificarContraHashFalso(""));
    }
}
