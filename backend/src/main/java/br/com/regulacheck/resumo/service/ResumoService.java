package br.com.regulacheck.resumo.service;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.repository.AnaliseRepository;
import br.com.regulacheck.analise.service.ComparacaoDeAnalises;
import br.com.regulacheck.auth.autorizacao.Propriedade;
import br.com.regulacheck.auth.service.SessaoAutenticada;
import br.com.regulacheck.common.exception.ExcecaoNegocio;

/**
 * Gera e publica a orientacao simulada da analise vigente sem mudar as regras. Devolve tambem a comparacao da analise
 * com a anterior (RC-16), para que toda rota que publica uma analise a mostre do mesmo jeito.
 */
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

    /**
     * Gera o resumo simulado da analise vigente, ou devolve o ja gerado pelo adaptador. Falha ou saida invalida do
     * adaptador grava a explicacao padrao identificada, que pode ser substituida numa nova tentativa.
     *
     * @param sessao paciente autenticada
     * @param solicitacaoId pedido
     * @return analise com resumo, indicacao de criacao nesta chamada e comparacao com a anterior
     * @throws ExcecaoNegocio 404 se o pedido nao existe ou e de outra paciente; 409 sem analise vigente ou se a
     *     vigencia mudou durante a geracao; 503 se o banco estiver indisponivel
     */
    public Resultado gerar(SessaoAutenticada sessao, long solicitacaoId) {
        // Duas: a vigente e sempre a mais recente; a segunda serve a comparacao com a anterior.
        var historico = analises.historico(sessao.usuarioId(), solicitacaoId, 2)
                .orElseThrow(() -> Propriedade.naoEncontrada(sessao));
        Analise vigente = historico.analises().stream().filter(Analise::vigente).findFirst()
                .orElseThrow(() -> new ExcecaoNegocio(409, "CONFLITO_ESTADO",
                        "Peça a análise da revisão atual antes de gerar o resumo."));
        ComparacaoDeAnalises comparacao = ComparacaoDeAnalises.comAnterior(vigente, historico.analises());
        if ("ADAPTADOR_MOCKADO".equals(vigente.resumoOrigem())) {
            return new Resultado(vigente, false, comparacao);
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
        return new Resultado(gravada, true, comparacao);
    }

    /**
     * Analise atualizada ou ja resumida.
     *
     * @param analise analise com o resumo
     * @param criada {@code true} se esta chamada gravou o resumo
     * @param comparacao comparacao com a analise anterior (RC-16); {@code null} se nao ha
     */
    public record Resultado(Analise analise, boolean criada, ComparacaoDeAnalises comparacao) {
    }
}
