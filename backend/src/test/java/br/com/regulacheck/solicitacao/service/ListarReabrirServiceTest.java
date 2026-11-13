package br.com.regulacheck.solicitacao.service;

import br.com.regulacheck.armazenamento.ArmazenamentoEmDisco;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.RelogioFixo;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Listar e reabrir: filtro pela dona, ordem, limite e 404 indistinguivel. */
class ListarReabrirServiceTest {

    static final String V01 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final Instant INICIO = Instant.parse("2026-11-12T20:00:00Z");
    static final SessaoAutenticada A = new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE, INICIO.plusSeconds(3600));
    static final SessaoAutenticada B = new SessaoAutenticada(11, 2, "B", Perfil.PACIENTE, INICIO.plusSeconds(3600));

    final SolicitacaoRepositoryMemoria solicitacoes = new SolicitacaoRepositoryMemoria();
    final RelogioFixo relogio = new RelogioFixo(INICIO);
    SolicitacaoService servico;

    @BeforeEach
    void preparar() {
        ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        servico = new SolicitacaoService(new ProtocoloService(protocolos), solicitacoes, new ArmazenamentoEmDisco(null), relogio);
    }

    @Test
    void listaSoOsPedidosDaPacienteDaSessao() {
        Solicitacao deA1 = servico.criar(A, V01);
        servico.criar(B, V01);
        Solicitacao deA2 = servico.criar(A, V01);

        List<Long> idsDeA = servico.listar(A).stream().map(Solicitacao::id).toList();

        assertEquals(List.of(deA2.id(), deA1.id()), idsDeA);
        assertTrue(servico.listar(A).stream().allMatch(p -> p.pacienteId() == 1));
        assertEquals(1, servico.listar(B).size());
    }

    @Test
    void ordenaDoMaisRecenteParaOMaisAntigoComDesempatePorId() {
        Solicitacao antigo = servico.criar(A, V01);
        relogio.avancar(Duration.ofMinutes(1));
        Solicitacao empate1 = servico.criar(A, V01);
        Solicitacao empate2 = servico.criar(A, V01);

        List<Long> ids = servico.listar(A).stream().map(Solicitacao::id).toList();

        assertEquals(List.of(empate2.id(), empate1.id(), antigo.id()), ids);
    }

    @Test
    void listaNoMaximoCinquentaOsMaisRecentes() {
        Solicitacao ultimo = null;
        for (int i = 0; i < SolicitacaoService.LIMITE_LISTA + 5; i++) {
            relogio.avancar(Duration.ofSeconds(1));
            ultimo = servico.criar(A, V01);
        }

        List<Solicitacao> lista = servico.listar(A);

        assertEquals(50, SolicitacaoService.LIMITE_LISTA);
        assertEquals(50, lista.size());
        assertEquals(ultimo.id(), lista.get(0).id());
    }

    @Test
    void semPedidosAListaEVazia() {
        assertTrue(servico.listar(A).isEmpty());
    }

    @Test
    void reabrirDevolveOQueFoiPersistido() {
        Solicitacao criada = servico.criar(A, V01);
        assertEquals(criada, servico.buscarPropria(A, criada.id()));
    }

    @Test
    void alheioEInexistenteDaoO404Identico() {
        Solicitacao deA = servico.criar(A, V01);

        ExcecaoNegocio alheio = assertThrows(ExcecaoNegocio.class, () -> servico.buscarPropria(B, deA.id()));
        ExcecaoNegocio inexistente = assertThrows(ExcecaoNegocio.class, () -> servico.buscarPropria(B, 123_456));

        assertEquals(404, alheio.status());
        assertEquals(alheio.status(), inexistente.status());
        assertEquals(alheio.codigo(), inexistente.codigo());
        assertEquals(alheio.getMessage(), inexistente.getMessage());
    }
}
