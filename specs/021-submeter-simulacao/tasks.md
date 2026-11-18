---

description: "Tarefas da RC-17 — Submeter solicitação em simulação"
---

# Tasks: Submeter solicitação em simulação (RC-17)

**Input**: Design documents from `specs/021-submeter-simulacao/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-submissao.md](contracts/api-submissao.md), [quickstart.md](quickstart.md)

**Tests**: Obrigatórios pela constituição (VII) e pelos SC-001 a SC-006: idempotência e concorrência, revisão desatualizada, autorização entre contas, linguagem de simulação e higiene de log.

**Organization**: Tarefas por história da spec (US1–US4). Backlog: RC-17 (#39).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história da spec (US1–US4)
- `main/` = `backend/src/main/java/br/com/regulacheck/`; `test/` = `backend/src/test/java/br/com/regulacheck/`

---

## Phase 1: Setup

- [X] T001 Registrar o build de referência (`sh ./mvnw clean verify`: 731 testes, 0 falhas, 32 ignorados) em "Resultado da verificação" deste `specs/021-submeter-simulacao/tasks.md`
- [X] T002 Atualizar o JavaDoc de `main/submissao/package-info.java` (RC-17: submissão simulada, referência interna e comprovante; nada é enviado ao SUS)

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Purpose**: Entidade, contrato do repositório, implementação JDBC, dobro em memória e ligação nas dependências

- [X] T003 [P] Criar `main/submissao/entity/Submissao.java` (record com id, solicitacaoId, analiseId, referencia, submetidaEm, revisaoAnalisada, protocoloIdentificador, protocoloVersao, localidade, especialidade, avisoFicticio; JavaDoc; nunca serializado)
- [X] T004 [P] Criar `main/submissao/repository/ResultadoSubmissao.java` (situações `NAO_ENCONTRADO`, `JA_SUBMETIDA`, `SOMENTE_CONSULTA`, `EM_ANDAMENTO`, `SEM_ANALISE_VIGENTE`, `COM_PENDENCIAS`, `SUBMETIDA`, com fábricas)
- [X] T005 Criar `main/submissao/repository/SubmissaoRepository.java` (`submeter(pacienteId, solicitacaoId, referencia, agora)` e `buscar(pacienteId, solicitacaoId)`; JavaDoc com a ordem de decisão sob o bloqueio do data-model)
- [X] T006 Criar `main/submissao/repository/SubmissaoRepositoryJdbc.java` (transação curta com `SELECT … FOR UPDATE` do pedido filtrado pela dona; análise vigente da revisão atual com contagem de resultados `PENDENTE`/`NAO_VERIFICAVEL`; `INSERT RC_SUBMISSAO` com `SUBMETIDA_EM` do relógio da aplicação; `UPDATE` do estado; leitura do comprovante com `JOIN RC_ANALISE`/`RC_PROTOCOLO`; ORA-00001 → devolve a existente; 503 sem detalhe; logs só com ids)
- [X] T007 Acrescentar `submissoes` a `main/config/Dependencias.java` e `new SubmissaoRepositoryJdbc(banco)` em `main/config/Servidor.java`
- [X] T008 Criar `test/submissao/SubmissaoRepositoryMemoria.java` (mesmas decisões do JDBC sobre os dobros de pedido e análise, no monitor do repositório de pedidos; contagem e injeção de falha para os testes) e ligá-lo em `test/auth/CenarioAuth.java` e nos testes que montam `Dependencias` à mão (`test/common/http/CabecalhosSegurosTest.java`, `test/documento/BancoEArmazenamentoIndisponiveisTest.java`, `test/seguranca/HigieneDeLogGeralTest.java`, `test/solicitacao/BancoIndisponivelSolicitacaoTest.java`)

**Checkpoint**: build compila e os 731 testes anteriores continuam verdes

---

## Phase 3: User Story 1 — Concluir e receber o comprovante (Priority: P1) 🎯 MVP

**Goal**: Pedido sem pendências vira `SUBMETIDA_SIMULACAO` com comprovante

**Independent Test**: Caso sintético 1 analisado sem pendências → `POST …/submissoes-simuladas` → 201 com todos os itens de FR-005; pedido só consulta

- [X] T009 [US1] Criar `main/submissao/service/SubmissaoService.java` (`submeter` e `consultar` pela sessão; `referencia(solicitacaoId, instante)` = `SIM-{ano em Salvador}-{id ≥ 6 dígitos}`; tradução dos desfechos em 201/200/404/409 com as mensagens do contrato; record `Resultado(submissao, criada)`; logs só com ids e desfecho; JavaDoc)
- [X] T010 [P] [US1] Criar `main/submissao/dto/SubmissaoResponse.java` (comprovante do contrato; destaque e nota fixos; `simulada: true`; ids como texto; instante ISO-8601 UTC)
- [X] T011 [US1] Criar `main/submissao/controller/SubmissaoController.java` (`POST` e `GET` em `/{id}/submissoes-simuladas`; id inválido ou segmento a mais → 404 padrão) e entregar o subcaminho em `main/solicitacao/controller/SolicitacaoController.java`; montar em `main/config/Composicao.java`
- [X] T012 [P] [US1] Criar `test/submissao/ReferenciaSimuladaTest.java` (formato, ano em Salvador na virada do ano UTC, ids grandes, diferença do formato da carga `SIM-2026-0001`)
- [X] T013 [US1] Criar `test/submissao/SubmeterEndpointTest.java`: percurso feliz (201, comprovante completo, estado `SUBMETIDA_SIMULACAO`, uma linha); depois da submissão, documentos/leitura/conferência/análise/reenvio/captura → 409 e consultas → 200 (FR-011)

---

## Phase 4: User Story 2 — Recusa quando o pedido não está pronto (Priority: P1)

**Goal**: 409 com mensagem do simulador, sem registro nem mudança de estado

**Independent Test**: Cada situação não pronta → 409, 0 submissões, estado igual

- [X] T014 [US2] Criar `test/submissao/RecusasDaSubmissaoTest.java`: `COM_PENDENCIAS` (encaminhamento sem motivo) e `REQUER_CONFERENCIA` (leitura não conferida) → mensagem de pendências com "restrição do simulador" e "não impede atendimento"; rascunho, nunca analisado, `FALHA_ANALISE` → sem análise vigente; análise sem pendências que perdeu a vigência por inclusão, substituição ou decisão de conferência → sem análise vigente; `PROCESSANDO` → em andamento; `SUBMETIDA_SIMULACAO` forçado sem registro → somente consulta; análise vigente sem pendências com estado do pedido forçado para outro valor (dado inconsistente) → sem análise vigente; banco indisponível na conclusão (falha injetada no dobro) → 503 sem registro parcial; em todos, 0 submissões e estado igual
- [X] T015 [P] [US2] Criar `test/submissao/MatrizPerfisSubmissaoTest.java` (anônimo 401; administrador e simulador 403 em `POST`/`GET` sem registrar nada; pedido alheio, inexistente e id ilegível → 404 idêntico em `POST` e `GET`)

---

## Phase 5: User Story 3 — Repetir sem duplicar e recuperar (Priority: P1)

**Goal**: Idempotência por pedido e consulta do comprovante

**Independent Test**: Concluir, repetir (inclusive 10 simultâneos) e consultar → 1 submissão, mesma referência e data

- [X] T016 [US3] Em `test/submissao/SubmeterEndpointTest.java`: repetir → 200 com o mesmo comprovante; `GET` → igual ao da conclusão; `GET` antes da submissão → 404 idêntico ao de pedido inexistente; 10 `POST` simultâneos → exatamente 1 submissão, um 201 e mesma referência em todas as respostas de sucesso

---

## Phase 6: User Story 4 — Linguagem de simulação (Priority: P2)

**Goal**: Destaque e nota em todo comprovante; nenhuma alegação de aprovação, fila, vaga, prazo, diagnóstico ou urgência; logs sem conteúdo

- [X] T017 [US4] Criar `test/submissao/HigieneDeLogSubmissaoTest.java`: conclusão, repetição, consulta e recusas não levam conteúdo das amostras ao log nem às respostas; comprovantes e mensagens não contêm termos afirmativos proibidos (lista do teste) e trazem destaque e `simulada: true`

---

## Phase 7: Polish & documentação

- [X] T018 [P] Criar `test/submissao/SubmissaoOracleTest.java` (opt-in; gravação, repetição, consulta, 4 simultâneos sob o bloqueio real, consulta da submissão da carga `SIM-2026-0001`; remove só o que criou)
- [X] T019 [P] Atualizar `docs/api/contratos-dados.md` (seção "Submissão simulada") e `CONTRATO_API.md` (rotas `POST`/`GET`, implementadas)
- [X] T020 [P] Atualizar `docs/api/openapi.yaml` (rota e schema `ComprovanteSimulado`, espelho de `SubmissaoResponse`)
- [X] T021 [P] Atualizar `docs/api/erros.md` (409 da submissão; remover a previsão de 422 para "submeter com pendências") e `docs/api/estados-e-revisoes.md` (transição implementada)
- [X] T022 [P] Atualizar `docs/api/autorizacao.md`, `docs/api/RegulaCheck.postman_collection.json` e `docs/api/README.md`
- [X] T023 [P] Atualizar `docs/arquitetura.md`, `docs/decisoes.md`, `docs/ambiente.md` (`SubmissaoOracleTest`) e `README.md`
- [X] T024 Rodar `sh ./mvnw clean verify` em `backend/`, registrar comando e resultado; revisar o diff (sem segredo, conteúdo de documento ou código morto)
- [X] T025 Executar `SubmissaoOracleTest` no Oracle **somente com autorização** para a conta compartilhada; sem ela, registrar "não verificado no Oracle"
- [X] T026 Atualizar o status em `specs/021-submeter-simulacao/spec.md` conforme o verificado

---

## Dependencies & Execution Order

- Setup (T001–T002) → Foundational (T003–T008) → US1 (T009–T013) → US2, US3 e US4 (testes sobre a rota da US1) → Polish.
- T006 depende de T003–T005; T008 depende de T005 e T007.
- T009 depende de T005; T011 depende de T009 e T010.
- T014–T017 dependem de T011.
- T024 depois de todo o código; T025 opcional sob autorização; T026 por último.

## Parallel Example

```text
Foundational: T003 ‖ T004
US1: T010 ‖ T012 depois de T009
US2: T015 ‖ T014
Polish: T018 ‖ T019 ‖ T020 ‖ T021 ‖ T022 ‖ T023
```

## Implementation Strategy

1. **MVP**: Phases 1–3 (conclusão com comprovante).
2. **Incremento**: recusas (US2), idempotência e consulta (US3), linguagem e logs (US4).
3. Documentação, build e Oracle sob autorização. Tela do comprovante fica no épico 6.

## Resultado da verificação

- **Referência antes da RC-17** (17/11/2026, `sh ./mvnw clean verify` em `backend/`, branch sobre a `main` com a RC-16): 731 testes, 0 falhas, 0 erros, 32 ignorados, `BUILD SUCCESS`.
- **Depois da RC-17** (17/11/2026, `sh ./mvnw -B clean verify`, JDK local 25 com `release 21`): **757 testes, 0 falhas, 0 erros, 35 ignorados** (os 32 Oracle opt-in anteriores + 3 do `SubmissaoOracleTest`), `BUILD SUCCESS`. +26 testes: `ReferenciaSimuladaTest` 4, `SubmeterEndpointTest` 6, `RecusasDaSubmissaoTest` 9, `MatrizPerfisSubmissaoTest` 3, `HigieneDeLogSubmissaoTest` 1, `SubmissaoOracleTest` 3 (ignorados).
- Durante a implementação o `RecusasDaSubmissaoTest` pegou um erro de ordem de decisão (pedido `COM_PENDENCIAS` recebia "sem análise vigente" em vez de "pendências"); corrigido no JDBC, no dobro e no data-model antes do build final.
- Teste existente ajustado por mudança real de comportamento: `SituacaoESemEstadoDoPedidoTest` (RC-11) afirmava 404 em `…/submissoes-simuladas` porque a rota não existia; agora afirma o que a intenção pedia — conferir não libera submissão — com 409, nenhuma submissão e estado `RASCUNHO`.
- `git diff --check` sem problemas; nenhum `TODO`, `System.out` ou segredo nos arquivos novos; OpenAPI validado como YAML e coleção Postman como JSON.
- **Oracle real** (17/11/2026, com autorização do responsável; `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw -B test -Dtest='SubmissaoOracleTest,AnaliseOracleTest,ReenvioOracleTest')`): **6 testes, 0 falhas, 0 ignorados**, `BUILD SUCCESS` — `SubmissaoOracleTest` 3 (conclusão grava `RC_SUBMISSAO` apontando a análise vigente sem pendências e muda o estado na mesma transação; repetição e consulta devolvem o mesmo comprovante com `SUBMETIDA_EM` igual; reenvio depois → 409; 4 conclusões simultâneas sob o bloqueio real → 1 submissão; a submissão da carga `SIM-2026-0001` é consultável e repetir não a altera), mais a regressão de `AnaliseOracleTest` (2) e `ReenvioOracleTest` (1). Limpeza do `@AfterAll` sem erro; nenhum segredo no log. Nenhum script SQL novo.
- **Não verificado:** `quickstart.md` contra a API no ar; tela do comprovante (épico 6).

### Rastreabilidade RC-17 (#39) → requisitos → testes

| Critério de aceite | Requisitos | Código | Verificação |
|---|---|---|---|
| 1. Só a própria paciente, com análise vigente sem pendências e sem não verificáveis | FR-001, FR-002, FR-006, FR-007 | `SubmissaoRepositoryJdbc.submeter` (decisão sob o bloqueio), `SubmissaoService`, `SubmissaoController` | `RecusasDaSubmissaoTest` (9 situações), `MatrizPerfisSubmissaoTest`, `SituacaoESemEstadoDoPedidoTest` |
| 2. Referência interna única, data e comprovante com o destaque | FR-003, FR-004, FR-005 | `SubmissaoService.referencia`, `SubmissaoResponse`, `Submissao` | `ReferenciaSimuladaTest`, `SubmeterEndpointTest.pedidoSemPendenciasEConcluidoComComprovanteCompleto` |
| 3. Repetir não cria outra; resultado anterior recuperável | FR-008, FR-009 | `RC_SUBMISSAO_UK_SOLIC` + checagem sob bloqueio; `GET …/submissoes-simuladas` | `SubmeterEndpointTest` (repetição, consulta, 404 antes, 10 simultâneos) |
| 4. Sem fila real, protocolo oficial ou aprovação; restrição é do simulador | FR-007, FR-010, FR-011, FR-012 | mensagens do `SubmissaoService`, destaque/nota do `SubmissaoResponse` | `HigieneDeLogSubmissaoTest`, `RecusasDaSubmissaoTest`, `SubmeterEndpointTest.depoisDaSubmissaoOPedidoESoConsulta` |
