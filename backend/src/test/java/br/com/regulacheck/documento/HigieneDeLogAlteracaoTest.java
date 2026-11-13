package br.com.regulacheck.documento;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArmazenamentoEmDisco;
import br.com.regulacheck.armazenamento.ArmazenamentoPrivado;
import br.com.regulacheck.armazenamento.ArquivoGuardado;
import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.auth.RelogioFixo;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.dto.EnvioDocumento;
import br.com.regulacheck.documento.service.DocumentoService;
import br.com.regulacheck.documento.service.DocumentoService.ResultadoEnvio;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import br.com.regulacheck.usuario.entity.Perfil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FR-022 / SC-008: remover, substituir e excluir (sucesso, repeticao, recusa, conflito, falha) nao deixam no log nem
 * nas respostas de erro nome do arquivo, identificacao de tentativa, hash, nome interno, caminho de armazenamento,
 * e-mail, token nem o valor de confirmacao; ficam so identificadores tecnicos e codigos. Cobre tambem o aviso de
 * arquivo que nao pode ser apagado depois do commit (falha injetada no armazenamento).
 */
class HigieneDeLogAlteracaoTest {

    private static final String NOME_SENTINELA = "NOME-SENTINELA-DO-ARQUIVO";
    private static final String CHAVE_SENTINELA = "chave-sentinela-que-nao-vaza";
    private static final String CONFIRMACAO_SENTINELA = "CONFIRMACAO-SENTINELA-ERRADA";

    @Test
    void logsEErrosDeRemoverSubstituirEExcluirNaoTrazemDadoDaPacienteNemDoArquivo() throws Exception {
        final byte[] pdf = ArquivosDeTeste.pdf(1);
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String token;
        String raiz;
        String nomeInterno;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            raiz = c.raiz.toString();
            token = c.api.entrar(EMAIL_A, SENHA);
            Map<String, String> auth = bearer(token);
            String pedido = c.criarRascunho(auth);
            Resposta primeiro = c.enviar(auth, pedido, "OUTRO", NOME_SENTINELA + ".pdf", pdf, null);
            String doc = primeiro.json().get("id").asText();
            nomeInterno = c.cenario.documentos.todos().get(0).caminhoInterno().valor();

            // Substituir: sucesso, repeticao, conflito de chave, arquivo recusado, documento inexistente.
            byte[] novo = ArquivosDeTeste.pdf(2);
            Resposta troca = c.substituir(auth, pedido, doc, "OUTRO", NOME_SENTINELA + ".pdf", novo, CHAVE_SENTINELA);
            assertEquals(201, troca.status());
            String docNovo = troca.json().get("id").asText();
            assertEquals(200, c.substituir(auth, pedido, doc, "OUTRO", NOME_SENTINELA + ".pdf", novo,
                    CHAVE_SENTINELA).status());
            Resposta conflito = c.substituir(auth, pedido, docNovo, "OUTRO", NOME_SENTINELA + ".pdf",
                    ArquivosDeTeste.pdf(3), CHAVE_SENTINELA);
            assertEquals(409, conflito.status());
            assertEquals(415, c.substituir(auth, pedido, docNovo, "OUTRO", NOME_SENTINELA + ".pdf",
                    ArquivosDeTeste.texto(), null).status());
            assertEquals(404, c.substituir(auth, pedido, "999999", "OUTRO", NOME_SENTINELA + ".pdf", novo,
                    null).status());
            // Remover: sucesso, repeticao e falhas injetadas (erro interno e dependencia indisponivel).
            assertEquals(200, c.remover(auth, pedido, docNovo).status());
            assertEquals(404, c.remover(auth, pedido, docNovo).status());
            String outro = c.enviar(auth, pedido, "OUTRO", "x.pdf", ArquivosDeTeste.pdf(4), null).json()
                    .get("id").asText();
            c.cenario.documentos.falharNaProximaAlteracao(new IllegalStateException("falha injetada pelo teste"));
            Resposta erroInterno = c.remover(auth, pedido, outro);
            assertEquals(500, erroInterno.status());
            c.cenario.documentos.falharNaProximaAlteracao(new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes."));
            c.remover(auth, pedido, outro);
            // Excluir: confirmacao errada, confirmacao certa, de novo (404).
            Resposta semConfirmar = c.excluir(auth, pedido, CONFIRMACAO_SENTINELA);
            assertEquals(400, semConfirmar.status());
            assertEquals(204, c.excluir(auth, pedido, "EXCLUIR").status());
            assertEquals(404, c.excluir(auth, pedido, "EXCLUIR").status());

            for (Resposta resposta : new Resposta[] {conflito, erroInterno, semConfirmar}) {
                assertFalse(resposta.corpo().contains(CHAVE_SENTINELA), "chave na resposta de erro");
                assertFalse(resposta.corpo().contains(raiz), "caminho na resposta de erro");
                assertFalse(resposta.corpo().contains(nomeInterno), "nome interno na resposta de erro");
                assertFalse(resposta.corpo().contains("Exception"), "pilha na resposta de erro");
            }
        } finally {
            System.setErr(original);
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Envio repetido reconhecido") && log.contains("Recurso nao encontrado"),
                "controle positivo: a captura de log funcionou");
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(pdf));
        for (String proibido : new String[] {NOME_SENTINELA, CHAVE_SENTINELA, CONFIRMACAO_SENTINELA, nomeInterno,
                raiz, EMAIL_A, "Paciente A", token, SENHA, hash}) {
            assertFalse(log.contains(proibido), "vazou no log: " + proibido.substring(0, Math.min(8, proibido.length())));
        }
    }

    /** Armazenamento cujo {@code remover} falha: simula o disco recusando o apagamento depois do commit. */
    private static final class ArmazenamentoQueNaoApaga implements ArmazenamentoPrivado {
        private final ArmazenamentoPrivado real;

        ArmazenamentoQueNaoApaga(ArmazenamentoPrivado real) {
            this.real = real;
        }

        @Override
        public ArquivoGuardado guardar(ArquivoConferido arquivo) {
            return real.guardar(arquivo);
        }

        @Override
        public InputStream abrir(IdInterno id) {
            return real.abrir(id);
        }

        @Override
        public void remover(IdInterno id) {
            throw new IllegalStateException("disco recusou o apagamento de " + id.valor());
        }
    }

    @Test
    void falhaAoApagarOArquivoDepoisDoCommitNaoDesfazAMudancaENaoVazaONomeInterno() throws Exception {
        Path raiz = Files.createTempDirectory("regulacheck-higiene-");
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String nomeInterno;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
            protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
            SolicitacaoRepositoryMemoria pedidos = new SolicitacaoRepositoryMemoria();
            DocumentoRepositoryMemoria documentos = new DocumentoRepositoryMemoria(pedidos);
            RelogioFixo relogio = new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z"));
            ArmazenamentoPrivado armazenamento = new ArmazenamentoQueNaoApaga(new ArmazenamentoEmDisco(raiz));
            SolicitacaoService solicitacoes = new SolicitacaoService(new ProtocoloService(protocolos), pedidos,
                    armazenamento, relogio);
            DocumentoService servico = new DocumentoService(solicitacoes, documentos, armazenamento,
                    ConferenciaDeArquivo.para(raiz), relogio);
            SessaoAutenticada a = new SessaoAutenticada(10, 1, "A", Perfil.PACIENTE,
                    Instant.parse("2026-11-12T22:00:00Z"));
            long pedido = solicitacoes.criar(a, "PROTOCOLO-ACADEMICO-FICTICIO-v0.1").id();
            ResultadoEnvio d1 = servico.enviar(a, pedido, EnvioDocumento.validar("OUTRO", null, "a.pdf",
                    new ByteArrayInputStream(ArquivosDeTeste.pdf(1))));
            ResultadoEnvio d2 = servico.enviar(a, pedido, EnvioDocumento.validar("OUTRO", null, "b.pdf",
                    new ByteArrayInputStream(ArquivosDeTeste.pdf(2))));
            nomeInterno = d1.documento().caminhoInterno().valor();

            // Remover e substituir continuam validos mesmo que o disco recuse apagar o arquivo antigo.
            assertEquals(4, servico.remover(a, pedido, d1.documento().id()).versaoConjuntoDocumental());
            ResultadoEnvio troca = servico.substituir(a, pedido, d2.documento().id(), EnvioDocumento.validar("OUTRO",
                    null, "n.pdf", new ByteArrayInputStream(ArquivosDeTeste.pdf(3))));
            assertFalse(troca.repetido());
            // Excluir tambem: o pedido some mesmo que o disco recuse apagar os arquivos.
            solicitacoes.excluirRascunho(a, pedido);
            assertTrue(pedidos.buscarDaPaciente(pedido, 1).isEmpty());
        } finally {
            System.setErr(original);
            try (Stream<Path> arvore = Files.walk(raiz)) {
                arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }

        String log = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("nao pode ser apagado") && log.contains("nao puderam ser apagados"),
                "controle positivo: os avisos de arquivo remanescente foram registrados");
        assertFalse(log.contains(nomeInterno), "o nome interno do arquivo nao vai ao log");
        assertFalse(log.contains(raiz.toString()), "o caminho de armazenamento nao vai ao log");
        assertFalse(log.contains("disco recusou"), "a mensagem da excecao (com o nome interno) nao vai ao log");
    }
}
