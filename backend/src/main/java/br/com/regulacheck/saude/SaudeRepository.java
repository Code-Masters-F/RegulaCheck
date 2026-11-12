package br.com.regulacheck.saude;

/**
 * Contrato de acesso ao estado tecnico da aplicacao.
 *
 * <p>Existe apenas para demonstrar a separacao Controller, Service e Repository exigida na
 * fundacao; nao representa dado de negocio. As repositories reais (JDBC/Oracle) chegam nas
 * historias seguintes.
 */
public interface SaudeRepository {

    /**
     * @return estado tecnico atual, por exemplo {@code "UP"}
     */
    String estadoTecnico();
}
