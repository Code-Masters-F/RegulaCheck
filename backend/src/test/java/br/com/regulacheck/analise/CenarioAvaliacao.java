package br.com.regulacheck.analise;

import static br.com.regulacheck.extracao.CenarioConferencia.confirmar;
import static br.com.regulacheck.extracao.CenarioConferencia.lote;

import br.com.regulacheck.analise.service.AvaliacaoService;
import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.AmostrasSinteticas.Amostra;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.usuario.entity.Perfil;
import br.com.regulacheck.validacao.motor.MotorDeRegras;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atalhos dos testes da avaliacao (RC-12): a API em memoria (envio, leitura simulada e conferencia pelas rotas
 * reais) e o {@link AvaliacaoService} montado sobre os mesmos repositorios em memoria.
 */
final class CenarioAvaliacao {

    /** Sessao da paciente A (conta 1 do {@link CenarioAuth}). */
    static final SessaoAutenticada PACIENTE_A = new SessaoAutenticada(101, 1, "Paciente A (ficticia)",
            Perfil.PACIENTE, Instant.parse("2026-11-14T00:00:00Z"));
    /** Sessao da paciente B (conta 2 do {@link CenarioAuth}). */
    static final SessaoAutenticada PACIENTE_B = new SessaoAutenticada(102, 2, "Paciente B (ficticia)",
            Perfil.PACIENTE, Instant.parse("2026-11-14T00:00:00Z"));

    private CenarioAvaliacao() {
    }

    /** Pedido preparado: id do pedido, ids dos documentos (na ordem de envio) e id da leitura. */
    record Pedido(String id, List<String> documentos, String leitura) {
        long numero() {
            return Long.parseLong(id);
        }
    }

    /** @return servico de avaliacao sobre os repositorios em memoria do cenario */
    static AvaliacaoService servico(CenarioDocumento c) {
        CenarioAuth a = c.cenario;
        return new AvaliacaoService(a.solicitacoes, a.extracoes, a.conferencias, a.documentos, a.protocolos,
                MotorDeRegras.doProtocoloAcademico());
    }

    /** Cria o pedido da paciente A, envia as amostras e pede a leitura simulada (sem conferir nada). */
    static Pedido lido(CenarioDocumento c, Amostra... amostras) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        List<String> documentos = new ArrayList<>();
        for (Amostra amostra : amostras) {
            documentos.add(CenarioExtracao.enviar(c, c.authA, pedido, amostra));
        }
        String leitura = c.lerDocumentos(c.authA, pedido).json().get("id").asText();
        return new Pedido(pedido, documentos, leitura);
    }

    /** Confirma todos os campos identificados da leitura (pela rota real da RC-11). */
    static void confirmarTudo(CenarioDocumento c, Pedido pedido) throws Exception {
        confirmarTudo(c, pedido, Map.of());
    }

    /**
     * Confirma todos os campos identificados da leitura, trocando alguns itens do lote.
     *
     * @param substituicoes item do lote a usar no lugar da confirmacao, por chave {@code documentoId/CODIGO}
     *     (ex.: uma divergencia); a chave pode ser de campo nao identificado (o item e so acrescentado)
     */
    static void confirmarTudo(CenarioDocumento c, Pedido pedido, Map<String, String> substituicoes)
            throws Exception {
        var conferencia = c.consultarConferencia(c.authA, pedido.id(), pedido.leitura()).json();
        Map<String, String> itens = new LinkedHashMap<>();
        for (var documento : conferencia.get("documentos")) {
            var campos = documento.get("campos");
            if (campos == null) {
                continue;
            }
            for (var campo : campos) {
                if ("IDENTIFICADO".equals(campo.get("situacao").asText())) {
                    String documentoId = documento.get("documentoId").asText();
                    String codigo = campo.get("codigo").asText();
                    itens.put(documentoId + "/" + codigo, confirmar(documentoId, codigo));
                }
            }
        }
        itens.putAll(substituicoes);
        var resposta = c.decidir(c.authA, pedido.id(), pedido.leitura(), lote(itens.values().toArray(String[]::new)));
        if (resposta.status() != 200) {
            throw new IllegalStateException("Conferencia falhou: " + resposta.status());
        }
    }
}
