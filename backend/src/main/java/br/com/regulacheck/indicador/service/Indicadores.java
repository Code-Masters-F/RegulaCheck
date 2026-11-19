package br.com.regulacheck.indicador.service;

import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resultado da consulta de indicadores do administrador (RC-20), ja pronto para apresentacao: grupos de resultado
 * completos e regras ordenadas e cortadas. Modelo interno; o contrato publico e
 * {@link br.com.regulacheck.indicador.dto.IndicadoresResponse}.
 *
 * @param protocolosAtivos protocolos ativos com as regras da versao, na ordem do catalogo
 * @param totalPedidos todos os pedidos
 * @param analisesPorResultado analises vigentes por resultado geral, com todos os valores de {@link ResultadoGeral}
 * @param pedidosEmFalhaAnalise pedidos com estado atual de falha na analise
 * @param regrasComMaisPendencias ate {@link IndicadorService#LIMITE_REGRAS} regras, total desc e codigo asc
 * @param consultadoEm instante da consulta (UTC)
 */
public record Indicadores(List<ProtocoloAtivo> protocolosAtivos, long totalPedidos,
        Map<ResultadoGeral, Long> analisesPorResultado, long pedidosEmFalhaAnalise,
        List<PendenciasDaRegra> regrasComMaisPendencias, Instant consultadoEm) {

    /**
     * Protocolo ativo com as regras da sua versao.
     *
     * @param protocolo protocolo lido do catalogo
     * @param regras regras da versao, na ordem do protocolo
     */
    public record ProtocoloAtivo(Protocolo protocolo, List<RegraDoProtocolo> regras) {
    }
}
