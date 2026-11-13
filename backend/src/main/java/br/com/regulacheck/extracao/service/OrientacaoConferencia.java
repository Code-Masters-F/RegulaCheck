package br.com.regulacheck.extracao.service;

import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.integracao.extracao.SituacaoCampo;

/**
 * Textos fixos de orientacao da conferencia. Ficam no servidor e nunca repetem valor, trecho ou texto de documento.
 * Nao diagnosticam, nao definem urgencia ou prioridade e nao prometem atendimento: so dizem o que a paciente pode
 * fazer. A informacao ausente num documento nao se preenche aqui: a saida e pedir documento corrigido a unidade
 * emissora.
 */
public final class OrientacaoConferencia {

    /** Campo nao localizado, sem divergencia apontada. */
    public static final String NAO_LOCALIZADO = "Esta informação não consta no documento lido. Se ela for necessária, "
            + "peça um documento corrigido à unidade emissora. Não é possível preencher aqui.";

    /** Campo ilegivel. */
    public static final String ILEGIVEL = "Não foi possível ler este campo. Envie um arquivo mais nítido.";

    /** Divergencia: o valor lido nao confere com o documento. */
    public static final String VALOR_DIFERENTE = "Você indicou que o valor lido não confere. Se o erro está no "
            + "documento, peça um documento corrigido à unidade emissora; se foi erro de leitura, envie um "
            + "arquivo mais nítido.";

    /** Divergencia: o campo consta no documento, mas nao foi lido. */
    public static final String EXISTE_E_NAO_LIDO = "Você indicou que o documento tem este campo, mas ele não foi "
            + "lido. Envie um arquivo mais nítido.";

    private OrientacaoConferencia() {
    }

    /**
     * Orientacao de um campo.
     *
     * @param situacao situacao do campo na leitura
     * @param motivoVigente motivo da divergencia vigente; {@code null} se nao ha divergencia
     * @return o texto fixo, ou {@code null} quando nao ha o que orientar (campo identificado sem divergencia)
     */
    public static String doCampo(SituacaoCampo situacao, MotivoDivergencia motivoVigente) {
        if (motivoVigente != null) {
            return switch (motivoVigente) {
                case VALOR_DIFERENTE_DO_DOCUMENTO -> VALOR_DIFERENTE;
                case CAMPO_EXISTE_E_NAO_FOI_LIDO -> EXISTE_E_NAO_LIDO;
            };
        }
        return switch (situacao) {
            case IDENTIFICADO -> null;
            case NAO_LOCALIZADO -> NAO_LOCALIZADO;
            case ILEGIVEL -> ILEGIVEL;
        };
    }
}
