package br.com.regulacheck.validacao.model;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de uma regra para uma entrada, com o motivo (so quando nao verificavel) e as evidencias consideradas.
 *
 * @param resultado um dos quatro resultados
 * @param motivo motivo de nao verificavel; {@code null} nos demais resultados
 * @param evidencias campos considerados para decidir (pode ser vazio quando nao ha documento a apontar)
 */
public record Veredito(Resultado resultado, MotivoNaoVerificavel motivo, List<Evidencia> evidencias) {

    /** Valida a invariante: motivo presente se, e somente se, nao verificavel. */
    public Veredito {
        Objects.requireNonNull(resultado, "resultado");
        if ((resultado == Resultado.NAO_VERIFICAVEL) != (motivo != null)) {
            throw new IllegalArgumentException("Motivo so existe em resultado nao verificavel.");
        }
        evidencias = List.copyOf(evidencias);
    }

    /**
     * @param evidencias evidencias consideradas
     * @return veredito {@code ATENDE}
     */
    public static Veredito atende(List<Evidencia> evidencias) {
        return new Veredito(Resultado.ATENDE, null, evidencias);
    }

    /**
     * @param evidencias evidencias consideradas
     * @return veredito {@code PENDENTE}
     */
    public static Veredito pendente(List<Evidencia> evidencias) {
        return new Veredito(Resultado.PENDENTE, null, evidencias);
    }

    /**
     * @param motivo por que nao foi possivel verificar
     * @param evidencias evidencias consideradas
     * @return veredito {@code NAO_VERIFICAVEL}
     */
    public static Veredito naoVerificavel(MotivoNaoVerificavel motivo, List<Evidencia> evidencias) {
        return new Veredito(Resultado.NAO_VERIFICAVEL, Objects.requireNonNull(motivo, "motivo"), evidencias);
    }

    /**
     * @param evidencias evidencias consideradas
     * @return veredito {@code NAO_APLICAVEL}
     */
    public static Veredito naoAplicavel(List<Evidencia> evidencias) {
        return new Veredito(Resultado.NAO_APLICAVEL, null, evidencias);
    }
}
