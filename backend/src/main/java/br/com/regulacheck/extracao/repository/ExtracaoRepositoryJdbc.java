package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.extracao.entity.Extracao;
import br.com.regulacheck.extracao.entity.ExtracaoDocumento;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_EXTRACAO}, {@code RC_EXTRACAO_DOCUMENTO} e {@code RC_EXTRACAO_CAMPO} por JDBC, com SQL
 * parametrizado e instantes em UTC.
 *
 * <p>Toda leitura filtra a dona do pedido na propria instrucao (regra de dona da RC-05). A gravacao usa uma
 * transacao curta, sem chamada externa e sem tocar em arquivo: {@code SELECT ... FOR UPDATE} do pedido, conferencia
 * da revisao capturada, {@code COMMIT} so depois de todos os passos e {@code ROLLBACK} explicito em qualquer falha.
 * Os logs trazem apenas identificadores e codigos, nunca valor extraido, trecho, nome de arquivo ou hash.
 */
public final class ExtracaoRepositoryJdbc implements ExtracaoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ExtracaoRepositoryJdbc.class);

    /** Codigo do Oracle para "tabela ou visao inexistente": esquema sem o script 08. */
    private static final int ORA_TABELA_INEXISTENTE = 942;

    private static final String SQL_PEDIDO = """
            SELECT ESTADO, REVISAO_ATUAL FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ?""";
    // So colunas de RC_SOLICITACAO e filtro pela dona: FOR UPDATE nao se aplica a juncao.
    private static final String SQL_BLOQUEAR = SQL_PEDIDO + " FOR UPDATE";
    private static final String SQL_DOCUMENTOS = """
            SELECT ID, TIPO_DOCUMENTAL, TIPO, PAGINAS, CAMINHO_INTERNO
              FROM RC_DOCUMENTO
             WHERE SOLICITACAO_ID = ? AND REVISAO_REMOCAO IS NULL
             ORDER BY ENVIADO_EM, ID""";
    private static final String SQL_MAIS_RECENTE = """
            SELECT e.ID, e.SOLICITACAO_ID, e.REVISAO, e.CRIADA_EM, s.REVISAO_ATUAL
              FROM RC_EXTRACAO e
              JOIN RC_SOLICITACAO s ON s.ID = e.SOLICITACAO_ID
             WHERE s.ID = ? AND s.PACIENTE_ID = ?
             ORDER BY e.REVISAO DESC
             FETCH FIRST 1 ROW ONLY""";
    private static final String SQL_POR_ID = """
            SELECT e.ID, e.SOLICITACAO_ID, e.REVISAO, e.CRIADA_EM, s.REVISAO_ATUAL
              FROM RC_EXTRACAO e
              JOIN RC_SOLICITACAO s ON s.ID = e.SOLICITACAO_ID
             WHERE e.ID = ? AND s.ID = ? AND s.PACIENTE_ID = ?""";
    private static final String SQL_EXISTENTE = """
            SELECT e.ID, e.SOLICITACAO_ID, e.REVISAO, e.CRIADA_EM, s.REVISAO_ATUAL
              FROM RC_EXTRACAO e
              JOIN RC_SOLICITACAO s ON s.ID = e.SOLICITACAO_ID
             WHERE e.SOLICITACAO_ID = ? AND e.REVISAO = ?""";
    private static final String SQL_DOCUMENTOS_LIDOS = """
            SELECT ed.ID, ed.DOCUMENTO_ID, d.TIPO_DOCUMENTAL, ed.SITUACAO
              FROM RC_EXTRACAO_DOCUMENTO ed
              JOIN RC_DOCUMENTO d ON d.ID = ed.DOCUMENTO_ID
             WHERE ed.EXTRACAO_ID = ?
             ORDER BY ed.ID""";
    private static final String SQL_CAMPOS = """
            SELECT c.EXTRACAO_DOCUMENTO_ID, c.CODIGO, c.SITUACAO, c.VALOR, c.PAGINA, c.TRECHO
              FROM RC_EXTRACAO_CAMPO c
              JOIN RC_EXTRACAO_DOCUMENTO ed ON ed.ID = c.EXTRACAO_DOCUMENTO_ID
             WHERE ed.EXTRACAO_ID = ?""";
    private static final String SQL_INSERIR_EXTRACAO = """
            INSERT INTO RC_EXTRACAO (SOLICITACAO_ID, REVISAO, CRIADA_EM) VALUES (?, ?, ?)""";
    private static final String SQL_INSERIR_DOCUMENTO_LIDO = """
            INSERT INTO RC_EXTRACAO_DOCUMENTO (EXTRACAO_ID, DOCUMENTO_ID, SITUACAO) VALUES (?, ?, ?)""";
    private static final String SQL_INSERIR_CAMPO = """
            INSERT INTO RC_EXTRACAO_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO, SITUACAO, VALOR, PAGINA, TRECHO)
            VALUES (?, ?, ?, ?, ?, ?)""";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public ExtracaoRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Optional<ContextoLeitura> contexto(long pacienteId, long solicitacaoId) {
        try (Connection conexao = banco.conexao()) {
            EstadoSolicitacao estado;
            int revisaoAtual;
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_PEDIDO)) {
                consulta.setLong(1, solicitacaoId);
                consulta.setLong(2, pacienteId);
                try (ResultSet linha = consulta.executeQuery()) {
                    if (!linha.next()) {
                        return Optional.empty();
                    }
                    estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                    revisaoAtual = linha.getInt("REVISAO_ATUAL");
                }
            }
            List<DocumentoParaLeitura> documentos = new ArrayList<>();
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_DOCUMENTOS)) {
                consulta.setLong(1, solicitacaoId);
                try (ResultSet linha = consulta.executeQuery()) {
                    while (linha.next()) {
                        documentos.add(new DocumentoParaLeitura(linha.getLong("ID"),
                                TipoDocumental.valueOf(linha.getString("TIPO_DOCUMENTAL")),
                                TipoArquivo.valueOf(linha.getString("TIPO")), linha.getInt("PAGINAS"),
                                caminhoOuNulo(linha.getString("CAMINHO_INTERNO"))));
                    }
                }
            }
            return Optional.of(new ContextoLeitura(estado, revisaoAtual, documentos));
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    @Override
    public Optional<ExtracaoRegistrada> buscarMaisRecente(long pacienteId, long solicitacaoId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_MAIS_RECENTE)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(carregar(conexao, linha)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    @Override
    public Optional<ExtracaoRegistrada> buscarPorId(long pacienteId, long solicitacaoId, long extracaoId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_POR_ID)) {
            consulta.setLong(1, extracaoId);
            consulta.setLong(2, solicitacaoId);
            consulta.setLong(3, pacienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(carregar(conexao, linha)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    @Override
    public ResultadoGravacao gravar(long pacienteId, long solicitacaoId, int revisaoCapturada,
            List<LeituraParaGravar> leituras, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoGravacao resultado = gravarBloqueado(conexao, pacienteId, solicitacaoId, revisaoCapturada,
                        leituras, agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha.
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

    private ResultadoGravacao gravarBloqueado(Connection conexao, long pacienteId, long solicitacaoId,
            int revisaoCapturada, List<LeituraParaGravar> leituras, Instant agora) throws SQLException {
        EstadoSolicitacao estado;
        int revisaoAtual;
        // O bloqueio serializa leituras e alteracoes de documentos do mesmo pedido: depois dele, a revisao lida
        // e a vigente e nenhuma outra leitura da mesma versao esta em curso.
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoGravacao.naoEncontrado();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
            }
        }
        if (estado == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            return ResultadoGravacao.submetido();
        }
        if (revisaoAtual != revisaoCapturada) {
            return ResultadoGravacao.revisaoMudou();
        }
        try (PreparedStatement existente = conexao.prepareStatement(SQL_EXISTENTE)) {
            existente.setLong(1, solicitacaoId);
            existente.setInt(2, revisaoCapturada);
            try (ResultSet linha = existente.executeQuery()) {
                if (linha.next()) {
                    return ResultadoGravacao.jaExistia(carregar(conexao, linha));
                }
            }
        }
        long extracaoId = inserirComChave(conexao, SQL_INSERIR_EXTRACAO, solicitacaoId, revisaoCapturada,
                utc(agora));
        for (LeituraParaGravar leitura : leituras) {
            long leituraDocumentoId = inserirComChave(conexao, SQL_INSERIR_DOCUMENTO_LIDO, extracaoId,
                    leitura.documentoId(), leitura.leitura().situacao().name());
            inserirCampos(conexao, leituraDocumentoId, leitura.leitura().campos());
        }
        try (PreparedStatement criada = conexao.prepareStatement(SQL_EXISTENTE)) {
            criada.setLong(1, solicitacaoId);
            criada.setInt(2, revisaoCapturada);
            try (ResultSet linha = criada.executeQuery()) {
                if (!linha.next()) {
                    throw new IllegalStateException("Leitura gravada nao encontrada.");
                }
                return ResultadoGravacao.gravada(carregar(conexao, linha));
            }
        }
    }

    private static long inserirComChave(Connection conexao, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(sql, new String[] {"ID"})) {
            for (int i = 0; i < parametros.length; i++) {
                insercao.setObject(i + 1, parametros[i]);
            }
            insercao.executeUpdate();
            try (ResultSet chaves = insercao.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new IllegalStateException("Identificador da leitura nao devolvido.");
                }
                return chaves.getLong(1);
            }
        }
    }

    private static void inserirCampos(Connection conexao, long leituraDocumentoId, List<CampoLido> campos)
            throws SQLException {
        if (campos.isEmpty()) {
            return;
        }
        try (PreparedStatement insercao = conexao.prepareStatement(SQL_INSERIR_CAMPO)) {
            for (CampoLido campo : campos) {
                insercao.setLong(1, leituraDocumentoId);
                insercao.setString(2, campo.codigo().name());
                insercao.setString(3, campo.situacao().name());
                insercao.setString(4, campo.valor());
                if (campo.pagina() == null) {
                    insercao.setNull(5, Types.NUMERIC);
                } else {
                    insercao.setInt(5, campo.pagina());
                }
                insercao.setString(6, campo.trecho());
                insercao.addBatch();
            }
            insercao.executeBatch();
        }
    }

    /** Monta a leitura (com documentos e campos) a partir da linha de {@code RC_EXTRACAO} ja posicionada. */
    private static ExtracaoRegistrada carregar(Connection conexao, ResultSet linha) throws SQLException {
        long id = linha.getLong("ID");
        long solicitacaoId = linha.getLong("SOLICITACAO_ID");
        int revisao = linha.getInt("REVISAO");
        Instant criadaEm = linha.getObject("CRIADA_EM", OffsetDateTime.class).toInstant();
        int revisaoAtual = linha.getInt("REVISAO_ATUAL");

        Map<Long, List<CampoLido>> camposPorDocumento = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CAMPOS)) {
            consulta.setLong(1, id);
            try (ResultSet campos = consulta.executeQuery()) {
                while (campos.next()) {
                    Integer pagina = campos.getObject("PAGINA") == null ? null : campos.getInt("PAGINA");
                    camposPorDocumento.computeIfAbsent(campos.getLong("EXTRACAO_DOCUMENTO_ID"),
                            chave -> new ArrayList<>()).add(new CampoLido(
                            CodigoCampo.valueOf(campos.getString("CODIGO")),
                            SituacaoCampo.valueOf(campos.getString("SITUACAO")), campos.getString("VALOR"), pagina,
                            campos.getString("TRECHO")));
                }
            }
        }
        List<ExtracaoDocumento> documentos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_DOCUMENTOS_LIDOS)) {
            consulta.setLong(1, id);
            try (ResultSet lidos = consulta.executeQuery()) {
                while (lidos.next()) {
                    List<CampoLido> campos = new ArrayList<>(
                            camposPorDocumento.getOrDefault(lidos.getLong("ID"), List.of()));
                    campos.sort(Comparator.comparing(CampoLido::codigo));
                    documentos.add(new ExtracaoDocumento(lidos.getLong("DOCUMENTO_ID"),
                            TipoDocumental.valueOf(lidos.getString("TIPO_DOCUMENTAL")),
                            SituacaoLeitura.valueOf(lidos.getString("SITUACAO")), campos));
                }
            }
        }
        return new ExtracaoRegistrada(new Extracao(id, solicitacaoId, revisao, criadaEm, documentos), revisaoAtual);
    }

    /**
     * As linhas da carga sintetica ({@code 02_}) apontam para nomes ficticios que nao tem o formato de um
     * identificador interno: ficam sem arquivo para ler.
     */
    private static IdInterno caminhoOuNulo(String caminho) {
        try {
            return IdInterno.de(caminho);
        } catch (IllegalArgumentException invalido) {
            return null;
        }
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
     * Traduz uma falha. Tabela inexistente (ORA-00942) indica esquema sem o script 08: so a extracao fica
     * indisponivel (503), com a orientacao apenas no log. Violacao de restricao e erro de programacao, porque todo
     * acesso de escrita do pedido passa antes pelo bloqueio da linha. Falha de comunicacao vira 503.
     */
    private static RuntimeException traduzir(Exception falha) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLException sql) {
            if (sql.getErrorCode() == ORA_TABELA_INEXISTENTE) {
                LOG.error("Esquema sem as tabelas de extracao. codigoFornecedor={} "
                        + "(execute database/scripts/08_extracao.sql)", sql.getErrorCode());
            } else {
                LOG.warn("Falha ao acessar extracoes. tipo={} codigoFornecedor={}",
                        sql.getClass().getSimpleName(), sql.getErrorCode());
            }
            return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
        return (RuntimeException) falha;
    }
}
