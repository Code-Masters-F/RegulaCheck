package br.com.regulacheck.app.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Modelos do contrato da API (docs/api/openapi.yaml) usados pela RC-18. Campos que a tela não
 * exibe são opcionais, para que uma mudança compatível no backend não derrube o app; os que a tela
 * exibe são obrigatórios e, se faltarem, a resposta vira ErroApi.RespostaInesperada.
 */

/** Configuração JSON do app: ignora campos novos e omite nulos ao enviar (#40). */
val JsonApi: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Corpo de `POST /auth/login`. O `toString` nunca mostra a senha nem o e-mail. */
@Serializable
data class LoginRequisicao(val email: String, val senha: String) {
    override fun toString(): String = "LoginRequisicao(email=***, senha=***)"
}

/** Resposta 200 do login. O token só aparece aqui; o `toString` o oculta. */
@Serializable
data class LoginResposta(
    val token: String,
    val expiraEm: String,
    val usuario: UsuarioResumo,
    val tipo: String? = null,
) {
    override fun toString(): String =
        "LoginResposta(token=***, expiraEm=$expiraEm, perfil=${usuario.perfil})"
}

/** Identidade resumida da conta. [perfil] é `PACIENTE` ou `ADMINISTRADOR`. */
@Serializable
data class UsuarioResumo(val nome: String, val perfil: String)

/** Resposta de `GET /solicitacoes`: pedidos da paciente, do mais recente ao mais antigo. */
@Serializable
data class ListaSolicitacoes(val itens: List<Solicitacao>, val limite: Int? = null)

/**
 * Pedido (solicitação) como a API o devolve.
 *
 * @property estado texto livre: um valor que o app não conhece não pode derrubar a tela (#41);
 * a conversão para o enum fica em `ui/EstadoPedidoUi.kt`.
 * @property criadaEm instante ISO-8601 com fuso (UTC no backend).
 */
@Serializable
data class Solicitacao(
    val id: String,
    val servico: Servico,
    val estado: String,
    val revisao: Int,
    val criadaEm: String,
    val atualizadaEm: String,
    val documentosDisponiveis: Int,
    val analise: Analise? = null,
    val protocoloId: String? = null,
)

/** Serviço demonstrativo ligado ao pedido; [avisoFicticio] vem do backend e é exibido como texto. */
@Serializable
data class Servico(
    val localidade: String,
    val especialidade: String,
    val avisoFicticio: String? = null,
)

/**
 * Situação da análise da versão atual dos documentos.
 *
 * @property resultadoGeral só existe quando [situacao] é `CONCLUIDA`.
 */
@Serializable
data class Analise(
    val situacao: String,
    val mensagem: String? = null,
    val resultadoGeral: String? = null,
)

/** Corpo padronizado de erro (`docs/api/erros.md`). */
@Serializable
data class ErroCorpo(
    val codigo: String? = null,
    val mensagem: String? = null,
    val detalhes: List<DetalheErro> = emptyList(),
    val idRastreamento: String? = null,
)

/** Erro de um campo de entrada. */
@Serializable
data class DetalheErro(val campo: String, val mensagem: String)
