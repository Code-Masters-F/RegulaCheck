# Tasks: Plano de testes do demonstrador (RC-24)

**Input**: `specs/023-plano-de-testes/` (spec.md, plan.md, research.md, data-model.md, contracts/formato-plano-de-testes.md, quickstart.md)

**Prerequisites**: branch `023-plano-de-testes`; RC-23 entregue (`samples/conjuntos/01..15`).

**Tests**: não solicitados. Feature **documental**: nenhum caso é executado, nenhum código muda. Verificação = leitura do repositório + os `git grep` do `quickstart.md`.

**Organization**: por user story. Quase todo o conteúdo vai para **um único arquivo** (`docs/testes/plano-de-testes.md`), então as tarefas dele são sequenciais; `[P]` só onde o arquivo é outro.

## Regras que valem para toda tarefa

- Todo caso nasce com os 14 campos do contrato; `Status = Não executado`; **Resultado obtido, Evidência, Responsável e data em branco**.
- Resultado esperado vem do contrato real (`docs/api/*`, gabaritos). Divergência com a issue #50 → usar o contrato e anotar a diferença.
- Leitura/extração e resumo = **simulados** (decisão de 12/11/2026). Nunca escrever "extração real", "resumo real" ou "IA real" fora de "a issue diz… o contrato é simulado".
- Casos com dublê dizem `Dublê (não é integração real)`. Impedimento conhecido ≠ status "Bloqueado".
- Todo caminho de `samples/` e todo comando citado deve existir: conferir por **leitura**, sem executar o caso.
- Sem credenciais, senha ou token no plano (referenciar `database/scripts/README.md`).
- Não criar pastas em `docs/evidencias/<ID>/`. Não commitar.

## Phase 1: Setup (levantamento por leitura)

- [X] T001 Listar `samples/conjuntos/*/gabarito.md` (01..15) e `samples/` (PDF, PNG, TXT) e anotar nome exato de pasta/arquivo de cada conjunto; confirmar existência de amostra JPEG (ponto 2 de `specs/023-plano-de-testes/research.md`)
- [X] T002 [P] Ler `docs/api/erros.md`, `docs/api/contratos-dados.md`, `docs/api/autorizacao.md`, `docs/api/estados-e-revisoes.md`, `docs/api/protecoes.md` e `docs/api/openapi.yaml`; anotar rota, código HTTP e código de erro de cada caso previsto
- [X] T003 [P] Ler `docs/ambiente.md`, `docs/testes/README.md`, `docs/evidencias/README.md`, `database/scripts/README.md`, `simulador-iot/` (README e `SimuladorSensores.java`) e `mobile/` (README/roteiros RC-18/RC-19); anotar comandos, variáveis (`SESSION_TTL_MINUTES`, `IOT_API_KEY`, `DOCUMENT_STORAGE_PATH`, `RC_TESTE_ORACLE`, `DB_*`) e contas sintéticas
- [X] T004 Resolver os pontos de contrato da pesquisa lendo código/testes (`backend/src/main/java`, `backend/src/test/java`): (a) campo obrigatório vazio no rascunho 400×422; (b) arquivo >5 MB 400×413; (c) código do conflito ao excluir pedido submetido; (d) divergência na conferência existe como caso executável?; (e) nomes exatos dos testes com dublê (resumo, conexão). Registrar a resposta de cada um em `specs/023-plano-de-testes/research.md` (seção "Pontos a conferir" → resolvidos)

**Checkpoint**: contrato real de cada cenário conhecido; nenhuma suposição em aberto.

---

## Phase 2: Foundational (esqueleto do documento)

**Purpose**: estrutura fixa do plano; bloqueia todas as stories.

- [X] T005 Criar `docs/testes/plano-de-testes.md` com as 10 seções do contrato (`specs/023-plano-de-testes/contracts/formato-plano-de-testes.md`) em ordem e com títulos; seção 1 "Aviso e escopo" completa (material fictício, leitura e resumo simulados em 12/11/2026, resultado ≠ aprovação do SUS, plano ≠ execução, execução/evidências/retestes = RC-25 #51)
- [X] T006 Escrever no plano a seção 3 "Decisões aprovadas × funcionalidades pendentes" (tabela de D3 em `research.md`) e "Diferenças entre a issue e o contrato" (extração/resumo "reais", 403×404 em T02, indicadores T03, alertas T16, T04 troca de serviço, T06 PDF protegido 400, T02 download existe)
- [X] T007 Escrever no plano a seção 4 "Status e regras de uso" (Não executado, Aprovado, Reprovado, Bloqueado; "Aprovado só se todo o esperado conferir"; "Bloqueado só depois de tentar e registrar motivo"; divergência = achado, esperado não é editado) e o **modelo do caso** (tabela de 14 campos do contrato)

**Checkpoint**: esqueleto pronto; casos podem ser escritos.

---

## Phase 3: User Story 2 — 16 cenários com rastreabilidade (P1) 🎯 MVP

**Goal**: T01..T16 com todos os casos `Txx-yy` da issue #50, cada um com regra/histórias e resultado esperado do contrato.

**Independent Test**: existem T01..T16; cada caso tem os 14 campos; matriz mostra lacunas.

- [X] T008 [US2] Seção 7, **T01 Autenticação** (RC-04): login válido, senha incorreta, e-mail inexistente (401 `CREDENCIAIS_INVALIDAS` idêntico), conta desativada, campos ausentes, resposta sem senha/hash, rota protegida sem sessão — em `docs/testes/plano-de-testes.md`
- [X] T009 [US2] **T02 Isolamento entre pacientes** (RC-05, RC-06): contas `paciente.a`/`paciente.b`; listar, abrir, alterar, excluir, baixar documento, abrir análise e submeter pedido alheio → mesmo 404 de inexistente; registrar diferença "negadas" × 404 — em `docs/testes/plano-de-testes.md`
- [X] T010 [US2] **T03 Permissões administrativas** (RC-05, RC-20): casos paciente × administrador (administrador não vê documento; paciente não acessa indicadores); `Impedimento conhecido: RC-20 (P1, rotas inexistentes)`; expectativa 403 só vale quando a rota existir — em `docs/testes/plano-de-testes.md`
- [X] T011 [US2] **T04 Rascunho** (RC-07): criar, ler, trocar serviço (`PROTOCOLO_ALTERADO`), campo obrigatório vazio (usar resposta resolvida em T004), não alterar pedido alheio — em `docs/testes/plano-de-testes.md`
- [X] T012 [US2] **T05 Upload e extração válidos** (RC-08, RC-10): PDF e PNG válidos com conjunto e gabarito, JPEG (ou lacuna se T001 não achou amostra), leitura **simulada**, lista de documentos — em `docs/testes/plano-de-testes.md`
- [X] T013 [US2] **T06 Documentos não aceitos** (RC-06, RC-08, RC-10): >5 págs, **PDF protegido (400 `REQUISICAO_INVALIDA`)**, vazio, corrompido, >5 MB (resposta de T004), >3 arquivos, tipo não suportado (415), extensão incompatível, conjunto `12-formato-invalido`; arquivo recusado não entra na lista nem na análise — em `docs/testes/plano-de-testes.md`
- [X] T014 [US2] **T07 Documento completo** (RC-11, RC-12, RC-14, RC-15): conjunto `01-completo`, geral `SEM_PENDENCIAS_IDENTIFICADAS`, só campo confirmado é evidência, "não é aprovação do SUS" — em `docs/testes/plano-de-testes.md`
- [X] T015 [US2] **T08 Ausências** (RC-12, RC-14, RC-15): conjuntos `02`, `03`, `04`; orientação diferencia paciente × unidade emissora — em `docs/testes/plano-de-testes.md`
- [X] T016 [US2] **T09 Regras condicionais** (RC-01, RC-12; RC-R06): conjuntos `08` (aplicável, atendida), `09` (aplicável, pendente), `10` (não aplicável, 18 anos, depende da data do envio), `15` (não verificável); registrar a data de referência — em `docs/testes/plano-de-testes.md`
- [X] T017 [US2] **T10 Leitura não confiável** (RC-10, RC-11, RC-15): conjuntos `05`, `06`, `07`; conferência `PUT` com decisões; divergência da paciente (resposta de T004; se não houver caso executável, registrar na matriz como lacuna declarada) — em `docs/testes/plano-de-testes.md`
- [X] T018 [US2] **T11 Reenvio e revisão** (RC-09, RC-13, RC-16): conjuntos `13`, `14`; nova revisão invalida resultado anterior; histórico preservado; alteração durante análise = só por teste de concorrência opt-in (limitação declarada) — em `docs/testes/plano-de-testes.md`
- [X] T019 [US2] **T12 Falhas e limites da IA** (RC-06, RC-14): provedor indisponível e saída fora do contrato com **dublê** (nomes de teste de T004, `Dublê (não é integração real)`), conjunto `11-instrucao-no-documento`, resultado determinístico preservado; "provedor real" não existe — em `docs/testes/plano-de-testes.md`
- [X] T020 [US2] **T13 Submissão simulada** (RC-13, RC-17): submissão válida com comprovante "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", repetição = 200 mesmo comprovante, recusas 409 `CONFLITO_ESTADO`, pedido alheio 404 — em `docs/testes/plano-de-testes.md`
- [X] T021 [US2] **T14 Exclusão** (RC-09): excluir rascunho próprio, excluir pedido submetido (código de T004), excluir pedido alheio, documento removido some da lista — em `docs/testes/plano-de-testes.md`
- [X] T022 [US2] **T15 Conectividade e sessão** (RC-04, RC-08, RC-13, RC-19): casos de **API** (idempotência por `X-Chave-Envio`, sessão expirada 401 com `SESSION_TTL_MINUTES`) separados dos casos de **app** (corte de rede do emulador, preservação de dados; cada caso de app com `Impedimento conhecido: nenhum` (PRs #71 e #74 já mergeados; exigem emulador)) — em `docs/testes/plano-de-testes.md`
- [X] T023 [US2] **T16 Sensores** (RC-21, RC-22): **backend** T16-01..05 (dentro da faixa 300–1000 lux/20–40 cm, fora da faixa, leitura >60 s `DESATUALIZADA`, sem chave 401, chave em outra rota 403, sensor indisponível não bloqueia upload) e **interface** T16-06 (alertas, `Impedimento conhecido: RC-22 aberta`) — em `docs/testes/plano-de-testes.md`
- [X] T024 [US2] Seção 9 **Matriz de rastreabilidade**: história (RC-01, RC-04..RC-17, RC-19..RC-22) → casos; regra RC-R01..RC-R06 e resultados atende/pendente/não verificável/não aplicável → casos; conjunto RC-23 → casos; colunas "Tipo de execução" e "Impedimento conhecido"; lacunas com motivo. Critérios: cada uma das 6 regras e cada um dos 4 resultados (atende, pendente, não verificável, não aplicável) em ≥1 caso, e os 15 conjuntos (01..15) em ≥1 caso, senão declarar lacuna — em `docs/testes/plano-de-testes.md`

**Checkpoint**: US2 completa; SC-001, SC-004 verificáveis.

---

## Phase 4: User Story 4 — Preparar, reproduzir falhas, restaurar, evidências (P1)

**Goal**: quem executa sobe o ambiente e provoca falhas só com o texto.

**Independent Test**: seguir apenas as seções 5–7 até o primeiro caso de cada tipo.

- [X] T025 [US4] Seção 5 **Ambiente e preparação**: subir API (`/health`), banco Oracle e variáveis `DB_*`, app (emulador, URL da API), simulador; contas sintéticas (`paciente.a/b@exemplo.invalid`, `admin@exemplo.invalid`, senha por referência a `database/scripts/README.md`); onde ficam `samples/` e gabaritos; data de referência 2026-11-12 só na regressão automática — em `docs/testes/plano-de-testes.md`
- [X] T026 [US4] Seção 5.5 **Procedimentos controlados**: falha de IA (dublê/teste), conectividade (corte de rede do emulador, comando de RC-19), sessão expirada (`SESSION_TTL_MINUTES`), sensores (`--sessao N --sequencia normal|fora-da-faixa|recuperacao [--seco]`, sem chave), **restauração dos dados** (pedidos criados pelo caso são do caso; não reaplicar `02_carga_sintetica.sql` sem limpeza; `90_limpeza_DESTRUTIVA.sql` só com autorização específica) — em `docs/testes/plano-de-testes.md`
- [X] T027 [US4] Seção 6 **Convenção de evidências**: `docs/evidencias/<ID-DO-CASO>/` com data, versão/commit, comando ou passos, resultado bruto, status, executor; nada de token, senha ou documento real — em `docs/testes/plano-de-testes.md`
- [X] T028 [P] [US4] Atualizar `docs/evidencias/README.md` com a convenção `<ID-DO-CASO>/` e o conteúdo mínimo (sem criar pastas de caso)
- [X] T029 [P] [US4] Atualizar `docs/testes/README.md` com link relativo para `plano-de-testes.md` e a separação plano (RC-24) × execução (RC-25)

**Checkpoint**: US4 verificável por leitura; SC-005 conferível.

---

## Phase 5: User Story 5 — Percurso integrado P01 (P2)

**Goal**: percurso ponta a ponta reutilizando os casos.

- [X] T030 [US5] Seção 8 **P01**: login → pedido → upload → leitura simulada → conferência → análise → resumo simulado → substituição → reanálise → submissão simulada; cada etapa aponta o caso `Txx-yy` e o estado esperado do pedido; declarar leitura e resumo simulados — em `docs/testes/plano-de-testes.md`

---

## Phase 6: User Story 1 e 3 — Executável sem orientação e honestidade (P1)

**Goal**: garantias de US1 (autossuficiência) e US3 (nada aprovado, campos em branco).

- [X] T031 [US3] Seção 2 **Status do plano** e seção 10 **Pendências**: data, commit do plano, versão testável "a preencher na execução", **revisão por outro integrante: pendente**; o que a RC-25 deve fazer; itens de T004 resolvidos
- [X] T032 [US1] Reler 1 caso por grupo (funcionamento, falha, segurança) como se fosse outra pessoa: conta, estado do pedido, arquivo, comando e resultado esperado estão no caso? Corrigir lacunas em `docs/testes/plano-de-testes.md`

---

## Phase 7: Polish e verificação (leitura, sem executar casos)

- [X] T033 Rodar os `git grep` do `specs/023-plano-de-testes/quickstart.md` (nenhum status diferente de Não executado; `samples/...` existentes; links relativos entre documentos válidos; `extração real|resumo real|IA real` só em contexto de diferença) e corrigir o que falhar
- [X] T034 Conferir as 8 garantias de `specs/023-plano-de-testes/contracts/formato-plano-de-testes.md`: T01..T16 + P01; 14 campos por caso; RC-R01..RC-R06 e 4 resultados presentes; todo comando citado existe; casos com dublê identificados; impedimentos de T03 e T16-interface; revisão pendente
- [X] T035 [P] Marcar checklist e atualizar `specs/023-plano-de-testes/checklists/requirements.md` se algo mudou; confirmar `git status` só com `docs/testes/`, `docs/evidencias/README.md`, `specs/023-plano-de-testes/`, `CLAUDE.md`
- [X] T036 Relatório final: implementado × verificado (por leitura) × pendente (revisão por outro integrante, execução = RC-25); vínculo RC-24 ↔ FR/SC ↔ tarefas; **sem commit**

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → Phase 3 (T008..T023 em ordem, mesmo arquivo; T024 por último) .
- Phase 4 (T025..T027 no mesmo arquivo) pode vir antes de Phase 3 se preferir ambiente primeiro; T028/T029 são arquivos distintos.
- T030 depende de T008..T023 (referencia IDs). T031/T032 depois dos casos. Phase 7 por último.
- T004 bloqueia T011, T013, T017, T019, T021.
- T025 e T026 dependem de T003 e T004 (comandos, variáveis e nomes de teste).
- T036 informa o total real de casos criados (estimativa do plano: ~45).

## Parallel Opportunities

- T002 ∥ T003 (leituras independentes).
- T028 ∥ T029 (arquivos distintos), e ambos ∥ qualquer tarefa do plano.
- Casos T008..T023 poderiam ser divididos entre pessoas escrevendo em rascunhos separados, mas o destino é um arquivo só; manter sequencial se uma pessoa só.

## Implementation Strategy

- **MVP**: Phase 1–2 + US2 (T008..T024) = plano com 16 cenários e matriz; já atende SC-001..SC-004.
- Depois US4 (reprodutibilidade), US5 (P01), e fechamento (US1/US3, Phase 7).
- Parar após cada fase e conferir por leitura; nenhum caso é executado nesta feature.

## Rastreabilidade

| Story | FR | SC | Tarefas |
|---|---|---|---|
| US1 | FR-004, FR-006 | SC-006 | T032 |
| US2 | FR-002, 003, 006, 007, 012, 013 | SC-001, 003, 004 | T001–T024 |
| US3 | FR-005, 015, 016, 017 | SC-002 | T007, T031 |
| US4 | FR-008, 009, 011, 014 | SC-005 | T025–T029 |
| US5 | FR-010 | — | T030 |
