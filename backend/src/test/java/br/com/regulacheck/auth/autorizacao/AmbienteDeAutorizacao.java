package br.com.regulacheck.auth.autorizacao;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import br.com.regulacheck.config.Configuracao;
import java.util.Map;

/**
 * Ambiente dos testes de autorizacao: API real em Jetty com as contas ficticias (pacientes A e B,
 * paciente desativada e administrador), os recursos de exemplo e a chave ficticia do simulador.
 */
final class AmbienteDeAutorizacao implements AutoCloseable {

    /** Chave ficticia, so de teste; o repositorio nunca guarda chave real. */
    static final String CHAVE = "chave-de-teste-ficticia";

    final CenarioAuth cenario = new CenarioAuth();
    final RecursosDeExemplo recursos = new RecursosDeExemplo();
    final ServidorDeTeste api;
    final String tokenA;
    final String tokenB;
    final String tokenAdmin;

    AmbienteDeAutorizacao(String chaveSimulador) throws Exception {
        Configuracao configuracao = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, chaveSimulador);
        api = ServidorDeTeste.iniciarComRotas(configuracao, cenario.dependencias(),
                rotas -> RotasDeExemplo.registrar(rotas, recursos));
        tokenA = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);
        tokenB = api.entrar(CenarioAuth.EMAIL_B, CenarioAuth.SENHA);
        tokenAdmin = api.entrar(CenarioAuth.EMAIL_ADMIN, CenarioAuth.SENHA);
    }

    AmbienteDeAutorizacao() throws Exception {
        this(CHAVE);
    }

    /** @return caminho do recurso do tipo e indice informados (documento inclui a solicitacao pai) */
    static String caminho(Tipo tipo, long id, long pai) {
        return tipo == Tipo.DOCUMENTO ? "/teste/documentos/" + pai + "/" + id : "/teste/" + tipo.caminho() + "/" + id;
    }

    /** @return caminho do recurso do indice (1 = A, 2 = B, 3 = desativada) */
    static String caminho(Tipo tipo, int indice) {
        return caminho(tipo, tipo.id(indice), Tipo.SOLICITACAO.id(indice));
    }

    Resposta chamar(String metodo, String caminho, String token, String json) throws Exception {
        return api.enviar(metodo, caminho, json, token == null ? Map.of() : ServidorDeTeste.bearer(token));
    }

    @Override
    public void close() throws Exception {
        api.close();
    }
}
