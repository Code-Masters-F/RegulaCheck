package br.com.regulacheck.common.http;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Entrega um arquivo guardado como <strong>anexo</strong>, com o tipo de conteudo vindo da conferencia
 * (nunca do nome) e cabecalhos que impedem o navegador de reinterpreta-lo.
 *
 * <p>A autorizacao nao acontece aqui: a rota que chama este metodo ja foi declarada em
 * {@code RotasPrivadas} e ja obteve o documento pela busca filtrada pela dona
 * ({@code Propriedade.exigir}). Este metodo so escreve a resposta.
 */
public final class DownloadSeguro {

    private static final int BUFFER = 8 * 1024;

    private DownloadSeguro() {
    }

    /**
     * Escreve o arquivo na resposta.
     *
     * @param resposta resposta HTTP ainda nao confirmada
     * @param tipo tipo real do arquivo (define o {@code Content-Type})
     * @param nomeExibicao nome ja saneado ({@code NomeExibicao}); usado em {@code Content-Disposition}
     * @param tamanhoBytes tamanho do conteudo, para {@code Content-Length}
     * @param conteudo fluxo do arquivo; fechado por este metodo
     * @throws IOException se a escrita na resposta falhar
     */
    public static void enviar(HttpServletResponse resposta, TipoArquivo tipo, String nomeExibicao, long tamanhoBytes,
            InputStream conteudo) throws IOException {
        try (conteudo) {
            resposta.setStatus(HttpServletResponse.SC_OK);
            resposta.setContentType(tipo.mime());
            resposta.setHeader("Content-Disposition", disposicao(nomeExibicao));
            resposta.setHeader("X-Content-Type-Options", "nosniff");
            resposta.setHeader("Cache-Control", "no-store");
            resposta.setContentLengthLong(tamanhoBytes);
            OutputStream saida = resposta.getOutputStream();
            byte[] buffer = new byte[BUFFER];
            int lidos;
            while ((lidos = conteudo.read(buffer)) >= 0) {
                saida.write(buffer, 0, lidos);
            }
        }
    }

    /**
     * Monta {@code Content-Disposition: attachment} com nome ASCII de reserva e {@code filename*} em UTF-8
     * (RFC 5987). O nome ja vem saneado (sem CR/LF); mesmo assim so caracteres seguros chegam ao cabecalho.
     */
    static String disposicao(String nome) {
        String reserva = nome.replaceAll("[^A-Za-z0-9._-]", "_");
        StringBuilder codificado = new StringBuilder();
        for (byte b : nome.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-'
                    || c == '_') {
                codificado.append((char) c);
            } else {
                codificado.append('%').append(String.format("%02X", c));
            }
        }
        return "attachment; filename=\"" + reserva + "\"; filename*=UTF-8''" + codificado;
    }
}
