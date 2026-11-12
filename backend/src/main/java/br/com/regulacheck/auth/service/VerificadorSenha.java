package br.com.regulacheck.auth.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Confere senhas contra hashes BCrypt usando a biblioteca {@code at.favre.lib:bcrypt}.
 *
 * <p>Compativel com os hashes {@code $2a$12$...} (versao 2a, custo 12) carregados pela RC-03. Nunca ha
 * comparacao de texto puro nem hash rapido: a verificacao e feita pela propria biblioteca, que recalcula
 * o hash com o sal e o custo do hash armazenado. Nada aqui registra a senha nem o hash.
 */
public class VerificadorSenha {

    private static final Logger LOG = LoggerFactory.getLogger(VerificadorSenha.class);

    /** Limite do BCrypt: bytes alem do 72o seriam ignorados, entao a API recusa senhas maiores. */
    static final int MAXIMO_BYTES = 72;

    /**
     * Hash BCrypt de custo 12 de um valor aleatorio descartado (ninguem conhece a senha). Serve apenas
     * para gastar o mesmo tempo de uma verificacao real quando o e-mail nao existe, de modo que o tempo
     * de resposta nao revele quais e-mails estao cadastrados.
     */
    private static final String HASH_FALSO = "$2a$12$XjUTS/bV.oNcaKpey7ucDeYL0ipsY1HQ0kVw6Qd9OSVBGpZG9R3JG";

    /**
     * Indica se a senha passa do limite suportado pelo BCrypt (72 bytes em UTF-8).
     *
     * @param senha senha em texto, como enviada pela cliente
     * @return {@code true} se tem mais de 72 bytes; nesse caso ela nunca pode coincidir com um hash da base
     */
    public boolean excedeLimite(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES;
    }

    /**
     * Confere a senha contra o hash armazenado.
     *
     * @param senha senha em texto; {@code null} ou vazia nunca confere
     * @param hash hash BCrypt armazenado; {@code null}, vazio ou em formato invalido nunca confere
     * @return {@code true} somente se a senha corresponde ao hash
     */
    public boolean verificar(String senha, String hash) {
        return conferir(senha, hash);
    }

    private boolean conferir(String senha, String hash) {
        if (senha == null || senha.isEmpty() || hash == null || hash.isEmpty() || excedeLimite(senha)) {
            return false;
        }
        char[] caracteres = senha.toCharArray();
        BCrypt.Result resultado = BCrypt.verifyer().verify(caracteres, hash.toCharArray());
        if (!resultado.validFormat) {
            // Erro de dados, nao de quem tentou entrar: registra sem o hash nem a senha.
            LOG.error("Hash armazenado em formato invalido: {}", resultado.formatErrorMessage);
            return false;
        }
        return resultado.verified;
    }

    /**
     * Gasta o tempo de uma verificacao BCrypt de custo 12 contra um hash que ninguem conhece.
     *
     * @param senha senha enviada; so influencia o trabalho feito, nunca o resultado
     * @return sempre {@code false}
     */
    public boolean verificarContraHashFalso(String senha) {
        // O resultado e descartado de proposito: o objetivo e apenas igualar o tempo gasto.
        conferir(senha == null || senha.isEmpty() ? "x" : senha, HASH_FALSO);
        return false;
    }
}
