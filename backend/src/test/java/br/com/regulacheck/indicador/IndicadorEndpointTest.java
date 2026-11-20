package br.com.regulacheck.indicador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.indicador.repository.ContagensIndicadores;
import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * RC-20 / T03 de {@code CONTRATO_TESTES.md}: {@code GET /admin/indicadores} na API real (Jetty) com repositorios em
 * memoria. A matriz e a mesma da rota de exemplo da RC-05 ({@code MatrizAdministradorTest}): administrador recebe so
 * agregados; paciente 403 sem consulta; sem sessao 401.
 */
class IndicadorEndpointTest {

    private static final String ROTA = "/admin/indicadores";

    /**
     * Dados de teste: 7 pedidos, 1 em falha; vigentes 2 com pendencias e 1 a conferir (nenhuma sem pendencias); seis
     * regras com pendencias, das quais a RC-R06 fica fora do corte de 5.
     */
    private static ContagensIndicadores dadosDeTeste() {
        return new ContagensIndicadores(7, 1,
                Map.of(ResultadoGeral.COM_PENDENCIAS, 2L, ResultadoGeral.REQUER_CONFERENCIA, 1L),
                List.of(new PendenciasDaRegra("RC-R03", "Documento de identificação", 1, 0),
                        new PendenciasDaRegra("RC-R02", "Identificação coerente entre documentos", 1, 1),
                        new PendenciasDaRegra("RC-R01", "Encaminhamento presente", 2, 0),
                        new PendenciasDaRegra("RC-R04", "Motivo do encaminhamento informado", 0, 1),
                        new PendenciasDaRegra("RC-R05", "Encaminhamento dentro da validade", 1, 0),
                        new PendenciasDaRegra("RC-R06", "Autorização do responsável legal", 0, 1)));
    }

    @Test
    void administradorRecebeAgregadosIguaisAosDadosDeTeste() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.indicadores.definir(dadosDeTeste());
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);

            Resposta r = api.get(ROTA, ServidorDeTeste.bearer(token));

            assertEquals(200, r.status());
            JsonNode corpo = r.json();
            assertTrue(corpo.get("dadosSinteticos").asBoolean());
            assertEquals("2026-11-12T20:00:00Z", corpo.get("consultadoEm").asText(), "relogio do cenario");
            assertEquals(7, corpo.get("totalPedidos").asLong());
            assertEquals(1, corpo.get("pedidosEmFalhaAnalise").asLong());
            JsonNode analises = corpo.get("analisesPorResultado");
            assertEquals(0, analises.get("semPendenciasIdentificadas").asLong());
            assertEquals(2, analises.get("comPendencias").asLong());
            assertEquals(1, analises.get("requerConferencia").asLong());

            JsonNode protocolos = corpo.get("protocolosAtivos");
            assertEquals(1, protocolos.size());
            JsonNode protocolo = protocolos.get(0);
            assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", protocolo.get("protocoloId").asText());
            assertEquals("v0.1", protocolo.get("versao").asText());
            assertEquals(6, protocolo.get("quantidadeRegras").asInt());
            assertEquals("RC-R01", protocolo.get("regras").get(0).get("codigo").asText());
            assertEquals("Encaminhamento presente", protocolo.get("regras").get(0).get("descricao").asText());
            assertEquals(Set.of("codigo", "descricao"), campos(protocolo.get("regras").get(0)));

            // Totais: R01=2, R02=2, R03=1, R04=1, R05=1, R06=1 -> corte em 5 por total desc e codigo asc.
            List<String> codigos = new ArrayList<>();
            corpo.get("regrasComMaisPendencias").forEach(regra -> codigos.add(regra.get("codigo").asText()));
            assertEquals(List.of("RC-R01", "RC-R02", "RC-R03", "RC-R04", "RC-R05"), codigos);
            JsonNode r02 = corpo.get("regrasComMaisPendencias").get(1);
            assertEquals(2, r02.get("total").asLong());
            assertEquals(1, r02.get("pendentes").asLong());
            assertEquals(1, r02.get("naoVerificaveis").asLong());
        }
    }

    @Test
    void respostaSoTemCamposAgregadosESemDadoIdentificavel() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.indicadores.definir(dadosDeTeste());
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String tokenA = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);
            // Um pedido real da paciente A existe no cenario: seu identificador nao pode aparecer na resposta.
            String pedido = api.postJson("/solicitacoes", "{\"protocoloId\":\"PROTOCOLO-ACADEMICO-FICTICIO-v0.1\"}",
                    ServidorDeTeste.bearer(tokenA)).json().get("id").asText();
            String token = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);

            Resposta r = api.get(ROTA, ServidorDeTeste.bearer(token));

            assertEquals(200, r.status());
            assertEquals(Set.of("dadosSinteticos", "aviso", "consultadoEm", "protocolosAtivos", "totalPedidos",
                    "analisesPorResultado", "pedidosEmFalhaAnalise", "regrasComMaisPendencias"), campos(r.json()));
            assertEquals(Set.of("protocoloId", "identificador", "versao", "quantidadeRegras", "regras"),
                    campos(r.json().get("protocolosAtivos").get(0)));
            assertEquals(Set.of("codigo", "descricao", "total", "pendentes", "naoVerificaveis"),
                    campos(r.json().get("regrasComMaisPendencias").get(0)));
            List<String> proibidos = new ArrayList<>(List.of("@", "Paciente", "email", "nome", "pacienteId",
                    "caminho", "arquivo", "trecho", "resumo", "mensagem", "\"" + pedido + "\""));
            ProtocoloRepositoryMemoria.regrasV01().forEach(regra -> proibidos.add(regra.mensagem()));
            for (String proibido : proibidos) {
                assertFalse(r.corpo().contains(proibido), "indicador identificavel ou clinico: " + proibido);
            }
        }
    }

    @Test
    void pacienteRecebe403SemQueOsIndicadoresSejamConsultados() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.indicadores.definir(dadosDeTeste());
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);

            Resposta r = api.get(ROTA, ServidorDeTeste.bearer(token));

            assertEquals(403, r.status());
            assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains("totalPedidos"));
            assertEquals(0, cenario.indicadores.consultas(), "o 403 deve ocorrer antes de qualquer consulta");
        }
    }

    @Test
    void semSessaoRecebe401SemConsulta() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Resposta semToken = api.get(ROTA, Map.of());
            Resposta tokenInvalido = api.get(ROTA, ServidorDeTeste.bearer("token-que-nao-existe"));

            assertEquals(401, semToken.status());
            assertEquals("NAO_AUTENTICADO", semToken.json().get("codigo").asText());
            assertEquals(401, tokenInvalido.status());
            assertEquals(0, cenario.indicadores.consultas());
        }
    }

    @Test
    void outroMetodoDoAdministradorRecebe405() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);

            Resposta r = api.postJson(ROTA, "{}", ServidorDeTeste.bearer(token));

            assertEquals(405, r.status());
            assertEquals("METODO_NAO_PERMITIDO", r.json().get("codigo").asText());
            assertEquals(0, cenario.indicadores.consultas());
        }
    }

    @Test
    void falhaDoBancoVira503PadraoSemDetalheInterno() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        cenario.indicadores.falharNaProximaConsulta(new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes."));
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);

            Resposta r = api.get(ROTA, ServidorDeTeste.bearer(token));

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains("SQL"));
        }
    }

    @Test
    void bancoSemDadosDevolve200ComZeros() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            String token = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);

            JsonNode corpo = api.get(ROTA, ServidorDeTeste.bearer(token)).json();

            assertEquals(0, corpo.get("totalPedidos").asLong());
            assertEquals(0, corpo.get("analisesPorResultado").get("semPendenciasIdentificadas").asLong());
            assertEquals(0, corpo.get("regrasComMaisPendencias").size());
        }
    }

    private static Set<String> campos(JsonNode objeto) {
        Set<String> nomes = new java.util.HashSet<>();
        objeto.fieldNames().forEachRemaining(nomes::add);
        return nomes;
    }
}
