package br.com.regulacheck.armazenamento.conferencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PedidoComVariosArquivosTest {

    @TempDir
    Path raiz;

    @Test
    void tresValidosPassamEOQuartoEhRecusadoPelaQuantidade() throws Exception {
        ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
        List<ArquivoConferido> aceitos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            aceitos.add(assertInstanceOf(ResultadoConferencia.Aceito.class,
                    conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.pdf(1)), "doc" + i + ".pdf"))
                    .arquivo());
        }
        LimitesArquivo.exigirQuantidade(aceitos.size());
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> LimitesArquivo.exigirQuantidade(4));
        assertEquals("No máximo 3 arquivos por pedido.", erro.detalhes().get(0).mensagem());
        aceitos.forEach(ArquivoConferido::descartar);
    }

    @Test
    void recusadoNoMeioNaoImpedeOsDemaisMasNaoFicaGravado() throws Exception {
        ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
        ResultadoConferencia a = conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.pdf(1)), "a.pdf");
        ResultadoConferencia b = conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.texto()), "b.pdf");
        ResultadoConferencia c = conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.png()), "c.png");
        assertInstanceOf(ResultadoConferencia.Aceito.class, a);
        assertInstanceOf(ResultadoConferencia.Recusado.class, b);
        assertInstanceOf(ResultadoConferencia.Aceito.class, c);
        try (var arquivos = Files.list(raiz.resolve(".quarentena"))) {
            assertEquals(2, arquivos.count(), "so os dois aceitos permanecem");
        }
    }

    @Test
    void envioSimultaneoComMesmoNomeNaoColide() throws Exception {
        ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<ArquivoConferido>> futuros = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                Callable<ArquivoConferido> tarefa = () -> assertInstanceOf(ResultadoConferencia.Aceito.class,
                        conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.pdf(1)), "mesmo-nome.pdf"))
                        .arquivo();
                futuros.add(pool.submit(tarefa));
            }
            Set<Path> caminhos = new HashSet<>();
            for (Future<ArquivoConferido> f : futuros) {
                caminhos.add(f.get().caminhoTemporario());
            }
            assertEquals(16, caminhos.size());
            assertTrue(caminhos.stream().allMatch(Files::exists));
        } finally {
            pool.shutdownNow();
        }
    }
}
