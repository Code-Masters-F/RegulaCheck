package br.com.regulacheck.solicitacao.service;

import br.com.regulacheck.armazenamento.ArmazenamentoEmDisco;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Troca de servico do rascunho: revisao, historico, mesmo servico, fora de rascunho e dono. */
class TrocarServicoServiceTest {

    static final String V01 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.1";
    static final String V02 = "PROTOCOLO-ACADEMICO-FICTICIO-v0.2";
    static final Instant INICIO = Instant.parse("2026-11-12T20:00:00Z");
    static final SessaoAutenticada A = new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE, INICIO.plusSeconds(3600));
    static final SessaoAutenticada B = new SessaoAutenticada(11, 2, "B", Perfil.PACIENTE, INICIO.plusSeconds(3600));

    final ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
    final SolicitacaoRepositoryMemoria solicitacoes = new SolicitacaoRepositoryMemoria();
    final RelogioFixo relogio = new RelogioFixo(INICIO);
    SolicitacaoService servico;
    Solicitacao rascunho;

    @BeforeEach
    void preparar() {
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), true);
        servico = new SolicitacaoService(new ProtocoloService(protocolos), solicitacoes, new ArmazenamentoEmDisco(null), relogio);
        rascunho = servico.criar(A, V01);
        relogio.avancar(Duration.ofMinutes(5));
    }

    @Test
    void servicoDiferenteIncrementaARevisaoERegistraOMotivo() {
        Solicitacao trocada = servico.trocarProtocolo(A, rascunho.id(), V02);

        assertEquals(V02, trocada.protocolo().protocoloId());
        assertEquals(2, trocada.revisaoAtual());
        assertEquals(INICIO.plus(Duration.ofMinutes(5)), trocada.atualizadaEm());
        assertEquals(INICIO, trocada.criadaEm());
        List<RevisaoRegistrada> historico = solicitacoes.revisoesDe(rascunho.id());
        assertEquals(2, historico.size());
        assertEquals(MotivoRevisao.PROTOCOLO_ALTERADO, historico.get(1).motivo());
        assertEquals(2, historico.get(1).numero());
        // A leitura posterior devolve o que foi gravado.
        assertEquals(trocada, servico.buscarPropria(A, rascunho.id()));
    }

    @Test
    void mesmoServicoNaoMudaRevisaoNemDataNemHistorico() {
        Solicitacao antes = servico.buscarPropria(A, rascunho.id());

        Solicitacao depois = servico.trocarProtocolo(A, rascunho.id(), V01);

        assertEquals(antes, depois);
        assertEquals(1, depois.revisaoAtual());
        assertEquals(1, solicitacoes.revisoesDe(rascunho.id()).size());
    }

    @Test
    void duasTrocasSeguidasGeramRevisoesSequenciais() {
        servico.trocarProtocolo(A, rascunho.id(), V02);
        Solicitacao volta = servico.trocarProtocolo(A, rascunho.id(), V01);

        assertEquals(3, volta.revisaoAtual());
        assertEquals(List.of(1, 2, 3),
                solicitacoes.revisoesDe(rascunho.id()).stream().map(RevisaoRegistrada::numero).toList());
    }

    @Test
    void foraDeRascunhoE409ENadaMuda() {
        solicitacoes.forcarEstado(rascunho.id(), EstadoSolicitacao.PROCESSANDO);
        Solicitacao antes = servico.buscarPropria(A, rascunho.id());

        ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class, () -> servico.trocarProtocolo(A, rascunho.id(), V02));

        assertEquals(409, e.status());
        assertEquals("CONFLITO_ESTADO", e.codigo());
        assertEquals("Este pedido não pode mais ser alterado.", e.getMessage());
        assertEquals(antes, servico.buscarPropria(A, rascunho.id()));
        assertEquals(1, solicitacoes.revisoesDe(rascunho.id()).size());
    }

    @Test
    void pedidoAlheioOuInexistenteE404Identico() {
        ExcecaoNegocio alheio = assertThrows(ExcecaoNegocio.class, () -> servico.trocarProtocolo(B, rascunho.id(), V02));
        ExcecaoNegocio inexistente = assertThrows(ExcecaoNegocio.class, () -> servico.trocarProtocolo(A, 999_999, V02));

        for (ExcecaoNegocio e : List.of(alheio, inexistente)) {
            assertEquals(404, e.status());
            assertEquals("NAO_ENCONTRADO", e.codigo());
        }
        assertEquals(alheio.getMessage(), inexistente.getMessage());
        assertEquals(V01, servico.buscarPropria(A, rascunho.id()).protocolo().protocoloId());
    }

    @Test
    void servicoInativoOuInexistenteE422SemAlterarOPedido() {
        protocolos.desativar(V02);

        for (String invalido : List.of(V02, "NAO-EXISTE-v1")) {
            ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class,
                    () -> servico.trocarProtocolo(A, rascunho.id(), invalido));
            assertEquals(422, e.status());
        }
        assertEquals(1, servico.buscarPropria(A, rascunho.id()).revisaoAtual());
    }

    @Test
    void semNenhumServicoAtivoATrocaERecusada() {
        protocolos.limpar();
        ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class, () -> servico.trocarProtocolo(A, rascunho.id(), V02));
        assertEquals(422, e.status());
        assertEquals(1, solicitacoes.revisoesDe(rascunho.id()).size());
    }
}
