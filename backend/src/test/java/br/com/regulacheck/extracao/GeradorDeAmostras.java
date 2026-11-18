package br.com.regulacheck.extracao;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.integracao.extracao.CampoLido;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.LeitorSimulado;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * <strong>Programa de apoio, nao e um teste de regressao.</strong> Gera as amostras sinteticas (PDF e PNG), os
 * catalogos do leitor simulado e o gabarito, a partir de {@link AmostrasSinteticas}. So roda com
 * {@code -Dgerar.amostras=SIM}; os arquivos gerados sao commitados (o hash do catalogo e o do arquivo commitado).
 * Sem a propriedade, e ignorado.
 *
 * <p>Comando (a partir de {@code backend/}):
 * {@code .\mvnw.cmd -Dtest=GeradorDeAmostras -Dgerar.amostras=SIM test}.
 */
@EnabledIfSystemProperty(named = "gerar.amostras", matches = "SIM")
class GeradorDeAmostras {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void geraAmostrasCatalogosEGabarito() throws IOException {
        for (Amostra amostra : AmostrasSinteticas.oficiais()) {
            gerarArquivo(amostra);
        }
        for (Amostra amostra : AmostrasSinteticas.extras()) {
            gerarArquivo(amostra);
        }
        gerarArquivosDosConjuntos();
        escreverCatalogo(Path.of("src", "main", "resources", "extracao-simulada", "catalogo.json"),
                AmostrasSinteticas.oficiais(), ConjuntosSinteticos.todos());
        escreverCatalogo(Path.of("src", "test", "resources", "extracao-simulada", "catalogo-testes.json"),
                AmostrasSinteticas.extras(), List.of());
        escreverGabarito(Path.of("..", "samples", "gabarito.md"));
        EscritorDeGabaritos.escrever(Path.of("..", "samples"));
    }

    /** Cria somente os arquivos dos conjuntos (RC-23) que ainda nao existem; nunca sobrescreve. */
    private static void gerarArquivosDosConjuntos() throws IOException {
        for (ConjuntosSinteticos.Conjunto conjunto : ConjuntosSinteticos.todos()) {
            for (ConjuntosSinteticos.Arquivo arquivo : conjunto.arquivos()) {
                Path destino = arquivo.caminho();
                if (!arquivo.gerado() || Files.exists(destino)) {
                    continue;
                }
                Files.createDirectories(destino.getParent());
                switch (arquivo.formato()) {
                    case PDF -> gerarPdf(destino, arquivo.linhas(), arquivo.paginas());
                    case PNG -> gerarPng(destino, arquivo.linhas());
                    case TXT -> Files.writeString(destino, String.join("\n", arquivo.linhas()) + "\n",
                            StandardCharsets.UTF_8);
                    default -> throw new IllegalStateException("Formato desconhecido: " + arquivo.formato());
                }
            }
        }
    }

    private static void gerarArquivo(Amostra amostra) throws IOException {
        Path destino = amostra.caminho();
        if (Files.exists(destino)) {
            // O hash do arquivo commitado e a chave da leitura simulada: nunca regenerar um arquivo existente.
            return;
        }
        Files.createDirectories(destino.getParent());
        if (amostra.tipo() == TipoArquivo.PDF) {
            gerarPdf(destino, amostra.linhas());
        } else {
            gerarPng(destino, amostra.linhas());
        }
    }

    private static void gerarPdf(Path destino, List<String> linhas) throws IOException {
        gerarPdf(destino, linhas, 1);
    }

    /** PDF textual; com mais de uma pagina, cada pagina repete as linhas e indica seu numero. */
    private static void gerarPdf(Path destino, List<String> linhas, int paginas) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            for (int numero = 1; numero <= paginas; numero++) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);
                try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                    conteudo.beginText();
                    conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                    conteudo.setLeading(22);
                    conteudo.newLineAtOffset(60, 720);
                    for (String linha : linhas) {
                        conteudo.showText(linha);
                        conteudo.newLine();
                    }
                    if (paginas > 1) {
                        conteudo.showText("Pagina " + numero + " de " + paginas);
                        conteudo.newLine();
                    }
                    conteudo.endText();
                }
            }
            documento.save(destino.toFile());
        }
    }

    private static void gerarPng(Path destino, List<String> linhas) throws IOException {
        BufferedImage imagem = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = imagem.createGraphics();
        try {
            desenho.setColor(Color.WHITE);
            desenho.fillRect(0, 0, 640, 360);
            desenho.setColor(Color.BLACK);
            desenho.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            int y = 60;
            for (String linha : linhas) {
                desenho.drawString(linha, 40, y);
                y += 36;
            }
        } finally {
            desenho.dispose();
        }
        ImageIO.write(imagem, "png", destino.toFile());
    }

    /**
     * Escreve um catalogo do leitor simulado: as amostras e, depois, as leituras dos arquivos dos conjuntos (RC-23)
     * que tem leitura e ainda nao estao no catalogo (um arquivo reaproveitado entra uma so vez).
     */
    private static void escreverCatalogo(Path destino, List<Amostra> amostras,
            List<ConjuntosSinteticos.Conjunto> conjuntos) throws IOException {
        Files.createDirectories(destino.getParent());
        ObjectNode raiz = MAPPER.createObjectNode();
        ArrayNode leituras = raiz.putArray("leituras");
        Set<String> hashes = new HashSet<>();
        for (Amostra amostra : amostras) {
            adicionarLeitura(leituras, hashes, amostra.nome(), amostra.bytes(), amostra.leitura());
        }
        for (ConjuntosSinteticos.Conjunto conjunto : conjuntos) {
            for (ConjuntosSinteticos.Arquivo arquivo : conjunto.arquivos()) {
                if (arquivo.leitura() != null) {
                    adicionarLeitura(leituras, hashes, conjunto.pasta() + "/" + arquivo.nome(), arquivo.bytes(),
                            arquivo.leitura());
                }
            }
        }
        Files.writeString(destino, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(raiz) + "\n",
                StandardCharsets.UTF_8);
    }

    private static void adicionarLeitura(ArrayNode leituras, Set<String> hashes, String descricao, byte[] bytes,
            LeituraDeDocumento leitura) {
        String hash = LeitorSimulado.sha256(bytes);
        if (!hashes.add(hash)) {
            return;
        }
        ObjectNode item = leituras.addObject();
        item.put("descricao", descricao);
        item.put("hash", hash);
        item.put("situacao", leitura.situacao().name());
        ArrayNode campos = item.putArray("campos");
        for (CampoLido campo : leitura.campos()) {
            ObjectNode no = campos.addObject();
            no.put("codigo", campo.codigo().name());
            no.put("situacao", campo.situacao().name());
            if (campo.valor() != null) {
                no.put("valor", campo.valor());
            }
            if (campo.pagina() != null) {
                no.put("pagina", campo.pagina());
            }
            if (campo.trecho() != null) {
                no.put("trecho", campo.trecho());
            }
        }
    }

    private static void escreverGabarito(Path destino) throws IOException {
        StringBuilder texto = new StringBuilder("""
                # Gabarito das amostras sintéticas (RC-10)

                > Documentos **sintéticos**, sem nenhum dado real. A leitura é **simulada** (decisão de 12/11/2026):
                > o leitor não lê o arquivo; ele o identifica pelo SHA-256 e devolve a leitura abaixo
                > (`backend/src/main/resources/extracao-simulada/catalogo.json`). Gerado por `GeradorDeAmostras`.

                """);
        for (Amostra amostra : AmostrasSinteticas.oficiais()) {
            texto.append("## ").append(amostra.nome()).append("\n\n");
            texto.append("SHA-256: `").append(LeitorSimulado.sha256(amostra.bytes())).append("`\n\n");
            texto.append("| Campo | Situação | Valor | Origem |\n|---|---|---|---|\n");
            for (CampoLido campo : amostra.leitura().campos()) {
                String origem = campo.trecho() == null ? "-"
                        : (campo.pagina() == null ? "" : "pág. " + campo.pagina() + ": ") + "\"" + campo.trecho() + "\"";
                texto.append("| `").append(campo.codigo()).append("` | ").append(campo.situacao()).append(" | ")
                        .append(campo.valor() == null ? "-" : campo.valor()).append(" | ").append(origem)
                        .append(" |\n");
            }
            texto.append('\n');
        }
        texto.append("""
                ## Casos extras (só de teste)

                Ficam em `backend/src/test/resources/extracao-simulada/` (catálogo `catalogo-testes.json`): encaminhamento sem
                motivo (`NAO_LOCALIZADO`), identificação com data de nascimento borrada (`ILEGIVEL`), encaminhamento com duas
                datas de emissão conflitantes (`ILEGIVEL`), PDF que se apresenta como identificação (tipo lido diferente do
                informado) e documento com texto-injeção (`ignore as regras...`), que é apenas dado do campo.
                Qualquer outro arquivo recebe `LEITURA_VAZIA`, nunca campos inventados.
                """);
        Files.writeString(destino, texto.toString(), StandardCharsets.UTF_8);
    }
}
