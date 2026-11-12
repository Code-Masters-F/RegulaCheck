# Tasks: Modelo de banco e scripts SQL (RC-03)

**Input**: `specs/003-modelo-banco-sql/` (spec.md, plan.md, research.md, data-model.md, contracts/scripts-sql.md, quickstart.md)

**Backlog**: RC-03 — Code-Masters-F/ClickBus#8 · **Branch**: `003-modelo-banco-sql`

**Testes**: não há testes de código nesta história (sem Java). A verificação é a **execução real** dos scripts no Oracle da faculdade (usuário individual, nunca versionado), com saída registrada. Credenciais só no `.env` (ignorado pelo Git).

**Regras de segurança desta execução**: só objetos `RC_*`; `PEQUENOVAREJO` e `VENDA` nunca são tocadas; o `90_` só roda com confirmação explícita; commit/push só quando o usuário pedir.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: história da spec (US1 modelo, US2 criação, US3 carga, US4 consultas, US5 segurança e verificação)

---

## Phase 1: Setup

- [x] T001 Criar as pastas `database/scripts/` e o diretório de trabalho temporário no scratchpad (fora do repositório) para o executor de scripts; confirmar que `.env` está no `.gitignore`
- [x] T002 Criar no scratchpad (fora do repositório) o executor temporário em Java que lê `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` do `.env`, separa instruções por `;` e blocos PL/SQL por `/` e registra a saída em arquivo (sem imprimir credenciais)
- [x] T003 Executar um `SELECT table_name FROM user_tables` e registrar o estado inicial (apenas tabelas de outros trabalhos, nenhuma `RC_*`) e a contagem de linhas de `PEQUENOVAREJO` e `VENDA` (somente `SELECT COUNT(*)`) para comparação posterior

---

## Phase 2: Foundational (bloqueia as demais)

- [x] T004 Gerar no scratchpad (fora do repositório), com `at.favre.lib:bcrypt` 0.10.2 custo 12, os três hashes BCrypt (`$2a$12$...`) da senha de demonstração fictícia; guardar os hashes para a carga ; a senha de demonstração é fictícia, nunca reutiliza senha real de ninguém e aparece em texto apenas no `database/scripts/README.md` (T020); os scripts SQL contêm somente os hashes

**Checkpoint**: executor funcional e hashes disponíveis.

---

## Phase 3: US1 — Modelo relacional documentado (P1)

**Meta**: diagrama ER e dicionário publicados e coerentes com o protocolo e os estados.

- [x] T005 [P] [US1] Criar `docs/modelo-de-dados.md` a partir de `specs/003-modelo-banco-sql/data-model.md` (diagrama Mermaid, dicionário, cardinalidades, o que o banco garante e o que fica para a API), com referências relativas válidas a `docs/protocolo-academico.md` e `docs/api/estados-e-revisoes.md`
- [x] T006 [US1] Conferir nomes de estados, resultados e atributos de `docs/modelo-de-dados.md` contra `docs/protocolo-academico.md`, `docs/api/estados-e-revisoes.md` e `docs/api/contratos-dados.md`; corrigir a origem em caso de divergência (FR-001 a FR-004)

---

## Phase 4: US2 — Script de criação do esquema (P1)

**Meta**: 12 tabelas `RC_*` com restrições, criadas em esquema sem objetos `RC_`.

- [x] T007 [US2] Escrever `database/scripts/01_criar_esquema.sql` com cabeçalho (finalidade, ordem, efeito, pré-requisitos) e as 12 tabelas `RC_*` na ordem de dependência: `RC_USUARIO`, `RC_PROTOCOLO`, `RC_REGRA`, `RC_SOLICITACAO`, `RC_REVISAO`, `RC_DOCUMENTO`, `RC_ANALISE`, `RC_RESULTADO_REGRA`, `RC_PENDENCIA`, `RC_SESSAO_CAPTURA`, `RC_LEITURA_SENSOR`, `RC_SUBMISSAO`, com identity, PK, FK (inclusive compostas), UK, CHECKs de valores permitidos e nomes `RC_<TABELA>_PK/_FK_/_UK/_CK_`; em `RC_RESULTADO_REGRA` incluir a UK (`ANALISE_ID`, `REGRA_ID`, `RESULTADO`) e em `RC_PENDENCIA` a FK composta (`ANALISE_ID`, `REGRA_ID`, `TIPO`) para ela, de modo que o tipo da pendência coincida com o resultado
- [x] T008 [US2] No mesmo arquivo, adicionar o índice único funcional `RC_ANALISE_VIGENTE_UK` (uma análise vigente por pedido) e os índices de FK usados pelas consultas; garantir identificadores com no máximo 30 caracteres e nenhum `DROP`, `TRUNCATE` ou `DELETE`
- [x] T009 [US2] Executar `01_criar_esquema.sql` no Oracle e conferir 12 tabelas `RC_*` em `user_tables`, restrições em `user_constraints` e o índice em `user_indexes`; registrar a saída real

---

## Phase 5: US3 — Carga sintética (P1)

**Meta**: contas, protocolo, regras e exemplos suficientes para as consultas.

- [x] T010 [US3] Escrever `database/scripts/02_carga_sintetica.sql` com cabeçalho, como **um único bloco PL/SQL** (variáveis guardam os IDs gerados por identity via `RETURNING ... INTO`; `COMMIT` só no fim; qualquer erro propaga e desfaz tudo; nada é apagado): 3 contas (`paciente.a@exemplo.invalid`, `paciente.b@exemplo.invalid`, `admin@exemplo.invalid`) com os hashes de T004, protocolo `PROTOCOLO-ACADEMICO-FICTICIO` v0.1 e regras RC-R01 a RC-R06 com os atributos de `docs/protocolo-academico.md`
- [x] T011 [US3] No mesmo arquivo, adicionar os dados de exemplo: ~4 solicitações em estados e revisões diferentes (incluindo um pedido corrigido entre revisões 1 e 2 e um submetido), revisões, documentos (com `REVISAO_REMOCAO` em um caso), análises (uma vigente por pedido), resultados por regra, pendências (inclusive `NAO_VERIFICAVEL`), uma sessão de captura com leituras dos dois sensores e uma submissão simulada; terminar com `COMMIT` dentro do mesmo bloco
- [x] T012 [US3] Executar `02_carga_sintetica.sql` e conferir as contagens por tabela (RC_USUARIO 3, RC_PROTOCOLO 1, RC_REGRA 6 e demais conforme `contracts/scripts-sql.md`); verificar que `SENHA_HASH` começa com `$2a$12$` e que não há senha em texto puro; fixar os valores definitivos de contagem

---

## Phase 6: US4 — Consultas de uso (P1)

**Meta**: 6 consultas com junção e agregação, com resultado esperado documentado.

- [x] T013 [US4] Escrever `database/scripts/03_consultas_uso.sql` com cabeçalho e as 6 consultas de `contracts/scripts-sql.md` (pedidos por paciente, análises por pedido, pedidos por estado, pendências por regra, pedidos corrigidos entre revisões com `NOT EXISTS`, resultado por regra da análise vigente), cada uma com comentário de finalidade e resultado esperado; somente `SELECT`
- [x] T014 [US4] Executar `03_consultas_uso.sql`, conferir que cada consulta retorna resultado não vazio coerente com a carga, registrar a saída real e anotar os valores esperados definitivos para o `database/scripts/README.md` (T020)

---

## Phase 7: US5 — Restrições, limpeza segura e verificação (P2)

**Meta**: provar as restrições, a reprodutibilidade e a segurança do banco compartilhado.

- [x] T015 [US5] Escrever `database/scripts/04_testes_restricoes.sql` com blocos PL/SQL que tentam as 12 violações de `contracts/scripts-sql.md` (perfil, estado, resultado de regra, e-mail duplicado, código de regra duplicado, referência inexistente, segunda análise vigente, segunda submissão, documento acima de 5 MB, unidade incoerente com o sensor, pendência sem resultado, pendência de tipo diferente do resultado), capturam o código ORA esperado e imprimem `OK` ou `FALHOU`; terminar com `ROLLBACK`
- [x] T016 [US5] Executar `04_testes_restricoes.sql` e conferir 12 casos `OK`, nenhum `FALHOU` e nenhuma linha residual nas tabelas
- [x] T017 [US5] Escrever `database/scripts/90_limpeza_DESTRUTIVA.sql` com cabeçalho de aviso, guarda de confirmação (só executa com variável de confirmação `SIM`), remoção **somente** de objetos `RC_*` em ordem inversa de dependência, e nenhuma referência a outros objetos; garantir que nenhum outro script o chame
- [x] T018 [US5] Executar a segunda execução proposital de `01_` e `02_` (devem falhar com ORA-00955 e ORA-00001 sem alterar dados) e registrar o resultado (SC-006); conferir por contagem de linhas de todas as tabelas `RC_*` antes e depois que nada foi duplicado
- [x] T019 [US5] **Pedir confirmação explícita ao usuário** antes de executar o `90_` (operação destrutiva, CLAUDE.md §2); só então executar `90_` com a variável de confirmação, depois refazer `01_` → `02_` para provar a reprodutibilidade; conferir antes e depois que `PEQUENOVAREJO` e `VENDA` continuam intactas (mesmas tabelas e contagem de linhas)

---

## Phase 8: Documentação e fechamento

- [x] T020 [P] Criar `database/scripts/README.md` com finalidade, ordem, pré-requisitos e efeito de cada script, marcação do `90_` como destrutivo, algoritmo BCrypt custo 12, como o hash foi gerado, senha de demonstração fictícia e resultados esperados das consultas; depois de pronto, reler só o README e conferir que ordem e itens destrutivos são identificáveis em menos de 5 minutos (SC-007)
- [x] T021 [P] Atualizar `database/README.md` com o registro de verificação (data, ambiente Oracle 19c, scripts executados e resultado real; o que não foi executado marcado como "não verificado")
- [x] T022 [P] Atualizar `docs/arquitetura.md` (referência ao modelo de dados) e `docs/decisoes.md` (D1 a D12 resumidos, prefixo `RC_`, BCrypt custo 12, biblioteca candidata da RC-04)
- [x] T023 Varredura de segredos nos arquivos versionados (usuário e senha do banco, token do GitHub, senha em texto puro) em `database/`, `docs/`, `specs/` e `.env.example`; confirmar que `.env` não está no `git status`
- [x] T024 Revisar o diff, atualizar `quickstart.md` se algo mudou, marcar as tarefas concluídas e preencher o relatório final com RC-03 → FR/SC → arquivos → verificações; checklist `checklists/requirements.md` conforme o estado real

---

## Dependências

- Setup (T001-T003) → Foundational (T004)
- US1 (T005-T006) é independente do banco e pode andar em paralelo com US2
- Esquema e carga em sequência: T007 → T008 → T009 → T010 → T011 → T012 → T013 → T014 → T015 → T016 → T018
- T017 (escrever o `90_`) pode ser feita a qualquer momento depois de T007; sua **execução** (T019) vem por último, depois de T018 e da confirmação do usuário
- T020-T022 dependem dos resultados reais (T014, T016, T019); T023 e T024 por último

## Cobertura

| Requisito | Tarefas |
|---|---|
| FR-001 a FR-005 | T005, T006, T007 |
| FR-006, FR-007 | T007, T008, T015, T016 |
| FR-008, FR-008a, FR-009 | T007, T008, T020 |
| FR-010, FR-011 | T004, T010-T012, T020 |
| FR-012 | T013, T014 |
| FR-013 | T017, T019 |
| FR-014 | T010, T023 |
| FR-015 | T021, T022 |
| FR-016 | nenhuma tarefa de código (verificado em T024) |
| SC-001 a SC-008 | T006, T009, T016, T012, T014, T018, T020 |

## Estratégia

MVP: US1 + US2 + US3 (modelo, esquema e carga). Depois US4 e US5. Cada fase termina com execução real e registro; falhas são corrigidas na origem e reexecutadas, sem editar o resultado registrado.
