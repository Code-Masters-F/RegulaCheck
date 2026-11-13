package br.com.regulacheck.armazenamento.conferencia;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Confere uma imagem JPEG/PNG lendo apenas o <strong>cabecalho</strong> (largura e altura), sem
 * decodificar os pixels, e verifica se o arquivo termina onde o formato manda.
 *
 * <p>Ler so o cabecalho evita a "bomba de descompressao": uma imagem de poucos KB pode declarar
 * dimensoes que exigiriam gigabytes de memoria. A checagem do final do arquivo (marcador {@code IEND}
 * do PNG, {@code FF D9} do JPEG) e uma heuristica barata para recusar arquivos truncados; ela nao
 * prova que todos os pixels sao validos.
 */
final class LeitorDeImagem {

    private static final byte[] FIM_PNG = {0, 0, 0, 0, 'I', 'E', 'N', 'D', (byte) 0xAE, 0x42, 0x60, (byte) 0x82};
    private static final int JANELA_FIM_JPEG = 64;

    private LeitorDeImagem() {
    }

    /**
     * @param arquivo imagem em quarentena
     * @param tipo tipo identificado pela assinatura ({@link TipoArquivo#JPEG} ou {@link TipoArquivo#PNG})
     * @throws RecusaException {@code ARQUIVO_ILEGIVEL} se o cabecalho nao puder ser lido, o formato do leitor
     *     divergir da assinatura, as dimensoes excederem o limite ou o arquivo estiver truncado
     */
    static void validar(Path arquivo, TipoArquivo tipo) {
        try {
            if (!terminaCorretamente(arquivo, tipo)) {
                throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
            }
            try (ImageInputStream fluxo = ImageIO.createImageInputStream(arquivo.toFile())) {
                if (fluxo == null) {
                    throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
                }
                Iterator<ImageReader> leitores = ImageIO.getImageReaders(fluxo);
                if (!leitores.hasNext()) {
                    throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
                }
                ImageReader leitor = leitores.next();
                try {
                    leitor.setInput(fluxo, true, true);
                    if (!formatoCorresponde(leitor.getFormatName(), tipo)) {
                        throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
                    }
                    long largura = leitor.getWidth(0);
                    long altura = leitor.getHeight(0);
                    if (largura <= 0 || altura <= 0 || largura * altura > LimitesArquivo.MAXIMO_PIXELS) {
                        throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
                    }
                } finally {
                    leitor.dispose();
                }
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof RecusaException recusa) {
                throw recusa;
            }
            throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
        }
    }

    private static boolean formatoCorresponde(String formato, TipoArquivo tipo) {
        String f = formato.toLowerCase(Locale.ROOT);
        return switch (tipo) {
            case PNG -> f.equals("png");
            case JPEG -> f.equals("jpeg") || f.equals("jpg");
            default -> false;
        };
    }

    private static boolean terminaCorretamente(Path arquivo, TipoArquivo tipo) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(arquivo.toFile(), "r")) {
            long tamanho = f.length();
            if (tipo == TipoArquivo.PNG) {
                if (tamanho < FIM_PNG.length) {
                    return false;
                }
                byte[] fim = new byte[FIM_PNG.length];
                f.seek(tamanho - FIM_PNG.length);
                f.readFully(fim);
                return java.util.Arrays.equals(fim, FIM_PNG);
            }
            int janela = (int) Math.min(tamanho, JANELA_FIM_JPEG);
            byte[] fim = new byte[janela];
            f.seek(tamanho - janela);
            f.readFully(fim);
            for (int i = fim.length - 2; i >= 0; i--) {
                if ((fim[i] & 0xFF) == 0xFF && (fim[i + 1] & 0xFF) == 0xD9) {
                    return true;
                }
            }
            return false;
        }
    }
}
