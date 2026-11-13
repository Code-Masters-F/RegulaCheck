package br.com.regulacheck.integracao;

import java.text.Normalizer;

/**
 * Texto vindo de um documento enviado ou de entrada de pessoa: <strong>dado nao confiavel</strong>, nunca
 * instrucao.
 *
 * <p>Regras de uso (CLAUDE.md secao 11):
 * <ul>
 *   <li>Todo texto extraido ou derivado de arquivo entra no codigo neste tipo.</li>
 *   <li>{@link #toString()} nao mostra o conteudo (so o tamanho), para que nao va parar em log por engano.</li>
 *   <li>Para incluir o texto numa mensagem a outro componente (adaptador de IA ou de extracao), use somente
 *       {@link #delimitar(String)}: o texto vai entre marcadores que o identificam como dado, e qualquer
 *       marcador igual dentro dele e neutralizado.</li>
 *   <li>Nunca derive criterios, perfil ou estado de solicitacao deste texto.</li>
 * </ul>
 *
 * @param texto conteudo bruto; nao e confiavel
 */
public record TextoNaoConfiavel(String texto) {

    /** Marcador de abertura do bloco de dado. */
    public static final String ABERTURA = "<<DADO_NAO_CONFIAVEL";

    /** Marcador de fechamento do bloco de dado. */
    public static final String FECHAMENTO = "<<FIM_DADO_NAO_CONFIAVEL>>";

    /**
     * @param texto conteudo bruto; {@code null} vira texto vazio
     */
    public TextoNaoConfiavel {
        texto = texto == null ? "" : texto;
    }

    /** @return numero de caracteres do texto */
    public int tamanho() {
        return texto.length();
    }

    /**
     * Envolve o texto em marcadores que o declaram como dado, para ser enviado a outro componente. Qualquer
     * ocorrencia dos marcadores dentro do texto (inclusive com variacao de caixa ou de forma Unicode) e
     * trocada por {@code [marcador removido]}, de modo que o conteudo nao consegue "fechar" o bloco e
     * escrever instrucoes fora dele.
     *
     * @param origem identificador tecnico da origem (por exemplo, o id do documento); so letras, numeros,
     *     {@code _} e {@code -} sao mantidos
     * @return bloco delimitado: abertura com origem, texto neutralizado e fechamento
     */
    public String delimitar(String origem) {
        String seguraOrigem = origem == null ? "desconhecida" : origem.replaceAll("[^A-Za-z0-9_-]", "");
        if (seguraOrigem.isEmpty()) {
            seguraOrigem = "desconhecida";
        }
        return ABERTURA + " origem=" + seguraOrigem + ">>\n" + neutralizar(texto) + "\n" + FECHAMENTO;
    }

    private static String neutralizar(String bruto) {
        String normalizado = Normalizer.normalize(bruto, Normalizer.Form.NFKC);
        return normalizado.replaceAll("(?i)<<\\s*(FIM_)?DADO_NAO_CONFIAVEL[^>]*>*", "[marcador removido]")
                .replace("<<", "< <").replace(">>", "> >");
    }

    /** Nunca revela o conteudo: so o tamanho. */
    @Override
    public String toString() {
        return "[texto nao confiavel, " + texto.length() + " caracteres]";
    }
}
