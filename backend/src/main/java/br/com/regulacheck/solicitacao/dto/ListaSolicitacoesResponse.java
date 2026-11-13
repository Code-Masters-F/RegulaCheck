package br.com.regulacheck.solicitacao.dto;

import br.com.regulacheck.solicitacao.entity.Solicitacao;
import java.util.List;

/**
 * Resposta de {@code GET /solicitacoes}: so os pedidos da paciente da sessao, do mais recente ao mais antigo.
 *
 * @param itens pedidos; vazio quando a paciente ainda nao criou nenhum
 * @param limite maximo de itens por resposta (a lista nao e paginada nesta historia)
 */
public record ListaSolicitacoesResponse(List<SolicitacaoResponse> itens, int limite) {

    /**
     * @param pedidos pedidos da paciente
     * @param limite limite aplicado na consulta
     * @return resposta publica correspondente
     */
    public static ListaSolicitacoesResponse de(List<Solicitacao> pedidos, int limite) {
        return new ListaSolicitacoesResponse(pedidos.stream().map(SolicitacaoResponse::de).toList(), limite);
    }
}
