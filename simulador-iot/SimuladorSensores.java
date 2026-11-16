import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulador dos dois sensores da estacao de captura do RegulaCheck (RC-21, Epico 7 - IoT simulada).
 *
 * <p>Envia leituras de <strong>luminosidade (lux)</strong> e <strong>distancia (cm)</strong> em JSON por HTTP POST
 * para {@code /sessoes-captura/{id}/leituras}, com a credencial restrita do simulador no cabecalho
 * {@code X-Chave-Simulador}. Nao ha hardware: os valores sao tabelas fixas, <strong>demonstrativas e sem
 * calibracao real</strong>. As leituras nao comprovam que a foto do documento ficou legivel e nao bloqueiam o envio
 * de documentos.
 *
 * <p>Arquivo unico, so com o JDK 21 ({@code java.net.http.HttpClient}), sem Maven, sem Spring e sem dependencias.
 * Execute a partir da raiz do repositorio:
 * <pre>
 *   java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia normal
 * </pre>
 * Argumentos: {@code --sessao <id>} (obrigatorio), {@code --sequencia normal|fora-da-faixa|recuperacao} (padrao
 * {@code normal}), {@code --intervalo-ms <n>} (padrao 1000), {@code --api <url>} (padrao: variavel {@code API_URL}
 * ou {@code http://localhost:8080}) e {@code --seco} (so imprime as leituras, sem conectar).
 *
 * <p>A chave vem <strong>somente</strong> da variavel de ambiente {@code IOT_API_KEY} (nunca por argumento, para nao
 * ficar no historico do terminal) e nunca e impressa. Uma leitura por requisicao; a primeira resposta diferente de
 * 201 ou falha de rede encerra o simulador <strong>sem repetir o envio</strong>.
 *
 * <p>Codigos de saida: 0 todas as leituras aceitas (201); 1 primeira falha de envio; 2 argumentos ou configuracao
 * invalidos.
 */
public final class SimuladorSensores {

    private static final int SAIDA_FALHA_ENVIO = 1;
    private static final int SAIDA_ARGUMENTOS = 2;
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(5);
    private static final Pattern CODIGO_ERRO = Pattern.compile("\"codigo\"\\s*:\\s*\"([A-Z_]+)\"");

    /** Uma leitura do par: luminosidade em lux e distancia em cm. */
    private record Par(double lux, double cm) {
    }

    /** Dentro das faixas demonstrativas (300 a 1000 lux; 20 a 40 cm). */
    private static final List<Par> NORMAL = List.of(new Par(450, 30), new Par(520, 28), new Par(610, 32),
            new Par(480, 26), new Par(550, 35));

    /** Luminosidade abaixo de 300 lux e distancia fora de 20 a 40 cm, alternando longe e perto. */
    private static final List<Par> FORA_DA_FAIXA = List.of(new Par(120, 55), new Par(90, 12), new Par(150, 60),
            new Par(80, 15), new Par(110, 58));

    /** Tres pares fora da faixa seguidos de tres dentro: a orientacao muda para condicao adequada. */
    private static final List<Par> RECUPERACAO = List.of(new Par(100, 60), new Par(180, 50), new Par(250, 45),
            new Par(350, 38), new Par(500, 30), new Par(650, 28));

    private SimuladorSensores() {
    }

    public static void main(String[] args) {
        System.exit(executar(args));
    }

    private static int executar(String[] args) {
        String sessao = null;
        String sequencia = "normal";
        long intervaloMs = 1000;
        String api = System.getenv().getOrDefault("API_URL", "http://localhost:8080");
        boolean seco = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--sessao" -> sessao = args[++i];
                    case "--sequencia" -> sequencia = args[++i];
                    case "--intervalo-ms" -> intervaloMs = Long.parseLong(args[++i]);
                    case "--api" -> api = args[++i];
                    case "--seco" -> seco = true;
                    default -> {
                        return erroDeArgumento("Argumento desconhecido: " + args[i]);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException invalido) {
            return erroDeArgumento("Argumento sem valor ou com valor invalido.");
        }
        if (sessao == null || !sessao.matches("[1-9][0-9]{0,17}")) {
            return erroDeArgumento("Informe --sessao com o id numerico da sessao de captura.");
        }
        if (intervaloMs < 0) {
            return erroDeArgumento("--intervalo-ms nao pode ser negativo.");
        }
        List<Par> pares = switch (sequencia) {
            case "normal" -> NORMAL;
            case "fora-da-faixa" -> FORA_DA_FAIXA;
            case "recuperacao" -> RECUPERACAO;
            default -> null;
        };
        if (pares == null) {
            return erroDeArgumento("--sequencia deve ser normal, fora-da-faixa ou recuperacao.");
        }
        if (seco) {
            for (Par par : pares) {
                System.out.println(linha("LUMINOSIDADE", par.lux(), "LUX"));
                System.out.println(linha("DISTANCIA", par.cm(), "CM"));
            }
            return 0;
        }
        String chave = System.getenv("IOT_API_KEY");
        if (chave == null || chave.isBlank()) {
            return erroDeArgumento("Defina a variavel de ambiente IOT_API_KEY com a chave do simulador.");
        }
        if (!api.startsWith("http://") && !api.startsWith("https://")) {
            return erroDeArgumento("--api deve comecar com http:// ou https://.");
        }
        return enviarTodas(URI.create(api.replaceAll("/+$", "") + "/sessoes-captura/" + sessao + "/leituras"),
                sessao, chave, pares, intervaloMs);
    }

    private static int enviarTodas(URI destino, String sessao, String chave, List<Par> pares, long intervaloMs) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();
        System.out.println("Simulador de sensores (valores demonstrativos, sem calibracao), sessao " + sessao);
        boolean primeira = true;
        for (Par par : pares) {
            for (int sensor = 0; sensor < 2; sensor++) {
                String tipo = sensor == 0 ? "LUMINOSIDADE" : "DISTANCIA";
                double valor = sensor == 0 ? par.lux() : par.cm();
                String unidade = sensor == 0 ? "LUX" : "CM";
                if (!primeira && !esperar(intervaloMs)) {
                    return SAIDA_FALHA_ENVIO;
                }
                primeira = false;
                int status = enviar(cliente, destino, chave, json(sessao, tipo, valor, unidade));
                if (status != 201) {
                    return SAIDA_FALHA_ENVIO;
                }
            }
        }
        System.out.println("Todas as leituras foram aceitas.");
        return 0;
    }

    /** Envia uma leitura; devolve o status HTTP ou -1 em falha de rede. Nunca repete. */
    private static int enviar(HttpClient cliente, URI destino, String chave, String json) {
        HttpRequest requisicao = HttpRequest.newBuilder(destino).timeout(TEMPO_LIMITE)
                .header("Content-Type", "application/json").header("X-Chave-Simulador", chave)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        try {
            HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
            String resumo = resumo(json);
            if (resposta.statusCode() == 201) {
                System.out.println(resumo + " -> 201");
            } else {
                Matcher codigo = CODIGO_ERRO.matcher(resposta.body());
                System.out.println(resumo + " -> " + resposta.statusCode()
                        + (codigo.find() ? " " + codigo.group(1) : "") + ". Envio interrompido, sem repetir.");
            }
            return resposta.statusCode();
        } catch (IOException falha) {
            System.out.println("Falha de rede (" + falha.getClass().getSimpleName() + "). Confira --api e se a API "
                    + "esta no ar. Envio interrompido, sem repetir.");
            return -1;
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static boolean esperar(long intervaloMs) {
        try {
            Thread.sleep(intervaloMs);
            return true;
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** JSON montado a mao (so numeros, enum e instante); medidaEm e o instante real do envio. */
    private static String json(String sessao, String tipo, double valor, String unidade) {
        return String.format(Locale.ROOT,
                "{\"sessaoId\":%s,\"tipo\":\"%s\",\"valor\":%s,\"unidade\":\"%s\",\"medidaEm\":\"%s\"}",
                sessao, tipo, numero(valor), unidade, Instant.now());
    }

    private static String resumo(String json) {
        Matcher tipo = Pattern.compile("\"tipo\":\"([A-Z]+)\"").matcher(json);
        Matcher valor = Pattern.compile("\"valor\":([0-9.]+)").matcher(json);
        Matcher unidade = Pattern.compile("\"unidade\":\"([A-Z]+)\"").matcher(json);
        tipo.find();
        valor.find();
        unidade.find();
        return tipo.group(1) + " " + valor.group(1) + " " + unidade.group(1);
    }

    /** Linha do modo --seco: sem instante, para a saida ser reproduzivel. */
    private static String linha(String tipo, double valor, String unidade) {
        return tipo + " " + numero(valor) + " " + unidade;
    }

    private static String numero(double valor) {
        return valor == Math.rint(valor) ? Long.toString((long) valor) : String.format(Locale.ROOT, "%.3f", valor);
    }

    private static int erroDeArgumento(String mensagem) {
        System.err.println(mensagem);
        System.err.println("Uso: java simulador-iot/SimuladorSensores.java --sessao <id> "
                + "[--sequencia normal|fora-da-faixa|recuperacao] [--intervalo-ms 1000] [--api http://localhost:8080] "
                + "[--seco]  (chave em IOT_API_KEY)");
        return SAIDA_ARGUMENTOS;
    }
}
