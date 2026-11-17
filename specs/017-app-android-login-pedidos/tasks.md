# Tasks: App Android — base, login e meus pedidos (RC-18)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-consumida.md](contracts/api-consumida.md)

**Tests**: pedidos pela #41 (JUnit4 + MockWebServer dos mapeamentos de erro).

## Format: `[ID] [P?] [Story] Description`

**Caminhos**: `SRC` = `mobile/app/src/main/java/br/com/regulacheck/app/` · `TEST` = `mobile/app/src/test/java/br/com/regulacheck/app/` · `RES` = `mobile/app/src/main/res/`.

## Phase 1: Setup

- [X] T001 Criar o projeto Gradle de um módulo em `mobile/app/` (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `.gitignore`) com as versões de research D1, `BuildConfig.API_BASE_URL` por `-PapiBaseUrl` e minify desligado no debug
- [X] T002 Gerar o Gradle Wrapper 8.7 (`mobile/app/gradlew`, `gradlew.bat`, `gradle/wrapper/`) com a distribuição do cache local
- [X] T003 [P] Criar `mobile/app/src/main/AndroidManifest.xml` (permissão INTERNET, `RegulaCheckApp`, `MainActivity`, `allowBackup=false`) e `mobile/app/src/debug/res/xml/network_security_config.xml` + manifesto debug que o referencia
- [X] T004 [P] Baixar Bricolage Grotesque e Nunito Sans (OFL) para `RES/font/` com a licença em `mobile/app/licencas/`

## Phase 2: Foundational

- [X] T005 [P] Modelos `@Serializable` em `SRC/api/Modelos.kt` (data-model.md), sem expor senha/token em `toString`
- [X] T006 [P] `ErroApi` e `Resultado<T>` em `SRC/api/ErroApi.kt` e `SRC/api/Resultado.kt` (research D6)
- [X] T007 `ApiClient` em `SRC/api/ApiClient.kt`: OkHttp com 10 s/30 s (+ cliente de 70 s), `retryOnConnectionFailure(false)`, token injetado, 401 central (D5), mapeamento HTTP/IO → `ErroApi`, sem log
- [X] T008 `RegulaCheckApi` em `SRC/api/RegulaCheckApi.kt` com `entrar`, `sair(token)`, `listarSolicitacoes`, `obterSolicitacao`
- [X] T009 [P] Sessão em `SRC/sessao/`: `SessaoLocal`, `CofreSessao` + `CofreSessaoKeystore` (AES-GCM, D4), `GerenciadorSessao` (`EstadoSessao`, expiração local)
- [X] T010 `ServicoSessao` em `SRC/sessao/ServicoSessao.kt`: entrar (recusa conta não paciente) e sair (apaga mesmo com falha)
- [X] T011 [P] Tema em `SRC/ui/tema/` (`Cores.kt`, `Tipografia.kt`, `TemaRegulaCheck.kt`, `Icones.kt` Lucide) a partir de `mobile/design/design-system.md`
- [X] T012 [P] Componentes em `SRC/ui/componentes/` (aviso de rodapé e de entrada, cabeçalhos marca/voltar, botões primário/contorno/adicionar, campo de texto e senha, rótulo de estado, retorno de erro/sem conexão/sem permissão, esqueleto)
- [X] T013 Navegação em `SRC/ui/navegacao/` (`Tela`, `EstadoTela`, `Navegador` com `ViewModelStore` por entrada, D3), `SRC/RegulaCheckApp.kt` e `SRC/MainActivity.kt` (401/sair → login)
- [X] T014 [P] Testes de `ApiClient` em `TEST/api/ApiClientTest.kt`: 400, 401 login, 401 sessão (callback), 403, 404, 409 + `Retry-After`, 413, 415, 503, sem conexão, tempo esgotado, sem retentativa, token no cabeçalho, JSON inválido, estado desconhecido, tempos configurados

**Checkpoint**: `./gradlew assembleDebug testDebugUnitTest` verde; app vazio abre no emulador.

## Phase 3: User Story 1 — Entrar (P1) 🎯 MVP

**Independent Test**: login com a conta fictícia chega à lista; senha errada mantém o e-mail e limpa a senha.

- [X] T015 [P] [US1] Testes de `ServicoSessao` em `TEST/sessao/ServicoSessaoTest.kt` (sucesso guarda sessão; conta não paciente → logout com o token e nada guardado; sair com 503/sem rede apaga a sessão)
- [X] T016 [US1] `LoginViewModel` e `LoginTela` em `SRC/ui/login/` (E1.1: padrão, enviando, credenciais inválidas, indisponível, sem conexão, conta não paciente, aviso de sessão expirada de E1.4)
- [X] T017 [US1] Registrar o texto novo da conta não paciente em `mobile/design/textos.md` (E1.1)

## Phase 4: User Story 2 — Meus pedidos (P1)

**Independent Test**: a lista mostra o que `GET /solicitacoes` devolve; backend parado → erro com "Tentar de novo"; Sair volta ao login.

- [X] T018 [P] [US2] `EstadoPedidoUi` e `Formatos` em `SRC/ui/EstadoPedidoUi.kt` e `SRC/ui/Formatos.kt` (tabela de data-model.md, `DESCONHECIDO`), com testes em `TEST/ui/EstadoPedidoUiTest.kt`
- [X] T019 [US2] `MeusPedidosViewModel` e `MeusPedidosTela` em `SRC/ui/pedidos/` (E1.2: carregando, vazia, sucesso com destaque, erro, sem conexão, sem permissão; Novo pedido; Sair)

## Phase 5: User Story 3 — Pedido (P2)

**Independent Test**: cada pedido mostra os dados de `GET /solicitacoes/{id}` e a ação esperada; 404 → Não encontrado.

- [X] T020 [US3] `PedidoViewModel` e `PedidoTela` em `SRC/ui/pedido/` (E1.3: carregando, dados, próximo passo e ação por estado, Atualizar em `PROCESSANDO`, não encontrado, sem permissão, erro)

## Phase 6: Polish

- [X] T021 Build, testes e verificação por screenshot no emulador comparando com as prévias; revisão do logcat; registro em `docs/evidencias/rc18-app-android.md`
- [X] T022 [P] Roteiro manual com backend real em `docs/evidencias/rc18-roteiro-manual.md` (ou bloqueio registrado)
- [X] T023 [P] Atualizar `mobile/README.md` e `docs/ambiente.md` (como executar, JDK 21, URL da API, implementado × só desenho)

## Phase 7: Aviso acadêmico discreto (17/11/2026)

**Purpose**: acompanhar a decisão do responsável registrada na spec 015 (clarification de 17/11, FR-003, FR-020; tarefas T075–T078 no desenho). FR-011 atualizado.

- [X] T024 [US1] Trocar `AvisoEntrada` por `AvisoProposta` (aviso curto sobre a proposta do app) em `SRC/ui/componentes/Estrutura.kt` e `SRC/ui/login/LoginTela.kt`
- [X] T025 Tornar `AvisoRodape` tocável (ícone de informação, alvo de 48 dp, rótulo para leitor de tela) e mostrar o aviso completo em balão (`TooltipBox` + `RichTooltip` do Material 3) em `SRC/ui/componentes/Estrutura.kt`
- [X] T026 Atualizar `SRC/ui/Textos.kt` com os textos de `mobile/design/textos.md` (F3)
- [X] T027 Build, testes e conferência no emulador (login com o aviso curto; balão aberto pelo rodapé) contra as prévias reexportadas; registrar em `docs/evidencias/`
- [X] T028 [P] Atualizar `mobile/README.md` (regras de conteúdo)

## Dependencies

Setup (T001–T004) → Foundational (T005–T014) → US1 (T015–T017) → US2 (T018–T019) → US3 (T020) → Polish. US2 e US3 dependem da sessão de US1 para serem exercitadas no app, mas o código de cada uma é independente.

**Paralelo**: T003/T004; T005/T006/T009/T011/T012; T014 com T015; T018 com T016; T022/T023.

## Implementation Strategy

MVP = Phases 1–3 (entrar). Depois lista (US2) e detalhe (US3). As ações das telas que pertencem à RC-19 mostram o aviso de etapa indisponível (research D7).

## Notas de execução (17/11/2026)

- **Verificado:** `./gradlew assembleDebug testDebugUnitTest` verde (34 testes); telas no emulador contra as prévias; login 503 e backend parado com o backend real sem banco; logcat sem segredo do app. Detalhes: `docs/evidencias/2026-11-17_rc18-app-android.md`.
- **Backend + Oracle (17/11, depois do `.env` preenchido):** roteiro `docs/evidencias/2026-11-17_rc18-roteiro-manual.md` executado no emulador — 11 de 12 passos ✅ (login certo e errado, lista e detalhe iguais à API, sem conexão por corte de rede, conta não paciente, sair, logcat). SC-005 verificado. **Pendente:** sessão expirada com o backend real (passo 9, exige `SESSION_TTL_MINUTES=1`).
- **Análise (A1/U1):** estados de E1.3 que dependem de rotas da RC-19 não foram conferidos; o selo SIMULADO não é usado por nenhuma tela da RC-18 e fica para a RC-19.
- **Ajustes durante a verificação:** Bricolage com `opsz` 96 (research D8) e conexão nova por chamada (research D10, teste em `ApiClientTest`).
