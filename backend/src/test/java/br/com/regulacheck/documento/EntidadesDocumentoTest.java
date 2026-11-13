package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.IdInterno;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.documento.entity.Documento;
import br.com.regulacheck.documento.entity.EstadoEnvio;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.documento.repository.NovoDocumento;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tipos fechados e privacidade das entidades do documento (T004). */
class EntidadesDocumentoTest {

    @Test
    void tipoDocumentalTemExatamenteOsValoresDaRestricaoDoBanco() {
        // Lista independente: copiada de RC_DOCUMENTO_CK_TIPO_DOC (database/scripts/01_ e 07_), nao derivada do enum.
        List<String> restricaoDoBanco = List.of("ENCAMINHAMENTO", "IDENTIFICACAO", "AUTORIZACAO_RESPONSAVEL", "OUTRO");
        assertEquals(restricaoDoBanco, Arrays.stream(TipoDocumental.values()).map(Enum::name).toList());
    }

    @Test
    void deConverteSoOTextoExatoEDevolveVazioParaOResto() {
        assertEquals(TipoDocumental.ENCAMINHAMENTO, TipoDocumental.de("ENCAMINHAMENTO").orElseThrow());
        assertEquals(TipoDocumental.OUTRO, TipoDocumental.de("OUTRO").orElseThrow());
        for (String invalido : new String[] {null, "", " ", "encaminhamento", "Encaminhamento", " OUTRO", "LAUDO"}) {
            assertTrue(TipoDocumental.de(invalido).isEmpty(), "deveria recusar: " + invalido);
        }
    }

    @Test
    void valoresAceitosListaTodosParaAMensagemDeErro() {
        String texto = TipoDocumental.valoresAceitos();
        for (TipoDocumental tipo : TipoDocumental.values()) {
            assertTrue(texto.contains(tipo.name()), texto);
        }
    }

    @Test
    void estadoDoEnvioTemUmUnicoValorHoje() {
        assertEquals(List.of("ENVIADO"), Arrays.stream(EstadoEnvio.values()).map(Enum::name).toList());
    }

    @Test
    void toStringDeDocumentoENovoDocumentoNaoExpoeCaminhoHashNemChave() {
        IdInterno interno = IdInterno.novo();
        String hash = "ab".repeat(32);
        Documento documento = new Documento(1, 2, "meu-nome-privado.pdf", TipoDocumental.OUTRO, TipoArquivo.PDF, 10, 1,
                2, Instant.parse("2026-11-13T12:00:00Z"), interno, hash, "chave-privada-123");
        NovoDocumento novo = new NovoDocumento("meu-nome-privado.pdf", TipoDocumental.OUTRO, TipoArquivo.PDF, 10, 1,
                interno, hash, "chave-privada-123");

        for (String texto : new String[] {documento.toString(), novo.toString()}) {
            assertFalse(texto.contains(interno.valor()), texto);
            assertFalse(texto.contains(hash), texto);
            assertFalse(texto.contains("chave-privada-123"), texto);
            assertFalse(texto.contains("meu-nome-privado"), texto);
        }
        assertEquals(EstadoEnvio.ENVIADO, documento.estadoEnvio());
    }
}
