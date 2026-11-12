package br.com.regulacheck.auth.autorizacao;

import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.caminho;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import br.com.regulacheck.usuario.entity.Perfil;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * US3 / SC-003: a ordem e sempre autenticacao (401), perfil (403), dona (404), corpo (400); as rotas
 * publicas continuam publicas e as mensagens nao variam por motivo.
 */
class OrdemDeVerificacaoTest {

    private static final List<String> ROTAS_PRIVADAS = List.of("/teste/solicitacoes/101", "/teste/documentos/101/401",
            "/teste/solicitacoes", "/teste/admin/indicadores", "/auth/sessao");

    @Test
    void semIdentidadeValidaTodaRotaPrivadaDa401AntesDeQualquerOutraConferencia() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            String referencia = null;
            for (String rota : ROTAS_PRIVADAS) {
                for (Map<String, String> cabecalhos : List.<Map<String, String>>of(Map.of(),
                        Map.of("Authorization", "Bearer token-que-nao-existe-0123456789012345678901234"),
                        Map.of("Authorization", "Basic abc"))) {
                    Resposta r = amb.api.get(rota, cabecalhos);

                    assertEquals(401, r.status(), rota);
                    assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
                    referencia = referencia == null ? r.corpo() : referencia;
                    assertEquals(referencia, r.corpo(), "o 401 nao pode variar por motivo");
                }
            }
            assertEquals(0, amb.recursos.acessos(), "401 nao pode consultar recurso");
        }
    }

    @Test
    void sessaoExpiradaDa401() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            amb.cenario.relogio.avancar(Duration.ofMinutes(31));

            assertEquals(401, amb.chamar("GET", "/teste/solicitacoes/101", amb.tokenA, null).status());
            assertEquals(401, amb.chamar("GET", "/teste/admin/indicadores", amb.tokenAdmin, null).status());
        }
    }

    @Test
    void perfilIndevidoDa403MesmoParaIdInexistente() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            // Administrador em rota de paciente com id que nao existe: 403, nunca 404 (perfil vem antes da dona).
            assertEquals(403, amb.chamar("GET", "/teste/solicitacoes/999999", amb.tokenAdmin, null).status());
            // Paciente em rota administrativa: 403.
            assertEquals(403, amb.chamar("GET", "/teste/admin/indicadores", amb.tokenB, null).status());
        }
    }

    @Test
    void donaVemAntesDaValidacaoDoCorpo() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            // Recurso de B, corpo ilegivel, chamado por A: 404, e nao 400 (nao se sonda existencia pelo corpo).
            Resposta alheio = amb.chamar("PUT", caminho(Tipo.SOLICITACAO, 2), amb.tokenA, "{ruim");
            Resposta inexistente = amb.chamar("PUT", "/teste/solicitacoes/999999", amb.tokenA, "{ruim");
            assertEquals(404, alheio.status());
            assertEquals(inexistente.corpo(), alheio.corpo());

            // Recurso proprio com corpo ruim ou sem o campo exigido: 400.
            assertEquals(400, amb.chamar("PUT", caminho(Tipo.SOLICITACAO, 1), amb.tokenA, "{ruim").status());
            assertEquals(400, amb.chamar("PUT", caminho(Tipo.SOLICITACAO, 1), amb.tokenA, "{}").status());
        }
    }

    @Test
    void metodoNaoPrevistoDa405SoDepoisDeAutenticar() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            assertEquals(401, amb.chamar("POST", "/teste/solicitacoes/101", null, "{}").status());
            assertEquals(405, amb.chamar("POST", "/teste/solicitacoes/101", amb.tokenA, "{}").status());
        }
    }

    @Test
    void rotasPublicasContinuamPublicasERotaInexistenteContinua404() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            assertEquals(200, amb.api.get("/health", Map.of()).status());
            Resposta login = amb.api.postJson("/auth/login",
                    "{\"email\":\"x@exemplo.invalid\",\"senha\":\"errada\"}", Map.of());
            assertEquals("CREDENCIAIS_INVALIDAS", login.json().get("codigo").asText());

            Resposta desconhecida = amb.api.get("/nao/existe", Map.of());
            assertEquals(404, desconhecida.status());
            // O 404 de recurso alheio e o da rota coringa sao a mesma resposta.
            assertEquals(desconhecida.corpo(),
                    amb.chamar("GET", caminho(Tipo.SOLICITACAO, 2), amb.tokenA, null).corpo());
        }
    }

    @Test
    void perfilAlteradoNoBancoValeJaNaChamadaSeguinte() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            assertEquals(200, amb.chamar("GET", caminho(Tipo.SOLICITACAO, 1), amb.tokenA, null).status());

            amb.cenario.usuarios.mudarPerfil(1, Perfil.ADMINISTRADOR);

            // Mesmo token, perfil novo: o perfil e lido da conta a cada requisicao (FR-013).
            assertEquals(403, amb.chamar("GET", caminho(Tipo.SOLICITACAO, 1), amb.tokenA, null).status());
            assertEquals(200, amb.chamar("GET", "/teste/admin/indicadores", amb.tokenA, null).status());
        }
    }

    @Test
    void mensagensDe403E404NaoDependemDoMotivo() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            Resposta paciente403 = amb.chamar("GET", "/teste/admin/indicadores", amb.tokenA, null);
            Resposta admin403 = amb.chamar("GET", caminho(Tipo.ANALISE, 1), amb.tokenAdmin, null);
            assertEquals(paciente403.corpo(), admin403.corpo());

            for (Resposta r : List.of(paciente403, admin403,
                    amb.chamar("GET", caminho(Tipo.ANALISE, 2), amb.tokenA, null))) {
                for (String proibido : List.of("Paciente", "ADMINISTRADOR", "PACIENTE", "SQL", "RC_", CenarioAuth.EMAIL_A,
                        "exception", "at br.com")) {
                    assertFalse(r.corpo().contains(proibido), "mensagem revela: " + proibido);
                }
            }
        }
    }
}
