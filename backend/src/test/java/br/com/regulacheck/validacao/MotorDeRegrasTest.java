package br.com.regulacheck.validacao;

import static br.com.regulacheck.validacao.EntradasDeTeste.encaminhamento;
import static br.com.regulacheck.validacao.EntradasDeTeste.entrada;
import static br.com.regulacheck.validacao.EntradasDeTeste.identificacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.validacao.model.AvaliacaoDeRegras;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Pendencia;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.ResultadoDeRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import br.com.regulacheck.validacao.model.Veredito;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import br.com.regulacheck.validacao.motor.ProtocoloNaoSuportadoException;
import br.com.regulacheck.validacao.regra.RegraDocumental;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Motor generico (FR-001 a FR-006, FR-019 a FR-021, FR-026) com regras falsas, sem HTTP nem banco. */
class MotorDeRegrasTest {

    private static final EntradaAvaliacao VAZIA = entrada();
    private static final Evidencia EVIDENCIA =
            new Evidencia(9, CodigoCampo.NOME, SituacaoEvidencia.ILEGIVEL, null, null);

    /** Regra falsa: devolve sempre o mesmo veredito e conta quantas vezes foi chamada. */
    private static final class RegraFalsa implements RegraDocumental {
        private final String codigo;
        private final NaturezaRegra natureza;
        private final Veredito veredito;
        private int chamadas;

        RegraFalsa(String codigo, NaturezaRegra natureza, Veredito veredito) {
            this.codigo = codigo;
            this.natureza = natureza;
            this.veredito = veredito;
        }

        @Override
        public String codigo() {
            return codigo;
        }

        @Override
        public NaturezaRegra natureza() {
            return natureza;
        }

        @Override
        public Veredito avaliar(EntradaAvaliacao entrada) {
            chamadas++;
            return veredito;
        }
    }

    private static RegraFalsa falsa(String codigo, Veredito veredito) {
        return new RegraFalsa(codigo, NaturezaRegra.OBRIGATORIA, veredito);
    }

    private static RegraDoProtocolo regra(String codigo, int ordem) {
        return regra(codigo, ordem, NaturezaRegra.OBRIGATORIA, ResponsavelCorrecao.UNIDADE_EMISSORA);
    }

    private static RegraDoProtocolo regra(String codigo, int ordem, NaturezaRegra natureza,
            ResponsavelCorrecao responsavel) {
        return new RegraDoProtocolo(codigo, "Descricao " + codigo, "Evidencia " + codigo, natureza,
                natureza == NaturezaRegra.CONDICIONAL ? "Condicao " + codigo : null, "Mensagem da versao " + codigo,
                responsavel, ordem);
    }

    private static List<Resultado> resultados(AvaliacaoDeRegras avaliacao) {
        return avaliacao.resultados().stream().map(ResultadoDeRegra::resultado).toList();
    }

    @Test
    void avaliaTodasAsRegrasMesmoDepoisDeUmaPendencia() {
        RegraFalsa a = falsa("A", Veredito.pendente(List.of()));
        RegraFalsa b = falsa("B", Veredito.atende(List.of()));
        RegraFalsa c = falsa("C", Veredito.naoVerificavel(MotivoNaoVerificavel.CAMPO_ILEGIVEL, List.of()));
        AvaliacaoDeRegras avaliacao = new MotorDeRegras(List.of(a, b, c))
                .avaliar(List.of(regra("A", 1), regra("B", 2), regra("C", 3)), VAZIA);
        assertEquals(List.of(Resultado.PENDENTE, Resultado.ATENDE, Resultado.NAO_VERIFICAVEL), resultados(avaliacao));
        assertEquals(1, a.chamadas);
        assertEquals(1, b.chamadas);
        assertEquals(1, c.chamadas);
        assertEquals(ResultadoGeral.COM_PENDENCIAS, avaliacao.resultadoGeral());
        assertEquals(List.of("A", "C"), avaliacao.pendencias().stream().map(Pendencia::codigoRegra).toList());
    }

    @Test
    void resultadoGeralSegueAPrecedenciaDoProtocolo() {
        assertEquals(ResultadoGeral.COM_PENDENCIAS,
                ResultadoGeral.de(List.of(Resultado.NAO_VERIFICAVEL, Resultado.PENDENTE, Resultado.ATENDE)));
        assertEquals(ResultadoGeral.REQUER_CONFERENCIA,
                ResultadoGeral.de(List.of(Resultado.ATENDE, Resultado.NAO_VERIFICAVEL, Resultado.NAO_APLICAVEL)));
        assertEquals(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS,
                ResultadoGeral.de(List.of(Resultado.ATENDE, Resultado.NAO_APLICAVEL)));
        assertThrows(IllegalArgumentException.class, () -> ResultadoGeral.de(List.of()));
    }

    @Test
    void segueAOrdemDaVersaoEDesempataPeloCodigo() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("X", Veredito.atende(List.of())),
                falsa("Y", Veredito.atende(List.of())), falsa("Z", Veredito.atende(List.of()))));
        AvaliacaoDeRegras avaliacao = motor.avaliar(List.of(regra("Z", 2), regra("Y", 1), regra("X", 2)), VAZIA);
        assertEquals(List.of("Y", "X", "Z"),
                avaliacao.resultados().stream().map(r -> r.regra().codigo()).toList());
    }

    @Test
    void pendenteUsaMensagemEResponsavelDaVersao() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A", Veredito.pendente(List.of(EVIDENCIA)))));
        Pendencia pendencia = motor.avaliar(List.of(regra("A", 1)), VAZIA).pendencias().get(0);
        assertEquals(Resultado.PENDENTE, pendencia.tipo());
        assertEquals("Mensagem da versao A", pendencia.explicacao());
        assertEquals("Descricao A", pendencia.descricaoRegra());
        assertEquals(ResponsavelCorrecao.UNIDADE_EMISSORA, pendencia.responsavel());
        assertNull(pendencia.motivo());
        assertEquals(List.of(EVIDENCIA), pendencia.evidencias());
    }

    @Test
    void naoVerificavelUsaMensagemPadraoMotivoEPaciente() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A",
                Veredito.naoVerificavel(MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA, List.of(EVIDENCIA)))));
        Pendencia pendencia = motor.avaliar(List.of(regra("A", 1)), VAZIA).pendencias().get(0);
        assertEquals(Resultado.NAO_VERIFICAVEL, pendencia.tipo());
        // Texto do protocolo academico v0.1, secao "Mensagem padrao para nao verificavel".
        assertEquals("Não conseguimos ler esta informação no documento enviado. Isso não significa que ela esteja "
                + "faltando. Envie uma foto ou arquivo mais nítido ou confira a transcrição.", pendencia.explicacao());
        assertEquals(MotivoNaoVerificavel.TRANSCRICAO_NAO_CONFERIDA, pendencia.motivo());
        // A regra seria da unidade emissora se pendente; nao verificavel e acao da paciente.
        assertEquals(ResponsavelCorrecao.PACIENTE, pendencia.responsavel());
        assertEquals(List.of(EVIDENCIA), pendencia.evidencias());
    }

    @Test
    void atendeENaoAplicavelNaoGeramPendencia() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A", Veredito.atende(List.of())),
                new RegraFalsa("B", NaturezaRegra.CONDICIONAL, Veredito.naoAplicavel(List.of()))));
        AvaliacaoDeRegras avaliacao = motor.avaliar(List.of(regra("A", 1),
                regra("B", 2, NaturezaRegra.CONDICIONAL, ResponsavelCorrecao.PACIENTE)), VAZIA);
        assertTrue(avaliacao.pendencias().isEmpty());
        assertEquals(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, avaliacao.resultadoGeral());
    }

    @Test
    void versaoSemRegrasERecusada() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A", Veredito.atende(List.of()))));
        ProtocoloNaoSuportadoException falha =
                assertThrows(ProtocoloNaoSuportadoException.class, () -> motor.avaliar(List.of(), VAZIA));
        assertEquals(ProtocoloNaoSuportadoException.Motivo.SEM_REGRAS, falha.motivo());
    }

    @Test
    void regraSemImplementacaoRecusaAVersaoSemAvaliarNenhuma() {
        RegraFalsa a = falsa("A", Veredito.atende(List.of()));
        MotorDeRegras motor = new MotorDeRegras(List.of(a));
        ProtocoloNaoSuportadoException falha = assertThrows(ProtocoloNaoSuportadoException.class,
                () -> motor.avaliar(List.of(regra("A", 1), regra("NOVA", 2)), VAZIA));
        assertEquals(ProtocoloNaoSuportadoException.Motivo.REGRA_DESCONHECIDA, falha.motivo());
        assertEquals("NOVA", falha.codigoRegra());
        assertEquals(0, a.chamadas, "nenhum resultado parcial");
    }

    @Test
    void naturezaDivergenteERegraRepetidaRecusamAVersao() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A", Veredito.atende(List.of()))));
        assertEquals(ProtocoloNaoSuportadoException.Motivo.NATUREZA_DIVERGENTE,
                assertThrows(ProtocoloNaoSuportadoException.class, () -> motor.avaliar(
                        List.of(regra("A", 1, NaturezaRegra.CONDICIONAL, ResponsavelCorrecao.PACIENTE)), VAZIA))
                        .motivo());
        assertEquals(ProtocoloNaoSuportadoException.Motivo.REGRA_REPETIDA,
                assertThrows(ProtocoloNaoSuportadoException.class,
                        () -> motor.avaliar(List.of(regra("A", 1), regra("A", 2)), VAZIA)).motivo());
    }

    @Test
    void regraObrigatoriaNuncaENaoAplicavel() {
        MotorDeRegras motor = new MotorDeRegras(List.of(falsa("A", Veredito.naoAplicavel(List.of()))));
        assertThrows(IllegalStateException.class, () -> motor.avaliar(List.of(regra("A", 1)), VAZIA));
    }

    @Test
    void implementacaoRepetidaNoMotorERecusada() {
        assertThrows(IllegalArgumentException.class, () -> new MotorDeRegras(
                List.of(falsa("A", Veredito.atende(List.of())), falsa("A", Veredito.pendente(List.of())))));
    }

    @Test
    void mesmosDadosProduzemOMesmoResultado() {
        MotorDeRegras motor = MotorDeRegras.doProtocoloAcademico();
        List<RegraDoProtocolo> versao = new ArrayList<>(ProtocoloRepositoryMemoria.regrasV01());
        EntradaAvaliacao dados = entrada(encaminhamento(1).pronto(), identificacao(2).pronto());
        AvaliacaoDeRegras primeira = motor.avaliar(versao, dados);
        Collections.reverse(versao);
        AvaliacaoDeRegras segunda = motor.avaliar(versao, dados);
        assertEquals(primeira, segunda);
        assertEquals(6, primeira.resultados().size());
    }
}
