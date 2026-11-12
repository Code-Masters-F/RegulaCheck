package br.com.regulacheck.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import org.junit.jupiter.api.Test;

/** Verifica que o banco e preguicoso e que sua indisponibilidade vira 503 generico. */
class BancoDadosTest {

    @Test
    void criarNaoTentaConectar() {
        BancoDados banco = BancoDados.criar(Configuracao.Banco.vazio());

        assertFalse(banco.configurado());
        banco.fechar();
    }

    @Test
    void semConfiguracaoObterConexaoDevolve503() {
        BancoDados banco = BancoDados.criar(Configuracao.Banco.vazio());

        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, banco::conexao);

        assertEquals(503, erro.status());
        assertEquals("DEPENDENCIA_INDISPONIVEL", erro.codigo());
        banco.fechar();
    }

    @Test
    void bancoInacessivelDevolve503SemExporDetalhesDoDriver() {
        // Porta 1 em localhost recusa a conexao rapidamente; o tempo curto evita teste lento.
        Configuracao.Banco inacessivel = new Configuracao.Banco(
                "jdbc:oracle:thin:@localhost:1:orcl", "usuario-ficticio", "senha-ficticia");
        BancoDados banco = BancoDados.criar(inacessivel, 1500);

        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, banco::conexao);

        assertEquals(503, erro.status());
        assertEquals("DEPENDENCIA_INDISPONIVEL", erro.codigo());
        String mensagem = erro.getMessage();
        assertFalse(mensagem.contains("ORA-"));
        assertFalse(mensagem.contains("jdbc"));
        assertFalse(mensagem.contains("senha-ficticia"));
        assertTrue(mensagem.length() > 0);
        banco.fechar();
    }
}
