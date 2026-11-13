package br.com.regulacheck.solicitacao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.solicitacao.repository.ResultadoExclusao;
import br.com.regulacheck.solicitacao.repository.ResultadoTroca;
import br.com.regulacheck.solicitacao.repository.SolicitacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Falhas do banco nas rotas da RC-07: 503 e 500 genericos, sem SQL nem pilha, sem pedido pela metade. */
class BancoIndisponivelSolicitacaoTest {

    static final String CORPO = "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}";

    /** Repositorio de pedidos cujas operacoes lancam a falha informada antes de tocar em qualquer dado. */
    private record SolicitacoesQueFalham(Supplier<RuntimeException> falha) implements SolicitacaoRepository {
        @Override
        public Solicitacao criar(long pacienteId, Protocolo protocolo, Instant agora) {
            throw falha.get();
        }

        @Override
        public Optional<Solicitacao> buscarDaPaciente(long id, long pacienteId) {
            throw falha.get();
        }

        @Override
        public List<Solicitacao> listarDaPaciente(long pacienteId, int limite) {
            throw falha.get();
        }

        @Override
        public ResultadoTroca trocarProtocolo(long id, long pacienteId, Protocolo novo, Instant agora) {
            throw falha.get();
        }

        @Override
        public ResultadoExclusao excluirRascunho(long id, long pacienteId) {
            throw falha.get();
        }
    }

    private record ProtocolosQueFalham(Supplier<RuntimeException> falha) implements ProtocoloRepository {
        @Override
        public List<Protocolo> listarAtivos() {
            throw falha.get();
        }

        @Override
        public Optional<Protocolo> buscarAtivoPorProtocoloId(String protocoloId) {
            throw falha.get();
        }
    }

    private static Dependencias comPedidosQueFalham(CenarioAuth c, Supplier<RuntimeException> falha) {
        return new Dependencias(c.usuarios, c.sessoes, c.protocolos, new SolicitacoesQueFalham(falha), c.documentos,
                c.relogio);
    }

    @Test
    void bancoForaDoArNosPedidosDa503GenericoENaoCriaNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Supplier<RuntimeException> fora = () -> new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(comPedidosQueFalham(cenario, fora))) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));

            for (Resposta r : List.of(api.postJson("/solicitacoes", CORPO, auth), api.get("/solicitacoes", auth),
                    api.get("/solicitacoes/1", auth), api.enviar("PUT", "/solicitacoes/1", CORPO, auth))) {
                assertEquals(503, r.status());
                assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
                assertSemDetalheInterno(r);
            }
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    @Test
    void falhaInesperadaDa500GenericoSemPilhaNemSql() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Supplier<RuntimeException> inesperada = () -> new IllegalStateException(
                "ORA-02290: check constraint (RC_REVISAO_CK_MOTIVO) violated SELECT * FROM RC_SOLICITACAO");
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(comPedidosQueFalham(cenario, inesperada))) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));

            Resposta r = api.postJson("/solicitacoes", CORPO, auth);

            assertEquals(500, r.status());
            assertEquals("ERRO_INTERNO", r.json().get("codigo").asText());
            assertSemDetalheInterno(r);
        }
    }

    @Test
    void catalogoForaDoArDa503() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Supplier<RuntimeException> fora = () -> new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
        Dependencias deps = new Dependencias(cenario.usuarios, cenario.sessoes, new ProtocolosQueFalham(fora),
                cenario.solicitacoes, cenario.documentos, cenario.relogio);
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(deps)) {
            Map<String, String> auth = bearer(api.entrar(EMAIL_A, SENHA));

            Resposta catalogo = api.get("/protocolos", auth);
            Resposta criar = api.postJson("/solicitacoes", CORPO, auth);

            assertEquals(503, catalogo.status());
            assertEquals(503, criar.status());
            assertEquals(0, cenario.solicitacoes.total());
        }
    }

    private static void assertSemDetalheInterno(Resposta r) {
        for (String interno : new String[] {"ORA-", "SELECT ", "RC_", "Exception", "br.com.regulacheck", "\tat "}) {
            assertFalse(r.corpo().contains(interno), "resposta expoe: " + interno);
        }
    }
}
