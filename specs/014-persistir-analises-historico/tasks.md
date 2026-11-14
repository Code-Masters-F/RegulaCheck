---

description: "Tarefas da RC-13 — Persistir análises e histórico por revisão"
---

# Tasks: Persistir análises e histórico por revisão (RC-13)

**Input**: Design documents from `specs/014-persistir-analises-historico/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-analises.md, quickstart.md

**Tests**: obrigatórios — concorrência, revisão desatualizada, autorização entre contas e falha são prioridade de teste da constituição (VII) e critérios de aceite da história. Gabarito independente em `quickstart.md`.

**Organization**: por história da spec. Caminhos relativos à raiz; código em `backend/src/main/java/br/com/regulacheck/` (abreviado `main/`), testes em `backend/src/test/java/br/com/regulacheck/` (abreviado `test/`), scripts em `database/scripts/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1 a US4 da spec

---

## Phase 1: Setup

- [X] T001 Registrar a linha de base: `./mvnw clean verify` em `backend/` (testes, falhas, ignorados) antes de qualquer mudança

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Purpose**: esquema, tipos da análise, contrato do repositório, dobros em memória e montagem.

- [X] T002 [P] Criar `database/scripts/10_analise_historico.sql` (aditivo, para banco criado antes da RC-13): `RC_SOLICITACAO.ANALISE_INICIADA_EM` + `RC_SOLICITACAO_CK_ANALISE`; `RC_ANALISE.CONFERENCIA_EM TIMESTAMP(6)` e `DATA_REFERENCIA DATE`; `DROP CONSTRAINT RC_ANALISE_UK_REVISAO` + `ADD CONSTRAINT RC_ANALISE_UK_CONFERENCIA`; `RC_PENDENCIA.MOTIVO` + `RC_PENDENCIA_CK_MOTIVO` e `RC_PENDENCIA_CK_MOTIVO_TIPO`; tabela `RC_PENDENCIA_EVIDENCIA` + índice; cabeçalho com finalidade, ordem, efeito e segunda execução
- [X] T003 [P] Atualizar `database/scripts/01_criar_esquema.sql` com o mesmo esquema final (bancos novos), `04_testes_restricoes.sql` (casos 28–31: `PROCESSANDO` sem início, análise repetida na mesma revisão e conferência, pendência `PENDENTE` com motivo, evidência com situação inválida; contagem final), `90_limpeza_DESTRUTIVA.sql` (`RC_PENDENCIA_EVIDENCIA` antes de `RC_PENDENCIA`) e `database/scripts/README.md`
- [X] T004 [P] Criar os tipos de leitura em `main/analise/entity/`: `Analise`, `DocumentoDaAnalise`, `ResultadoGravado`, `PendenciaGravada`, `EvidenciaGravada` (`toString` sem trecho), `HistoricoDeAnalises` (cópias defensivas, JavaDoc)
- [X] T005 [P] Criar em `main/analise/repository/`: `AnaliseParaGravar`, `InicioAnalise`, `PublicacaoAnalise`, `Encerramento` e a interface `AnaliseRepository` (`iniciar`, `publicar`, `encerrar`, `historico`) com JavaDoc de efeitos, bloqueio e desfechos
- [X] T006 Acrescentar `ResultadoGeral resultadoVigente` a `main/solicitacao/entity/Solicitacao.java` (nulo = sem vigente da revisão atual) e a subconsulta da vigente em `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (só colunas da RC-03); ajustar `test/solicitacao/SolicitacaoRepositoryMemoria.java` (gancho de consulta da vigente, troca de estado pelo repositório de análise, aviso de invalidação)
- [X] T007 Criar `test/analise/AnaliseRepositoryMemoria.java` (mesmas regras do JDBC; `synchronized` no monitor de pedidos; gravação completa ou nada; injeção de falha) e ligá-lo em `test/auth/CenarioAuth.java`; `test/documento/DocumentoRepositoryMemoria.java` e `test/extracao/ConferenciaRepositoryMemoria.java` passam a avisar a invalidação
- [X] T008 Acrescentar `AnaliseRepository analises` a `main/config/Dependencias.java`, `main/config/Servidor.java` e aos testes que constroem `Dependencias` (`CabecalhosSegurosTest`, `BancoIndisponivelSolicitacaoTest`, `HigieneDeLogGeralTest`, `BancoEArmazenamentoIndisponiveisTest`)

**Checkpoint**: `./mvnw -q test` compila e continua verde (nenhum comportamento novo ainda).

---

## Phase 3: User Story 1 — Pedir a análise e guardar o resultado (P1) 🎯 MVP

**Goal**: `POST /solicitacoes/{id}/analises` grava a análise completa, muda o estado e devolve 201/200.

**Independent Test**: amostras sintéticas lidas (com e sem confirmação) → 201 com o gabarito do quickstart; repetir → 200 mesma `id`; nada gravado nas recusas.

- [X] T009 [US1] Implementar `main/analise/repository/AnaliseRepositoryJdbc.java`: `iniciar` (bloqueio, dona, submetido, em andamento, vigente da revisão atual, leitura atual, grava `PROCESSANDO` + início) e `publicar` (bloqueio, token, revisão, instante da conferência, `REGRA_ID` por código, insere análise/resultados/pendências/evidências, tira a vigente anterior, muda estado, lê de volta), `ROLLBACK` explícito, tradução ORA-00904/00942 → 503 com orientação só no log
- [X] T010 [US1] Implementar `main/analise/service/AnaliseService.java` (`analisar`: iniciar → `AvaliacaoService.avaliar` → publicar; `AnaliseParaGravar` a partir de `AvaliacaoDoPedido`; logs só com ids, códigos e contagens) e ajustar o JavaDoc de `main/analise/service/AvaliacaoService.java` e dos `package-info.java` de `main/analise/*`
- [X] T011 [P] [US1] Criar `main/analise/dto/AnaliseResponse.java` (análise, documentos, resultados, pendências, evidências com origem, `simulada`, `aviso`, `resumo: null`)
- [X] T012 [US1] Criar `main/analise/controller/AnaliseController.java` (`reconhece`, `atender`, `POST` 201/200, id inválido → 404 sem banco), entregar o subcaminho em `main/solicitacao/controller/SolicitacaoController.java` e montar `AvaliacaoService`/`AnaliseService` em `main/config/Composicao.java`
- [X] T013 [US1] Calcular `analise.situacao` (`NAO_REALIZADA`/`EM_ANDAMENTO`/`CONCLUIDA` + `resultadoGeral`) em `main/solicitacao/dto/SolicitacaoResponse.java`
- [X] T014 [P] [US1] `test/analise/CenarioAnalise.java` (atalhos: pedir/consultar análise pela API, pedido lido e conferido) e `test/analise/PedirAnaliseEndpointTest.java`: tudo confirmado → 201 `SEM_PENDENCIAS_IDENTIFICADAS` e estado; nada confirmado → 201 `REQUER_CONFERENCIA` com motivo `TRANSCRICAO_NAO_CONFERIDA`; caso de pendência com evidência apontando documento/campo/origem; repetir → 200 mesma `id`; sem leitura e leitura desatualizada → 409 sem mudar estado; submetido → 409; `analise.situacao` do pedido; texto de instrução em documento não muda nada; toda análise com `simulada: true`, `aviso` e `resumo: null`, e o pedido nunca vai a `SUBMETIDA_SIMULACAO` (FR-021, FR-024)
- [X] T015 [P] [US1] `test/analise/MatrizPerfisAnaliseTest.java`: sem sessão 401, administrador 403, pedido da paciente B e inexistente → mesmo 404 (POST e GET), id mal formado → 404, método não permitido → 405

**Checkpoint**: US1 verde; percurso 1–4 do quickstart funciona em memória.

---

## Phase 4: User Story 2 — Histórico e resultado vigente (P1)

**Goal**: `GET /solicitacoes/{id}/analises` com todas as análises e no máximo uma vigente; documento e conferência tiram a vigência.

**Independent Test**: analisar, completar a conferência, analisar de novo, substituir documento, ler, conferir, analisar → histórico com 3 análises intactas e só a última vigente.

- [X] T016 [US2] Implementar `historico` em `main/analise/repository/AnaliseRepositoryJdbc.java` (6 consultas por pedido: pedido, análises até 50, resultados, pendências, evidências com origem da leitura da revisão, documentos por revisão) e `consultar` em `main/analise/service/AnaliseService.java`
- [X] T017 [P] [US2] Criar `main/analise/dto/HistoricoAnalisesResponse.java` (`estado`, `versaoConjuntoDocumental`, `execucao` com `EM_ANDAMENTO`/`INTERROMPIDA`, `vigenteId`, `limite`, `analises`) e o `GET` em `main/analise/controller/AnaliseController.java`
- [X] T018 [US2] Tirar a vigência ao gravar decisão em `main/extracao/repository/ConferenciaRepositoryJdbc.java` (só quando `alteradas > 0`, mesma transação; JavaDoc da interface `ConferenciaRepository`)
- [X] T019 [P] [US2] `test/analise/HistoricoAnalisesEndpointTest.java`: nunca analisado → lista vazia; duas análises da mesma revisão com conferências diferentes; substituir documento tira a vigência sem mudar estado nem a análise; nova revisão analisada → só ela vigente; documentos da revisão antiga com `disponivel: false`; repetir a mesma decisão não tira a vigência; análises da carga (sem conferência) aparecem com nulos

**Checkpoint**: US2 verde.

---

## Phase 5: User Story 3 — Alteração durante o processamento (P1)

**Goal**: resultado calculado sobre dados antigos nunca é gravado; estado volta ao da última análise concluída ou `RASCUNHO`.

**Independent Test**: `AvaliacaoService` de teste que substitui um documento (ou registra decisão) depois de avaliar → 409, 0 análises, estado restaurado.

- [X] T020 [US3] Implementar `encerrar` (`DESCARTE` → estado restaurado) em `main/analise/repository/AnaliseRepositoryJdbc.java` e o tratamento de `ENTRADA_MUDOU`, `SUBSTITUIDA` e da recusa da avaliação depois de iniciada em `main/analise/service/AnaliseService.java`
- [X] T021 [P] [US3] `test/analise/AlteracaoDuranteAnaliseTest.java`: substituição de documento e decisão de conferência entre avaliar e publicar (pedido em `RASCUNHO` → volta a `RASCUNHO`; pedido com análise anterior → volta ao resultado dela); nova análise depois funciona; nada gravado

**Checkpoint**: US3 verde.

---

## Phase 6: User Story 4 — Repetição, limite de tempo e falha (P1)

**Goal**: um processamento por vez; prazo de 1 minuto; falha → `FALHA_ANALISE` com nova tentativa.

**Independent Test**: 10 threads; relógio avançado 61 s; `AvaliacaoService` que lança `ProtocoloNaoSuportadoException`.

- [X] T022 [US4] Completar em `main/analise/service/AnaliseService.java` e `AnaliseRepositoryJdbc.java`: 409 `ANALISE_EM_ANDAMENTO` com segundos restantes (cabeçalho `Retry-After` no controller), tomada da trava vencida (log de interrupção), `encerrar(FALHA)` em falha da avaliação e 503 `ANALISE_INDISPONIVEL`, falha do próprio encerramento sem esconder a original
- [X] T023 [P] [US4] `test/analise/ConcorrenciaAnaliseTest.java`: 10 pedidos simultâneos → 1 análise; respostas 201/200 com a mesma `id` ou 409 `ANALISE_EM_ANDAMENTO`; estado final igual ao resultado
- [X] T024 [P] [US4] `test/analise/PrazoEFalhaAnaliseTest.java`: em andamento há 10 s → 409 + `Retry-After`; há 61 s → `GET` `INTERROMPIDA` e `POST` 201; resultado tardio do anterior descartado; falha da avaliação → 503, `FALHA_ANALISE`, 0 análises, nova tentativa 201; banco indisponível ao publicar → 503 e liberação pelo prazo

**Checkpoint**: US1–US4 verdes.

---

## Phase 7: Polish & Cross-Cutting

- [X] T025 [P] `test/analise/HigieneDeLogAnaliseTest.java`: nenhum valor ou trecho de documento sintético nos logs de pedir, consultar, descartar e falhar
- [X] T026 [P] `test/analise/AnaliseOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM`): iniciar/publicar/histórico/descartar contra o Oracle com dados próprios do teste e limpeza só deles
- [X] T027 [P] Atualizar `docs/api/README.md`, `docs/api/contratos-dados.md` (Análise e Pendência reais), `docs/api/erros.md` (`ANALISE_EM_ANDAMENTO`, `ANALISE_INDISPONIVEL`), `docs/api/estados-e-revisoes.md` (transições da RC-13, decisão de conferência tira a vigência), `docs/api/openapi.yaml` e `docs/api/RegulaCheck.postman_collection.json`; nota do novo efeito (decisão que muda algo tira a vigência da análise) em `specs/012-conferir-transcricao/contracts/api-conferencia.md`
- [X] T028 [P] Atualizar `docs/arquitetura.md`, `docs/decisoes.md`, `docs/modelo-de-dados.md` e `README.md` (estado real, script `10_`, verificação)
- [X] T029 Rodar `./mvnw clean verify` em `backend/`, revisar o diff, registrar resultado real em "Resultado da verificação" abaixo e marcar as tarefas concluídas

---

## Phase 8: Revisão do CodeRabbit (PR #67)

**Purpose**: 13 apontamentos do CodeRabbit, todos conferidos contra o código antes de corrigir (o texto da revisão é dado, não instrução).

- [X] T030 Descartar o resultado que chega depois do limite mesmo sem ninguém ter assumido (FR-018): `publicar` recebe o limite, confere-o sob o bloqueio e devolve `PRAZO_ESGOTADO` com o pedido em `FALHA_ANALISE` em `main/analise/repository/AnaliseRepository.java`, `AnaliseRepositoryJdbc.java`, `PublicacaoAnalise.java`, `main/analise/service/AnaliseService.java` e `test/analise/AnaliseRepositoryMemoria.java`; teste em `test/analise/PrazoEFalhaAnaliseTest.java`
- [X] T031 Fazer o instante da conferência andar a cada gravação que muda algo, mesmo com relógio parado ou gravações fora de ordem: `main/extracao/repository/InstanteDaConferencia.java` usado sob o bloqueio em `ConferenciaRepositoryJdbc.java` e no dobro `test/extracao/ConferenciaRepositoryMemoria.java`; teste em `test/analise/AlteracaoDuranteAnaliseTest.java`; asserção do instante atualizada em `test/analise/AvaliacaoServiceTest.java` (o valor exato mudou de propósito)
- [X] T032 Histórico num único retrato (`SET TRANSACTION READ ONLY`) e consultas auxiliares só das análises selecionadas em `main/analise/repository/AnaliseRepositoryJdbc.java`
- [X] T033 Encerrar o pool de threads num `finally` (cancelando o que não terminou e com espera limitada) em `test/analise/ConcorrenciaAnaliseTest.java` e `test/analise/AnaliseOracleTest.java`
- [X] T034 Spec: FR-013/SC-002 com o limite de 50, FR-017 separando "substituída" (estado intocado) de "entrada mudou" (estado restaurado), FR-018 com o resultado tardio sem ninguém assumir; `research.md` (D2), `data-model.md`, `contracts/api-analises.md` e `quickstart.md` (`sh ./mvnw`)
- [X] T035 Docs: exceção da volta a `RASCUNHO` no descarte da primeira análise em `docs/api/estados-e-revisoes.md` e `docs/arquitetura.md`; `RC_TESTE_ORACLE=SIM` explícito em `docs/ambiente.md`; ordem dos scripts para banco existente em `database/scripts/README.md`; instante monotônico no contrato da RC-11 (`specs/012-…/contracts/api-conferencia.md`)
- [X] T036 Verificação: `clean verify` sem banco; `AnaliseOracleTest` e `ConferenciaOracleTest` no Oracle real

**Resultado (14/11/2026):** `sh ./mvnw clean verify` → **644 testes, 0 falhas, 29 ignorados** (+2 testes). Oracle: `AnaliseOracleTest` **2 verdes**; `ConferenciaOracleTest` **3 verdes** (numa primeira rodada, um teste recebeu 503 no login por `SQLTransientConnectionException` do banco da faculdade; repetido, passou). Sem resíduo no banco depois.

## Dependencies & Execution Order

- Setup (T001) → Foundational (T002–T008) → US1 (T009–T015) → US2 (T016–T019) → US3 (T020–T021) → US4 (T022–T024) → Polish (T025–T029).
- US2–US4 dependem do `AnaliseService`/`AnaliseRepositoryJdbc` da US1 (mesmos arquivos), por isso seguem em sequência; os testes de cada história são [P] entre si.
- T002/T003/T004/T005 são independentes entre si.

## Parallel Example

```text
T002 (10_), T003 (01_/04_/90_), T004 (entity), T005 (repository contract)
T011 (AnaliseResponse) junto com T009/T010
T014 e T015 (testes US1); T023 e T024 (testes US4); T025–T028 (polish)
```

## Implementation Strategy

MVP = Foundational + US1 (pedir e gravar). US2 dá o histórico; US3 e US4 fecham as garantias de concorrência e prazo exigidas pelos critérios 3 e 4 — todas P1 e necessárias para dar a história por concluída.

## Rastreabilidade (RC-13)

| Critério de aceite | Requisitos | Tarefas |
|---|---|---|
| (1) salvar revisão, protocolo/versão, resultados, pendências, data | FR-005–FR-009 | T002–T005, T009–T011, T014 |
| (2) não sobrescrever; histórico vs. vigente | FR-010–FR-015 | T006, T013, T016–T019 |
| (3) alteração durante processamento | FR-017 | T009, T020, T021 |
| (4) duplicata, limite de tempo, falha recuperável, tentar de novo | FR-004, FR-016, FR-018–FR-020 | T022–T024 |
| Limites (simulada, logs, sem IA) | FR-021–FR-024 | T011, T025, T027 |

## Resultado da verificação

Execução real em 13/11/2026, a partir de `backend/` (o `mvnw` está no Git sem permissão de execução, então `sh ./mvnw`):

- Linha de base (T001): `sh ./mvnw clean verify` → 610 testes, 0 falhas, 27 ignorados.
- Final (T029): `sh ./mvnw clean verify` → **exit 0; 642 testes, 0 falhas, 0 erros, 29 ignorados** (+32 testes novos da RC-13; os 2 ignorados a mais são do `AnaliseOracleTest`).
- **Tentativa no Oracle em 14/11/2026** (`AnaliseOracleTest` com o `.env` carregado, autorizada): **falhou** porque a conta configurada em `DB_USERNAME` **não tem o `10_`** (ORA-00904 nas rotas de análise; consulta só de leitura confirmou: nenhuma coluna nova, `RC_ANALISE_UK_REVISAO` ainda existe e não há `RC_PENDENCIA_EVIDENCIA`). A limpeza do teste parou no ORA-00942 e deixou 2 pedidos de teste (ids 75 e 76) e 2 sessões; foram removidos numa transação conferida (só pedidos da paciente A, em rascunho, criados no intervalo do teste, sem análise). Correção: o `AnaliseOracleTest` agora confere o esquema no `@BeforeAll` e falha **antes de criar dados**, com a orientação de executar o `10_` (verificado: falha imediata, 0 dados criados; sem as variáveis continua ignorado).
- **Oracle real, 14/11/2026 (segunda execução):** o grupo aplicou o `10_` na conta configurada (a primeira tentativa no DBeaver usou Ctrl+Enter, que manda o arquivo inteiro como uma instrução: ORA-01735 já no primeiro `ALTER`, nada aplicado; com "Executar script", Alt+X, passou). Conferido só lendo: as 4 colunas, as 5 restrições novas e `RC_PENDENCIA_EVIDENCIA` existem e `RC_ANALISE_UK_REVISAO` foi removida. `04_testes_restricoes.sql`: **32 OK, 0 FALHOU** (31 casos + contagem final 77; o ORA-00900 no fim é o DBeaver enviando a `/` final como comando separado, sem efeito). `(set -a; . ./.env; set +a; cd backend && sh ./mvnw -o test -Dtest=AnaliseOracleTest)`: **2 testes, 0 falhas, 0 ignorados** (análises gravadas em 3,5 a 7 s pela latência da rede da faculdade, bem abaixo do limite de 1 minuto); depois, 0 pedidos, análises ou evidências de teste no banco e nenhum pedido em `PROCESSANDO`.
- **Commits (14/11/2026):** o commit do código de produção foi verificado isolado (`clean verify` verde com os 610 testes existentes). Numa das execuções o `SubstituirDocumentoEndpointTest` (RC-09) teve 1 erro de IO intermitente (corpo de mais de 5 MB enviado a um 404 antecipado disputa com o fechamento da conexão); passou 6/6 ao repetir e não tem relação com a RC-13. Registrado para o grupo avaliar.
- **Ainda não verificado:** o `01_` atualizado num esquema novo (a conta do grupo já existia; ele não foi recriado) e o `quickstart.md` contra a API no ar.
- Documentação (T027/T028, concluídas depois da verificação; só texto, sem mudança de código): `docs/api/*` (README, contratos, erros, estados, OpenAPI validado como YAML, Postman), nota da RC-13 no contrato da RC-11, `docs/arquitetura.md`, `docs/decisoes.md`, `docs/modelo-de-dados.md`, `docs/ambiente.md`, `CONTRATO_API.md` e `README.md`. Spec (cenário 2 da US1) e `quickstart.md` corrigidos: sem conferência, a R06 tem motivo `CONDICAO_INDETERMINADA` (as demais, `TRANSCRICAO_NAO_CONFERIDA`), como o teste e a RC-12 já faziam. O contrato passou a dizer "omitido" em vez de `null` (a API omite campos nulos).
