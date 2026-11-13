package br.com.regulacheck.documento.entity;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import java.time.Instant;

/**
 * Arquivo anexado a um pedido, ja lido do banco. Modelo interno: nunca e serializado como resposta publica
 * (usar {@code DocumentoResponse}).
 *
 * <p>{@code caminhoInterno}, {@code hashConteudo} e {@code chaveEnvio} existem so para o Repository e o Service:
 * o primeiro e o nome aleatorio na area privada, o segundo e o SHA-256 do conteudo e o terceiro e a identificacao
 * da tentativa de envio. Por isso {@link #toString()} os omite, para que nao vazem em log por descuido.
 *
 * @param id identificador do documento
 * @param solicitacaoId pedido ao qual o documento pertence
 * @param nomeArquivo nome de exibicao ja saneado (nunca o nome de armazenamento)
 * @param tipoDocumental tipo informado pela paciente
 * @param formato formato real, identificado pelo conteudo
 * @param tamanhoBytes tamanho em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param revisaoEntrada revisao do pedido em que o documento entrou
 * @param enviadoEm instante do envio, em UTC
 * @param caminhoInterno nome aleatorio do arquivo na area privada; {@code null} nas linhas da carga sintetica
 *     ({@code 02_}), que apontam para arquivos ficticios que nao existem em disco
 * @param hashConteudo SHA-256 do conteudo em hexadecimal minusculo; {@code null} em linhas anteriores a RC-08
 * @param chaveEnvio identificacao da tentativa de envio; {@code null} quando o envio nao informou chave
 */
public record Documento(long id, long solicitacaoId, String nomeArquivo, TipoDocumental tipoDocumental,
        TipoArquivo formato, long tamanhoBytes, int paginas, int revisaoEntrada, Instant enviadoEm,
        IdInterno caminhoInterno, String hashConteudo, String chaveEnvio) {

    /** @return o estado do envio: um documento registrado e sempre {@link EstadoEnvio#ENVIADO} */
    public EstadoEnvio estadoEnvio() {
        return EstadoEnvio.ENVIADO;
    }

    @Override
    public String toString() {
        return "Documento[id=" + id + ", solicitacaoId=" + solicitacaoId + ", formato=" + formato
                + ", tipoDocumental=" + tipoDocumental + ", revisaoEntrada=" + revisaoEntrada + "]";
    }
}
