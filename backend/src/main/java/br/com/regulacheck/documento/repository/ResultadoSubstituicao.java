package br.com.regulacheck.documento.repository;

import br.com.regulacheck.documento.entity.Documento;

/**
 * Desfecho da substituicao de um documento (ver {@link DocumentoRepository#substituir}). Reaproveita o
 * {@link ResultadoRegistro} do envio (mesmas situacoes, menos {@code LIMITE}, que nao se aplica porque sai um
 * documento e entra outro) e acrescenta o documento que saiu, para o Service apagar o arquivo dele depois do
 * commit.
 *
 * @param registro desfecho; em {@code REGISTRADO}, o documento e o <em>novo</em>
 * @param substituido documento que deixou de estar disponivel; {@code null} se nada foi substituido
 */
public record ResultadoSubstituicao(ResultadoRegistro registro, Documento substituido) {

    /** @return resultado sem alteracao: pedido ou documento do caminho nao encontrado */
    public static ResultadoSubstituicao naoEncontrado() {
        return new ResultadoSubstituicao(ResultadoRegistro.naoEncontrado(), null);
    }

    /** @return resultado sem alteracao: pedido submetido em simulacao */
    public static ResultadoSubstituicao submetido() {
        return new ResultadoSubstituicao(ResultadoRegistro.submetido(), null);
    }

    /** @return resultado sem alteracao: chave de envio reutilizada com outro conteudo ou tipo */
    public static ResultadoSubstituicao conflitoChave() {
        return new ResultadoSubstituicao(ResultadoRegistro.conflitoChave(), null);
    }

    /**
     * @param existente documento ja registrado com essa chave
     * @param versao revisao atual do pedido
     * @param disponiveis documentos disponiveis do pedido
     * @return repeticao da mesma tentativa, sem gravar nada
     */
    public static ResultadoSubstituicao repetido(Documento existente, int versao, int disponiveis) {
        return new ResultadoSubstituicao(ResultadoRegistro.repetido(existente, versao, disponiveis), null);
    }

    /**
     * @param novo documento recem-gravado
     * @param antigo documento que saiu
     * @param versao revisao do pedido apos a substituicao
     * @param disponiveis documentos disponiveis apos a substituicao (igual a antes)
     * @return substituicao aplicada
     */
    public static ResultadoSubstituicao substituido(Documento novo, Documento antigo, int versao, int disponiveis) {
        return new ResultadoSubstituicao(ResultadoRegistro.registrado(novo, versao, disponiveis), antigo);
    }
}
