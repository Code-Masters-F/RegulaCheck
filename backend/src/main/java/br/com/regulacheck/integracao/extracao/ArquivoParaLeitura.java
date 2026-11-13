package br.com.regulacheck.integracao.extracao;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;

/**
 * Arquivo entregue a um {@link LeitorDeDocumento}. Existe so em memoria, durante a leitura.
 *
 * @param tipo formato real do arquivo, ja conferido no envio
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param conteudo bytes do arquivo (dado nao confiavel)
 */
public record ArquivoParaLeitura(TipoArquivo tipo, int paginas, byte[] conteudo) {

    /** O conteudo nunca vai para log por descuido. */
    @Override
    public String toString() {
        return "ArquivoParaLeitura[tipo=" + tipo + ", paginas=" + paginas + "]";
    }
}
