package br.com.regulacheck.extracao;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import java.util.Map;

/** Atalhos dos testes de extracao (RC-10) sobre o {@link CenarioDocumento}. */
public final class CenarioExtracao {

    private CenarioExtracao() {
    }

    /** Inicia a API com o leitor dado, repositorios em memoria e area de arquivos temporaria. */
    public static CenarioDocumento iniciar(LeitorDeDocumento leitor) throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        return CenarioDocumento.iniciar(cenario, cenario.dependencias(leitor), true);
    }

    /** Inicia a API com o leitor simulado que conhece todas as amostras (oficiais e extras). */
    public static CenarioDocumento iniciarComSimulado() throws Exception {
        return iniciar(LeitoresDeTeste.simuladoComTodasAsAmostras());
    }

    /** Tipo documental informado no envio, coerente com a amostra. */
    public static String tipoInformado(Amostra amostra) {
        return amostra == AmostrasSinteticas.IDENTIFICACAO || amostra == AmostrasSinteticas.BORRADA
                ? "IDENTIFICACAO" : "ENCAMINHAMENTO";
    }

    /** Envia uma amostra ao pedido e devolve o id do documento criado. */
    public static String enviar(CenarioDocumento c, Map<String, String> auth, String pedido, Amostra amostra)
            throws Exception {
        var r = c.enviar(auth, pedido, tipoInformado(amostra), amostra.nome(), amostra.bytes(), null);
        if (r.status() != 201) {
            throw new IllegalStateException("Envio da amostra falhou: " + r.status());
        }
        return r.json().get("id").asText();
    }

    /** Cria um rascunho da paciente e envia as amostras; devolve o id do pedido. */
    public static String pedidoCom(CenarioDocumento c, Map<String, String> auth, Amostra... amostras)
            throws Exception {
        String pedido = c.criarRascunho(auth);
        for (Amostra amostra : amostras) {
            enviar(c, auth, pedido, amostra);
        }
        return pedido;
    }
}
