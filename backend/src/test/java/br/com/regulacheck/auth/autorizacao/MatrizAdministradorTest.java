package br.com.regulacheck.auth.autorizacao;

import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.caminho;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US2 / SC-002: o administrador consulta indicadores agregados, mas recebe 403 (e nenhum dado) em
 * qualquer recurso individual de paciente; a paciente recebe 403 na rota administrativa.
 */
class MatrizAdministradorTest {

    @Test
    void administradorConsultaIndicadoresAgregadosSemDadoIdentificavel() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            Resposta r = amb.chamar("GET", "/teste/admin/indicadores", amb.tokenAdmin, null);

            assertEquals(200, r.status());
            r.json().properties().forEach(campo -> assertTrue(campo.getValue().isInt(), campo.getKey()));
            for (String proibido : List.of("@", "Paciente", "email", "nome", "pacienteId", "caminho")) {
                assertFalse(r.corpo().contains(proibido), "indicador identificavel: " + proibido);
            }
        }
    }

    @Test
    void administradorRecebe403EmTodoRecursoIndividualSemDadoENemConsultaOBanco() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            int acessosAntes = amb.recursos.acessos();
            String referencia = null;
            for (Tipo tipo : Tipo.values()) {
                for (int indice : new int[] {1, 2}) {
                    for (String metodo : List.of("GET", "PUT", "DELETE")) {
                        String corpo = metodo.equals("PUT") ? "{\"nome\":\"x\"}" : null;
                        Resposta r = amb.chamar(metodo, caminho(tipo, indice), amb.tokenAdmin, corpo);

                        assertEquals(403, r.status(), tipo + " " + metodo);
                        assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                        referencia = referencia == null ? r.corpo() : referencia;
                        assertEquals(referencia, r.corpo(), "a mensagem de 403 nao pode variar");
                        assertFalse(r.corpo().contains(String.valueOf(tipo.id(indice))), "vazou o identificador");
                    }
                }
                // Lista: 403, nao uma lista vazia que sugira "nao ha dados".
                assertEquals(403, amb.chamar("GET", "/teste/" + tipo.caminho(), amb.tokenAdmin, null).status(), tipo + " lista");
            }
            assertEquals(acessosAntes, amb.recursos.acessos(), "o 403 deve ocorrer antes de qualquer consulta");
            for (Tipo tipo : Tipo.values()) {
                assertEquals(0, amb.recursos.estado(tipo, 1).versao());
                assertFalse(amb.recursos.estado(tipo, 1).removido());
            }
        }
    }

    @Test
    void pacienteNaRotaAdministrativaRecebe403() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            Resposta r = amb.chamar("GET", "/teste/admin/indicadores", amb.tokenA, null);

            assertEquals(403, r.status());
            assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
        }
    }
}
