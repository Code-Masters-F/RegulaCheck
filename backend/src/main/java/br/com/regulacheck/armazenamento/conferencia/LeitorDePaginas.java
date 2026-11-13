package br.com.regulacheck.armazenamento.conferencia;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

/**
 * Conta as paginas de um PDF e detecta PDF protegido, usando o PDFBox somente para <strong>leitura</strong>.
 *
 * <p>A contagem vem da arvore de paginas do proprio documento; contar ocorrencias de {@code /Type /Page}
 * no texto seria facil de falsificar. Nenhum recurso de renderizacao, extracao de texto, JavaScript ou
 * anexo e usado: o conteudo ativo do arquivo nunca e executado nem aberto.
 */
final class LeitorDePaginas {

    /** Bytes finais examinados em busca do marcador {@code %%EOF}. */
    private static final int JANELA_FIM = 1024;

    private LeitorDePaginas() {
    }

    /**
     * @param arquivo PDF em quarentena
     * @return numero de paginas
     * @throws RecusaException {@code PDF_PROTEGIDO} se exigir senha ou estiver criptografado;
     *     {@code ARQUIVO_ILEGIVEL} se estiver corrompido ou truncado
     */
    static int contar(Path arquivo) {
        // O PDFBox reconstroi PDFs sem o final (modo de reparo) e poderia aceitar um arquivo cortado;
        // um PDF completo termina com o marcador %%EOF, entao a ausencia dele indica truncamento.
        if (!terminaComMarcadorDeFim(arquivo)) {
            throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
        }
        try (PDDocument documento = Loader.loadPDF(new RandomAccessReadBufferedFile(arquivo.toFile()))) {
            // Mesmo sem senha de abertura, PDF criptografado pode ter restricoes que travam as etapas
            // seguintes; a orientacao a pessoa e a mesma: enviar uma versao sem protecao.
            if (documento.isEncrypted()) {
                throw new RecusaException(MotivoRecusa.PDF_PROTEGIDO);
            }
            return documento.getNumberOfPages();
        } catch (InvalidPasswordException e) {
            throw new RecusaException(MotivoRecusa.PDF_PROTEGIDO);
        } catch (IOException e) {
            throw new RecusaException(MotivoRecusa.ARQUIVO_ILEGIVEL);
        }
    }

    private static boolean terminaComMarcadorDeFim(Path arquivo) {
        try (RandomAccessFile f = new RandomAccessFile(arquivo.toFile(), "r")) {
            int janela = (int) Math.min(f.length(), JANELA_FIM);
            byte[] fim = new byte[janela];
            f.seek(f.length() - janela);
            f.readFully(fim);
            return new String(fim, StandardCharsets.ISO_8859_1).contains("%%EOF");
        } catch (IOException e) {
            return false;
        }
    }
}
