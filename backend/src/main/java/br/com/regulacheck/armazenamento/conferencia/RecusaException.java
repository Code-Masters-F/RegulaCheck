package br.com.regulacheck.armazenamento.conferencia;

/**
 * Sinaliza, dentro da conferencia, que o arquivo deve ser recusado por um motivo da lista fechada.
 * Nao sai do pacote: {@link ConferenciaDeArquivo} a converte em {@link ResultadoConferencia.Recusado}.
 */
final class RecusaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient MotivoRecusa motivo;

    RecusaException(MotivoRecusa motivo) {
        super(motivo.name(), null, false, false);
        this.motivo = motivo;
    }

    MotivoRecusa motivo() {
        return motivo;
    }
}
