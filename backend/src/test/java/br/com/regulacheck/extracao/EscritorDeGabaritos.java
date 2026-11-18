package br.com.regulacheck.extracao;

import br.com.regulacheck.extracao.ConjuntosSinteticos.Arquivo;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Conjunto;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Recusa;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Veredicto;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.LeitorSimulado;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Escreve o {@code gabarito.md} de cada conjunto sintetico (RC-23) e o indice {@code catalogo-conjuntos.md}, no
 * formato de {@code specs/020-conjunto-sintetico-demonstracao/contracts/formato-conjunto.md}. O texto vem das
 * definicoes de {@link ConjuntosSinteticos}, escritas antes da execucao, e nunca do resultado do motor.
 *
 * <p>Um {@code gabarito.md} ja existente <strong>nao e sobrescrito</strong>: ele registra a ordem "gabarito antes da
 * execucao". O indice e reescrito a cada geracao.
 */
final class EscritorDeGabaritos {

    private EscritorDeGabaritos() {
    }

    /**
     * Escreve os gabaritos e o indice.
     *
     * @param samples pasta {@code samples/} (relativa a {@code backend/})
     * @throws IOException se algum arquivo nao puder ser escrito
     */
    static void escrever(Path samples) throws IOException {
        for (Conjunto conjunto : ConjuntosSinteticos.todos()) {
            Path pasta = samples.resolve("conjuntos").resolve(conjunto.pasta());
            Files.createDirectories(pasta);
            Path destino = pasta.resolve("gabarito.md");
            if (Files.notExists(destino)) {
                Files.writeString(destino, gabarito(conjunto), StandardCharsets.UTF_8);
            }
        }
        Files.writeString(samples.resolve("catalogo-conjuntos.md"), indice(), StandardCharsets.UTF_8);
    }

    private static String gabarito(Conjunto conjunto) {
        StringBuilder t = new StringBuilder();
        t.append("# Conjunto ").append(conjunto.numero()).append(" — ").append(conjunto.nome()).append("\n\n");
        t.append("> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor\n"
                + "> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é\n"
                + "> aprovação do SUS, vaga, prazo nem autorização de atendimento.\n\n");
        t.append("- **Objetivo**: ").append(conjunto.objetivo()).append('\n');
        t.append("- **Data de referência**: ").append(ConjuntosSinteticos.DATA_REFERENCIA)
                .append(" (regressão automática; em execução manual vale o dia do envio)\n");
        t.append("- **Gabarito registrado em**: ").append(ConjuntosSinteticos.DATA_DO_REGISTRO)
                .append(", antes da primeira execução do conjunto\n");
        t.append("- **Original**: ").append(conjunto.originalDe() == null ? "-" : "conjunto "
                + conjunto.originalDe() + " (versão corrigida; o gabarito do original não muda)").append('\n');
        if (conjunto.arquivosDe() != null) {
            t.append("- **Arquivos**: os mesmos do conjunto ").append(conjunto.arquivosDe()).append('\n');
        }
        if (!conjunto.recusadoNoEnvio()) {
            t.append("- **Conferência pressuposta**: ").append(conjunto.conferencia()).append('\n');
        }
        t.append('\n');
        t.append("## Arquivos\n\n| Arquivo | Formato | SHA-256 | Ação no envio |\n|---|---|---|---|\n");
        for (int i = 0; i < ConjuntosSinteticos.arquivosEfetivos(conjunto).size(); i++) {
            Arquivo arquivo = ConjuntosSinteticos.arquivosEfetivos(conjunto).get(i);
            t.append("| `").append(arquivo.nome()).append("` | ").append(arquivo.formato()).append(" | `")
                    .append(LeitorSimulado.sha256(arquivo.bytes())).append("` | ")
                    .append(acao(conjunto, i)).append(" |\n");
        }
        t.append('\n');
        if (conjunto.recusadoNoEnvio()) {
            t.append("## Recusa esperada\n\nRecusado **no envio**: nada é gravado, não há leitura nem análise e nenhuma"
                    + " regra é avaliada.\n\n| Arquivo | HTTP | Código aceito | Motivo |\n|---|---|---|---|\n");
            for (Recusa recusa : conjunto.recusas()) {
                t.append("| `").append(recusa.arquivo()).append("` | ").append(recusa.status()).append(" | ")
                        .append(String.join(" ou ", recusa.codigosAceitos())).append(" | ").append(recusa.motivo())
                        .append(" |\n");
            }
        } else {
            if (conjunto.arquivosDe() == null) {
                t.append("## Campos esperados\n\n| Arquivo | Campo | Situação | Valor | Origem |\n|---|---|---|---|---|\n");
                for (Arquivo arquivo : conjunto.arquivos()) {
                    for (CampoLido campo : arquivo.leitura().campos()) {
                        String origem = campo.trecho() == null ? "-" : (campo.pagina() == null ? "" : "pág. "
                                + campo.pagina() + ": ") + "\"" + campo.trecho() + "\"";
                        t.append("| `").append(arquivo.nome()).append("` | `").append(campo.codigo()).append("` | ")
                                .append(campo.situacao()).append(" | ")
                                .append(campo.valor() == null ? "-" : campo.valor()).append(" | ").append(origem)
                                .append(" |\n");
                    }
                }
                t.append('\n');
            } else {
                t.append("## Campos esperados\n\nOs mesmos do conjunto ").append(conjunto.arquivosDe())
                        .append(", mas **nenhum foi confirmado**: sem confirmação, nenhum campo vale como"
                                + " evidência.\n\n");
            }
            t.append("## Regras esperadas\n\n| Regra | Resultado | Responsável (se pendente) |\n|---|---|---|\n");
            for (Map.Entry<String, Veredicto> regra : conjunto.regras().entrySet()) {
                t.append("| ").append(regra.getKey()).append(" | ").append(regra.getValue().resultado())
                        .append(" | ").append(regra.getValue().responsavel() == null ? "-"
                                : regra.getValue().responsavel())
                        .append(" |\n");
            }
            t.append("\n## Resultado geral esperado\n\n").append(conjunto.geral()).append("\n");
        }
        t.append("\n## Observação\n\n").append(conjunto.observacao()).append('\n');
        return t.toString();
    }

    private static String acao(Conjunto conjunto, int indice) {
        if (conjunto.substitui() == null) {
            return "enviar";
        }
        int alvo = conjunto.substitui()[indice];
        return alvo < 0 ? "acrescentar ao pedido do conjunto " + conjunto.originalDe()
                : "substituir o documento " + (alvo + 1) + " do conjunto " + conjunto.originalDe();
    }

    private static String indice() {
        StringBuilder t = new StringBuilder();
        t.append("# Catálogo dos conjuntos sintéticos (RC-23)\n\n");
        t.append("> Material **fictício**, sem dado real. Leitura **simulada**. Resultado esperado não é aprovação do"
                + " SUS. Data de referência da regressão: ").append(ConjuntosSinteticos.DATA_REFERENCIA)
                .append(". Gabaritos registrados em ").append(ConjuntosSinteticos.DATA_DO_REGISTRO)
                .append(", antes da primeira execução. Registro de commit do gabarito: **pendente** (commit não"
                        + " autorizado até a escrita deste arquivo).\n\n");
        t.append("| # | Conjunto | Situação coberta | Arquivos | Conferência | Resultado geral esperado | Original |\n"
                + "|---|---|---|---|---|---|---|\n");
        for (Conjunto c : ConjuntosSinteticos.todos()) {
            t.append("| ").append(c.numero()).append(" | [").append(c.pasta()).append("](conjuntos/")
                    .append(c.pasta()).append("/gabarito.md) | ").append(c.objetivo()).append(" | ")
                    .append(tipos(c)).append(" | ").append(c.recusadoNoEnvio() ? "-" : c.conferencia().toString())
                    .append(" | ").append(c.recusadoNoEnvio() ? "recusa no envio" : c.geral().toString())
                    .append(" | ").append(c.originalDe() == null ? "-" : c.originalDe()).append(" |\n");
        }
        return t.toString();
    }

    private static String tipos(Conjunto conjunto) {
        StringBuilder t = new StringBuilder();
        for (Arquivo arquivo : ConjuntosSinteticos.arquivosEfetivos(conjunto)) {
            if (!t.isEmpty()) {
                t.append(", ");
            }
            t.append(arquivo.formato());
        }
        return t.toString();
    }
}
