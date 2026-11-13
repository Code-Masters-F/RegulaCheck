package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_A;
import static br.com.regulacheck.analise.CenarioAvaliacao.PACIENTE_B;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static br.com.regulacheck.analise.CenarioAvaliacao.lido;
import static br.com.regulacheck.analise.CenarioAvaliacao.servico;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.analise.service.AvaliacaoDoPedido;
import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.Solicitacao;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Pendencia;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.ResultadoDeRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import br.com.regulacheck.validacao.motor.ProtocoloNaoSuportadoException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Montagem da avaliacao a partir do pedido (US4, FR-005, FR-018, FR-023 a FR-027, SC-005, SC-007, SC-008): API em
 * memoria com as amostras sinteticas, leitura simulada e conferencia pelas rotas reais; avaliacao pelo servico. O
 * relogio do cenario comeca em 2026-11-12T20:00:00Z (17h em Salvador).
 */
class AvaliacaoServiceTest {

    private CenarioDocumento c;
    private AvaliacaoService avaliacao;

    @BeforeEach
    void iniciar() throws Exception {
        c = CenarioExtracao.iniciarComSimulado();
        avaliacao = servico(c);
    }

    @AfterEach
    void encerrar() throws Exception {
        c.close();
    }

    private static List<Resultado> resultados(AvaliacaoDoPedido a) {
        return a.avaliacao().resultados().stream().map(ResultadoDeRegra::resultado).toList();
    }

    private static Pendencia pendencia(AvaliacaoDoPedido a, String codigo) {
        return a.avaliacao().pendencias().stream().filter(p -> p.codigoRegra().equals(codigo)).findFirst()
                .orElseThrow(() -> new AssertionError("sem pendencia para " + codigo));
    }

    @Test
    void amostrasOficiaisSemConferirRequeremConferencia() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(ResultadoGeral.REQUER_CONFERENCIA, a.avaliacao().resultadoGeral());
        assertTrue(resultados(a).stream().allMatch(r -> r == Resultado.NAO_VERIFICAVEL));
        for (String codigo : List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05")) {
            assertEquals(MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA, pendencia(a, codigo).motivo(), codigo);
        }
        assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, pendencia(a, "RC-R06").motivo());
    }

    @Test
    void amostrasOficiaisConferidasNaoTemPendencias() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(List.of(Resultado.ATENDE, Resultado.ATENDE, Resultado.ATENDE, Resultado.ATENDE, Resultado.ATENDE,
                Resultado.NAO_APLICAVEL), resultados(a));
        assertEquals(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, a.avaliacao().resultadoGeral());
        assertTrue(a.avaliacao().pendencias().isEmpty());

        Solicitacao atual = c.cenario.solicitacoes.buscarDaPaciente(pedido.numero(), 1).orElseThrow();
        assertEquals(pedido.numero(), a.solicitacaoId());
        assertEquals(atual.revisaoAtual(), a.revisao());
        assertEquals(Long.parseLong(pedido.leitura()), a.extracaoId());
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", a.protocolo().protocoloId());
        assertEquals(LocalDate.of(2026, 11, 12), a.dataReferencia());
        assertEquals(Instant.parse("2026-11-12T20:00:00Z"), a.conferenciaAtualizadaEm());
        assertTrue(a.leituraSimulada());
    }

    @Test
    void divergenciaApontadaTiraOCampoDaEvidencia() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        String encaminhamento = pedido.documentos().get(0);
        confirmarTudo(c, pedido, Map.of(encaminhamento + "/NOME",
                divergir(encaminhamento, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO")));
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(Resultado.NAO_VERIFICAVEL, a.avaliacao().resultadoDe("RC-R02").resultado());
        assertEquals(MotivoNaoVerificavel.DIVERGENCIA_APONTADA, pendencia(a, "RC-R02").motivo());
        assertEquals(ResultadoGeral.REQUER_CONFERENCIA, a.avaliacao().resultadoGeral());
    }

    @Test
    void identificacaoBorradaDeixaR02R03R06NaoVerificaveis() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.BORRADA);
        confirmarTudo(c, pedido);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(List.of(Resultado.ATENDE, Resultado.NAO_VERIFICAVEL, Resultado.NAO_VERIFICAVEL, Resultado.ATENDE,
                Resultado.ATENDE, Resultado.NAO_VERIFICAVEL), resultados(a));
        assertEquals(MotivoNaoVerificavel.CAMPO_ILEGIVEL, pendencia(a, "RC-R03").motivo());
        assertEquals(MotivoNaoVerificavel.CONDICAO_INDETERMINADA, pendencia(a, "RC-R06").motivo());
    }

    @Test
    void encaminhamentoSemMotivoFicaPendenteParaAUnidadeEmissora() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.SEM_MOTIVO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(ResultadoGeral.COM_PENDENCIAS, a.avaliacao().resultadoGeral());
        assertEquals(List.of("RC-R04"), a.avaliacao().pendencias().stream().map(Pendencia::codigoRegra).toList());
        assertEquals(ResponsavelCorrecao.UNIDADE_EMISSORA, pendencia(a, "RC-R04").responsavel());
    }

    @Test
    void tipoLidoPrevaleceSobreOInformadoNoEnvio() throws Exception {
        // pdf-tipo-divergente.pdf e enviado como encaminhamento, mas e lido como identificacao.
        Pedido pedido = lido(c, AmostrasSinteticas.TIPO_DIVERGENTE, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(Resultado.PENDENTE, a.avaliacao().resultadoDe("RC-R01").resultado());
        assertEquals(Resultado.ATENDE, a.avaliacao().resultadoDe("RC-R03").resultado());
    }

    @Test
    void pedidoDeOutraPacienteEInexistenteRecebemAMesmaRecusa() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        ExcecaoNegocio alheio = assertThrows(ExcecaoNegocio.class,
                () -> avaliacao.avaliar(PACIENTE_B, pedido.numero()));
        ExcecaoNegocio inexistente = assertThrows(ExcecaoNegocio.class, () -> avaliacao.avaliar(PACIENTE_A, 999_999));
        assertEquals(404, alheio.status());
        assertEquals("NAO_ENCONTRADO", alheio.codigo());
        assertEquals(alheio.status(), inexistente.status());
        assertEquals(alheio.codigo(), inexistente.codigo());
        assertEquals(alheio.getMessage(), inexistente.getMessage());
    }

    @Test
    void pedidoSemLeituraNaoEAvaliado() throws Exception {
        String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
        ExcecaoNegocio falha = assertThrows(ExcecaoNegocio.class,
                () -> avaliacao.avaliar(PACIENTE_A, Long.parseLong(pedido)));
        assertEquals(409, falha.status());
        assertEquals("CONFLITO_ESTADO", falha.codigo());
        assertEquals("Leia os documentos antes de conferir os critérios.", falha.getMessage());
    }

    @Test
    void leituraDesatualizadaNaoEAvaliada() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        CenarioExtracao.enviar(c, c.authA, pedido.id(), AmostrasSinteticas.SEM_MOTIVO);
        ExcecaoNegocio falha = assertThrows(ExcecaoNegocio.class,
                () -> avaliacao.avaliar(PACIENTE_A, pedido.numero()));
        assertEquals(409, falha.status());
        assertEquals("Os documentos mudaram; leia os documentos de novo.", falha.getMessage());
    }

    /** Avanca o relogio e entra de novo (a sessao do cenario dura 30 minutos). */
    private Map<String, String> avancarEEntrar(Duration quanto) throws Exception {
        c.cenario.relogio.avancar(quanto);
        return bearer(c.api.entrar(EMAIL_A, SENHA));
    }

    @Test
    void dataDeReferenciaEOEnvioMaisRecenteNoHorarioDeSalvador() throws Exception {
        String pedido = c.criarRascunho(c.authA);
        CenarioExtracao.enviar(c, c.authA, pedido, AmostrasSinteticas.IDENTIFICACAO);
        // 2026-11-13T02:30Z: ainda dia 12 em Salvador (UTC-3), ja dia 13 em UTC.
        Map<String, String> auth = avancarEEntrar(Duration.ofHours(6).plusMinutes(30));
        CenarioExtracao.enviar(c, auth, pedido, AmostrasSinteticas.ENCAMINHAMENTO);
        c.lerDocumentos(auth, pedido);
        assertEquals(LocalDate.of(2026, 11, 12), avaliacao.avaliar(PACIENTE_A, Long.parseLong(pedido)).dataReferencia());

        // O envio mais recente vale, qualquer que seja o tipo do documento: aqui a identificacao, 3 dias depois.
        String outro = c.criarRascunho(auth);
        CenarioExtracao.enviar(c, auth, outro, AmostrasSinteticas.ENCAMINHAMENTO);
        auth = avancarEEntrar(Duration.ofDays(3));
        CenarioExtracao.enviar(c, auth, outro, AmostrasSinteticas.IDENTIFICACAO);
        c.lerDocumentos(auth, outro);
        assertEquals(LocalDate.of(2026, 11, 15), avaliacao.avaliar(PACIENTE_A, Long.parseLong(outro)).dataReferencia());
    }

    @Test
    void avaliarNaoMudaNadaNoPedidoEEDeterministico() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.BORRADA);
        confirmarTudo(c, pedido);
        CenarioAuth a = c.cenario;
        Solicitacao antes = a.solicitacoes.buscarDaPaciente(pedido.numero(), 1).orElseThrow();
        var leituraAntes = a.extracoes.buscarMaisRecente(1, pedido.numero()).orElseThrow();
        var conferenciaAntes = a.conferencias.buscar(1, pedido.numero(), leituraAntes.extracao().id()).orElseThrow();
        var revisoesAntes = a.solicitacoes.revisoesDe(pedido.numero());

        AvaliacaoDoPedido primeira = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        AvaliacaoDoPedido segunda = avaliacao.avaliar(PACIENTE_A, pedido.numero());

        assertEquals(primeira, segunda);
        assertEquals(antes, a.solicitacoes.buscarDaPaciente(pedido.numero(), 1).orElseThrow());
        assertEquals(leituraAntes, a.extracoes.buscarMaisRecente(1, pedido.numero()).orElseThrow());
        assertEquals(conferenciaAntes,
                a.conferencias.buscar(1, pedido.numero(), leituraAntes.extracao().id()).orElseThrow());
        assertEquals(revisoesAntes, a.solicitacoes.revisoesDe(pedido.numero()));
        assertEquals(EstadoSolicitacao.RASCUNHO, antes.estado());
    }

    @Test
    void pedidoSubmetidoContinuaAvaliavelSoParaLeitura() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        c.cenario.solicitacoes.forcarEstado(pedido.numero(), EstadoSolicitacao.SUBMETIDA_SIMULACAO);
        AvaliacaoDoPedido a = avaliacao.avaliar(PACIENTE_A, pedido.numero());
        assertEquals(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, a.avaliacao().resultadoGeral());
        assertEquals(EstadoSolicitacao.SUBMETIDA_SIMULACAO,
                c.cenario.solicitacoes.buscarDaPaciente(pedido.numero(), 1).orElseThrow().estado());
    }

    @Test
    void versaoSemRegrasNaoProduzResultado() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        confirmarTudo(c, pedido);
        c.cenario.protocolos.definirRegras(1, List.of());
        ProtocoloNaoSuportadoException falha = assertThrows(ProtocoloNaoSuportadoException.class,
                () -> avaliacao.avaliar(PACIENTE_A, pedido.numero()));
        assertEquals(ProtocoloNaoSuportadoException.Motivo.SEM_REGRAS, falha.motivo());
    }

    @Test
    void bancoIndisponivelPropagaO503SemResultado() throws Exception {
        Pedido pedido = lido(c, AmostrasSinteticas.ENCAMINHAMENTO, AmostrasSinteticas.IDENTIFICACAO);
        ProtocoloRepository fora = new ProtocoloRepository() {
            @Override
            public List<Protocolo> listarAtivos() {
                throw indisponivel();
            }

            @Override
            public Optional<Protocolo> buscarAtivoPorProtocoloId(String protocoloId) {
                throw indisponivel();
            }

            @Override
            public List<RegraDoProtocolo> listarRegras(long protocoloId) {
                throw indisponivel();
            }

            private ExcecaoNegocio indisponivel() {
                return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                        "Servico temporariamente indisponivel. Tente novamente em instantes.");
            }
        };
        CenarioAuth a = c.cenario;
        AvaliacaoService comBancoFora = new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias,
                a.documentos, fora, MotorDeRegras.doProtocoloAcademico());
        ExcecaoNegocio falha = assertThrows(ExcecaoNegocio.class,
                () -> comBancoFora.avaliar(PACIENTE_A, pedido.numero()));
        assertEquals(503, falha.status());
    }
}
