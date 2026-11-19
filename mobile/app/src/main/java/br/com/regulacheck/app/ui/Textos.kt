package br.com.regulacheck.app.ui

/**
 * Textos exibidos pelas telas, copiados de `mobile/design/textos.md` (RC-18: E1.1–E1.5, F3, F4;
 * RC-19: E2.1, E2.2, E2.3, E2.7, E3.1, E3.3, E3.4, E3.8, E4.3; RC-22: E2.7 Origem e E4.1). Alterou
 * aqui, altere lá. Os textos marcados "RC-18", "RC-19" ou "RC-22" foram criados (ou ajustados para
 * não prometer ação ausente nesta versão) nessas histórias e registrados em `textos.md`.
 */
object Textos {
    const val MARCA = "RegulaCheck"

    // F3: aviso acadêmico em duas intensidades (decisão de 17/11/2026): rodapé discreto em toda
    // tela, que abre o aviso completo num balão; no login, um aviso curto sobre a proposta.
    const val AVISO_RODAPE = "Protótipo acadêmico · dados fictícios"
    const val AVISO_RODAPE_LEITOR = "Protótipo acadêmico · dados fictícios. Toque para ler o aviso completo."
    const val AVISO_COMPLETO_TITULO = "Sobre este aplicativo"
    const val AVISO_COMPLETO = "Aplicativo acadêmico com dados fictícios. O protocolo é de teste e não é " +
        "exigência oficial. Não substitui atendimento nem orientação de saúde."
    const val PROPOSTA_TITULO = "Prepare seus documentos"
    const val PROPOSTA_TEXTO = "Veja o que está completo e o que falta antes de enviar."
    const val TENTAR_DE_NOVO = "Tentar de novo"
    const val SAIR = "Sair"
    const val VOLTAR = "Voltar"

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

    /** RC-18/RC-20: recusa de conta com perfil que o app não atende (nem paciente nem administrador). */
    const val PERFIL_NAO_SUPORTADO = "Esta conta não pode ser usada neste aplicativo. Entre com uma conta de paciente ou de administrador."

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

    // ---------------------------------------------------------------- RC-19

    const val TENTAR_NOVAMENTE = "Tentar novamente"
    const val ATUALIZAR = "Atualizar"
    const val CANCELAR = "Cancelar"
    fun pedidoNumero(id: String) = "Pedido $id"
    fun protocoloVersao(versao: String) = "Protocolo acadêmico fictício, versão $versao"

    // E4.3 Conexão ruim
    const val CONFIRMACAO_INCERTA_TITULO = "Não foi possível confirmar a operação. O que você escolheu foi mantido."
    const val CONFIRMACAO_INCERTA_TEXTO = "Nada é dado como enviado até recebermos a confirmação."

    // E2.1 Novo pedido
    const val NOVO_PEDIDO_TITULO = "Novo pedido"
    const val NOVO_PEDIDO_APOIO = "Escolha o serviço para preparar seus documentos."
    fun linhaServico(localidade: String, versao: String) = "$localidade · Protocolo acadêmico fictício · versão $versao"
    const val PROPOSTA_NOVO_TITULO = "Em seguida, seus documentos"
    const val PROPOSTA_NOVO_TEXTO = "Você vai anexar o encaminhamento e um documento de identificação."
    const val CRIAR_RASCUNHO = "Criar rascunho"
    const val CRIANDO = "Criando…"
    const val CRIAR_ERRO_TITULO = "Não foi possível criar o rascunho."
    const val CRIAR_ERRO_TEXTO = "Sua escolha foi mantida. Tente de novo."
    const val SERVICO_RECUSADO = "Escolha um dos serviços da lista."
    const val SEM_SERVICOS_TITULO = "Nenhum serviço disponível agora."
    const val SEM_SERVICOS_TEXTO = "Tente de novo mais tarde."

    /** RC-19: carga e falha da lista de serviços (sem quadro próprio). */
    const val SERVICOS_CARREGANDO = "Carregando os serviços…"
    const val SERVICOS_ERRO_TITULO = "Não foi possível carregar os serviços."

    /** RC-19: a API cria um pedido por chamada; em tempo esgotado ele pode já existir (research D7). */
    const val RASCUNHO_PODE_EXISTIR = "O rascunho pode ter sido criado. Confira em “Meus pedidos” antes de tentar de novo."

    // E2.2 Seus documentos
    const val DOCUMENTOS_TITULO = "Seus documentos"
    const val DOCUMENTOS_APOIO = "Anexe o encaminhamento e seu documento de identificação."
    const val DOCUMENTOS_APOIO_VERIFICADO = "Encaminhamento e identificação deste pedido."
    const val DOCUMENTOS_CARREGANDO = "Carregando os documentos…"
    const val ARQUIVOS_DO_PEDIDO = "Arquivos deste pedido"
    const val NENHUM_DOCUMENTO = "Nenhum documento anexado ainda."
    const val ANEXAR_DOCUMENTO = "Anexar documento"
    fun contagemDocumentos(quantidade: Int) = "$quantidade de 3 documentos · até 5 MB cada"
    const val LIMITES_FORMATO = "PDF até 5 páginas, JPEG ou PNG"

    /** RC-19: sem "Remova um para anexar outro.", porque remover não existe nesta versão. */
    const val LIMITE_ATINGIDO = "3 de 3 documentos. Este é o limite do pedido."
    const val ENVIADO = "Enviado"
    const val ENVIANDO = "Enviando…"
    const val NAO_ENVIADO = "Não enviado"
    const val MANTENHA_ABERTO = "Mantenha o aplicativo aberto até o envio terminar."
    fun envioInterrompido(nome: String) = "O envio de “$nome” foi interrompido."
    const val ENVIO_INTERROMPIDO_TEXTO = "O arquivo ainda não foi enviado. Toque em “Tentar de novo”."
    fun arquivoNaoEnviado(nome: String) = "“$nome” não foi enviado."
    const val NADA_SALVO = "Nada foi salvo."
    const val ESCOLHER_OUTRO_ARQUIVO = "Escolher outro arquivo"
    const val ESCOLHA_OUTRA_VEZ = "Escolha o arquivo outra vez."
    const val PEDIDO_NAO_RECEBE = "Este pedido não pode mais receber documentos."
    const val LER_DOCUMENTOS = "Ler documentos"

    /** RC-19: o desenho diz "Depois você confere o que foi lido."; a conferência fica só no desenho. */
    const val LER_APOIO = "Leitura simulada com dados sintéticos."

    /**
     * RC-19: pedido já verificado. Desde a RC-16 a API aceita a versão corrigida depois da análise,
     * mas o app ainda não oferece o reenvio; o texto não afirma que é impossível.
     */
    const val PEDIDO_VERIFICADO_AVISO = "Enviar uma versão corrigida ainda não está disponível neste app."
    const val VER_RESULTADO = "Ver resultado"
    const val PEDIDO_SUBMETIDO = "Este pedido já foi submetido e só pode ser consultado."

    // E2.3 Recusa de arquivo (usados só se a API não mandar mensagem)
    const val RECUSA_FORMATO = "Envie um arquivo PDF, JPEG ou PNG."
    const val RECUSA_TAMANHO = "O arquivo excede 5 MB. Reduza o tamanho ou tire uma nova foto."
    const val RECUSA_ILEGIVEL = "Não foi possível ler o arquivo. Envie um novo arquivo."
    const val RECUSA_LIMITE = "No máximo 3 documentos por pedido."

    // E2.7 Anexar (folha)
    const val FOLHA_TIPO_TITULO = "Que documento você vai anexar?"

    /** Opções da folha: código da API, título e apoio. */
    val TIPOS_DOCUMENTO = listOf(
        Triple("ENCAMINHAMENTO", "Encaminhamento", "Pedido de consulta emitido pela unidade de saúde"),
        Triple("IDENTIFICACAO", "Identificação ou cartão do SUS", "RG, CNH ou cartão do SUS"),
        Triple("AUTORIZACAO_RESPONSAVEL", "Autorização do responsável", "Para pacientes menores de 18 anos"),
        Triple("OUTRO", "Outro documento", "Qualquer outro documento do pedido"),
    )

    // E3.1 Leitura simulada
    const val LENDO_TITULO = "Lendo seus documentos"
    const val LER_TITULO = "Ler documentos"
    const val AVISO_LEITURA_SIMULADA = "Leitura simulada com dados sintéticos; não é leitura real dos documentos."
    const val LENDO = "Lendo os documentos…"
    const val LENDO_BOTAO = "Lendo…"
    const val LEITURA_INDISPONIVEL_TITULO = "A leitura simulada não está disponível agora."
    const val LEITURA_INDISPONIVEL_TEXTO = "Nenhum documento foi lido e nada foi gravado. Tente de novo em alguns instantes."
    const val SEM_DOCUMENTOS_LER = "Anexe ao menos um documento antes de ler."
    const val DOCUMENTOS_MUDARAM = "Os documentos mudaram; tente de novo."

    /** RC-19: resultado da leitura sem a conferência (E3.2 fica só no desenho, #40). */
    const val O_QUE_FOI_LIDO = "O que foi lido"
    fun informacoesIdentificadas(identificadas: Int, total: Int) = "$identificadas de $total informações identificadas"
    const val CONFERENCIA_SO_DESENHO =
        "A conferência da leitura está disponível só no desenho nesta versão. Informações não conferidas contam como não verificadas."

    // E3.8 Verificar critérios (diálogo)
    const val VERIFICAR_CRITERIOS = "Verificar critérios"
    const val VERIFICAR_CRITERIOS_PERGUNTA = "Verificar critérios?"

    /** RC-19: o app não consulta a conferência, então não conta as informações sem decisão. */
    const val DIALOGO_NAO_CONFERIDAS = "As informações lidas que não foram conferidas contarão como não verificadas."

    const val ANALISANDO = "Analisando…"

    /** RC-19: 409 `ANALISE_EM_ANDAMENTO`; o app não repete sozinho. */
    fun analiseEmAndamento(segundos: Long?) =
        if (segundos != null) {
            "Já há uma verificação em andamento. Aguarde cerca de $segundos segundos e consulte o resultado."
        } else {
            "Já há uma verificação em andamento. Aguarde e consulte o resultado."
        }
    const val CONSULTAR_RESULTADO = "Consultar resultado"
    const val VERIFICACAO_NAO_TERMINOU = "A verificação ainda não terminou. Isso não é um resultado."

    /** RC-19: tempo esgotado e a consulta ao histórico também falhou (research D6). */
    const val CONSULTAR_DE_NOVO = "Consultar de novo"

    // E3.3 Resultado da verificação
    const val RESULTADO_TITULO = "Resultado da verificação"
    fun verificadoEm(dataHora: String, revisao: Int) = "Verificado em $dataHora · revisão $revisao"
    const val RESUMO_TITULO = "Resumo das pendências"
    const val RESUMO_NOTA = "Gerado por simulação, sem inteligência artificial real. A lista de critérios prevalece."
    const val RESUMO_INDISPONIVEL = "O resumo não está disponível agora. Os critérios abaixo continuam valendo."
    const val RESUMO_PADRAO_NOTA = "Resumo padrão: o resumo simulado falhou; a lista de critérios continua valendo."
    const val O_QUE_FALTA = "O que falta"
    const val OUTROS_CRITERIOS = "Outros critérios"
    const val CRITERIOS = "Critérios"
    const val CONFIRA_TEXTO = "Alguma informação não pôde ser verificada. Isso não significa que ela esteja faltando."

    /** RC-19: "Conferir leitura" é só desenho nesta versão (#42). */
    const val CONFIRA_SO_DESENHO = "Confira a leitura: essa etapa está disponível só no desenho nesta versão."
    const val SEM_PENDENCIAS_AVISO = "Isso não é aprovação do SUS. Os critérios são demonstrativos e não garantem vaga nem prazo."

    /** RC-19: aviso permanente da RC-15 (#42, spec 019 FR-009). */
    const val AVISO_RESULTADO =
        "Um resultado sem pendências não garante aceitação pela regulação. Os critérios deste protocolo são demonstrativos."
    const val FALHA_VERIFICACAO_TITULO = "A verificação não pôde ser feita."
    const val FALHA_VERIFICACAO_TEXTO = "Nenhuma conclusão foi tirada. Você pode tentar de novo."
    const val RESULTADO_CARREGANDO = "Carregando o resultado…"
    const val RESULTADO_ERRO = "Não foi possível consultar o resultado agora."

    /** RC-19: estados do contrato de apresentação sem quadro próprio. */
    const val SEM_ANALISE = "Este pedido ainda não tem verificação concluída."
    const val SEM_VIGENTE = "Ainda não há resultado para a versão atual dos documentos."
    const val RESULTADO_ANTERIOR = "Resultado anterior"
    fun resultadoAnteriorTexto(revisao: Int) = "Revisão $revisao. Este resultado não vale para a revisão atual dos documentos."
    const val VER_RESULTADO_ANTERIOR = "Ver resultado anterior"

    // E3.4 Detalhe da pendência
    const val PENDENCIA_TITULO = "O que falta resolver"
    fun apoioPendencia(codigo: String, versao: String) = "$codigo · Protocolo acadêmico fictício, versão $versao"
    const val SITUACAO = "Situação"
    const val RESPONSAVEL_REGISTRADO = "Responsável registrado pela regra"
    const val QUEM_PODE_RESOLVER = "Quem pode resolver"
    fun documentoEvidencia(nome: String, detalhe: String) = "Documento: $nome · $detalhe"
    fun pagina(numero: Int) = "página $numero"

    /** RC-19: trecho de origem, sempre literal. */
    fun trechoLido(trecho: String) = "Trecho lido: “$trecho”"

    /** RC-19: adaptado de "Você confere a transcrição aqui. O documento clínico é corrigido pela unidade emissora." */
    const val UNIDADE_CORRIGE = "O documento clínico é corrigido pela unidade emissora. Ele não pode ser alterado no aplicativo."
    const val ORIENTACAO_RC_R02 = "Se o erro estiver no documento médico original, solicite a correção à unidade emissora."

    /** RC-19: sem botão de reenvio (RC-16) nem de substituição. */
    const val REENVIO_INDISPONIVEL = "O reenvio de arquivos ainda não está disponível nesta versão do app."

    const val CONFERENCIA_SO_DESENHO_CURTO = "Conferir a leitura está disponível só no desenho nesta versão."

    // ---------------------------------------------------------------- RC-22 (spec 023)

    // E2.7 Anexar (folha) — passo Origem
    const val FOLHA_ORIGEM_TITULO = "Como você quer anexar?"
    const val ORIGEM_FOTOGRAFAR = "Fotografar"

    /** RC-22: era "Mostramos as condições da captura antes da foto"; o app não abre a câmera (research D2). */
    const val ORIGEM_FOTOGRAFAR_APOIO = "Veja as condições da captura e depois escolha a foto"
    const val ESCOLHER_ARQUIVO = "Escolher arquivo"
    const val ORIGEM_ARQUIVO_APOIO = "PDF, JPEG ou PNG do seu aparelho"

    // E4.1 Condições da captura
    const val CAPTURA_TITULO = "Condições da captura"

    /** RC-22: o desenho não diz como a foto é tirada; o app não abre a câmera (research D2). */
    const val CAPTURA_APOIO = "Posicione o documento sob a estação de captura e fotografe com a câmera do aparelho. " +
        "As leituras se atualizam enquanto esta tela está aberta."

    /** RC-22: carregando (sem quadro próprio). */
    const val CAPTURA_CARREGANDO = "Consultando os sensores…"
    const val LUMINOSIDADE = "Luminosidade"
    const val DISTANCIA = "Distância"
    const val SITUACAO_ADEQUADA = "Adequada"
    const val SITUACAO_ADEQUADA_DE_NOVO = "Adequada de novo"
    const val SITUACAO_BAIXA = "Baixa"

    /** RC-22: luminosidade acima da faixa (sem quadro próprio). */
    const val SITUACAO_ALTA = "Alta"
    const val SITUACAO_FORA_DO_IDEAL = "Fora do ideal"
    const val SITUACAO_DESATUALIZADA = "Desatualizada"

    /** RC-22: só um dos sensores sem leitura (sem quadro próprio). */
    const val SITUACAO_SEM_LEITURA = "Sem leitura"
    const val SENSORES_INDISPONIVEIS = "Sensores indisponíveis agora"
    const val PODE_ANEXAR_NORMALMENTE = "Você pode anexar e salvar seus documentos normalmente."
    const val CONDICOES_ADEQUADAS_DE_NOVO = "Condições adequadas de novo."
    const val SEM_LEITURA_DISPONIVEL = "Sem leitura disponível · sensor simulado"

    /** RC-22: leitura com instante ilegível (a idade não pode ser calculada). */
    const val LEITURA_SENSOR_SIMULADO = "Leitura do sensor simulado"
    const val NOTA_DEMONSTRACAO = "Valores de demonstração. Eles não comprovam que o documento ficará legível."

    /** RC-22: era "Fotografar"; abre o seletor do sistema com imagens, não a câmera (research D2). */
    const val ESCOLHER_FOTO = "Escolher a foto"

    /** RC-22: era "Fotografar mesmo assim" (research D2). */
    const val ESCOLHER_FOTO_MESMO_ASSIM = "Escolher a foto mesmo assim"
    fun leituraHa(tempo: String) = "Leitura há $tempo · sensor simulado"
    fun leituraHaAntes(tempo: String, antes: String) = "Leitura há $tempo · antes: $antes · sensor simulado"

    /** Quadro `Cna25` (vale sobre "· desatualizada" de textos.md, research D7). */
    fun ultimaLeituraHa(tempo: String) = "Última leitura há $tempo · sensor simulado"

    /**
     * RC-22: orientação local por código, usada só se a API não mandar `orientacao.mensagem`
     * (research D7). `null` = sem orientação (condição adequada ou código desconhecido).
     */
    fun orientacaoLocal(codigo: String?): String? = when (codigo) {
        "MELHORAR_ILUMINACAO" -> "Melhore a iluminação e tente de novo."
        "REDUZIR_ILUMINACAO_OU_REFLEXO" -> "Reduza a luz direta ou o reflexo sobre o documento."
        "AFASTAR_DOCUMENTO" -> "Afaste o celular do documento."
        "APROXIMAR_DOCUMENTO" -> "Aproxime o celular do documento."
        "VERIFICAR_ESTACAO" -> "Verifique se a estação de captura está ligada."
        "CAPTURA_SEGUE_SEM_SENSOR" -> PODE_ANEXAR_NORMALMENTE
        else -> null
    }

    // E4.2 Protocolo e indicadores (administrador, RC-20). Textos de mobile/design/textos.md; os
    // marcados "criado" não têm quadro e estão registrados lá.
    const val INDICADORES_TITULO = "Protocolo e indicadores"
    const val PROTOCOLO_ATIVO = "Protocolo ativo"
    fun protocoloAtivoResumo(versao: String, regras: Int) =
        "Protocolo acadêmico fictício · versão $versao · $regras ${if (regras == 1) "regra" else "regras"}"
    const val TOTAL_PEDIDOS = "Total de pedidos"
    const val ANALISES_POR_RESULTADO = "Análises por resultado"
    const val RESULTADO_SEM_PENDENCIAS = "Sem pendências identificadas"
    const val RESULTADO_COM_PENDENCIAS = "Com pendências"
    const val RESULTADO_CONFIRA = "Confira a leitura"
    const val RESULTADO_FALHA = "Falha"
    const val REGRAS_MAIS_PENDENCIAS = "Regras com mais pendências"
    const val INDICADORES_NOTA = "Dados agregados de demonstração. Documentos e pedidos individuais não são exibidos."
    const val INDICADORES_CARREGANDO = "Carregando indicadores…"
    const val INDICADORES_ERRO = "Não foi possível carregar os indicadores."

    /**
     * Criado (RC-20): "Falha" conta pedidos, não análises, porque a análise que falha não é gravada.
     * Encurtado em 19/11/2026 (spec 024, Clarifications).
     */
    const val INDICADORES_BASE = "“Falha” mostra os pedidos cuja última verificação não terminou."
    /** Criado (RC-20): estados sem quadro. */
    const val SEM_PROTOCOLO_ATIVO = "Nenhum protocolo ativo."
    const val SEM_PENDENCIAS_REGISTRADAS = "Nenhuma pendência registrada."
    fun quantidadePendencias(total: Long) = "$total ${if (total == 1L) "pendência" else "pendências"}"

    /**
     * Parcelas de uma regra com pendências (ajuste de 19/11/2026): separa o que é pendente do que é
     * não verificável (leitura não conferida), que não significa documento faltando. Só as parcelas
     * maiores que zero; sem parcelas (resposta antiga), devolve `null`.
     */
    fun parcelasPendencias(pendentes: Long, naoVerificaveis: Long): String? = listOfNotNull(
        if (pendentes > 0) "$pendentes ${if (pendentes == 1L) "pendente" else "pendentes"}" else null,
        if (naoVerificaveis > 0) "$naoVerificaveis ${if (naoVerificaveis == 1L) "não verificável" else "não verificáveis"}" else null,
    ).joinToString(" · ").ifEmpty { null }
}
