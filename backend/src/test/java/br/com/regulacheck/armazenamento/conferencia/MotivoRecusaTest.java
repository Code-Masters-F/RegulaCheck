package br.com.regulacheck.armazenamento.conferencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MotivoRecusaTest {

    @Test
    void cadaMotivoTemStatusECodigoDoContrato() {
        assertEquals(415, MotivoRecusa.TIPO_NAO_SUPORTADO.status());
        assertEquals("TIPO_NAO_SUPORTADO", MotivoRecusa.TIPO_NAO_SUPORTADO.codigo());
        assertEquals(415, MotivoRecusa.EXTENSAO_INCOMPATIVEL.status());
        assertEquals("EXTENSAO_INCOMPATIVEL", MotivoRecusa.EXTENSAO_INCOMPATIVEL.codigo());
        assertEquals(413, MotivoRecusa.TAMANHO_EXCEDIDO.status());
        assertEquals("ARQUIVO_MUITO_GRANDE", MotivoRecusa.TAMANHO_EXCEDIDO.codigo());
        for (MotivoRecusa m : new MotivoRecusa[] {MotivoRecusa.EXCESSO_DE_PAGINAS, MotivoRecusa.PDF_PROTEGIDO,
                MotivoRecusa.ARQUIVO_ILEGIVEL, MotivoRecusa.EXCESSO_DE_ARQUIVOS}) {
            assertEquals(400, m.status(), m.name());
            assertEquals("REQUISICAO_INVALIDA", m.codigo(), m.name());
        }
    }

    @Test
    void erroPadraoTrazDetalheDeCampoQuandoHouver() {
        ExcecaoNegocio erro = MotivoRecusa.PDF_PROTEGIDO.excecao();
        assertEquals("arquivo", erro.detalhes().get(0).campo());
        assertEquals(MotivoRecusa.TIPO_NAO_SUPORTADO.mensagem(), MotivoRecusa.TIPO_NAO_SUPORTADO.excecao().getMessage());
    }

    @Test
    void mensagensNaoCitamCaminhoBibliotecaOuPilha() {
        for (MotivoRecusa m : MotivoRecusa.values()) {
            String texto = (m.mensagem() + " " + m.excecao().getMessage()).toLowerCase(Locale.ROOT);
            assertNotNull(m.mensagem());
            for (String proibido : new String[] {"pdfbox", "exception", "java.", "\\", "/tmp", ".tmp", "quarentena",
                    "stack", "at br."}) {
                assertFalse(texto.contains(proibido), m.name() + " cita " + proibido);
            }
        }
    }
}
