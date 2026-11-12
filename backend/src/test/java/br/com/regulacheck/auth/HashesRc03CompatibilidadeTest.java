package br.com.regulacheck.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.service.VerificadorSenha;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Prova, sem banco, que a biblioteca de verificacao da API aceita os hashes que a RC-03 carrega em
 * {@code RC_USUARIO}: le os tres hashes do script de carga e confere a senha de demonstracao.
 *
 * <p>A senha abaixo e a senha de demonstracao <strong>ficticia</strong> documentada em
 * {@code database/scripts/README.md}; nao e senha de ninguem.
 */
class HashesRc03CompatibilidadeTest {

    private static final String SENHA_DEMONSTRACAO = "RegulaCheck#Demo2026";

    // Maven roda a partir de backend/ (como o ArquiteturaTest, que le src/main/java).
    private static final Path CARGA = Path.of("../database/scripts/02_carga_sintetica.sql");

    private static List<String> hashesDaCarga() throws IOException {
        String script = Files.readString(CARGA, StandardCharsets.UTF_8);
        Matcher achados = Pattern.compile("\\$2a\\$12\\$[./A-Za-z0-9]{53}").matcher(script);
        List<String> hashes = new ArrayList<>();
        while (achados.find()) {
            hashes.add(achados.group());
        }
        return hashes;
    }

    @Test
    void scriptDeCargaTemTresHashesBcryptCusto12() throws IOException {
        List<String> hashes = hashesDaCarga();

        assertEquals(3, hashes.size());
        assertEquals(3, hashes.stream().distinct().count(), "cada conta tem sal proprio");
        hashes.forEach(h -> assertEquals(60, h.length()));
    }

    @Test
    void senhaDeDemonstracaoConfereComOsTresHashesDaCarga() throws IOException {
        VerificadorSenha verificador = new VerificadorSenha();

        for (String hash : hashesDaCarga()) {
            assertTrue(verificador.verificar(SENHA_DEMONSTRACAO, hash));
        }
    }

    @Test
    void senhaErradaNaoConfereComNenhumHashDaCarga() throws IOException {
        VerificadorSenha verificador = new VerificadorSenha();

        for (String hash : hashesDaCarga()) {
            assertFalse(verificador.verificar("senha-errada", hash));
            assertFalse(verificador.verificar(SENHA_DEMONSTRACAO.toLowerCase(), hash));
        }
    }
}
