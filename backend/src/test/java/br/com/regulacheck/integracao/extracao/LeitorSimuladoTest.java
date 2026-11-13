package br.com.regulacheck.integracao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.extracao.AmostrasSinteticas;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.extracao.LeitoresDeTeste;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * O leitor simulado nao le: identifica o arquivo pelo SHA-256 e devolve a leitura predefinida; qualquer outro
 * arquivo vira leitura vazia. Os valores esperados para as duas amostras oficiais sao conferidos aqui por extenso,
 * independentemente do gerador do catalogo.
 */
class LeitorSimuladoTest {

    private static ArquivoParaLeitura arquivo(Amostra amostra) {
        return new ArquivoParaLeitura(amostra.tipo(), amostra.tipo() == TipoArquivo.PDF ? 1 : 1, amostra.bytes());
    }

    private static CampoLido campo(LeituraDeDocumento leitura, CodigoCampo codigo) {
        return leitura.campos().stream().filter(c -> c.codigo() == codigo).findFirst().orElseThrow();
    }

    @Test
    void pdfOficialSaiComOGabaritoEscritoPorExtenso() {
        LeituraDeDocumento leitura = new LeitorSimulado().ler(arquivo(AmostrasSinteticas.ENCAMINHAMENTO));

        assertEquals(SituacaoLeitura.LIDO, leitura.situacao());
        assertEquals(6, leitura.campos().size());
        CampoLido motivo = campo(leitura, CodigoCampo.MOTIVO_ENCAMINHAMENTO);
        assertEquals(SituacaoCampo.IDENTIFICADO, motivo.situacao());
        assertEquals("Avaliacao cardiologica ambulatorial (texto sintetico)", motivo.valor());
        assertEquals(1, motivo.pagina());
        assertEquals("1990-04-12", campo(leitura, CodigoCampo.DATA_NASCIMENTO).valor());
        assertEquals("2026-10-20", campo(leitura, CodigoCampo.DATA_EMISSAO).valor());
        assertEquals("ENCAMINHAMENTO", campo(leitura, CodigoCampo.TIPO_DOCUMENTO).valor());
        CampoLido autorizacao = campo(leitura, CodigoCampo.AUTORIZACAO_ASSINADA);
        assertEquals(SituacaoCampo.NAO_LOCALIZADO, autorizacao.situacao());
        assertNull(autorizacao.valor());
    }

    @Test
    void fotoOficialSaiSemPaginaDeOrigem() {
        LeituraDeDocumento leitura = new LeitorSimulado().ler(arquivo(AmostrasSinteticas.IDENTIFICACAO));

        assertEquals(SituacaoLeitura.LIDO, leitura.situacao());
        CampoLido tipo = campo(leitura, CodigoCampo.TIPO_DOCUMENTO);
        assertEquals("IDENTIFICACAO", tipo.valor());
        assertNull(tipo.pagina());
        assertTrue(tipo.trecho() != null && !tipo.trecho().isBlank());
        assertEquals(SituacaoCampo.NAO_LOCALIZADO, campo(leitura, CodigoCampo.MOTIVO_ENCAMINHAMENTO).situacao());
    }

    @Test
    void arquivoDesconhecidoViraLeituraVaziaSemCampos() {
        byte[] qualquer = "%PDF-1.4 qualquer coisa que nao e uma amostra".getBytes(StandardCharsets.UTF_8);
        LeituraDeDocumento leitura = new LeitorSimulado().ler(new ArquivoParaLeitura(TipoArquivo.PDF, 1, qualquer));

        assertEquals(SituacaoLeitura.LEITURA_VAZIA, leitura.situacao());
        assertTrue(leitura.campos().isEmpty());
    }

    @Test
    void bytesAlteradosDeixamDeSerReconhecidos() throws Exception {
        byte[] alterados = Files.readAllBytes(AmostrasSinteticas.ENCAMINHAMENTO.caminho());
        alterados[alterados.length - 1] ^= 0x01;
        LeituraDeDocumento leitura = new LeitorSimulado().ler(
                new ArquivoParaLeitura(TipoArquivo.PDF, 1, alterados));
        assertEquals(SituacaoLeitura.LEITURA_VAZIA, leitura.situacao());
    }

    @Test
    void mesmaEntradaMesmaSaidaEsoOConteudoImporta() {
        LeitorSimulado leitor = new LeitorSimulado();
        byte[] bytes = AmostrasSinteticas.ENCAMINHAMENTO.bytes();
        LeituraDeDocumento primeira = leitor.ler(new ArquivoParaLeitura(TipoArquivo.PDF, 1, bytes));
        LeituraDeDocumento segunda = leitor.ler(new ArquivoParaLeitura(TipoArquivo.PDF, 1, bytes.clone()));
        assertEquals(primeira, segunda);
    }

    @Test
    void toda_saida_do_catalogo_respeita_o_validador() {
        LeitorSimulado leitor = LeitoresDeTeste.simuladoComTodasAsAmostras();
        for (Amostra amostra : java.util.stream.Stream.concat(AmostrasSinteticas.oficiais().stream(),
                AmostrasSinteticas.extras().stream()).toList()) {
            LeituraDeDocumento leitura = leitor.ler(arquivo(amostra));
            assertEquals(amostra.leitura(), leitura, amostra.nome());
            assertTrue(ValidadorDeLeitura.verificar(leitura, amostra.tipo(), 1).isEmpty(), amostra.nome());
        }
    }

    @Test
    void arquivosCommitadosCoincidemComOHashDoCatalogoEDoGabarito() throws Exception {
        String gabarito = Files.readString(Path.of("..", "samples", "gabarito.md"), StandardCharsets.UTF_8);
        for (Amostra amostra : AmostrasSinteticas.oficiais()) {
            String hash = LeitorSimulado.sha256(amostra.bytes());
            assertTrue(gabarito.contains(hash), "gabarito cita o hash de " + amostra.nome());
            assertEquals(amostra.leitura().situacao(), new LeitorSimulado().ler(arquivo(amostra)).situacao());
        }
    }

    @Test
    void catalogoForaDoContratoImpedeACriacaoDoLeitor() {
        String hashValido = "a".repeat(64);
        String valorEmNaoLocalizado = "{\"leituras\":[{\"hash\":\"" + hashValido + "\",\"situacao\":\"LIDO\","
                + "\"campos\":[{\"codigo\":\"NOME\",\"situacao\":\"NAO_LOCALIZADO\",\"valor\":\"x\"}]}]}";
        assertThrows(IllegalStateException.class, () -> LeitoresDeTeste.simuladoCom(valorEmNaoLocalizado));
        assertThrows(IllegalStateException.class, () -> LeitoresDeTeste.simuladoCom("{\"leituras\":[{\"hash\":\"x\","
                + "\"situacao\":\"LEITURA_VAZIA\"}]}"));
        assertThrows(IllegalStateException.class, () -> LeitoresDeTeste.simuladoCom("nao e json"));
        assertThrows(IllegalStateException.class, () -> LeitoresDeTeste.simuladoCom("{}"));
        String repetido = "{\"hash\":\"" + hashValido + "\",\"situacao\":\"LEITURA_VAZIA\"}";
        assertThrows(IllegalStateException.class,
                () -> LeitoresDeTeste.simuladoCom("{\"leituras\":[" + repetido + "," + repetido + "]}"));
        assertThrows(IllegalStateException.class, () -> LeitoresDeTeste.simuladoCom("{\"leituras\":[{\"hash\":\""
                + hashValido + "\",\"situacao\":\"INVENTADA\"}]}"));
    }

    @Test
    void naoLogaNemExpoeConteudoNoToStringDoArquivo() {
        byte[] bytes = "SEGREDO-NO-DOCUMENTO".getBytes(StandardCharsets.UTF_8);
        String texto = new ArquivoParaLeitura(TipoArquivo.PDF, 1, bytes).toString();
        assertTrue(!texto.contains("SEGREDO"));
        assertTrue(!CampoLido.identificado(CodigoCampo.NOME, "SEGREDO", 1, "SEGREDO").toString().contains("SEGREDO"));
    }
}
