package br.com.regulacheck.armazenamento;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;

/**
 * Resultado de guardar um arquivo na area privada: o que a historia de envio grava em
 * {@code RC_DOCUMENTO} ({@code CAMINHO_INTERNO} = {@link #id()}, {@code TIPO}, {@code TAMANHO_BYTES},
 * {@code PAGINAS}, {@code NOME_ORIGINAL} = {@link #nomeExibicao()}).
 *
 * @param id identificador interno aleatorio (nunca o nome enviado)
 * @param tipo tipo real do conteudo
 * @param tamanhoBytes tamanho em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param nomeExibicao nome original saneado, somente para exibicao
 */
public record ArquivoGuardado(IdInterno id, TipoArquivo tipo, long tamanhoBytes, int paginas, String nomeExibicao) {
}
