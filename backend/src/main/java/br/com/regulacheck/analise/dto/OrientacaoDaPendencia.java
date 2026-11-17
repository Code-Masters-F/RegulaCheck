package br.com.regulacheck.analise.dto;

import br.com.regulacheck.analise.entity.PendenciaGravada;
import br.com.regulacheck.validacao.model.Resultado;
import java.util.Objects;

/**
 * Orientacao textual da pendencia para o contrato publico. Nao altera a regra, o responsavel ou o documento original.
 * As acoes de reenvio dependem do estado do pedido e nao sao prometidas por este texto.
 */
public final class OrientacaoDaPendencia {

    private OrientacaoDaPendencia() {
    }

    /**
     * @param pendencia resultado gravado de uma regra do protocolo academico
     * @return proxima providencia segura, sem inferir erro clinico de mera divergencia
     */
    public static String de(PendenciaGravada pendencia) {
        Objects.requireNonNull(pendencia, "pendencia");
        if (pendencia.tipo() == Resultado.NAO_VERIFICAVEL) {
            return "Confira a transcrição. Se o arquivo estiver ilegível ou incompleto, obtenha uma versão legível "
                    + "para reenviar quando essa ação estiver disponível. Informação não verificável não comprova ausência.";
        }
        return switch (pendencia.codigoRegra()) {
            case "RC-R01", "RC-R03", "RC-R06" -> "Obtenha o documento solicitado e envie ou substitua o arquivo "
                    + "quando essa ação estiver disponível no pedido.";
            case "RC-R02" -> "Confira nome e data de nascimento nos documentos. Se o erro estiver no documento "
                    + "médico original, solicite a correção à unidade emissora; não altere o original no aplicativo.";
            case "RC-R04" -> "Solicite à unidade emissora a revisão do encaminhamento. O documento médico original "
                    + "deve ser corrigido pela unidade.";
            case "RC-R05" -> "Solicite um novo encaminhamento à unidade emissora. A validade citada é apenas "
                    + "deste protocolo acadêmico fictício.";
            default -> "Confira o critério e a explicação desta pendência antes de providenciar a correção.";
        };
    }
}
