package br.com.regulacheck.armazenamento.conferencia;

import br.com.regulacheck.armazenamento.NomeExibicao;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decide se um arquivo recebido pode ser aceito: tipo real pelo conteudo, extensao correspondente,
 * tamanho, paginas (PDF) e dimensoes (imagem). Nada fica gravado quando o arquivo e recusado.
 *
 * <p>O fluxo e copiado para uma <strong>quarentena</strong> contando bytes; ao passar de
 * {@link LimitesArquivo#MAXIMO_BYTES} a copia para, sem confiar em {@code Content-Length}. So um arquivo
 * aceito permanece la, e quem o recebe deve guarda-lo ({@code ArmazenamentoPrivado}) ou
 * {@link ArquivoConferido#descartar() descarta-lo}. Instancias sao thread-safe (sem estado por pedido).
 */
public final class ConferenciaDeArquivo {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenciaDeArquivo.class);
    private static final int BUFFER = 8 * 1024;

    private final Path quarentena;

    /**
     * @param quarentena diretorio privado para cópias temporarias; criado se nao existir
     */
    public ConferenciaDeArquivo(Path quarentena) {
        this.quarentena = quarentena;
    }

    /**
     * Cria a conferencia com a quarentena adequada: {@code <raiz>/.quarentena} quando ha area de arquivos
     * configurada e, sem ela, um diretorio temporario privado criado agora (a conferencia funciona mesmo
     * sem armazenamento configurado).
     *
     * @param raizArmazenamento raiz da area privada, ou {@code null}
     * @return conferencia pronta para uso
     * @throws IOException se o diretorio de quarentena nao puder ser criado
     */
    public static ConferenciaDeArquivo para(Path raizArmazenamento) throws IOException {
        Path diretorio = raizArmazenamento == null
                ? Files.createTempDirectory("regulacheck-quarentena-")
                : raizArmazenamento.resolve(".quarentena");
        Files.createDirectories(diretorio);
        return new ConferenciaDeArquivo(diretorio);
    }

    /** @return pasta privada das copias temporarias (usada tambem como pasta temporaria do multipart) */
    public Path quarentena() {
        return quarentena;
    }

    /**
     * Confere um arquivo recebido.
     *
     * @param fluxo conteudo do arquivo; lido no maximo ate o limite de tamanho e nao fechado aqui
     * @param nomeOriginal nome informado por quem enviou (usado so para checar a extensao e gerar o nome de
     *     exibicao); pode ser {@code null}
     * @return {@link ResultadoConferencia.Aceito} com o arquivo em quarentena, ou
     *     {@link ResultadoConferencia.Recusado} com o motivo; em recusa nada permanece gravado
     */
    public ResultadoConferencia conferir(InputStream fluxo, String nomeOriginal) {
        Path temporario = null;
        boolean aceito = false;
        try {
            Files.createDirectories(quarentena);
            temporario = Files.createTempFile(quarentena, "conf-", ".tmp");
            long tamanho = copiarComLimite(fluxo, temporario);
            if (tamanho > LimitesArquivo.MAXIMO_BYTES) {
                return recusar(MotivoRecusa.TAMANHO_EXCEDIDO);
            }
            if (tamanho == 0) {
                return recusar(MotivoRecusa.ARQUIVO_ILEGIVEL);
            }
            TipoArquivo tipo = tipoReal(temporario);
            if (!tipo.aceitaExtensao(nomeOriginal)) {
                return recusar(MotivoRecusa.EXTENSAO_INCOMPATIVEL);
            }
            int paginas = 1;
            if (tipo == TipoArquivo.PDF) {
                paginas = LeitorDePaginas.contar(temporario);
                if (paginas > LimitesArquivo.MAXIMO_PAGINAS_PDF) {
                    return recusar(MotivoRecusa.EXCESSO_DE_PAGINAS);
                }
                if (paginas < 1) {
                    return recusar(MotivoRecusa.ARQUIVO_ILEGIVEL);
                }
            } else {
                LeitorDeImagem.validar(temporario, tipo);
            }
            aceito = true;
            return new ResultadoConferencia.Aceito(
                    new ArquivoConferido(temporario, tipo, tamanho, paginas, NomeExibicao.sanear(nomeOriginal)));
        } catch (RecusaException recusa) {
            return recusar(recusa.motivo());
        } catch (IOException | RuntimeException falha) {
            // Nunca registra o conteudo do arquivo nem o nome enviado: so a classe da falha.
            LOG.warn("Falha ao conferir arquivo. tipo={}", falha.getClass().getSimpleName());
            return recusar(MotivoRecusa.ARQUIVO_ILEGIVEL);
        } finally {
            if (!aceito && temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException e) {
                    LOG.warn("Nao foi possivel remover temporario da quarentena. tipo={}",
                            e.getClass().getSimpleName());
                }
            }
        }
    }

    private static ResultadoConferencia recusar(MotivoRecusa motivo) {
        return new ResultadoConferencia.Recusado(motivo);
    }

    /**
     * Copia ate o limite mais um byte; devolve o total lido (maior que o limite significa excedido).
     */
    private static long copiarComLimite(InputStream origem, Path destino) throws IOException {
        long total = 0;
        byte[] buffer = new byte[BUFFER];
        try (OutputStream saida = Files.newOutputStream(destino)) {
            while (total <= LimitesArquivo.MAXIMO_BYTES) {
                long restante = LimitesArquivo.MAXIMO_BYTES + 1 - total;
                int lidos = origem.read(buffer, 0, (int) Math.min(buffer.length, restante));
                if (lidos < 0) {
                    break;
                }
                saida.write(buffer, 0, lidos);
                total += lidos;
            }
        }
        return total;
    }

    private static TipoArquivo tipoReal(Path arquivo) throws IOException {
        byte[] inicio = new byte[TipoArquivo.BYTES_DE_ASSINATURA];
        int lidos;
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            lidos = entrada.readNBytes(inicio, 0, inicio.length);
        }
        Optional<TipoArquivo> tipo = TipoArquivo.porAssinatura(Arrays.copyOf(inicio, lidos));
        return tipo.orElseThrow(() -> new RecusaException(MotivoRecusa.TIPO_NAO_SUPORTADO));
    }
}
