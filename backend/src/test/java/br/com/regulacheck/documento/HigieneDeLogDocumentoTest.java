package br.com.regulacheck.documento;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-022 / SC-007: enviar, repetir, recusar, listar, baixar e falhar nas rotas de documentos nao deixa no log
 * (nem nas respostas de erro) nome do arquivo, identificacao de tentativa, hash, nome interno, caminho de
 * armazenamento, e-mail, nome da paciente, token nem conteudo; ficam so identificadores tecnicos e codigos.
 */
class HigieneDeLogDocumentoTest {

    private static final String NOME_SENTINELA = "NOME-SENTINELA-DO-ARQUIVO";
    private static final String CHAVE_SENTINELA = "chave-sentinela-que-nao-vaza";

    @Test
    void logsEErrosDasRotasDeDocumentoNaoTrazemDadoDaPacienteNemDoArquivo() throws Exception {
        final byte[] pdf = ArquivosDeTeste.pdf(1);
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String token;
        String raiz;
        String nomeInterno = null;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            raiz = c.raiz.toString();
            token = c.api.entrar(EMAIL_A, SENHA);
            Map<String, String> auth = bearer(token);
            String pedido = c.criarRascunho(auth);

            Resposta primeiro = c.enviar(auth, pedido, "ENCAMINHAMENTO", NOME_SENTINELA + ".pdf", pdf, CHAVE_SENTINELA);
            assertEquals(201, primeiro.status());
            assertEquals(200, c.enviar(auth, pedido, "ENCAMINHAMENTO", NOME_SENTINELA + ".pdf", pdf,
                    CHAVE_SENTINELA).status());
            // Recusas e conflitos: tipo invalido, arquivo falso, chave reutilizada, pedido inexistente.
            assertEquals(400, c.enviar(auth, pedido, NOME_SENTINELA, "a.pdf", pdf, null).status());
            assertEquals(415, c.enviar(auth, pedido, "OUTRO", NOME_SENTINELA + ".pdf", ArquivosDeTeste.texto(),
                    null).status());
            Resposta conflito = c.enviar(auth, pedido, "OUTRO", NOME_SENTINELA + ".pdf", pdf, CHAVE_SENTINELA);
            assertEquals(409, conflito.status());
            assertEquals(404, c.enviar(auth, "999999", "OUTRO", NOME_SENTINELA + ".pdf", pdf, null).status());
            // Falha inesperada no registro.
            c.cenario.documentos.falharNoProximoRegistro(new IllegalStateException("falha injetada pelo teste"));
            Resposta erroInterno = c.enviar(auth, pedido, "OUTRO", "x.pdf", ArquivosDeTeste.pdf(2), null);
            assertEquals(500, erroInterno.status());
            // Falha de dependencia no registro.
            c.cenario.documentos.falharNoProximoRegistro(new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes."));
            c.enviar(auth, pedido, "OUTRO", "y.pdf", ArquivosDeTeste.pdf(3), null);

            c.api.get("/solicitacoes/" + pedido + "/documentos", auth);
            String docId = primeiro.json().get("id").asText();
            c.api.getBinario("/solicitacoes/" + pedido + "/documentos/" + docId, auth);
            c.api.get("/solicitacoes/" + pedido + "/documentos/999999", auth);
            nomeInterno = c.cenario.documentos.todos().get(0).caminhoInterno().valor();

            for (Resposta resposta : new Resposta[] {conflito, erroInterno}) {
                assertFalse(resposta.corpo().contains(CHAVE_SENTINELA), "chave na resposta de erro");
                assertFalse(resposta.corpo().contains(raiz), "caminho na resposta de erro");
                assertFalse(resposta.corpo().contains(nomeInterno), "nome interno na resposta de erro");
            }
        } finally {
            System.setErr(original);
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Envio repetido reconhecido") && log.contains("Recurso nao encontrado"),
                "controle positivo: a captura de log funcionou");
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(pdf));
        for (String proibido : new String[] {NOME_SENTINELA, CHAVE_SENTINELA, nomeInterno, raiz, EMAIL_A, "Paciente A",
                token, SENHA, hash}) {
            assertFalse(log.contains(proibido), "vazou no log: " + proibido.substring(0, Math.min(8, proibido.length())));
        }
    }
}
