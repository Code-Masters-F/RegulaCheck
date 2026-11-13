package br.com.regulacheck.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.seguranca.VarreduraDeSegredos.Achado;
import br.com.regulacheck.seguranca.VarreduraDeSegredos.Resultado;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SC-004: a varredura de segredos acha o que deve achar, nao acusa o que e inofensivo e o repositorio (inclusive
 * o aplicativo movel) esta limpo. Os achados nunca mostram o valor do segredo.
 *
 * <p>Para rodar so esta verificacao: {@code .\mvnw.cmd -Dtest=VarreduraDeSegredosTest test}.
 */
class VarreduraDeSegredosTest {

    @TempDir
    Path pasta;

    private Resultado varrer(Set<String> valoresReais) throws IOException {
        return VarreduraDeSegredos.varrer(pasta, valoresReais);
    }

    private void escrever(String nome, String conteudo) throws IOException {
        Path destino = pasta.resolve(nome);
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
    }

    private static List<String> regras(Resultado r) {
        return r.achados().stream().map(Achado::regra).toList();
    }

    // ---- (a) deteccao: um segredo plantado precisa ser achado ----

    @Test
    void detectaSegredosPlantadosDeCadaTipo() throws IOException {
        escrever("config/app.properties", "db.password=SenhaReal123\n");
        escrever("scripts/run.sh", "export DB_PASSWORD=AbC123xyz789\n");
        escrever("chaves/id.txt", "-----BEGIN " + "PRIVATE KEY-----\nabc\n");
        escrever("notas/token.txt", "usei " + "ghp_" + "A".repeat(30) + " ontem\n");
        escrever("src/Conexao.java", "String u = \"jdbc:oracle:thin:" + "usuario/MinhaSenha1@host:1521:orcl\";\n");
        escrever("docs/exemplo.http", "Authorization: Bearer " + "abcdefghijklmnopqrstuvwxyz0123\n");
        escrever("config/ia.env", "OPENAI_API_KEY=qualquer\n");
        escrever("src/Ia.java", "String k = System.getenv(\"AI_SECRET_VALUE\");\n");

        List<String> regras = regras(varrer(Set.of()));
        for (String esperada : List.of("ATRIBUICAO_DE_SEGREDO_EM_CONFIGURACAO", "ATRIBUICAO_DE_SEGREDO",
                "CHAVE_PRIVADA", "TOKEN_DE_PROVEDOR", "URL_JDBC_COM_CREDENCIAL", "TOKEN_BEARER", "CHAVE_DE_IA")) {
            assertTrue(regras.contains(esperada), "regra nao detectada: " + esperada + " em " + regras);
        }
    }

    @Test
    void detectaValorRealMesmoSemPadraoConhecidoENuncaImprimeOValor() throws IOException {
        String valorReal = "ValorRealQueNaoPodeAparecer-9981";
        escrever("docs/nota.md", "lembrete: a senha e " + valorReal + " ok\n");
        Resultado r = varrer(Set.of(valorReal));
        assertEquals(List.of("VALOR_REAL_DE_SEGREDO"), regras(r));
        assertFalse(r.achados().toString().contains(valorReal), "o valor nao pode aparecer no relatorio");
    }

    // ---- (b) nao acusa o que e inofensivo ----

    @Test
    void naoAcusaPlaceholdersNemProsa() throws IOException {
        escrever(".env.example", "DB_USERNAME=\nDB_PASSWORD=\nIOT_API_KEY=\nDB_URL=jdbc:oracle:thin:@host:1521:orcl\n");
        escrever("docs/ambiente.md", "Defina `DB_PASSWORD` no shell.\nVariáveis AI_* não são necessárias.\n"
                + "| `IOT_API_KEY` | chave do simulador |\nUse `Authorization: Bearer <token>`.\n");
        escrever("scripts/exemplo.sh", "export DB_PASSWORD=<sua-senha>\nexport IOT_API_KEY=changeme\n");
        escrever("src/Teste.java", "String senha = \"qualquer\";\nvar m = Map.of(\"DB_PASSWORD\", \"x\");\n");
        Resultado r = varrer(Set.of());
        assertEquals(List.of(), r.achados(), "falsos positivos: " + r.achados());
    }

    @Test
    void valoresCurtosDemaisNaoViramFalsoPositivo() throws IOException {
        escrever("docs/nota.md", "valor ab aparece aqui\n");
        assertEquals(List.of(), varrer(Set.of("ab")).achados());
    }

    @Test
    void ignoraEnvLocalPastasGeradasEBinarios() throws IOException {
        escrever(".env", "DB_PASSWORD=ValorDoEnvLocal123\n");
        escrever("backend/target/classes/x.properties", "db.password=Gerado123\n");
        escrever(".git/config", "password=NaoVarrer123\n");
        Files.write(pasta.resolve("logo.png"), "password=Binario123".getBytes(StandardCharsets.UTF_8));
        Resultado r = varrer(Set.of());
        assertEquals(List.of(), r.achados());
        assertEquals(0, r.arquivosVarridos());
    }

    // ---- (c) o repositorio real ----

    @Test
    void repositorioEMobileEstaoLimposDeSegredos() throws IOException {
        Path raiz = raizDoRepositorio();
        Set<String> valoresReais = new HashSet<>(VarreduraDeSegredos.valoresDoAmbiente());
        valoresReais.addAll(VarreduraDeSegredos.valoresDoEnvLocal(raiz));

        Resultado r = VarreduraDeSegredos.varrer(raiz, valoresReais);

        assertTrue(r.arquivosVarridos() > 50, "a varredura deveria cobrir o repositorio: " + r.arquivosVarridos());
        assertTrue(r.pastasDeNivelUm().containsAll(Set.of("backend", "docs", "mobile", "specs", "database")),
                "pastas varridas: " + r.pastasDeNivelUm());
        assertEquals(List.of(), r.achados(), "segredos (sem valor) encontrados: " + r.achados());
    }

    @Test
    void envLocalEstaIgnoradoPeloGit() throws IOException {
        Path raiz = raizDoRepositorio();
        List<String> gitignore = Files.readAllLines(raiz.resolve(".gitignore"), StandardCharsets.UTF_8);
        assertTrue(gitignore.stream().anyMatch(l -> l.trim().equals(".env")), ".env deve constar no .gitignore");
        assertTrue(gitignore.stream().anyMatch(l -> l.trim().equals("!.env.example")));
        assumeTrue(gitDisponivel(raiz), "git indisponivel: conferencia por comando ignorada");
        assertEquals(0, executar(raiz, "git", "check-ignore", "-q", ".env"), ".env nao esta ignorado pelo git");
    }

    @Test
    void naoExisteChaveDeIaNemVariavelDeIaNoExemploDeConfiguracao() throws IOException {
        Path raiz = raizDoRepositorio();
        String exemplo = Files.readString(raiz.resolve(".env.example"), StandardCharsets.UTF_8);
        for (String linha : exemplo.split("\\R")) {
            assertFalse(linha.matches("^\\s*(AI_|OPENAI_|ANTHROPIC_)[A-Z0-9_]*\\s*=.*"), "variavel de IA: " + linha);
        }
    }

    private static Path raizDoRepositorio() {
        Path atual = Path.of("").toAbsolutePath();
        while (atual != null && !(Files.isRegularFile(atual.resolve("CLAUDE.md"))
                && Files.isDirectory(atual.resolve("backend")))) {
            atual = atual.getParent();
        }
        assertTrue(atual != null, "raiz do repositorio nao encontrada a partir de " + Path.of("").toAbsolutePath());
        return atual;
    }

    private static boolean gitDisponivel(Path raiz) {
        try {
            return executar(raiz, "git", "rev-parse", "--git-dir") == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static int executar(Path pasta, String... comando) throws IOException {
        Process p = new ProcessBuilder(comando).directory(pasta.toFile()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
