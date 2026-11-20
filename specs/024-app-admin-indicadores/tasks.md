# Tasks: Administrador — protocolo e indicadores (RC-20)

**Input**: `specs/024-app-admin-indicadores/` (plan.md, spec.md, research.md, data-model.md, contracts/admin-indicadores.md, quickstart.md)

**Tests**: pedidos na história (paciente 403, sem sessão 401, administrador 200 conferido contra dados de teste, ausência de dado identificável; no app, login de administrador → E4.2 e de paciente → Meus pedidos).

**Rastreabilidade**: todas as tarefas pertencem à RC-20 (#43). US1 = rota da API; US2 = tela E4.2 e login por perfil; US3 = isolamento de perfis no app.

## Phase 1: Setup

Sem tarefas: projetos, versões e dependências existentes são reaproveitados (plan.md).

## Phase 2: Foundational

- [X] T001 Acrescentar `IndicadorRepository indicadores` em `backend/src/main/java/br/com/regulacheck/config/Dependencias.java` e atualizar as montagens em `backend/src/main/java/br/com/regulacheck/config/Servidor.java`, `backend/src/test/java/br/com/regulacheck/auth/CenarioAuth.java`, `.../solicitacao/BancoIndisponivelSolicitacaoTest.java`, `.../seguranca/HigieneDeLogGeralTest.java`, `.../documento/BancoEArmazenamentoIndisponiveisTest.java`, `.../common/http/CabecalhosSegurosTest.java`
- [X] T002 Criar o contrato `IndicadorRepository` e o registro `ContagensIndicadores` (com `PendenciasDaRegra`) em `backend/src/main/java/br/com/regulacheck/indicador/repository/`

## Phase 3: User Story 1 — Agregados pela API como administrador (P1) 🎯 MVP

**Goal**: `GET /admin/indicadores` só para ADMINISTRADOR, só agregados.

**Independent Test**: `IndicadorEndpointTest` com contagens de teste conhecidas.

- [X] T003 [P] [US1] Repositório em memória com contagens configuráveis, contador de consultas e falha programável em `backend/src/test/java/br/com/regulacheck/indicador/IndicadorRepositoryMemoria.java`
- [X] T004 [US1] `IndicadorRepositoryJdbc` (3 consultas agregadas numa conexão; 503 `DEPENDENCIA_INDISPONIVEL`) em `backend/src/main/java/br/com/regulacheck/indicador/repository/IndicadorRepositoryJdbc.java`
- [X] T005 [US1] `Indicadores` e `IndicadorService` (zeros nos 3 grupos, top 5 por total desc/código asc, só total > 0, protocolos ativos com regras, relógio) em `backend/src/main/java/br/com/regulacheck/indicador/service/`
- [X] T006 [US1] `IndicadoresResponse` (contrato de `contracts/admin-indicadores.md`) em `backend/src/main/java/br/com/regulacheck/indicador/dto/IndicadoresResponse.java`
- [X] T007 [US1] `IndicadorController` (`GET`) em `backend/src/main/java/br/com/regulacheck/indicador/controller/IndicadorController.java` e registro `rotas.registrar("/admin/indicadores", ..., Perfil.ADMINISTRADOR)` em `backend/src/main/java/br/com/regulacheck/config/Composicao.java`
- [X] T008 [P] [US1] `IndicadorServiceTest` (zeros, ordenação e desempate, corte em 5, exclusão de total 0, sem protocolo ativo, `consultadoEm`) em `backend/src/test/java/br/com/regulacheck/indicador/IndicadorServiceTest.java`
- [X] T009 [US1] `IndicadorEndpointTest` (admin 200 com valores esperados; paciente 403 sem consultar o repositório; sem token 401; POST do admin 405; falha → 503; corpo sem `@`, `Paciente`, `email`, `nome`, `pacienteId`, `caminho`, ids de pedido nem mensagens) em `backend/src/test/java/br/com/regulacheck/indicador/IndicadorEndpointTest.java`
- [X] T010 [US1] Documentar a rota em `docs/api/openapi.yaml` e em `docs/api/autorizacao.md`
- [X] T011 [US1] Rodar `./mvnw clean verify` em `backend/` (só a falha conhecida do ambiente)

## Phase 4: User Story 2 — Login de administrador e tela E4.2 (P1)

**Goal**: administrador entra e vê E4.2; paciente continua em Meus pedidos.

**Independent Test**: testes do app com MockWebServer e do ViewModel.

- [X] T012 [P] [US2] Modelos `Indicadores` e afins em `mobile/app/src/main/java/br/com/regulacheck/app/api/Modelos.kt` e `consultarIndicadores()` em `mobile/app/src/main/java/br/com/regulacheck/app/api/RegulaCheckApi.kt`
- [X] T013 [US2] `ServicoSessao.entrar` aceita PACIENTE e ADMINISTRADOR (outro perfil recusado: `PerfilNaoSuportado`) em `mobile/app/src/main/java/br/com/regulacheck/app/sessao/ServicoSessao.kt`; ajustar `FalhaLogin` e texto em `mobile/app/src/main/java/br/com/regulacheck/app/ui/login/LoginTela.kt`
- [X] T014 [US2] `Tela.Indicadores` e `telaDaSessao(perfil)` em `mobile/app/src/main/java/br/com/regulacheck/app/ui/navegacao/Navegacao.kt`; usar na tela inicial e na reação à sessão em `.../ui/navegacao/AppRegulaCheck.kt`
- [X] T015 [US2] Textos de E4.2 em `mobile/app/src/main/java/br/com/regulacheck/app/ui/Textos.kt`
- [X] T016 [US2] `IndicadoresViewModel` e `IndicadoresTela` (padrão, carregando, erro com "Tentar de novo", 403, nota de demonstração, rodapé, "Sair") em `mobile/app/src/main/java/br/com/regulacheck/app/ui/indicadores/IndicadoresTela.kt`
- [X] T017 [P] [US2] `IndicadoresApiTest` (decodificação, `Authorization`, 403 → `SemPermissao`, 401 → sessão encerrada) em `mobile/app/src/test/java/br/com/regulacheck/app/api/IndicadoresApiTest.kt`
- [X] T018 [US2] Testes de login por perfil (admin guarda sessão ADMINISTRADOR; paciente segue) em `mobile/app/src/test/java/br/com/regulacheck/app/sessao/ServicoSessaoTest.kt` e do ViewModel (carregando → sucesso; falha → tentar de novo; 403) em `mobile/app/src/test/java/br/com/regulacheck/app/ui/IndicadoresViewModelTest.kt` (arquivo próprio em vez de `FluxoViewModelTest.kt`)
- [X] T019 [US2] Rodar `./gradlew assembleDebug testDebugUnitTest` em `mobile/app/`

## Phase 5: User Story 3 — Isolamento entre perfis no app (P2)

- [X] T020 [US3] Testes de `telaDaSessao` (admin → Indicadores; paciente nunca → Indicadores) e de perfil desconhecido recusado sem guardar sessão em `mobile/app/src/test/java/br/com/regulacheck/app/sessao/ServicoSessaoTest.kt`

## Phase 6: Polish

- [X] T021 [P] Registrar textos criados de E4.2 em `mobile/design/textos.md` e o estado da tela em `mobile/README.md` (implementado × só desenho × não verificado)
- [X] T022 Verificação manual com o backend real (reinício pelo usuário a partir da branch): API com admin/paciente/sem token e E4.2 no emulador; registrar só em texto em `docs/evidencias/2026-11-18_rc20-indicadores-administrador.md`
- [X] T023 Revisar o diff (sem segredos, sem capturas, sem `.env`) e atualizar `docs/ambiente.md` só se algum comando mudou

## Phase 7: Ajuste de textos da E4.2 (19/11/2026)

Decisão em spec.md, Clarifications, sessão 2026-11-19 (FR-010). Só o app muda; a rota e a resposta continuam iguais.

- [X] T024 Regras só pelo nome no cartão do protocolo e em "Regras com mais pendências" (`ui/Textos.kt`, `ui/indicadores/IndicadoresTela.kt`)
- [X] T025 Parcelas pendente e não verificável abaixo de cada regra com mais pendências, só as maiores que zero, e teste do texto (`ui/Textos.kt`, `ui/indicadores/IndicadoresTela.kt`, `TextosIndicadoresTest.kt`)
- [X] T026 Nota da base dos números mais curta, só sobre "Falha" (`ui/Textos.kt`)
- [X] T027 [P] Atualizar `mobile/design/textos.md`
- [X] T028 `.\gradlew.bat assembleDebug testDebugUnitTest` verde e E4.2 conferida no emulador com o backend real — 123 testes, 0 falhas (19/11/2026, com o contorno de loopback de `docs/ambiente.md`); E4.2 com regras só pelo nome, parcelas abaixo de cada regra (ex.: "1 pendente · 5 não verificáveis") e a nota curta de "Falha"

## Dependencies

- T001–T002 → US1. US1 (rota) → T022 (verificação real). US2 depende só do contrato (T012 pode começar com US1). US3 depende de T013–T014.
- Paralelos: T003 ∥ T004; T008 ∥ T009 após T005–T007; T012 ∥ backend; T017 ∥ T016; T021 a qualquer momento após T015.

## Implementation Strategy

MVP = US1 (rota protegida e testada). Depois US2 (tela e login) e US3 (testes de isolamento). Polimento e verificação real por último; a verificação real depende do reinício do backend pelo usuário.
