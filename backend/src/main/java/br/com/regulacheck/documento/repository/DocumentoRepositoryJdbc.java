package br.com.regulacheck.documento.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
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
 * Persistencia de {@code RC_DOCUMENTO} (com {@code RC_SOLICITACAO} e {@code RC_REVISAO}) por JDBC, com SQL
 * parametrizado e instantes em UTC.
 *
 * <p>Toda leitura filtra a dona do pedido na propria instrucao (regra de dona da RC-05) e amarra o documento ao
 * pedido informado. O registro usa uma transacao curta, sem chamada externa e sem tocar em arquivo:
 * {@code COMMIT} so depois de todos os passos e {@code ROLLBACK} explicito em qualquer falha. Os logs trazem
 * apenas identificadores e codigos, nunca nome de arquivo, hash, chave de envio ou nome interno.
 */
public final class DocumentoRepositoryJdbc implements DocumentoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentoRepositoryJdbc.class);

    /** Codigo do Oracle para "identificador invalido" (coluna inexistente): esquema sem o script 07. */
    private static final int ORA_IDENTIFICADOR_INVALIDO = 904;

    private static final String COLUNAS_DOCUMENTO = """
            SELECT d.ID, d.SOLICITACAO_ID, d.NOME_ORIGINAL, d.TIPO, d.TAMANHO_BYTES, d.PAGINAS, d.REVISAO_ENVIO,
                   d.ENVIADO_EM, d.CAMINHO_INTERNO, d.HASH_CONTEUDO, d.CHAVE_ENVIO, d.TIPO_DOCUMENTAL
              FROM RC_DOCUMENTO d
              JOIN RC_SOLICITACAO s ON s.ID = d.SOLICITACAO_ID
             WHERE s.ID = ? AND s.PACIENTE_ID = ? AND d.REVISAO_REMOCAO IS NULL""";
    private static final String SQL_LISTAR = COLUNAS_DOCUMENTO + " ORDER BY d.ENVIADO_EM, d.ID";
    private static final String SQL_BUSCAR = COLUNAS_DOCUMENTO + " AND d.ID = ?";
    private static final String SQL_POR_CHAVE = COLUNAS_DOCUMENTO + " AND d.CHAVE_ENVIO = ?";

    // So colunas de RC_SOLICITACAO e filtro pela dona: FOR UPDATE nao se aplica a juncao.
    private static final String SQL_BLOQUEAR = """
            SELECT ESTADO, REVISAO_ATUAL
              FROM RC_SOLICITACAO
             WHERE ID = ? AND PACIENTE_ID = ?
               FOR UPDATE""";
    private static final String SQL_CONTAR = """
            SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ? AND REVISAO_REMOCAO IS NULL""";
    private static final String SQL_ATUALIZAR_PEDIDO = """
            UPDATE RC_SOLICITACAO SET REVISAO_ATUAL = ?, ATUALIZADA_EM = ? WHERE ID = ?""";
    private static final String SQL_INSERIR_REVISAO = """
            INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO, CRIADA_EM) VALUES (?, ?, ?, ?)""";
    private static final String SQL_INSERIR_DOCUMENTO = """
            INSERT INTO RC_DOCUMENTO (SOLICITACAO_ID, REVISAO_ENVIO, NOME_ORIGINAL, TIPO, TAMANHO_BYTES, PAGINAS,
                                      CAMINHO_INTERNO, ENVIADO_EM, TIPO_DOCUMENTAL, CHAVE_ENVIO, HASH_CONTEUDO)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public DocumentoRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public ResultadoRegistro registrar(long pacienteId, long solicitacaoId, NovoDocumento novo, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoRegistro resultado = registrarBloqueado(conexao, pacienteId, solicitacaoId, novo, agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha.
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private ResultadoRegistro registrarBloqueado(Connection conexao, long pacienteId, long solicitacaoId,
            NovoDocumento novo, Instant agora) throws SQLException {
        EstadoSolicitacao estado;
        int revisaoAtual;
        // O bloqueio serializa envios (e trocas de servico) simultaneos do mesmo pedido: o seguinte le a
        // contagem, a chave e a revisao ja gravadas pelo anterior.
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoRegistro.naoEncontrado();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
            }
        }
        if (estado != EstadoSolicitacao.RASCUNHO) {
            return ResultadoRegistro.foraDeRascunho();
        }
        int disponiveis = contar(conexao, solicitacaoId);
        if (novo.chaveEnvio() != null) {
            Optional<Documento> existente = buscarPorChave(conexao, solicitacaoId, pacienteId, novo.chaveEnvio());
            if (existente.isPresent()) {
                // Mesma tentativa so se for o mesmo conteudo e o mesmo tipo documental.
                boolean mesmaTentativa = novo.hashConteudo().equals(existente.get().hashConteudo())
                        && novo.tipoDocumental() == existente.get().tipoDocumental();
                return mesmaTentativa ? ResultadoRegistro.repetido(existente.get(), revisaoAtual, disponiveis)
                        : ResultadoRegistro.conflitoChave();
            }
        }
        if (disponiveis >= LimitesArquivo.MAXIMO_ARQUIVOS) {
            return ResultadoRegistro.limite();
        }
        int novaRevisao = revisaoAtual + 1;
        try (PreparedStatement atualizar = conexao.prepareStatement(SQL_ATUALIZAR_PEDIDO)) {
            atualizar.setInt(1, novaRevisao);
            atualizar.setObject(2, utc(agora));
            atualizar.setLong(3, solicitacaoId);
            atualizar.executeUpdate();
        }
        try (PreparedStatement revisao = conexao.prepareStatement(SQL_INSERIR_REVISAO)) {
            revisao.setLong(1, solicitacaoId);
            revisao.setInt(2, novaRevisao);
            revisao.setString(3, MotivoRevisao.DOCUMENTO_ENVIADO.name());
            revisao.setObject(4, utc(agora));
            revisao.executeUpdate();
        }
        long documentoId = inserirDocumento(conexao, solicitacaoId, novaRevisao, novo, agora);
        LOG.info("Documento registrado. solicitacaoId={} documentoId={} revisao={} formato={}", solicitacaoId,
                documentoId, novaRevisao, novo.formato());
        Documento documento = new Documento(documentoId, solicitacaoId, novo.nomeArquivo(), novo.tipoDocumental(),
                novo.formato(), novo.tamanhoBytes(), novo.paginas(), novaRevisao, agora, novo.caminhoInterno(),
                novo.hashConteudo(), novo.chaveEnvio());
        return ResultadoRegistro.registrado(documento, novaRevisao, disponiveis + 1);
    }

    private static long inserirDocumento(Connection conexao, long solicitacaoId, int revisao, NovoDocumento novo,
            Instant agora) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_DOCUMENTO, new String[] {"ID"})) {
            comando.setLong(1, solicitacaoId);
            comando.setInt(2, revisao);
            comando.setString(3, novo.nomeArquivo());
            comando.setString(4, novo.formato().name());
            comando.setLong(5, novo.tamanhoBytes());
            comando.setInt(6, novo.paginas());
            comando.setString(7, novo.caminhoInterno().valor());
            comando.setObject(8, utc(agora));
            comando.setString(9, novo.tipoDocumental().name());
            comando.setString(10, novo.chaveEnvio());
            comando.setString(11, novo.hashConteudo());
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new IllegalStateException("Documento gravado sem identificador gerado.");
                }
                return chaves.getLong(1);
            }
        }
    }

    private static int contar(Connection conexao, long solicitacaoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CONTAR)) {
            consulta.setLong(1, solicitacaoId);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        }
    }

    @Override
    public List<Documento> listarDoPedido(long solicitacaoId, long pacienteId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_LISTAR)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            try (ResultSet resultado = consulta.executeQuery()) {
                List<Documento> documentos = new ArrayList<>();
                while (resultado.next()) {
                    documentos.add(ler(resultado));
                }
                return documentos;
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<Documento> buscarDoPedido(long documentoId, long solicitacaoId, long pacienteId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_BUSCAR)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            consulta.setLong(3, documentoId);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? Optional.of(ler(resultado)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<Documento> buscarPorChave(long solicitacaoId, long pacienteId, String chaveEnvio) {
        try (Connection conexao = banco.conexao()) {
            return buscarPorChave(conexao, solicitacaoId, pacienteId, chaveEnvio);
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private static Optional<Documento> buscarPorChave(Connection conexao, long solicitacaoId, long pacienteId,
            String chaveEnvio) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_POR_CHAVE)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            consulta.setString(3, chaveEnvio);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? Optional.of(ler(resultado)) : Optional.empty();
            }
        }
    }

    private static Documento ler(ResultSet resultado) throws SQLException {
        return new Documento(resultado.getLong("ID"), resultado.getLong("SOLICITACAO_ID"),
                resultado.getString("NOME_ORIGINAL"), TipoDocumental.valueOf(resultado.getString("TIPO_DOCUMENTAL")),
                TipoArquivo.valueOf(resultado.getString("TIPO")), resultado.getLong("TAMANHO_BYTES"),
                resultado.getInt("PAGINAS"), resultado.getInt("REVISAO_ENVIO"),
                resultado.getObject("ENVIADO_EM", OffsetDateTime.class).toInstant(),
                caminhoOuNulo(resultado.getString("CAMINHO_INTERNO")), resultado.getString("HASH_CONTEUDO"),
                resultado.getString("CHAVE_ENVIO"));
    }

    /**
     * As linhas da carga sintetica ({@code 02_}) apontam para nomes ficticios (por exemplo {@code sim/doc-0001.dat}),
     * que nao tem o formato de um identificador interno. Elas continuam listaveis, mas sem arquivo para abrir.
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
     * Traduz uma falha ocorrida dentro da transacao. Coluna inexistente (ORA-00904) indica esquema sem o script
     * 07; violacao de restricao e erro de programacao (a unicidade da chave nao pode disparar aqui, porque todo
     * envio do pedido passa antes pelo bloqueio da linha). Ambos viram erro interno generico, com a orientacao so
     * no log. Falha de comunicacao vira 503.
     */
    private static RuntimeException traduzir(Exception falha) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLException sql && sql.getErrorCode() == ORA_IDENTIFICADOR_INVALIDO) {
            LOG.error("Esquema sem as colunas de documento. codigoFornecedor={} "
                    + "(execute database/scripts/07_documento_tipo_e_tentativa.sql)", sql.getErrorCode());
            return new IllegalStateException("Nao foi possivel gravar o documento.");
        }
        if (falha instanceof SQLIntegrityConstraintViolationException integridade) {
            LOG.error("Violacao de integridade ao registrar documento. codigoFornecedor={}",
                    integridade.getErrorCode());
            return new IllegalStateException("Nao foi possivel gravar o documento.");
        }
        if (falha instanceof SQLException sql) {
            return indisponivel(sql);
        }
        return (RuntimeException) falha;
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        // Detalhe do driver so no log (sem parametros).
        LOG.warn("Falha ao acessar documentos. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
