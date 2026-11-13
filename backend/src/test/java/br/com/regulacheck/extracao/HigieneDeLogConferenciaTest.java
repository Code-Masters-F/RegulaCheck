package br.com.regulacheck.extracao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.divergir;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.SituacaoLeitura;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FR-020: consultar, decidir, repetir, divergir e todas as recusas (400, 404, 409, 422, 503) nao deixam no log nem
 * nas respostas de erro valor extraido, trecho, nome de arquivo, caminho, hash, e-mail, token, motivo digitado ou
 * texto do documento; ficam so identificadores tecnicos, quantidades e codigos, e nunca um stack trace.
 */
class HigieneDeLogConferenciaTest {

    private static final String NOME_SENTINELA = "NOME-SENTINELA-DO-ARQUIVO";
    private static final String VALOR_SENTINELA = "VALOR-SENTINELA-EXTRAIDO";
    private static final String TRECHO_SENTINELA = "TRECHO-SENTINELA-DO-DOCUMENTO";
    private static final String DIGITADO_SENTINELA = "TEXTO-DIGITADO-PELA-PACIENTE";
    private static final ExcecaoNegocio FORA = new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
            "Servico temporariamente indisponivel. Tente novamente em instantes.");

    @Test
    void logsEErrosDaConferenciaNaoTrazemConteudoDoDocumentoNemTextoDigitado() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> respostasDeErro = new ArrayList<>();
        List<String> segredos = new ArrayList<>(List.of(NOME_SENTINELA, VALOR_SENTINELA, TRECHO_SENTINELA,
                DIGITADO_SENTINELA, "Maria Exemplo", EMAIL_A));
        try {
            LeituraDeDocumento comSentinela = new LeituraDeDocumento(SituacaoLeitura.LIDO,
                    java.util.Arrays.stream(CodigoCampo.values()).map(codigo -> codigo == CodigoCampo.NOME
                            ? CampoLido.identificado(codigo, VALOR_SENTINELA, 1, TRECHO_SENTINELA)
                            : codigo == CodigoCampo.DATA_NASCIMENTO ? CampoLido.ilegivel(codigo)
                            : CampoLido.naoLocalizado(codigo)).toList());
            CenarioAuth cenario = new CenarioAuth();
            try (CenarioDocumento c = CenarioDocumento.iniciar(cenario,
                    cenario.dependencias(new LeitorFalso(arquivo -> comSentinela)), true)) {
                String token = c.api.entrar(EMAIL_A, SENHA);
                Map<String, String> auth = bearer(token);
                String pedido = c.criarRascunho(auth);
                String doc = c.enviar(auth, pedido, "OUTRO", NOME_SENTINELA + ".pdf", ArquivosDeTeste.pdf(1), null)
                        .json().get("id").asText();
                Documento registro = c.cenario.documentos.todos().get(0);
                segredos.add(token);
                segredos.add(registro.caminhoInterno().valor());
                segredos.add(registro.hashConteudo());
                segredos.add(c.raiz.toString());
                String extracao = c.lerDocumentos(auth, pedido).json().get("id").asText();

                // sucesso: consultar, decidir, repetir, divergir
                assertEquals(200, c.consultarConferencia(auth, pedido, extracao).status());
                assertEquals(200, c.decidir(auth, pedido, extracao, lote(confirmar(doc, "NOME"))).status());
                assertEquals(200, c.decidir(auth, pedido, extracao, lote(confirmar(doc, "NOME"))).status());
                assertEquals(200, c.decidir(auth, pedido, extracao,
                        lote(divergir(doc, "NOME", "VALOR_DIFERENTE_DO_DOCUMENTO"))).status());

                // recusas
                List<Resposta> recusas = new ArrayList<>();
                recusas.add(c.decidir(auth, pedido, extracao, lote(confirmar(doc, "DATA_NASCIMENTO")))); // 422
                recusas.add(c.decidir(auth, pedido, extracao, lote(confirmar("999999", "NOME")))); // 404
                recusas.add(c.decidir(auth, pedido, extracao, "{\"decisoes\":[{\"documentoId\":\"" + doc
                        + "\",\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\",\"valor\":\"" + DIGITADO_SENTINELA
                        + "\"}]}")); // 400: valor digitado
                recusas.add(c.decidir(auth, pedido, extracao, "{\"decisoes\":[{\"documentoId\":\"" + doc
                        + "\",\"codigo\":\"NOME\",\"decisao\":\"DIVERGENTE\",\"motivo\":\"" + DIGITADO_SENTINELA
                        + "\"}]}")); // 400: motivo livre
                recusas.add(c.decidir(auth, pedido, extracao, "{" + DIGITADO_SENTINELA)); // 400: corpo ilegivel
                recusas.add(c.consultarConferencia(c.authB, pedido, extracao)); // 404 alheio
                assertEquals(List.of(422, 404, 400, 400, 400, 404), recusas.stream().map(Resposta::status).toList());

                // 503: banco fora na consulta e no registro
                c.cenario.conferencias.falharNaProximaConsulta(FORA);
                recusas.add(c.consultarConferencia(auth, pedido, extracao));
                c.cenario.conferencias.falharNoProximoRegistro(FORA);
                recusas.add(c.decidir(auth, pedido, extracao, lote(confirmar(doc, "NOME"))));

                // 409: leitura desatualizada
                c.substituir(auth, pedido, doc, "OUTRO", "outro.pdf", ArquivosDeTeste.pdf(2), null);
                recusas.add(c.decidir(auth, pedido, extracao, lote(confirmar(doc, "NOME"))));
                assertEquals(409, recusas.get(recusas.size() - 1).status());

                for (Resposta recusa : recusas) {
                    respostasDeErro.add(recusa.corpo());
                }
            }
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Conferencia atualizada"), "o registro bem-sucedido deve deixar rastro tecnico");
        for (String segredo : segredos) {
            assertFalse(log.contains(segredo), "o log vazou: " + segredo);
            for (String erro : respostasDeErro) {
                assertFalse(erro.contains(segredo), "resposta de erro vazou: " + segredo);
            }
        }
        assertFalse(log.contains("\tat "), "sem stack trace no log de uma falha esperada");
    }
}
