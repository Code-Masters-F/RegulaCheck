package br.com.regulacheck;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Garante a restricao da disciplina: o codigo de producao nao usa Spring nem JPA/Hibernate.
 * (Spring Boot so poderia aparecer em testes, e hoje nem la e usado.)
 */
class ArquiteturaTest {

    private static final List<String> PROIBIDOS = List.of(
            "org.springframework", "jakarta.persistence", "javax.persistence", "org.hibernate");

    @Test
    void codigoDeProducaoNaoUsaSpringNemJpa() throws IOException {
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main/java"))) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String texto = Files.readString(arquivo);
                for (String proibido : PROIBIDOS) {
                    assertTrue(!texto.contains(proibido), arquivo + " referencia " + proibido);
                }
            }
        }
    }

    @Test
    void pomNaoDeclaraDependenciasProibidas() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        for (String proibido : List.of("org.springframework", "hibernate", "jakarta.persistence", "postgresql")) {
            assertTrue(!pom.contains(proibido), "pom.xml referencia " + proibido);
        }
    }
}
