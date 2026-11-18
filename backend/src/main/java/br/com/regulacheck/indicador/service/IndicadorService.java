package br.com.regulacheck.indicador.service;

import br.com.regulacheck.indicador.repository.ContagensIndicadores;
import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.indicador.repository.IndicadorRepository;
import br.com.regulacheck.indicador.service.Indicadores.ProtocoloAtivo;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso "consultar protocolo e indicadores" do administrador (RC-20, spec 024).
 *
 * <p>So agregados: o Service nao recebe a conta porque a rota nao tem recurso por dono ({@code CONTRATO_API.md}); o
 * perfil {@code ADMINISTRADOR} e exigido antes, por {@code RotasPrivadas}. Nada e gravado.
 */
public class IndicadorService {

    /** Quantidade maxima de regras em "regras com mais pendencias" (FR-005). */
    public static final int LIMITE_REGRAS = 5;

    private static final Comparator<PendenciasDaRegra> MAIS_PENDENCIAS = Comparator
            .comparingLong(PendenciasDaRegra::total).reversed()
            .thenComparing(PendenciasDaRegra::codigo);

    private final IndicadorRepository indicadores;
    private final ProtocoloRepository protocolos;
    private final Clock relogio;

    /**
     * @param indicadores contagens agregadas
     * @param protocolos catalogo de protocolos (protocolos ativos e regras)
     * @param relogio relogio da aplicacao, para o instante da consulta
     */
    public IndicadorService(IndicadorRepository indicadores, ProtocoloRepository protocolos, Clock relogio) {
        this.indicadores = indicadores;
        this.protocolos = protocolos;
        this.relogio = relogio;
    }

    /**
     * Monta os indicadores do momento: protocolos ativos com as regras, total de pedidos, analises vigentes por
     * resultado (todos os grupos, zero quando nao ha), pedidos em falha e as regras com mais pendencias.
     *
     * @return indicadores agregados, sem dado identificavel
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 se o banco falhar
     */
    public Indicadores consultar() {
        ContagensIndicadores contagens = indicadores.contar();
        List<ProtocoloAtivo> ativos = new ArrayList<>();
        for (Protocolo protocolo : protocolos.listarAtivos()) {
            ativos.add(new ProtocoloAtivo(protocolo, protocolos.listarRegras(protocolo.id())));
        }
        Map<ResultadoGeral, Long> porResultado = new EnumMap<>(ResultadoGeral.class);
        for (ResultadoGeral resultado : ResultadoGeral.values()) {
            porResultado.put(resultado, contagens.analisesVigentesPorResultado().getOrDefault(resultado, 0L));
        }
        List<PendenciasDaRegra> regras = contagens.pendenciasPorRegra().stream()
                .filter(regra -> regra.total() > 0)
                .sorted(MAIS_PENDENCIAS)
                .limit(LIMITE_REGRAS)
                .toList();
        return new Indicadores(List.copyOf(ativos), contagens.totalPedidos(), porResultado,
                contagens.pedidosEmFalhaAnalise(), regras, relogio.instant());
    }
}
