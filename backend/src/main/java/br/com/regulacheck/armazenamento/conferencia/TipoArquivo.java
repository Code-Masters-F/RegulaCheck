package br.com.regulacheck.armazenamento.conferencia;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tipos de arquivo aceitos, identificados pelos primeiros bytes do conteudo (a extensao e o
 * {@code Content-Type} informados por quem envia nao decidem o tipo).
 */
public enum TipoArquivo {

    /** Documento PDF ({@code %PDF-}). */
    PDF("application/pdf", Set.of("pdf"), new int[] {0x25, 0x50, 0x44, 0x46, 0x2D}),

    /** Imagem JPEG (inicio {@code FF D8 FF}). */
    JPEG("image/jpeg", Set.of("jpg", "jpeg"), new int[] {0xFF, 0xD8, 0xFF}),

    /** Imagem PNG (assinatura de 8 bytes). */
    PNG("image/png", Set.of("png"), new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    /** Quantidade de bytes iniciais necessaria para reconhecer qualquer tipo aceito. */
    public static final int BYTES_DE_ASSINATURA = 8;

    private final String mime;
    private final Set<String> extensoes;
    private final int[] assinatura;

    TipoArquivo(String mime, Set<String> extensoes, int[] assinatura) {
        this.mime = mime;
        this.extensoes = extensoes;
        this.assinatura = assinatura;
    }

    /** @return tipo de conteudo ({@code Content-Type}) a usar quando o arquivo for devolvido */
    public String mime() {
        return mime;
    }

    /**
     * Reconhece o tipo pelos bytes iniciais.
     *
     * @param inicio primeiros bytes do arquivo (ate {@link #BYTES_DE_ASSINATURA})
     * @return o tipo, ou vazio se o conteudo nao for PDF, JPEG nem PNG
     */
    public static Optional<TipoArquivo> porAssinatura(byte[] inicio) {
        for (TipoArquivo tipo : values()) {
            if (tipo.comecaCom(inicio)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }

    /**
     * Confere se a extensao do nome enviado corresponde a este tipo.
     *
     * @param nomeOriginal nome informado por quem enviou; pode ser {@code null}
     * @return {@code true} se a ultima extensao (sem diferenciar caixa) e uma das deste tipo
     */
    public boolean aceitaExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return false;
        }
        int ponto = nomeOriginal.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeOriginal.length() - 1) {
            return false;
        }
        return extensoes.contains(nomeOriginal.substring(ponto + 1).trim().toLowerCase(Locale.ROOT));
    }

    private boolean comecaCom(byte[] inicio) {
        if (inicio.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if ((inicio[i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
