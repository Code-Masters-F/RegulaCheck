package br.com.regulacheck.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.TipoSensor;
import br.com.regulacheck.iot.service.FaixasDemonstrativas;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * RC-21, US5 (FR-009 a FR-011, SC-001, SC-004): executa o simulador real ({@code simulador-iot/SimuladorSensores.java})
 * como processo, com o {@code java} desta JVM, contra a API de teste em memoria. Se o arquivo nao for encontrado o
 * teste falha (nao e ignorado).
 */
class SimuladorSensoresProcessoTest {

    private static final Path SIMULADOR = Path.of("..", "simulador-iot", "SimuladorSensores.java");

    @Test
    void modoSecoEReproduzivelEAsSequenciasTemOsValoresEsperados() throws Exception {
        assertTrue(Files.isRegularFile(SIMULADOR), "simulador nao encontrado em " + SIMULADOR.toAbsolutePath());
        for (String sequencia : List.of("normal", "fora-da-faixa", "recuperacao")) {
            Execucao primeira = executar(Map.of(), "--sessao", "1", "--sequencia", sequencia, "--seco");
            Execucao segunda = executar(Map.of(), "--sessao", "1", "--sequencia", sequencia, "--seco");
            assertEquals(0, primeira.saida(), primeira.texto());
            assertEquals(primeira.texto(), segunda.texto(), "saida do --seco deve ser identica");
            List<String[]> linhas = primeira.linhas();
            assertEquals(0, linhas.size() % 2);
            for (int i = 0; i < linhas.size(); i += 2) {
                boolean luzDentro = dentro(TipoSensor.LUMINOSIDADE, linhas.get(i));
                boolean distanciaDentro = dentro(TipoSensor.DISTANCIA, linhas.get(i + 1));
                switch (sequencia) {
                    case "normal" -> assertTrue(luzDentro && distanciaDentro, sequencia + " par " + i / 2);
                    case "fora-da-faixa" -> assertTrue(!luzDentro && !distanciaDentro, sequencia + " par " + i / 2);
                    default -> assertEquals(i / 2 >= linhas.size() / 4, luzDentro && distanciaDentro,
                            sequencia + " par " + i / 2);
                }
            }
        }
    }

    @Test
    void enviaASequenciaNormalParaAApi() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            // O simulador usa o relogio real: alinha o relogio fixo da API para nao recusar a medida como futura.
            c.cenario.relogio.avancar(Duration.between(c.agora(), Instant.now()));
            Execucao envio = executar(Map.of("IOT_API_KEY", CenarioIot.CHAVE), "--sessao", sessao, "--sequencia",
                    "normal", "--intervalo-ms", "0", "--api", "http://127.0.0.1:" + c.api.porta());
            assertEquals(0, envio.saida(), envio.texto());
            List<LeituraSensor> gravadas = c.cenario.capturas.leituras();
            assertEquals(10, gravadas.size());
            assertEquals(TipoSensor.LUMINOSIDADE, gravadas.get(0).tipo());
            assertEquals(TipoSensor.DISTANCIA, gravadas.get(1).tipo());
            assertFalse(envio.texto().contains(CenarioIot.CHAVE), "a saida nao pode mostrar a chave");
        }
    }

    @Test
    void chaveErradaParaNaPrimeiraRecusaSemRepetir() throws Exception {
        try (CenarioIot c = CenarioIot.iniciar()) {
            String sessao = c.sessaoAberta(c.criarPedido(c.authA));
            String chaveErrada = "chave-ficticia-errada-do-simulador";
            Execucao envio = executar(Map.of("IOT_API_KEY", chaveErrada), "--sessao", sessao, "--intervalo-ms", "0",
                    "--api", "http://127.0.0.1:" + c.api.porta());
            assertEquals(1, envio.saida(), envio.texto());
            assertTrue(envio.texto().contains("401"), envio.texto());
            assertEquals(1, envio.texto().lines().filter(l -> l.contains(" -> ")).count(), "uma unica tentativa");
            assertFalse(envio.texto().contains(chaveErrada));
            assertEquals(0, c.cenario.capturas.leituras().size());
        }
    }

    @Test
    void semChaveOuSemSessaoSaiComCodigo2() throws Exception {
        assertEquals(2, executar(Map.of(), "--sessao", "1", "--intervalo-ms", "0").saida());
        assertEquals(2, executar(Map.of("IOT_API_KEY", CenarioIot.CHAVE), "--sequencia", "normal").saida());
        assertEquals(2, executar(Map.of(), "--sessao", "1", "--sequencia", "outra", "--seco").saida());
    }

    private static boolean dentro(TipoSensor tipo, String[] linha) {
        assertEquals(tipo.name(), linha[0]);
        assertEquals(tipo.unidade(), linha[2]);
        BigDecimal valor = new BigDecimal(linha[1]);
        return valor.compareTo(FaixasDemonstrativas.minimo(tipo)) >= 0
                && valor.compareTo(FaixasDemonstrativas.maximo(tipo)) <= 0;
    }

    private record Execucao(int saida, String texto) {
        List<String[]> linhas() {
            return texto.lines().filter(l -> !l.isBlank()).map(l -> l.trim().split(" ")).toList();
        }
    }

    private static Execucao executar(Map<String, String> ambiente, String... argumentos)
            throws IOException, InterruptedException {
        String java = ProcessHandle.current().info().command().orElse("java");
        List<String> comando = new ArrayList<>(List.of(java, SIMULADOR.toString()));
        comando.addAll(List.of(argumentos));
        ProcessBuilder construtor = new ProcessBuilder(comando).redirectErrorStream(true);
        construtor.environment().remove("IOT_API_KEY");
        construtor.environment().remove("API_URL");
        construtor.environment().putAll(ambiente);
        Process processo = construtor.start();
        String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!processo.waitFor(60, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new IllegalStateException("simulador nao terminou em 60 s");
        }
        return new Execucao(processo.exitValue(), saida);
    }
}
