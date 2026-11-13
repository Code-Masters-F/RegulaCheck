package br.com.regulacheck.extracao;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
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
 * FR-024 / SC-008: nem sucesso, nem leitura vazia, nem falha do leitor, nem saida invalida deixam no log ou nas
 * respostas de erro valor extraido, trecho, nome do arquivo, hash, nome interno, caminho, e-mail, token ou texto do
 * documento; ficam so identificadores tecnicos e codigos.
 */
class HigieneDeLogExtracaoTest {

    private static final String NOME_SENTINELA = "NOME-SENTINELA-DO-ARQUIVO";
    private static final String VALOR_SENTINELA = "VALOR-SENTINELA-EXTRAIDO";
    private static final String TRECHO_SENTINELA = "TRECHO-SENTINELA-DO-DOCUMENTO";

    @Test
    void logsEErrosDaLeituraNaoTrazemConteudoDoDocumentoNemDadoDaPaciente() throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        List<String> respostasDeErro = new ArrayList<>();
        List<String> segredos = new ArrayList<>(List.of(NOME_SENTINELA, VALOR_SENTINELA, TRECHO_SENTINELA,
                "Maria Exemplo", "Avaliacao cardiologica", EMAIL_A));
        try {
            // 1) Sucesso com valores e trechos sentinela; 2) leitura vazia; 3) protegido; 4) falha; 5) invalida.
            LeituraDeDocumento comSentinela = new LeituraDeDocumento(SituacaoLeitura.LIDO,
                    java.util.Arrays.stream(CodigoCampo.values()).map(codigo -> codigo == CodigoCampo.NOME
                            ? CampoLido.identificado(codigo, VALOR_SENTINELA, 1, TRECHO_SENTINELA)
                            : CampoLido.naoLocalizado(codigo)).toList());
            for (int caso = 0; caso < 5; caso++) {
                final int atual = caso;
                LeitorFalso leitor = new LeitorFalso(arquivo -> switch (atual) {
                    case 0 -> comSentinela;
                    case 1 -> LeituraDeDocumento.vazia();
                    case 2 -> LeituraDeDocumento.protegido();
                    case 3 -> LeitorFalso.falhar();
                    default -> new LeituraDeDocumento(SituacaoLeitura.LIDO, List.of(new CampoLido(CodigoCampo.NOME,
                            br.com.regulacheck.integracao.extracao.SituacaoCampo.ILEGIVEL, VALOR_SENTINELA, null,
                            TRECHO_SENTINELA)));
                });
                CenarioAuth cenario = new CenarioAuth();
                try (CenarioDocumento c = CenarioDocumento.iniciar(cenario, cenario.dependencias(leitor), true)) {
                    String token = c.api.entrar(EMAIL_A, SENHA);
                    Map<String, String> auth = bearer(token);
                    String pedido = c.criarRascunho(auth);
                    c.enviar(auth, pedido, "OUTRO", NOME_SENTINELA + ".pdf", ArquivosDeTeste.pdf(1), null);
                    Documento doc = c.cenario.documentos.todos().get(0);
                    segredos.add(token);
                    segredos.add(doc.caminhoInterno().valor());
                    segredos.add(doc.hashConteudo());
                    segredos.add(c.raiz.toString());

                    Resposta r = c.lerDocumentos(auth, pedido);
                    assertEquals(caso == 3 || caso == 4 ? 503 : 201, r.status());
                    if (r.status() >= 400) {
                        respostasDeErro.add(r.corpo());
                    }
                    // Um segundo POST (repeticao) e uma consulta tambem nao podem vazar.
                    c.lerDocumentos(auth, pedido);
                    c.consultarLeitura(auth, pedido);
                    // Pedido alheio: 404 tambem sem dado.
                    respostasDeErro.add(c.lerDocumentos(c.authB, pedido).corpo());
                }
            }
        } finally {
            System.setErr(original);
        }
        String log = buffer.toString(StandardCharsets.UTF_8);
        for (String segredo : segredos) {
            assertFalse(log.contains(segredo), "o log vazou: " + segredo);
            for (String erro : respostasDeErro) {
                assertFalse(erro.contains(segredo), "resposta de erro vazou: " + segredo);
            }
        }
        assertFalse(log.contains("	at "), "sem stack trace no log de uma falha esperada");
    }
}
