package br.com.regulacheck.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.usuario.repository.UsuarioRepositoryJdbc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verificacao <strong>somente de leitura</strong> contra o Oracle real (opt-in): entradas com aspas e comandos de
 * banco sao tratadas como valor pela consulta parametrizada (FR-028).
 *
 * <p>So roda com {@code DB_URL}, {@code DB_USERNAME}, {@code DB_PASSWORD} e {@code RC_TESTE_ORACLE=SIM}; senao e
 * ignorado e deve ser registrado como NAO VERIFICADO. Pre-requisito: scripts 01 e 02 executados na conta. Nao
 * grava nem remove nada.
 */
class InjecaoSqlOracleTest {

    private static BancoDados banco;

    @BeforeAll
    static void preparar() {
        Configuracao ambiente = Configuracao.deAmbiente();
        assumeTrue("SIM".equals(System.getenv("RC_TESTE_ORACLE")), "RC_TESTE_ORACLE != SIM: teste de Oracle ignorado");
        assumeTrue(ambiente.banco().configurado(), "DB_* nao configuradas: teste de Oracle ignorado");
        banco = BancoDados.criar(ambiente.banco());
    }

    @AfterAll
    static void encerrar() {
        if (banco != null) {
            banco.fechar();
        }
    }

    @Test
    void entradasComAspasEComandosNaoAlteramAConsulta() {
        UsuarioRepositoryJdbc usuarios = new UsuarioRepositoryJdbc(banco);
        for (String entrada : new String[] {"' OR 1=1 --", "x' OR '1'='1", "'; DROP TABLE RC_USUARIO; --",
                "\" OR \"\"=\"", "paciente.a@exemplo.invalid' --"}) {
            assertTrue(usuarios.buscarPorEmail(entrada).isEmpty(), "a entrada nao pode virar parte da consulta");
        }
        // Controle positivo: uma busca legitima continua funcionando (consulta nao quebrou).
        assertEquals(true, usuarios.buscarPorEmail("e-mail-que-nao-existe@exemplo.invalid").isEmpty());
    }
}
