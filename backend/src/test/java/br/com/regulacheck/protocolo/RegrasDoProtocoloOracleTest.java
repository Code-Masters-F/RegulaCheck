package br.com.regulacheck.protocolo;

import static br.com.regulacheck.validacao.EntradasDeTeste.encaminhamento;
import static br.com.regulacheck.validacao.EntradasDeTeste.entrada;
import static br.com.regulacheck.validacao.EntradasDeTeste.identificacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.protocolo.repository.ProtocoloRepositoryJdbc;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao da RC-12 contra o <strong>Oracle real</strong> da faculdade (opt-in): as regras da v0.1 sao lidas de
 * {@code RC_REGRA} na ordem, com natureza e responsavel esperados, e o motor aceita a versao cadastrada.
 *
 * <p>So roda se {@code DB_URL}, {@code DB_USERNAME} e {@code DB_PASSWORD} estiverem exportadas e
 * {@code RC_TESTE_ORACLE=SIM}; caso contrario e ignorado e deve ser registrado como NAO VERIFICADO. Pre-requisito:
 * scripts 01 e 02 executados na conta (nenhum script novo nesta historia). Somente leitura: nao cria nem apaga nada.
 */
class RegrasDoProtocoloOracleTest {

    private static BancoDados banco;

    @BeforeAll
    static void preparar() {
        Configuracao ambiente = Configuracao.deAmbiente();
        assumeTrue("SIM".equals(System.getenv("RC_TESTE_ORACLE")), "RC_TESTE_ORACLE != SIM: teste de Oracle ignorado");
        assumeTrue(ambiente.banco().configurado(), "DB_* nao configuradas: teste de Oracle ignorado");
        banco = BancoDados.criar(ambiente.banco());
    }

    @AfterAll
    static void fechar() {
        if (banco != null) {
            banco.fechar();
        }
    }

    @Test
    void regrasDaV01VemDoBancoNaOrdemEOMotorAsAceita() {
        ProtocoloRepositoryJdbc protocolos = new ProtocoloRepositoryJdbc(banco);
        Protocolo v01 = protocolos.buscarAtivoPorProtocoloId("PROTOCOLO-ACADEMICO-FICTICIO-v0.1").orElseThrow();
        List<RegraDoProtocolo> regras = protocolos.listarRegras(v01.id());

        assertEquals(List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05", "RC-R06"),
                regras.stream().map(RegraDoProtocolo::codigo).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), regras.stream().map(RegraDoProtocolo::ordem).toList());
        assertEquals(List.of(NaturezaRegra.OBRIGATORIA, NaturezaRegra.OBRIGATORIA, NaturezaRegra.OBRIGATORIA,
                NaturezaRegra.OBRIGATORIA, NaturezaRegra.OBRIGATORIA, NaturezaRegra.CONDICIONAL),
                regras.stream().map(RegraDoProtocolo::natureza).toList());
        assertEquals(List.of(ResponsavelCorrecao.PACIENTE, ResponsavelCorrecao.PACIENTE, ResponsavelCorrecao.PACIENTE,
                ResponsavelCorrecao.UNIDADE_EMISSORA, ResponsavelCorrecao.UNIDADE_EMISSORA,
                ResponsavelCorrecao.PACIENTE), regras.stream().map(RegraDoProtocolo::responsavel).toList());
        assertTrue(regras.get(5).condicaoAplicacao() != null && regras.get(0).condicaoAplicacao() == null);
        // Os textos do banco sao os mesmos do dobro em memoria usado pelos testes sem Oracle.
        assertEquals(ProtocoloRepositoryMemoria.regrasV01(), regras);

        var avaliacao = MotorDeRegras.doProtocoloAcademico()
                .avaliar(regras, entrada(encaminhamento(1).pronto(), identificacao(2).pronto()));
        assertEquals(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, avaliacao.resultadoGeral());
        assertTrue(protocolos.listarRegras(-1).isEmpty(), "versao inexistente nao tem regras");
    }
}
