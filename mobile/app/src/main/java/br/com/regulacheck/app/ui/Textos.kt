package br.com.regulacheck.app.ui

/**
 * Textos exibidos pelas telas da RC-18, copiados de `mobile/design/textos.md` (E1.1–E1.5, F3, F4).
 * Alterou aqui, altere lá. Os três textos marcados "RC-18" foram criados nesta história e
 * registrados em `textos.md`.
 */
object Textos {
    const val MARCA = "RegulaCheck"
    const val AVISO_RODAPE = "Protótipo acadêmico · dados fictícios"
    const val AVISO_ENTRADA = "Aplicativo acadêmico com dados fictícios. O protocolo é de teste e não é " +
        "exigência oficial. Não substitui atendimento nem orientação de saúde."
    const val TENTAR_DE_NOVO = "Tentar de novo"
    const val SAIR = "Sair"
    const val VOLTAR = "Voltar"

    /** RC-18: ações que dependem das rotas da RC-19 (research D7). */
    const val ETAPA_INDISPONIVEL = "Esta etapa ainda não está disponível nesta versão do app."

    // E1.1 Login
    const val LOGIN_TITULO = "Entrar"
    const val LOGIN_APOIO = "Use o e-mail e a senha da sua conta."
    const val CAMPO_EMAIL = "E-mail"
    const val CAMPO_SENHA = "Senha"
    const val MOSTRAR_SENHA = "Mostrar senha"
    const val OCULTAR_SENHA = "Ocultar senha"
    const val BOTAO_ENTRAR = "Entrar"
    const val BOTAO_ENTRANDO = "Entrando…"
    const val CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos."
    const val INDISPONIVEL_TITULO = "O serviço está indisponível agora."
    const val LOGIN_INDISPONIVEL_TEXTO = "Tente de novo em alguns instantes. Seu e-mail foi mantido."
    const val SEM_CONEXAO_TITULO = "Sem conexão com a internet."
    const val SEM_CONEXAO_TEXTO = "Verifique sua rede e tente de novo."
    const val LOGIN_DADOS_RECUSADOS = "Confira o e-mail e a senha e tente de novo."

    /** RC-18: recusa de conta que não é de paciente (#41). */
    const val CONTA_NAO_PACIENTE = "Esta conta não é de paciente. Entre com uma conta de paciente."

    // E1.4 Sessão expirada (mostrada no próprio login)
    const val SESSAO_EXPIRADA_TITULO = "Sua sessão terminou. Entre novamente."
    const val SESSAO_EXPIRADA_TEXTO = "Por segurança, entre de novo para continuar."

    // E1.5 Sem permissão
    const val SEM_PERMISSAO = "Você não tem permissão para ver esta tela."

    // E1.2 Meus pedidos
    const val PEDIDOS_TITULO = "Meus pedidos"
    const val PEDIDOS_APOIO = "Retome de onde parou."
    const val PEDIDOS_CARREGANDO = "Carregando seus pedidos…"
    const val PEDIDOS_VAZIO_TITULO = "Você ainda não tem pedidos."
    const val PEDIDOS_VAZIO_TEXTO = "Toque em “Novo pedido” para começar a preparar seus documentos."
    const val NOVO_PEDIDO = "Novo pedido"
    const val PEDIDOS_ERRO_TITULO = "Não foi possível carregar seus pedidos."
    const val PEDIDOS_ERRO_TEXTO = "Tente de novo. Se o problema continuar, volte mais tarde."
    const val PEDIDOS_SEM_CONEXAO_TEXTO = "Seus pedidos aparecerão quando a conexão voltar."

    fun saudacao(primeiroNome: String) = "Olá, $primeiroNome"
    fun linhaPedido(id: String, atualizadoEm: String) = "Pedido $id · atualizado em $atualizadoEm"

    // E1.3 Pedido
    const val PROXIMO_PASSO = "Próximo passo"
    const val PEDIDO_CARREGANDO = "Carregando o pedido…"
    const val NAO_ENCONTRADO_TITULO = "Pedido não encontrado."
    const val NAO_ENCONTRADO_TEXTO = "Ele pode ter sido excluído ou não estar disponível para você."
    const val VOLTAR_PARA_PEDIDOS = "Voltar para meus pedidos"
    const val DOCUMENTOS = "Documentos"
    const val RESULTADO = "Resultado"
    const val DETALHE_INDISPONIVEL_TEXTO = "Tente de novo em alguns instantes."

    fun tituloPedido(id: String) = "Pedido $id"
    fun documentosAnexados(quantidade: Int) = "$quantidade de 3 anexados"
    fun metadados(criadoEm: String, atualizadoEm: String, revisao: Int) =
        "Criado em $criadoEm · atualizado em $atualizadoEm · revisão $revisao"

    /** RC-18: estado que o app não conhece (#41). */
    const val ESTADO_NAO_RECONHECIDO = "Estado não reconhecido"
    const val MENSAGEM_ESTADO_NAO_RECONHECIDO = "Esta versão do app não reconhece a situação deste pedido."
}
