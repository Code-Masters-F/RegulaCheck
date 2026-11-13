package br.com.regulacheck.armazenamento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.RotasDeArquivoDeExemplo.Documento;
import br.com.regulacheck.armazenamento.RotasDeArquivoDeExemplo.Documentos;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.armazenamento.conferencia.ResultadoConferencia;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.ServidorDeTeste.RespostaBinaria;
import br.com.regulacheck.config.Configuracao;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Download autenticado nas cinco identidades, com a regra de perfis e dona da RC-05 (SC-003). */
class DownloadAutenticadoTest {

    private static final String CHAVE = "chave-de-teste-ficticia";

    @TempDir
    Path base;

    CenarioAuth cenario;
    Documentos documentos;
    byte[] conteudoA;
    ServidorDeTeste api;
    String tokenA;
    String tokenB;
    String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        Path raiz = base.resolve("area-privada");
        ArmazenamentoEmDisco armazenamento = new ArmazenamentoEmDisco(raiz);
        ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
        documentos = new Documentos();
        conteudoA = ArquivosDeTeste.pdf(2);
        documentos.adicionar(new Documento(10, 1, guardar(armazenamento, conferencia, conteudoA, "laudo da A.pdf")));
        documentos.adicionar(new Documento(20, 2, guardar(armazenamento, conferencia, ArquivosDeTeste.png(), "foto B.png")));
        // Registrado para a paciente A, mas o arquivo nao esta mais na area privada.
        ArquivoGuardado sumido = guardar(armazenamento, conferencia, ArquivosDeTeste.pdf(1), "sumido.pdf");
        armazenamento.remover(sumido.id());
        documentos.adicionar(new Documento(30, 1, sumido));

        cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(),
                rotas -> RotasDeArquivoDeExemplo.registrar(rotas, documentos, armazenamento));
        tokenA = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);
        tokenB = api.entrar(CenarioAuth.EMAIL_B, CenarioAuth.SENHA);
        tokenAdmin = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);
    }

    @AfterEach
    void encerrar() throws Exception {
        api.close();
    }

    private static ArquivoGuardado guardar(ArmazenamentoEmDisco armazenamento, ConferenciaDeArquivo conferencia,
            byte[] conteudo, String nome) {
        ResultadoConferencia r = conferencia.conferir(new ByteArrayInputStream(conteudo), nome);
        return armazenamento.guardar(assertInstanceOf(ResultadoConferencia.Aceito.class, r).arquivo());
    }

    @Test
    void donaRecebeOArquivoComCabecalhosSeguros() throws Exception {
        RespostaBinaria r = api.getBinario("/teste/arquivos/10", ServidorDeTeste.bearer(tokenA));
        assertEquals(200, r.status());
        assertArrayEquals(conteudoA, r.corpo());
        assertEquals("application/pdf", r.cabecalho("Content-Type"));
        assertTrue(r.cabecalho("Content-Disposition").startsWith("attachment;"), r.cabecalho("Content-Disposition"));
        assertTrue(r.cabecalho("Content-Disposition").contains("filename*=UTF-8''laudo%20da%20A.pdf"));
        assertEquals("nosniff", r.cabecalho("X-Content-Type-Options"));
        assertEquals("no-store", r.cabecalho("Cache-Control"));
        assertEquals(String.valueOf(conteudoA.length), r.cabecalho("Content-Length"));
    }

    @Test
    void outraPacienteRecebeOMesmo404DeDocumentoInexistente() throws Exception {
        Resposta alheio = api.get("/teste/arquivos/10", ServidorDeTeste.bearer(tokenB));
        Resposta inexistente = api.get("/teste/arquivos/999", ServidorDeTeste.bearer(tokenB));
        assertEquals(404, alheio.status());
        assertEquals(404, inexistente.status());
        assertEquals("NAO_ENCONTRADO", alheio.json().get("codigo").asText());
        assertEquals(inexistente.json().get("mensagem").asText(), alheio.json().get("mensagem").asText());
        assertEquals(inexistente.json().get("codigo").asText(), alheio.json().get("codigo").asText());
        assertFalse(alheio.corpo().contains("laudo"), "nada do arquivo no corpo do erro");
    }

    @Test
    void administradorRecebe403AntesDeQualquerConsultaAoRepositorio() throws Exception {
        int antes = documentos.acessos();
        Resposta r = api.get("/teste/arquivos/10", ServidorDeTeste.bearer(tokenAdmin));
        assertEquals(403, r.status());
        assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
        assertEquals(antes, documentos.acessos(), "403 deve ocorrer antes de tocar no repositorio");
    }

    @Test
    void simuladorRecebe403() throws Exception {
        Resposta r = api.get("/teste/arquivos/10", Map.of("X-Chave-Simulador", CHAVE));
        assertEquals(403, r.status());
    }

    @Test
    void semTokenOuComTokenInvalidoRecebe401() throws Exception {
        assertEquals(401, api.get("/teste/arquivos/10", Map.of()).status());
        assertEquals(401, api.get("/teste/arquivos/10", ServidorDeTeste.bearer("token-invalido")).status());
    }

    @Test
    void nenhumaRotaServeAAreaDeArquivosPorEndereco() throws Exception {
        for (String caminho : new String[] {"/arquivos/10", "/uploads/10", "/storage/10", "/.quarentena/x",
                "/../etc/passwd", "/teste/arquivos", "/teste/arquivos/"}) {
            Resposta r = api.get(caminho, ServidorDeTeste.bearer(tokenA));
            assertTrue(r.status() == 404 || r.status() == 400, caminho + " -> " + r.status());
            assertFalse(r.corpo().contains("%PDF"), caminho);
        }
    }

    @Test
    void arquivoAusenteDaAreaGeraErroInternoSemCaminho() throws Exception {
        Resposta r = api.get("/teste/arquivos/30", ServidorDeTeste.bearer(tokenA));
        assertEquals(500, r.status());
        assertEquals("ERRO_INTERNO", r.json().get("codigo").asText());
        assertFalse(r.corpo().contains(base.toString()));
        assertFalse(r.corpo().contains("area-privada"));
        assertTrue(r.json().has("idRastreamento"));
    }
}
