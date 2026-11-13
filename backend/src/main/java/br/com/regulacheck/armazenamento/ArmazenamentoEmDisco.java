package br.com.regulacheck.armazenamento;

import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Area privada em disco: um diretorio-raiz (da configuracao {@code DOCUMENT_STORAGE_PATH}) onde cada
 * arquivo e gravado com o nome de um {@link IdInterno}, sem extensao e sem o nome original.
 *
 * <p>Gravacao atomica: o conteudo vai para um temporario <em>dentro da raiz</em> e so entao e renomeado
 * ({@code ATOMIC_MOVE}), o que evita arquivo parcial. A leitura recusa links simbolicos e qualquer caminho
 * que nao fique dentro da raiz. Sem raiz configurada o objeto existe, mas {@code guardar} falha com erro
 * claro (a API continua iniciando).
 */
public final class ArmazenamentoEmDisco implements ArmazenamentoPrivado {

    private static final Logger LOG = LoggerFactory.getLogger(ArmazenamentoEmDisco.class);

    private final Path raiz;

    /**
     * @param raiz diretorio-raiz da area privada; {@code null} indica "nao configurada"
     */
    public ArmazenamentoEmDisco(Path raiz) {
        this.raiz = raiz == null ? null : raiz.toAbsolutePath().normalize();
    }

    @Override
    public ArquivoGuardado guardar(ArquivoConferido arquivo) {
        if (raiz == null) {
            arquivo.descartar();
            throw indisponivel();
        }
        Path temporario = null;
        try {
            Files.createDirectories(raiz);
            Path raizReal = raiz.toRealPath();
            IdInterno id = IdInterno.novo();
            temporario = Files.createTempFile(raizReal, ".gravando-", ".tmp");
            Files.copy(arquivo.caminhoTemporario(), temporario, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporario, raizReal.resolve(id.valor()), StandardCopyOption.ATOMIC_MOVE);
            temporario = null;
            return new ArquivoGuardado(id, arquivo.tipo(), arquivo.tamanhoBytes(), arquivo.paginas(),
                    arquivo.nomeExibicao());
        } catch (IOException falha) {
            LOG.error("Falha ao guardar arquivo na area privada. tipo={}", falha.getClass().getSimpleName());
            throw indisponivel();
        } finally {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException e) {
                    LOG.warn("Nao foi possivel remover temporario de gravacao. tipo={}",
                            e.getClass().getSimpleName());
                }
            }
            arquivo.descartar();
        }
    }

    @Override
    public InputStream abrir(IdInterno id) {
        Path arquivo = localizar(id);
        try {
            return Files.newInputStream(arquivo, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new ArquivoAusenteException();
        }
    }

    @Override
    public void remover(IdInterno id) {
        if (raiz == null) {
            return;
        }
        try {
            Files.deleteIfExists(localizar(id));
        } catch (ArquivoAusenteException e) {
            // Idempotente: nada a remover.
        } catch (IOException e) {
            LOG.warn("Nao foi possivel remover arquivo. tipo={}", e.getClass().getSimpleName());
        }
    }

    /**
     * Resolve o identificador dentro da raiz e confere que o resultado e um arquivo comum (nao link) que
     * realmente esta dentro dela.
     */
    private Path localizar(IdInterno id) {
        if (raiz == null) {
            throw new ArquivoAusenteException();
        }
        try {
            Path raizReal = raiz.toRealPath();
            Path candidato = raizReal.resolve(id.valor());
            if (!candidato.normalize().startsWith(raizReal)
                    || !Files.isRegularFile(candidato, LinkOption.NOFOLLOW_LINKS)
                    || !candidato.toRealPath().startsWith(raizReal)) {
                throw new ArquivoAusenteException();
            }
            return candidato;
        } catch (IOException e) {
            throw new ArquivoAusenteException();
        }
    }

    private static ExcecaoNegocio indisponivel() {
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "O armazenamento de arquivos está indisponível. Tente novamente mais tarde.");
    }
}
