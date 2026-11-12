package br.com.regulacheck.config;

import java.util.Map;

/**
 * Configuracao de execucao lida de variaveis de ambiente.
 *
 * <p>Nenhuma variavel e obrigatoria para iniciar a API: sem valor, valem os padroes. As variaveis
 * do banco ({@code DB_URL}, {@code DB_USERNAME}, {@code DB_PASSWORD}) so sao necessarias para as
 * rotas que usam o banco; sem elas a API sobe normalmente e essas rotas respondem 503. O arquivo
 * {@code .env} nao e lido automaticamente; as variaveis devem ser exportadas no shell ou na IDE.
 *
 * @param host endereco de escuta; {@code 0.0.0.0} permite acesso do emulador e de aparelhos na rede
 * @param porta porta HTTP; {@code 0} escolhe uma porta livre (usado em testes)
 * @param banco dados de conexao com o Oracle; podem estar ausentes
 * @param ttlSessaoMinutos validade da sessao de login, em minutos (1 a 1440)
 */
public record Configuracao(String host, int porta, Banco banco, int ttlSessaoMinutos) {

    /** Porta padrao quando {@code SERVER_PORT} nao e informada. */
    public static final int PORTA_PADRAO = 8080;

    /** Endereco padrao de escuta quando {@code SERVER_HOST} nao e informado. */
    public static final String HOST_PADRAO = "0.0.0.0";

    /** Validade padrao da sessao quando {@code SESSION_TTL_MINUTES} nao e informada. */
    public static final int TTL_SESSAO_PADRAO_MINUTOS = 30;

    private static final int TTL_SESSAO_MAXIMO_MINUTOS = 1440;

    /**
     * Cria uma configuracao sem banco e com a validade padrao da sessao (usada em testes).
     *
     * @param host endereco de escuta
     * @param porta porta HTTP
     */
    public Configuracao(String host, int porta) {
        this(host, porta, Banco.vazio(), TTL_SESSAO_PADRAO_MINUTOS);
    }

    /**
     * Dados de conexao com o banco. O usuario e a senha nunca aparecem em {@link #toString()}.
     *
     * @param url URL JDBC (ex.: {@code jdbc:oracle:thin:@host:1521:orcl})
     * @param usuario usuario do banco
     * @param senha senha do banco
     */
    public record Banco(String url, String usuario, String senha) {

        /** @return dados de banco ausentes */
        public static Banco vazio() {
            return new Banco(null, null, null);
        }

        /** @return {@code true} somente se URL, usuario e senha foram informados */
        public boolean configurado() {
            return naoVazio(url) && naoVazio(usuario) && naoVazio(senha);
        }

        private static boolean naoVazio(String valor) {
            return valor != null && !valor.isBlank();
        }

        /** Oculta credenciais para que nao vazem em logs. */
        @Override
        public String toString() {
            return "Banco[url=" + (naoVazio(url) ? "informada" : "ausente")
                    + ", credenciais=" + (naoVazio(usuario) && naoVazio(senha) ? "informadas" : "ausentes") + "]";
        }
    }

    /**
     * Le a configuracao do ambiente do processo.
     *
     * @return configuracao com os padroes aplicados as variaveis ausentes
     * @throws IllegalArgumentException se {@code SERVER_PORT} ou {@code SESSION_TTL_MINUTES} forem invalidas
     */
    public static Configuracao deAmbiente() {
        return deMapa(System.getenv());
    }

    /**
     * Le a configuracao de um mapa de variaveis (facilita testes sem alterar o ambiente real).
     *
     * @param variaveis mapa nome-valor das variaveis
     * @return configuracao com os padroes aplicados
     * @throws IllegalArgumentException se {@code SERVER_PORT} ou {@code SESSION_TTL_MINUTES} forem invalidas
     */
    public static Configuracao deMapa(Map<String, String> variaveis) {
        String host = valorOuPadrao(variaveis.get("SERVER_HOST"), HOST_PADRAO);
        String portaTexto = variaveis.get("SERVER_PORT");
        int porta = PORTA_PADRAO;
        if (portaTexto != null && !portaTexto.isBlank()) {
            try {
                porta = Integer.parseInt(portaTexto.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SERVER_PORT deve ser um numero inteiro entre 0 e 65535.");
            }
            if (porta < 0 || porta > 65535) {
                throw new IllegalArgumentException("SERVER_PORT deve ser um numero inteiro entre 0 e 65535.");
            }
        }
        Banco banco = new Banco(aparar(variaveis.get("DB_URL")), aparar(variaveis.get("DB_USERNAME")),
                variaveis.get("DB_PASSWORD"));
        return new Configuracao(host, porta, banco, ttlDe(variaveis.get("SESSION_TTL_MINUTES")));
    }

    private static int ttlDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TTL_SESSAO_PADRAO_MINUTOS;
        }
        String mensagem = "SESSION_TTL_MINUTES deve ser um numero inteiro entre 1 e " + TTL_SESSAO_MAXIMO_MINUTOS + ".";
        int minutos;
        try {
            minutos = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(mensagem);
        }
        if (minutos < 1 || minutos > TTL_SESSAO_MAXIMO_MINUTOS) {
            throw new IllegalArgumentException(mensagem);
        }
        return minutos;
    }

    private static String aparar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
