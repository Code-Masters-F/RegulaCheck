package br.com.regulacheck.armazenamento.conferencia;

import br.com.regulacheck.common.exception.ExcecaoNegocio;

/**
 * Limites demonstrativos do MVP para arquivos enviados (CLAUDE.md secao 9). Ficam num unico lugar
 * para que conferencia, documentacao e testes usem os mesmos numeros.
 */
public final class LimitesArquivo {

    /** Arquivos aceitos por pedido. */
    public static final int MAXIMO_ARQUIVOS = 3;

    /** Tamanho maximo de cada arquivo, em bytes (5 MB). */
    public static final long MAXIMO_BYTES = 5L * 1024 * 1024;

    /** Paginas maximas de um PDF. */
    public static final int MAXIMO_PAGINAS_PDF = 5;

    /** Pixels maximos de uma imagem (largura x altura); protege contra imagens que expandem na memoria. */
    public static final long MAXIMO_PIXELS = 25_000_000L;

    /** Tamanho maximo do nome de exibicao, em caracteres. */
    public static final int MAXIMO_NOME_EXIBICAO = 100;

    private LimitesArquivo() {
    }

    /**
     * Confere a quantidade de arquivos de um pedido.
     *
     * @param quantidade numero de arquivos recebidos no pedido
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} se houver mais arquivos que o permitido
     */
    public static void exigirQuantidade(int quantidade) {
        if (quantidade > MAXIMO_ARQUIVOS) {
            throw MotivoRecusa.EXCESSO_DE_ARQUIVOS.excecao();
        }
    }
}
