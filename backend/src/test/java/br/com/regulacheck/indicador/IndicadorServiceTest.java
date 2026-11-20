package br.com.regulacheck.indicador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.indicador.repository.ContagensIndicadores;
import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.indicador.service.IndicadorService;
import br.com.regulacheck.indicador.service.Indicadores;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** RC-20 (FR-002, FR-004, FR-005, FR-007): regras de apresentacao dos indicadores, sem banco. */
class IndicadorServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-18T20:00:00Z");

    private final IndicadorRepositoryMemoria contagens = new IndicadorRepositoryMemoria();
    private final ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
    private final IndicadorService servico =
            new IndicadorService(contagens, protocolos, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void bancoVazioDevolveZerosEmTodosOsGruposESemRegrasComPendencias() {
        Indicadores indicadores = servico.consultar();

        assertEquals(0, indicadores.totalPedidos());
        assertEquals(0, indicadores.pedidosEmFalhaAnalise());
        assertEquals(Map.of(ResultadoGeral.COM_PENDENCIAS, 0L, ResultadoGeral.REQUER_CONFERENCIA, 0L,
                ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, 0L), indicadores.analisesPorResultado());
        assertTrue(indicadores.regrasComMaisPendencias().isEmpty());
        assertTrue(indicadores.protocolosAtivos().isEmpty(), "o repositorio novo nao tem protocolo ativo");
        assertEquals(AGORA, indicadores.consultadoEm());
    }

    @Test
    void grupoAusenteViraZeroEOsDemaisSaoMantidos() {
        contagens.definir(new ContagensIndicadores(9, 2, Map.of(ResultadoGeral.COM_PENDENCIAS, 4L), List.of()));

        Map<ResultadoGeral, Long> grupos = servico.consultar().analisesPorResultado();

        assertEquals(4L, grupos.get(ResultadoGeral.COM_PENDENCIAS));
        assertEquals(0L, grupos.get(ResultadoGeral.REQUER_CONFERENCIA));
        assertEquals(0L, grupos.get(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS));
    }

    @Test
    void regrasOrdenadasPorTotalDecrescenteComDesempatePorCodigoECortadasEmCinco() {
        contagens.definir(new ContagensIndicadores(10, 0, Map.of(), List.of(
                new PendenciasDaRegra("RC-R06", "F", 1, 0),
                new PendenciasDaRegra("RC-R03", "C", 1, 2),
                new PendenciasDaRegra("RC-R01", "A", 0, 3),
                new PendenciasDaRegra("RC-R05", "E", 2, 0),
                new PendenciasDaRegra("RC-R02", "B", 5, 1),
                new PendenciasDaRegra("RC-R04", "D", 0, 2),
                new PendenciasDaRegra("RC-R07", "G", 0, 0))));

        List<String> ordem = servico.consultar().regrasComMaisPendencias().stream()
                .map(PendenciasDaRegra::codigo).toList();

        // Totais: R02=6, R01=3, R03=3, R04=2, R05=2, R06=1, R07=0 (excluida).
        assertEquals(List.of("RC-R02", "RC-R01", "RC-R03", "RC-R04", "RC-R05"), ordem);
    }

    @Test
    void protocoloAtivoVemComTodasAsRegrasDaVersaoNaOrdem() {
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        protocolos.adicionar(ProtocoloRepositoryMemoria.segundo(), false);

        List<Indicadores.ProtocoloAtivo> ativos = servico.consultar().protocolosAtivos();

        assertEquals(1, ativos.size(), "protocolo inativo nao entra");
        assertEquals("v0.1", ativos.get(0).protocolo().versao());
        assertEquals(List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05", "RC-R06"),
                ativos.get(0).regras().stream().map(r -> r.codigo()).toList());
    }
}
