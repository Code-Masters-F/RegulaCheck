package br.com.regulacheck.indicador.repository;

/**
 * Fronteira com o banco para os indicadores do administrador (RC-20). Devolve somente contagens: nenhuma linha de
 * pedido, analise, documento ou conta sai desta fronteira.
 */
public interface IndicadorRepository {

    /**
     * Conta pedidos, analises vigentes e pendencias vigentes no momento da chamada.
     *
     * @return contagens agregadas; zeros e listas vazias quando nao ha dados
     * @throws br.com.regulacheck.common.exception.ExcecaoNegocio 503 {@code DEPENDENCIA_INDISPONIVEL} se o banco
     *     falhar
     */
    ContagensIndicadores contar();
}
