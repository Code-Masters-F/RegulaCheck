# Tasks: Conferir a transcrição antes da validação (RC-11)

**Input**: `specs/012-conferir-transcricao/` (spec.md, plan.md, research.md, data-model.md, contracts/api-conferencia.md, quickstart.md)

**Backlog**: RC-11 — Code-Masters-F/ClickBus#19 · **Branch**: `012-conferir-transcricao` · **Depende de**: RC-10 (leitura simulada, concluída e mesclada)

**Testes**: exigidos pelo `CLAUDE.md` §14 (autorização entre contas, revisões desatualizadas, concorrência/idempotência, higiene de log, campos não verificáveis). Em cada história o teste é escrito **antes** da implementação e deve **falhar primeiro**; registrar no fim, com honestidade, o que não foi visto falhar. Tudo roda sem banco no `.\mvnw.cmd clean verify`; o teste contra o Oracle é opt-in (T041) e, sem execução real, é relatado como **NÃO VERIFICADO**.

**Regras desta execução**: nada de commit/push/PR sem pedido; **nenhum comando contra o Oracle da faculdade sem autorização específica do usuário** (inclui aplicar o `09_`); só dados sintéticos; **a paciente só confirma ou marca divergência: nenhum valor digitado é aceito**; esta história **não altera o estado nem a revisão do pedido**, não gera resultado de regra, pendência nem resultado geral, e **nunca libera submissão**; leitura simulada continua identificada; nenhuma dependência nova no `pom.xml`; nenhum teste das histórias anteriores é enfraquecido.

**Limitação declarada de verificação**: a leitura é simulada (testes provam o contrato, não leitura real); `SUBMETIDA_SIMULACAO` só existe em pedido montado em memória; os dobros em memória provam a lógica de concorrência, não o bloqueio de linha do Oracle.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 ver a conferência · US2 confirmar/divergir · US3 informação ausente não é acrescentada · US4 situação e sem liberação · US5 leitura desatualizada, submetido e concorrência
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [X] T001 Conferir `git status` na branch `012-conferir-transcricao` e rodar `.\mvnw.cmd clean verify` em `backend/` **antes** de qualquer alteração; registrar o resultado-base (esperado: 452 testes, 24 ignorados) em "Resultado da verificação"
- [X] T002 [P] Reconhecer os pontos de extensão: ler `main/solicitacao/controller/SolicitacaoController.java` (entrega de subcaminhos), `main/extracao/controller/ExtracaoController.java` (`reconhece`), `main/extracao/repository/ExtracaoRepository(+Jdbc).java` (`carregar`, `SQL_CAMPOS`), `main/config/{Composicao,Dependencias,Servidor}.java`, `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (`excluirRascunho`, `apagarExtracoes`), `test/auth/CenarioAuth.java`, `test/extracao/{ExtracaoRepositoryMemoria,CenarioExtracao,LeitoresDeTeste}.java`, `test/documento/CenarioDocumento.java`; **corrigir `plan.md`/`research.md` antes de seguir** se algo não bater com o código
- [X] T003 [P] Conferir com `git ls-files` que não existe nada de conferência (`RC_CONFERENCIA`, `ConferenciaController`) para não sobrescrever; confirmar que as amostras de `samples/` e o catálogo de testes cobrem os casos precisos (campo identificado, não localizado, ilegível, documento não lido por leitor falso); se faltar um caso, registrar em T033

---

## Phase 2: Foundational (bloqueia as histórias)

**Meta**: tipos, regra de situação, repositório (interface, dobro e JDBC), esquema, `Dependencias` e validação do corpo compilando; nenhuma rota responde ainda.

- [X] T004 [P] `main/extracao/entity/{TipoDecisao,MotivoDivergencia,DecisaoConferencia}.java`: enums `CONFIRMADA`/`DIVERGENTE` e `VALOR_DIFERENTE_DO_DOCUMENTO`/`CAMPO_EXISTE_E_NAO_FOI_LIDO`; record `DecisaoConferencia` (documentoId, `CodigoCampo`, tipo, motivo anulável, instante); JavaDoc; `toString` sem dados do documento
- [X] T005 [P] `test/extracao/SituacaoConferenciaTest.java` (escrito antes, visto falhar): precedência `REQUER_CONFERENCIA` > `PENDENTE` > `CONFERIDA`; documento `PROTEGIDO`/`LEITURA_VAZIA` → requer; campo `ILEGIVEL` em documento lido → requer (mesmo com "decisão" injetada); decisão `DIVERGENTE` → requer; campo identificado sem decisão → pendente; tudo confirmado → conferida; `NAO_LOCALIZADO` sem divergência não pesa, com divergência → requer; leitura desatualizada → `DESATUALIZADA`; contagens (`camposNaoVerificados`, `camposSemDecisao`, `divergencias`); aplicabilidade da decisão por situação do campo (identificado: confirmar ou valor diferente; não localizado: só "campo existe"; ilegível: nenhuma)
- [X] T006 `main/extracao/service/SituacaoConferencia.java` (regra pura, sem HTTP nem JDBC; situação, contagens e aplicabilidade da decisão) e `main/extracao/service/OrientacaoConferencia.java` (textos fixos de D8 por situação do campo e motivo; sem eco de valor, sem diagnóstico, urgência ou promessa); JavaDoc. Depende de T004 e T005
- [X] T007 [P] `database/scripts/09_conferencia.sql` (aditivo, só `CREATE TABLE`, prefixo `RC_`; `RC_CONFERENCIA_CAMPO` com PK, FK composta para `RC_EXTRACAO_CAMPO` e as `CHECK`s do `data-model.md`; comentário de finalidade, ordem e efeito) e `database/scripts/01_criar_esquema.sql` (cria a mesma tabela em banco novo), `04_testes_restricoes.sql` (casos 23 a 27: duas decisões para o mesmo campo, motivo em `CONFIRMADA`, `DIVERGENTE` sem motivo, motivo fora da lista, decisão para campo inexistente na leitura; ajustar cabeçalho e contagem), `90_limpeza_DESTRUTIVA.sql` e `README.md` dos scripts. **Não executar nada no Oracle**
- [X] T008 `main/extracao/repository/ExtracaoRepository.java` (+ `buscarPorId(pacienteId, solicitacaoId, extracaoId)` → `Optional<ExtracaoRegistrada>`, com a dona e o pedido na própria consulta), `ExtracaoRepositoryJdbc.java` (reaproveita `carregar`; filtra `e.ID` e `e.SOLICITACAO_ID` e a dona) e `test/extracao/ExtracaoRepositoryMemoria.java` (idem, sob o monitor do pedido); JavaDoc
- [X] T009 `main/extracao/repository/{ConferenciaRepository,ResultadoRegistroDecisao,DecisaoNova}.java`: `buscar(pacienteId, solicitacaoId, extracaoId)` → leitura + decisões vigentes + revisão atual; `registrar(pacienteId, solicitacaoId, extracaoId, List<DecisaoNova>, agora)` → `REGISTRADA`, `NAO_ENCONTRADO`, `SUBMETIDO`, `DESATUALIZADA`, `DOCUMENTO_FORA_DA_LEITURA`, `DECISAO_NAO_APLICAVEL`; JavaDoc de atomicidade, dona e `FOR UPDATE`. Depende de T004 e T006
- [X] T010 [P] `test/extracao/ConferenciaRepositoryMemoria.java` (dobro, mesmas regras do JDBC e mesmas decisões de aplicabilidade via `SituacaoConferencia`; `synchronized` no monitor dos pedidos; upsert; lote tudo ou nada; `registrarLimpeza` no `SolicitacaoRepositoryMemoria` para apagar as decisões ao excluir o pedido, **antes** das leituras; `falharNaProximaConsulta`/`falharNoProximoRegistro`; contagem de gravações). Depende de T008 e T009
- [X] T011 `main/extracao/repository/ConferenciaRepositoryJdbc.java`: `buscar` em leitura; `registrar` numa transação curta com `SELECT … FOR UPDATE` do pedido (dona), recusa `SUBMETIDA_SIMULACAO`, confere que a leitura pertence ao pedido e que `REVISAO = REVISAO_ATUAL` (senão `DESATUALIZADA`), valida cada decisão contra a situação do campo lida **sob o bloqueio**, lê as decisões vigentes **sob o bloqueio** e faz `UPDATE` (já existia e mudou) ou `INSERT` (não existia), sem mexer em `DECIDIDA_EM` quando a decisão não mudou; `COMMIT`/`ROLLBACK` explícito; ORA-00942 → 503 com orientação só no log (`09_`); SQL só com parâmetros; log só com ids, quantidade e código. Depende de T007 e T009
- [X] T012 `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (`excluirRascunho`): apagar `RC_CONFERENCIA_CAMPO` do pedido **antes** de `RC_EXTRACAO_CAMPO` (ignorando ORA-00942 como já é feito para o `08_`); atualizar o teste de exclusão existente **só acrescentando** o caso com decisão gravada. Depende de T007
- [X] T013 `main/config/{Dependencias,Servidor,Composicao}.java` e testes que constroem `Dependencias` (`test/auth/CenarioAuth.java` e os 5 outros: `CabecalhosSegurosTest`, `BancoEArmazenamentoIndisponiveisTest`, `HigieneDeLogGeralTest`, `BancoIndisponivelSolicitacaoTest` ×2): `Dependencias` ganha `conferencias`; `CenarioAuth` cria `ConferenciaRepositoryMemoria`; **só o construtor muda, nenhuma asserção**. A ligação do controller em `Composicao` é feita no **T018**. Depende de T009 e T010
- [X] T014 [P] `test/extracao/ConferenciaRequestTest.java` (escrito antes, visto falhar) e `main/extracao/dto/ConferenciaRequest.java` (`JsonNode decisoes`, validação à mão, D1): aceita 1 a 18 decisões com as chaves `documentoId`, `codigo`, `decisao`, `motivo`; recusa (400 com detalhe no campo) lista ausente/vazia/>18, não-objeto, **chave desconhecida (`valor`, `texto`, `observacao`)**, `decisao`/`motivo`/`codigo` fora da lista, `DIVERGENTE` sem motivo, `CONFIRMADA` com motivo, par `(documentoId, codigo)` repetido, `documentoId` malformado ou não textual; nenhuma mensagem reproduz o conteúdo recebido

**Checkpoint**: `.\mvnw.cmd -q test-compile` compila produção e testes; `clean verify` continua com o resultado-base de T001; nenhuma rota nova responde.

---

## Phase 3: US1 — Ver o que foi lido, com a origem, e o que não pôde ser verificado (P1) 🎯 MVP

**Meta**: a paciente abre a conferência da leitura e vê campos, origem, destaques e orientações, marcados como simulados. Atende FR-001 a FR-005, FR-010, FR-019 a FR-021.

**Teste independente**: pedido com leitura contendo campos identificados, não localizados e ilegíveis e um documento não lido → `GET …/conferencia` → 200 com `naoVerificado`, orientações e situação; sem token 401; alheio 404.

### Testes (escritos antes e vistos falhar)

- [X] T015 [P] [US1] `test/extracao/ConsultarConferenciaEndpointTest.java` (Jetty real, repositórios em memória, `LeitorSimulado` com todas as amostras e `LeitorFalso` para não lidos): 200 com `extracaoId`, `situacao`, `liberadoParaSubmissao: false`, `simulada: true`, `aviso`, `atualizadaEm`, `resumo` e `documentos[]` com `valor` e `origem` só nos identificados, `naoVerificado` e `orientacao` (texto exato de D8) nos ilegíveis/não localizados, documento protegido ou vazio com `motivo`/`reenviar`/`orientacao`; a leitura original (valores, situações, origens) idêntica à de `GET …/extracoes`; pedido sem leitura, `extracaoId` inexistente, de outro pedido da mesma paciente, de outra paciente e id mal formado → 404 **idêntico**; sem token 401; administrador e simulador 403; `DELETE`/`PATCH` na rota → 405 com `Allow: GET, PUT`; nenhuma resposta traz caminho, hash ou nome interno; estado e revisão do pedido inalterados depois de várias consultas

### Implementação

- [X] T016 [US1] `main/extracao/dto/{ConferenciaResponse,DocumentoConferidoResponse,CampoConferidoResponse}.java` (records; `valor`/`origem` só em identificado; `decisao` com tipo, motivo e `decididaEm`; omitem o que é nulo; `aviso` e `simulada` fixos; `liberadoParaSubmissao` sempre `false`; sem caminho, hash ou nome interno) com JavaDoc. Depende de T006
- [X] T017 [US1] `main/extracao/service/ConferenciaService.java` (`consultar(sessao, solicitacaoId, extracaoId)`: `ConferenciaRepository.buscar` → 404 idêntico se vazio; monta a visão com `SituacaoConferencia` e `OrientacaoConferencia`; JavaDoc de efeitos; log só com ids). Depende de T006, T009, T016
- [X] T018 [US1] `main/extracao/controller/ConferenciaController.java` (`reconhece(pathInfo)` para `/{id}/extracoes/{extracaoId}/conferencia`, `atender`, `doGet`; `Allow: GET, PUT`; ids validados como no `ExtracaoController`; 404 padrão para forma inválida), `main/solicitacao/controller/SolicitacaoController.java` (entrega o subcaminho ao `ConferenciaController` **antes** do `ExtracaoController`) e `main/config/Composicao.java` (liga `ConferenciaService` e `ConferenciaController`); o `PUT` só passa a funcionar no T021 (até lá, o método não implementado responde 405). Depende de T013 e T017

**Checkpoint**: US1 demonstrável e testável sozinha.

---

## Phase 4: US2 — Confirmar ou marcar divergência, campo a campo (P1)

**Meta**: a paciente decide por campo; uma decisão vigente por campo; só confirmar/divergir; tudo ou nada. Atende FR-006 a FR-010, FR-018.

### Testes (escritos antes e vistos falhar)

- [X] T019 [P] [US2] `test/extracao/RegistrarDecisaoEndpointTest.java`: confirmar campo identificado → 200 com a decisão (`tipo`, `decididaEm`) e a conferência refletindo; divergir com `VALOR_DIFERENTE_DO_DOCUMENTO`; marcar `CAMPO_EXISTE_E_NAO_FOI_LIDO` em campo não localizado; decidir de novo substitui (uma vigente; `DECIDIDA_EM` muda só quando a decisão muda); repetir a mesma decisão é no-op (200, mesmo `decididaEm`); lote com várias decisões; **lote com uma inválida não grava nenhuma** (400/422/404 e conferência igual à de antes); recusas: chave `valor` (400), `decisao`/`motivo` inválidos (400), confirmar campo não localizado (422), qualquer decisão em campo ilegível (422 `DECISAO_NAO_APLICAVEL`), documento fora da leitura (404 idêntico), `codigo` desconhecido (400), corpo que não é JSON (415), corpo vazio ou > 4 KB (400); a leitura original (valores, situações, origens) **idêntica** antes e depois; `atualizadaEm` da conferência é o instante da leitura quando não há decisão, **avança** quando uma decisão muda e **não muda** quando a mesma decisão é repetida; cabeçalhos de dono/estado/revisão ignorados e chaves de dono/estado/revisão no corpo recusadas (400, corpo fechado); pedido de outra paciente/inexistente/leitura de outro pedido → 404 idêntico **antes** de ler o corpo

### Implementação

- [X] T020 [US2] `main/extracao/service/ConferenciaService.java`: `registrar(sessao, solicitacaoId, extracaoId, ConferenciaRequest)` — dona (404) antes do corpo; valida o corpo (`ConferenciaRequest.validar`); converte em `DecisaoNova`; chama `ConferenciaRepository.registrar`; mapeia `ResultadoRegistroDecisao` (`NAO_ENCONTRADO`/`DOCUMENTO_FORA_DA_LEITURA` → 404 idêntico; `SUBMETIDO` e `DESATUALIZADA` → 409 com as mensagens do contrato; `DECISAO_NAO_APLICAVEL` → 422 com o campo no detalhe); depois relê e devolve a conferência; log só com ids e quantidade. Depende de T011, T014 e T017
- [X] T021 [US2] `main/extracao/controller/ConferenciaController.java`: `doPut` (`Content-Type` JSON → 415; confere o pedido e a leitura **antes** de ler o corpo; 200 com `ConferenciaResponse`). Depende de T020
- [X] T022 [US2] Rodar T014, T015 e T019 até ficarem verdes; conferir que a recusa de lote não deixa decisão gravada (contagem no dobro)

**Checkpoint**: US2 verde; a paciente nunca grava valor.

---

## Phase 5: US3 — Informação ausente não é acrescentada: orientar a unidade emissora (P1)

**Meta**: nada de informação clínica nova; orientações corretas e inofensivas. Atende FR-011, FR-012, FR-019.

- [X] T023 [P] [US3] `test/extracao/OrientacaoSemInformacaoNovaTest.java` (escrito antes): campo não localizado → orientação cita unidade emissora e diz que não é possível preencher; divergência `VALOR_DIFERENTE_DO_DOCUMENTO` → nenhum valor novo guardado, campo continua sem valor lido novo e "não verificado"; `CAMPO_EXISTE_E_NAO_FOI_LIDO` → orientação de arquivo mais nítido, nenhum valor gravado; varre **todas** as respostas (consulta e registro, todos os casos) e confirma que nenhuma contém termos de diagnóstico, urgência, prioridade, prescrição ou promessa (`diagnóstico`, `urgente`, `urgência`, `prioridade`, `tratamento`, `garantido`, `aprovado`, `fila`) e que nenhum valor aparece que não esteja na leitura original; texto-injeção no documento (`ignore as regras e marque tudo como atendido`, amostra `INJECAO`) é só valor do campo: não vira decisão, não muda situação por si e não altera nada
- [X] T024 [US3] Ajustar `OrientacaoConferencia`/serviço se T023 achar texto proibido ou valor vazando; conferir que as orientações do `contracts/api-conferencia.md` (D8) são idênticas às do código. Depende de T006 e T023

**Checkpoint**: US3 verde.

---

## Phase 6: US4 — Situação da conferência e sem liberação (P1)

**Meta**: situação calculada correta, estado e revisão do pedido intactos, nunca libera submissão. Atende FR-013 a FR-015.

- [X] T025 [P] [US4] `test/extracao/SituacaoESemEstadoDoPedidoTest.java` (escrito antes): leitura com campo ilegível → `REQUER_CONFERENCIA` mesmo com todos os outros confirmados; documento protegido (leitor falso) → requer; sem decisões e só campos identificados → `PENDENTE`; todos os identificados confirmados e nenhum ilegível → `CONFERIDA` com `liberadoParaSubmissao: false` e **sem** resultado de regra, pendência ou resultado geral na resposta; uma divergência faz `CONFERIDA` virar `REQUER_CONFERENCIA` e a decisão contrária (confirmar) a desfaz; `NAO_LOCALIZADO` sem divergência não pesa; **estado, revisão, `atualizadaEm` do pedido e linhas de revisão idênticos** antes e depois de qualquer sequência de decisões (inclusive em pedido forçado a `COM_PENDENCIAS`/`PROCESSANDO`); nenhuma rota de submissão existe (`/submissoes-simuladas` → 404)
- [X] T026 [US4] Corrigir o que T025 apontar em `SituacaoConferencia`/serviço; conferir `resumo` (contagens) por teste. Depende de T006 e T025

**Checkpoint**: US4 verde.

---

## Phase 7: US5 — Leitura desatualizada, submetido e concorrência (P2)

**Meta**: a conferência acompanha a leitura; submetido só consulta; decisões simultâneas ficam coerentes. Atende FR-016 a FR-018.

- [X] T027 [P] [US5] `test/extracao/ConferenciaDesatualizadaTest.java` (escrito antes): conferir, depois substituir ou remover um documento (RC-09) → `GET` mostra `DESATUALIZADA` (somente leitura, com as decisões anteriores visíveis) e `PUT` → 409 ("Os documentos mudaram; leia os documentos de novo."), nada gravado; nova leitura (`POST …/extracoes`) → conferência nova **sem decisões herdadas** e a anterior continua consultável pelo `extracaoId` antigo; enviar documento novo também desatualiza; pedido `SUBMETIDA_SIMULACAO` (montado em memória) → `PUT` 409 documentado, `GET` 200; documento removido deixa a decisão sem efeito na leitura nova
- [X] T028 [P] [US5] `test/extracao/ConcorrenciaConferenciaTest.java` (dobro em memória, várias threads, dizendo no teste que prova a lógica e não o bloqueio de linha do Oracle): N `PUT` simultâneos do mesmo campo com decisões diferentes → resta **uma** decisão vigente, coerente com uma das enviadas, sem erro; `PUT` concorrente com substituição de documento → ou a decisão vale para a leitura atual da época, ou 409 sem gravar; nenhuma decisão fica gravada para leitura que já não era a atual; lotes simultâneos disjuntos não perdem nenhuma decisão
- [X] T029 [US5] Corrigir `ConferenciaRepositoryMemoria`/`Jdbc`/serviço no que T027/T028 apontarem (checar a revisão capturada **sob o bloqueio**). Depende de T011, T027 e T028

**Checkpoint**: US5 verde.

---

## Phase 8: Transversais, documentação e verificação

- [X] T030 [P] `test/extracao/MatrizPerfisConferenciaTest.java`: `GET` e `PUT` × {sem token, paciente, administrador, simulador} (401/200/403/403), incluindo ids inexistentes; métodos não previstos → 405 com `Allow`; confirmar que as matrizes das RC-05/RC-08/RC-10 continuam verdes sem alteração de asserção
- [X] T031 [P] `test/extracao/HigieneDeLogConferenciaTest.java`: captura de log durante consulta, registro, repetição, divergência, 409, 422, 400 e 503: nenhum valor extraído, trecho, nome de arquivo, caminho, hash, e-mail, token, motivo digitado ou texto do documento em log ou resposta de erro; sem stack trace (FR-020)
- [X] T032 [P] `test/extracao/BancoIndisponivelConferenciaTest.java`: banco fora na consulta e no registro → 503 `DEPENDENCIA_INDISPONIVEL` sem detalhe técnico, nada gravado pela metade; passada a falha, a repetição funciona
- [X] T033 [P] (nenhuma amostra faltou: as 7 amostras e o catálogo de teste da RC-10 cobriram campo identificado, não localizado, ilegível, documento não lido, divergência de tipo e injeção) Se T003 apontou amostra faltante, acrescentar em `test/extracao/AmostrasSinteticas.java`/catálogo de teste (commitadas, sem alterar as oficiais nem o catálogo de produção)
- [X] T034 `CONTRATO_API.md` (linha do `PUT …/conferencia` passa a implementada + a do `GET`; tabela de perfis) e `docs/api/{README,contratos-dados,erros,autorizacao,estados-e-revisoes}.md`: rotas, `ConferenciaResponse`, a regra "só confirmar/divergir, sem valor digitado", motivos, situação calculada, `liberadoParaSubmissao`, erros (`DECISAO_NAO_APLICAVEL` 422, 409 de leitura desatualizada), nota de que a conferência não muda estado nem revisão; `docs/api/openapi.yaml` (0.9.0, referências íntegras, YAML carrega) e `docs/api/RegulaCheck.postman_collection.json` (itens `GET`/`PUT` da conferência)
- [X] T035 `docs/{modelo-de-dados,arquitetura,decisoes,ambiente}.md`, `README.md` e `database/scripts/README.md`: tabela `RC_CONFERENCIA_CAMPO`, `09_`, fluxo da conferência, decisões de D3/D4 (sem estado nem revisão; ilegível sem decisão), como rodar o teste Oracle, pendências para RC-12/RC-13/RC-16; remover a nota "aberta" que a RC-10 deixou sobre estado quando passar a ser respondida; **em `estados-e-revisoes.md` (seção Revisão) e `decisoes.md`, registrar o desvio do princípio VI**: a decisão de conferência não cria revisão e a RC-12/RC-13 devem capturar a conferência (`atualizadaEm`) ao publicar uma análise e invalidá-la se mudar
- [X] T036 [P] Revisão cruzada: `estados-e-revisoes.md` (nota da RC-11 coerente com o fluxo), `docs/protocolo-academico.md` ("não encontrar campo não comprova ausência" respeitado), `CLAUDE.md` §11 (a paciente confirma ou aponta divergência; não inventa informação) sem contradição; corrigir a origem se houver
- [X] T037 `main`: JavaDoc revisado em todas as classes novas; nenhum `System.out`, import curinga ou código comentado; nenhuma dependência nova
- [X] T038 Rodar `.\mvnw.cmd clean verify` em `backend/`; registrar o número real de testes, falhas e ignorados
- [X] T039 Conferir o **quickstart** contra o código e o contrato (rotas, códigos, mensagens); o que depende do Oracle ou da API no ar fica **NÃO VERIFICADO** até execução real
- [X] T040 Matriz de rastreabilidade FR-001…FR-022 / SC-001…SC-008 ↔ tarefas ↔ testes e seções "Desvios" e "Resultado da verificação" no fim deste arquivo; atualizar `spec.md` (Status) com o que foi realmente implementado e verificado
- [X] T041 [opt-in] **(executado em 13/11/2026, com autorização do usuário: `09_` aplicado na conta do grupo e `ConferenciaOracleTest` com 3 testes, 0 falhas)** `test/extracao/ConferenciaOracleTest.java` (`RC_TESTE_ORACLE=SIM`, credenciais só por variáveis, dados sintéticos, limpa só o que criou; exige `08_` e `09_` aplicados): gravar decisões, duas threads no mesmo campo (uma decisão vigente), `CHECK`s (motivo em `CONFIRMADA`, `DIVERGENTE` sem motivo, motivo fora da lista), FK composta (campo inexistente), leitura desatualizada após substituir (409), excluir rascunho com decisão sem resíduo. **Só executar com autorização específica do usuário para aplicar o `09_` e rodar no Oracle**; sem isso, relatar **NÃO VERIFICADO**

---

## Dependências e ordem

- Phase 1 → Phase 2 → US1 → US2 → (US3 ‖ US4) → US5 → Phase 8.
- T006 depende de T004/T005; T009 de T004/T006; T010 de T008/T009; T011 de T007/T009; T013 de T009/T010; T017 de T006/T009/T016; T018 de T013/T017; T020 de T011/T014/T017; T021 de T020.
- T007 (SQL) não depende de Java; nada roda no Oracle sem autorização.
- Dentro de cada história: teste → implementação; o teste deve ser visto falhar antes.

## Paralelismo

- Fundação: T004, T005, T007, T014 em paralelo; depois T006, T008, T009; depois T010, T011, T012, T013.
- US1: T015 e T016 em paralelo; depois T017 → T018.
- Transversais: T030 a T033 em paralelo; T034 a T036 em paralelo.

## Estratégia

MVP = Phase 1 + 2 + US1 (consultar a conferência). Em seguida US2 (decidir), US3/US4 (honestidade das orientações e da situação) e US5 (versão e concorrência). Cortar primeiro T032 e o item de Postman se o prazo apertar; **não** cortar higiene de log, autorização, recusa de valor digitado nem a regra de não alterar estado e revisão.

---

## Resultado da verificação

Executado em 13/11/2026, a partir de `backend/`.

| Verificação | Resultado |
|---|---|
| T001 baseline antes de qualquer alteração (`.\mvnw.cmd clean verify`) | 452 testes, 0 falhas, 0 erros, 24 ignorados |
| T038 final (`.\mvnw.cmd clean verify`) | 523 testes, 0 falhas, 0 erros, 27 ignorados (os 24 anteriores mais os 3 do `ConferenciaOracleTest`); `BUILD SUCCESS` |
| Testes novos desta história | 71 (68 que rodam: 11 `SituacaoConferenciaTest`, 11 `ConferenciaRequestTest`, 7 `ConsultarConferenciaEndpointTest`, 11 `RegistrarDecisaoEndpointTest`, 5 `OrientacaoSemInformacaoNovaTest`, 8 `SituacaoESemEstadoDoPedidoTest`, 5 `ConferenciaDesatualizadaTest`, 3 `ConcorrenciaConferenciaTest`, 3 `MatrizPerfisConferenciaTest`, 1 `HigieneDeLogConferenciaTest`, 3 `BancoIndisponivelConferenciaTest`; mais 3 do `ConferenciaOracleTest`, ignorados) |
| Verificação por mutação (7 proteções quebradas de propósito, uma por vez, e depois revertidas) | as 7 foram pegas por falhas reais de teste: ilegível não exigir conferência (3 falhas), dobro sem checar leitura atual (4), corpo aceitando chave desconhecida (4), sem validar aplicabilidade da decisão (2), repetir decisão regravando o instante (1), dona não conferida antes do corpo (1), `liberadoParaSubmissao` verdadeiro (4) |
| Testes vistos falhar antes da implementação | `SituacaoConferenciaTest` e `ConferenciaRequestTest` (não compilavam), e a asserção `Allow: GET, PUT` do `ConsultarConferenciaEndpointTest` (falhou até o `PUT` existir). **Não vistos falhar antes**: os demais testes de US2 a US5, escritos antes do código de cada fatia mas só executados depois que ele existia; a prova de que eles pegam defeitos é a verificação por mutação acima |
| T041 `ConferenciaOracleTest` | **executado no Oracle em 13/11/2026**: 3 testes, 0 falhas (junto de `ExtracaoOracleTest` 4 e `SolicitacaoOracleTest` 3); `RC_CONFERENCIA_CAMPO`, `RC_EXTRACAO*` com 0 linhas depois; sem `RC_TESTE_ORACLE=SIM` continua ignorado |
| `09_conferencia.sql` | escrito e incorporado ao `01_`, `04_` (casos 23 a 27), `90_` e ao README dos scripts; **aplicado no Oracle em 13/11/2026 (com autorização)**: tabela criada com 9 constraints; casos 23 a 27 do `04_` **não executados** |
| T039 quickstart | conferido contra o código e o contrato (rotas, códigos, mensagens); **não executado contra a API no ar** nem contra o Oracle |
| OpenAPI | `openapi.yaml` 0.9.0 carrega como YAML e as 32 referências `$ref` resolvem; coleção Postman carrega como JSON |

**O que o sucesso do build prova e o que não prova:** a lógica da conferência (situação, aplicabilidade, corpo fechado, ordem dos erros, higiene de log, autorização, tudo ou nada, versão e concorrência com o dobro em memória) está verificada. **Não** está verificado: o bloqueio de linha do Oracle, as restrições e a chave estrangeira da nova tabela, o `ORA-00942` ignorado ao excluir rascunho em banco sem o `09_`, e a leitura real de arquivo (a leitura é simulada).

## Desvios

1. **Não cria revisão nem muda o estado do pedido** (já registrado no plano, "Complexity Tracking", como desvio justificado do princípio VI): a decisão é anotação da leitura. Compensação: `atualizadaEm`; a RC-12/RC-13 devem capturar a conferência ao publicar uma análise e invalidá-la se mudar. Registrado em `docs/api/estados-e-revisoes.md` e `docs/decisoes.md`.
2. **Campo identificado com divergência continua mostrando o valor lido** (o contrato dizia "nunca traz valor nem origem" para campos divergentes, o que contradizia FR-010 e a regra de que a paciente não substitui o valor): o contrato foi esclarecido. Campo ilegível e não localizado nunca trazem valor nem origem.
3. **Corpo fechado também na raiz:** chaves de dono, estado ou revisão no corpo do `PUT` são recusadas (400), e não ignoradas como na RC-10 (o `POST` de lá não tem corpo). Cabeçalhos de dono, estado e revisão continuam ignorados.
4. **Upsert:** o repositório lê as decisões vigentes sob o bloqueio e faz `UPDATE` ou `INSERT`, em vez de `UPDATE` seguido de `INSERT` (equivalente sob o bloqueio e permite não tocar em `DECIDIDA_EM` quando a decisão não muda).
5. **`DELETE`/`POST`/`PATCH` na rota da conferência** respondem 405 com `Allow: GET, PUT`; o `PUT` só foi habilitado no T021 (até lá a rota aceitava só `GET`).
6. **Documento sem campos (não lido) + decisão** responde 422 `DECISAO_NAO_APLICAVEL` (o documento pertence à leitura; só não há campo a decidir), e não 404.
7. **Correção de `CONTRATO_API.md`:** o parágrafo de situação trazia a frase da RC-09 repetida quatro vezes (resíduo de fases anteriores); ficou uma só, e foram acrescentadas as frases da RC-10 e da RC-11. Também foi corrigida em `docs/modelo-de-dados.md` a frase "Não há script `08_`", que ficou errada depois da RC-10.

## Rastreabilidade

| Requisito | Tarefas | Testes / evidência |
|---|---|---|
| FR-001, FR-002, FR-004 (consultar, campos, simulada) | T016 a T018 | `ConsultarConferenciaEndpointTest` |
| FR-003 (destaque dos não verificados e orientações) | T006, T016, T017 | `ConsultarConferenciaEndpointTest`, `OrientacaoSemInformacaoNovaTest` |
| FR-005 (404 idêntico; só paciente) | T008, T011, T018, T021, T030 | `ConsultarConferenciaEndpointTest`, `RegistrarDecisaoEndpointTest`, `MatrizPerfisConferenciaTest` |
| FR-006, FR-007 (confirmar ou divergir; nenhum valor digitado) | T004, T014, T020, T021 | `ConferenciaRequestTest`, `RegistrarDecisaoEndpointTest` |
| FR-008, FR-009 (uma decisão vigente; instante) | T009 a T011 | `RegistrarDecisaoEndpointTest`, `ConcorrenciaConferenciaTest` |
| FR-010, FR-011 (original intacto; sem informação nova) | T006, T011, T023 | `RegistrarDecisaoEndpointTest`, `OrientacaoSemInformacaoNovaTest` |
| FR-012 (orientação: unidade emissora ou arquivo mais nítido) | T006, T024 | `OrientacaoSemInformacaoNovaTest` |
| FR-013 a FR-015 (situação calculada; nunca libera submissão) | T005, T006, T025, T026 | `SituacaoConferenciaTest`, `SituacaoESemEstadoDoPedidoTest` |
| FR-016, FR-017 (leitura desatualizada; pedido submetido) | T011, T020, T027, T029 | `ConferenciaDesatualizadaTest` |
| FR-018 (atômico e correto sob concorrência) | T011, T028, T029 | `ConcorrenciaConferenciaTest`; `ConferenciaOracleTest` (T041) executado no Oracle: 3/3 |
| FR-019 (conteúdo do documento é dado) | T023 | `OrientacaoSemInformacaoNovaTest` (texto de injeção) |
| FR-020, SC-008 (higiene de log) | T011, T020, T031 | `HigieneDeLogConferenciaTest` |
| FR-021 (erro padrão, códigos coerentes) | T014, T020, T032 | `RegistrarDecisaoEndpointTest`, `BancoIndisponivelConferenciaTest` |
| FR-022 (documentação) | T034 a T036 | `CONTRATO_API.md`, `docs/api/*`, `openapi.yaml` 0.9.0, Postman, `docs/*.md` |
| SC-001, SC-003 | T015, T023 | `ConsultarConferenciaEndpointTest`, `OrientacaoSemInformacaoNovaTest` |
| SC-002 | T014, T019 | `ConferenciaRequestTest`, `RegistrarDecisaoEndpointTest` |
| SC-004 | T025 | `SituacaoESemEstadoDoPedidoTest` |
| SC-005 | T027 | `ConferenciaDesatualizadaTest` |
| SC-006 | T028 | `ConcorrenciaConferenciaTest` (lógica em memória; Oracle **não verificado**) |
| SC-007 | T015, T030 | `ConsultarConferenciaEndpointTest`, `MatrizPerfisConferenciaTest` |

Arquivos principais: `main/extracao/{controller/ConferenciaController, service/{ConferenciaService,SituacaoConferencia,OrientacaoConferencia,VisaoConferencia}, repository/{ConferenciaRepository,ConferenciaRepositoryJdbc,...}, dto/{ConferenciaRequest,ConferenciaResponse,...}, entity/{TipoDecisao,MotivoDivergencia,DecisaoConferencia}}`; `ExtracaoRepository.buscarPorId`; `database/scripts/09_conferencia.sql` (+ `01_`, `04_`, `90_`).
