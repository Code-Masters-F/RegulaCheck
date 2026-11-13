package br.com.regulacheck.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.common.log.RedatorDeLog;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class RedatorDeLogTest {

    @Test
    void mascaraSenhaEmPares() {
        String saida = RedatorDeLog.redigir("falha password=Segredo123 e senha: OutraSenha, token=abc.def.ghi");
        assertFalse(saida.contains("Segredo123"));
        assertFalse(saida.contains("OutraSenha"));
        assertFalse(saida.contains("abc.def.ghi"));
        assertTrue(saida.contains("password=***"));
    }

    @Test
    void mascaraBearerEUsuarioSenhaDeUrlJdbc() {
        String saida = RedatorDeLog.redigir("Authorization Bearer abcdef0123456789 e jdbc:oracle:thin:"
                + "usuario/SenhaSecreta@host:1521:orcl");
        assertFalse(saida.contains("abcdef0123456789"));
        assertFalse(saida.contains("SenhaSecreta"));
        assertFalse(saida.contains("usuario/"));
        assertTrue(saida.contains("host:1521:orcl"), "o restante da mensagem permanece util");
    }

    @Test
    void mascaraCredencialEmUrlComUsuarioESenha() {
        String saida = RedatorDeLog.redigir("conectando em https://pessoa:minhaSenha@exemplo.org/api");
        assertFalse(saida.contains("minhaSenha"));
        assertFalse(saida.contains("pessoa:"));
    }

    @Test
    void mascaraTokensDeProvedoresConhecidos() {
        String falso = "ghp_" + "A".repeat(30);
        assertFalse(RedatorDeLog.redigir("erro " + falso).contains(falso));
        String github = "github_pat_" + "B".repeat(30);
        assertFalse(RedatorDeLog.redigir(github).contains(github));
    }

    @Test
    void textoSemSegredoNaoMuda() {
        String texto = "Falha ao guardar arquivo na area privada. tipo=NoSuchFileException";
        assertEquals(texto, RedatorDeLog.redigir(texto));
        assertEquals("", RedatorDeLog.redigir((String) null));
        assertNull(RedatorDeLog.redigir((Throwable) null));
    }

    @Test
    void falhaRedigidaPreservaClassePilhaECausaSemSegredo() {
        RuntimeException causa = new IllegalStateException("conexao password=CausaSecreta");
        RuntimeException falha = new RuntimeException("erro jdbc:oracle:thin:" + "u/MinhaSenha@h:1:s", causa);
        Throwable copia = RedatorDeLog.redigir(falha);
        StringWriter pilha = new StringWriter();
        copia.printStackTrace(new PrintWriter(pilha));
        String texto = pilha.toString();
        assertFalse(texto.contains("MinhaSenha"));
        assertFalse(texto.contains("CausaSecreta"));
        assertTrue(texto.contains("java.lang.RuntimeException"));
        assertTrue(texto.contains("java.lang.IllegalStateException"));
        assertTrue(texto.contains("RedatorDeLogTest"), "a pilha original e preservada");
    }
}
