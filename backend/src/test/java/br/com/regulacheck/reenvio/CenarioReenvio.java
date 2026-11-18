package br.com.regulacheck.reenvio;

import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;

import br.com.regulacheck.analise.service.AnaliseService;
import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.armazenamento.ArmazenamentoEmDisco;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.extracao.service.ConferenciaService;
import br.com.regulacheck.extracao.service.ExtracaoService;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import br.com.regulacheck.reenvio.service.ReenvioService;
import br.com.regulacheck.resumo.service.GeradorResumo;
import br.com.regulacheck.resumo.service.ResumoService;
import br.com.regulacheck.solicitacao.service.SolicitacaoService;
import br.com.regulacheck.usuario.entity.Perfil;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atalhos dos testes da verificacao da versao corrigida (RC-16): a API em memoria (envio, substituicao, leitura
 * simulada, conferencia, analise e reenvio pelas rotas reais) e o {@link ReenvioService} montado sobre os mesmos
 * repositorios, com leitor, avaliacao e gerador de resumo trocaveis para provocar falhas por etapa. So dados
 * sinteticos ({@code AmostrasSinteticas}).
 */
public final class CenarioReenvio {

    /** Sessao da paciente A (conta 1 do {@link CenarioAuth}). */
    public static final SessaoAutenticada PACIENTE_A = new SessaoAutenticada(101, 1, "Paciente A (ficticia)",
            Perfil.PACIENTE, Instant.MAX);

    private CenarioReenvio() {
    }

    /** Pedido preparado: id e ids dos documentos, na ordem de envio. */
    public record Pedido(String id, List<String> documentos) {
        /** @return o id numerico do pedido */
        public long numero() {
            return Long.parseLong(id);
        }
    }

    /** POST /solicitacoes/{id}/reenvio (sem corpo). */
    public static Resposta reenviar(CenarioDocumento c, Map<String, String> auth, String pedido) throws Exception {
        return c.api.enviar("POST", "/solicitacoes/" + pedido + "/reenvio", null, auth);
    }

    /** GET /solicitacoes/{id}/analises. */
    public static JsonNode historico(CenarioDocumento c, String pedido) throws Exception {
        Resposta r = c.api.get("/solicitacoes/" + pedido + "/analises", c.authA);
        exigir(r, 200);
        return r.json();
    }

    /** POST /solicitacoes/{id}/analises (sem corpo). */
    public static Resposta analisar(CenarioDocumento c, String pedido) throws Exception {
        return c.api.enviar("POST", "/solicitacoes/" + pedido + "/analises", null, c.authA);
    }

    /**
     * Cria o pedido da paciente A, envia as amostras, le, confirma todos os campos identificados e pede a analise
     * pelas rotas reais (a primeira analise do pedido).
     */
    public static Pedido analisado(CenarioDocumento c, Amostra... amostras) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        List<String> documentos = new ArrayList<>();
        for (Amostra amostra : amostras) {
            documentos.add(CenarioExtracao.enviar(c, c.authA, pedido, amostra));
        }
        Resposta leitura = c.lerDocumentos(c.authA, pedido);
        exigir(leitura, 201);
        confirmarTudo(c, pedido, leitura.json().get("id").asText());
        exigir(analisar(c, pedido), 201);
        return new Pedido(pedido, documentos);
    }

    /** Confirma todos os campos identificados da leitura (pela rota real da RC-11). */
    public static void confirmarTudo(CenarioDocumento c, String pedido, String leitura) throws Exception {
        JsonNode conferencia = c.consultarConferencia(c.authA, pedido, leitura).json();
        Map<String, String> itens = new LinkedHashMap<>();
        for (JsonNode documento : conferencia.get("documentos")) {
            JsonNode campos = documento.get("campos");
            if (campos == null) {
                continue;
            }
            for (JsonNode campo : campos) {
                if ("IDENTIFICADO".equals(campo.get("situacao").asText())) {
                    String documentoId = documento.get("documentoId").asText();
                    String codigo = campo.get("codigo").asText();
                    itens.put(documentoId + "/" + codigo, confirmar(documentoId, codigo));
                }
            }
        }
        exigir(c.decidir(c.authA, pedido, leitura, lote(itens.values().toArray(String[]::new))), 200);
    }

    /** Substitui o documento pela amostra (PUT multipart, RC-09) e devolve o id do documento novo. */
    public static String substituir(CenarioDocumento c, String pedido, String documento, Amostra amostra)
            throws Exception {
        Resposta r = c.substituir(c.authA, pedido, documento, CenarioExtracao.tipoInformado(amostra), amostra.nome(),
                amostra.bytes(), null);
        if (r.status() / 100 != 2) {
            throw new IllegalStateException("Substituicao falhou: " + r.status() + " " + r.corpo());
        }
        return r.json().get("id").asText();
    }

    /** Inclui a amostra no pedido (POST multipart) e devolve a resposta. */
    public static Resposta incluir(CenarioDocumento c, String pedido, Amostra amostra) throws Exception {
        return c.enviar(c.authA, pedido, CenarioExtracao.tipoInformado(amostra), amostra.nome(), amostra.bytes(),
                null);
    }

    /**
     * Le, confirma e verifica: o percurso que a paciente faz depois de corrigir (reenvio que para na conferencia,
     * conferencia, reenvio de novo). Devolve a resposta da segunda verificacao.
     */
    public static Resposta verificarConferindo(CenarioDocumento c, String pedido) throws Exception {
        Resposta parada = reenviar(c, c.authA, pedido);
        exigir(parada, 200);
        if (!"CONFERENCIA".equals(parada.json().get("etapa").asText())) {
            throw new IllegalStateException("Esperava parar na conferencia: " + parada.corpo());
        }
        confirmarTudo(c, pedido, parada.json().get("extracaoId").asText());
        return reenviar(c, c.authA, pedido);
    }

    /** @return situacao na comparacao por codigo de regra (vazio se a analise nao tem comparacao) */
    public static Map<String, String> situacoes(JsonNode analise) {
        Map<String, String> situacoes = new LinkedHashMap<>();
        JsonNode comparacao = analise.get("comparacao");
        if (comparacao != null) {
            for (JsonNode item : comparacao.get("itens")) {
                situacoes.put(item.get("codigoRegra").asText(), item.get("situacao").asText());
            }
        }
        return situacoes;
    }

    /** @return {@code true} se alguma analise do historico traz alguma regra como resolvida */
    public static boolean algumaResolvida(JsonNode historico) {
        for (JsonNode analise : historico.get("analises")) {
            if (situacoes(analise).containsValue("RESOLVIDA")) {
                return true;
            }
        }
        return false;
    }

    /** @return avaliacao real (RC-12) sobre os repositorios do cenario */
    public static AvaliacaoService avaliacao(CenarioDocumento c) {
        CenarioAuth a = c.cenario;
        return new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias, a.documentos, a.protocolos,
                MotorDeRegras.doProtocoloAcademico());
    }

    /**
     * Monta o servico de verificacao sobre os repositorios e a area de arquivos do cenario, como a
     * {@code Composicao} faz, com leitor, avaliacao e gerador dados.
     */
    public static ReenvioService servico(CenarioDocumento c, LeitorDeDocumento leitor, AvaliacaoService avaliacao,
            GeradorResumo gerador) {
        CenarioAuth a = c.cenario;
        ArmazenamentoEmDisco armazenamento = new ArmazenamentoEmDisco(c.raiz);
        SolicitacaoService solicitacoes = new SolicitacaoService(new ProtocoloService(a.protocolos), a.solicitacoes,
                armazenamento, a.relogio);
        return new ReenvioService(solicitacoes, new ExtracaoService(a.extracoes, leitor, armazenamento, a.relogio),
                new ConferenciaService(a.conferencias, a.relogio), new AnaliseService(a.analises, avaliacao,
                        a.relogio), new ResumoService(a.analises, gerador));
    }

    private static void exigir(Resposta r, int status) {
        if (r.status() != status) {
            throw new IllegalStateException("Esperava " + status + ", veio " + r.status() + ": " + r.corpo());
        }
    }
}
