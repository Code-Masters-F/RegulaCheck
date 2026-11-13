package br.com.regulacheck.validacao.model;

import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import java.util.List;
import java.util.Objects;

/**
 * O que precisa de atencao em uma regra pendente ou nao verificavel, para a paciente saber o que fazer e para a
 * explicacao simulada (RC-14) ter o que resumir. Nao diagnostica, nao define urgencia e nao promete vaga.
 *
 * @param codigoRegra codigo da regra na versao
 * @param descricaoRegra descricao da regra na versao
 * @param tipo {@code PENDENTE} ou {@code NAO_VERIFICAVEL}
 * @param explicacao mensagem da regra na versao (pendente) ou mensagem padrao de nao verificavel do protocolo
 * @param motivo motivo de nao verificavel; {@code null} quando pendente
 * @param evidencias campos considerados (documento, campo, situacao, origem)
 * @param responsavel quem provavelmente corrige: o da regra (pendente) ou a paciente (nao verificavel)
 */
public record Pendencia(String codigoRegra, String descricaoRegra, Resultado tipo, String explicacao,
        MotivoNaoVerificavel motivo, List<Evidencia> evidencias, ResponsavelCorrecao responsavel) {

    /** Valida o tipo e copia as evidencias. */
    public Pendencia {
        if (!Objects.requireNonNull(tipo, "tipo").geraPendencia()) {
            throw new IllegalArgumentException("Pendencia so para regra pendente ou nao verificavel.");
        }
        Objects.requireNonNull(explicacao, "explicacao");
        Objects.requireNonNull(responsavel, "responsavel");
        evidencias = List.copyOf(evidencias);
    }
}
