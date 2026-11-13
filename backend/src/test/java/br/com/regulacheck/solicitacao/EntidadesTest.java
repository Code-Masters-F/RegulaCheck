package br.com.regulacheck.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.solicitacao.entity.MotivoRevisao;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Conjuntos fechados do codigo iguais as restricoes do banco, e identificador publico do servico. */
class EntidadesTest {

    @Test
    void protocoloIdEIdentificadorMaisVersao() {
        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", ProtocoloRepositoryMemoria.padrao().protocoloId());
    }

    @Test
    void estadosIguaisARestricaoDoBanco() {
        // Lista copiada de RC_SOLICITACAO_CK_ESTADO (database/scripts/01_criar_esquema.sql).
        List<String> banco = List.of("RASCUNHO", "PROCESSANDO", "REQUER_CONFERENCIA", "COM_PENDENCIAS",
                "SEM_PENDENCIAS_IDENTIFICADAS", "FALHA_ANALISE", "SUBMETIDA_SIMULACAO");
        assertEquals(banco, Arrays.stream(EstadoSolicitacao.values()).map(Enum::name).toList());
    }

    @Test
    void motivosIguaisARestricaoDoBanco() {
        // Lista copiada de RC_REVISAO_CK_MOTIVO (01_criar_esquema.sql e 06_motivo_revisao_protocolo.sql).
        List<String> banco = List.of("CRIACAO", "DOCUMENTO_ENVIADO", "DOCUMENTO_SUBSTITUIDO", "DOCUMENTO_REMOVIDO",
                "CONFERENCIA_ALTERADA", "PROTOCOLO_ALTERADO");
        assertEquals(banco, Arrays.stream(MotivoRevisao.values()).map(Enum::name).toList());
    }
}
