package br.com.regulacheck.integracao.extracao;

/**
 * O leitor nao conseguiu responder (falha tecnica) ou respondeu fora do contrato. A mensagem nunca traz conteudo
 * de documento nem detalhe do leitor.
 */
public class LeituraIndisponivelException extends Exception {

    private static final long serialVersionUID = 1L;

    /** @param mensagem motivo tecnico curto, sem conteudo de documento */
    public LeituraIndisponivelException(String mensagem) {
        super(mensagem);
    }

    /**
     * @param mensagem motivo tecnico curto, sem conteudo de documento
     * @param causa falha original
     */
    public LeituraIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
