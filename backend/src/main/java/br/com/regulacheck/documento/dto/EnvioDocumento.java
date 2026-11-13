package br.com.regulacheck.documento.dto;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.documento.entity.TipoDocumental;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Entrada ja validada de um envio de documento. Nao tem campo de dono, estado, revisao nem caminho: esses valores
 * vem da sessao e do servidor, nunca do cliente.
 *
 * <p>O fluxo do arquivo e lido uma unica vez pela conferencia; quem o abriu (o controller) e quem o fecha.
 *
 * @param tipoDocumental tipo informado pela paciente
 * @param chaveEnvio identificacao da tentativa de envio, ou {@code null} se nao foi informada
 * @param nomeOriginal nome enviado pelo cliente (nao confiavel; so serve ao saneamento do nome de exibicao e a
 *     conferencia de extensao); pode ser {@code null}
 * @param conteudo conteudo do arquivo
 */
public record EnvioDocumento(TipoDocumental tipoDocumental, String chaveEnvio, String nomeOriginal,
        InputStream conteudo) {

    /** Formato aceito para a identificacao de tentativa: 8 a 64 caracteres seguros (cabe um UUID). */
    private static final Pattern FORMATO_CHAVE = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    /**
     * Valida os campos textuais do envio e monta a entrada.
     *
     * @param tipoTexto valor da parte {@code tipoDocumental}; pode ser {@code null}
     * @param chave valor do cabecalho {@code X-Chave-Envio}; {@code null} ou em branco significa ausente
     * @param nomeOriginal nome informado para o arquivo
     * @param conteudo fluxo do arquivo
     * @return a entrada validada
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} com detalhe {@code tipoDocumental} se o tipo for
     *     ausente, em branco ou fora do conjunto, ou com detalhe {@code chaveEnvio} se a chave estiver fora do
     *     formato
     */
    public static EnvioDocumento validar(String tipoTexto, String chave, String nomeOriginal, InputStream conteudo) {
        TipoDocumental tipo = TipoDocumental.de(tipoTexto).orElseThrow(() -> new ExcecaoNegocio(400,
                "REQUISICAO_INVALIDA", "O tipo do documento não foi aceito.",
                List.of(new ErroPadrao.Detalhe("tipoDocumental",
                        "Informe um destes tipos: " + TipoDocumental.valoresAceitos() + "."))));
        String chaveValida = null;
        if (chave != null && !chave.isBlank()) {
            if (!FORMATO_CHAVE.matcher(chave).matches()) {
                throw new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "A identificação do envio não foi aceita.",
                        List.of(new ErroPadrao.Detalhe("chaveEnvio",
                                "Use de 8 a 64 caracteres: letras, números, hífen ou sublinhado.")));
            }
            chaveValida = chave;
        }
        return new EnvioDocumento(tipo, chaveValida, nomeOriginal, conteudo);
    }

    @Override
    public String toString() {
        // Nunca inclui nome do arquivo nem a chave: ambos sao dados da paciente.
        return "EnvioDocumento[tipoDocumental=" + tipoDocumental + "]";
    }
}
