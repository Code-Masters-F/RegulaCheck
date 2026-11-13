package br.com.regulacheck.armazenamento.conferencia;

import java.nio.file.Path;

/**
 * Arquivo que passou na conferencia e ainda esta na area de quarentena, aguardando ser guardado
 * (ou descartado). Quem recebe este objeto e responsavel por chamar
 * {@code ArmazenamentoPrivado.guardar} ou {@link #descartar()}.
 *
 * @param caminhoTemporario arquivo na quarentena; nao e o local definitivo
 * @param tipo tipo real identificado pelo conteudo
 * @param tamanhoBytes tamanho real, em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param nomeExibicao nome original saneado, apenas para exibicao
 */
public record ArquivoConferido(Path caminhoTemporario, TipoArquivo tipo, long tamanhoBytes, int paginas,
        String nomeExibicao) {

    /** Remove o arquivo da quarentena sem falhar se ele ja nao existir. */
    public void descartar() {
        try {
            java.nio.file.Files.deleteIfExists(caminhoTemporario);
        } catch (java.io.IOException e) {
            // Falha ao limpar nao muda o resultado; o arquivo temporario fica na quarentena privada.
            org.slf4j.LoggerFactory.getLogger(ArquivoConferido.class)
                    .warn("Nao foi possivel remover arquivo temporario da quarentena. tipo={}",
                            e.getClass().getSimpleName());
        }
    }
}
