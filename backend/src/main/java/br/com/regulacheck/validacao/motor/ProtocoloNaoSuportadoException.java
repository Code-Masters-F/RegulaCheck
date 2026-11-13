package br.com.regulacheck.validacao.motor;

/**
 * A versao do protocolo nao pode ser aplicada pelo motor; nenhuma regra e avaliada e nenhum resultado parcial e
 * produzido (FR-006 da RC-12). E um problema de configuracao do protocolo, nao da paciente: quem grava analises
 * (RC-13) o trata como falha de analise. A mensagem traz so o motivo e o codigo da regra, nunca dado de documento.
 */
public class ProtocoloNaoSuportadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Por que a versao foi recusada. */
    public enum Motivo {
        /** A versao nao tem regra alguma (avaliar seria afirmar "sem pendencias" sem conferir nada). */
        SEM_REGRAS,
        /** Uma regra da versao nao tem implementacao no motor. */
        REGRA_DESCONHECIDA,
        /** A natureza cadastrada (obrigatoria/condicional) difere da que o motor implementa para o codigo. */
        NATUREZA_DIVERGENTE,
        /** O mesmo codigo aparece duas vezes na versao. */
        REGRA_REPETIDA
    }

    private final Motivo motivo;
    private final String codigoRegra;

    /**
     * @param motivo por que a versao foi recusada
     * @param codigoRegra codigo da regra envolvida; {@code null} em {@link Motivo#SEM_REGRAS}
     */
    public ProtocoloNaoSuportadoException(Motivo motivo, String codigoRegra) {
        super("Versao do protocolo nao suportada pelo motor: motivo=" + motivo + " regra=" + codigoRegra);
        this.motivo = motivo;
        this.codigoRegra = codigoRegra;
    }

    /** @return por que a versao foi recusada */
    public Motivo motivo() {
        return motivo;
    }

    /** @return codigo da regra envolvida, ou {@code null} */
    public String codigoRegra() {
        return codigoRegra;
    }
}
