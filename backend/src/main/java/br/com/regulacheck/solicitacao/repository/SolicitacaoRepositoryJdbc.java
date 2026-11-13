package br.com.regulacheck.solicitacao.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_SOLICITACAO} e {@code RC_REVISAO} por JDBC, com SQL parametrizado e instantes em UTC.
 *
 * <p>Toda consulta filtra {@code PACIENTE_ID} na propria instrucao (regra de dona da RC-05). As duas operacoes de
 * escrita usam uma transacao curta, sem chamada externa: {@code COMMIT} so depois de todos os passos e
 * {@code ROLLBACK} explicito em qualquer falha.
 */
public final class SolicitacaoRepositoryJdbc implements SolicitacaoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitacaoRepositoryJdbc.class);

    /** Codigo do Oracle para "registro filho encontrado" (ORA-02292) ao apagar um pai ainda referenciado. */
    private static final int ORA_FILHO_ENCONTRADO = 2292;

    private static final String COLUNAS_PEDIDO = """
            SELECT s.ID, s.PACIENTE_ID, s.ESTADO, s.REVISAO_ATUAL, s.CRIADA_EM, s.ATUALIZADA_EM,
                   p.ID AS PROTOCOLO_ID, p.IDENTIFICADOR, p.VERSAO, p.LOCALIDADE, p.ESPECIALIDADE, p.AVISO_FICTICIO,
                   (SELECT COUNT(*) FROM RC_DOCUMENTO d
                     WHERE d.SOLICITACAO_ID = s.ID AND d.REVISAO_REMOCAO IS NULL) AS DOCUMENTOS
              FROM RC_SOLICITACAO s
              JOIN RC_PROTOCOLO p ON p.ID = s.PROTOCOLO_ID
             WHERE s.PACIENTE_ID = ?""";
    private static final String SQL_BUSCAR = COLUNAS_PEDIDO + " AND s.ID = ?";
    private static final String SQL_LISTAR = COLUNAS_PEDIDO + " ORDER BY s.CRIADA_EM DESC, s.ID DESC "
            + "FETCH FIRST ? ROWS ONLY";

    private static final String SQL_INSERIR_PEDIDO = """
            INSERT INTO RC_SOLICITACAO (PACIENTE_ID, PROTOCOLO_ID, ESTADO, REVISAO_ATUAL, CRIADA_EM, ATUALIZADA_EM)
            VALUES (?, ?, 'RASCUNHO', 1, ?, ?)""";
    private static final String SQL_INSERIR_REVISAO = """
            INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO, CRIADA_EM) VALUES (?, ?, ?, ?)""";
    // So colunas de RC_SOLICITACAO e filtro pela dona: FOR UPDATE nao se aplica a juncao.
    private static final String SQL_BLOQUEAR = """
            SELECT ESTADO, PROTOCOLO_ID, REVISAO_ATUAL, CRIADA_EM, ATUALIZADA_EM
              FROM RC_SOLICITACAO
             WHERE ID = ? AND PACIENTE_ID = ?
               FOR UPDATE""";
    /** Codigo do Oracle para "tabela ou visao inexistente" (esquema sem o script 08). */
    private static final int ORA_TABELA_INEXISTENTE = 942;

    private static final String SQL_CONTAR_DOCUMENTOS = """
            SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ? AND REVISAO_REMOCAO IS NULL""";
    private static final String SQL_CAMINHOS_DOCUMENTOS = """
            SELECT CAMINHO_INTERNO FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?""";
    // RC-10: leituras (simuladas) do pedido; apagadas antes dos documentos que elas referenciam.
    private static final String SQL_APAGAR_CONFERENCIAS = """
            DELETE FROM RC_CONFERENCIA_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN (
                SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID
                 WHERE e.SOLICITACAO_ID = ?)""";
    private static final String SQL_APAGAR_CAMPOS_EXTRACAO = """
            DELETE FROM RC_EXTRACAO_CAMPO WHERE EXTRACAO_DOCUMENTO_ID IN (
                SELECT ed.ID FROM RC_EXTRACAO_DOCUMENTO ed JOIN RC_EXTRACAO e ON e.ID = ed.EXTRACAO_ID
                 WHERE e.SOLICITACAO_ID = ?)""";
    private static final String SQL_APAGAR_DOCUMENTOS_EXTRACAO = """
            DELETE FROM RC_EXTRACAO_DOCUMENTO WHERE EXTRACAO_ID IN (
                SELECT ID FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?)""";
    private static final String SQL_APAGAR_EXTRACOES = "DELETE FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ?";
    private static final String SQL_APAGAR_DOCUMENTOS = "DELETE FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ?";
    private static final String SQL_APAGAR_REVISOES = "DELETE FROM RC_REVISAO WHERE SOLICITACAO_ID = ?";
    private static final String SQL_APAGAR_PEDIDO = "DELETE FROM RC_SOLICITACAO WHERE ID = ?";
    private static final String SQL_TROCAR = """
            UPDATE RC_SOLICITACAO SET PROTOCOLO_ID = ?, REVISAO_ATUAL = ?, ATUALIZADA_EM = ? WHERE ID = ?""";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public SolicitacaoRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Solicitacao criar(long pacienteId, Protocolo protocolo, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_PEDIDO, new String[] {"ID"})) {
                    comando.setLong(1, pacienteId);
                    comando.setLong(2, protocolo.id());
                    comando.setObject(3, utc(agora));
                    comando.setObject(4, utc(agora));
                    comando.executeUpdate();
                    try (ResultSet chaves = comando.getGeneratedKeys()) {
                        if (!chaves.next()) {
                            throw new IllegalStateException("Solicitacao gravada sem identificador gerado.");
                        }
                        id = chaves.getLong(1);
                    }
                }
                inserirRevisao(conexao, id, 1, MotivoRevisao.CRIACAO, agora);
                conexao.commit();
                LOG.info("Rascunho criado. solicitacaoId={} revisao=1", id);
                return new Solicitacao(id, pacienteId, protocolo, EstadoSolicitacao.RASCUNHO, 1, agora, agora);
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha, "criar");
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<Solicitacao> buscarDaPaciente(long id, long pacienteId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_BUSCAR)) {
            consulta.setLong(1, pacienteId);
            consulta.setLong(2, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? Optional.of(ler(resultado)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public List<Solicitacao> listarDaPaciente(long pacienteId, int limite) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_LISTAR)) {
            consulta.setLong(1, pacienteId);
            consulta.setInt(2, limite);
            try (ResultSet resultado = consulta.executeQuery()) {
                List<Solicitacao> pedidos = new ArrayList<>();
                while (resultado.next()) {
                    pedidos.add(ler(resultado));
                }
                return pedidos;
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public ResultadoTroca trocarProtocolo(long id, long pacienteId, Protocolo novoProtocolo, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoTroca resultado = trocarBloqueado(conexao, id, pacienteId, novoProtocolo, agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha.
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha, "trocar");
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private ResultadoTroca trocarBloqueado(Connection conexao, long id, long pacienteId, Protocolo novoProtocolo,
            Instant agora) throws SQLException {
        long protocoloAtual;
        int revisaoAtual;
        EstadoSolicitacao estado;
        Instant criadaEm;
        Instant atualizadaEm;
        // O bloqueio serializa PUTs simultaneos do mesmo pedido: o segundo le o estado e a revisao ja gravados.
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, id);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoTroca.naoEncontrada();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                protocoloAtual = linha.getLong("PROTOCOLO_ID");
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
                criadaEm = linha.getObject("CRIADA_EM", OffsetDateTime.class).toInstant();
                atualizadaEm = linha.getObject("ATUALIZADA_EM", OffsetDateTime.class).toInstant();
            }
        }
        if (estado != EstadoSolicitacao.RASCUNHO) {
            return ResultadoTroca.foraDeRascunho();
        }
        if (protocoloAtual == novoProtocolo.id()) {
            return ResultadoTroca.semMudanca(new Solicitacao(id, pacienteId, novoProtocolo, estado, revisaoAtual,
                    criadaEm, atualizadaEm, contarDocumentos(conexao, id)));
        }
        int novaRevisao = revisaoAtual + 1;
        try (PreparedStatement troca = conexao.prepareStatement(SQL_TROCAR)) {
            troca.setLong(1, novoProtocolo.id());
            troca.setInt(2, novaRevisao);
            troca.setObject(3, utc(agora));
            troca.setLong(4, id);
            troca.executeUpdate();
        }
        inserirRevisao(conexao, id, novaRevisao, MotivoRevisao.PROTOCOLO_ALTERADO, agora);
        LOG.info("Servico do rascunho trocado. solicitacaoId={} revisao={}", id, novaRevisao);
        return ResultadoTroca.alterada(new Solicitacao(id, pacienteId, novoProtocolo, estado, novaRevisao, criadaEm,
                agora, contarDocumentos(conexao, id)));
    }

    @Override
    public ResultadoExclusao excluirRascunho(long id, long pacienteId) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoExclusao resultado = excluirBloqueado(conexao, id, pacienteId);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha.
                conexao.commit();
                return resultado;
            } catch (SQLException falha) {
                desfazer(conexao);
                if (falha.getErrorCode() == ORA_FILHO_ENCONTRADO) {
                    // Algo ainda ligado ao pedido (ex.: tabela de uma historia futura): nada e apagado.
                    LOG.warn("Exclusao de rascunho impedida por dependencia. solicitacaoId={} codigoFornecedor={}",
                            id, falha.getErrorCode());
                    return ResultadoExclusao.comDependencias();
                }
                throw traduzir(falha, "excluir");
            } catch (RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha, "excluir");
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private ResultadoExclusao excluirBloqueado(Connection conexao, long id, long pacienteId) throws SQLException {
        EstadoSolicitacao estado;
        // O bloqueio serializa envio, remocao, substituicao, troca de servico e exclusao do mesmo pedido.
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, id);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoExclusao.naoEncontrado();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
            }
        }
        if (estado == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            return ResultadoExclusao.submetido();
        }
        if (estado != EstadoSolicitacao.RASCUNHO) {
            return ResultadoExclusao.foraDeRascunho();
        }
        List<IdInterno> arquivos = new ArrayList<>();
        // Todos os documentos, disponiveis e removidos: o arquivo dos removidos ja foi apagado (ou sobrou).
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CAMINHOS_DOCUMENTOS)) {
            consulta.setLong(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    IdInterno interno = caminhoOuNulo(resultado.getString("CAMINHO_INTERNO"));
                    if (interno != null) {
                        arquivos.add(interno);
                    }
                }
            }
        }
        // Ordem imposta pelas chaves estrangeiras: decisoes -> leituras -> documentos -> revisoes -> pedido.
        apagarConferencias(conexao, id);
        apagarExtracoes(conexao, id);
        apagarDoPedido(conexao, SQL_APAGAR_DOCUMENTOS, id);
        apagarDoPedido(conexao, SQL_APAGAR_REVISOES, id);
        apagarDoPedido(conexao, SQL_APAGAR_PEDIDO, id);
        LOG.info("Rascunho excluido. solicitacaoId={} arquivos={}", id, arquivos.size());
        return ResultadoExclusao.excluido(arquivos);
    }

    /**
     * Apaga as decisoes de conferencia do pedido (RC-11). Em banco sem o script 09 a tabela nao existe (ORA-00942)
     * e, portanto, nao ha decisao a apagar: a exclusao do rascunho segue funcionando. Fica separada de
     * {@link #apagarExtracoes} para que a falta do 09 nao impeca apagar as leituras.
     */
    private static void apagarConferencias(Connection conexao, long id) throws SQLException {
        try {
            apagarDoPedido(conexao, SQL_APAGAR_CONFERENCIAS, id);
        } catch (SQLException falha) {
            if (falha.getErrorCode() != ORA_TABELA_INEXISTENTE) {
                throw falha;
            }
        }
    }

    /**
     * Apaga as leituras do pedido (RC-10). Em banco sem o script 08 as tabelas nao existem (ORA-00942) e, portanto,
     * nao ha leitura a apagar: a exclusao do rascunho segue funcionando.
     */
    private static void apagarExtracoes(Connection conexao, long id) throws SQLException {
        try {
            apagarDoPedido(conexao, SQL_APAGAR_CAMPOS_EXTRACAO, id);
            apagarDoPedido(conexao, SQL_APAGAR_DOCUMENTOS_EXTRACAO, id);
            apagarDoPedido(conexao, SQL_APAGAR_EXTRACOES, id);
        } catch (SQLException falha) {
            if (falha.getErrorCode() != ORA_TABELA_INEXISTENTE) {
                throw falha;
            }
        }
    }

    private static void apagarDoPedido(Connection conexao, String sql, long id) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            comando.executeUpdate();
        }
    }

    /** Linhas da carga sintetica apontam para nomes ficticios (sem formato de nome interno): sem arquivo real. */
    private static IdInterno caminhoOuNulo(String caminho) {
        try {
            return IdInterno.de(caminho);
        } catch (IllegalArgumentException invalido) {
            return null;
        }
    }

    /** Conta os documentos disponiveis do pedido, na mesma transacao (e sob o mesmo bloqueio) da troca. */
    private static int contarDocumentos(Connection conexao, long solicitacaoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CONTAR_DOCUMENTOS)) {
            consulta.setLong(1, solicitacaoId);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        }
    }

    private static void inserirRevisao(Connection conexao, long solicitacaoId, int numero, MotivoRevisao motivo,
            Instant quando) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_REVISAO)) {
            comando.setLong(1, solicitacaoId);
            comando.setInt(2, numero);
            comando.setString(3, motivo.name());
            comando.setObject(4, utc(quando));
            comando.executeUpdate();
        }
    }

    private static Solicitacao ler(ResultSet resultado) throws SQLException {
        Protocolo protocolo = new Protocolo(resultado.getLong("PROTOCOLO_ID"), resultado.getString("IDENTIFICADOR"),
                resultado.getString("VERSAO"), resultado.getString("LOCALIDADE"),
                resultado.getString("ESPECIALIDADE"), resultado.getString("AVISO_FICTICIO"));
        return new Solicitacao(resultado.getLong("ID"), resultado.getLong("PACIENTE_ID"), protocolo,
                EstadoSolicitacao.valueOf(resultado.getString("ESTADO")), resultado.getInt("REVISAO_ATUAL"),
                resultado.getObject("CRIADA_EM", OffsetDateTime.class).toInstant(),
                resultado.getObject("ATUALIZADA_EM", OffsetDateTime.class).toInstant(),
                resultado.getInt("DOCUMENTOS"));
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
     * Traduz uma falha ocorrida dentro da transacao. Violacao de restricao e erro de programacao ou de esquema
     * desatualizado (ex.: ORA-02290 enquanto o script 06 nao foi executado) e vira erro interno generico; o codigo
     * do fornecedor fica so no log. Falha de comunicacao vira 503.
     */
    private static RuntimeException traduzir(Exception falha, String operacao) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLIntegrityConstraintViolationException integridade) {
            LOG.error("Violacao de integridade ao {} solicitacao. codigoFornecedor={} "
                    + "(2290 em RC_REVISAO_CK_MOTIVO: execute database/scripts/06_motivo_revisao_protocolo.sql)",
                    operacao, integridade.getErrorCode());
            return new IllegalStateException("Nao foi possivel gravar a solicitacao.");
        }
        if (falha instanceof SQLException sql) {
            return indisponivel(sql);
        }
        return (RuntimeException) falha;
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        // Detalhe do driver so no log (sem parametros).
        LOG.warn("Falha ao acessar solicitacoes. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
