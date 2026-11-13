package br.com.regulacheck.documento;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta, so para testes, um corpo {@code multipart/form-data} com partes de texto e de arquivo sinteticos.
 * Nenhum conteudo aqui e documento de pessoa real.
 */
public final class MultipartTeste {

    private final String limite = "----regulacheck" + UUID.randomUUID().toString().replace("-", "");
    private final List<byte[]> partes = new ArrayList<>();

    /** Adiciona um campo de texto. */
    public MultipartTeste texto(String nome, String valor) {
        partes.add(montar("Content-Disposition: form-data; name=\"" + nome + "\"", null,
                valor.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    /** Adiciona uma parte de arquivo com o tipo de conteudo declarado pelo cliente (que a API nunca usa). */
    public MultipartTeste arquivo(String nome, String nomeArquivo, String tipoDeclarado, byte[] conteudo) {
        partes.add(montar("Content-Disposition: form-data; name=\"" + nome + "\"; filename=\"" + nomeArquivo + "\"",
                tipoDeclarado, conteudo));
        return this;
    }

    /** @return valor do cabecalho {@code Content-Type} correspondente ao corpo */
    public String contentType() {
        return "multipart/form-data; boundary=" + limite;
    }

    /** @return corpo completo, terminado pelo delimitador final */
    public byte[] corpo() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        for (byte[] parte : partes) {
            saida.writeBytes(("--" + limite + "\r\n").getBytes(StandardCharsets.US_ASCII));
            saida.writeBytes(parte);
            saida.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        saida.writeBytes(("--" + limite + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        return saida.toByteArray();
    }

    private static byte[] montar(String disposicao, String tipo, byte[] conteudo) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.writeBytes((disposicao + "\r\n").getBytes(StandardCharsets.UTF_8));
        if (tipo != null) {
            saida.writeBytes(("Content-Type: " + tipo + "\r\n").getBytes(StandardCharsets.US_ASCII));
        }
        saida.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
        saida.writeBytes(conteudo);
        return saida.toByteArray();
    }
}
