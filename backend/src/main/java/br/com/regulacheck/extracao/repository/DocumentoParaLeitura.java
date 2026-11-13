package br.com.regulacheck.extracao.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.documento.entity.TipoDocumental;

/**
 * Documento disponivel do pedido, com o que e preciso para le-lo.
 *
 * @param documentoId identificador do documento
 * @param tipoDocumental tipo informado no envio
 * @param formato formato real do arquivo
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param arquivo nome interno na area privada; {@code null} nas linhas da carga sintetica (sem arquivo)
 */
public record DocumentoParaLeitura(long documentoId, TipoDocumental tipoDocumental, TipoArquivo formato,
        int paginas, IdInterno arquivo) {

    /** O nome interno nao vai para log por descuido. */
    @Override
    public String toString() {
        return "DocumentoParaLeitura[documentoId=" + documentoId + ", formato=" + formato + "]";
    }
}
