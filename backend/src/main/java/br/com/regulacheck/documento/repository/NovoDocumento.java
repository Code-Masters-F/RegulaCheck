package br.com.regulacheck.documento.repository;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.documento.entity.TipoDocumental;

/**
 * Dados de um documento a registrar: tudo o que o Service apurou depois de conferir e guardar o arquivo. Nao
 * contem dono, estado nem revisao: esses valores vem da sessao e do proprio repositorio, sob o bloqueio do pedido.
 *
 * @param nomeArquivo nome de exibicao saneado
 * @param tipoDocumental tipo informado pela paciente
 * @param formato formato real do conteudo
 * @param tamanhoBytes tamanho em bytes
 * @param paginas paginas do PDF; {@code 1} para imagens
 * @param caminhoInterno nome aleatorio do arquivo ja guardado na area privada
 * @param hashConteudo SHA-256 do conteudo em hexadecimal minusculo (64 caracteres)
 * @param chaveEnvio identificacao da tentativa de envio, ou {@code null} se nao foi informada
 */
public record NovoDocumento(String nomeArquivo, TipoDocumental tipoDocumental, TipoArquivo formato,
        long tamanhoBytes, int paginas, IdInterno caminhoInterno, String hashConteudo, String chaveEnvio) {

    @Override
    public String toString() {
        return "NovoDocumento[formato=" + formato + ", tipoDocumental=" + tipoDocumental + "]";
    }
}
