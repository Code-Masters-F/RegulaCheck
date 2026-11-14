package br.com.regulacheck.analise.entity;

import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Resultado;
import java.util.List;
import java.util.Objects;

/**
 * Pendencia de uma analise gravada (uma linha de {@code RC_PENDENCIA} com suas evidencias), como o motor a produziu
 * na RC-12. Nao diagnostica, nao define urgencia e nao promete vaga.
 *
 * @param codigoRegra codigo da regra na versao do protocolo da analise
 * @param descricaoRegra descricao da regra naquela versao
 * @param tipo {@code PENDENTE} ou {@code NAO_VERIFICAVEL}
 * @param explicacao mensagem gravada (a da regra, se pendente; a padrao do protocolo, se nao verificavel)
 * @param motivo motivo do nao verificavel; {@code null} se pendente ou se a pendencia veio da carga sintetica
 * @param responsavel quem provavelmente corrige
 * @param evidencias evidencias na ordem do motor; vazia nas pendencias da carga sintetica
 */
public record PendenciaGravada(String codigoRegra, String descricaoRegra, Resultado tipo, String explicacao,
        MotivoNaoVerificavel motivo, ResponsavelCorrecao responsavel, List<EvidenciaGravada> evidencias) {

    /** Exige os campos obrigatorios e copia as evidencias. */
    public PendenciaGravada {
        Objects.requireNonNull(codigoRegra, "codigoRegra");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(explicacao, "explicacao");
        Objects.requireNonNull(responsavel, "responsavel");
        evidencias = List.copyOf(evidencias);
    }
}
