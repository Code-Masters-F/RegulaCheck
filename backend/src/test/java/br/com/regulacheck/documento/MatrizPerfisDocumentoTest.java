package br.com.regulacheck.documento;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-020: matriz identidade x rota das rotas de documentos (modelo: {@code MatrizPerfisSolicitacaoTest}).
 * Administrador e simulador nunca chegam ao pedido ou ao documento (403, inclusive para id inexistente); anonimo
 * recebe 401; metodos nao previstos recebem 405 com {@code Allow}; subcaminhos desconhecidos recebem o 404 padrao.
 */
class MatrizPerfisDocumentoTest {

    static final String CHAVE = "chave-ficticia-do-simulador-0123456789";

    private record Chamada(String metodo, String caminho) {
    }

    private static List<Chamada> chamadas() {
        return List.of(new Chamada("POST", "/solicitacoes/1/documentos"),
                new Chamada("GET", "/solicitacoes/1/documentos"), new Chamada("GET", "/solicitacoes/1/documentos/1"),
                new Chamada("POST", "/solicitacoes/999999/documentos"),
                new Chamada("GET", "/solicitacoes/999999/documentos/999999"));
    }

    @Test
    void anonimoRecebe401EmTodasAsRotasDeDocumento() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            for (Chamada chamada : chamadas()) {
                Resposta r = c.api.enviar(chamada.metodo(), chamada.caminho(), null, Map.of());
                assertEquals(401, r.status(), chamada.metodo() + " " + chamada.caminho());
                assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
            }
            assertEquals(0, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void administradorESimuladorRecebem403SemTocarEmNada() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            Map<String, String> admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
            Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);
            Map<String, String> authA = bearer(api.entrar(EMAIL_A, SENHA));
            api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, authA);

            for (Chamada chamada : chamadas()) {
                for (Map<String, String> identidade : List.of(admin, simulador)) {
                    Resposta r = api.enviar(chamada.metodo(), chamada.caminho(), null, identidade);
                    assertEquals(403, r.status(), chamada.metodo() + " " + chamada.caminho());
                    assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
                }
            }
            assertEquals(1, cenario.solicitacoes.buscarDaPaciente(1, 1).orElseThrow().revisaoAtual());
            assertEquals(0, cenario.documentos.todos().size());
        }
    }

    @Test
    void metodosNaoPrevistosRecebem405ComAllowCorreto() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String colecao = "/solicitacoes/" + pedido + "/documentos";
            String item = colecao + "/1";

            for (String metodo : List.of("PUT", "DELETE", "PATCH")) {
                Resposta naColecao = c.api.enviar(metodo, colecao, null, c.authA);
                assertEquals(405, naColecao.status(), metodo + " " + colecao);
                assertTrue(naColecao.cabecalho("Allow").contains("GET")
                        && naColecao.cabecalho("Allow").contains("POST"), naColecao.cabecalho("Allow"));
                Resposta noItem = c.api.enviar(metodo, item, null, c.authA);
                assertEquals(405, noItem.status(), metodo + " " + item);
            }
            // POST num documento: so GET e permitido.
            Resposta postNoItem = c.api.enviar("POST", item, null, c.authA);
            assertEquals(405, postNoItem.status());
            assertEquals("GET", postNoItem.cabecalho("Allow"));
            assertEquals(0, c.cenario.documentos.todos().size());
        }
    }

    @Test
    void subcaminhosDesconhecidosOuMalFormadosRecebemOMesmo404PadraoSemGravarNada() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);

            for (String caminho : List.of("/solicitacoes/" + pedido + "/documentos/abc",
                    "/solicitacoes/abc/documentos", "/solicitacoes/" + pedido + "/documentos/1/x",
                    "/solicitacoes/" + pedido + "/documentos/0", "/solicitacoes/" + pedido + "/documentos/-1",
                    "/solicitacoes/" + pedido + "/outra-coisa", "/solicitacoes/0/documentos")) {
                Resposta r = c.api.get(caminho, c.authA);
                assertEquals(404, r.status(), caminho);
                assertEquals("NAO_ENCONTRADO", r.json().get("codigo").asText(), caminho);
            }
            Resposta envio = c.enviar(c.authA, "abc", "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null);
            assertEquals(404, envio.status());
            assertEquals(0, c.arquivosGuardados());
        }
    }
}
