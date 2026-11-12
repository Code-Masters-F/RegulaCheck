package br.com.regulacheck;

import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Servidor;
import java.io.IOException;

/**
 * Ponto de entrada do backend do RegulaCheck.
 *
 * <p>Inicia a API sem exigir banco, IA, sensores nem variaveis de ambiente: sem configuracao,
 * escuta em {@code 0.0.0.0:8080}. Execucao: {@code java -jar target/regulacheck-backend.jar}.
 */
public final class RegulaCheckApplication {

    private RegulaCheckApplication() {
    }

    /**
     * @param args nao utilizados; a configuracao vem de variaveis de ambiente
     * @throws InterruptedException se a espera pelo servidor for interrompida
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            Servidor servidor = Servidor.criar(Configuracao.deAmbiente());
            servidor.iniciar();
            servidor.aguardar();
        } catch (IllegalArgumentException | IOException erro) {
            System.err.println("Nao foi possivel iniciar: " + erro.getMessage());
            System.exit(1);
        }
    }
}
