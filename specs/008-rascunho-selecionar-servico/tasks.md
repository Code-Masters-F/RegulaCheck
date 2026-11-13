# Tasks: Criar rascunho e selecionar serviço (RC-07)

**Input**: `specs/008-rascunho-selecionar-servico/` (spec.md, plan.md, research.md, data-model.md, contracts/api-solicitacoes.md, quickstart.md)

**Backlog**: RC-07 — Code-Masters-F/ClickBus#14 · **Branch**: `008-rascunho-selecionar-servico`

**Testes**: solicitados pelo `CLAUDE.md` §14 (autorização entre contas, revisões, concorrência) e pelo plano. Em cada história o teste é escrito **antes** da implementação e deve falhar primeiro. Tudo roda sem banco no `.\mvnw.cmd clean verify`; o único teste contra o Oracle é opt-in (T037) e, sem execução real, é relatado como **NÃO VERIFICADO**.

**Regras desta execução**: nada de commit/push sem pedido; nenhum comando é executado no Oracle da faculdade pelo agente (o script 06 é executado pelo grupo); só dados sintéticos; sem tela, documento, extração, análise, exclusão de rascunho nem paginação rica; nenhuma dependência nova no `pom.xml`.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 serviços e criar rascunho · US2 trocar o serviço do rascunho · US3 listar e reabrir meus pedidos · US4 entender o erro da entrada
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Conferir `git status` na branch `008-rascunho-selecionar-servico`, rodar `.\mvnw.cmd clean verify` em `backend/` **antes** de qualquer alteração e registrar o resultado-base (quantos testes passam e quantos Oracle são ignorados)
- [x] T002 [P] `database/scripts/06_motivo_revisao_protocolo.sql` (novo, aditivo): `DROP CONSTRAINT RC_REVISAO_CK_MOTIVO` + `ADD CONSTRAINT` com os 6 motivos (inclui `PROTOCOLO_ALTERADO`); cabeçalho no padrão do `05_` (finalidade, ordem: depois do `01_`, efeito aditivo, quando **não** rodar: banco criado já com o `01_` novo, banco compartilhado, prefixo `RC_`). Conferir que `database/scripts/04_testes_restricoes.sql` (linha 109 usa `CONFERENCIA_ALTERADA`) continua válido e acrescentar nele um caso que aceita `PROTOCOLO_ALTERADO` e outro que recusa um motivo inventado (com `ROLLBACK`, como os demais; executado só pelo grupo). Mesma lista de motivos em `database/scripts/01_criar_esquema.sql` (instalação nova) e linha do `06_` na tabela de ordem de `database/scripts/README.md` com o aviso de que sem ele a troca de serviço falha só ao registrar a revisão (FR-010, research D9)

---

## Phase 2: Foundational (bloqueia as histórias)

**Meta**: tipos, contratos de repositório, implementações JDBC, dobros em memória e a montagem das dependências, tudo compilando; nenhuma rota ainda.

- [x] T003 [P] `main/protocolo/entity/Protocolo.java` (record: `id`, `identificador`, `versao`, `localidade`, `especialidade`, `avisoFicticio`; método `protocoloId()` = `identificador + "-" + versao`) e `main/solicitacao/entity/{EstadoSolicitacao,MotivoRevisao,Solicitacao}.java` (enums fechados com os 7 estados e os 6 motivos de `RC_SOLICITACAO_CK_ESTADO`/`RC_REVISAO_CK_MOTIVO`; `Solicitacao` record com `id`, `pacienteId`, `protocolo`, `estado`, `revisaoAtual`, `criadaEm`, `atualizadaEm`), com JavaDoc em português; `test/solicitacao/EntidadesTest.java` escrito antes (`protocoloId()` monta `IDENTIFICADOR-VERSAO`; enums iguais à restrição do banco)
- [x] T004 [P] `main/protocolo/repository/ProtocoloRepository.java` (`listarAtivos()`, `buscarAtivoPorProtocoloId(String)`) e `main/solicitacao/repository/{SolicitacaoRepository,ResultadoTroca}.java`: `criar(pacienteId, protocoloId, agora)`, `buscarDaPaciente(id, pacienteId)`, `listarDaPaciente(pacienteId, limite)`, `trocarProtocolo(id, pacienteId, novoProtocoloId, agora)`; `ResultadoTroca` com `Situacao` (`NAO_ENCONTRADA`, `FORA_DE_RASCUNHO`, `SEM_MUDANCA`, `ALTERADA`) e a `Solicitacao` quando existe; JavaDoc descrevendo atomicidade, filtro pela dona e `ExcecaoNegocio` 503 quando o banco está fora
- [x] T005 [P] `main/protocolo/repository/ProtocoloRepositoryJdbc.java`: `SELECT` parametrizado só de `ATIVO = 1`, `buscarAtivoPorProtocoloId` com `IDENTIFICADOR || '-' || VERSAO = ?`, instantes em UTC, falha de SQL → 503 `DEPENDENCIA_INDISPONIVEL` sem texto do driver (mesmo padrão de `SessaoRepositoryJdbc`)
- [x] T006 [P] `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java`: `criar` (INSERT em `RC_SOLICITACAO` recuperando `ID` + INSERT `RC_REVISAO(ID, 1, 'CRIACAO')`, transação com `COMMIT`/`ROLLBACK`); `buscarDaPaciente` e `listarDaPaciente` com junção a `RC_PROTOCOLO` e `s.PACIENTE_ID = ?` **na própria instrução** (`ORDER BY s.CRIADA_EM DESC, s.ID DESC FETCH FIRST ? ROWS ONLY`); `trocarProtocolo` com `SELECT … FOR UPDATE` (só colunas de `RC_SOLICITACAO`, filtro por `ID` e `PACIENTE_ID`), decisão entre as 4 situações, `UPDATE` de `PROTOCOLO_ID`/`REVISAO_ATUAL = REVISAO_ATUAL + 1`/`ATUALIZADA_EM` e INSERT em `RC_REVISAO` com `PROTOCOLO_ALTERADO` na mesma transação (research D5, D6). Nenhuma concatenação de entrada em SQL; logs só com ids
- [x] T007 `main/config/Dependencias.java`, `main/config/Servidor.java` e os 3 pontos de teste que constroem `Dependencias` (`test/auth/CenarioAuth.java`, `test/common/http/CabecalhosSegurosTest.java`, `test/seguranca/HigieneDeLogGeralTest.java`): `Dependencias` ganha `protocolos` e `solicitacoes`; `Servidor.criar(Configuracao)` monta os dois repositórios JDBC; os testes passam os dobros em memória. Depende de T004, T005, T006 e T008
- [x] T008 [P] `test/protocolo/ProtocoloRepositoryMemoria.java` e `test/solicitacao/SolicitacaoRepositoryMemoria.java` (dobros de teste): um protocolo ativo sintético (`PROTOCOLO-ACADEMICO-FICTICIO-v0.1`) e um segundo ativo e um inativo para os cenários; o repositório de pedidos replica as **mesmas** regras do JDBC (filtro pela dona, 4 situações de `trocarProtocolo`, revisão +1 e histórico de revisões consultável pelo teste) com `synchronized` por pedido; `ArquiteturaTest` existente continua verde (sem Spring/JPA)

**Checkpoint**: `.\mvnw.cmd -q test-compile` compila produção e testes; nenhuma rota nova ainda.

---

## Phase 3: US1 — Ver os serviços e criar o rascunho (P1) 🎯 MVP

**Meta**: a paciente autenticada vê o serviço, cria o rascunho e o reabre pelo `Location`. Atende FR-001 a FR-008, FR-020 e FR-022.

**Teste independente**: login como paciente sintética → `GET /protocolos` → `POST /solicitacoes` → `GET /solicitacoes/{id}`; conferir dono, serviço, `RASCUNHO`, revisão 1, `simulacao: true`; administrador e anônimo recusados.

### Testes (escritos antes e vistos falhar)

- [x] T009 [P] [US1] `test/protocolo/ProtocoloServiceTest.java` e `test/protocolo/ProtocoloEndpointTest.java`: catálogo só com ativos, lista vazia = 200 `{ "itens": [] }`, campos exatos do contrato, aviso fictício e `simulacao: true` em todo item, 401 sem token, 403 para administrador, 405 para `POST`
- [x] T010 [P] [US1] `test/solicitacao/service/CriarRascunhoServiceTest.java`: dono = paciente da sessão, `RASCUNHO`, revisão 1, revisão inicial com motivo `CRIACAO`, protocolo inexistente/inativo → `ExcecaoNegocio` 422 `REGRA_DE_NEGOCIO` com campo `protocoloId` e nada criado (inclui o cenário em que **nenhum** protocolo está ativo, na criação e na troca), duas criações iguais geram dois pedidos
- [x] T011 [P] [US1] `test/solicitacao/CriarRascunhoEndpointTest.java`: 201 com `Location`, corpo conforme `contracts/api-solicitacoes.md`; corpo com `pacienteId`, `estado` e `revisao` forjados é ignorado (dono e estado vêm da sessão e do sistema); 401 sem token; 403 para administrador; 403 para a chave do simulador; `GET` do `Location` devolve os mesmos dados (persistência)

### Implementação

- [x] T012 [P] [US1] `main/protocolo/service/ProtocoloService.java` (`listarAtivos`, `exigirAtivo(String protocoloId)` → `ExcecaoNegocio` 422 com detalhe `protocoloId` e mensagem "Escolha um dos serviços da lista.") e DTOs `main/protocolo/dto/{ProtocoloResponse,ListaProtocolosResponse}.java` (mapeamento explícito de `Protocolo`, `simulacao` sempre `true`, sem `id` interno)
- [x] T013 [P] [US1] `main/protocolo/controller/ProtocoloController.java`: `GET /protocolos`, só `GET`, `ServletJson`; sem lógica de negócio
- [x] T014 [US1] `main/solicitacao/service/SolicitacaoService.java` (`criar(SessaoAutenticada, String protocoloId)`: resolve o protocolo por `ProtocoloService.exigirAtivo`, usa o `Clock` de `Dependencias`, devolve `Solicitacao`; `buscarPropria(SessaoAutenticada, long id)`: `buscarDaPaciente` + `Propriedade.exigir`; log só `usuarioId` e `solicitacaoId`) e DTOs `main/solicitacao/dto/{SolicitacaoRequest,SolicitacaoResponse}.java` (`SolicitacaoRequest` com `protocoloId` apenas; `SolicitacaoResponse.de(Solicitacao)` com `id` string, `servico`, `estado`, `revisao`, `criadaEm`, `atualizadaEm`, `simulacao`). Depende de T012
- [x] T015 [US1] `main/solicitacao/controller/SolicitacaoController.java`: roteia `/solicitacoes` e `/solicitacoes/{id}` por `getPathInfo()` (trata `null` e `/` como a coleção; id fora de `^[1-9][0-9]{0,17}$` ou subcaminho extra → 404 padrão sem consultar o banco, via `Erros.naoEncontrado()`); `doPost` lê o corpo com `CorpoJson.ler`, chama o Service e responde 201 com `Location`; `doGet` em `/solicitacoes/{id}` reabre o pedido próprio (200), enquanto a lista `GET /solicitacoes` e o `PUT` entram nas fases seguintes; usa `AutenticacaoFiltro.sessaoDe(requisicao)` para a paciente. Depende de T014
- [x] T016 [US1] `main/config/Composicao.java`: montar `ProtocoloService`/`SolicitacaoService` com `dependencias` e registrar por `RotasPrivadas` `/protocolos` e `/solicitacoes/*` com `Perfil.PACIENTE` (antes da rota coringa); conferir que `exigirTudoDeclarado()` continua passando. Depende de T013, T015

**Checkpoint**: T009–T011 verdes; é possível criar e reabrir um rascunho de ponta a ponta com os dobros em memória.

---

## Phase 4: US2 — Trocar o serviço enquanto é rascunho (P1)

**Meta**: troca com revisão +1 e motivo registrado, sem mudar nada quando o serviço é o mesmo, 409 fora de rascunho, concorrência coerente. Atende FR-009 a FR-013.

**Teste independente**: criar rascunho → `PUT` com outro serviço ativo (revisão 2) → `PUT` repetido com o mesmo serviço (revisão 2 e `atualizadaEm` iguais) → mudar o estado do pedido no dobro e tentar `PUT` (409).

### Testes (antes)

- [x] T017 [P] [US2] `test/solicitacao/service/TrocarServicoServiceTest.java`: serviço diferente → revisão +1, histórico ganha `PROTOCOLO_ALTERADO`, `atualizadaEm` = relógio fixo; mesmo serviço → nada muda; estado ≠ `RASCUNHO` → 409 `CONFLITO_ESTADO` e nada muda; pedido alheio/inexistente → 404; protocolo inativo → 422 sem alterar o pedido
- [x] T018 [P] [US2] `test/solicitacao/TrocarServicoEndpointTest.java`: respostas HTTP conforme o contrato (200/404/409/422), 404 do pedido **antes** do 400 do corpo (corpo inválido em pedido alheio devolve 404, nunca 400), 401/403, 405 para métodos não previstos
- [x] T019 [P] [US2] `test/solicitacao/ConcorrenciaTrocaServicoTest.java`: N threads trocam o serviço do mesmo rascunho (alternando entre dois ativos) e outro grupo troca o estado para fora de rascunho no meio; ao final, revisões sem salto nem repetição, número de linhas de revisão = `revisaoAtual`, nenhuma troca aplicada após a saída de rascunho (FR-012, SC-005)

### Implementação

- [x] T020 [US2] `main/solicitacao/service/SolicitacaoService.java`: `trocarProtocolo(SessaoAutenticada, long id, String protocoloId)` (resolve o protocolo, chama `trocarProtocolo` do repositório e traduz `ResultadoTroca`: `NAO_ENCONTRADA` → 404, `FORA_DE_RASCUNHO` → 409 `CONFLITO_ESTADO` "Este pedido não pode mais ser alterado.", `SEM_MUDANCA`/`ALTERADA` → pedido); JavaDoc do efeito, da revisão e da concorrência
- [x] T021 [US2] `main/solicitacao/controller/SolicitacaoController.java`: `doPut` em `/solicitacoes/{id}` — primeiro `servico.buscarPropria` (404; criado na T014), depois `CorpoJson.ler` + `SolicitacaoRequest.validar()` (400/415), depois `trocarProtocolo`; resposta 200 com `SolicitacaoResponse`. Depende de T020

**Checkpoint**: T017–T019 verdes; `PUT` completo com os dobros em memória.

---

## Phase 5: US3 — Listar e reabrir meus pedidos (P1)

**Meta**: a paciente vê só os pedidos dela, do mais recente ao mais antigo, e reabre um com os dados persistidos; pedido alheio indistinguível de inexistente. Atende FR-014 a FR-017.

**Teste independente**: duas pacientes com pedidos; cada uma lista só os seus; A abre o seu e recebe 404 idêntico ao tentar abrir o de B ou um id inexistente ou `abc`; nova sessão mostra os mesmos rascunhos.

### Testes (antes)

- [x] T022 [P] [US3] `test/solicitacao/service/ListarReabrirServiceTest.java`: filtro pela dona, ordem `criadaEm` desc com desempate por `id`, limite de 50, lista vazia, 404 para alheio/inexistente
- [x] T023 [P] [US3] `test/solicitacao/ListarReabrirEndpointTest.java`: `GET /solicitacoes` (`itens` + `limite: 50`), `GET /solicitacoes/{id}`; corpo do 404 **byte a byte igual** para alheio, inexistente, `abc`, `0`, negativo, `99999999999999999999` e `/solicitacoes/1/qualquer`; rascunhos reaparecem após novo login (FR-017, SC-006); 401/403/405
- [x] T024 [P] [US3] `test/solicitacao/MatrizPerfisSolicitacaoTest.java`: matriz perfil × rota (anônimo, paciente A, paciente B, administrador, simulador) para as 5 combinações método+rota, no padrão de `MatrizPacienteTest`/`MatrizAdministradorTest`; administrador recebe 403 também para id inexistente (SC-003)

### Implementação

- [x] T025 [US3] `main/solicitacao/service/SolicitacaoService.java`: `listar(SessaoAutenticada)` (limite constante `LIMITE_LISTA = 50`); `main/solicitacao/dto/ListaSolicitacoesResponse.java` (`itens`, `limite`)
- [x] T026 [US3] `main/solicitacao/controller/SolicitacaoController.java`: `doGet` em `/solicitacoes` (lista); o reabrir `/solicitacoes/{id}` já existe desde a T015 e é coberto também pelos testes desta fase; `Cache-Control` já vem do filtro de cabeçalhos seguros. Depende de T025

**Checkpoint**: T022–T024 verdes; percurso criar → trocar → listar → reabrir completo em memória.

---

## Phase 6: US4 — Entender o que está errado na entrada (P2)

**Meta**: erros de contrato distinguíveis dos de regra, com campo identificado, formato padrão e sem vazamento. Atende FR-018, FR-019, FR-021.

**Teste independente**: enviar ao `POST` e ao `PUT` serviço ausente, em branco, inexistente, inativo, número, objeto, lista, 101 caracteres, corpo vazio, JSON malformado, `Content-Type` errado e corpo maior que o limite.

### Testes (antes)

- [x] T027 [P] [US4] `test/solicitacao/ValidacaoEntradaSolicitacaoTest.java`: tabela de entradas inválidas (incluindo `protocoloId` numérico, booleano, objeto e lista → 400, não coerção para texto) × (status, código, `detalhes[0].campo`) conforme `contracts/api-solicitacoes.md`; 400 para contrato, 422 para serviço inexistente/inativo (mesma resposta para os dois), 415 para tipo de conteúdo; nenhum pedido criado/alterado em nenhum caso; nenhuma resposta contém `ORA-`, `SELECT`, `Exception`, caminho de arquivo ou o corpo recebido (FR-021, SC-004)
- [x] T028 [P] [US4] `test/solicitacao/BancoIndisponivelSolicitacaoTest.java`: repositório que lança `ExcecaoNegocio` 503 e outro que lança `RuntimeException`; respostas 503 `DEPENDENCIA_INDISPONIVEL` e 500 `ERRO_INTERNO` genéricas, sem SQL nem pilha (padrão de `CabecalhosSegurosTest`), pedido não aparece pela metade no dobro

### Implementação

- [x] T029 [US4] `main/solicitacao/dto/SolicitacaoRequest.java`: `validar()` acumulando detalhes por campo — `protocoloId` obrigatório, não em branco, ≤ 100 caracteres, **tipo texto estrito**. O Jackson do projeto aceita coerção de escalares (`123` e `true` virariam a string), então `SolicitacaoRequest` recebe `protocoloId` como `JsonNode` (ou equivalente) e exige `isTextual()`; número, booleano, objeto e lista dão 400 `REQUISICAO_INVALIDA` com campo `protocoloId`, sem alterar `common/Json.java` (compartilhado com as RC-04 a RC-06) — lançando 400 `REQUISICAO_INVALIDA` com mensagens em português que dizem o que fazer; campos desconhecidos continuam ignorados (nunca lidos). Garantir que `POST` e `PUT` chamam `validar()` antes do Service (ordem de `docs/api/erros.md`)

**Checkpoint**: T027–T028 verdes.

---

## Phase 7: Polish e documentação

- [x] T030 [P] `test/seguranca/HigieneDeLogGeralTest.java` e/ou `test/solicitacao/HigieneDeLogSolicitacaoTest.java`: criar/trocar/listar/falhar com e-mail, nome e corpo sentinela; nenhum log contém e-mail, nome, token nem o corpo; só ids técnicos (FR-022)
- [x] T031 [P] `test/seguranca/SqlParametrizadoTest.java`: confirmar que a varredura existente cobre os novos repositórios JDBC (sem concatenação de entrada em SQL; `FETCH FIRST ?` e filtros por `?`); ajustar a varredura somente se ela não enxergar os novos arquivos, nunca afrouxá-la
- [x] T032 [P] `CONTRATO_API.md`: adicionar `GET /protocolos`, marcar `GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}` como implementados (RC-07), manter `DELETE` e demais como planejados, e a linha de autorização de `/protocolos` (PACIENTE)
- [x] T033 [P] `docs/api/contratos-dados.md`, `docs/api/estados-e-revisoes.md`, `docs/api/erros.md`, `docs/api/README.md`: `id` string numérica opaca, `servico`, `PUT`, `GET /solicitacoes` com `itens`/`limite`, motivo `PROTOCOLO_ALTERADO` e regra "mesmo serviço não muda revisão", uso real do 422 `REGRA_DE_NEGOCIO` e do 409 `CONFLITO_ESTADO` neste fluxo, nota de que só `/auth/*`, `/protocolos` e `/solicitacoes` (rascunho) estão implementadas
- [x] T034 [P] `docs/api/openapi.yaml` e `docs/api/RegulaCheck.postman_collection.json`: rotas, esquemas e exemplos novos conforme `contracts/api-solicitacoes.md`; validar que o YAML continua legível (sintaxe) e que a coleção é JSON válido
- [x] T035 [P] `docs/modelo-de-dados.md`, `docs/decisoes.md`, `docs/arquitetura.md`, `README.md`/`docs/ambiente.md`: motivo `PROTOCOLO_ALTERADO` e script 06; decisões D1–D10 resumidas e a pendência "executar o `06_` no Oracle"; fluxo de rascunho na arquitetura; rotas e comandos de verificação (referenciar `quickstart.md` sem duplicar)
- [x] T036 [P] `docs/api/autorizacao.md`: trocar o exemplo hipotético de `SolicitacaoController` pela referência ao uso real (`/solicitacoes/*`, `Propriedade.exigir` com `buscarDaPaciente`) e registrar a decisão D10 sobre crescimento do ponto de entrada para as próximas histórias
- [x] T037 **(executado em 13/11/2026 contra o Oracle da faculdade, na conta do grupo: 1ª rodada 1 falha + 1 erro por falta do `06_`; após aplicá-lo, 3 testes, 0 falhas, 0 erros)** `test/solicitacao/SolicitacaoOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM` + `DB_*`, no padrão de `AuthOracleTest`): com scripts 01, 02, 05 e 06 aplicados, cria pedidos próprios com a conta sintética, confere `RC_REVISAO` (1 → `CRIACAO`; troca → `PROTOCOLO_ALTERADO`), dispara PUTs simultâneos e confere revisões sem salto nem repetição, isolamento entre as contas A e B e, ao final, remove **somente** os pedidos e revisões que criou. Sem a variável: ignorado e relatado como NÃO VERIFICADO
- [x] T038 Verificação final: `.\mvnw.cmd clean verify` em `backend/`; registrar comando, total de testes, falhas e quantos Oracle foram ignorados em `specs/008-rascunho-selecionar-servico/` (seção "Resultado da verificação" ao final deste arquivo) e, se existir convenção, em `docs/testes/`; conferir `git diff` sem segredos, sem `System.out`, sem código comentado e sem alterar arquivos de outras histórias além dos citados; confirmar o `quickstart.md` rodando as chamadas de exemplo contra a API em memória ou relatando que não foi possível
- [x] T039 Rastreabilidade e fechamento: preencher abaixo a tabela RC-07 → requisito → tarefas → arquivos → verificação; marcar as tarefas somente com evidência real; relatório final separando **implementado**, **verificado** (comando e resultado), **não verificado** (Oracle real, script 06 no banco da faculdade) e **fora do escopo** (tela, documentos, exclusão, paginação, mobile)

---

## Dependências e ordem

- Setup (T001, T002) → Foundational (T003–T008) → histórias. T007 depende de T004, T005, T006 e T008.
- **US1 (MVP)** depende só da Foundational. **US2** e **US3** dependem de US1 (compartilham `SolicitacaoService` e `SolicitacaoController`) mas não uma da outra; como editam os mesmos arquivos, executar em sequência. **US4** depende de US1 e valida também o que US2 entrega.
- Dentro de cada história: testes primeiro (vistos falhar) → Service → Controller → registro em `Composicao`.
- Polish depende de todas as histórias; T037 depende de o grupo ter aplicado o `06_` para ser **executado** (a escrita do teste não depende).

## Paralelismo

- Foundational: T003, T004 e T008 juntos; depois T005 e T006 juntos; T007 por último.
- US1: T009, T010, T011 (testes) em paralelo; T012 e T013 em paralelo; T014 → T015 → T016.
- US2: T017, T018, T019 em paralelo. US3: T022, T023, T024 em paralelo. US4: T027 e T028 em paralelo.
- Polish: T030–T036 em paralelo (arquivos distintos).

## Estratégia de implementação

1. **MVP = US1** (T001–T016): catálogo + criar + reabrir pelo `Location` já prova o percurso e a segurança de dono. Demonstrável sozinha.
2. Em seguida US3 (listar/reabrir, necessária para retomar) e US2 (troca com revisão), depois US4 (refino de erros) e o Polish.
3. A cada fase: rodar apenas os testes da fase (`-Dtest=...`), depois o `clean verify` completo no fechamento (T038).
4. Se faltar tempo (prazo desta história: 14/11/2026), cortar primeiro T036 e partes de T035; **nunca** cortar testes de isolamento entre contas, de concorrência nem a nota de "NÃO VERIFICADO" para o Oracle.

## Rastreabilidade (RC-07)

| Requisito (spec) | Tarefas | Verificação planejada |
|---|---|---|
| FR-001–FR-003 catálogo | T003, T004, T005, T012, T013, T016 | T009 |
| FR-004–FR-008 criar | T006, T014, T015, T016 | T010, T011 |
| FR-009–FR-013 trocar serviço e concorrência | T006, T020, T021 | T017, T018, T019, T037 |
| FR-014–FR-017 listar e reabrir | T006, T025, T026 | T022, T023, T024 |
| FR-018, FR-019, FR-021 validação e erros | T029 | T027, T028 |
| FR-020 perfis | T016 | T011, T024 |
| FR-022 logs | T014, T030 | T030 |
| FR-023 sem dado clínico | T029 (`SolicitacaoRequest` só tem `protocoloId`) | T011, T027 |
| FR-024, FR-025 documentação | T002, T032–T036, T039 | revisão do diff (T038) |
| SC-001–SC-007 | todas | T038 e, para o Oracle, T037 |

## Resultado da verificação

Execução em 13/11/2026, a partir de `backend/`, sem banco e sem variáveis `DB_*`:

| Momento | Comando | Resultado |
|---|---|---|
| Base (T001), antes de qualquer alteração | `.\mvnw.cmd clean verify` | 218 testes, 0 falhas, 0 erros, 8 ignorados (Oracle opt-in); `regulacheck-backend.jar` gerado |
| Fechamento (T038) | `.\mvnw.cmd clean verify` | **280 testes, 0 falhas, 0 erros, 11 ignorados** (8 Oracle anteriores + 3 do `SolicitacaoOracleTest`); build sem erros. Depois, só `ConcorrenciaTrocaServicoTest` foi reexecutado (renomeação de uma variável): 1 teste verde |

Testes novos da RC-07 (62 a mais que a base), todos verdes: `EntidadesTest` (3), `ProtocoloServiceTest` (5), `ProtocoloEndpointTest` (4), `CriarRascunhoServiceTest` (5), `CriarRascunhoEndpointTest` (5), `TrocarServicoServiceTest` (7), `TrocarServicoEndpointTest` (6), `ConcorrenciaTrocaServicoTest` (1), `ListarReabrirServiceTest` (6), `ListarReabrirEndpointTest` (5), `MatrizPerfisSolicitacaoTest` (3), `ValidacaoEntradaSolicitacaoTest` (5), `BancoIndisponivelSolicitacaoTest` (3), `HigieneDeLogSolicitacaoTest` (1). Verdes e já existentes que cobrem a feature: `SqlParametrizadoTest` (varre os novos repositórios JDBC), `ArquiteturaTest`, `HigieneDeLogGeralTest`, `VarreduraDeSegredosTest`.

**Verificado no Oracle real (13/11/2026, conta usada pelo grupo, Oracle 19c da faculdade, via JDBC):**
- `SolicitacaoOracleTest` (opt-in `RC_TESTE_ORACLE=SIM`): 1ª rodada, antes do `06_`: 3 testes, 1 falha e 1 erro, ambos 500 ao trocar de serviço, com `ORA-02290` em `RC_REVISAO_CK_MOTIVO` no log (confirma o efeito documentado de não aplicar o `06_`). Após o `06_`: **3 testes, 0 falhas, 0 erros** (criar, trocar com `FOR UPDATE`, revisões sem salto nem repetição sob PUTs simultâneos, isolamento entre contas, limpeza só dos dados próprios).
- `06_motivo_revisao_protocolo.sql`: aplicado (DROP + ADD); `USER_CONSTRAINTS` mostra `PROTOCOLO_ALTERADO` na lista.
- `04_testes_restricoes.sql` (14 casos + conferência final): **15 OK, 0 FALHOU**, inclusive os casos 13 e 14 e "nenhuma linha restante (total 77)". Executado por um executor JDBC temporário (fora do repositório), não pelo SQL Developer.

**Não verificado:**
- `ProtocoloRepositoryJdbc` (`GET /protocolos`) não tem teste Oracle próprio; foi exercitado indiretamente pelo `SolicitacaoOracleTest` (que resolve o protocolo ativo), mas a listagem do catálogo não foi chamada isoladamente.
- Os exemplos do `quickstart.md` contra a API com banco; SC-001 (menos de 1 minuto) não foi medido.
- A validade do `openapi.yaml` por um parser YAML (só conferida a estrutura por inspeção); o JSON do Postman foi validado.
