package br.com.regulacheck.common.exception;

/**
 * Falha prevista, com status HTTP e codigo de erro estaveis, que deve ser devolvida a quem chamou.
 *
 * <p>A mensagem e exibida a pessoa: nao deve conter detalhes tecnicos nem dados sensiveis.
 */
public class ExcecaoNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String codigo;

    /**
     * @param status status HTTP da resposta
     * @param codigo codigo estavel do erro (ver catalogo em {@code docs/api/erros.md})
     * @param mensagem texto compreensivel para a pessoa
     */
    public ExcecaoNegocio(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    /** @return status HTTP da resposta */
    public int status() {
        return status;
    }

    /** @return codigo estavel do erro */
    public String codigo() {
        return codigo;
    }
}
