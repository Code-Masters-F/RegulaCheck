---

description: "Tarefas da RC-21 + RC-22 — IoT simulada (Épico 7)"
---

# Tasks: IoT simulada — leituras de dois sensores e alertas da estação de captura (RC-21 + RC-22)

**Input**: Design documents from `specs/015-iot-simulada/`

**Prerequisites**: plan.md, spec.md, research.md (D1–D10), data-model.md, contracts/api-iot.md, quickstart.md

**Tests**: obrigatórios — autorização entre contas, credencial restrita, concorrência/idempotência da abertura e ausência de bloqueio são prioridade da constituição (VII) e critérios de aceite das histórias. Valores esperados vêm da spec (faixas, prazo, limites), não da implementação.

**Organization**: por história da spec. Caminhos relativos à raiz; código em `backend/src/main/java/br/com/regulacheck/` (abreviado `main/`), testes em `backend/src/test/java/br/com/regulacheck/` (abreviado `test/`), scripts em `database/scripts/`. Build a partir de `backend/` com `sh ./mvnw` (o `mvnw` está no Git sem permissão de execução) ou `.\mvnw.cmd` no PowerShell.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1 a US5 da spec

---

## Phase 1: Setup

- [X] T001 Registrar a linha de base: `sh ./mvnw clean verify` em `backend/` (testes, falhas, ignorados) antes de qualquer mudança, na seção "Resultado da verificação" deste arquivo

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Purpose**: esquema, tipos do domínio `iot`, contrato do repositório, dobro em memória e montagem.

- [X] T002 [P] Criar `database/scripts/11_iot_sessao_captura.sql` (aditivo, para banco criado antes da RC-21): `CREATE UNIQUE INDEX RC_SESSAO_CAPTURA_UK_ABERTA ON RC_SESSAO_CAPTURA (CASE WHEN ENCERRADA_EM IS NULL THEN SOLICITACAO_ID END)`; cabeçalho com finalidade, ordem (depois do `10_`), efeito, segunda execução (ORA-00955 = já aplicado) e pré-condição (no máximo uma sessão aberta por pedido; consulta de conferência antes de criar)
- [X] T003 [P] Atualizar `database/scripts/01_criar_esquema.sql` (mesmo índice, junto dos índices de apoio), `database/scripts/04_testes_restricoes.sql` (casos novos: segunda sessão aberta no mesmo pedido recusada com ORA-00001; nova sessão aceita depois de encerrar a anterior; contagem final ajustada) e `database/scripts/README.md` (ordem de execução com o `11_`)
- [X] T004 [P] Criar em `main/iot/entity/`: `TipoSensor` (enum `LUMINOSIDADE(LUX)`, `DISTANCIA(CM)`, com a unidade amarrada), `SessaoCaptura` (record: `id`, `solicitacaoId`, `iniciadaEm`, `encerradaEm` nulo = aberta; `aberta()`), `LeituraSensor` (record: `id`, `sessaoId`, `tipo`, `valor` `BigDecimal`, `medidaEm`, `recebidaEm`), com JavaDoc
- [X] T005 [P] Criar em `main/iot/repository/`: `AberturaSessao` (desfechos `naoEncontrado`, `submetido`, `existente(sessao)`, `criada(sessao)`), `TelemetriaDoPedido` (sessão opcional + última leitura opcional por tipo), `NovaLeitura` (sessaoId, tipo, valor, medidaEm, recebidaEm) e a interface `CapturaRepository` (`abrirSessao(solicitacaoId, pacienteId, agora)`, `sessaoAbertaExiste(sessaoId)` (consulta sem bloqueio, para a ordem 404 → 400 do T019), `gravarLeitura(nova)` → `Optional<LeituraSensor>` vazio se a sessão não existe ou está encerrada (confere de novo sob bloqueio do pedido), `telemetriaDaPaciente(solicitacaoId, pacienteId)` → `Optional`) com JavaDoc de efeitos, bloqueio e desfechos
- [X] T006 [P] Criar `main/iot/service/FaixasDemonstrativas.java` (constantes D5: luminosidade 300–1000 lux, distância 20–40 cm, inclusivos; prazo de atualidade 60 s; tolerância de futuro 30 s; limites físicos 0–200000 lux e (0, 500] cm; JavaDoc dizendo que são demonstrativos e não calibrados), `SituacaoSensor` (enum) e `Orientacao` (enum com código e texto fixo em português: `CONDICAO_ADEQUADA`, `MELHORAR_ILUMINACAO`, `REDUZIR_ILUMINACAO_OU_REFLEXO`, `AFASTAR_DOCUMENTO`, `APROXIMAR_DOCUMENTO`, `VERIFICAR_ESTACAO`, `CAPTURA_SEGUE_SEM_SENSOR`)
- [X] T007 Criar `test/iot/CapturaRepositoryMemoria.java` (mesmas regras do JDBC: dona, pedido submetido, uma sessão aberta por pedido, `synchronized` no mesmo monitor de pedidos do `SolicitacaoRepositoryMemoria`, gravação só em sessão aberta, injeção de falha de banco) e ligá-lo como `capturas` em `test/auth/CenarioAuth.java`
- [X] T008 Acrescentar `CapturaRepository capturas` a `main/config/Dependencias.java`, `main/config/Servidor.java` (`new CapturaRepositoryJdbc(banco)`) e aos testes que constroem `Dependencias` (`test/common/http/CabecalhosSegurosTest.java`, `test/documento/BancoEArmazenamentoIndisponiveisTest.java`, `test/seguranca/HigieneDeLogGeralTest.java`, `test/solicitacao/BancoIndisponivelSolicitacaoTest.java`); `sh ./mvnw clean verify` continua verde

**Checkpoint**: compila, testes existentes verdes; nenhuma rota nova ainda.

---

## Phase 3: User Story 1 — Simulador envia leituras válidas da sessão de captura (P1) — RC-21 🎯 MVP

**Goal**: a paciente abre a sessão do próprio pedido (idempotente) e o envio com a chave grava leituras válidas.

**Independent Test**: com Jetty + memória, abrir a sessão (201, depois 200 com o mesmo id), enviar luminosidade 350 lux e distância 30 cm com a chave → 201 e leituras gravadas com `recebidaEm` do relógio do servidor.

### Tests for User Story 1

- [X] T009 [P] [US1] `test/iot/AbrirSessaoEndpointTest.java`: 201 com `id`, `iniciadaEm`, `aberta`, `criada: true`, `simulada: true` e `Location`; segunda chamada → 200, mesmo `id`, `criada: false`; pedido de outra paciente e inexistente → 404 com corpo idêntico; pedido `SUBMETIDA_SIMULACAO` → 409 `CONFLITO_ESTADO`; abrir sessão não muda estado nem revisão do pedido
- [X] T010 [P] [US1] `test/iot/ConcorrenciaAberturaSessaoTest.java`: 10 threads abrindo a sessão do mesmo pedido → exatamente 1 `criada: true`, todas com o mesmo `id`, 1 sessão no repositório
- [X] T011 [P] [US1] `test/iot/EnviarLeituraEndpointTest.java` (casos de sucesso): com `X-Chave-Simulador` correta, luminosidade 350 `LUX` e distância 30 `CM` → 201 com o contrato de `LeituraRegistradaResponse`; `recebidaEm` = `RelogioFixo`, não o enviado; valor com mais de 3 casas é arredondado a 3; leitura gravada no repositório

### Implementation for User Story 1

- [X] T012 [US1] Criar `main/iot/repository/CapturaRepositoryJdbc.java`: `abrirSessao` em transação curta (`SELECT … FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE` → estado → sessão aberta existente ou `INSERT … RETURNING ID`; ORA-00001 do índice → reler e devolver a existente; `COMMIT` também nos desfechos sem escrita; `ROLLBACK` explícito); `gravarLeitura` em transação curta com o **mesmo bloqueio do pedido** usado pela exclusão do rascunho: descobre o pedido da sessão (`SELECT SOLICITACAO_ID FROM RC_SESSAO_CAPTURA WHERE ID = ?`; o pedido de uma sessão nunca muda) → `SELECT ID FROM RC_SOLICITACAO WHERE ID = ? FOR UPDATE` → confere de novo, sob o bloqueio, que a sessão existe e está aberta (sem linha = vazio, nada gravado). Implementado em dois passos em vez de `FOR UPDATE OF` com junção, seguindo a convenção do `SolicitacaoRepositoryJdbc` de bloquear só a linha de `RC_SOLICITACAO` → `INSERT` em `RC_LEITURA_SENSOR` → `COMMIT`; assim uma leitura nunca chega entre o apagar das leituras e o das sessões na exclusão (D7, FR-020); `RECEBIDA_EM` do parâmetro, em UTC; falhas → `DEPENDENCIA_INDISPONIVEL` como os demais repositórios JDBC; logs só com ids e códigos
- [X] T013 [P] [US1] Criar DTOs em `main/iot/dto/`: `SessaoCapturaResponse` (`id`, `solicitacaoId`, `iniciadaEm`, `aberta`, `criada`, `simulada`), `LeituraSensorRequest` (campos do contrato; `validar(idDoCaminho, agora)` com regras do D4 e `detalhes[].campo`; sem `toString` com valores), `LeituraRegistradaResponse`
- [X] T014 [US1] Criar `main/iot/service/CapturaService.java` com `abrirSessao(sessao, solicitacaoId)` (paciente da sessão autenticada; `Propriedade.naoEncontrada` para alheio/inexistente; 409 `CONFLITO_ESTADO` com a mensagem de pedido submetido) e `registrarLeitura(sessaoId, requisicao)` (valida, grava com `recebidaEm = relogio.instant()`, sessão inexistente/encerrada → 404); JavaDoc de autorização, idempotência e efeitos
- [X] T015 [US1] Criar `main/iot/controller/CapturaController.java` (`reconhece(pathInfo)` para `/{id}/sessoes-captura` e `/{id}/leituras`; `POST …/sessoes-captura` → 201/200 + `Location: /solicitacoes/{id}/leituras`) e `main/iot/controller/LeituraSensorController.java` (`POST /sessoes-captura/{id}/leituras`, `CorpoJson.ler`, 201); despachar o `CapturaController` em `main/solicitacao/controller/SolicitacaoController.java`; em `main/config/Composicao.java` montar o `CapturaService` e publicar `rotas.registrarTelemetria("/sessoes-captura/*", …)`

**Checkpoint**: T009–T011 verdes.

---

## Phase 4: User Story 2 — API recusa credencial, sessão ou leitura inválida (P1) — RC-21

**Goal**: só o simulador, só em sessão aberta, só leitura válida; nada gravado nas recusas.

**Independent Test**: matriz de recusas contra Jetty + memória; após cada uma, o repositório não tem leitura nova.

- [X] T016 [P] [US2] Completar `test/iot/EnviarLeituraEndpointTest.java` com recusas, conferindo zero leituras gravadas em cada uma: sem chave e chave errada → 401; chave não configurada no servidor → 401; paciente e administrador autenticados → 403; sessão inexistente e sessão encerrada (criada encerrada no dobro) → 404 idênticos; `sessaoId` diferente do caminho, tipo desconhecido, `DISTANCIA` com `LUX`, valor ausente/texto/negativo, luminosidade 200001, distância 0 e 501, `medidaEm` ausente/sem fuso/31 s no futuro → 400 `REQUISICAO_INVALIDA` com o campo; `medidaEm` 29 s no futuro → 201; corpo `text/plain` → 415; corpo acima de 4 KB e JSON malformado → 400; **ordem**: sessão inexistente com corpo inválido ou `text/plain` → 404 (não 400/415), conferindo que a sessão é verificada antes do corpo
- [X] T017 [P] [US2] `test/iot/LeituraSensorRequestTest.java`: unidade de validação do `LeituraSensorRequest` nas bordas (0 e 200000 lux aceitos; distância 500 aceita; arredondamento a 3 casas; campo desconhecido no JSON recusado: o `Json` do projeto ignora campos desconhecidos, então o `LeituraSensorRequest` lê o corpo como `JsonNode` e confere a lista fechada de campos, como o `ConferenciaRequest` da RC-11)
- [X] T018 [P] [US2] `test/iot/HigieneDeLogIotTest.java`: após envios aceitos e recusados, o log capturado não contém a chave do simulador, o valor enviado nem o corpo; contém só ids, método, caminho e código (padrão de `HigieneDeLogAnaliseTest`)
- [X] T019 [US2] Ajustar `LeituraSensorRequest`/`CapturaService`/`LeituraSensorController` até T016–T018 passarem, mantendo a ordem 401/403 (filtro) → 404 (o controller consulta se a sessão existe e está aberta **antes** de chamar `CorpoJson.ler`) → 415/400 de formato do corpo (`CorpoJson`) → 400 de conteúdo (`LeituraSensorRequest.validar`); a gravação confere a sessão de novo sob bloqueio (T012), então uma sessão encerrada entre as duas conferências ainda resulta em 404; documentar a ordem no JavaDoc do controller. Para isso, `CapturaRepository` ganha `sessaoAbertaExiste(sessaoId)` (consulta sem bloqueio), também no dobro em memória

**Checkpoint**: matriz de recusas verde.

---

## Phase 5: User Story 3 — Paciente consulta a telemetria e recebe orientação (P1) — RC-22

**Goal**: dois sensores sempre, com última leitura, situação e orientação; isolamento por paciente.

**Independent Test**: leituras gravadas no dobro (dentro, abaixo, acima, recuperação) → `GET /solicitacoes/{id}/leituras` devolve situação/orientação esperadas.

### Tests for User Story 3

- [X] T020 [P] [US3] `test/iot/ClassificadorDeLeituraTest.java`: luminosidade 299,999 → `ABAIXO_DA_FAIXA`/`MELHORAR_ILUMINACAO`; 300 e 1000 → `DENTRO_DA_FAIXA`/`CONDICAO_ADEQUADA`; 1000,001 → `ACIMA_DA_FAIXA`/`REDUZIR_ILUMINACAO_OU_REFLEXO`; distância 19,999 → `AFASTAR_DOCUMENTO`; 40,001 → `APROXIMAR_DOCUMENTO`; medida há 60 s → atual; há 60,001 s → `DESATUALIZADA`/`VERIFICAR_ESTACAO` mesmo fora da faixa; medida 10 s no futuro → atual; sem leitura → `SEM_LEITURA`/`CAPTURA_SEGUE_SEM_SENSOR`
- [X] T021 [P] [US3] `test/iot/ConsultarTelemetriaEndpointTest.java`: dois sensores dentro → `CONDICAO_ADEQUADA` nos dois; luminosidade abaixo e distância dentro → cada sensor com a sua situação; sequência fora → dentro (recuperação) → situação muda e as leituras antigas continuam no repositório; leituras fora de ordem → vale a de maior `medidaEm`; duas sessões (a antiga encerrada) → vale a mais recente; resposta sempre com `simulada: true`, `aviso`, `prazoAtualidadeSegundos: 60`, faixas e ordem luminosidade, distância
- [X] T022 [P] [US3] `test/iot/MatrizPerfisIotTest.java`: `GET …/leituras` e `POST …/sessoes-captura` → 401 sem token, 403 administrador, 403 chave do simulador, 404 idêntico para pedido de outra paciente e inexistente; `POST /sessoes-captura/{id}/leituras` → 401/403 conforme o T016 (padrão das demais `MatrizPerfis*Test`)

### Implementation for User Story 3

- [X] T023 [US3] Criar `main/iot/service/ClassificadorDeLeitura.java` (puro: tipo + última leitura opcional + agora → situação + orientação, precedência do D5, limites de `FaixasDemonstrativas`)
- [X] T024 [US3] Implementar `telemetriaDaPaciente` em `main/iot/repository/CapturaRepositoryJdbc.java` (sessão mais recente da dona: `JOIN RC_SOLICITACAO … PACIENTE_ID = ?`, `ORDER BY INICIADA_EM DESC, ID DESC FETCH FIRST 1 ROW ONLY`; última leitura por tipo por `MEDIDA_EM DESC, ID DESC`; sem transação de escrita) e no dobro em memória
- [X] T025 [US3] Criar `main/iot/dto/TelemetriaResponse.java` (contrato do item 3; `sessao` e `ultimaLeitura` omitidos quando ausentes) e `CapturaService.consultarTelemetria(sessao, solicitacaoId)` (pedido alheio/inexistente → 404 via `Propriedade`); rota `GET …/leituras` no `CapturaController`

**Checkpoint**: T020–T022 verdes; RC-22 critérios (1) e (2) atendidos pela API.

---

## Phase 6: User Story 4 — Leitura desatualizada ou sensores indisponíveis não bloqueiam (P2) — RC-22

**Goal**: desatualizada e sem leitura respondem com sucesso; nenhum fluxo do pedido depende dos sensores.

**Independent Test**: com `RelogioFixo` avançado 61 s → `DESATUALIZADA`; pedido sem sessão → 200 `SEM_LEITURA`; envio de documento e exclusão do rascunho funcionam com sessão aberta.

- [X] T026 [P] [US4] Completar `test/iot/ConsultarTelemetriaEndpointTest.java`: relógio avançado além de 60 s → os dois `DESATUALIZADA`, com `medidaEm` e sem `CONDICAO_ADEQUADA`; pedido sem sessão → 200, `sessao` omitida, dois `SEM_LEITURA`; sessão sem leituras → 200, `sessao` presente, dois `SEM_LEITURA`
- [X] T027 [P] [US4] `test/iot/SensoresNaoBloqueiamTest.java`: com sessão aberta e leituras fora da faixa/desatualizadas, enviar e substituir documento, trocar serviço e excluir o rascunho seguem com os mesmos códigos de sem sensores; após excluir o rascunho, sessão e leituras do pedido não existem mais no dobro
- [X] T028 [US4] Em `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java`, `excluirRascunho` passa a apagar `RC_LEITURA_SENSOR` (via sessões do pedido) e `RC_SESSAO_CAPTURA` do pedido antes de `RC_DOCUMENTO`, na mesma transação e bloqueio (D7), com comentário do porquê; espelhar em `test/solicitacao/SolicitacaoRepositoryMemoria.java` (ou aviso ao `CapturaRepositoryMemoria`)

**Checkpoint**: RC-22 critério (3) atendido.

---

## Phase 7: User Story 5 — Sequências reproduzíveis e documentação para a demonstração (P2) — RC-21 e RC-22

**Goal**: simulador executável com três sequências reproduzíveis; documentação para o grupo e a avaliação.

**Independent Test**: `--seco` duas vezes → saídas idênticas; envio contra `ServidorDeTeste` grava as leituras; chave errada → saída 1 sem repetir.

- [X] T029 [US5] Criar `simulador-iot/SimuladorSensores.java` (D8): arquivo único, sem pacote nem dependências; argumentos `--sessao`, `--sequencia normal|fora-da-faixa|recuperacao`, `--intervalo-ms`, `--api`, `--seco` (imprime uma linha por leitura com `tipo valor unidade`, **sem instante**, para que a saída seja reproduzível; não abre conexão); chave só de `IOT_API_KEY` (ausente fora de `--seco` → saída 2 com orientação); tabelas fixas de valores (normal: 5 pares dentro da faixa; fora-da-faixa: 5 pares com luminosidade abaixo de 300 e distância fora de 20–40, alternando perto e longe; recuperação: 3 pares fora + 3 dentro); JSON montado à mão com `Locale.ROOT`; `HttpClient` com timeouts de 5 s; uma leitura por requisição, `medidaEm = Instant.now()`; imprime `tipo valor unidade → status`; primeira resposta ≠ 201 ou falha de rede → imprime o código, não repete, saída 1; nunca imprime a chave; comentário de cabeçalho dizendo que os valores são demonstrativos e sem calibração
- [X] T030 [US5] `test/iot/SimuladorSensoresProcessoTest.java`: executa o arquivo com `ProcessBuilder` e o `java` da JVM corrente (`ProcessHandle.current().info().command()`), localizando `../simulador-iot/SimuladorSensores.java` a partir de `backend/` (falha, não ignora, se não achar): `--seco` duas vezes por sequência → saídas idênticas byte a byte (sem instante) e valores dentro/fora da faixa conforme a sequência; envio real contra `ServidorDeTeste` com memória e chave de teste → 10 leituras gravadas na sequência normal; chave errada → saída 1 e exatamente 1 requisição recebida; sem `IOT_API_KEY` → saída 2; nenhuma saída contém a chave
- [X] T031 [P] [US5] Reescrever `simulador-iot/README.md`: o que cada sensor mede e a unidade; faixas demonstrativas, prazo de 60 s e limites físicos; que as leituras são simuladas, sem calibração, e **não comprovam a legibilidade** do arquivo nem bloqueiam envio/salvamento; como abrir a sessão, executar as três sequências (bash e PowerShell), `--seco`, variáveis `IOT_API_KEY` e `API_URL`; códigos de saída; a tela do app como dependência do Épico 6

**Checkpoint**: RC-21 critérios (1) e (4) e RC-22 critério (4) atendidos.

---

## Phase 8: Polish & Cross-Cutting

- [X] T032 [P] Atualizar `docs/api/README.md`, `docs/api/contratos-dados.md`, `docs/api/erros.md` (erros das três rotas), `docs/api/autorizacao.md` (rota de telemetria real), `docs/api/openapi.yaml` (três rotas, schemas; validar como YAML) e `docs/api/RegulaCheck.postman_collection.json` (abrir sessão, enviar leitura com `X-Chave-Simulador` de variável, consultar telemetria); `CONTRATO_API.md` (rota `POST /solicitacoes/{id}/sessoes-captura`, perfis)
- [X] T033 [P] Atualizar `docs/arquitetura.md` (módulo `iot` real, simulador fora do Maven), `docs/decisoes.md` (linha da RC-21/RC-22 com D1–D10 e as decisões do clarify), `docs/modelo-de-dados.md` (índice novo), `docs/ambiente.md` (`IOT_API_KEY`, `API_URL`, como rodar o simulador) e `README.md` (seção curta do simulador com link)
- [X] T034 [P] Criar `test/iot/CapturaOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM`, padrão do `AnaliseOracleTest`: confere no `@BeforeAll` se o `11_` foi aplicado e falha **antes** de criar dados; abre sessão, reabre, envia leituras, consulta, exclui o rascunho; limpa só os próprios dados)
- [X] T035 Conferir JavaDoc em português nos tipos novos e o `package-info.java` de `main/iot/` (sem texto de "reservado"); `ArquiteturaTest` verde (controller não acessa repository; classificador sem HTTP/JDBC)
- [X] T036 Verificação final: `sh ./mvnw clean verify` em `backend/`; registrar comando, testes, falhas e ignorados em "Resultado da verificação"; conferir SC-007 com uma busca em `main/iot/`, `simulador-iot/` e nos docs alterados por "calibra" e "legibilidade" (cada ocorrência precisa negar calibração e negar que a leitura comprove legibilidade); SC-001: quando houver banco, executar o `quickstart.md` seção 3 contra a API local e registrar o resultado; marcar `CapturaOracleTest`, casos do `04_` e o `quickstart.md` como **NÃO VERIFICADOS** se não forem executados; atualizar o Status da `spec.md`

---

## Dependencies & Execution Order

- Setup (T001) → Foundational (T002–T008) → US1 (T009–T015) → US2 (T016–T019) → US3 (T020–T025) → US4 (T026–T028) → US5 (T029–T031) → Polish (T032–T036).
- US2 refina os arquivos da US1 (`LeituraSensorRequest`, `CapturaService`, controllers): segue a US1.
- US3 depende do repositório e do controller da US1, mas o classificador (T020, T023) é independente e pode começar logo após a Foundational.
- US4 depende da consulta da US3 (T026) e do repositório de solicitação (T028, independente do `iot`).
- US5: o simulador (T029) só precisa do contrato; o teste por processo (T030) precisa de US1/US2 prontas.
- T002/T003/T004/T005/T006 são independentes entre si.

## Parallel Example

```text
Foundational: T002 (11_), T003 (01_/04_/README), T004 (entity), T005 (repository), T006 (faixas/enums)
US1:          T009, T010, T011 (testes) junto com T013 (DTOs)
US3:          T020 (classificador) pode começar logo após a Foundational, em paralelo à US1
US5:          T029 (simulador) e T031 (README) em paralelo à US3/US4
Polish:       T032, T033, T034
```

## Implementation Strategy

MVP = Foundational + US1 (sessão aberta pela paciente e leitura válida gravada). US2 fecha a segurança da rota do simulador; US3 entrega o valor da RC-22 pela API. US4 e US5 (P2) completam os critérios (3) e (4) das duas histórias — necessárias para dar o épico por concluído. Se faltar tempo (marco de 16/11 para o percurso completo), retirar primeiro detalhes de documentação extensa, nunca a matriz de recusas nem o isolamento por paciente.

## Rastreabilidade (RC-21 + RC-22)

| História / critério | Requisitos | Tarefas |
|---|---|---|
| RC-21 (1) script Java envia JSON por HTTP com lux e cm | FR-001, FR-009 | T011, T015, T029, T030 |
| RC-21 (2) payload com sessão, tipo, valor, unidade, instante | FR-001, FR-005, FR-008 | T005, T009–T015 |
| RC-21 (3) credencial e vínculo verificados; inválidos rejeitados; válidos persistidos | FR-002–FR-007 | T012, T016–T019 |
| RC-21 (4) sequências reproduzíveis; limites demonstrativos documentados | FR-010, FR-011, FR-015, FR-021 | T006, T029–T031 |
| RC-22 (1) telemetria da própria paciente, dois sensores | FR-012, FR-013, FR-018 | T021, T022, T024, T025 |
| RC-22 (2) orientação fora da faixa; recuperação muda o aviso | FR-014, FR-015 | T020, T021, T023 |
| RC-22 (3) desatualizada identificada; sensores não bloqueiam | FR-016, FR-017, FR-020 | T026–T028 |
| RC-22 (4) documentação do que cada sensor mede e que não comprova legibilidade | FR-019, FR-021, FR-022 | T025, T031–T033 |
| Uma sessão aberta por pedido (clarify Q4) | FR-008 | T002, T003, T009, T010, T012 |

## Resultado da verificação

Execução real em 16/11/2026, a partir de `backend/`, com o JDK 21 de `~/.jdks/ms-21.0.9` no `JAVA_HOME` (o `java` do PATH desta máquina é o Java 8; nada foi instalado nem alterado na máquina) e `sh ./mvnw` (o `mvnw` está no Git sem permissão de execução):

- **Linha de base (T001):** `sh ./mvnw clean verify` → exit 0; **644 testes, 0 falhas, 0 erros, 30 ignorados**.
- **Testes do `iot` durante a implementação:** `sh ./mvnw -o test -Dtest="br.com.regulacheck.iot.*Test"` → 36 testes, 0 falhas (todos verdes na primeira execução); `SimuladorSensoresProcessoTest` → 4 testes, 0 falhas depois de uma correção: o cabeçalho impresso pelo simulador tinha " -> " e o teste contava duas tentativas (texto do cabeçalho trocado; o simulador nunca repetiu o envio).
- **Final (T036):** `sh ./mvnw -o clean verify` → exit 0; **686 testes, 0 falhas, 0 erros, 32 ignorados** (+42 testes da RC-21/RC-22; os 2 ignorados a mais são do `CapturaOracleTest`, opt-in). Inclui `ArquiteturaTest`, `SqlParametrizadoTest` e `VarreduraDeSegredosTest` verdes com o código e os docs novos. Repetido depois das mudanças de documentação com o mesmo resultado.
- **Simulador executado à mão:** `java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia recuperacao --seco` → 12 linhas (3 pares fora da faixa e 3 dentro), saída 0.
- **SC-007:** busca por "calibra", "legib" e "legív" em `main/iot/`, `simulador-iot/` e nos docs alterados: toda ocorrência do Épico 7 nega calibração e nega que a leitura comprove legibilidade (as demais ocorrências de "ilegível" são da extração, RC-10/RC-11).
- **Não verificado:** `CapturaOracleTest` (precisa de `RC_TESTE_ORACLE=SIM` e `DB_*`), os casos 32 e 33 do `04_` e o `11_` no Oracle real; o `01_` atualizado num esquema novo; a demonstração do `quickstart.md` seção 3 contra a API com banco; o `openapi.yaml` só foi conferido por inspeção (não há parser de YAML nesta máquina; nada foi baixado para isso). A coleção Postman foi editada e regravada por um script Node que leu e escreveu o JSON (formato válido).
- **Ajustes de implementação em relação ao texto original das tarefas:** T012 usa dois passos (descobre o pedido da sessão e bloqueia só `RC_SOLICITACAO`) em vez de `FOR UPDATE OF` com junção; T017 confirma que o corpo da leitura recusa campo fora do contrato (o `Json` do projeto ignora campos desconhecidos, por isso a lista fechada é conferida no `LeituraSensorRequest`); os identificadores nas respostas vêm como texto, como no resto da API (contrato atualizado).
