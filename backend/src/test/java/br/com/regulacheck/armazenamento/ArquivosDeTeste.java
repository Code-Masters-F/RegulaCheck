package br.com.regulacheck.armazenamento;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;

/**
 * Gera, em memoria, arquivos 100% sinteticos para os testes das protecoes: validos, invalidos e
 * disfarçados. Nenhum e documento de pessoa real.
 */
public final class ArquivosDeTeste {

    private ArquivosDeTeste() {
    }

    /** @return PDF valido com o numero de paginas pedido */
    public static byte[] pdf(int paginas) {
        try (PDDocument documento = new PDDocument()) {
            for (int i = 0; i < paginas; i++) {
                documento.addPage(new PDPage());
            }
            return salvar(documento);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return PDF de 1 pagina protegido por senha de abertura */
    public static byte[] pdfProtegido() {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            StandardProtectionPolicy politica = new StandardProtectionPolicy("dono-ficticio", "abrir-ficticio",
                    new AccessPermission());
            politica.setEncryptionKeyLength(128);
            documento.protect(politica);
            return salvar(documento);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return PDF de 1 pagina com acao JavaScript ao abrir e um anexo embutido (conteudo ativo) */
    public static byte[] pdfComConteudoAtivo() {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            documento.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('nao deve executar');"));
            PDComplexFileSpecification anexo = new PDComplexFileSpecification();
            anexo.setFile("anexo.txt");
            anexo.setEmbeddedFile(new PDEmbeddedFile(documento,
                    new ByteArrayInputStream("conteudo sintetico".getBytes(StandardCharsets.UTF_8))));
            PDEmbeddedFilesNameTreeNode arvore = new PDEmbeddedFilesNameTreeNode();
            arvore.setNames(Map.of("anexo.txt", anexo));
            PDDocumentNameDictionary nomes = new PDDocumentNameDictionary(documento.getDocumentCatalog());
            nomes.setEmbeddedFiles(arvore);
            documento.getDocumentCatalog().setNames(nomes);
            return salvar(documento);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return PDF valido cortado no meio (truncado) */
    public static byte[] pdfTruncado() {
        byte[] inteiro = pdf(2);
        return Arrays.copyOf(inteiro, inteiro.length / 2);
    }

    /** @return PNG valido pequeno */
    public static byte[] png() {
        return imagem("png");
    }

    /** @return JPEG valido pequeno */
    public static byte[] jpeg() {
        return imagem("jpg");
    }

    /** @return PNG valido cortado antes do fim (sem o marcador IEND) */
    public static byte[] pngTruncado() {
        byte[] inteiro = png();
        return Arrays.copyOf(inteiro, inteiro.length - 6);
    }

    /** @return PNG minimo cujo cabecalho declara dimensoes enormes (bomba de descompressao) */
    public static byte[] pngDimensaoExtrema() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        ByteBuffer ihdr = ByteBuffer.allocate(13);
        ihdr.putInt(60_000).putInt(60_000).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        bloco(saida, "IHDR", ihdr.array());
        bloco(saida, "IDAT", new byte[] {0x78, (byte) 0x9C, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01});
        bloco(saida, "IEND", new byte[0]);
        return saida.toByteArray();
    }

    /** @return texto simples (nao e PDF, JPEG nem PNG) */
    public static byte[] texto() {
        return "Isto e apenas um texto sintetico.".getBytes(StandardCharsets.UTF_8);
    }

    /** @return bytes que comecam como um executavel do Windows ("MZ") */
    public static byte[] executavel() {
        byte[] bytes = new byte[256];
        bytes[0] = 'M';
        bytes[1] = 'Z';
        return bytes;
    }

    /**
     * @return PDF valido de 1 pagina com o tamanho exato pedido: espacos de enchimento seguidos de um
     *     novo marcador {@code %%EOF} no final
     */
    public static byte[] pdfComTamanho(int tamanho) {
        byte[] base = pdf(1);
        byte[] fim = "\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        byte[] resultado = new byte[Math.max(tamanho, base.length + fim.length)];
        System.arraycopy(base, 0, resultado, 0, base.length);
        Arrays.fill(resultado, base.length, resultado.length - fim.length, (byte) ' ');
        System.arraycopy(fim, 0, resultado, resultado.length - fim.length, fim.length);
        return resultado;
    }

    /**
     * Fluxo "infinito" que comeca como PDF e nunca termina; usado para provar que a leitura para no limite
     * em vez de confiar no tamanho declarado.
     */
    public static InputStream fluxoInfinitoPdf() {
        return new InputStream() {
            private final byte[] inicio = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
            private long posicao;

            @Override
            public int read() {
                if (posicao < inicio.length) {
                    return inicio[(int) posicao++];
                }
                return 0;
            }
        };
    }

    private static byte[] imagem(String formato) {
        BufferedImage imagem = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagem, formato, saida);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return saida.toByteArray();
    }

    private static byte[] salvar(PDDocument documento) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        documento.save(saida);
        return saida.toByteArray();
    }

    private static void bloco(ByteArrayOutputStream saida, String tipo, byte[] dados) {
        byte[] nome = tipo.getBytes(StandardCharsets.US_ASCII);
        saida.writeBytes(ByteBuffer.allocate(4).putInt(dados.length).array());
        saida.writeBytes(nome);
        saida.writeBytes(dados);
        CRC32 crc = new CRC32();
        crc.update(nome);
        crc.update(dados);
        saida.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
