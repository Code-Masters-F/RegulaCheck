---

description: "Tarefas da RC-16 — Reenviar e conferir a versão corrigida"
---

# Tasks: Reenviar e conferir a versão corrigida (RC-16)

**Input**: Design documents from `specs/020-reenviar-versao-corrigida/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-reenvio.md](contracts/api-reenvio.md), [quickstart.md](quickstart.md)

**Tests**: Obrigatórios pela constituição (VII) e pelos critérios SC-001 a SC-007 da spec: revisões desatualizadas, concorrência/idempotência, autorização entre contas, falhas por etapa e higiene de log.

**Organization**: Tarefas agrupadas por história da spec (US1–US4). Backlog: RC-16 (#38).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história da spec (US1, US2, US3, US4)
- Caminhos relativos à raiz do repositório; código em `backend/src/main/java/br/com/regulacheck/` (abreviado `main/`) e testes em `backend/src/test/java/br/com/regulacheck/` (abreviado `test/`)

---

## Phase 1: Setup

**Purpose**: Base verificada e pacote novo

- [X] T001 Registrar o build de referência antes da RC-16 (`sh ./mvnw clean verify` em `backend/`: 695 testes, 0 falhas, 31 ignorados) na seção "Resultado da verificação" deste `specs/020-reenviar-versao-corrigida/tasks.md`
- [X] T002 Criar `backend/src/main/java/br/com/regulacheck/reenvio/package-info.java` com o JavaDoc do pacote (orquestração da verificação da versão corrigida, RC-16; sem regra própria, sem IA real, leitura e resumo simulados)

---

## Phase 2: Foundational (bloqueia US1 e US2)

**Purpose**: Comparação calculada e retorno do serviço de análises, usados pela verificação (US1) e pelas rotas existentes (US2)

- [X] T003 [P] Criar `main/analise/service/ComparacaoDeAnalises.java`: record `(analiseAnteriorId, revisaoAnterior, executadaEmAnterior, itens)`, record aninhado `ItemComparado(codigoRegra, descricaoRegra, Situacao situacao, Resultado resultadoAnterior, Resultado resultadoAtual)`, enum `Situacao { RESOLVIDA, CONTINUA, NOVA }`, função pura `comparar(Analise atual, Analise anterior)` (nulo sem anterior ou com protocolo/versão diferentes; ordem dos resultados da atual; regra sem pendência nas duas fica fora) e `comAnterior(Analise atual, List<Analise> historicoDoMaisRecente)` (procura a atual pelo id e compara com a seguinte); JavaDoc pt-BR conforme data-model
- [X] T004 [P] Criar `test/analise/ComparacaoDeAnalisesTest.java`: RESOLVIDA (PENDENTE→ATENDE, NAO_VERIFICAVEL→NAO_APLICAVEL), CONTINUA (PENDENTE→NAO_VERIFICAVEL), NOVA (ATENDE→PENDENTE), regra sem pendência nas duas ausente, sem anterior → nulo, versão de protocolo diferente → nulo, `comAnterior` com a atual fora da lista → nulo, ordem dos itens, análises não alteradas
- [X] T005 Acrescentar a `main/analise/dto/AnaliseResponse.java` o campo opcional `comparacao` (records `Comparacao` e `ItemComparacao`, ids como texto, instante ISO-8601 UTC) e a fábrica `de(Analise, ComparacaoDeAnalises)`; `de(Analise)` continua sem comparação; JavaDoc do campo
- [X] T006 Em `main/analise/service/AnaliseService.java`: `ResultadoAnalise` ganha `comparacao` (calculada com `historico(…, 2)` depois de publicar ou devolver a vigente); `consultar` passa a devolver `HistoricoComparado(historico, comparacoes por id)` lendo `LIMITE_HISTORICO + 1` e devolvendo no máximo `LIMITE_HISTORICO` análises; JavaDoc atualizado. Na mesma tarefa (para o build não quebrar), adaptar `main/analise/controller/AnaliseController.java` e `main/analise/dto/HistoricoAnalisesResponse.java` ao novo retorno, ainda sem exigir a comparação na resposta (T015 a publica)
- [X] T007 Em `main/analise/service/AnaliseEmAndamentoException.java`, acrescentar construtor com mensagem e detalhes (para a etapa do reenvio), mantendo o atual

**Checkpoint**: comparação pura testada; serviço de análises entrega a comparação

---

## Phase 3: User Story 1 — Verificar a versão corrigida (Priority: P1) 🎯 MVP

**Goal**: Uma ação refaz leitura → conferência → análise → resumo da revisão atual e para na conferência sem registrar análise

**Independent Test**: Encaminhamento sem motivo + identificação analisados (R04 pendente); substituir pelo encaminhamento completo; `POST …/reenvio` → 200 `CONFERENCIA`; confirmar; `POST …/reenvio` → 201 `CONCLUIDA` com resumo simulado e R04 `RESOLVIDA`; repetir → 200 mesma análise

- [X] T008 [US1] Criar `main/reenvio/service/ReenvioService.java`: dependências `SolicitacaoService`, `ExtracaoService`, `ConferenciaService`, `AnaliseService`, `ResumoService`; `verificar(sessao, solicitacaoId)` na ordem do research D2 (dona/submetido → leitura → conferência `PENDENTE` para; `DESATUALIZADA` → 409 etapa `LEITURA` → análise → resumo); record `Resultado(etapa, revisao, extracaoId, leituraCriada, camposSemDecisao, analise, analiseCriada, comparacao)` e enum `Etapa { CONFERENCIA, RESUMO, CONCLUIDA }`; tradução de falhas por etapa com `detalhes` `etapa` e mensagens do contrato (research D7), 404 idêntico; falha do resumo depois da análise vira `etapa=RESUMO` sem desfazer a análise; logs só com ids, etapa, códigos e contagens; JavaDoc pt-BR
- [X] T009 [P] [US1] Criar `main/reenvio/dto/ReenvioResponse.java` (campos do contrato: `solicitacaoId`, `etapa`, `revisao`, `extracaoId`, `leituraCriada`, `camposSemDecisao`, `analiseCriada`, `simulada: true`, `orientacao`, `analise` opcional com `comparacao`) e a orientação textual por etapa
- [X] T010 [US1] Criar `main/reenvio/controller/ReenvioController.java` (só `POST`, `/{id}/reenvio`, id inválido → 404 padrão, 201 quando `analiseCriada`, senão 200; `ANALISE_EM_ANDAMENTO` com `Retry-After`)
- [X] T011 [US1] Entregar o subcaminho `/{id}/reenvio` em `main/solicitacao/controller/SolicitacaoController.java` e montar `ReenvioService`/`ReenvioController` em `main/config/Composicao.java` (mesmo `ResumoService` da rota de resumo)
- [X] T012 [US1] Criar `test/reenvio/ReenvioEndpointTest.java` (API em memória, leitor simulado): percurso completo com parada na conferência e nova análise vigente; leitura reaproveitada na segunda chamada; repetição sem mudança → 200, mesma análise, total de análises e resumo iguais; histórico com a anterior intacta; respostas com `simulada: true` e o aviso da análise (FR-018); caso `REQUER_CONFERENCIA` da conferência (identificação borrada) registra análise com não verificável; 404 idêntico para pedido alheio e inexistente; 409 submetido; 422 `SEM_DOCUMENTOS` com etapa `LEITURA`
- [X] T013 [P] [US1] Criar `test/reenvio/MatrizPerfisReenvioTest.java`: anônimo 401; administrador e simulador 403 sem criar leitura nem análise
- [X] T014 [US1] Concorrência em `test/reenvio/ReenvioEndpointTest.java`: 10 `POST …/reenvio` simultâneos com a leitura conferida registram no máximo 1 análise nova e no máximo 1 vigente; cada resposta é 201/200 ou 409 `ANALISE_EM_ANDAMENTO`

**Checkpoint**: verificação da versão corrigida funcional e testada

---

## Phase 4: User Story 2 — Comparar o resultado anterior com o novo (Priority: P1)

**Goal**: Toda rota que publica análise traz a comparação com a anterior; nada aparece como resolvido antes do novo resultado

**Independent Test**: Duas análises do mesmo pedido antes/depois de uma substituição; conferir classificação, ausência de `RESOLVIDA` entre substituição e nova análise e histórico intacto

- [X] T015 [US2] Usar a comparação em `main/analise/controller/AnaliseController.java` (POST) e `main/analise/dto/HistoricoAnalisesResponse.java` (GET, uma comparação por análise)
- [X] T016 [US2] Em `main/resumo/service/ResumoService.java` ler `historico(…, 2)` e devolver a comparação no `Resultado`; usar em `main/resumo/controller/ResumoController.java`
- [X] T017 [US2] Criar `test/analise/ComparacaoEndpointTest.java`: primeira análise sem `comparacao`; depois de substituir documento e antes de nova análise, nenhum `RESOLVIDA` em `GET …/analises` e análise antiga não vigente com as mesmas pendências; nova análise com `RESOLVIDA`; `CONTINUA` e `NOVA` com dados sintéticos (identificação borrada e remoção de documento); cada análise do histórico comparada com a imediatamente anterior; `POST …/resumo` com a mesma comparação do `POST …/analises`; mudança só de conferência (mesma revisão) também compara

**Checkpoint**: comparação disponível em todas as rotas de análise

---

## Phase 5: User Story 3 — Incluir o documento que faltava depois da análise (Priority: P1)

**Goal**: `POST …/documentos` aceito em qualquer estado exceto submetido, invalidando a análise vigente na mesma transação

**Independent Test**: Pedido só com encaminhamento analisado (R03 pendente); incluir identificação → revisão + 1, análise não vigente, estado mantido; reenvio → R03 `RESOLVIDA`

- [X] T018 [US3] Em `main/documento/service/DocumentoService.java`, trocar a recusa "fora de rascunho" por recusa só de `SUBMETIDA_SIMULACAO` (mensagem de somente consulta) no pré-teste e no desfecho do repositório; atualizar JavaDoc (classe e `enviar`)
- [X] T019 [US3] Em `main/documento/repository/DocumentoRepositoryJdbc.java` (`registrarBloqueado`), recusar só `SUBMETIDA_SIMULACAO` e chamar `invalidarAnalise` na mesma transação; remover `FORA_DE_RASCUNHO`/`foraDeRascunho()` de `main/documento/repository/ResultadoRegistro.java`; atualizar o JavaDoc de `registrar` em `main/documento/repository/DocumentoRepository.java`
- [X] T020 [US3] Mesma regra e invalidação em `test/documento/DocumentoRepositoryMemoria.java`
- [X] T021 [US3] Atualizar `test/documento/EnviarDocumentoEndpointTest.java` (`PROCESSANDO` passa a aceitar; `SUBMETIDA_SIMULACAO` → 409 sem gravar arquivo nem revisão) e incluir o envio em `test/solicitacao/PedidoSubmetidoSomenteConsultaTest.java`
- [X] T022 [US3] Criar `test/documento/InclusaoAposAnaliseTest.java`: inclusão em `COM_PENDENCIAS` aceita (revisão + 1, motivo `DOCUMENTO_ENVIADO`, vigente perde vigência, estado mantido, nenhum `RESOLVIDA`); limite de 3 → 422; mesma chave de tentativa → sem duplicar; arquivo inválido na inclusão **e na substituição** → análise continua vigente e revisão igual (FR-015); percurso até R03 `RESOLVIDA` via `POST …/reenvio`
- [X] T023 [P] [US3] Criar `test/reenvio/ReenvioOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM` + `DB_*`, padrão de `test/analise/AnaliseOracleTest.java`): inclusão após análise no JDBC incrementa a revisão, invalida a vigente e mantém o estado; remove só o que criou

**Checkpoint**: pendência de documento ausente tem correção após a análise

---

## Phase 6: User Story 4 — Falha preserva o último resultado e explica o que repetir (Priority: P2)

**Goal**: Cada falha informa a etapa e o que repetir, sem resultado parcial e sem alterar a última análise concluída

**Independent Test**: Provocar cada falha com substitutos de teste e conferir mensagem, etapa, histórico intacto e sucesso ao repetir

- [X] T024 [US4] Criar `test/reenvio/FalhasDoReenvioTest.java` (serviço montado sobre os repositórios do cenário): leitor indisponível → 503 `LEITURA_INDISPONIVEL` etapa `LEITURA`, sem leitura gravada, documentos e histórico intactos, nova tentativa com leitor ok conclui; avaliação falha → 503 `ANALISE_INDISPONIVEL` etapa `ANALISE`, pedido em `FALHA_ANALISE`, nova tentativa conclui; substituição durante a análise → 409 etapa `ANALISE`, nada publicado; resumo que perde a vigência → `etapa=RESUMO`, análise registrada sem resumo; análise em andamento → 409 `ANALISE_EM_ANDAMENTO` com `Retry-After` pela rota
- [X] T025 [P] [US4] Criar `test/reenvio/HigieneDeLogReenvioTest.java`: com amostras sintéticas contendo nome e trecho, nenhum log do reenvio (sucesso, parada e falhas) contém valor ou trecho de documento (padrão de `test/analise/HigieneDeLogAnaliseTest.java`)

**Checkpoint**: critério 4 verificado

---

## Phase 7: Polish & documentação

- [X] T026 [P] Atualizar `docs/api/contratos-dados.md` (seção RC-16: reenvio, `comparacao`, inclusão após análise; RC-08/RC-09 ajustados)
- [X] T027 [P] Atualizar `docs/api/openapi.yaml` (rota `/solicitacoes/{id}/reenvio`, schema `ReenvioResponse`, `comparacao` em `Analise`, estados aceitos no `POST …/documentos`)
- [X] T028 [P] Atualizar `docs/api/erros.md` (mensagem do 409 da inclusão, `detalhes[].campo = "etapa"` do reenvio)
- [X] T029 [P] Atualizar `docs/api/estados-e-revisoes.md` (inclusão após análise; decisão da RC-09 "aberta para RC-16" resolvida; verificação da versão corrigida)
- [X] T030 [P] Atualizar `docs/api/autorizacao.md` e `docs/api/RegulaCheck.postman_collection.json` com a rota nova
- [X] T031 [P] Registrar decisões em `docs/decisoes.md` e o pacote `reenvio` em `docs/arquitetura.md`; anotar em `mobile/design/textos.md` (E3.4 "Documento ausente") que a RC-16 liberou a inclusão e o texto da tela precisa ser revisto no épico 6
- [X] T032 Rodar `sh ./mvnw clean verify` em `backend/` e registrar comando e resultado real em "Resultado da verificação"; revisar o diff (sem segredo, sem conteúdo de documento, sem código morto)
- [X] T033 Executar `ReenvioOracleTest` e `AnaliseOracleTest` no Oracle **somente com autorização** para usar a conta compartilhada; sem autorização, registrar "não verificado no Oracle"
- [X] T034 Atualizar o status em `specs/020-reenviar-versao-corrigida/spec.md` conforme o que foi realmente verificado

---

## Dependencies & Execution Order

- **Setup (T001–T002)** → **Foundational (T003–T007)** → US1, US2 e US3 podem seguir; US4 depende de US1 (testa o `ReenvioService`).
- **US1**: T008 → T009/T010 → T011 → T012/T013/T014. Usa T006/T007.
- **US2**: T015/T016 dependem de T005/T006; T017 depende de T015/T016.
- **US3**: T018–T020 independentes de US1; o último cenário de T022 usa o reenvio (T011).
- **US4**: T024 depende de T008–T011; T025 depende de T011.
- **Polish**: T026–T031 em paralelo depois do código; T032 depois de tudo; T033 opcional sob autorização; T034 por último.

## Parallel Example

```text
Foundational: T003 (ComparacaoDeAnalises) ‖ T004 (teste unitário) ‖ T007 (exceção)
US1: T009 (DTO) ‖ T013 (matriz de perfis) depois de T008
US3: T023 (Oracle opt-in) ‖ T021 depois de T018–T020
Polish: T026 ‖ T027 ‖ T028 ‖ T029 ‖ T030 ‖ T031
```

## Implementation Strategy

1. **MVP**: Phases 1–3 (comparação + verificação da versão corrigida) entregam os critérios 1–3 para correção por substituição.
2. **Incremento**: US2 (comparação nas rotas existentes) e US3 (inclusão do documento que faltava).
3. **Robustez**: US4 (falhas por etapa) e documentação.
4. Não declarar telas, usabilidade ou Oracle como verificados sem execução real.

## Resultado da verificação

- **Referência antes da RC-16** (17/11/2026, `sh ./mvnw -B clean verify` em `backend/`): 695 testes, 0 falhas, 0 erros, 31 ignorados (Oracle opt-in), `BUILD SUCCESS`. O `./mvnw` está versionado sem bit de execução (`100644`), então `./mvnw` falha com *Permission denied*; `sh ./mvnw` funciona (problema pré-existente, fora do escopo).
- **Depois da RC-16** (17/11/2026, `sh ./mvnw -B clean verify` em `backend/`, JDK local 25 com `release 21`): **731 testes, 0 falhas, 0 erros, 32 ignorados** (os 31 Oracle opt-in anteriores + `ReenvioOracleTest`), `BUILD SUCCESS`. +36 testes: `ComparacaoDeAnalisesTest` 7, `ComparacaoEndpointTest` 4, `ReenvioEndpointTest` 10, `MatrizPerfisReenvioTest` 2, `FalhasDoReenvioTest` 6, `HigieneDeLogReenvioTest` 1, `InclusaoAposAnaliseTest` 4, `ReenvioOracleTest` 1 (ignorado) e 1 a mais em `EnviarDocumentoEndpointTest` (o antigo teste "fora de rascunho → 409" virou "submetido → 409" + "fora de rascunho e não submetido → 201", por mudança de requisito registrada na spec).
- `git diff --check` sem problemas; nenhum `TODO`, `System.out` ou segredo nos arquivos novos; OpenAPI validado como YAML e coleção Postman como JSON.
- **Oracle real** (17/11/2026, com autorização do responsável; `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw -B test -Dtest='ReenvioOracleTest,AnaliseOracleTest')`): **3 testes, 0 falhas, 0 ignorados**, `BUILD SUCCESS` — `ReenvioOracleTest` 1 (inclusão após análise no JDBC tira a vigência na mesma transação, revisão `DOCUMENTO_ENVIADO`, estado mantido; verificação grava a nova análise vigente com RC-R03 `RESOLVIDA`) e `AnaliseOracleTest` 2 (regressão da RC-13 com o `consultar`/`analisar` alterados). Limpeza do `@AfterAll` executada sem erro; nenhum segredo no log. Nenhum script SQL novo.
- **Não verificado:** `quickstart.md` contra a API no ar; telas do app e usabilidade (épico 6).

### Rastreabilidade RC-16 (#38) → requisitos → testes

| Critério de aceite | Requisitos | Código | Verificação |
|---|---|---|---|
| 1. Reenvio cria nova revisão e executa de novo extração, conferência, regras e resumo | FR-001–FR-006, FR-012, FR-013 | `reenvio/service/ReenvioService`, `reenvio/controller/ReenvioController`, `reenvio/dto/ReenvioResponse`, `documento/*` (inclusão após análise) | `ReenvioEndpointTest` (percurso completo, parada na conferência, repetição, ilegível, 404/409/422/405, concorrência), `InclusaoAposAnaliseTest` |
| 2. Pendência resolvida só após o novo resultado, nunca pelo upload | FR-009 | `analise/service/ComparacaoDeAnalises` (só entre análises registradas) | `ReenvioEndpointTest` (nenhum `RESOLVIDA` após upload e na parada), `ComparacaoEndpointTest.primeiraAnaliseNaoTemComparacaoESubstituirNaoResolveNada`, `InclusaoAposAnaliseTest.inclusaoDepoisDaAnalise…` |
| 3. Comparação simples, preservando o histórico | FR-007, FR-008, FR-010, FR-011 | `ComparacaoDeAnalises`, `AnaliseService` (`comparacao` em analisar/consultar), `ResumoService`, `AnaliseResponse.comparacao` | `ComparacaoDeAnalisesTest`, `ComparacaoEndpointTest`, `ReenvioEndpointTest` (anterior intacta no histórico) |
| 4. Erro preserva o último conjunto íntegro e explica o que repetir | FR-014–FR-017 | `ReenvioService` (etapa em `detalhes`, mensagens do que repetir, `etapa=RESUMO`) | `FalhasDoReenvioTest` (leitura, avaliação, mudança durante a análise, resumo, em andamento), `InclusaoAposAnaliseTest.arquivoInvalido…`, `ReenvioEndpointTest.pedidoSemDocumentos…` |
| Limites (simulação, dados, logs, autorização) | FR-018–FR-021 | idem | `HigieneDeLogReenvioTest`, `MatrizPerfisReenvioTest`, asserções de `simulada`/`aviso` em `ReenvioEndpointTest` |

