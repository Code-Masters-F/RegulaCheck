package br.com.regulacheck.solicitacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.regulacheck.auth.RelogioFixo;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria.RevisaoRegistrada;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Criacao de rascunho: dono da sessao, estado e revisao iniciais, servico ativo obrigatorio. */
class CriarRascunhoServiceTest {

    static final String PROTOCOLO = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final SessaoAutenticada PACIENTE_A =
            new SessaoAutenticada(10, 1, "Paciente A (ficticia)", Perfil.PACIENTE, Instant.parse("2026-11-12T21:00:00Z"));

    final ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
    final SolicitacaoRepositoryMemoria solicitacoes = new SolicitacaoRepositoryMemoria();
    final RelogioFixo relogio = new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z"));
    SolicitacaoService servico;

    @BeforeEach
    void preparar() {
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        servico = new SolicitacaoService(new ProtocoloService(protocolos), solicitacoes, relogio);
    }

    @Test
    void criaRascunhoDaPacienteDaSessaoNaRevisaoUm() {
        Solicitacao pedido = servico.criar(PACIENTE_A, PROTOCOLO);

        assertEquals(1, pedido.pacienteId());
        assertEquals(EstadoSolicitacao.RASCUNHO, pedido.estado());
        assertEquals(1, pedido.revisaoAtual());
        assertEquals(PROTOCOLO, pedido.protocolo().protocoloId());
        assertEquals(Instant.parse("2026-11-12T20:00:00Z"), pedido.criadaEm());
        assertEquals(pedido.criadaEm(), pedido.atualizadaEm());
        List<RevisaoRegistrada> historico = solicitacoes.revisoesDe(pedido.id());
        assertEquals(1, historico.size());
        assertEquals(1, historico.get(0).numero());
        assertEquals(MotivoRevisao.CRIACAO, historico.get(0).motivo());
    }

    @Test
    void servicoInexistenteOuInativoNaoCriaNada() {
        protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), false);

        for (String invalido : List.of("NAO-EXISTE-v1", "PROTOCOLO-ACADEMICO-FICTICIO-v0.2")) {
            ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class, () -> servico.criar(PACIENTE_A, invalido));
            assertEquals(422, e.status());
            assertEquals("REGRA_DE_NEGOCIO", e.codigo());
            assertEquals("protocoloId", e.detalhes().get(0).campo());
        }
        assertEquals(0, solicitacoes.total());
    }

    @Test
    void semNenhumServicoAtivoACriacaoERecusada() {
        protocolos.limpar();
        ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class, () -> servico.criar(PACIENTE_A, PROTOCOLO));
        assertEquals(422, e.status());
        assertEquals(0, solicitacoes.total());
    }

    @Test
    void duasCriacoesIguaisGeramDoisPedidosDistintos() {
        Solicitacao um = servico.criar(PACIENTE_A, PROTOCOLO);
        Solicitacao dois = servico.criar(PACIENTE_A, PROTOCOLO);

        assertNotEquals(um.id(), dois.id());
        assertEquals(2, solicitacoes.total());
    }

    @Test
    void instantesSaemTruncadosEmMilissegundosParaBaterComOQueOBancoGuarda() {
        // O Oracle guarda 6 casas decimais; o relogio do sistema tem mais. Truncar na origem evita que a
        // resposta da criacao difira do que uma leitura posterior devolve.
        Instant comNanos = Instant.parse("2026-11-12T20:00:00.123456789Z");
        Clock relogioPreciso = Clock.fixed(comNanos, ZoneOffset.UTC);
        SolicitacaoService preciso = new SolicitacaoService(new ProtocoloService(protocolos), solicitacoes,
                relogioPreciso);

        Solicitacao pedido = preciso.criar(PACIENTE_A, PROTOCOLO);

        assertEquals(Instant.parse("2026-11-12T20:00:00.123Z"), pedido.criadaEm());
        assertEquals(pedido.criadaEm(), solicitacoes.buscarDaPaciente(pedido.id(), 1).orElseThrow().criadaEm());
    }
}
