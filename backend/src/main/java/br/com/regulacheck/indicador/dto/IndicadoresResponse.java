package br.com.regulacheck.indicador.dto;

import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.indicador.service.Indicadores;
import br.com.regulacheck.indicador.service.Indicadores.ProtocoloAtivo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.util.List;

/**
 * Resposta de {@code GET /admin/indicadores} (RC-20; contrato em
 * {@code specs/024-app-admin-indicadores/contracts/admin-indicadores.md} e {@code docs/api/openapi.yaml}).
 *
 * <p>So agregados e dados do protocolo. Nao ha campo de conta, pedido, arquivo, trecho lido, resumo nem mensagem de
 * pendencia; o "nome" da regra sai como {@code descricao} (research D4 da spec 024).
 *
 * @param dadosSinteticos sempre {@code true}: o banco de demonstracao so tem dados sinteticos
 * @param aviso frase de demonstracao exibivel ao administrador
 * @param consultadoEm instante da consulta em ISO-8601 UTC
 * @param protocolosAtivos protocolos ativos com as regras
 * @param totalPedidos todos os pedidos
 * @param analisesPorResultado analises vigentes por resultado geral
 * @param pedidosEmFalhaAnalise pedidos com estado atual de falha na analise (contados a parte)
 * @param regrasComMaisPendencias ate 5 regras com mais pendencias nas analises vigentes
 */
public record IndicadoresResponse(boolean dadosSinteticos, String aviso, String consultadoEm,
        List<ProtocoloIndicadores> protocolosAtivos, long totalPedidos, AnalisesPorResultado analisesPorResultado,
        long pedidosEmFalhaAnalise, List<RegraComPendencias> regrasComMaisPendencias) {

    /** Aviso fixo; nao afirma reducao de filas nem representa servico real (CLAUDE.md §1). */
    public static final String AVISO = "Indicadores agregados de demonstracao, calculados sobre dados sinteticos. "
            + "Nao representam pacientes, filas ou servicos reais.";

    /**
     * @param protocoloId identificador publico ({@code identificador-versao})
     * @param identificador nome do protocolo
     * @param versao versao do protocolo
     * @param quantidadeRegras quantidade de regras da versao
     * @param regras regras na ordem do protocolo
     */
    public record ProtocoloIndicadores(String protocoloId, String identificador, String versao, int quantidadeRegras,
            List<RegraResumo> regras) {
    }

    /**
     * @param codigo codigo da regra
     * @param descricao nome curto da regra
     */
    public record RegraResumo(String codigo, String descricao) {
    }

    /**
     * @param semPendenciasIdentificadas analises vigentes sem pendencias identificadas
     * @param comPendencias analises vigentes com ao menos uma pendencia
     * @param requerConferencia analises vigentes que pedem conferencia da leitura
     */
    public record AnalisesPorResultado(long semPendenciasIdentificadas, long comPendencias, long requerConferencia) {
    }

    /**
     * @param codigo codigo da regra
     * @param descricao nome curto da regra
     * @param total pendentes + nao verificaveis
     * @param pendentes pendencias do tipo pendente
     * @param naoVerificaveis pendencias do tipo nao verificavel
     */
    public record RegraComPendencias(String codigo, String descricao, long total, long pendentes,
            long naoVerificaveis) {
    }

    /**
     * @param indicadores resultado do caso de uso
     * @return resposta publica correspondente
     */
    public static IndicadoresResponse de(Indicadores indicadores) {
        List<ProtocoloIndicadores> protocolos = indicadores.protocolosAtivos().stream()
                .map(IndicadoresResponse::protocolo)
                .toList();
        var grupos = indicadores.analisesPorResultado();
        AnalisesPorResultado analises = new AnalisesPorResultado(
                grupos.getOrDefault(ResultadoGeral.SEM_PENDENCIAS_IDENTIFICADAS, 0L),
                grupos.getOrDefault(ResultadoGeral.COM_PENDENCIAS, 0L),
                grupos.getOrDefault(ResultadoGeral.REQUER_CONFERENCIA, 0L));
        List<RegraComPendencias> regras = indicadores.regrasComMaisPendencias().stream()
                .map(IndicadoresResponse::regra)
                .toList();
        return new IndicadoresResponse(true, AVISO, indicadores.consultadoEm().toString(), protocolos,
                indicadores.totalPedidos(), analises, indicadores.pedidosEmFalhaAnalise(), regras);
    }

    private static ProtocoloIndicadores protocolo(ProtocoloAtivo ativo) {
        List<RegraResumo> regras = ativo.regras().stream().map(IndicadoresResponse::resumo).toList();
        return new ProtocoloIndicadores(ativo.protocolo().protocoloId(), ativo.protocolo().identificador(),
                ativo.protocolo().versao(), regras.size(), regras);
    }

    private static RegraResumo resumo(RegraDoProtocolo regra) {
        return new RegraResumo(regra.codigo(), regra.descricao());
    }

    private static RegraComPendencias regra(PendenciasDaRegra regra) {
        return new RegraComPendencias(regra.codigo(), regra.descricao(), regra.total(), regra.pendentes(),
                regra.naoVerificaveis());
    }
}
