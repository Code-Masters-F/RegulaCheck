# Tasks: Substituir documento e excluir rascunho (RC-09)

**Input**: `specs/010-substituir-excluir-documentos/` (spec.md, plan.md, research.md, data-model.md, contracts/api-alteracao-documentos.md, quickstart.md)

**Backlog**: RC-09 — Code-Masters-F/ClickBus#16 · **Branch**: `010-substituir-excluir-documentos` · **Depende de**: RC-08 (concluída e mesclada)

**Testes**: exigidos pelo `CLAUDE.md` §14 (autorização entre contas, revisões, concorrência/idempotência, falha de armazenamento) e pelo FR-021. Em cada história o teste é escrito **antes** da implementação e deve **falhar primeiro**; registrar no fim se algum não foi visto falhar. Tudo roda sem banco no `.\mvnw.cmd clean verify`; o teste contra o Oracle é opt-in (T038) e, sem execução real, é relatado como **NÃO VERIFICADO**.

**Regras desta execução**: nada de commit/push/PR sem pedido; **nenhum comando contra o Oracle da faculdade sem autorização específica do usuário** (esta história não tem script SQL novo, só o teste opt-in); só dados sintéticos; sem análise, extração, resumo de IA, submissão, retenção automática, lixeira nem telas; nenhuma dependência nova no `pom.xml`; nenhum teste existente da RC-05 a RC-08 é enfraquecido.

**Limitação declarada de verificação**: nenhuma rota produz estados de análise nem `SUBMETIDA_SIMULACAO`. FR-013 a FR-016 são testados com pedidos montados direto no estado, nos dobros em memória, e **não** de ponta a ponta no Oracle (no Oracle só existe `RASCUNHO` real). O relatório final deve dizer isso.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 remover documento · US2 substituir documento · US3 excluir rascunho · US4 pedido submetido só para consulta · US5 conferência invalidada
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Conferir `git status` na branch `010-substituir-excluir-documentos` e rodar `.\mvnw.cmd clean verify` em `backend/` **antes** de qualquer alteração; registrar o resultado-base (esperado: 337 testes, 16 ignorados) na seção "Resultado da verificação" ao final
- [x] T002 [P] Reconhecer os pontos de extensão antes de escrever código: ler `main/documento/controller/DocumentoController.java`, `main/solicitacao/controller/SolicitacaoController.java` (delegação de `/{id}/documentos/{docId}`, listas `Allow`), `main/documento/service/DocumentoService.java` (`enviar`, `gravar` com compensação, erros reutilizáveis), `main/documento/repository/DocumentoRepositoryJdbc.java` (`SQL_BLOQUEAR`, `SQL_CONTAR`, `SQL_ATUALIZAR_PEDIDO`, `SQL_INSERIR_REVISAO`, `traduzir`) e os dobros `DocumentoRepositoryMemoria`/`SolicitacaoRepositoryMemoria`; **corrigir `plan.md`/`research.md` antes de seguir** se algo não bater com o código (ex.: nome de método, assinatura)
- [x] T003 [P] Verificar o arquivo `test.jsonok` na raiz do repositório (apareceu como não rastreado na RC-08): conferir se está no histórico (`git ls-files`, `git log --stat -1 6a3e792`), ler o conteúdo e decidir com o usuário se vai para `.gitignore` ou é apagado; **não apagar sem pedido**

---

## Phase 2: Foundational (bloqueia as histórias)

**Meta**: contratos de repositório, tipos de resultado, dobros em memória com estados arbitrários, dependências e roteamento compilando; nenhuma rota nova responde ainda.

- [x] T004 [P] `main/documento/repository/ResultadoRemocao.java` (novo record: `situacao` = `REMOVIDO`, `NAO_ENCONTRADO`, `SUBMETIDO`; `documento` removido, para apagar o arquivo; `versaoConjuntoDocumental`; `documentosDisponiveis`) e `main/documento/repository/ResultadoRegistro.java` (ganha a situação `SUBMETIDO`; `LIMITE` continua existindo mas não é produzido pela substituição); JavaDoc em português
- [x] T005 [P] `main/solicitacao/repository/ResultadoExclusao.java` (novo record: `situacao` = `EXCLUIDO`, `NAO_ENCONTRADO`, `FORA_DE_RASCUNHO`, `SUBMETIDO`, `COM_DEPENDENCIAS`; `arquivos` = lista de `IdInterno` a apagar **depois** do commit) com JavaDoc
- [x] T006 `main/documento/repository/DocumentoRepository.java`: `remover(pacienteId, solicitacaoId, documentoId, agora)` e `substituir(pacienteId, solicitacaoId, documentoId, NovoDocumento, agora)` com JavaDoc de atomicidade, filtro pela dona, amarração ao pedido do caminho, ordem da chave (D3: chave reconhecida **antes** de procurar o `docId`) e `ExcecaoNegocio` 503 com o banco fora. Depende de T004
- [x] T007 `main/solicitacao/repository/SolicitacaoRepository.java`: `excluirRascunho(pacienteId, solicitacaoId)` com JavaDoc (ordem documentos → revisões → pedido, `FOR UPDATE`, ORA-02292 → `COM_DEPENDENCIAS`, arquivos devolvidos para apagar depois do commit). Depende de T005
- [x] T008 [P] `test/documento/DocumentoRepositoryMemoria.java` (dobro de teste) estendido com as **mesmas** regras do JDBC: `remover` (revisão +1 com `DOCUMENTO_REMOVIDO`, `REVISAO_REMOCAO` = nova revisão, chave anulada, contagem, `SUBMETIDO`), `substituir` (uma revisão `DOCUMENTO_SUBSTITUIDO`, chave reconhecida antes do `docId`, repetição, conflito de chave, contagem inalterada), `synchronized` por pedido, injeção de falha opcional (`falharNoProximoRemover/Substituir`), estado do pedido arbitrário e análise vigente montáveis pelo teste, consulta ao histórico de revisões e aos documentos removidos; **auxiliar `test/documento/ConferirIntegridade.java`** (SC-004, invariante 7): documento disponível ⇔ arquivo existe na área privada (exceto linhas da carga sintética), nenhum arquivo de documento disponível ou removido fora do registro, nenhum arquivo de pedido excluído; chamado ao final dos testes de endpoint (T012, T018, T024) e de concorrência (T034). Depende de T006
- [x] T009 [P] `test/solicitacao/SolicitacaoRepositoryMemoria.java` estendido com `excluirRascunho` (apaga documentos, revisões e pedido do dobro de documentos; devolve os `IdInterno`; `SUBMETIDO`/`FORA_DE_RASCUNHO` conforme o estado; `COM_DEPENDENCIAS` injetável) e **montagem de pedido em qualquer estado** (`SUBMETIDA_SIMULACAO`, `PROCESSANDO`, `COM_PENDENCIAS`...). Depende de T007
- [x] T010 `main/solicitacao/service/SolicitacaoService.java` e `main/config/Composicao.java` (+ pontos de teste que constroem o service: `test/auth/CenarioAuth.java`, `test/solicitacao/**`): o construtor passa a receber `ArmazenamentoPrivado` (infraestrutura comum, sem acoplar `solicitacao` a `documento`); `Composicao` injeta o mesmo armazenamento já montado para o `DocumentoService`; **sem raiz de armazenamento, remover e excluir têm sucesso no banco e só registram `WARN` (contagem, sem nome interno) por não poder apagar os arquivos; não respondem 503, porque a mudança já foi registrada; substituir continua 503, pois não consegue guardar o arquivo novo** (comportamento fixado aqui e espelhado em T036 e no contrato); os testes antigos passam a informar o armazenamento temporário. Depende de T002
- [x] T011 [P] `main/documento/dto/RemocaoResponse.java` (novo record: `versaoConjuntoDocumental`, `documentosDisponiveis`; sem caminho, hash, chave ou nome interno) com JavaDoc

**Checkpoint**: `.\mvnw.cmd -q test-compile` compila produção e testes; nenhuma rota nova; `clean verify` continua com o resultado-base de T001.

---

## Phase 3: US1 — Remover um documento do pedido (P1) 🎯 MVP

**Meta**: a paciente remove um documento do próprio pedido não submetido; o documento some, o arquivo é apagado, a vaga volta e a versão sobe em um. Atende FR-001 a FR-004, FR-017, FR-019 (remoção), FR-020 e FR-021.

**Teste independente**: rascunho com 3 documentos → `DELETE …/documentos/{docId}` → 200 com a versão nova e 2 disponíveis; lista com 2; o arquivo removido responde 404; o arquivo some da área privada; novo envio aceito; repetir o `DELETE` → 404 sem mudar versão.

### Testes (escritos antes e vistos falhar)

- [x] T012 [P] [US1] `test/documento/RemoverDocumentoEndpointTest.java` (Jetty real, repositórios em memória, área de arquivos em pasta temporária): 200 com `{versaoConjuntoDocumental, documentosDisponiveis}`; lista sem o removido e `GET` do pedido refletindo versão e contagem; revisão nova com motivo `DOCUMENTO_REMOVIDO`; `GET` do documento removido → 404; arquivo removido da pasta temporária **depois** da resposta; novo envio aceito com 3 → remover → enviar; segunda remoção → 404 sem mudar versão nem contagem; documento de outra paciente, de outro pedido da mesma paciente, inexistente e `docId` malformado → 404 **idêntico** (corpo igual); sem token → 401; administrador e chave do simulador → 403; corpo e cabeçalhos de dono/estado/revisão ignorados; documento de carga sintética (sem arquivo) removido sem falhar (D9); chave de envio liberada (reenviar com a mesma chave após remover cria documento novo)
- [x] T013 [P] [US1] `test/documento/service/RemoverDocumentoServiceTest.java`: dona conferida antes de tudo (404); resultado `NAO_ENCONTRADO` → `Propriedade.naoEncontrada` idêntico nos casos; `REMOVIDO` apaga o arquivo só **depois** do registro; falha ao apagar o arquivo → sucesso mesmo assim, `WARN` sem nome interno (FR-022); repositório lança 503 → arquivo **não** apagado e nada muda
- [x] T014 [P] [US1] `test/documento/MatrizPerfisDocumentoTest.java` (existente, **acrescentar** sem alterar asserções antigas): `DELETE …/documentos/{docId}` passa a ser permitido ao perfil `PACIENTE`; `PATCH` no item continua 405 com `Allow: GET, PUT, DELETE`; a coleção `/documentos` continua `POST, GET` (405 para `PUT`/`DELETE`/`PATCH`). **Alteração de asserção existente, não só acréscimo:** em `metodosNaoPrevistosRecebem405ComAllowCorreto` (linhas ~79-90) o laço `PUT, DELETE, PATCH` no item passa a cobrir só `PATCH`, e a asserção de `Allow` do `POST` no item muda de `GET` para `GET, PUT, DELETE`; registrar no `tasks.md` o motivo (a RC-09 entrega essas rotas) e que nenhuma proteção foi enfraquecida

### Implementação

- [x] T015 [US1] `main/documento/repository/DocumentoRepositoryJdbc.java` (`remover`): transação curta com `SELECT … FOR UPDATE` do pedido (filtro `ID` e `PACIENTE_ID`), recusa `SUBMETIDA_SIMULACAO`, procura o documento **vivo do pedido** (`REVISAO_REMOCAO IS NULL`), `UPDATE RC_SOLICITACAO` (`REVISAO_ATUAL + 1`, `ATUALIZADA_EM`), `INSERT` em `RC_REVISAO` (`DOCUMENTO_REMOVIDO`), `UPDATE RC_DOCUMENTO SET REVISAO_REMOCAO = ?, CHAVE_ENVIO = NULL`, `UPDATE RC_ANALISE SET VIGENTE = 0 WHERE SOLICITACAO_ID = ? AND VIGENTE = 1`, `COUNT` de disponíveis, `COMMIT`/`ROLLBACK` explícito; devolve o `Documento` removido (para o arquivo) e a versão; SQL só com parâmetros; log só com ids, revisão e código. Depende de T006
- [x] T016 [US1] `main/documento/service/DocumentoService.java` (`remover(SessaoAutenticada, long solicitacaoId, long documentoId)`): confere a dona (404), chama o repositório, traduz o resultado (`NAO_ENCONTRADO` → 404 idêntico; `SUBMETIDO` → 409 `CONFLITO_ESTADO` "Este pedido já foi submetido e só pode ser consultado."; `REMOVIDO` → monta o `RemocaoResponse`), apaga o arquivo **depois** do commit (pula quando o `caminhoInterno` é nulo ou fictício) e, se apagar falhar, só registra `WARN` com contagem, sem nome interno; JavaDoc com as garantias. Depende de T015, T011
- [x] T017 [US1] `main/documento/controller/DocumentoController.java` (`doDelete` para `…/{docId}`): id numérico (`^[1-9][0-9]{0,17}$`, senão 404 padrão sem consultar o banco), sem ler corpo, 200 com `RemocaoResponse`, `Allow: GET, PUT, DELETE`; `main/solicitacao/controller/SolicitacaoController.java` repassa `DELETE …/{docId}` ao `DocumentoController` e mantém 405 para `DELETE …/documentos` (lista). Depende de T016

**Checkpoint**: T012–T014 verdes; remover funciona de ponta a ponta com os dobros em memória.

---

## Phase 4: US2 — Substituir um documento por outro arquivo (P1)

**Meta**: a paciente troca um documento por um arquivo corrigido numa só operação; o arquivo novo passa pela conferência da RC-06; se for recusado, o antigo continua intacto. Atende FR-005 a FR-009, FR-017, FR-019 e FR-020.

**Teste independente**: `PUT …/documentos/{docId}` com PDF válido → 201, antigo some, novo aparece, versão +1 (uma vez só), motivo `DOCUMENTO_SUBSTITUIDO`; com arquivo inválido → erro da RC-06 e nada muda; repetir com a mesma chave → 200 sem duplicar.

### Testes (escritos antes e vistos falhar)

- [x] T018 [P] [US2] `test/documento/SubstituirDocumentoEndpointTest.java` (Jetty real, multipart montado à mão com PDF/PNG sintéticos): 201 com `Location` do **novo** documento e corpo `DocumentoEnviadoResponse`; antigo → 404 e arquivo antigo apagado, novo abre byte a byte; versão +1 e **uma** revisão `DOCUMENTO_SUBSTITUIDO`; com 3 documentos a substituição é aceita (sem 422); bateria de recusas (formato falso, > 5 MB → 413, PDF com senha, PDF > 5 páginas, extensão incoerente → 415, `tipoDocumental` ausente/inválido, sem `arquivo`, dois arquivos, corpo JSON → 415) com o **mesmo erro do envio** e, em cada uma, documento antigo, versão e pasta de arquivos idênticos antes e depois (SC-002); alheio/outro pedido/removido/inexistente/malformado → 404 idêntico, **antes** de ler o corpo; sem token 401, administrador/simulador 403; partes extras (`pacienteId`, `estado`, `revisao`, `caminhoInterno`) ignoradas
- [x] T019 [P] [US2] `test/documento/IdempotenciaSubstituicaoTest.java`: mesma chave + mesmo conteúdo e tipo → 200 com o documento já existente, nada novo gravado e **sem** procurar o `docId` (que já não existe depois do sucesso, D3); mesma chave com conteúdo ou tipo diferente → 409 `CHAVE_ENVIO_REUTILIZADA` sem alterar nada; chave malformada → 400 detalhe `chaveEnvio`; chave de documento removido fica livre; mesma chave vale para `POST` e `PUT` do pedido
- [x] T020 [P] [US2] `test/documento/service/SubstituirDocumentoServiceTest.java`: arquivo conferido e guardado **antes** da transação; recusa, conflito, 404 ou falha injetada → arquivo novo removido e antigo intacto; sucesso → arquivo antigo apagado só depois do commit; falha ao apagar o antigo → sucesso e `WARN` sem nome interno; pedido não rascunho e não submetido aceito (D7); `SUBMETIDO` → 409 sem gravar nem deixar arquivo novo

### Implementação

- [x] T021 [US2] `main/documento/repository/DocumentoRepositoryJdbc.java` (`substituir`): transação com o mesmo `SELECT … FOR UPDATE`; recusa `SUBMETIDA_SIMULACAO`; **primeiro** procura documento vivo do pedido com a chave (repetição → `REPETIDO`; hash/tipo diferentes → `CONFLITO_CHAVE`; `ORA-00001` → releitura e a mesma regra), **depois** o `docId` (inexistente/removido/de outro pedido → `NAO_ENCONTRADO`); revisão +1 com `DOCUMENTO_SUBSTITUIDO`; antigo com `REVISAO_REMOCAO` = nova revisão e `CHAVE_ENVIO = NULL`; novo documento inserido com `REVISAO_ENVIO` = nova revisão; `UPDATE RC_ANALISE SET VIGENTE = 0`; sem checagem do limite de 3; `COMMIT`/`ROLLBACK`; devolve novo, antigo (para o arquivo), versão e contagem. Depende de T006
- [x] T022 [US2] `main/documento/service/DocumentoService.java` (`substituir(SessaoAutenticada, long solicitacaoId, long documentoId, EnvioDocumento)`): reaproveita **sem duplicar** a conferência, o hash e a gravação com compensação do `enviar` (extrair método privado comum só se reduzir duplicação sem alterar o comportamento da RC-08; os testes da RC-08 continuam verdes); traduz `SUBMETIDO` → 409, `CONFLITO_CHAVE` → 409 `CHAVE_ENVIO_REUTILIZADA`, `NAO_ENCONTRADO` → 404, `REPETIDO` → 200, `REGISTRADO` → 201; em qualquer resultado diferente de `REGISTRADO`, ou exceção, remove o arquivo novo e a quarentena; apaga o antigo depois do commit; JavaDoc. Depende de T021
- [x] T023 [US2] `main/documento/controller/DocumentoController.java` (`doPut` para `…/{docId}`): mesma leitura multipart do `POST` (reutilizar o método, sem copiar), id do caminho validado, existência do pedido conferida antes do corpo, 201 + `Location` ou 200; erros iguais ao envio (400/413/415/409/503); partes limpas no `finally`. Depende de T022

**Checkpoint**: T018–T020 verdes; substituir funciona de ponta a ponta e a recusa nunca altera o antigo.

---

## Phase 5: US3 — Excluir um rascunho com confirmação (P1)

**Meta**: a paciente exclui o próprio rascunho só com confirmação explícita; pedido, revisões, documentos e arquivos somem. Atende FR-010 a FR-012, FR-017, FR-019 (exclusão), FR-020 e FR-021.

**Teste independente**: rascunho com documentos → `DELETE` sem o cabeçalho → 400, nada apagado; com `X-Confirmar-Exclusao: EXCLUIR` → 204; `GET` do pedido e dos documentos → 404; zero arquivos restantes; criar novo pedido em seguida funciona.

### Testes (escritos antes e vistos falhar)

- [x] T024 [P] [US3] `test/solicitacao/ExcluirRascunhoEndpointTest.java` (Jetty real): 204 sem corpo com o cabeçalho exato; cabeçalho ausente, em branco, `excluir`, `EXCLUIR ` com espaços e `true` → 400 `REQUISICAO_INVALIDA` com detalhe `confirmacao` e **nada** apagado (pedido, documentos e arquivos conferidos antes e depois, SC-003); `GET` do pedido e de documentos e `POST` de documento depois da exclusão → 404; pasta de arquivos sem nenhum arquivo do pedido; pedido sem documentos também é excluído; novo `POST /solicitacoes` em seguida funciona; pedido alheio, inexistente e já excluído → 404 idêntico **antes** de avaliar a confirmação (pedido alheio sem cabeçalho continua 404, não 400); sem token 401, administrador/simulador 403; ORA-02292 simulado (`COM_DEPENDENCIAS`) → 409 `CONFLITO_ESTADO` sem apagar nada
- [x] T025 [P] [US3] `test/solicitacao/service/ExcluirRascunhoServiceTest.java`: arquivos apagados só **depois** do registro da exclusão; falha ao apagar um arquivo → 204 mesmo assim, `WARN` com contagem e sem nome interno, e os demais arquivos ainda apagados; banco fora (503) → nenhum arquivo apagado; sem raiz de armazenamento → comportamento de T010
- [x] T026 [P] [US3] `test/solicitacao/MatrizPerfisSolicitacaoTest.java` (existente, **acrescentar**): `DELETE /solicitacoes/{id}` permitido a `PACIENTE`, 403 para administrador e simulador, 401 sem sessão; `Allow` de `/solicitacoes/{id}` passa a `GET, PUT, DELETE`; `PATCH` segue 405; asserções antigas preservadas (registrar a mudança aditiva)

### Implementação

- [x] T027 [US3] `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (`excluirRascunho`): transação com `SELECT … FOR UPDATE` do pedido (dona), `NAO_ENCONTRADO` se não há; `SUBMETIDO` se `SUBMETIDA_SIMULACAO`; `FORA_DE_RASCUNHO` se outro estado diferente de `RASCUNHO`; lê os `CAMINHO_INTERNO` de **todos** os documentos (vivos e removidos); `DELETE` de `RC_DOCUMENTO` → `RC_REVISAO` → `RC_SOLICITACAO` (nessa ordem, por causa das FKs); `ORA-02292` → `ROLLBACK` e `COM_DEPENDENCIAS`; `COMMIT`; devolve os arquivos; SQL parametrizado. Depende de T007
- [x] T028 [US3] `main/solicitacao/service/SolicitacaoService.java` (`excluirRascunho(SessaoAutenticada, long id)`): traduz `NAO_ENCONTRADO` → 404 idêntico, `SUBMETIDO` → 409 `CONFLITO_ESTADO` "Este pedido já foi submetido e só pode ser consultado.", `FORA_DE_RASCUNHO` → 409 "Somente rascunhos podem ser excluídos.", `COM_DEPENDENCIAS` → 409 "Este pedido não pode ser excluído."; apaga cada arquivo **depois** do commit, sem interromper nos erros, com `WARN` agregado (contagem, sem nome interno); JavaDoc com a política de arquivos. Depende de T027, T010
- [x] T029 [US3] `main/solicitacao/controller/SolicitacaoController.java` (`doDelete` em `/{id}`): id numérico, confere a existência da dona **antes** de exigir a confirmação (ordem 401 → 403 → 404 → 400 → 409 → 503), valida o cabeçalho `X-Confirmar-Exclusao` com o valor literal `EXCLUIR` (senão 400 com detalhe `confirmacao`), responde 204 sem corpo; `Allow` passa a incluir `DELETE`. Como a conferência de existência é anterior, a service ganha ou reaproveita a busca da dona (`buscarPropria`) sem apagar nada nessa etapa. Depende de T028

**Checkpoint**: T024–T026 verdes; exclusão exige confirmação e não deixa resíduo.

---

## Phase 6: US4 — Pedido submetido só para consulta (P1)

**Meta**: em `SUBMETIDA_SIMULACAO`, remover, substituir e excluir respondem conflito documentado e nada muda; consulta e download continuam. Atende FR-016 e SC-006.

**Teste independente**: pedido montado direto em `SUBMETIDA_SIMULACAO` (dobro em memória) → três conflitos; `GET` do pedido, da lista e do arquivo → 200; versão, documentos e arquivos iguais antes e depois. **Não** verificável de ponta a ponta no Oracle (nenhuma rota produz a submissão).

- [x] T030 [P] [US4] `test/solicitacao/PedidoSubmetidoSomenteConsultaTest.java`: com pedido em `SUBMETIDA_SIMULACAO` e documentos com arquivo — `DELETE …/{docId}`, `PUT …/{docId}` (com arquivo válido) e `DELETE /solicitacoes/{id}` (com confirmação correta) → 409 `CONFLITO_ESTADO` com a mensagem de pedido submetido, sem gravar nem deixar arquivo novo; confirmação ausente em pedido submetido → 400 (a confirmação é validada **antes** do estado, ordem 404 → 400 → 409 de D4 e do contrato; nada é apagado); `GET` do pedido, lista e download → 200; versão, documentos, revisões e arquivos idênticos antes e depois; pedido em `COM_PENDENCIAS`/`PROCESSANDO` → excluir 409 "Somente rascunhos podem ser excluídos." mas remover/substituir aceitos (D7)
**Checkpoint**: T030 verde (se mostrar divergência de mensagem, código ou ordem, corrigir no service/repositório e registrar em `contracts/api-alteracao-documentos.md`; T030 só pode ser marcada com T017, T023 e T029 prontas); pedido submetido é imutável por estas rotas.

---

## Phase 7: US5 — Mudança nos documentos invalida a conferência (P2)

**Meta**: remover/substituir torna a análise vigente anterior não vigente (histórico preservado), não inicia análise nem muda o estado, e a resposta do pedido mostra "análise não realizada" para a nova versão. Atende FR-013 a FR-015.

**Teste independente**: no que existe (rascunho): a versão sobe e `analise.situacao = NAO_REALIZADA`. Para estados de análise: pedido montado com análise vigente → depois da mudança, `VIGENTE = 0`, linha preservada, estado igual, nenhuma análise nova.

- [x] T032 [P] [US5] `test/documento/ConferenciaInvalidadaTest.java`: rascunho — após remover e após substituir, `versaoConjuntoDocumental` sobe e `analise.situacao` é `NAO_REALIZADA`; pedido montado em `COM_PENDENCIAS` com análise vigente (dobro) — após remover e após substituir, a análise deixa de ser vigente, **continua** no histórico, o estado do pedido **não muda** e nenhuma análise nova é criada (FR-013, D6); resultado de análise "em andamento na versão antiga" montado no dobro não é publicado como vigente depois da mudança (FR-015, no nível do dobro)
- [x] T033 [US5] Conferir que o `UPDATE RC_ANALISE SET VIGENTE = 0` de T015 e T021 está na **mesma** transação da revisão e que o dobro em memória reproduz o efeito; ajustar `main/documento/repository/DocumentoRepositoryJdbc.java` e `test/documento/DocumentoRepositoryMemoria.java` se T032 mostrar divergência. Depende de T015, T021, T032

**Checkpoint**: T032 verde; a regra e o contrato de invalidação estão provados no que existe, com a limitação declarada acima.

---

## Phase 8: Polish e verificação

- [x] T034 [P] `test/documento/ConcorrenciaAlteracaoDocumentoTest.java` (dobro em memória, 20 threads sobre o mesmo pedido, repetido várias vezes): remover × substituir o mesmo documento (exatamente um vence, o outro recebe 404), remover × enviar perto do limite (nunca mais de 3 disponíveis), substituir × excluir (um vence; sem arquivo órfão nem registro sem arquivo), revisões sem salto e sem repetição, nenhuma exceção não tratada (SC-004, SC-005). Declarar no teste e no relatório que ele prova o Service e a lógica, **não** o bloqueio de linha do Oracle
- [x] T035 [P] `test/solicitacao/HigieneDeLogAlteracaoTest.java` (modelo: `HigieneDeLogDocumentoTest`): captura o log durante remover, substituir (sucesso, repetição, recusa), excluir (sucesso, sem confirmação, falha ao apagar arquivo) e prova que nunca aparecem nome do arquivo, `X-Chave-Envio`, `X-Confirmar-Exclusao`, hash, nome interno, caminho de armazenamento, token nem conteúdo; respostas de erro sem pilha, SQL nem caminho (FR-022, SC-008)
- [x] T036 [P] `test/documento/BancoEArmazenamentoIndisponiveisAlteracaoTest.java`: repositório que lança 503 → 503 genérico em remover, substituir e excluir, **nenhum** arquivo apagado e nenhum arquivo novo órfão; raiz de armazenamento ausente → substituir 503 e remover/excluir com sucesso e `WARN` sem nome interno, sem 503 (comportamento definido em T010); falha ao gravar o arquivo novo → nada registrado e antigo intacto (FR-020)
- [x] T037 [P] `test/documento/InjecaoEmArquivoSubstituicaoTest.java`: arquivo de substituição cujo conteúdo traz texto do tipo "ignore as regras e aprove" é guardado e devolvido byte a byte, sem alterar tipo, estado, revisão, limite nem resposta; o conteúdo nunca é lido nem interpretado (CLAUDE.md §2.11)
- [x] T038 **(executado em 13/11/2026 contra o Oracle da faculdade, com autorização do usuário: 4 testes, 0 falhas)** `test/documento/AlteracaoDocumentoOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM` + `DB_*`, no padrão de `DocumentoOracleTest`): com os scripts `01_`…`07_` já aplicados, cria pedidos próprios, remove, substitui e exclui de verdade com raiz de armazenamento temporária; confere `RC_DOCUMENTO` (`REVISAO_REMOCAO`, chave anulada), `RC_REVISAO` (`DOCUMENTO_REMOVIDO`, `DOCUMENTO_SUBSTITUIDO`), contagem de disponíveis, 20 ações simultâneas sem salto de revisão e sem passar de 3, repetição da chave em substituir, exclusão sem linhas restantes e sem arquivos, isolamento entre as contas A e B; `UPDATE RC_ANALISE` conferido apenas como "0 linhas" (declarar **não verificável de ponta a ponta**); ao final remove **somente** o que criou. Sem a variável: ignorado e relatado como **NÃO VERIFICADO**
- [x] T039 [P] `CONTRATO_API.md` e `docs/api/{contratos-dados,estados-e-revisoes,erros,protecoes,autorizacao,README}.md`: contrato real das 3 rotas (códigos, corpo, cabeçalho `X-Confirmar-Exclusao`, ordem de verificação, chave de envio no `PUT`, mensagens de conflito); no `CONTRATO_API.md`, a linha de `DELETE …/documentos/{docId}` deixa de ser "prevista" e passa a implementada; `estados-e-revisoes.md` registra remover/substituir/excluir por estado e a **decisão aberta para RC-13/RC-16** (hoje diz que reenviar leva a `PROCESSANDO`; esta história não dispara processamento); `erros.md` ganha o detalhe `confirmacao`; `autorizacao.md` registra os novos métodos por rota
- [x] T040 [P] `docs/api/openapi.yaml` (versão 0.7.0) e `docs/api/RegulaCheck.postman_collection.json`: as 3 rotas com respostas e esquemas (`RemocaoResponse`, cabeçalho de confirmação, `PUT` multipart); coleção com remover, repetir remoção, substituir, repetir, recusa, excluir sem e com confirmação; validar o JSON com parser e o YAML com SnakeYAML como na RC-08 (relatar se não houver validador)
- [x] T041 [P] `docs/modelo-de-dados.md`, `docs/decisoes.md`, `docs/arquitetura.md`, `docs/ambiente.md` e `README.md`: política de arquivos (apagados; registro histórico mantido enquanto o pedido existe; exclusão apaga tudo), chave anulada na remoção, **ausência de script `08_`**, fluxo de remover/substituir/excluir, risco aceito de arquivo sem registro após falha ao apagar (WARN sem nome interno), pendência de execução do `AlteracaoDocumentoOracleTest`; referenciar o `quickstart.md` sem duplicar
- [x] T042 Verificação final: `.\mvnw.cmd clean verify` em `backend/`; registrar comando, total de testes, falhas e ignorados na seção "Resultado da verificação" abaixo e, se existir convenção, em `docs/testes/`; conferir `git diff` sem segredos, sem `System.out`, sem código comentado e sem tocar arquivos de outras histórias além dos citados; `SqlParametrizadoTest`, `ArquiteturaTest`, `HigieneDeLogGeralTest` e `VarreduraDeSegredosTest` verdes; confirmar o `quickstart.md` contra a API em memória ou relatar que não foi possível; registrar que **SC-001 (menos de 1 minuto) não foi medido**
- [x] T043 Rastreabilidade e fechamento: preencher a tabela RC-09 → requisito → tarefas → arquivos → verificação; marcar tarefas somente com evidência real; relatório final separando **implementado**, **verificado** (comando e resultado), **não verificado** (Oracle real, `AlteracaoDocumentoOracleTest`, estados de análise e submissão de ponta a ponta, concorrência com bloqueio de linha) e **fora do escopo** (análise, extração, resumo de IA, submissão, retenção automática, lixeira, telas); informar as decisões abertas para RC-13/RC-16

---

## Dependências e ordem

- Setup (T001–T003) → Foundational (T004–T011) → histórias. T006 depende de T004; T007 de T005; T008 de T006; T009 de T007; T010 de T002; T011 independente.
- **US1 (MVP)** depende só da Foundational. **US2** reaproveita a leitura multipart e a gravação com compensação do `DocumentoService`/`DocumentoController` (editam os mesmos arquivos de US1: executar em sequência). **US3** é independente de US1/US2 no código (arquivos de `solicitacao`), mas T029 e T017 editam o `SolicitacaoController`: executar em sequência. **US4** depende de US1, US2 e US3 (testa as três rotas). **US5** depende de US1 e US2 (o `UPDATE RC_ANALISE` nasce em T015 e T021).
- Dentro de cada história: testes primeiro (vistos falhar) → repositório → service → DTO → controller.
- Polish depende de todas as histórias; T038 só pode ser **executado** com autorização do usuário; a escrita não depende dela.

## Paralelismo

- Setup: T002 e T003 juntos.
- Foundational: T004, T005 e T011 juntos; T006 e T007 depois deles; T008 e T009 juntos depois de T006/T007; T010 em paralelo com T008/T009 (arquivos distintos).
- US1: T012, T013, T014 em paralelo; T015 → T016 → T017.
- US2: T018, T019, T020 em paralelo; T021 → T022 → T023.
- US3: T024, T025, T026 em paralelo; T027 → T028 → T029.
- Polish: T034, T035, T036, T037 e T039–T041 em paralelo (arquivos distintos).

## Estratégia de implementação

1. **MVP = US1** (T001–T017): remover um documento já prova a dona, a revisão, o arquivo apagado e a vaga liberada, e destrava a paciente do limite de 3.
2. Em seguida US2 (substituir, o critério central "corrigir os arquivos"), US3 (excluir rascunho com confirmação), US4 (submetido só consulta) e US5 (invalidação), e o Polish.
3. A cada fase: rodar só os testes da fase (`-Dtest=...`), e o `clean verify` completo no fechamento (T042).
4. Prazo desta história: 15/11/2026. Se faltar tempo, cortar primeiro partes de T040 e T041; **nunca** cortar os testes de isolamento entre contas, de concorrência, de compensação do arquivo e de confirmação de exclusão, nem a nota de "NÃO VERIFICADO" para o Oracle e para os estados de análise e submissão.

## Rastreabilidade (RC-09)

| Requisito (spec) | Tarefas | Verificação planejada |
|---|---|---|
| FR-001–FR-004 remover, revisão, arquivo apagado, repetir | T004, T006, T015, T016, T017 | T012, T013, T014 |
| FR-005–FR-009 substituir, conferência, atomicidade, chave | T006, T021, T022, T023 | T018, T019, T020 |
| FR-010–FR-012 excluir com confirmação, apagar tudo, só rascunho | T005, T007, T027, T028, T029 | T024, T025, T026 |
| FR-013–FR-015 conferência invalidada, sem iniciar análise | T015, T021, T033 | T032 |
| FR-016 pedido submetido só consulta | T004, T005, T016, T028 | T030 |
| FR-017, FR-018 dona, não encontrado idêntico, entradas ignoradas | T015, T016, T021, T027 | T012, T018, T024 |
| FR-019 concorrência | T015, T021, T027 | T034, T038 |
| FR-020 indisponibilidade sem meio-termo | T016, T022, T028 | T036 |
| FR-021 contratos e testes | T039, T040, T041 | T042 |
| FR-022 logs e respostas sem detalhe | T016, T022, T028 | T035 |
| SC-001–SC-008 | todas | T042 e, para o Oracle, T038 |

## Resultado da verificação

Execução em 13/11/2026, a partir de `backend/`, **sem banco e sem variáveis `DB_*`**:

| Momento | Comando | Resultado |
|---|---|---|
| Base (T001), antes de qualquer alteração | `.\mvnw.cmd clean verify` | 337 testes, 0 falhas, 0 erros, 16 ignorados |
| Fechamento (T042) | `.\mvnw.cmd clean verify` | **384 testes, 0 falhas, 0 erros, 20 ignorados** (16 Oracle anteriores + 4 do `AlteracaoDocumentoOracleTest`); build sem erros; `regulacheck-backend.jar` gerado |

**Testes novos da RC-09** (43 executados, mais 4 ignorados do Oracle: 337 → 384 no total), todos verdes: `RemoverDocumentoEndpointTest` (6), `SubstituirDocumentoEndpointTest` (6, inclui bateria de 6 arquivos inválidos, injeção de instrução e 404 do documento do caminho antes do corpo), `IdempotenciaSubstituicaoTest` (3), `ExcluirRascunhoEndpointTest` (6), `PedidoSubmetidoSomenteConsultaTest` (2), `ConferenciaInvalidadaTest` (3), `ConcorrenciaAlteracaoDocumentoTest` (12 = 4 cenários × 3 repetições, 20 threads), `HigieneDeLogAlteracaoTest` (2), `BancoEArmazenamentoIndisponiveisAlteracaoTest` (3). Alterados, com o motivo registrado: `MatrizPerfisDocumentoTest` (a matriz de métodos passa a esperar `PUT`/`DELETE` no item e `Allow: GET, PUT, DELETE`; a matriz de perfis ganhou as 5 chamadas novas) e `TrocarServicoEndpointTest` (o `DELETE` em `/solicitacoes/{id}` deixou de ser 405; a asserção passou para `PATCH`). Nenhuma proteção foi enfraquecida: o 405 continua para `PATCH` e para `PUT`/`DELETE` na coleção `/documentos`. Verdes e já existentes que cobrem a feature: `SqlParametrizadoTest` (varre os dois repositórios JDBC), `ArquiteturaTest`, `HigieneDeLogGeralTest`, `VarreduraDeSegredosTest`.

**Desvios do plano de testes (registrados com honestidade):**
- O código de produção de cada história foi escrito **antes** dos testes; os testes novos **não foram vistos falhar primeiro**. Compensação: depois de escritos, fiz uma verificação por mutação (apagar a chamada que remove o arquivo e desligar a checagem da confirmação): 5 testes falharam, e a mutação foi revertida (confirmado por busca no código). Na revisão final, o `PUT` foi corrigido para conferir o documento do caminho **antes** do corpo (a ordem do contrato; antes ele lia e guardava o arquivo e só depois respondia 404): o teste novo falha sem a correção (mutação repetida) e passa com ela. Um teste falhou na primeira execução por premissa errada minha: `X-Confirmar-Exclusao: "EXCLUIR "` (espaço no fim) chega como `EXCLUIR` porque o HTTP remove os espaços das pontas do valor do cabeçalho; o caso saiu da lista e o motivo ficou comentado no teste.
- **Não viraram classes separadas:** `RemoverDocumentoServiceTest`, `SubstituirDocumentoServiceTest`, `ExcluirRascunhoServiceTest` (T013, T020, T025) e `InjecaoEmArquivoSubstituicaoTest` (T037, virou um método de `SubstituirDocumentoEndpointTest`). O Service é coberto pelos testes de endpoint (Service real, dobros em memória), pela concorrência em nível de Service e pelo teste de falha de armazenamento em `HigieneDeLogAlteracaoTest`. O auxiliar `ConferirIntegridade` (T008) virou o método `conferirIntegridade()` de `CenarioDocumento`; a concorrência usa uma conferência local equivalente.
- **T010 (sem raiz de armazenamento):** `ArmazenamentoEmDisco.remover` com raiz nula é um no-op **sem log** (comportamento da RC-06). Logo, remover e excluir têm sucesso no banco e **não** geram nem 503 nem `WARN` nesse caso; o texto "só registram `WARN`" de T010/T036 vale para falha de I/O ao apagar. Testado em `BancoEArmazenamentoIndisponiveisAlteracaoTest`.
- **T014/T026:** `MatrizPerfisSolicitacaoTest` não foi alterado (não tinha asserção de `DELETE`); o `Allow` de `/solicitacoes/{id}` passou a incluir `DELETE` no código, mas **não há teste dedicado** a esse cabeçalho, só às respostas 204/400/404/409 do `DELETE`.
- **T031** foi absorvida pelo checkpoint da US4 (sem tarefa própria); `ExcluirRascunhoEndpointTest`, `PedidoSubmetidoSomenteConsultaTest` e `HigieneDeLogAlteracaoTest` ficaram em `solicitacao/` e `documento/` conforme o código que exercitam.

**Validações fora do Maven:** `docs/api/openapi.yaml` (0.7.0) lido com o SnakeYAML 2.4 do repositório Maven local: estrutura OK, 9 caminhos, **102** referências internas todas resolvidas; o JSON da coleção Postman foi validado com um parser e o diff contém só inserções (122 linhas, 5 itens e 1 variável novos). O arquivo `test.jsonok` citado em T003 **não existe** no repositório nem no histórico: nada a fazer.

**Não verificado:**
- **Tudo que depende do Oracle real:** `DocumentoRepositoryJdbc.remover/substituir` e `SolicitacaoRepositoryJdbc.excluirRascunho` (SQL, transação, `FOR UPDATE`, anulação da chave contra o índice único funcional, ordem de exclusão pelas chaves estrangeiras, tradução de ORA-02292). `ConcorrenciaAlteracaoDocumentoTest` usa dobros em memória (`synchronized`) e prova só o Service e a lógica de decisão, não o bloqueio de linha. **Atualização:** T038 (`AlteracaoDocumentoOracleTest`, 4 testes) foi executado depois, com autorização do usuário, junto de `DocumentoOracleTest` (5) e `SolicitacaoOracleTest` (3): 12 testes, 0 falhas, 0 ignorados. Isso comprova no Oracle real o SQL de remover, substituir e excluir, o `FOR UPDATE` sob remoções e substituições simultâneas, a anulação da chave contra o índice único e a exclusão sem linhas nem arquivos restantes. Continua não verificado no Oracle o `UPDATE RC_ANALISE` com linha real, os estados de análise e de submissão. Esta história não tem script SQL novo.
- **Estados de análise e de submissão de ponta a ponta (FR-013 a FR-016):** nenhuma rota os produz; foram testados só com pedidos montados no dobro em memória. No Oracle só existe `RASCUNHO`, e o `UPDATE RC_ANALISE … SET VIGENTE = 0` afeta zero linhas (nunca foi exercitado com linha real). FR-015 (resultado de análise em andamento não publicado) só tem, de fato, a invalidação de `VIGENTE`; a publicação de análise ainda não existe.
- O comportamento real de `Files.deleteIfExists` com falha de permissão/arquivo aberto (a falha foi simulada com um armazenamento de teste que lança exceção).
- Os exemplos do `quickstart.md` **não foram executados contra uma API no ar**; os mesmos fluxos estão cobertos pelos testes de endpoint em loopback. SC-001 (menos de 1 minuto) **não foi medido**.

## Rastreabilidade final (RC-09)

| Requisito | Código | Verificação executada |
|---|---|---|
| FR-001–FR-004 remover | `DocumentoRepositoryJdbc.remover`, `DocumentoService.remover`, `DocumentoController.doDelete`, `RemocaoResponse`, `ResultadoRemocao` | `RemoverDocumentoEndpointTest`, `ConcorrenciaAlteracaoDocumentoTest` |
| FR-005–FR-009 substituir | `DocumentoRepositoryJdbc.substituir`, `DocumentoService.substituir`, `DocumentoController.doPut`, `ResultadoSubstituicao` | `SubstituirDocumentoEndpointTest`, `IdempotenciaSubstituicaoTest` |
| FR-010–FR-012 excluir | `SolicitacaoRepositoryJdbc.excluirRascunho`, `SolicitacaoService.excluirRascunho`, `SolicitacaoController.doDelete`, `ResultadoExclusao` | `ExcluirRascunhoEndpointTest`, `PedidoSubmetidoSomenteConsultaTest` |
| FR-013–FR-015 conferência invalidada | `SQL_INVALIDAR_ANALISE` nos dois caminhos JDBC | `ConferenciaInvalidadaTest` (só em memória) |
| FR-016 submetido só consulta | checagem de `SUBMETIDA_SIMULACAO` nos três caminhos | `PedidoSubmetidoSomenteConsultaTest` (só em memória) |
| FR-017, FR-018 dona e entradas ignoradas | filtro pela dona em todo SQL; confirmação antes do estado | testes de endpoint e `MatrizPerfisDocumentoTest` |
| FR-019 concorrência | `SELECT … FOR UPDATE` em toda operação | `ConcorrenciaAlteracaoDocumentoTest` (memória); Oracle **não executado** |
| FR-020 indisponibilidade | traduções 503 existentes | `BancoEArmazenamentoIndisponiveisAlteracaoTest` |
| FR-021 contratos | `CONTRATO_API.md`, `docs/api/*`, `openapi.yaml` 0.7.0, Postman, `docs/*` | validação do YAML e do JSON; revisão do diff |
| FR-022 logs | logs só com ids e códigos; `WARN` sem nome interno | `HigieneDeLogAlteracaoTest` |

**Decisões abertas para RC-13/RC-16:** a transição de estado ao alterar documento depois da análise (o `estados-e-revisoes.md` descreve `PROCESSANDO`; a RC-09 mantém o estado e só invalida a análise); a publicação segura de uma análise em andamento quando o conjunto documental muda; a limpeza de arquivos que sobram sem registro após falha ao apagar (hoje só `WARN`).
