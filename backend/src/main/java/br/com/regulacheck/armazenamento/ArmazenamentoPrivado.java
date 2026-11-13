package br.com.regulacheck.armazenamento;

import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import java.io.InputStream;

/**
 * Contrato da area privada de arquivos: fora de qualquer diretorio servido publicamente, com nome
 * interno gerado pelo sistema. Quem chama nunca informa caminho.
 *
 * <p>Fluxo de uso (ver {@code docs/api/protecoes.md}): conferir ({@code ConferenciaDeArquivo}), guardar,
 * gravar a linha de {@code RC_DOCUMENTO} com o {@link IdInterno} e, se a gravacao da linha falhar,
 * {@link #remover remover} o arquivo para nao deixar arquivo sem registro.
 */
public interface ArmazenamentoPrivado {

    /**
     * Guarda um arquivo ja conferido, de forma atomica: ou o arquivo final existe completo, ou nada
     * permanece gravado. Ao terminar (com sucesso ou nao) a copia de quarentena e descartada.
     *
     * @param arquivo arquivo aceito pela conferencia, ainda em quarentena
     * @return dados a registrar no banco
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 {@code DEPENDENCIA_INDISPONIVEL} se a
     *     area de arquivos nao estiver configurada ou nao puder ser gravada
     */
    ArquivoGuardado guardar(ArquivoConferido arquivo);

    /**
     * Abre o conteudo de um arquivo guardado. Quem chama deve fechar o fluxo.
     *
     * @param id identificador interno (formato ja validado por {@link IdInterno})
     * @return fluxo de leitura do conteudo
     * @throws ArquivoAusenteException se o arquivo nao existir na area privada
     */
    InputStream abrir(IdInterno id);

    /**
     * Remove um arquivo guardado; nao falha se ele ja nao existir.
     *
     * @param id identificador interno
     */
    void remover(IdInterno id);
}
