package br.com.regulacheck.seguranca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Varredura repetivel de segredos em arquivos de texto (repositorio e aplicativo movel).
 *
 * <p>Regras, todas olhando <strong>atribuicoes e formatos de segredo</strong>, nunca prosa de
 * documentacao: atribuicao de variavel secreta com valor real, chave privada PEM, token de provedor
 * conhecido, URL JDBC com usuario/senha e token {@code Bearer} longo. Alem disso, {@link #varrer(Path, Set)}
 * confere se algum <em>valor real</em> informado (por exemplo, o do ambiente ou do {@code .env} local)
 * aparece em algum arquivo.
 *
 * <p>Os achados trazem apenas arquivo, linha e regra: <strong>o valor nunca e impresso</strong>.
 */
final class VarreduraDeSegredos {

    /** Um achado: onde esta e qual regra pegou (nunca o valor). */
    record Achado(String arquivo, int linha, String regra) {
        @Override
        public String toString() {
            return arquivo + ":" + linha + " [" + regra + "]";
        }
    }

    /** Resultado da varredura. */
    record Resultado(List<Achado> achados, int arquivosVarridos, Set<String> pastasDeNivelUm) {
    }

    private static final Set<String> PASTAS_IGNORADAS = Set.of(".git", "target", "node_modules", ".idea", ".vscode",
            ".gradle", "build", "uploads", "storage", ".claude");
    private static final Set<String> EXTENSOES_BINARIAS = Set.of("jar", "class", "png", "jpg", "jpeg", "gif", "ico",
            "pdf", "zip", "gz", "keystore", "jks", "db", "mp4", "pen", "woff", "woff2", "ttf");
    /** Os proprios arquivos da varredura descrevem os padroes, entao nao sao varridos. */
    private static final Set<String> ARQUIVOS_DA_VARREDURA = Set.of("VarreduraDeSegredos.java",
            "VarreduraDeSegredosTest.java");
    private static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;
    private static final int TAMANHO_MINIMO_VALOR_REAL = 6;

    private static final Pattern ATRIBUICAO = Pattern.compile(
            "^\\s*(?:export\\s+|\\$env:|set\\s+)?[\"']?([A-Za-z0-9_.-]*(?:PASSWORD|PASSWD|SECRET|API_KEY|TOKEN)"
                    + "[A-Za-z0-9_.-]*|DB_USERNAME|IOT_API_KEY)[\"']?\\s*[=:]\\s*(\\S.*?)\\s*$");
    private static final Pattern ATRIBUICAO_MINUSCULA_EM_CONFIG = Pattern.compile(
            "^\\s*[A-Za-z0-9_.-]*(?:password|passwd|senha|secret|api[_-]?key)[A-Za-z0-9_.-]*\\s*[=:]\\s*(\\S.*?)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAVE_PRIVADA = Pattern.compile(
            "-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED |PGP )?PRIVATE KEY(?: BLOCK)?-----");
    private static final Pattern TOKEN_CONHECIDO = Pattern.compile(
            "\\b(?:ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{12,}|sk-[A-Za-z0-9]{20,})");
    private static final Pattern JDBC_COM_CREDENCIAL = Pattern.compile(
            "jdbc:[a-z]+:[a-z]+:[^/@\\s\"'<>]+/[^@\\s\"'<>]+@", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_LONGO = Pattern.compile("\\bBearer\\s+[A-Za-z0-9._-]{24,}");
    private static final Pattern CHAVE_DE_IA = Pattern.compile(
            "^\\s*(?:export\\s+|\\$env:|set\\s+)?[\"']?(?:AI_[A-Z0-9_]+|OPENAI_[A-Z0-9_]+|ANTHROPIC_[A-Z0-9_]+)"
                    + "[\"']?\\s*[=:]");
    private static final Pattern LEITURA_DE_CHAVE_DE_IA = Pattern.compile(
            "(?:getenv|get)\\(\\s*\"(?:AI_|OPENAI_|ANTHROPIC_)");
    private static final Set<String> EXTENSOES_DE_CONFIGURACAO = Set.of("properties", "yml", "yaml", "conf", "ini",
            "sh", "ps1", "cmd", "bat", "env", "xml", "toml");
    private static final List<String> PLACEHOLDERS = List.of("<", "changeme", "exemplo", "ficticia", "ficticio",
            "seu_", "sua_", "xxxx", "***", "...", "${", "{{", "(vazio)", "nao informad", "string", "obrigatori");

    private VarreduraDeSegredos() {
    }

    /**
     * Varre uma arvore de diretorios.
     *
     * @param raiz diretorio a varrer (por exemplo, a raiz do repositorio)
     * @param valoresReais valores literais de segredos reais que nao podem aparecer em nenhum arquivo; podem
     *     ser vazios. Valores curtos demais sao ignorados para nao gerar falso positivo.
     * @return arquivos varridos e achados (sem valores)
     * @throws IOException se a arvore nao puder ser percorrida
     */
    static Resultado varrer(Path raiz, Set<String> valoresReais) throws IOException {
        List<Achado> achados = new ArrayList<>();
        Set<String> pastas = new java.util.TreeSet<>();
        int[] varridos = {0};
        List<String> reais = valoresReais.stream().filter(v -> v != null && v.length() >= TAMANHO_MINIMO_VALOR_REAL)
                .toList();
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            arquivos.filter(Files::isRegularFile).filter(p -> deveVarrer(raiz, p)).forEach(arquivo -> {
                List<String> linhas;
                try {
                    linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
                } catch (IOException | java.io.UncheckedIOException e) {
                    return; // nao e texto UTF-8 legivel: tratado como binario
                }
                varridos[0]++;
                Path relativo = raiz.relativize(arquivo);
                if (relativo.getNameCount() > 1) {
                    pastas.add(relativo.getName(0).toString());
                }
                String nome = relativo.toString().replace('\\', '/');
                String extensao = extensaoDe(arquivo);
                for (int i = 0; i < linhas.size(); i++) {
                    conferirLinha(linhas.get(i), nome, i + 1, extensao, reais, achados);
                }
            });
        }
        return new Resultado(achados, varridos[0], pastas);
    }

    private static void conferirLinha(String linha, String arquivo, int numero, String extensao, List<String> reais,
            List<Achado> achados) {
        // Em codigo Java, variaveis como "tokenA = ..." sao comuns e inofensivas; a regra de atribuicao em
        // MAIUSCULAS e o Bearer literal valem para configuracao, scripts e documentos. Segredo real em Java
        // continua sendo pego pelos padroes de formato e pela comparacao com os valores reais.
        boolean ehJava = extensao.equals("java");
        Matcher atribuicao = ATRIBUICAO.matcher(linha);
        if (!ehJava && atribuicao.find() && !ehPlaceholder(atribuicao.group(2))) {
            achados.add(new Achado(arquivo, numero, "ATRIBUICAO_DE_SEGREDO"));
        } else if (EXTENSOES_DE_CONFIGURACAO.contains(extensao)) {
            Matcher minuscula = ATRIBUICAO_MINUSCULA_EM_CONFIG.matcher(linha);
            if (minuscula.find() && !ehPlaceholder(minuscula.group(1))) {
                achados.add(new Achado(arquivo, numero, "ATRIBUICAO_DE_SEGREDO_EM_CONFIGURACAO"));
            }
        }
        if (CHAVE_PRIVADA.matcher(linha).find()) {
            achados.add(new Achado(arquivo, numero, "CHAVE_PRIVADA"));
        }
        if (TOKEN_CONHECIDO.matcher(linha).find()) {
            achados.add(new Achado(arquivo, numero, "TOKEN_DE_PROVEDOR"));
        }
        if (JDBC_COM_CREDENCIAL.matcher(linha).find()) {
            achados.add(new Achado(arquivo, numero, "URL_JDBC_COM_CREDENCIAL"));
        }
        if (!ehJava && BEARER_LONGO.matcher(linha).find()) {
            achados.add(new Achado(arquivo, numero, "TOKEN_BEARER"));
        }
        if (CHAVE_DE_IA.matcher(linha).find() || LEITURA_DE_CHAVE_DE_IA.matcher(linha).find()) {
            achados.add(new Achado(arquivo, numero, "CHAVE_DE_IA"));
        }
        for (String valor : reais) {
            if (linha.contains(valor)) {
                achados.add(new Achado(arquivo, numero, "VALOR_REAL_DE_SEGREDO"));
                break;
            }
        }
    }

    private static boolean ehPlaceholder(String valor) {
        String v = valor.trim();
        if (v.isEmpty() || v.equals("\"\"") || v.equals("''") || v.equals("`") || v.equals("`,")) {
            return true;
        }
        String minusculo = v.toLowerCase(Locale.ROOT);
        for (String marcador : PLACEHOLDERS) {
            if (minusculo.contains(marcador)) {
                return true;
            }
        }
        return false;
    }

    private static boolean deveVarrer(Path raiz, Path arquivo) {
        Path relativo = raiz.relativize(arquivo);
        for (int i = 0; i < relativo.getNameCount() - 1; i++) {
            if (PASTAS_IGNORADAS.contains(relativo.getName(i).toString())) {
                return false;
            }
        }
        String nome = arquivo.getFileName().toString();
        if (nome.equals(".env") || ARQUIVOS_DA_VARREDURA.contains(nome)) {
            return false;
        }
        if (EXTENSOES_BINARIAS.contains(extensaoDe(arquivo))) {
            return false;
        }
        try {
            return Files.size(arquivo) <= TAMANHO_MAXIMO;
        } catch (IOException e) {
            return false;
        }
    }

    private static String extensaoDe(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Le o {@code .env} local, se existir, e devolve os valores de variaveis que parecem segredo.
     *
     * @param raiz raiz do repositorio
     * @return valores (nunca impressos) para {@link #varrer(Path, Set)}
     */
    static Set<String> valoresDoEnvLocal(Path raiz) {
        Set<String> valores = new java.util.HashSet<>();
        Path env = raiz.resolve(".env");
        if (!Files.isRegularFile(env)) {
            return valores;
        }
        try {
            for (String linha : Files.readAllLines(env, StandardCharsets.UTF_8)) {
                int igual = linha.indexOf('=');
                if (igual <= 0 || linha.trim().startsWith("#")) {
                    continue;
                }
                String nome = linha.substring(0, igual).trim().toUpperCase(Locale.ROOT);
                if (nome.contains("PASSWORD") || nome.contains("SECRET") || nome.contains("TOKEN")
                        || nome.contains("API_KEY") || nome.equals("DB_USERNAME")) {
                    String valor = linha.substring(igual + 1).trim();
                    if (valor.length() >= 2 && (valor.startsWith("\"") && valor.endsWith("\""))) {
                        valor = valor.substring(1, valor.length() - 1);
                    }
                    valores.add(valor);
                }
            }
        } catch (IOException e) {
            // Sem .env legivel nao ha o que comparar.
        }
        return valores;
    }

    /**
     * @return valores de variaveis de ambiente do processo que parecem segredo (nunca impressos)
     */
    static Set<String> valoresDoAmbiente() {
        Set<String> valores = new java.util.HashSet<>();
        System.getenv().forEach((nome, valor) -> {
            String n = nome.toUpperCase(Locale.ROOT);
            if (n.equals("DB_PASSWORD") || n.equals("DB_USERNAME") || n.equals("IOT_API_KEY")
                    || n.equals("CODE_MASTERS_GITHUB_MCP_TOKEN") || n.equals("GITHUB_TOKEN") || n.equals("GH_TOKEN")) {
                valores.add(valor);
            }
        });
        return valores;
    }
}
