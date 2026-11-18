# Tasks: Executar testes e registrar evidências (RC-25)

**Input**: `specs/024-executar-testes-evidencias/` (plan.md, spec.md, research.md, data-model.md, contracts/formato-evidencia-e-registro.md, quickstart.md) e o plano `docs/testes/plano-de-testes.md`.

**Prerequisites**: PR #77 (plano) na `main`; API, Oracle, emulador e simulador conforme plano 5.1. O responsável sobe a API com `DB_*` (o agente não lê `.env`).

**Tests**: esta feature **executa** casos; não cria teste novo, salvo o teste de regressão de um defeito bloqueador corrigido (US4).

**Organization**: por história da spec. Tarefas de execução são agrupadas **por cenário** (um cenário = vários casos do plano); cada caso executado gera sua própria pasta de evidência. Casos não tentados permanecem "Não executado".

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (casos e pastas diferentes, sem dependência)
- **[Story]**: US1..US5 conforme a spec
- Regras permanentes em toda tarefa de execução: pedido novo por caso; mascarar `<TOKEN>`, `<SENHA>`, `<CHAVE>`, `<DB>`; extração e resumo declarados simulados; dublê identificado; esperado do plano nunca editado; sem commit/push/comentário sem autorização.

## Path Conventions

- Plano: `docs/testes/plano-de-testes.md` · Evidência: `docs/evidencias/<ID-DO-CASO>/{registro.md,bruto/}` · Resumo: `docs/testes/resultado-da-rodada.md`

---

## Phase 1: Setup (preparação e gate de ambiente)

- [X] T001 Registrar a versão testada: `git rev-parse --short HEAD` e `git status --short`; anotar o commit do código testado e informar que as alterações locais são só de `docs/` e `specs/` (a árvore não fica limpa por causa das evidências), em `docs/testes/resultado-da-rodada.md` (rascunho inicial)
- [X] T002 Conferir ambiente por leitura e comando e anotar o resultado no rascunho de `docs/testes/resultado-da-rodada.md`: `java -version`, presença (não valor) de `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`DOCUMENT_STORAGE_PATH`/`IOT_API_KEY`, localização do `adb` e do emulador, JDK 21 do Android Studio
- [X] T003 Construir a versão testada: a partir de `backend/`, `.\mvnw.cmd clean verify`; guardar a saída em `docs/evidencias/_ambiente/00-build.txt` (não é evidência de caso). Executar **antes** de o responsável subir a API (o jar fica bloqueado no Windows se a API estiver rodando). Baseline não aprova caso por si só
- [X] T004 [P] Rodar a regressão dos conjuntos: `.\mvnw.cmd -Dtest=ConjuntosDemonstracaoTest test` (a partir de `backend/`); guardar a saída em `docs/evidencias/_ambiente/00-regressao-conjuntos.txt`
- [ ] T005 Verificar se a RC-20 e a RC-22 já existem (tentativa real, D7): `GET /admin/indicadores` (sem token agora; com a conta administrador, repetido em T030 após T007), e procurar a tela de alertas no app; anotar no rascunho do resumo para decidir se T03 e T16-06 são executados ou Bloqueados — **parcial (18/11):** `GET /admin/indicadores` sem token → 404 (`docs/evidencias/_ambiente/03-indicadores-tentativa.txt`); nenhuma tela de alertas no app (busca por `FORA_DA_FAIXA`/`alerta`). Falta repetir com token administrador após T007

---

## Phase 2: Foundational (bloqueia as histórias)

**⚠️ CRITICAL**: nenhuma execução de API começa antes da T007.

- [ ] T006 Pedir ao responsável que suba a API com `DB_*`, `DOCUMENT_STORAGE_PATH` (fora do repositório) e `IOT_API_KEY`, e informar que o agente não lê `.env`; registrar o pedido no rascunho do resumo. Combinar também as **janelas de reinício**: (a) `SESSION_TTL_MINUTES=1` para T01-05, T15-03 e T15-07; (b) API sem `DB_*` para T15-04; (c) `IOT_API_KEY` igual na API e no simulador para T16; depois de cada janela, voltar ao padrão — **pendente do responsável**
- [ ] T007 Confirmar a API: `curl -s http://localhost:8080/health` → 200 `{"status":"UP"}` e um login válido de teste (PB-1). Se `/auth/login` responder 503 `DEPENDENCIA_INDISPONIVEL`, guardar a resposta em `docs/evidencias/T01-01/bruto/` como tentativa e marcar os casos "API real + Oracle" como Bloqueados na etapa US3 — **tentado (18/11), pendente:** `/health` 200 UP, `POST /auth/login` → 503 `DEPENDENCIA_INDISPONIVEL` (API do agente sem `DB_*`; `docs/evidencias/_ambiente/01-*.txt`, `02-*.txt`). Aguardando o responsável subir a API com Oracle
- [X] T008 [P] Criar o esqueleto de `docs/testes/resultado-da-rodada.md` (versão, ambiente, tabela caso → status → evidência com os 94 IDs "Não executado", achados, lacunas, pendências) conforme `data-model.md`
- [ ] T009 [P] Conferir o PB-1..PB-7 do plano 5.4 contra `docs/api/contratos-dados.md` e anotar qualquer rota ou cabeçalho divergente como achado `A-nn` (sem editar o esperado)

**Ordem lógica:** T033 (PDF protegido) antes de T019; T003 antes de T006; a parte autenticada de T005 em T030.

**Checkpoint**: ambiente conhecido (de pé ou Bloqueado com tentativa); versão registrada.

---

## Phase 3: User Story 1 — Percurso P01 com evidência (Priority: P1) 🎯 MVP

**Goal**: provar o fluxo integrado: login → pedido → upload real → leitura simulada → conferência → regras → resumo simulado → correção → reanálise → submissão simulada.

**Independent Test**: `docs/evidencias/P01/registro.md` descreve as 11 etapas com resultado bruto e o status reflete o esperado do P01.

- [ ] T010 [US1] Executar as etapas 1–3 do P01 (PB-1, PB-2, PB-3 com `encaminhamento.pdf` e `identificacao.png` de `samples/conjuntos/09-menor-sem-autorizacao/`), guardando respostas em `docs/evidencias/P01/bruto/` e o `id` do pedido em `docs/evidencias/P01/registro.md`
- [ ] T011 [US1] Executar as etapas 4–7 do P01: leitura simulada (PB-4), conferência de todos os campos `IDENTIFICADO` (PB-5), análise (PB-6, RC-R06 `PENDENTE`) e resumo simulado; comparar com `samples/conjuntos/09-menor-sem-autorizacao/gabarito.md`
- [ ] T012 [US1] Executar as etapas 8–9: substituir o encaminhamento pelo do conjunto 14 (`samples/conjuntos/14-corrigido-de-09/`), reler, reconferir e reanalisar; conferir `SEM_PENDENCIAS_IDENTIFICADAS` e `comparacao` RC-R06 `RESOLVIDA` contra o gabarito 14
- [ ] T013 [US1] Executar as etapas 10–11: `POST /solicitacoes/{id}/submissoes-simuladas` e a repetição com o mesmo comprovante; conferir "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" e estado `SUBMETIDA_SIMULACAO`
- [ ] T014 [US1] Fechar `docs/evidencias/P01/registro.md` (cadeia completa, esperado × obtido por etapa, "extração e resumo simulados", status) e preencher o caso P01 em `docs/testes/plano-de-testes.md` (Ambiente, Resultado obtido, Status, Evidência, Responsável e data)

**Checkpoint**: P01 com status real e evidência; o critério "upload → … → submissão" está registrado.

---

## Phase 4: User Story 2 — Os 16 cenários e o mínimo de 5 evidências (Priority: P1)

**Goal**: ao menos um caso executado por cenário, cada um com pasta individual; casos executáveis restantes também.

**Independent Test**: as pastas existem, o plano reflete os status, cada cenário tem caso executado ou motivo.

- [ ] T015 [P] [US2] T01 (Autenticação) — API: T01-01, T01-02, T01-03, T01-04, T01-05 (TTL 1 min via `SESSION_TTL_MINUTES=1`, restaurar depois); automático: T01-06 (teste de não vazamento de senha/hash/token); uma pasta por caso em `docs/evidencias/T01-xx/`
- [ ] T016 [P] [US2] T02 (Isolamento) — contas A e B: T02-01 a T02-05 (404 idêntico para recurso alheio e inexistente) e T02-06 (matriz automatizada, com comando e saída)
- [ ] T017 [P] [US2] T04 (Rascunho) — T04-01, T04-02, T04-03, T04-05 em API; T04-04 por teste automatizado (troca de serviço)
- [ ] T018 [US2] T05 (Upload e extração válidos) — T05-01, T05-02, T05-04, T05-05, T05-06 com `samples/` do conjunto 01; T05-03 (JPEG) por teste automatizado; registrar `id` do pedido e SHA-256 quando o caso pedir
- [ ] T019 [US2] T06 (Documentos não aceitos) — T06-01, T06-02, T06-04..T06-07 em API; T06-08 por teste automatizado; T06-03 (PDF protegido) só após T033
- [ ] T020 [US2] T07 (Documento completo), T08 (Ausentes) e T09 (Condicionais) — T07-01..T07-03, T08-01..T08-03, T09-01..T09-04 com os conjuntos 01–04 e 07–10 conforme o caso; comparar com cada `gabarito.md`
- [ ] T021 [US2] T10 (Leitura não confiável) — T10-01..T10-05 com conjuntos 05, 06, 11 e 15; registrar que a instrução dentro do documento (conjunto 11) é dado e não é obedecida
- [ ] T022 [US2] T11 (Reenvio e revisão) — T11-01..T11-04 em API com conjuntos 04→13; T11-05 por teste de concorrência (dublê/automático)
- [ ] T023 [P] [US2] T12 (Falhas e limites da IA) — T12-01, T12-02, T12-04, T12-05 por dublê (comandos e saída dos testes, frase `Dublê (não é integração real)`); T12-03 em API
- [ ] T024 [US2] T13 (Submissão simulada) — T13-01..T13-03, T13-05, T13-06 em API; T13-04 e T13-07 por teste automatizado
- [ ] T025 [P] [US2] T14 (Exclusão permitida e bloqueada) — T14-01..T14-06 com `X-Confirmar-Exclusao: EXCLUIR`; usar T14-01 só em pedido de teste próprio
- [ ] T026 [US2] T15 (Conectividade e sessão) — T15-01, T15-02, T15-03, T15-05 em API; T15-04 (o plano o classifica como "API em memória (sem banco)", mas o procedimento 5.5 manda subir a API sem `DB_*`; executar como o procedimento e registrar a divergência de classificação como achado menor) com apoio do responsável; restaurar ao fim
- [ ] T027 [US2] T16 (Sensores) — subir `java simulador-iot/SimuladorSensores.java --sessao N --sequencia normal|fora-da-faixa|recuperacao [--seco]`; executar T16-01..T16-05, T16-07, T16-08 em API e T16-09 automático; T16-04 espera mais de 60 s
- [ ] T028 [US2] Casos "App no emulador + API real": T01-07, T01-08, T15-06..T15-09 (e a repetição opcional do P01 no app) com `adb`, JDK 21 do Android Studio só na sessão, URL `http://10.0.2.2:8080`; capturas em `bruto/`; registrar os textos literais observados em `Textos.kt` e o texto literal da issue (plano, seção 10); corte de rede com `adb shell svc wifi disable`/`data disable` e restauração
- [ ] T029 [US2] Garantir o mínimo da issue: conferir ao fim que existem ≥ 5 pastas de evidência completas e que cada cenário T01..T16 tem um caso executado ou motivo registrado; listar lacunas em `docs/testes/resultado-da-rodada.md`

**Checkpoint**: mínimo de 5 evidências e 16 cenários cobertos ou justificados.

---

## Phase 5: User Story 3 — Bloqueios e impedimentos conhecidos (Priority: P2)

**Goal**: nenhum Bloqueado sem tentativa; nenhum impedimento escondido.

**Independent Test**: cada Bloqueado tem tentativa e motivo na pasta.

- [ ] T030 [US3] T03 (Administrador): tentar T03-01 e T03-02 (rotas de indicadores, RC-20); se 404/inexistentes, registrar a resposta bruta e marcar Bloqueado com "RC-20"; executar T03-03 e T03-04 normalmente
- [ ] T031 [US3] T16-06 (alertas na interface, RC-22): procurar a tela e tentar a sequência no app; se ausente, registrar captura e busca e marcar Bloqueado com "RC-22"
- [ ] T032 [US3] Para cada caso que não pôde ser executado por ambiente (sem Oracle, sem emulador, sem `adb`): registrar a tentativa (resposta 503, erro do `adb`) em `docs/evidencias/<ID>/` e marcar Bloqueado com motivo; não marcar sem tentar
- [ ] T033 [US3] Gerar o PDF protegido (T06-03) — **executar antes de T019**, na Fase 2 (ordem lógica; mantido aqui por ser da US3) e uma amostra JPEG, se possível, com nome novo **fora** de `samples/` existentes (nunca sobrescrever), usando a biblioteca de PDF que o projeto já tem (ex.: a usada pelo código/testes de leitura de PDF) ou, se não houver forma de criptografar o arquivo com o que já existe, sem instalar ferramenta global; registrar método e SHA-256 em `docs/evidencias/T06-03/registro.md`; se não for possível, Bloqueado com motivo (D8)

**Checkpoint**: todo status Bloqueado é auditável.

---

## Phase 6: User Story 4 — Corrigir bloqueadores e repetir só o afetado (Priority: P2)

**Goal**: defeitos bloqueadores corrigidos com teste; reteste focado e P01 repetido.

**Independent Test**: cada achado bloqueador mostra falha original, correção, retestes e P01 repetido.

- [ ] T034 [US4] Classificar cada divergência em achado `A-nn` (bloqueador | não bloqueador) em `docs/testes/resultado-da-rodada.md` com esperado × obtido e evidência
- [ ] T035 [US4] Para cada achado **bloqueador**: atualizar spec/plano/tarefas desta feature se mudar requisito; corrigir no código existente (padrão do projeto, JavaDoc, teste de regressão em `backend/src/test/java/…` ou `mobile/app/src/test/…`); documentar a correção
- [ ] T036 [US4] Repetir somente os casos afetados em nova execução (`Execução 2` em `docs/evidencias/<ID>/registro.md`, falha original preservada) e atualizar o plano
- [ ] T037 [US4] Repetir o P01 após qualquer correção de bloqueador (nova execução em `docs/evidencias/P01/registro.md`); se não houve correção, registrar "sem bloqueador corrigido"
- [ ] T038 [US4] Registrar achados não bloqueadores como pendentes no resumo, sem correção forçada

**Checkpoint**: nenhum bloqueador aberto sem registro.

---

## Phase 7: User Story 5 — Consolidar a rodada (Priority: P3)

**Goal**: resumo auditável e índices atualizados.

**Independent Test**: contagens do resumo coincidem com o plano e somam 94.

- [ ] T039 [US5] Preencher `docs/testes/resultado-da-rodada.md`: versão, ambiente, contagens por status (soma 94), tabela caso → status → evidência, achados, correções e retestes, lacunas da seção 9.5 do plano (o que mudou), pendências (revisão por outro integrante **pendente**)
- [ ] T040 [US5] Atualizar a seção 2 e a seção 10 de `docs/testes/plano-de-testes.md` (commit/versão testável preenchidos, "execução: concluída/parcial", revisão por outro integrante pendente) sem alterar Resultado esperado
- [ ] T041 [P] [US5] Atualizar `docs/testes/README.md` (link para `resultado-da-rodada.md`, status da rodada) e `docs/evidencias/README.md` (índice das pastas da rodada, regra de mascaramento)

**Checkpoint**: o resumo bate com o plano e com as pastas.

---

## Phase 8: Polish e verificação transversal

- [ ] T042 [P] Buscar segredos nas evidências e no plano com `grep` simples: `Bearer [A-Za-z0-9._-]{20,}`, `DB_PASSWORD=`, a senha de demonstração (de `database/scripts/README.md`), a chave do simulador; corrigir qualquer ocorrência antes de qualquer commit
- [ ] T043 [P] Conferir invariantes de `data-model.md` por contagem: soma dos status = 94; todo Aprovado tem `registro.md`; todo Bloqueado tem tentativa; todo Reprovado tem achado; "extração real" e "resumo real" não aparecem; nenhum Aprovado se apoia só em evidência de RC-18/RC-19 ou de outra data (FR-015)
- [ ] T044 Restaurar o ambiente (TTL padrão, rede do emulador, API com `DB_*`), sem limpeza destrutiva nem reaplicar `02_carga_sintetica.sql`; anotar no resumo
- [ ] T045 Autoanálise final contra `spec.md` (FR-001..FR-015, SC-001..SC-007) e relatório ao responsável: o que foi executado, aprovado, reprovado, bloqueado, pendente; aguardar autorização para commit, push, PR, comentário e fechamento da #51

---

## Dependencies & Execution Order

- **Fase 1 → Fase 2 → US1/US2**: T007 (API de pé ou Bloqueada com tentativa) libera as execuções.
- **US1 (P01)** primeiro: sustenta o objetivo da issue; T014 antes de US4.
- **US2** pode ser dividida por pessoa: T015–T017, T023, T025 são independentes entre si ([P]); T018→T019→T020→T021 compartilham o fluxo upload/leitura/análise e rodam em sequência; T028 depende de T026 e do emulador; T027 depende de `IOT_API_KEY`.
- **US3** convive com US2: T030, T031 e T033 são tentadas durante US2; T033 precede T019 (T06-03).
- **US4** só começa quando US1 e US2 produziram achados; T037 por último.
- **US5** depende de US2–US4; Polish por último.

## Parallel Examples

- US2: T015, T016, T017, T023 e T025 podem rodar juntos (contas A/B e pedidos novos separados).
- Setup: T004 e T008/T009 em paralelo com T003.
- Fim: T041, T042, T043 em paralelo.

## Implementation Strategy

- **MVP**: Fase 1 + Fase 2 + US1 (P01 com evidência) + o mínimo de 5 pastas de US2 (T015, T016, T017 e dois de T018/T019).
- **Incremental**: ampliar US2 cenário a cenário; fechar Bloqueios (US3); corrigir bloqueadores (US4); consolidar (US5).
- **Honestidade**: se o ambiente não permitir a API real, entregar o que for possível (testes automatizados e dublês marcados como tais), registrar as tentativas e deixar os demais "Bloqueado" ou "Não executado". Nada é aprovado sem execução.

## Notes

- Total: 45 tarefas.
- Cada marcação [X] só vale com a evidência gravada e o plano atualizado.
- Tarefas que dependem do responsável (T006, T026 em T15-04, T028 se o emulador não abrir) devem registrar o pedido e a resposta.
