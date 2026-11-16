package br.com.regulacheck.resumo.service;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.repository.AnaliseRepository;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;

/** Gera e publica a orientacao simulada da analise vigente sem mudar as regras. */
public final class ResumoService {

    private static final String PADRAO = "Resumo simulado indisponível. Consulte as pendências por regra e tente "
            + "gerar o resumo novamente. Nenhuma conclusão adicional foi tirada.";

    private final AnaliseRepository analises;
    private final GeradorResumo gerador;

    /**
     * @param analises persistencia das analises
     * @param gerador adaptador simulado
     */
    public ResumoService(AnaliseRepository analises, GeradorResumo gerador) {
        this.analises = analises;
        this.gerador = gerador;
    }

    /** @return analise com resumo e indicacao de criacao nesta chamada */
    public Resultado gerar(SessaoAutenticada sessao, long solicitacaoId) {
        var historico = analises.historico(sessao.usuarioId(), solicitacaoId, 1)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
        Analise vigente = historico.analises().stream().filter(Analise::vigente).findFirst()
                .orElseThrow(() -> new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                        "Peça a análise da revisão atual antes de gerar o resumo."));
        if ("ADAPTADOR_MOCKADO".equals(vigente.resumoOrigem())) {
            return new Resultado(vigente, false);
        }
        String texto = PADRAO;
        String origem = "PADRAO";
        try {
            GeradorResumo.Saida proposta = gerador.gerar(vigente);
            if (ValidadorResumo.valida(vigente, proposta)) {
                texto = proposta.texto();
                origem = "ADAPTADOR_MOCKADO";
            }
        } catch (RuntimeException falha) {
            // O adaptador e dado nao confiavel; nao registrar mensagem ou conteudo na falha.
        }
        Analise gravada = analises.guardarResumo(sessao.usuarioId(), solicitacaoId, vigente.id(), texto, origem)
                .orElseThrow(() -> new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                        "A análise mudou. Consulte a revisão atual e tente novamente."));
        return new Resultado(gravada, true);
    }

    /** Analise atualizada ou ja resumida. */
    public record Resultado(Analise analise, boolean criada) {
    }
}
