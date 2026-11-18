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

/*
 * Modelos da RC-19 (envio, leitura, análise e resumo). Enums do contrato ficam como texto: valor
 * novo não derruba a tela, e o rótulo vem de ui/RotulosApi.kt.
 */

/** Resposta de `GET /protocolos`: serviços ativos; `itens` vazio quando não há nenhum. */
@Serializable
data class ListaProtocolos(val itens: List<Protocolo>)

/**
 * Serviço demonstrativo (protocolo acadêmico fictício).
 *
 * @property versao versão do protocolo como a API a devolve (ex.: `v0.1`).
 * @property avisoFicticio texto do backend exibido como texto puro.
 */
@Serializable
data class Protocolo(
    val protocoloId: String,
    val versao: String,
    val localidade: String,
    val especialidade: String,
    val avisoFicticio: String? = null,
)

/** Corpo de `POST /solicitacoes`: só o serviço; a dona é a paciente da sessão. */
@Serializable
data class NovaSolicitacao(val protocoloId: String)

/**
 * Documento enviado e disponível no pedido.
 *
 * @property nomeArquivo nome de exibição saneado pela API (nunca o nome de armazenamento).
 * @property formato `PDF`, `JPEG` ou `PNG`, decidido pela API pelo conteúdo.
 * @property paginas páginas do PDF; 1 para imagens.
 */
@Serializable
data class Documento(
    val id: String,
    val nomeArquivo: String,
    val tipoDocumental: String,
    val formato: String,
    val tamanhoBytes: Long,
    val paginas: Int = 1,
    val estadoEnvio: String = "ENVIADO",
)

/** Resposta de `GET /solicitacoes/{id}/documentos`, na ordem de envio. */
@Serializable
data class ListaDocumentos(
    val documentos: List<Documento>,
    val total: Int? = null,
    val maximo: Int? = null,
)

/**
 * Resposta 201 (novo) ou 200 (repetição da mesma `X-Chave-Envio`) de
 * `POST /solicitacoes/{id}/documentos`. Só depois dela o documento entra na lista.
 */
@Serializable
data class DocumentoEnviado(
    val id: String,
    val nomeArquivo: String,
    val tipoDocumental: String,
    val formato: String,
    val tamanhoBytes: Long,
    val paginas: Int = 1,
    val estadoEnvio: String = "ENVIADO",
    val documentosDisponiveis: Int? = null,
) {
    /** O documento como aparece na lista. */
    fun comoDocumento() = Documento(id, nomeArquivo, tipoDocumental, formato, tamanhoBytes, paginas, estadoEnvio)
}

/**
 * Leitura SIMULADA dos documentos (`POST /solicitacoes/{id}/extracoes`).
 *
 * @property aviso texto fixo do backend sobre a simulação.
 */
@Serializable
data class Extracao(
    val id: String,
    val situacao: String,
    val simulada: Boolean = true,
    val aviso: String,
    val documentos: List<DocumentoLido>,
)

/**
 * Situação da leitura de um documento: `LIDO`, `PROTEGIDO` ou `LEITURA_VAZIA`.
 *
 * @property motivo e [orientacao] só quando não lido; textos do backend exibidos como texto puro.
 */
@Serializable
data class DocumentoLido(
    val documentoId: String,
    val tipoDocumental: String,
    val situacao: String,
    val motivo: String? = null,
    val orientacao: String? = null,
    val campos: List<CampoLido> = emptyList(),
)

/** Campo lido: só a situação é usada pela RC-19 (o valor é da conferência, fora do épico). */
@Serializable
data class CampoLido(val codigo: String, val situacao: String)

/**
 * Histórico de análises (`GET /solicitacoes/{id}/analises`).
 *
 * @property estado estado atual do pedido (texto livre).
 * @property execucao só enquanto o pedido está em `PROCESSANDO`.
 * @property vigenteId análise que vale para a versão atual; ausente quando nenhuma vale.
 */
@Serializable
data class HistoricoAnalises(
    val estado: String,
    val versaoConjuntoDocumental: Int,
    val execucao: ExecucaoAnalise? = null,
    val vigenteId: String? = null,
    val analises: List<AnaliseGravada> = emptyList(),
)

/** Análise em andamento (`EM_ANDAMENTO`) ou interrompida pelo limite (`INTERROMPIDA`). */
@Serializable
data class ExecucaoAnalise(val situacao: String, val limiteEm: String? = null)

/**
 * Análise gravada (RC-13), com os campos de apresentação da RC-15.
 *
 * @property rotuloResultado texto do resultado geral (RC-15); opcional para não falhar com um
 * backend anterior ao commit `65cbadf` (research D12).
 * @property aviso texto fixo de simulação do backend.
 * @property resumo resumo simulado da RC-14; ausente até ser pedido.
 */
@Serializable
data class AnaliseGravada(
    val id: String,
    val revisao: Int,
    val vigente: Boolean,
    val executadaEm: String,
    val protocolo: ProtocoloAnalise,
    val resultadoGeral: String,
    val rotuloResultado: String? = null,
    val aviso: String,
    val documentos: List<DocumentoAvaliado> = emptyList(),
    val resultados: List<ResultadoRegra> = emptyList(),
    val pendencias: List<PendenciaGravada> = emptyList(),
    val resumo: ResumoSimulado? = null,
)

/** Protocolo e versão usados na análise. */
@Serializable
data class ProtocoloAnalise(val identificador: String? = null, val versao: String)

/** Documento da revisão avaliada; o nome de cada evidência vem daqui pelo `documentoId`. */
@Serializable
data class DocumentoAvaliado(
    val documentoId: String,
    val nomeArquivo: String,
    val tipoDeclarado: String? = null,
    val disponivel: Boolean = true,
)

/** Resultado de uma regra: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL` ou `NAO_APLICAVEL`. */
@Serializable
data class ResultadoRegra(
    val codigoRegra: String,
    val descricao: String,
    val resultado: String,
    val motivo: String? = null,
)

/**
 * Pendência gravada (`PENDENTE` ou `NAO_VERIFICAVEL`).
 *
 * @property responsavel `PACIENTE` ou `UNIDADE_EMISSORA`, registrado pela regra; o app não o reescreve.
 * @property orientacao próxima providência textual da RC-15 (texto puro); opcional pelo mesmo
 * motivo de [AnaliseGravada.rotuloResultado].
 */
@Serializable
data class PendenciaGravada(
    val codigoRegra: String,
    val descricaoRegra: String,
    val tipo: String,
    val explicacao: String,
    val motivo: String? = null,
    val responsavel: String,
    val orientacao: String? = null,
    val evidencias: List<Evidencia> = emptyList(),
)

/** Documento, campo e situação considerados por uma pendência; [origem] só quando existir. */
@Serializable
data class Evidencia(
    val documentoId: String,
    val campo: String,
    val situacao: String,
    val origem: Origem? = null,
)

/** Página (só em PDF) e trecho literal do documento; dado não confiável, exibido como texto. */
@Serializable
data class Origem(val pagina: Int? = null, val trecho: String? = null)

/**
 * Resumo SIMULADO da análise (RC-14).
 *
 * @property origem `ADAPTADOR_MOCKADO` ou `PADRAO` (explicação padrão depois de falha).
 */
@Serializable
data class ResumoSimulado(val texto: String, val origem: String, val simulado: Boolean = true)
