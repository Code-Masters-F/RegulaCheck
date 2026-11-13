package br.com.regulacheck.armazenamento;

import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Saneamento do nome original de um arquivo para <strong>exibicao</strong>.
 *
 * <p>O nome enviado e texto nao confiavel: nunca participa do caminho de gravacao (o arquivo se chama
 * {@link IdInterno}). Aqui ele so e limpo para poder aparecer na interface e no cabecalho
 * {@code Content-Disposition} sem separadores de pasta, caracteres de controle (inclusive CR/LF, que
 * permitiriam injetar cabecalhos), marcadores de direcao de texto ou tamanho excessivo.
 */
public final class NomeExibicao {

    /** Nome usado quando nada utilizavel sobra do original. */
    public static final String PADRAO = "documento";

    private static final Set<String> RESERVADOS_WINDOWS = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3",
            "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7",
            "lpt8", "lpt9");

    private static final int TAMANHO_MAXIMO_EXTENSAO = 10;

    private NomeExibicao() {
    }

    /**
     * Sanea o nome original.
     *
     * @param original nome informado por quem enviou; pode ser {@code null}
     * @return texto seguro para exibicao, nunca vazio e com no maximo
     *     {@link LimitesArquivo#MAXIMO_NOME_EXIBICAO} caracteres
     */
    public static String sanear(String original) {
        if (original == null) {
            return PADRAO;
        }
        String texto = Normalizer.normalize(original, Normalizer.Form.NFC);
        int ultimoSeparador = Math.max(texto.lastIndexOf('/'), texto.lastIndexOf('\\'));
        texto = texto.substring(ultimoSeparador + 1);

        StringBuilder limpo = new StringBuilder(texto.length());
        texto.codePoints().forEach(c -> {
            if (Character.isISOControl(c) || ehInvisivelOuDirecional(c)) {
                return;
            }
            limpo.appendCodePoint(proibidoEmNome(c) ? '_' : c);
        });
        String nome = limpo.toString().trim();
        if (nome.isEmpty() || nome.chars().allMatch(c -> c == '.')) {
            return PADRAO;
        }
        nome = prefixarSeReservado(nome);
        return limitar(nome);
    }

    private static boolean ehInvisivelOuDirecional(int c) {
        return (c >= 0x200B && c <= 0x200F) || (c >= 0x202A && c <= 0x202E) || (c >= 0x2060 && c <= 0x2069)
                || c == 0xFEFF;
    }

    private static boolean proibidoEmNome(int c) {
        return "<>:\"|?*`".indexOf(c) >= 0;
    }

    private static String prefixarSeReservado(String nome) {
        int ponto = nome.indexOf('.');
        String base = (ponto < 0 ? nome : nome.substring(0, ponto)).toLowerCase(Locale.ROOT);
        return RESERVADOS_WINDOWS.contains(base) ? "_" + nome : nome;
    }

    private static String limitar(String nome) {
        int maximo = LimitesArquivo.MAXIMO_NOME_EXIBICAO;
        if (nome.codePointCount(0, nome.length()) <= maximo) {
            return nome;
        }
        int ponto = nome.lastIndexOf('.');
        String extensao = ponto > 0 && nome.length() - ponto <= TAMANHO_MAXIMO_EXTENSAO ? nome.substring(ponto) : "";
        int espacoParaBase = maximo - extensao.codePointCount(0, extensao.length());
        String base = nome.substring(0, nome.offsetByCodePoints(0, espacoParaBase));
        return base + extensao;
    }
}
