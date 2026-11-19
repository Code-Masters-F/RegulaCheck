# Tasks: Normalizar documentação e eliminar redundâncias

**Input**: `specs/025-normalizar-documentacao/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md).

**Prerequisites**: branch `025-normalizar-documentacao`; JDK 21 e Maven Wrapper para o baseline; nenhuma infraestrutura nova.

**Tests**: não há teste novo. A verificação é a comparação antes/depois definida em `contracts/verificacao.md` (build/testes existentes, links, convenções, hash do OpenAPI).

**Organization**: por história da spec. US1 (fonte única) e US4 (desempenho preservado) são P1; US2 (convenções) e US3 (links) são P2. Scripts auxiliares ficam no scratchpad, fora do repositório. Sem commit automático.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência)
- **[Story]**: US1..US4
- Regras permanentes: não reescrever `docs/evidencias/`, `docs/testes/resultado-da-rodada.md` (conteúdo), `specs/*` anteriores (só links), `.pen`, `.agents/`, `.claude/`, `.specify/`; manter "simulado" onde IA, OCR ou sensores aparecem; os 94 casos do plano não diminuem; divergência não verificável vira pendência, não escolha silenciosa.

## Path Conventions

- Inventário: `specs/025-normalizar-documentacao/inventario.md`
- Resultados de verificação: `specs/025-normalizar-documentacao/verificacao-resultados.md`
- Decisões: `docs/decisoes.md`

---

## Phase 1: Setup (baseline)

- [X] T001 Registrar o estado de referência em `specs/025-normalizar-documentacao/verificacao-resultados.md`: `git status`, versão do JDK, e `.\mvnw.cmd clean verify` em `backend/` com resultado e tempo reais em 3 execuções (registrar cada tempo, a mediana e o ruído observado; declarar "não executado" se bloqueado)
- [X] T002 Registrar em `specs/025-normalizar-documentacao/verificacao-resultados.md` o hash de `docs/api/openapi.yaml` e de `docs/api/RegulaCheck.postman_collection.json` (SC-007) e a contagem de casos do `docs/testes/plano-de-testes.md` (esperado: 94)
- [X] T003 Criar no scratchpad o script de verificação de links relativos e caminhos citados (Markdown, YAML, SQL e JavaDoc) e executá-lo no baseline, tratando como "caminho citado" todo caminho relativo à raiz entre crases ou em link Markdown, com lista de falsos positivos aceitos; gravar a contagem de quebrados e a lista em `specs/025-normalizar-documentacao/verificacao-resultados.md`
- [X] T004 [P] Criar no scratchpad o script de detecção de duplicação (parágrafos de 3 ou mais linhas repetidos entre documentos) definindo a fórmula de SC-004 (linhas em parágrafos duplicados ÷ total de linhas dos documentos mantidos) e gravando as linhas redundantes iniciais em `specs/025-normalizar-documentacao/verificacao-resultados.md`

---

## Phase 2: Foundational (inventário e mapa de temas)

**Bloqueia todas as histórias.**

- [X] T005 Gerar `specs/025-normalizar-documentacao/inventario.md` com uma linha por arquivo de documentação rastreado (`git ls-files`, formato de `data-model.md`), marcando os protegidos
- [X] T006 Ler o conteúdo de `README.md`, `CONTRATO_API.md`, `CONTRATO_TESTES.md`, `AGENTS.md` e `docs/**` e preencher em `specs/025-normalizar-documentacao/inventario.md` a tabela de Temas com fonte de verdade e ocorrências (partindo do mapa D1 de `research.md`)
- [ ] T007 [P] Ler `mobile/**`, `design/*.md`, `database/**/README.md`, `samples/**/*.md`, `simulador-iot/README.md`, `backend/README.md` e `entrega/README.md` e completar Temas e ocorrências em `specs/025-normalizar-documentacao/inventario.md`
- [ ] T008 Conferir cada tema de contrato (rotas, limites, estados, erros) contra `backend/src/main` e `docs/api/openapi.yaml`, anotando em `specs/025-normalizar-documentacao/inventario.md` divergências encontradas (corrigir na documentação ou abrir pendência)
- [X] T009 Definir o destino de cada documento (manter, normalizar, consolidar-em, remover) pelo critério D2 de `research.md` e marcar em `specs/025-normalizar-documentacao/inventario.md`; documentos exigidos por `CLAUDE.md` ou constituição ficam em manter/normalizar; `CONTRATO_API.md`, `CONTRATO_TESTES.md` e qualquer contrato da raiz recebem a marca "verificar exigência da disciplina" e só são removidos após confirmação do grupo (perguntar ao responsável); sem renomeações em massa: renomear apenas as exceções listadas

**Checkpoint**: inventário completo, todo tema com uma fonte de verdade, destinos decididos.

---

## Phase 3: User Story 1 — Uma única fonte para cada informação (P1) 🎯 MVP

**Goal**: cada tema tem uma definição completa; demais ocorrências são referências.

**Independent Test**: para a amostra de pelo menos 15 temas, busca no repositório mostra uma definição completa e o resto como link (SC-001); redução de pelo menos 20% das linhas redundantes (SC-004).

- [X] T010 [US1] Consolidar o parágrafo de situação por RC de `CONTRATO_API.md` em referência a `docs/api/README.md` e `docs/api/openapi.yaml`, migrando antes qualquer conteúdo único (tabela de perfis e rotas planejadas) para a fonte de verdade
- [X] T011 [US1] Consolidar `CONTRATO_TESTES.md` com `docs/testes/plano-de-testes.md` sem reduzir os 94 casos; deixar referência ou remover conforme o destino definido em T009
- [ ] T012 [P] [US1] Reduzir repetições de rotas, formatos e estados em `docs/arquitetura.md` para links a `docs/api/*`
- [ ] T013 [P] [US1] Reduzir repetições de autorização e proteções em `docs/api/autorizacao.md` e `docs/api/protecoes.md` frente a `docs/arquitetura.md` e `CLAUDE.md`, mantendo as fontes em `docs/api/`
- [ ] T014 [P] [US1] Remover repetições de comandos e variáveis de ambiente em `README.md`, `backend/README.md`, `mobile/README.md` e `simulador-iot/README.md`, apontando para `docs/ambiente.md`
- [ ] T015 [P] [US1] Reduzir repetições de decisões entre `docs/decisoes.md` e `docs/escopo-e-limitacoes.md`; manter as decisões em `docs/decisoes.md`
- [ ] T016 [P] [US1] Reconciliar `database/README.md` e `database/scripts/README.md` com `docs/modelo-de-dados.md` (um dono por tema: ordem de execução em `database/scripts/README.md`, modelo em `docs/modelo-de-dados.md`)
- [ ] T017 [P] [US1] Reconciliar `design/README.md`, `design/verificacao-2026-11-13.md`, `mobile/design/*` e a seção de design de `AGENTS.md`, mantendo o conteúdo exigido por `AGENTS.md` e eliminando cópias
- [ ] T018 [P] [US1] Reduzir repetição interna de `docs/testes/plano-de-testes.md` (preâmbulos e regras repetidas por seção) sem alterar nenhum caso, esperado ou ID; conferir 94 casos ao final
- [ ] T019 [US1] Reduzir em `CLAUDE.md` e `AGENTS.md` somente contradições ou duplicações comprovadas entre si e com a constituição, preservando todas as regras; registrar cada trecho alterado em `specs/025-normalizar-documentacao/inventario.md` e em `docs/decisoes.md` (T036)
- [ ] T020 [US1] Executar a amostra de conferência (mínimo 15 temas) e gravar o resultado em `specs/025-normalizar-documentacao/verificacao-resultados.md`

**Checkpoint**: US1 verificável sozinha pela amostra de temas.

---

## Phase 4: User Story 4 — Comportamento e desempenho preservados (P1)

**Goal**: nenhuma mudança funcional nem de tempo.

**Independent Test**: build/testes equivalentes ao baseline, OpenAPI inalterado, diff de produção só em comentários (SC-005, SC-007).

- [ ] T021 [US4] Revisar JavaDoc e comentários de `backend/src/main/java/**` apenas nos pontos que divergem do comportamento ou da documentação consolidada (um pacote por vez), sem alterar assinaturas
- [ ] T022 [US4] Identificar candidatos a código morto por busca de referências; remover somente com prova (nenhuma referência em `src/main` nem em `src/test`), em lotes pequenos com `.\mvnw.cmd clean verify` verde após cada lote, e registrar cada remoção em `specs/025-normalizar-documentacao/verificacao-resultados.md`; manter e registrar pendência em caso de dúvida
- [ ] T023 [US4] Conferir que os scripts de `database/scripts/*.sql` mudaram apenas em comentários de cabeçalho, comparando o diff
- [X] T024 [US4] Executar `.\mvnw.cmd clean verify` em `backend/` e comparar resultado e tempo com T001; comparar os hashes do OpenAPI e da coleção Postman com T002; usar 3 execuções e a mediana, avaliando o limite de +5% contra o ruído registrado em T001; gravar o resultado real em `specs/025-normalizar-documentacao/verificacao-resultados.md`
- [X] T025 [US4] Confirmar por `git diff --stat -- backend/src/main` que alterações são só comentários/JavaDoc ou remoções registradas em T022

**Checkpoint**: P1 completo (US1 e US4); entrega mínima válida.

---

## Phase 5: User Story 2 — Formato e estrutura padronizados (P2)

**Goal**: convenções C1 a C12 aplicadas.

**Independent Test**: lista de `contracts/convencoes-documentais.md` com zero desvios (SC-003, SC-006).

- [ ] T026 [US2] Aplicar C1 a C5 e C8 a C10 aos documentos da raiz e de `docs/` (título, linha de propósito, datas, glossário, marcação de simulado)
- [ ] T027 [P] [US2] Aplicar C1 a C5 e C8 a C10 a `docs/api/**` (exceto `openapi.yaml` e coleção Postman, sem mudança de contrato) e `docs/testes/README.md`
- [ ] T028 [P] [US2] Aplicar as convenções a `mobile/**`, `design/*.md`, `database/**/README.md`, `samples/**/*.md`, `simulador-iot/README.md`, `backend/README.md` e `entrega/README.md`
- [ ] T029 [US2] Definir exceções de nome de arquivo (README, INDEX, CLAUDE, AGENTS, CONTRATO_*) em `specs/025-normalizar-documentacao/inventario.md`; renomear só se nenhum link externo à feature depender, atualizando todas as referências
- [ ] T030 [US2] Atualizar os índices `docs/README.md`, `docs/api/README.md`, `docs/testes/README.md`, `docs/evidencias/README.md` e `mobile/design/INDEX.md` para listar todos os documentos existentes, cada um em um índice (C11)
- [ ] T031 [US2] Aplicar a lista C1 a C12 a cada documento mantido e gravar desvios restantes (meta zero) em `specs/025-normalizar-documentacao/verificacao-resultados.md`; medir SC-006 (caminho do README raiz até cada documento em até 3 links)

**Checkpoint**: US2 verificável pela lista de convenções.

---

## Phase 6: User Story 3 — Links e referências íntegros (P2)

**Goal**: zero links quebrados e zero órfãos.

**Independent Test**: script de T003 retorna zero (SC-002).

- [ ] T032 [US3] Executar o script de links de T003 e corrigir cada quebrado nos documentos mantidos, redirecionando citações de documentos removidos à fonte de verdade
- [ ] T033 [P] [US3] Ajustar somente links em `specs/**` anteriores e em `docs/evidencias/**` que apontem para arquivos movidos ou removidos, sem tocar em conteúdo (somente a URL do link; mostrar o diff de cada arquivo de evidência alterado)
- [ ] T034 [US3] Conferir IDs RC-XX citados (RC-01 a RC-25) contra `specs/` e o backlog, corrigindo ou registrando pendência
- [ ] T035 [US3] Reexecutar o script de links e o de órfãos de índice e gravar zero/zero em `specs/025-normalizar-documentacao/verificacao-resultados.md`

**Checkpoint**: US3 verificável pelo script.

---

## Phase 7: Polish e registro

- [X] T036 Registrar em `docs/decisoes.md` as decisões da limpeza: fontes de verdade, arquivos removidos ou consolidados, divergências resolvidas e pendências (FR-012)
- [ ] T037 [P] Conferir que `docs/arquitetura.md` e `docs/ambiente.md` seguem consistentes com as alterações (FR-005)
- [ ] T038 Repetir todas as verificações de `contracts/verificacao.md` e completar `specs/025-normalizar-documentacao/verificacao-resultados.md` com resultados reais; calcular SC-004 pela fórmula de T004; declarar o que não foi executado
- [ ] T039 Marcar as tarefas concluídas apenas com evidência registrada e revisar `git diff` completo, sem apagar trabalho alheio; conferir com `git diff --stat` que `.specify/`, `.agents/`, `.claude/` e `specs/` anteriores só mudaram em links e no ponteiro do plano em `CLAUDE.md` (FR-013)
- [ ] T040 Relatório final distinguindo implementado, verificado, pendente e fora do escopo, com o vínculo `025-normalizar-documentacao` (sem ID RC definido)

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → US1 e US4 (P1) → US2 e US3 (P2) → Polish.
- Phase 2 bloqueia todas as histórias.
- US4 (T024) deve rodar depois de US1 e da revisão de código (T021 a T023); a conferência final repete-se em T038.
- US2 depende de US1 (consolidar antes de padronizar); US3 depende de US1 e US2 (links só estabilizam depois de mover e renomear).
- T010 e T011 antes de T012 a T018 se compartilharem conteúdo migrado; T019 por último em US1.

## Parallel Opportunities

- Setup: T004 em paralelo a T001 a T003.
- Foundational: T007 em paralelo a T006.
- US1: T012 a T018 em paralelo (arquivos distintos).
- US2: T027 e T028 em paralelo.
- US3: T033 em paralelo a T034.

## Implementation Strategy

1. **MVP**: Phases 1, 2, 3 e 4 (fonte única com desempenho e comportamento comprovadamente preservados).
2. Incremento: US2 (convenções), depois US3 (links), pois os links só estabilizam no fim.
3. Cada fase termina com verificação registrada; não marcar tarefa sem evidência.

## Rastreabilidade

| História | Requisitos | Tarefas | Critérios |
|---|---|---|---|
| US1 | FR-001 a FR-003, FR-005, FR-008, FR-009, FR-014 | T005 a T020 | SC-001, SC-004 |
| US2 | FR-004, FR-007 | T026 a T031 | SC-003, SC-006 |
| US3 | FR-006, FR-007 | T032 a T035 | SC-002 |
| US4 | FR-010, FR-011, FR-013 | T001, T002, T021 a T025 | SC-005, SC-007 |
| Transversal | FR-012 | T036 a T040 | todos |
