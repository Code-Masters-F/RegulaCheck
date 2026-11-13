package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import org.junit.jupiter.api.Test;

/**
 * FR-008: repetir a substituicao com a mesma identificacao de tentativa nao duplica nada, e isso e reconhecido
 * antes de procurar o documento do caminho (que, depois do sucesso, ja nao existe).
 */
class IdempotenciaSubstituicaoTest {

    private static final String CHAVE = "troca-0001-abc";

    @Test
    void repetirATrocaDevolve200ComOMesmoDocumentoSemGravarNadaNovo() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String antigo = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();
            byte[] corrigido = ArquivosDeTeste.pdf(2);
            Resposta primeira = c.substituir(c.authA, pedido, antigo, "ENCAMINHAMENTO", "novo.pdf", corrigido, CHAVE);
            assertEquals(201, primeira.status(), primeira.corpo());
            int revisoes = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size();

            // Mesmo caminho (o documento antigo ja nao existe), mesma chave, mesmo conteudo e tipo.
            Resposta repetida = c.substituir(c.authA, pedido, antigo, "ENCAMINHAMENTO", "novo.pdf", corrigido, CHAVE);

            assertEquals(200, repetida.status(), repetida.corpo());
            assertEquals(primeira.json().get("id").asText(), repetida.json().get("id").asText());
            assertEquals(primeira.json().get("versaoConjuntoDocumental").asInt(),
                    repetida.json().get("versaoConjuntoDocumental").asInt());
            assertEquals(revisoes, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size(),
                    "a repeticao nao cria revisao");
            assertEquals(1, c.cenario.documentos.todos().size());
            assertEquals(1, c.cenario.documentos.removidos().size(), "so o antigo foi removido, uma vez");
            assertEquals(1, c.arquivosGuardados());
            assertEquals(0, c.arquivosNaQuarentena());
            c.conferirIntegridade();
        }
    }

    @Test
    void mesmaChaveComConteudoOuTipoDiferenteEhConflitoENadaMuda() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            String antigo = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", ArquivosDeTeste.pdf(1), null).json()
                    .get("id").asText();
            byte[] corrigido = ArquivosDeTeste.pdf(2);
            Resposta primeira = c.substituir(c.authA, pedido, antigo, "ENCAMINHAMENTO", "novo.pdf", corrigido, CHAVE);
            String novo = primeira.json().get("id").asText();
            int revisoes = c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size();

            Resposta outroConteudo = c.substituir(c.authA, pedido, novo, "ENCAMINHAMENTO", "novo.pdf",
                    ArquivosDeTeste.pdf(3), CHAVE);
            Resposta outroTipo = c.substituir(c.authA, pedido, novo, "IDENTIFICACAO", "novo.pdf", corrigido, CHAVE);

            for (Resposta r : new Resposta[] {outroConteudo, outroTipo}) {
                assertEquals(409, r.status(), r.corpo());
                assertEquals("CHAVE_ENVIO_REUTILIZADA", r.json().get("codigo").asText());
            }
            assertEquals(revisoes, c.cenario.solicitacoes.revisoesDe(Long.parseLong(pedido)).size());
            assertEquals(1, c.arquivosGuardados(), "nenhum arquivo novo ficou guardado");
            assertEquals(0, c.arquivosNaQuarentena());
            c.conferirIntegridade();
        }
    }

    @Test
    void aMesmaChaveVaiParaOEnvioEParaATroca() throws Exception {
        try (CenarioDocumento c = CenarioDocumento.iniciar()) {
            String pedido = c.criarRascunho(c.authA);
            byte[] conteudo = ArquivosDeTeste.pdf(1);
            Resposta envio = c.enviar(c.authA, pedido, "OUTRO", "a.pdf", conteudo, CHAVE);
            assertEquals(201, envio.status());
            String outro = c.enviar(c.authA, pedido, "OUTRO", "b.pdf", ArquivosDeTeste.pdf(2), null).json()
                    .get("id").asText();

            // A chave e por tentativa, nao por rota: repetir o mesmo conteudo com ela devolve o documento do envio.
            Resposta igual = c.substituir(c.authA, pedido, outro, "OUTRO", "a.pdf", conteudo, CHAVE);
            Resposta diferente = c.substituir(c.authA, pedido, outro, "OUTRO", "z.pdf", ArquivosDeTeste.pdf(3), CHAVE);

            assertEquals(200, igual.status(), igual.corpo());
            assertEquals(envio.json().get("id").asText(), igual.json().get("id").asText());
            assertEquals(409, diferente.status());
            assertEquals(2, c.cenario.documentos.todos().size(), "nenhum dos dois foi trocado");
            c.conferirIntegridade();
        }
    }
}
