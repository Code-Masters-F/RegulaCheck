package br.com.regulacheck.saude;

/**
 * Implementacao em memoria de {@link SaudeRepository}.
 *
 * <p>Nao consulta banco nem recurso externo: indica somente que o processo esta de pe. Por isso
 * {@code UP} nao comprova que o banco esta acessivel nem que o MVP esta completo.
 */
public class SaudeRepositoryMemoria implements SaudeRepository {

    @Override
    public String estadoTecnico() {
        return "UP";
    }
}
