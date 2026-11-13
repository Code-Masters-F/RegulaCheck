package br.com.regulacheck.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Regra de canal seguro: na execucao remota autenticada a API deve ser alcancada por HTTPS.
 *
 * <p>O servidor embutido escuta em HTTP; o TLS e feito por proxy ou tunel da equipe (esta aplicacao nao emite
 * certificado). O que esta classe garante e que a configuracao {@code API_PUBLIC_URL}, o endereco que o app
 * usa, nao aponte para um destino remoto em texto claro: HTTP so e aceito para desenvolvimento local
 * (loopback e o alias do emulador Android). Nao existe verificacao automatica de que o app respeita isso,
 * pois o app ainda nao existe; ver {@code docs/ambiente.md}.
 */
public final class ConfiguracaoDeCanal {

    private static final Set<String> HOSTS_LOCAIS = Set.of("localhost", "127.0.0.1", "[::1]", "10.0.2.2");

    private ConfiguracaoDeCanal() {
    }

    /**
     * Confere a URL publica da configuracao.
     *
     * @param configuracao configuracao de execucao
     * @throws IllegalArgumentException se a URL for invalida, trouxer usuario e senha, ou for remota sem
     *     {@code https}; a mensagem nunca reproduz a URL recebida (ela pode conter credenciais)
     */
    public static void validar(Configuracao configuracao) {
        String texto = configuracao.urlPublica();
        if (texto == null || texto.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = new URI(texto.trim());
        } catch (URISyntaxException e) {
            throw invalida();
        }
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!esquema.equals("http") && !esquema.equals("https")) || uri.getHost() == null) {
            throw invalida();
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "API_PUBLIC_URL nao deve conter usuario e senha; remova as credenciais da URL.");
        }
        if (esquema.equals("http") && !HOSTS_LOCAIS.contains(uri.getHost().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "API_PUBLIC_URL remota deve usar https. HTTP so e aceito em localhost, 127.0.0.1 e 10.0.2.2.");
        }
    }

    /**
     * @param configuracao configuracao de execucao
     * @return {@code true} se a URL publica foi informada e usa {@code https} (habilita HSTS)
     */
    public static boolean ehHttps(Configuracao configuracao) {
        String texto = configuracao.urlPublica();
        return texto != null && texto.trim().toLowerCase(Locale.ROOT).startsWith("https://");
    }

    private static IllegalArgumentException invalida() {
        return new IllegalArgumentException("API_PUBLIC_URL invalida: informe uma URL http(s) completa.");
    }
}
