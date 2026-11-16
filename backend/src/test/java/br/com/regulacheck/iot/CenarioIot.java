package br.com.regulacheck.iot;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_ADMIN;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.documento.CenarioDocumento;
import java.time.Instant;
import java.util.Map;

/**
 * Cenario dos testes da estacao de captura (RC-21/RC-22): API em memoria com a chave do simulador configurada,
 * pacientes A e B, administrador e atalhos para abrir sessao, enviar leitura e consultar a telemetria.
 */
final class CenarioIot implements AutoCloseable {

    /** Chave ficticia do simulador, so para teste. */
    static final String CHAVE = "chave-ficticia-do-simulador-iot-0123456789";

    final CenarioAuth cenario;
    final ServidorDeTeste api;
    final Map<String, String> authA;
    final Map<String, String> authB;
    final Map<String, String> admin;
    final Map<String, String> simulador = Map.of("X-Chave-Simulador", CHAVE);

    private CenarioIot(CenarioAuth cenario, ServidorDeTeste api) throws Exception {
        this.cenario = cenario;
        this.api = api;
        this.authA = bearer(api.entrar(EMAIL_A, SENHA));
        this.authB = bearer(api.entrar(EMAIL_B, SENHA));
        this.admin = bearer(api.entrar(EMAIL_ADMIN, SENHA));
    }

    static CenarioIot iniciar() throws Exception {
        return iniciar(CHAVE);
    }

    /**
     * @param chaveDoServidor chave configurada na API ({@code null} = sem chave)
     */
    static CenarioIot iniciar(String chaveDoServidor) throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, chaveDoServidor);
        return new CenarioIot(cenario, ServidorDeTeste.iniciar(config, cenario.dependencias()));
    }

    String criarPedido(Map<String, String> auth) throws Exception {
        Resposta r = api.postJson("/solicitacoes", CenarioDocumento.CORPO_RASCUNHO, auth);
        if (r.status() != 201) {
            throw new IllegalStateException("Criar pedido deveria ser 201, foi " + r.status());
        }
        return r.json().get("id").asText();
    }

    Resposta abrirSessao(Map<String, String> auth, String pedidoId) throws Exception {
        return api.enviar("POST", "/solicitacoes/" + pedidoId + "/sessoes-captura", null, auth);
    }

    /** Abre a sessao da paciente A e devolve o id. */
    String sessaoAberta(String pedidoId) throws Exception {
        Resposta r = abrirSessao(authA, pedidoId);
        if (r.status() != 201 && r.status() != 200) {
            throw new IllegalStateException("Abrir sessao deveria ser 201/200, foi " + r.status());
        }
        return r.json().get("id").asText();
    }

    Resposta enviar(String sessaoId, String json) throws Exception {
        return api.postJson("/sessoes-captura/" + sessaoId + "/leituras", json, simulador);
    }

    /** Envia uma leitura valida medida agora (relogio do cenario). */
    Resposta enviar(String sessaoId, String tipo, String valor) throws Exception {
        return enviar(sessaoId, leitura(sessaoId, tipo, valor, unidadeDe(tipo), agora()));
    }

    Resposta telemetria(Map<String, String> auth, String pedidoId) throws Exception {
        return api.get("/solicitacoes/" + pedidoId + "/leituras", auth);
    }

    Instant agora() {
        return cenario.relogio.instant();
    }

    static String leitura(String sessaoId, String tipo, String valor, String unidade, Instant medidaEm) {
        return "{\"sessaoId\":" + sessaoId + ",\"tipo\":\"" + tipo + "\",\"valor\":" + valor + ",\"unidade\":\""
                + unidade + "\",\"medidaEm\":\"" + medidaEm + "\"}";
    }

    static String unidadeDe(String tipo) {
        return "LUMINOSIDADE".equals(tipo) ? "LUX" : "CM";
    }

    @Override
    public void close() throws Exception {
        api.close();
    }
}
