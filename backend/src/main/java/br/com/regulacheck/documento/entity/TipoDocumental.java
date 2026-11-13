package br.com.regulacheck.documento.entity;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tipo documental informado pela paciente ao enviar um arquivo: <em>o que</em> o arquivo diz ser, e nao o seu
 * formato (PDF/JPEG/PNG, que vem do conteudo). Conjunto fechado, igual a restricao
 * {@code RC_DOCUMENTO_CK_TIPO_DOC} do banco e alinhado as evidencias do protocolo academico (encaminhamento,
 * documento de identificacao, autorizacao do responsavel).
 *
 * <p>A informacao so descreve o envio: nenhuma regra e avaliada aqui e o tipo declarado nao prova que o conteudo
 * corresponde a ele.
 */
public enum TipoDocumental {

    /** Documento de encaminhamento emitido pela unidade. */
    ENCAMINHAMENTO,

    /** Documento de identificacao ou cartao do SUS (sinteticos). */
    IDENTIFICACAO,

    /** Autorizacao assinada do responsavel legal. */
    AUTORIZACAO_RESPONSAVEL,

    /** Qualquer outro documento de apoio. */
    OUTRO;

    /**
     * Interpreta o texto recebido da paciente. A comparacao e exata: nao ha conversao de caixa nem de espacos,
     * porque o valor faz parte do contrato da API.
     *
     * @param texto valor enviado; pode ser {@code null}
     * @return o tipo, ou vazio se o texto for nulo, em branco ou fora do conjunto
     */
    public static Optional<TipoDocumental> de(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(texto)).findFirst();
    }

    /** @return os valores aceitos, separados por virgula, para mensagens de erro ao cliente */
    public static String valoresAceitos() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
