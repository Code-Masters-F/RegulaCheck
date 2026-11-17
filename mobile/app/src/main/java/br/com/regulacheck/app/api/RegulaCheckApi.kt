package br.com.regulacheck.app.api

/**
 * Rotas da API usadas pela RC-18 (`contracts/api-consumida.md`). O app nunca envia identificador
 * da paciente: a dona é a da sessão (token).
 */
class RegulaCheckApi(private val cliente: ApiClient) {

    /**
     * `POST /auth/login`. A senha só existe no corpo desta requisição; nada é registrado.
     *
     * @return 401 vira [ErroApi.CredenciaisInvalidas] (sem apagar sessão).
     */
    suspend fun entrar(email: String, senha: String): Resultado<LoginResposta> {
        val corpo = JsonApi.encodeToString(LoginRequisicao.serializer(), LoginRequisicao(email, senha))
        return cliente.executar(
            RequisicaoApi("POST", listOf("auth", "login"), corpo, Autenticacao.Nenhuma),
        ) { JsonApi.decodeFromString(LoginResposta.serializer(), it) }
    }

    /**
     * `POST /auth/logout` com o [token] informado. Quem chama apaga a sessão local
     * independentemente do resultado (#41).
     */
    suspend fun sair(token: String): Resultado<Unit> =
        cliente.executar(
            RequisicaoApi("POST", listOf("auth", "logout"), autenticacao = Autenticacao.TokenExplicito(token)),
        ) { }

    /** `GET /solicitacoes`: só os pedidos da paciente da sessão (até 50). */
    suspend fun listarSolicitacoes(): Resultado<ListaSolicitacoes> =
        cliente.executar(RequisicaoApi("GET", listOf("solicitacoes"))) {
            JsonApi.decodeFromString(ListaSolicitacoes.serializer(), it)
        }

    /** `GET /solicitacoes/{id}`: 404 também para pedido de outra paciente. */
    suspend fun obterSolicitacao(id: String): Resultado<Solicitacao> =
        cliente.executar(RequisicaoApi("GET", listOf("solicitacoes", id))) {
            JsonApi.decodeFromString(Solicitacao.serializer(), it)
        }
}
