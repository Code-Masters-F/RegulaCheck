package br.com.regulacheck.config;

import br.com.regulacheck.auth.repository.SessaoRepository;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.time.Clock;

/**
 * Pecas externas que a montagem da aplicacao precisa: os repositorios (fronteira com o banco) e o
 * relogio. Em producao vem do Oracle ({@link Servidor#criar(Configuracao)}); nos testes vem de
 * implementacoes em memoria, sem banco.
 *
 * @param usuarios consulta de contas
 * @param sessoes persistencia das sessoes de login
 * @param relogio relogio da aplicacao
 */
public record Dependencias(UsuarioRepository usuarios, SessaoRepository sessoes, Clock relogio) {
}
