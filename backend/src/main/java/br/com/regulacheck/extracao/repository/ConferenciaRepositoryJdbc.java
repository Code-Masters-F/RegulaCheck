package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.extracao.entity.DecisaoConferencia;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.service.SituacaoConferencia;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_CONFERENCIA_CAMPO} por JDBC, com SQL parametrizado e instantes em UTC.
 *
 * <p>Toda leitura filtra a dona do pedido na propria instrucao (regra de dona da RC-05). {@link #registrar} usa uma
 * transacao curta, sem chamada externa: {@code SELECT ... FOR UPDATE} do pedido, conferencia da leitura, do estado
 * e da versao atual <em>sob o bloqueio</em>, validacao de cada decisao contra a situacao do campo, gravacao e
 * {@code COMMIT} so depois de todos os passos (ou {@code ROLLBACK} explicito em qualquer falha). O mesmo bloqueio
 * serializa enviar, remover, substituir, ler e decidir no mesmo pedido: nunca fica gravada decisao para uma leitura
 * que deixou de ser a atual. Os logs trazem apenas identificadores, quantidades e codigos, nunca valor extraido,
 * trecho, nome de arquivo ou hash.
 */
public final class ConferenciaRepositoryJdbc implements ConferenciaRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenciaRepositoryJdbc.class);

    /** Codigo do Oracle para "tabela ou visao inexistente": esquema sem o script 09. */
    private static final int ORA_TABELA_INEXISTENTE = 942;

    // So colunas de RC_SOLICITACAO e filtro pela dona: FOR UPDATE nao se aplica a juncao.
    private static final String SQL_BLOQUEAR = """
            SELECT ESTADO, REVISAO_ATUAL FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE""";
    private static final String SQL_LEITURA = """
            SELECT REVISAO FROM RC_EXTRACAO WHERE ID = ? AND SOLICITACAO_ID = ?""";
    private static final String SQL_CAMPOS_DA_LEITURA = """
            SELECT ed.ID AS LEITURA_DOCUMENTO_ID, ed.DOCUMENTO_ID, c.CODIGO, c.SITUACAO
              FROM RC_EXTRACAO_DOCUMENTO ed
              LEFT JOIN RC_EXTRACAO_CAMPO c ON c.EXTRACAO_DOCUMENTO_ID = ed.ID
             WHERE ed.EXTRACAO_ID = ?""";
    private static final String SQL_DECISOES = """
            SELECT ed.DOCUMENTO_ID, d.CODIGO, d.DECISAO, d.MOTIVO, d.DECIDIDA_EM
              FROM RC_CONFERENCIA_CAMPO d
              JOIN RC_EXTRACAO_DOCUMENTO ed ON ed.ID = d.EXTRACAO_DOCUMENTO_ID
             WHERE ed.EXTRACAO_ID = ?""";
    private static final String SQL_INSERIR = """
            INSERT INTO RC_CONFERENCIA_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO, DECISAO, MOTIVO, DECIDIDA_EM)
            VALUES (?, ?, ?, ?, ?)""";
    private static final String SQL_ATUALIZAR = """
            UPDATE RC_CONFERENCIA_CAMPO SET DECISAO = ?, MOTIVO = ?, DECIDIDA_EM = ?
             WHERE EXTRACAO_DOCUMENTO_ID = ? AND CODIGO = ?""";

    private final BancoDados banco;
    private final ExtracaoRepository extracoes;

    /**
     * @param banco acesso ao Oracle
     * @param extracoes leituras (reaproveitado para montar a leitura com documentos e campos)
     */
    public ConferenciaRepositoryJdbc(BancoDados banco, ExtracaoRepository extracoes) {
        this.banco = banco;
        this.extracoes = extracoes;
    }

    @Override
    public Optional<ConferenciaRegistrada> buscar(long pacienteId, long solicitacaoId, long extracaoId) {
        Optional<ExtracaoRegistrada> leitura = extracoes.buscarPorId(pacienteId, solicitacaoId, extracaoId);
        if (leitura.isEmpty()) {
            return Optional.empty();
        }
        try (Connection conexao = banco.conexao()) {
            return Optional.of(new ConferenciaRegistrada(leitura.get(), decisoesDaLeitura(conexao, extracaoId)));
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    @Override
    public ResultadoRegistroDecisao registrar(long pacienteId, long solicitacaoId, long extracaoId,
            List<DecisaoNova> decisoes, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoRegistroDecisao resultado = registrarBloqueado(conexao, pacienteId, solicitacaoId,
                        extracaoId, decisoes, agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha. As recusas acontecem antes
                // de qualquer escrita, entao o COMMIT nao grava nada nesses casos.
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    private ResultadoRegistroDecisao registrarBloqueado(Connection conexao, long pacienteId, long solicitacaoId,
            long extracaoId, List<DecisaoNova> decisoes, Instant agora) throws SQLException {
        EstadoSolicitacao estado;
        int revisaoAtual;
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoRegistroDecisao.naoEncontrado();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
            }
        }
        int revisaoDaLeitura;
        try (PreparedStatement leitura = conexao.prepareStatement(SQL_LEITURA)) {
            leitura.setLong(1, extracaoId);
            leitura.setLong(2, solicitacaoId);
            try (ResultSet linha = leitura.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoRegistroDecisao.naoEncontrado();
                }
                revisaoDaLeitura = linha.getInt("REVISAO");
            }
        }
        if (estado == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            return ResultadoRegistroDecisao.submetido();
        }
        // A revisao e a situacao dos campos foram lidas depois do bloqueio: nenhuma substituicao ou remocao de
        // documento do mesmo pedido pode ter mudado a versao entre esta conferencia e a gravacao abaixo.
        if (revisaoDaLeitura != revisaoAtual) {
            return ResultadoRegistroDecisao.desatualizada();
        }
        Map<Long, DocumentoLido> documentos = camposDaLeitura(conexao, extracaoId);
        for (DecisaoNova decisao : decisoes) {
            if (!documentos.containsKey(decisao.documentoId())) {
                return ResultadoRegistroDecisao.documentoForaDaLeitura();
            }
        }
        for (DecisaoNova decisao : decisoes) {
            SituacaoCampo situacao = documentos.get(decisao.documentoId()).campos().get(decisao.codigo());
            if (situacao == null
                    || !SituacaoConferencia.aplicavel(situacao, decisao.tipo(), decisao.motivo())) {
                return ResultadoRegistroDecisao.naoAplicavel(decisao.documentoId(), decisao.codigo());
            }
        }
        Map<String, DecisaoConferencia> vigentes = new HashMap<>();
        for (DecisaoConferencia vigente : decisoesDaLeitura(conexao, extracaoId)) {
            vigentes.put(chave(vigente.documentoId(), vigente.codigo()), vigente);
        }
        int gravadas = 0;
        for (DecisaoNova decisao : decisoes) {
            DecisaoConferencia vigente = vigentes.get(chave(decisao.documentoId(), decisao.codigo()));
            if (vigente != null && vigente.tipo() == decisao.tipo() && vigente.motivo() == decisao.motivo()) {
                continue; // repetir a mesma decisao nao muda nada, nem o instante
            }
            long leituraDocumentoId = documentos.get(decisao.documentoId()).leituraDocumentoId();
            if (vigente == null) {
                inserir(conexao, leituraDocumentoId, decisao, agora);
            } else {
                atualizar(conexao, leituraDocumentoId, decisao, agora);
            }
            gravadas++;
        }
        LOG.info("Decisoes de conferencia registradas. solicitacaoId={} extracaoId={} recebidas={} alteradas={}",
                solicitacaoId, extracaoId, decisoes.size(), gravadas);
        return ResultadoRegistroDecisao.registrada();
    }

    /** Documento da leitura com a situacao de cada campo (vazio se o documento nao foi lido). */
    private record DocumentoLido(long leituraDocumentoId, Map<CodigoCampo, SituacaoCampo> campos) {
    }

    private static Map<Long, DocumentoLido> camposDaLeitura(Connection conexao, long extracaoId)
            throws SQLException {
        Map<Long, DocumentoLido> documentos = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CAMPOS_DA_LEITURA)) {
            consulta.setLong(1, extracaoId);
            try (ResultSet linhas = consulta.executeQuery()) {
                while (linhas.next()) {
                    long documentoId = linhas.getLong("DOCUMENTO_ID");
                    DocumentoLido documento = documentos.get(documentoId);
                    if (documento == null) {
                        documento = new DocumentoLido(linhas.getLong("LEITURA_DOCUMENTO_ID"),
                                new EnumMap<>(CodigoCampo.class));
                        documentos.put(documentoId, documento);
                    }
                    String codigo = linhas.getString("CODIGO");
                    if (codigo != null) {
                        documento.campos().put(CodigoCampo.valueOf(codigo),
                                SituacaoCampo.valueOf(linhas.getString("SITUACAO")));
                    }
                }
            }
        }
        return documentos;
    }

    private static List<DecisaoConferencia> decisoesDaLeitura(Connection conexao, long extracaoId)
            throws SQLException {
        List<DecisaoConferencia> decisoes = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_DECISOES)) {
            consulta.setLong(1, extracaoId);
            try (ResultSet linhas = consulta.executeQuery()) {
                while (linhas.next()) {
                    String motivo = linhas.getString("MOTIVO");
                    decisoes.add(new DecisaoConferencia(linhas.getLong("DOCUMENTO_ID"),
                            CodigoCampo.valueOf(linhas.getString("CODIGO")),
                            TipoDecisao.valueOf(linhas.getString("DECISAO")),
                            motivo == null ? null : MotivoDivergencia.valueOf(motivo),
                            linhas.getObject("DECIDIDA_EM", OffsetDateTime.class).toInstant()));
                }
            }
        }
        return decisoes;
    }

    private static void inserir(Connection conexao, long leituraDocumentoId, DecisaoNova decisao, Instant agora)
            throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(SQL_INSERIR)) {
            insercao.setLong(1, leituraDocumentoId);
            insercao.setString(2, decisao.codigo().name());
            insercao.setString(3, decisao.tipo().name());
            insercao.setString(4, decisao.motivo() == null ? null : decisao.motivo().name());
            insercao.setObject(5, utc(agora));
            insercao.executeUpdate();
        }
    }

    private static void atualizar(Connection conexao, long leituraDocumentoId, DecisaoNova decisao, Instant agora)
            throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(SQL_ATUALIZAR)) {
            atualizacao.setString(1, decisao.tipo().name());
            atualizacao.setString(2, decisao.motivo() == null ? null : decisao.motivo().name());
            atualizacao.setObject(3, utc(agora));
            atualizacao.setLong(4, leituraDocumentoId);
            atualizacao.setString(5, decisao.codigo().name());
            if (atualizacao.executeUpdate() != 1) {
                throw new IllegalStateException("Decisao vigente nao encontrada para atualizar.");
            }
        }
    }

    private static String chave(long documentoId, CodigoCampo codigo) {
        return documentoId + "/" + codigo.name();
    }

    private static void desfazer(Connection conexao) {
        try {
            conexao.rollback();
        } catch (SQLException falha) {
            LOG.warn("Falha ao desfazer transacao. tipo={} codigoFornecedor={}",
                    falha.getClass().getSimpleName(), falha.getErrorCode());
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    /**
     * Traduz uma falha. Tabela inexistente (ORA-00942) indica esquema sem o script 09: so a conferencia fica
     * indisponivel (503), com a orientacao apenas no log. Violacao de restricao e erro de programacao, porque toda
     * escrita passa antes pelo bloqueio da linha do pedido e pela validacao da decisao. Falha de comunicacao
     * vira 503.
     */
    private static RuntimeException traduzir(Exception falha) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLException sql) {
            if (sql.getErrorCode() == ORA_TABELA_INEXISTENTE) {
                LOG.error("Esquema sem a tabela de conferencia. codigoFornecedor={} "
                        + "(execute database/scripts/09_conferencia.sql)", sql.getErrorCode());
            } else {
                LOG.warn("Falha ao acessar conferencias. tipo={} codigoFornecedor={}",
                        sql.getClass().getSimpleName(), sql.getErrorCode());
            }
            return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
        return (RuntimeException) falha;
    }
}
