package br.com.regulacheck.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Catalogo de servicos e resolucao do {@code protocoloId}, sem HTTP e sem banco. */
class ProtocoloServiceTest {

    private final ProtocoloRepositoryMemoria repositorio = new ProtocoloRepositoryMemoria();
    private final ProtocoloService servico = new ProtocoloService(repositorio);

    @Test
    void listaSoOsProtocolosAtivos() {
        repositorio.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        repositorio.adicionar(ProtocoloRepositoryMemoria.segundo(), false);

        List<Protocolo> lista = servico.listarAtivos();

        assertEquals(1, lista.size());
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", lista.get(0).protocoloId());
    }

    @Test
    void catalogoVazioDevolveListaVaziaSemErro() {
        assertTrue(servico.listarAtivos().isEmpty());
    }

    @Test
    void exigirAtivoDevolveOProtocolo() {
        repositorio.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        assertEquals(1L, servico.exigirAtivo("PROTOCOLO-ACADEMICO-FICTICIO-v0.1").id());
    }

    @Test
    void inexistenteEInativoDaoARespostaIdentica() {
        repositorio.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        repositorio.adicionar(ProtocoloRepositoryMemoria.segundo(), false);

        ExcecaoNegocio inexistente = assertThrows(ExcecaoNegocio.class, () -> servico.exigirAtivo("NAO-EXISTE-v9"));
        ExcecaoNegocio inativo = assertThrows(ExcecaoNegocio.class,
                () -> servico.exigirAtivo("PROTOCOLO-ACADEMICO-FICTICIO-v0.2"));

        for (ExcecaoNegocio e : List.of(inexistente, inativo)) {
            assertEquals(422, e.status());
            assertEquals("REGRA_DE_NEGOCIO", e.codigo());
            assertEquals("protocoloId", e.detalhes().get(0).campo());
            assertEquals("Escolha um dos serviços da lista.", e.detalhes().get(0).mensagem());
        }
        assertEquals(inexistente.getMessage(), inativo.getMessage());
    }

    @Test
    void semNenhumProtocoloAtivoNadaPodeSerEscolhido() {
        ExcecaoNegocio e = assertThrows(ExcecaoNegocio.class,
                () -> servico.exigirAtivo("PROTOCOLO-ACADEMICO-FICTICIO-v0.1"));
        assertEquals(422, e.status());
    }
}
