package br.com.regulacheck.config;

import br.com.regulacheck.auth.repository.SessaoRepository;
import br.com.regulacheck.documento.repository.DocumentoRepository;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.time.Clock;

/**
 * Pecas externas que a montagem da aplicacao precisa: os repositorios (fronteira com o banco) e o
 * relogio. Em producao vem do Oracle ({@link Servidor#criar(Configuracao)}); nos testes vem de
 * implementacoes em memoria, sem banco.
 *
 * @param usuarios consulta de contas
 * @param sessoes persistencia das sessoes de login
 * @param protocolos leitura dos servicos (protocolos) disponiveis (RC-07)
 * @param solicitacoes persistencia dos pedidos em rascunho e suas revisoes (RC-07)
 * @param documentos persistencia dos documentos anexados aos pedidos (RC-08)
 * @param relogio relogio da aplicacao
 */
public record Dependencias(UsuarioRepository usuarios, SessaoRepository sessoes, ProtocoloRepository protocolos,
        SolicitacaoRepository solicitacoes, DocumentoRepository documentos, Clock relogio) {
}
