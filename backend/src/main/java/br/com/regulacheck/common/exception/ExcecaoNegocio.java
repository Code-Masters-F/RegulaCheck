package br.com.regulacheck.common.exception;

import java.util.List;

/**
 * Falha prevista, com status HTTP e codigo de erro estaveis, que deve ser devolvida a quem chamou.
 *
 * <p>A mensagem e exibida a pessoa: nao deve conter detalhes tecnicos nem dados sensiveis.
 */
public class ExcecaoNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String codigo;
    private final transient List<ErroPadrao.Detalhe> detalhes;

    /**
     * @param status status HTTP da resposta
     * @param codigo codigo estavel do erro (ver catalogo em {@code docs/api/erros.md})
     * @param mensagem texto compreensivel para a pessoa
     */
    public ExcecaoNegocio(int status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    /**
     * @param status status HTTP da resposta
     * @param codigo codigo estavel do erro
     * @param mensagem texto compreensivel para a pessoa
     * @param detalhes erros por campo de entrada; {@code null} quando nao se aplica
     */
    public ExcecaoNegocio(int status, String codigo, String mensagem, List<ErroPadrao.Detalhe> detalhes) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.detalhes = detalhes;
    }

    /** @return erros por campo de entrada, ou {@code null} quando nao se aplica */
    public List<ErroPadrao.Detalhe> detalhes() {
        return detalhes;
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
