package br.com.regulacheck.auth.autorizacao;

import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.caminho;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US1 / SC-001: a paciente so enxerga o que e dela. Para os cinco tipos de recurso e para leitura,
 * alteracao e exclusao, recurso de outra paciente responde exatamente como um que nao existe, e nada
 * muda no recurso alheio.
 */
class MatrizPacienteTest {

    private static final String CORPO_VALIDO = "{\"nome\":\"novo\"}";

    @Test
    void pacienteAcessaOProprioRecursoNosCincoTipos() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            for (Tipo tipo : Tipo.values()) {
                Resposta le = amb.chamar("GET", caminho(tipo, 1), amb.tokenA, null);
                assertEquals(200, le.status(), tipo + " GET");
                assertEquals(tipo.id(1), le.json().get("id").asLong());

                Resposta altera = amb.chamar("PUT", caminho(tipo, 1), amb.tokenA, CORPO_VALIDO);
                assertEquals(200, altera.status(), tipo + " PUT");
                assertEquals(1, altera.json().get("versao").asInt());

                assertEquals(204, amb.chamar("DELETE", caminho(tipo, 1), amb.tokenA, null).status(), tipo + " DELETE");
                assertEquals(404, amb.chamar("GET", caminho(tipo, 1), amb.tokenA, null).status(), tipo + " apos excluir");
            }
        }
    }

    @Test
    void recursoDeOutraPacienteRespondeIgualAoInexistenteENadaMuda() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            for (Tipo tipo : Tipo.values()) {
                String alheio = caminho(tipo, 2);
                String inexistente = caminho(tipo, tipo.id(2) + 50, Tipo.SOLICITACAO.id(1));
                for (String metodo : List.of("GET", "PUT", "DELETE")) {
                    String corpo = metodo.equals("PUT") ? CORPO_VALIDO : null;
                    Resposta dePaciente = amb.chamar(metodo, alheio, amb.tokenA, corpo);
                    Resposta semExistir = amb.chamar(metodo, inexistente, amb.tokenA, corpo);

                    assertEquals(404, dePaciente.status(), tipo + " " + metodo);
                    assertEquals("NAO_ENCONTRADO", dePaciente.json().get("codigo").asText());
                    assertEquals(semExistir.corpo(), dePaciente.corpo(), tipo + " " + metodo + ": corpo diferente");
                }
                assertEquals(0, amb.recursos.estado(tipo, 2).versao(), tipo + " alterado por quem nao e a dona");
                assertFalse(amb.recursos.estado(tipo, 2).removido(), tipo + " removido por quem nao e a dona");
                // E a dona continua enxergando o seu.
                assertEquals(200, amb.chamar("GET", alheio, amb.tokenB, null).status(), tipo + " dona B");
            }
        }
    }

    @Test
    void documentoDeOutraPacientePelaSolicitacaoDeAEhPelaDeBDa404() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            long documentoDeB = Tipo.DOCUMENTO.id(2);
            long solicitacaoDeA = Tipo.SOLICITACAO.id(1);
            long solicitacaoDeB = Tipo.SOLICITACAO.id(2);

            // Rota aninhada: o pai de A nao protege o documento de B, e o pai de B nao abre o documento para A.
            assertEquals(404, amb.chamar("GET", caminho(Tipo.DOCUMENTO, documentoDeB, solicitacaoDeA), amb.tokenA, null).status());
            assertEquals(404, amb.chamar("GET", caminho(Tipo.DOCUMENTO, documentoDeB, solicitacaoDeB), amb.tokenA, null).status());
            // Pai e filho incoerentes ate para a dona (documento de A sob a solicitacao de B): tambem 404.
            assertEquals(404, amb.chamar("GET", caminho(Tipo.DOCUMENTO, Tipo.DOCUMENTO.id(1), solicitacaoDeB), amb.tokenA, null).status());
            assertEquals(200, amb.chamar("GET", caminho(Tipo.DOCUMENTO, Tipo.DOCUMENTO.id(1), solicitacaoDeA), amb.tokenA, null).status());
        }
    }

    @Test
    void listagemTrazSoOsRecursosDaConta() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            for (Tipo tipo : Tipo.values()) {
                Resposta deA = amb.chamar("GET", "/teste/" + tipo.caminho(), amb.tokenA, null);
                Resposta deB = amb.chamar("GET", "/teste/" + tipo.caminho(), amb.tokenB, null);
                Resposta deAComParametro = amb.chamar("GET", "/teste/" + tipo.caminho() + "?pacienteId=2", amb.tokenA, null);

                assertEquals("[" + tipo.id(1) + "]", deA.json().get("ids").toString(), tipo + " A");
                assertEquals("[" + tipo.id(2) + "]", deB.json().get("ids").toString(), tipo + " B");
                assertEquals(deA.corpo(), deAComParametro.corpo(), tipo + ": parametro de dona nao pode valer");
            }
        }
    }

    @Test
    void donoEnviadoNoCorpoNaoMudaADona() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            // A altera o proprio recurso dizendo, no corpo, que a dona e B: o campo e ignorado.
            Resposta propria = amb.chamar("PUT", caminho(Tipo.SOLICITACAO, 1), amb.tokenA,
                    "{\"nome\":\"x\",\"pacienteId\":2,\"ownerId\":2}");
            assertEquals(200, propria.status());
            assertEquals(200, amb.chamar("GET", caminho(Tipo.SOLICITACAO, 1), amb.tokenA, null).status());
            assertEquals(404, amb.chamar("GET", caminho(Tipo.SOLICITACAO, 1), amb.tokenB, null).status());

            // A tenta alterar o de B dizendo que e a dona: continua 404 e nada muda.
            Resposta alheio = amb.chamar("PUT", caminho(Tipo.SOLICITACAO, 2), amb.tokenA,
                    "{\"nome\":\"x\",\"pacienteId\":1,\"ownerId\":1}");
            assertEquals(404, alheio.status());
            assertEquals(0, amb.recursos.estado(Tipo.SOLICITACAO, 2).versao());
        }
    }

    @Test
    void identificadorInvalidoRespondeComoNaoEncontradoSemErroInterno() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            String referencia = amb.chamar("GET", "/teste/solicitacoes/999999", amb.tokenA, null).corpo();
            for (String id : List.of("abc", "-1", "0", "99999999999999999999", "1.5", "%20")) {
                Resposta r = amb.chamar("GET", "/teste/solicitacoes/" + id, amb.tokenA, null);
                assertEquals(404, r.status(), id);
                assertEquals(referencia, r.corpo(), id);
            }
        }
    }

    @Test
    void recursoDeContaDesativadaContinuaInacessivelAsOutras() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            for (Tipo tipo : Tipo.values()) {
                assertEquals(404, amb.chamar("GET", caminho(tipo, 3), amb.tokenA, null).status(), tipo + " A");
                assertEquals(404, amb.chamar("GET", caminho(tipo, 3), amb.tokenB, null).status(), tipo + " B");
            }
        }
    }
}
