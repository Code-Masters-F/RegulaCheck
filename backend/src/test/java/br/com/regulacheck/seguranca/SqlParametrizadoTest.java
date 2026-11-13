package br.com.regulacheck.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FR-028 / SC-007: toda consulta ao banco usa parametros. Conferencia repetivel por varredura do codigo-fonte de
 * producao (heuristica; nao substitui revisao):
 * <ul>
 *   <li>proibe {@code Statement} simples ({@code createStatement}, {@code executeQuery(texto)});</li>
 *   <li>{@code prepareStatement} so aceita literal, constante, variavel local inicializada apenas com literais e
 *       constantes, ou parametro de metodo;</li>
 *   <li>proibe, em linhas com SQL, concatenar texto com variavel (qualquer identificador que nao seja constante
 *       em MAIUSCULAS), {@code +=}, {@code String.format} e {@code formatted}.</li>
 * </ul>
 * O teste tambem prova que a varredura nao e cega (acha concatenacao plantada).
 */
class SqlParametrizadoTest {

    @TempDir
    Path pasta;

    private static final Pattern STATEMENT_SIMPLES = Pattern.compile(
            "\\bcreateStatement\\s*\\(|\\bStatement\\s+\\w+\\s*=|\\.execute(?:Query|Update)?\\s*\\(\\s*[^)\\s]");
    private static final Pattern PREPARE = Pattern.compile("\\bprepareStatement\\s*\\(\\s*([^,)]*)");
    private static final Pattern LITERAL_OU_CONSTANTE = Pattern.compile("^(\"[^\"]*\"|[A-Z][A-Z0-9_]*)$");
    private static final Pattern IDENTIFICADOR = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    private static final Pattern PALAVRA_SQL = Pattern.compile("\"[^\"]*\\b(SELECT|INSERT|UPDATE|DELETE|WHERE|FROM)\\b");
    private static final Pattern CONCATENA_VARIAVEL = Pattern.compile(
            "\"\\s*\\+\\s*[a-z][A-Za-z0-9_.()]*|(?<![A-Za-z0-9_])[a-z][A-Za-z0-9]*\\s*\\+\\s*\"");
    private static final Pattern FORMATA_TEXTO = Pattern.compile("\\+=|String\\.format\\(|\\.formatted\\(");

    /** @return achados no formato arquivo:linha [regra] */
    static List<String> varrer(Path raiz) throws IOException {
        List<String> problemas = new ArrayList<>();
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
                List<String> linhas = conteudo.lines().toList();
                String nome = raiz.relativize(arquivo).toString().replace('\\', '/');
                for (int i = 0; i < linhas.size(); i++) {
                    String linha = linhas.get(i);
                    String inicio = linha.stripLeading();
                    if (inicio.startsWith("*") || inicio.startsWith("//") || inicio.startsWith("/*")) {
                        continue;
                    }
                    if (STATEMENT_SIMPLES.matcher(linha).find()) {
                        problemas.add(nome + ":" + (i + 1) + " [STATEMENT_SEM_PARAMETROS]");
                    }
                    Matcher prepare = PREPARE.matcher(linha);
                    while (prepare.find()) {
                        if (!argumentoSeguro(prepare.group(1).trim(), conteudo)) {
                            problemas.add(nome + ":" + (i + 1) + " [SQL_MONTADO]");
                        }
                    }
                    if (PALAVRA_SQL.matcher(linha).find()
                            && (CONCATENA_VARIAVEL.matcher(linha).find() || FORMATA_TEXTO.matcher(linha).find())) {
                        problemas.add(nome + ":" + (i + 1) + " [SQL_COM_VARIAVEL_CONCATENADA]");
                    }
                }
            }
        }
        return problemas;
    }

    private static boolean argumentoSeguro(String argumento, String conteudo) {
        if (LITERAL_OU_CONSTANTE.matcher(argumento).matches()) {
            return true;
        }
        if (!IDENTIFICADOR.matcher(argumento).matches()) {
            return false;
        }
        String id = Pattern.quote(argumento);
        Pattern literalLocal = Pattern.compile("String\\s+" + id + "\\s*=\\s*(?:\"[^\"\\n]*\"|\"\"\"[\\s\\S]*?\"\"\"|"
                + "[A-Z][A-Z0-9_]*)(?:\\s*\\+\\s*(?:\"[^\"\\n]*\"|[A-Z][A-Z0-9_]*))*\\s*;");
        Pattern parametro = Pattern.compile("\\(\\s*(?:[^()]*,\\s*)?String\\s+" + id + "\\s*[,)]");
        return literalLocal.matcher(conteudo).find() || parametro.matcher(conteudo).find();
    }

    @Test
    void codigoDeProducaoSoUsaConsultasParametrizadas() throws IOException {
        Path producao = Path.of("src", "main", "java").toAbsolutePath();
        assertTrue(Files.isDirectory(producao), "diretorio de producao nao encontrado: " + producao);
        assertEquals(List.of(), varrer(producao));
    }

    @Test
    void aVarreduraAchaConcatenacaoEStatementPlantados() throws IOException {
        Files.writeString(pasta.resolve("RepositorioRuim.java"), """
                class RepositorioRuim {
                    void a(java.sql.Connection c, String email) throws Exception {
                        c.prepareStatement("SELECT * FROM RC_USUARIO WHERE EMAIL = '" + email + "'");
                        c.prepareStatement(sqlMontado);
                        java.sql.Statement s = c.createStatement();
                        s.executeQuery("SELECT 1 FROM DUAL");
                    }
                    void b(java.sql.Connection c, String nome) throws Exception {
                        String sql = "SELECT * FROM RC_X WHERE NOME = '" + nome + "'";
                        c.prepareStatement(sql);
                    }
                    void c(String coluna) {
                        String sql = String.format("SELECT %s FROM RC_X", coluna);
                    }
                }
                """);
        List<String> problemas = varrer(pasta);
        assertTrue(problemas.stream().anyMatch(p -> p.contains("[SQL_MONTADO]")), problemas.toString());
        assertTrue(problemas.stream().anyMatch(p -> p.contains("[STATEMENT_SEM_PARAMETROS]")), problemas.toString());
        assertTrue(problemas.stream().anyMatch(p -> p.contains("[SQL_COM_VARIAVEL_CONCATENADA]")), problemas.toString());
        assertTrue(problemas.size() >= 6, problemas.toString());
    }

    @Test
    void aVarreduraAceitaLiteralConstanteEParametroComPlaceholders() throws IOException {
        Files.writeString(pasta.resolve("RepositorioBom.java"), """
                class RepositorioBom {
                    private static final String SELECT_BASE = "SELECT ID FROM RC_USUARIO WHERE ";
                    private static final String SQL = "SELECT ID FROM RC_USUARIO WHERE EMAIL = ?";
                    void a(java.sql.Connection c) throws Exception {
                        c.prepareStatement(SQL);
                        c.prepareStatement("SELECT 1 FROM DUAL WHERE ? = ?");
                        String sql = "UPDATE RC_X SET A = ? WHERE ID = ?";
                        c.prepareStatement(sql);
                        buscar(SELECT_BASE + "EMAIL = ?", "x");
                    }
                    void buscar(String texto, Object parametro) throws Exception {
                        java.sql.Connection c = null;
                        c.prepareStatement(texto);
                    }
                }
                """);
        assertEquals(List.of(), varrer(pasta));
    }
}
