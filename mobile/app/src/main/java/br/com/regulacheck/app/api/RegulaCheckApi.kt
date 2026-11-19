package br.com.regulacheck.app.api

/**
 * Rotas da API usadas pelo app: RC-18 (`specs/017-…/contracts/api-consumida.md`), RC-19
 * (`specs/022-…/contracts/api-consumida.md`) e RC-22 (`specs/023-…/contracts/api-consumida.md`). O app nunca envia identificador da paciente: a dona é
 * a da sessão (token).
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

    // RC-19 (contracts/api-consumida.md da spec 022).

    /** `GET /protocolos`: serviços ativos; a RC-19 usa só o primeiro. */
    suspend fun listarProtocolos(): Resultado<ListaProtocolos> =
        cliente.executar(RequisicaoApi("GET", listOf("protocolos"))) {
            JsonApi.decodeFromString(ListaProtocolos.serializer(), it)
        }

    /**
     * `POST /solicitacoes`: cria um rascunho do serviço [protocoloId]. A API não deduplica: cada
     * sucesso é um pedido novo (research D7).
     *
     * @return 422 (serviço inexistente ou inativo) vira [ErroApi.RequisicaoInvalida].
     */
    suspend fun criarSolicitacao(protocoloId: String): Resultado<Solicitacao> {
        val corpo = JsonApi.encodeToString(NovaSolicitacao.serializer(), NovaSolicitacao(protocoloId))
        return cliente.executar(RequisicaoApi("POST", listOf("solicitacoes"), corpo)) {
            JsonApi.decodeFromString(Solicitacao.serializer(), it)
        }
    }

    /** `GET /solicitacoes/{id}/documentos`: documentos disponíveis, na ordem de envio. */
    suspend fun listarDocumentos(id: String): Resultado<ListaDocumentos> =
        cliente.executar(RequisicaoApi("GET", listOf("solicitacoes", id, "documentos"))) {
            JsonApi.decodeFromString(ListaDocumentos.serializer(), it)
        }

    /**
     * `POST /solicitacoes/{id}/documentos` (multipart). [chaveEnvio] identifica a tentativa e deve
     * ser a mesma em todas as repetições do mesmo arquivo: a repetição devolve 200 com o documento
     * já gravado, sem duplicar.
     *
     * @return 201 ou 200 → [DocumentoEnviado]; 413/415 → [ErroApi.ArquivoRecusado]; 400/422 →
     * [ErroApi.RequisicaoInvalida]; arquivo que não abre → [ErroApi.ArquivoInacessivel].
     */
    suspend fun enviarDocumento(
        id: String,
        fonte: FonteArquivo,
        tipoDocumental: String,
        chaveEnvio: String,
    ): Resultado<DocumentoEnviado> =
        cliente.executar(
            RequisicaoApi(
                "POST",
                listOf("solicitacoes", id, "documentos"),
                corpo = corpoEnvioDocumento(fonte, tipoDocumental),
                cabecalhos = mapOf(CABECALHO_CHAVE_ENVIO to chaveEnvio),
            ),
        ) { JsonApi.decodeFromString(DocumentoEnviado.serializer(), it) }

    /**
     * `POST /solicitacoes/{id}/extracoes`: leitura SIMULADA (tempo de leitura de 70 s). Repetir
     * sobre a mesma versão devolve a leitura existente (200).
     *
     * @return 422 `SEM_DOCUMENTOS`, 409 e 503 `LEITURA_INDISPONIVEL` como [ErroApi].
     */
    suspend fun lerDocumentos(id: String): Resultado<Extracao> =
        cliente.executar(RequisicaoApi("POST", listOf("solicitacoes", id, "extracoes"), leituraLonga = true)) {
            JsonApi.decodeFromString(Extracao.serializer(), it)
        }

    /**
     * `POST /solicitacoes/{id}/analises`: verifica os critérios (síncrono; o servidor limita a
     * 1 min; o cliente espera 70 s). Repetir sem mudança devolve a vigente (200).
     *
     * @return 409 `ANALISE_EM_ANDAMENTO` traz `Retry-After` em [ErroApi.Conflito.retryAfterSegundos].
     */
    suspend fun analisar(id: String): Resultado<AnaliseGravada> =
        cliente.executar(RequisicaoApi("POST", listOf("solicitacoes", id, "analises"), leituraLonga = true)) {
            JsonApi.decodeFromString(AnaliseGravada.serializer(), it)
        }

    /** `GET /solicitacoes/{id}/analises`: histórico, com a vigente marcada por `vigenteId`. */
    suspend fun consultarAnalises(id: String): Resultado<HistoricoAnalises> =
        cliente.executar(RequisicaoApi("GET", listOf("solicitacoes", id, "analises"))) {
            JsonApi.decodeFromString(HistoricoAnalises.serializer(), it)
        }

    /**
     * `POST /solicitacoes/{id}/resumo`: resumo SIMULADO da análise vigente (RC-14). Falha do
     * adaptador volta como resumo `PADRAO`, não como erro.
     *
     * @return 409 (sem vigente ou análise alterada) → quem chama reconsulta o histórico.
     */
    suspend fun gerarResumo(id: String): Resultado<AnaliseGravada> =
        cliente.executar(RequisicaoApi("POST", listOf("solicitacoes", id, "resumo"))) {
            JsonApi.decodeFromString(AnaliseGravada.serializer(), it)
        }

    // RC-22 (contracts/api-consumida.md da spec 023; rotas da spec 016).

    /**
     * `POST /solicitacoes/{id}/sessoes-captura`: abre a sessão de captura SIMULADA do pedido ou
     * devolve a que já está aberta (201 ou 200; idempotente).
     *
     * @return 409 (pedido submetido) vira [ErroApi.Conflito]; 404 também para pedido de outra paciente.
     */
    suspend fun abrirSessaoCaptura(id: String): Resultado<SessaoCaptura> =
        cliente.executar(RequisicaoApi("POST", listOf("solicitacoes", id, "sessoes-captura"))) {
            JsonApi.decodeFromString(SessaoCaptura.serializer(), it)
        }

    /**
     * `GET /solicitacoes/{id}/leituras`: telemetria dos dois sensores simulados. Sem sessão ou sem
     * leitura, responde 200 com `SEM_LEITURA`.
     */
    suspend fun consultarTelemetria(id: String): Resultado<Telemetria> =
        cliente.executar(RequisicaoApi("GET", listOf("solicitacoes", id, "leituras"))) {
            JsonApi.decodeFromString(Telemetria.serializer(), it)
        }

    /**
     * `GET /admin/indicadores` (RC-20): só agregados, só para administrador.
     *
     * @return 403 (conta de paciente) vira [ErroApi.SemPermissao] sem encerrar a sessão.
     */
    suspend fun consultarIndicadores(): Resultado<Indicadores> =
        cliente.executar(RequisicaoApi("GET", listOf("admin", "indicadores"))) {
            JsonApi.decodeFromString(Indicadores.serializer(), it)
        }

    companion object {
        /** Cabeçalho de idempotência do envio de documento (RC-08). */
        const val CABECALHO_CHAVE_ENVIO = "X-Chave-Envio"
    }
}
