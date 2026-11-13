# Tasks: Enviar e consultar documentos (RC-08)

**Input**: `specs/009-enviar-consultar-documentos/` (spec.md, plan.md, research.md, data-model.md, contracts/api-documentos.md, quickstart.md)

**Backlog**: RC-08 — Code-Masters-F/ClickBus#15 · **Branch**: `009-enviar-consultar-documentos`

**Testes**: solicitados pelo `CLAUDE.md` §14 (autorização entre contas, revisões, concorrência/idempotência) e pelo plano. Em cada história o teste é escrito **antes** da implementação e deve falhar primeiro. Tudo roda sem banco no `.\mvnw.cmd clean verify`; o único teste contra o Oracle é opt-in (T034) e, sem execução real, é relatado como **NÃO VERIFICADO**.

**Desvios do plano de testes (registrados com honestidade):**
- Na execução, o código de produção de cada história foi escrito **antes** de parte dos testes; os testes de US1–US4 **não foram vistos falhar primeiro**. Os dois únicos testes que falharam e foram corrigidos foram os da RC-07 que listam os campos exatos do pedido (T028, falha esperada pela mudança aditiva) e um teste de concorrência com defeito próprio (cada thread gerava um PDF diferente, e o PDFBox grava datas no arquivo, o que mudava o hash), corrigido no teste e repetido 6 vezes sem falha.
- Os testes de Service unitários citados em T010, T017 e T022 **não viraram classes `*ServiceTest` separadas**: o comportamento do Service é coberto pelos testes de endpoint (Service real, dobros em memória) e, ao nível do Service puro, por `ConcorrenciaEnvioDocumentoTest`. T024 foi fundida em `IdempotenciaEnvioTest`; T032 em `BancoEArmazenamentoIndisponiveisTest` mais os casos de armazenamento ausente de `ListarAbrirDocumentoEndpointTest`.
- T008 não ganhou caso dedicado em `RotasPrivadasTest`: a sobrecarga com `MultipartConfigElement` é exercida por todos os testes de endpoint de documentos.

**Regras desta execução**: nada de commit/push sem pedido; nenhum comando é executado no Oracle da faculdade pelo agente sem autorização específica do usuário (o script 07 é do grupo, salvo autorização expressa); só dados sintéticos (arquivos gerados pelos testes); sem extração, análise, resumo, substituir/remover documento, nem tela; nenhuma dependência nova no `pom.xml`.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 anexar documento · US2 consultar e abrir · US3 recusar inválido e repetir envio · US4 versão do conjunto documental e distinção de análise
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Conferir `git status` na branch `009-enviar-consultar-documentos`, rodar `.\mvnw.cmd clean verify` em `backend/` **antes** de qualquer alteração e registrar o resultado-base (quantos testes passam e quantos Oracle são ignorados; esperado: 280 e 11)
- [x] T002 **Reconhecimento do multipart** (resolve os "confirmar" de research D2): teste descartável ou permanente em `test/documento/MultipartJettyTest.java`, com Jetty real em porta livre e um servlet mínimo sob `/solicitacoes/*`, que prove em Jetty 12.1.15 EE10: (a) `ServletHolder.getRegistration().setMultipartConfig(MultipartConfigElement)` vale para requisição roteada por `PathInfo`; (b) o tipo exato de exceção quando o arquivo passa de `maxFileSize` e quando o corpo passa de `maxRequestSize`; (c) as partes gravadas em `location` são apagadas ao fim da requisição, inclusive em erro; (d) requisição que não é multipart faz `getPart` lançar o que o controller precisa tratar. Registrar o resultado em `research.md` (D2), ajustando o plano se alguma suposição falhar **antes** de seguir
- [x] T003 [P] `database/scripts/07_documento_tipo_e_tentativa.sql` (novo, aditivo): `ALTER TABLE RC_DOCUMENTO ADD` de `TIPO_DOCUMENTAL VARCHAR2(30 CHAR) DEFAULT 'OUTRO' NOT NULL`, `CHAVE_ENVIO VARCHAR2(64 CHAR)` e `HASH_CONTEUDO VARCHAR2(64 CHAR)`, mais `RC_DOCUMENTO_CK_TIPO_DOC`, `RC_DOCUMENTO_CK_HASH` e `RC_DOCUMENTO_UK_CHAVE` (índice único funcional por pedido, ver data-model.md); cabeçalho no padrão do `06_` (finalidade, ordem: depois do `01_` e do `06_`, efeito aditivo, quando **não** rodar: banco criado com o `01_` novo, o que acontece se falhar no meio, banco compartilhado, prefixo `RC_`, sem o script o envio falha ao gravar). Mesmas colunas e restrições em `database/scripts/01_criar_esquema.sql` (instalação nova); `database/scripts/04_testes_restricoes.sql` ganha casos de tipo documental inválido (ORA-02290), hash malformado (ORA-02290) e chave de envio duplicada no mesmo pedido (ORA-00001), com `ROLLBACK` e a conferência final de 77 linhas preservada; linha do `07_` na tabela de ordem de `database/scripts/README.md`. **Escrito, não executado** (cabe ao grupo ou a autorização específica)

---

## Phase 2: Foundational (bloqueia as histórias)

**Meta**: tipos, contratos de repositório, implementação JDBC, dobro em memória, montagem das dependências e configuração multipart, tudo compilando; nenhuma rota ainda.

- [x] T004 [P] `main/documento/entity/{TipoDocumental,EstadoEnvio,Documento}.java`: `TipoDocumental` enum fechado (`ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL`, `OUTRO`) com `de(String)` que devolve `Optional` vazio fora do conjunto; `EstadoEnvio` enum com `ENVIADO`; `Documento` record (`id`, `solicitacaoId`, `nomeArquivo`, `tipoDocumental`, `formato` = `TipoArquivo`, `tamanhoBytes`, `paginas`, `revisaoEntrada`, `enviadoEm`, `caminhoInterno` = `IdInterno`, `hashConteudo`, `chaveEnvio`); JavaDoc em português; `test/documento/EntidadesDocumentoTest.java` escrito antes (valores do enum iguais aos da restrição `RC_DOCUMENTO_CK_TIPO_DOC`; `de("encaminhamento")` e `de("")` → vazio; `Documento` não expõe caminho em `toString`)
- [x] T005 [P] `main/documento/repository/{DocumentoRepository,ResultadoRegistro}.java`: `registrar(pacienteId, solicitacaoId, NovoDocumento, agora)` atômico; `listarDoPedido(solicitacaoId, pacienteId)`; `buscarDoPedido(docId, solicitacaoId, pacienteId)`; `buscarPorChave(solicitacaoId, pacienteId, chave)`; `ResultadoRegistro` com `Situacao` (`NAO_ENCONTRADO`, `FORA_DE_RASCUNHO`, `LIMITE`, `CONFLITO_CHAVE`, `REPETIDO`, `REGISTRADO`) e o `Documento`/`Solicitacao` quando houver; JavaDoc descrevendo atomicidade, filtro pela dona, amarração ao pedido do caminho e `ExcecaoNegocio` 503 quando o banco está fora
- [x] T006 [P] `main/documento/repository/DocumentoRepositoryJdbc.java`: `registrar` em transação curta — `SELECT … FOR UPDATE` do pedido (só colunas de `RC_SOLICITACAO`, filtro por `ID` e `PACIENTE_ID`), conferir estado `RASCUNHO`, procurar a chave (repetição → `REPETIDO` se hash e tipo iguais, senão `CONFLITO_CHAVE`), `COUNT(*)` de documentos com `REVISAO_REMOCAO IS NULL` (≥ 3 → `LIMITE`), `UPDATE` de `REVISAO_ATUAL + 1`/`ATUALIZADA_EM`, `INSERT` em `RC_REVISAO` com `DOCUMENTO_ENVIADO` e `INSERT` em `RC_DOCUMENTO` com `REVISAO_ENVIO` = nova revisão, tudo com `COMMIT`/`ROLLBACK` explícito; violação `ORA-00001` na chave vira releitura e a mesma regra de repetição; `ORA-00904` (coluna ausente) vira erro interno genérico com orientação a executar o `07_` apenas no log; `listarDoPedido` e `buscarDoPedido` com junção até `RC_SOLICITACAO.PACIENTE_ID = ?` na própria instrução e `REVISAO_REMOCAO IS NULL`, `ORDER BY ENVIADO_EM, ID`. Nenhuma concatenação de entrada em SQL; logs só com ids, revisão, tipo de arquivo, tamanho e código
- [x] T007 [P] `test/documento/DocumentoRepositoryMemoria.java` (dobro de teste): replica as **mesmas** regras do JDBC (dona, amarração ao pedido, as 6 situações de `registrar`, revisão +1 refletida em `SolicitacaoRepositoryMemoria`, histórico de revisões consultável pelo teste, contagem ≤ 3) com `synchronized` por pedido; injeção de falha opcional (`falharNoProximoRegistro`) para testar compensação; `ArquiteturaTest` existente continua verde
- [x] T008 `main/auth/autorizacao/RotasPrivadas.java`: sobrecarga `registrar(caminho, servlet, MultipartConfigElement, Perfil...)` que aplica a configuração ao `ServletHolder`; a versão antiga delega sem mudar comportamento; `test/auth/autorizacao/RotasPrivadasTest.java` ganha casos da sobrecarga (recusa perfil vazio e caminho repetido como antes). Depende de T002
- [x] T009 `main/config/Dependencias.java`, `main/config/Servidor.java`, `main/config/Composicao.java` e os pontos de teste que constroem `Dependencias` (`test/auth/CenarioAuth.java`, `test/common/http/CabecalhosSegurosTest.java`, `test/seguranca/HigieneDeLogGeralTest.java`): `Dependencias` ganha `documentos`; `Servidor.criar(Configuracao)` monta `DocumentoRepositoryJdbc`; `Composicao` monta `ArmazenamentoPrivado`/`ConferenciaDeArquivo` a partir de `Configuracao.armazenamentoRaiz()` (snippet do passo 0 de `docs/api/protecoes.md`; sem raiz, a conferência usa quarentena temporária e `guardar`/`abrir` respondem 503), constrói o `MultipartConfigElement` (`location` = pasta privada de quarentena, `maxFileSize` = `LimitesArquivo.MAXIMO_BYTES`, `maxRequestSize` = limite + 64 KB, `fileSizeThreshold` = 0); os testes passam o dobro em memória e uma raiz temporária por cenário em `CenarioAuth`. `ConferenciaDeArquivo.para(raiz)` lança `IOException` e `Composicao.montar` não declara exceção: converter em `IllegalStateException` com mensagem clara ("não foi possível preparar a pasta privada de arquivos"), sem caminho completo na mensagem, e cobrir com um teste (raiz apontando para um arquivo comum). Depende de T005, T006, T007, T008
- [x] T028 [P] **(antecipada da US4: o contrato do envio já devolve estes campos)** ajuste dos testes da RC-07 que constroem `Solicitacao` ou comparam o corpo do pedido (`test/solicitacao/**`, `SolicitacaoRepositoryMemoria`, `EntidadesTest`): passam a informar `documentosDisponiveis` e aceitam os campos novos; nenhum teste da RC-07 é enfraquecido (as asserções antigas permanecem)
- [x] T029 **(antecipada da US4)** `main/solicitacao/entity/Solicitacao.java`, `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` e `main/solicitacao/dto/SolicitacaoResponse.java`: `Solicitacao` ganha `documentosDisponiveis` (o `SolicitacaoRepositoryMemoria` obtém a contagem do `DocumentoRepositoryMemoria` de T007); `COLUNAS_PEDIDO` ganha subconsulta `(SELECT COUNT(*) FROM RC_DOCUMENTO d WHERE d.SOLICITACAO_ID = s.ID AND d.REVISAO_REMOCAO IS NULL)` (uma consulta só, sem N+1 na lista de até 50); `SolicitacaoResponse` acrescenta `versaoConjuntoDocumental` (= `revisao`), `documentosDisponiveis` e `analise { situacao: NAO_REALIZADA, mensagem }`, derivada de o pedido ainda não ter análise (nesta história sempre); o `criar`/`trocarProtocolo` devolvem `documentosDisponiveis` coerente. Depende de T007 e T028

**Checkpoint**: `.\mvnw.cmd -q test-compile` compila produção e testes; nenhuma rota nova ainda; o build completo continua verde (inclusive os testes da RC-07, já com os campos novos do pedido).

---

## Phase 3: US1 — Anexar um documento ao rascunho (P1) 🎯 MVP

**Meta**: a paciente envia um arquivo válido ao próprio rascunho com o tipo documental, e a revisão do pedido aumenta. Atende FR-001 a FR-007, FR-020, FR-021 e FR-024.

**Teste independente**: login como paciente sintética → rascunho (RC-07) → `POST /solicitacoes/{id}/documentos` com PDF e imagem sintéticos → conferir 201, dono, tipo documental, estado do envio, revisão +1, motivo `DOCUMENTO_ENVIADO`, pedido ainda em `RASCUNHO`; administrador, simulador e anônimo recusados.

### Testes (escritos antes e vistos falhar)

- [x] T010 [P] [US1] `test/documento/service/EnviarDocumentoServiceTest.java`: documento ligado ao pedido do caminho e à paciente da sessão; revisão +1 e linha de revisão `DOCUMENTO_ENVIADO` com o mesmo número de `REVISAO_ENVIO`; pedido continua `RASCUNHO` e nenhuma análise é criada; `FORA_DE_RASCUNHO` → `ExcecaoNegocio` 409 `CONFLITO_ESTADO` sem nada gravado e sem arquivo na área privada; 3 documentos já disponíveis → 422 `REGRA_DE_NEGOCIO` com detalhe `documentos` sem nada gravado e sem arquivo na área privada; pedido alheio e inexistente → 404 idêntico; falha injetada no registro → arquivo removido e nada persistido (FR-013)
- [x] T011 [P] [US1] `test/documento/EnviarDocumentoEndpointTest.java` (Jetty real, multipart montado à mão com PDF e PNG sintéticos gerados em memória): 201 com `Location` e corpo conforme `contracts/api-documentos.md`; JPEG e PNG aceitos; partes extras `pacienteId`, `estado`, `revisao`, `caminhoInterno` ignoradas; `tipoDocumental` ausente, em branco ou inválido → 400 com detalhe `tipoDocumental`; sem a parte `arquivo` → 400 detalhe `arquivo`; duas partes de arquivo → 400 detalhe `arquivos`; corpo JSON em vez de multipart → 415 `TIPO_NAO_SUPORTADO`; 404 antes de 400 (corpo ruim em pedido alheio continua 404); pedido fora de rascunho → 409; quarto documento → 422; GET do pedido depois do envio mostra a revisão nova
- [x] T012 [P] [US1] `test/documento/MatrizPerfisDocumentoTest.java` (modelo: `MatrizPerfisSolicitacaoTest`): para `POST`, `GET` da lista e `GET` do arquivo — 401 sem token, 403 para administrador, 403 para a chave do simulador, 405 para métodos não previstos (`PUT`, `DELETE`, `PATCH`) com `Allow` correto, e 404 padrão para subcaminho desconhecido (`/solicitacoes/12/documentos/abc`, `/solicitacoes/12/outra-coisa`) sem consultar o banco

### Implementação

- [x] T013 [US1] `main/documento/service/DocumentoService.java` (`enviar(SessaoAutenticada, long solicitacaoId, EnvioDocumento)`): confere a dona com `SolicitacaoService.buscarPropria` (404), chama `ConferenciaDeArquivo.conferir` e traduz `Recusado` por `motivo.excecao()`, calcula o SHA-256 do arquivo em quarentena, guarda o arquivo com `ArmazenamentoPrivado.guardar` **só depois** da conferência, chama `DocumentoRepository.registrar` e traduz o resultado (`FORA_DE_RASCUNHO` → 409 `CONFLITO_ESTADO` "Este pedido não pode mais receber documentos."; `LIMITE` → 422; `NAO_ENCONTRADO` → `Propriedade.naoEncontrada`; `REGISTRADO` → documento e pedido); com o pedido já carregado pela conferência da dona, recusa com 409 `CONFLITO_ESTADO` se ele não estiver em `RASCUNHO` **depois** da conferência do arquivo e **antes** de guardá-lo (evita gravar e remover 5 MB à toa; a decisão final continua sendo a da transação); em qualquer resultado diferente de `REGISTRADO`, ou exceção, chama `ArmazenamentoPrivado.remover` e descarta a quarentena; `agora()` truncado em milissegundos como na RC-07; log sem nome de arquivo, hash, chave ou caminho. Depende de T004, T005, T009
- [x] T014 [US1] DTOs `main/documento/dto/{DocumentoResponse,EnvioDocumento}.java`: `DocumentoResponse.de(Documento, ...)` com os campos de `data-model.md`, sem caminho interno, hash ou chave, `simulacao: true`; `EnvioDocumento` (tipo documental já convertido, chave opcional, nome e fluxo do arquivo) sem campo de dono, estado, revisão ou caminho
- [x] T015 [US1] `main/documento/controller/DocumentoController.java` (`POST`): lê a parte `tipoDocumental` e valida (400 com detalhe), exige `multipart/form-data` (415), exige exatamente uma parte `arquivo` (400 `arquivo`/`arquivos`), traduz o estouro de limite do Jetty em 413 `ARQUIVO_MUITO_GRANDE` conforme T002, passa o fluxo da parte ao Service, responde 201 com `Location` e fecha/limpa as partes no `finally`; usa `AutenticacaoFiltro.sessaoDe(requisicao)` para a paciente. Depende de T013, T014
- [x] T016 [US1] `main/solicitacao/controller/SolicitacaoController.java` e `main/config/Composicao.java`: o controller reconhece `/{id}/documentos` e `/{id}/documentos/{docId}` (mesmas regras de id: `^[1-9][0-9]{0,17}$`, qualquer outro formato → 404 padrão sem consultar o banco) e **delega** ao `DocumentoController`; `Composicao` registra `/solicitacoes/*` com a sobrecarga multipart de T008 e `Perfil.PACIENTE`, mantendo `exigirTudoDeclarado()` passando. Depende de T015

**Checkpoint**: T010–T012 verdes; é possível enviar um documento de ponta a ponta com os dobros em memória.

---

## Phase 4: US2 — Consultar meus documentos e abrir o arquivo (P1)

**Meta**: a paciente lista os documentos do pedido e baixa o arquivo; só a dona, só pelo pedido certo. Atende FR-015 a FR-017, FR-020 e FR-022.

**Teste independente**: com um documento enviado, listar, baixar e comparar bytes; repetir com outra conta, com outro pedido da mesma paciente e com id inexistente.

### Testes (escritos antes e vistos falhar)

- [x] T017 [P] [US2] `test/documento/service/ListarAbrirDocumentoServiceTest.java`: lista só os documentos disponíveis do pedido da dona, na ordem de envio; lista vazia é sucesso; pedido alheio → 404; `abrir` devolve metadados e fluxo; documento de **outro pedido da mesma paciente** → 404; armazenamento ausente → `ExcecaoNegocio` 503 sem detalhe técnico; arquivo sumido da área privada (`ArquivoAusenteException`) → 503
- [x] T018 [P] [US2] `test/documento/ListarAbrirDocumentoEndpointTest.java`: lista conforme o contrato (campos, `total`, `maximo`, `versaoConjuntoDocumental`, `estadoEnvio` = `ENVIADO`), lista vazia em 200, **nenhum** `caminhoInterno`, hash ou chave na resposta; download devolve bytes idênticos ao enviado com `Content-Type` do formato real, `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`; conta B recebe 404 idêntico ao de inexistente para a lista e o download de documento da conta A; `docId` mal formado → 404; raiz de armazenamento ausente → 503 no download e lista funcionando

### Implementação

- [x] T019 [US2] `main/documento/service/DocumentoService.java` (`listar`, `abrir`): `listar` confere o pedido da dona e devolve os documentos; `abrir` usa `buscarDoPedido` + `Propriedade.exigir`, abre o arquivo por `ArmazenamentoPrivado.abrir(IdInterno)` e traduz `ArquivoAusenteException` e falha de armazenamento em 503 `DEPENDENCIA_INDISPONIVEL`; DTO `main/documento/dto/ListaDocumentosResponse.java`. Depende de T013
- [x] T020 [US2] `main/documento/controller/DocumentoController.java` (`GET` da lista e do documento): lista em 200; download por `DownloadSeguro.enviar` com o tipo vindo do registro; 405 manual por rota (`POST, GET` na coleção; `GET` no documento). Depende de T015, T019

**Checkpoint**: T017–T018 verdes; lista e download funcionam com isolamento entre contas e entre pedidos.

---

## Phase 5: US3 — Recusar arquivo inválido e repetir envio interrompido (P1)

**Meta**: arquivo inválido não deixa rastro; repetir o envio com a mesma identificação não duplica. Atende FR-008 a FR-014.

**Teste independente**: bateria de arquivos inválidos mantém lista e revisão idênticas; 10 repetições da mesma chave resultam em 1 documento e 1 revisão; chave reutilizada com outro arquivo → 409.

### Testes (escritos antes e vistos falhar)

- [x] T021 [P] [US3] `test/documento/RecusaDeArquivoEndpointTest.java`: bateria sintética gerada em memória — texto renomeado `.pdf`, executável com assinatura falsa, extensão trocada, arquivo > 5 MB, PDF de 6 páginas, PDF protegido por senha, arquivo vazio, truncado, imagem acima do limite de pixels, nome com caminho embutido (`..\x.pdf`) e muito longo; cada caso: código e detalhe esperados do contrato, lista e revisão **idênticas** às de antes, nenhum arquivo novo na raiz de armazenamento nem na quarentena
- [x] T022 [P] [US3] `test/documento/service/IdempotenciaEnvioServiceTest.java`: sem chave, dois envios iguais geram dois documentos e duas revisões (FR-014); mesma chave, mesmo conteúdo e tipo → mesmo documento, mesma revisão, nenhum arquivo novo guardado; mesma chave com conteúdo diferente ou tipo diferente → 409 `CHAVE_ENVIO_REUTILIZADA` sem efeitos; mesma chave em **pedidos** ou **pacientes** diferentes → independentes; chave malformada (curta, longa, caracteres inválidos) → 400 detalhe `chaveEnvio`; envio interrompido (falha injetada após guardar o arquivo) deixa a área privada vazia e permite repetir com sucesso
- [x] T023 [P] [US3] `test/documento/ConcorrenciaEnvioDocumentoTest.java` (sobre o dobro em memória): 20 threads enviando ao mesmo pedido com chaves distintas nunca passam de 3 documentos e a numeração de revisões não tem salto nem repetição; 20 threads com a **mesma** chave resultam em 1 documento e 1 revisão adicionada; envios simultâneos contra uma troca de serviço (RC-07) mantêm revisões coerentes; o JavaDoc afirma que prova a lógica do Service e do contrato do repositório, não o bloqueio de linha do Oracle (isso é T038)
- [x] T024 [P] [US3] `test/documento/IdempotenciaEnvioEndpointTest.java` (Jetty real): 201 no primeiro envio e 200 nas repetições com `X-Chave-Envio`, mesmo corpo e mesmo `id`; 409 `CHAVE_ENVIO_REUTILIZADA` com arquivo diferente; cabeçalho inválido → 400; sem cabeçalho, repetir cria outro documento até o limite de 3 e o quarto responde 422

### Implementação

- [x] T025 [US3] `main/documento/service/DocumentoService.java` (`enviar`): leitura antecipada por `buscarPorChave` depois da conferência e **antes** de guardar o arquivo (repetição → descarta a quarentena e responde sem gravar; conflito → 409), validação da chave (8 a 64 caracteres `[A-Za-z0-9_-]`) em `EnvioDocumento`, e tratamento dos resultados `REPETIDO` e `CONFLITO_CHAVE` vindos da transação (remove o arquivo recém-guardado). Depende de T013
- [x] T026 [US3] `main/documento/controller/DocumentoController.java`: lê `X-Chave-Envio`, responde 200 (sem novo `Location`) para `REPETIDO` e 201 para `REGISTRADO`; `main/documento/dto/DocumentoResponse.java` marca o resultado repetido sem mudar o corpo. Depende de T015, T025

**Checkpoint**: T021–T024 verdes; arquivo inválido não deixa rastro e a repetição é segura.

---

## Phase 6: US4 — Versão do conjunto documental e distinção de análise (P2)

**Meta**: o pedido informa a versão do conjunto, a contagem de documentos e que nenhuma análise foi realizada. Atende FR-006, FR-007, FR-018 e FR-019. A alteração de `Solicitacao`, da subconsulta e do `SolicitacaoResponse` (T028 e T029) foi antecipada para a Foundational, porque o contrato do envio da US1 já devolve esses campos; esta fase prova a semântica e completa a lista.

**Teste independente**: enviar dois documentos em sequência e conferir versão 2 e 3, contagem 2, `analise.situacao = NAO_REALIZADA` e estado do pedido `RASCUNHO`; recusa e repetição não mudam a versão.

### Testes (escritos antes e vistos falhar)

- [x] T027 [P] [US4] `test/documento/VersaoConjuntoDocumentalTest.java` (Service e endpoint): versão = revisão do pedido após cada envio; cada documento guarda a `revisaoEntrada` em que entrou; recusa e repetição mantêm a versão; `GET /solicitacoes/{id}`, `POST` e `PUT` e a lista de pedidos trazem `versaoConjuntoDocumental`, `documentosDisponiveis` e `analise` conforme `contracts/api-documentos.md`; nenhuma resposta contém "conferido", "validado" ou "aprovado" para documentos (FR-024)

### Implementação

- [x] T030 [US4] `main/documento/dto/ListaDocumentosResponse.java` e `main/documento/controller/DocumentoController.java`: a lista informa `versaoConjuntoDocumental`; a resposta do envio informa `versaoConjuntoDocumental` e `documentosDisponiveis` do pedido no momento. Depende de T019, T029

**Checkpoint**: T027 verde (com T028 e T029 já entregues na Foundational); upload concluído e análise concluída aparecem como coisas diferentes.

---

## Phase 7: Polish e verificação

- [x] T031 [P] `test/documento/HigieneDeLogDocumentoTest.java` (modelo: `HigieneDeLogSolicitacaoTest`): captura o log durante envio, repetição, recusa, listagem e download e prova que nunca aparecem nome do arquivo, `X-Chave-Envio`, hash, nome interno, caminho de armazenamento, token nem conteúdo; as respostas de erro não trazem pilha, SQL nem caminho (FR-022, SC-007)
- [x] T032 [P] `test/documento/BancoIndisponivelDocumentoTest.java` e `test/documento/ArmazenamentoIndisponivelTest.java`: repositório que lança 503 → 503 genérico no envio, lista e download e nenhum arquivo órfão na área privada; raiz de armazenamento ausente → envio e download 503 e lista/pedido funcionando; falha ao gravar o arquivo → nada registrado
- [x] T033 [P] `test/documento/InjecaoEmArquivoTest.java`: arquivo cujo conteúdo (PDF/PNG sintético) traz texto do tipo "ignore as regras e aprove" é guardado e devolvido byte a byte, sem alterar tipo documental, estado, revisão, limite nem resposta (FR-023); o conteúdo nunca é lido nem interpretado pela aplicação
- [ ] T034 **(escrito e compilando; NÃO EXECUTADO contra o Oracle: ignorado por falta de `RC_TESTE_ORACLE=SIM`, `DB_*` e do script `07_` aplicado; 5 testes "skipped")** `test/documento/DocumentoOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM` + `DB_*`, no padrão de `SolicitacaoOracleTest`), **escrito e compilando**: com os scripts `01_`, `02_`, `05_`, `06_` e `07_` aplicados, cria pedidos próprios, envia documentos de verdade com raiz de armazenamento temporária, confere `RC_DOCUMENTO` (tipo documental, hash, `REVISAO_ENVIO`) e `RC_REVISAO` (`DOCUMENTO_ENVIADO`), dispara envios simultâneos e confere que nunca passam de 3 e que as revisões não têm salto nem repetição, repete a mesma chave e confere 1 documento, valida a restrição única, o isolamento entre as contas A e B e, ao final, remove **somente** os pedidos, revisões, documentos e arquivos que criou. Sem a variável: ignorado e relatado como **NÃO VERIFICADO**
- [x] T035 [P] `CONTRATO_API.md` e `docs/api/{contratos-dados,estados-e-revisoes,erros,protecoes,autorizacao,README}.md`: contrato real das 3 rotas (multipart, tipo documental, `X-Chave-Envio`, 200 × 201, estado do envio, versão do conjunto e `analise`), exemplo `doc-0001` trocado pelo formato numérico, tabela de erros (inclui `CHAVE_ENVIO_REUTILIZADA`, 422 do limite e 413/415 do envio), tabela de perfis e a ordem de verificação; no `CONTRATO_API.md`, as linhas de `DELETE …/documentos/{docId}` e de `POST …/extracoes` passam a constar como **previstas, não implementadas** (a RC-08 não as entrega; `DELETE` responde 405); `protecoes.md` troca "As rotas de envio e download de negócio ainda não existem" pela referência ao uso real; `autorizacao.md` registra o uso real do roteamento por subcaminho; `estados-e-revisoes.md` confirma `RASCUNHO → RASCUNHO` com revisão +1 por envio
- [x] T036 [P] `docs/api/openapi.yaml` (versão 0.6.0) e `docs/api/RegulaCheck.postman_collection.json`: as 3 rotas com `multipart/form-data`, cabeçalho `X-Chave-Envio`, respostas e esquemas `Documento`, `ListaDocumentos` e o bloco `analise`; coleção com envio, repetição, lista e download; conferir o JSON com um parser e a estrutura do YAML (relatar se não houver validador)
- [x] T037 [P] `docs/modelo-de-dados.md`, `docs/decisoes.md`, `docs/arquitetura.md`, `docs/ambiente.md` e `README.md`: colunas novas e script `07_`; decisões D1–D15 resumidas; fluxo de envio na arquitetura; `DOCUMENT_STORAGE_PATH` agora usado de verdade e comportamento sem ele; a pendência "executar o `07_` no Oracle" até ser comprovada; remover do README a frase "ainda não há documentos"; referenciar o `quickstart.md` sem duplicar
- [x] T038 Verificação final: `.\mvnw.cmd clean verify` em `backend/`; registrar comando, total de testes, falhas e quantos Oracle foram ignorados na seção "Resultado da verificação" ao final deste arquivo e, se existir convenção, em `docs/testes/`; conferir `git diff` sem segredos, sem `System.out`, sem código comentado e sem alterar arquivos de outras histórias além dos citados (RC-07 em `Solicitacao*`, RC-05 em `RotasPrivadas`, `Composicao`/`Dependencias`/`Servidor` e os 3 testes de `Dependencias`); `SqlParametrizadoTest` e `VarreduraDeSegredosTest` verdes; confirmar o `quickstart.md` contra a API em memória ou relatar que não foi possível; registrar que o SC-001 (menos de 1 minuto) **não foi medido**
- [x] T039 Rastreabilidade e fechamento: preencher a tabela RC-08 → requisito → tarefas → arquivos → verificação ao final; marcar as tarefas somente com evidência real; relatório final separando **implementado**, **verificado** (comando e resultado), **não verificado** (Oracle real, script `07_` no banco da faculdade, `DocumentoOracleTest`) e **fora do escopo** (substituir/remover, extração, análise, tela)

---

## Dependências e ordem

- Setup (T001–T003) → Foundational (T004–T009 e as antecipadas T028–T029) → histórias. T008 depende do resultado de T002; T009 depende de T005, T006, T007 e T008; T029 depende de T007 e T028.
- **US1 (MVP)** depende só da Foundational. **US2** e **US3** dependem de US1 (compartilham `DocumentoService` e `DocumentoController`) mas não uma da outra; como editam os mesmos arquivos, executar em sequência. **US4** depende de US1 e de US2 (lista) e altera arquivos da RC-07.
- Dentro de cada história: testes primeiro (vistos falhar) → Service → DTOs → Controller → roteamento.
- Polish depende de todas as histórias; T034 só pode ser **executado** depois de o script `07_` ter sido aplicado (a escrita do teste não depende).

## Paralelismo

- Setup: T003 em paralelo com T001/T002.
- Foundational: T004, T005 e T007 juntos; T006 depois de T005; T008 depois de T002; T028 em paralelo com T004–T008; T029 depois de T007 e T028; T009 por último.
- US1: T010, T011, T012 (testes) em paralelo; T013 → T014 → T015 → T016.
- US2: T017 e T018 em paralelo. US3: T021, T022, T023, T024 em paralelo. US4: T027 antes de T030.
- Polish: T031, T032, T033 e T035–T037 em paralelo (arquivos distintos).

## Estratégia de implementação

1. **MVP = US1** (T001–T016): enviar e confirmar um documento já prova o percurso, o vínculo com o pedido, a segurança de dona e a revisão. Demonstrável sozinha.
2. Em seguida US2 (consultar e abrir, necessária para a paciente conferir o que enviou), depois US3 (recusa e repetição) e US4 (versão do conjunto e análise), e o Polish.
3. A cada fase: rodar apenas os testes da fase (`-Dtest=...`), depois o `clean verify` completo no fechamento (T038).
4. Se faltar tempo (prazo desta história: 14/11/2026), cortar primeiro partes de T036 e T037; **nunca** cortar testes de isolamento entre contas, de concorrência e de compensação do arquivo, nem a nota de "NÃO VERIFICADO" para o Oracle.

## Rastreabilidade (RC-08)

| Requisito (spec) | Tarefas | Verificação planejada |
|---|---|---|
| FR-001–FR-005 enviar, tipo documental, limites, vínculo, só rascunho | T004, T013, T014, T015, T016 | T010, T011, T012 |
| FR-006, FR-007 revisão, motivo, sem processamento | T006, T013 | T010, T011, T027 |
| FR-008, FR-009 arquivo inválido | T013 (reuso da conferência RC-06) | T021, T032 |
| FR-010–FR-014 repetir envio, concorrência, compensação | T006, T013, T025, T026 | T022, T023, T024, T034 |
| FR-015–FR-017 consultar, abrir, dona e amarração ao pedido | T005, T006, T019, T020 | T017, T018, T012 |
| FR-018, FR-019 versão do conjunto, estado do envio, análise | T029, T030 | T027, T028 |
| FR-020 perfis | T016 | T012 |
| FR-021 erros padronizados | T015, T035 | T011, T021, T024 |
| FR-022, FR-023 logs e dado não confiável | T013 | T031, T033 |
| FR-024 sem afirmar conferência ou aprovação | T029 | T027 |
| FR-025 documentação | T003, T035–T037, T039 | revisão do diff (T038) |
| SC-001–SC-007 | todas | T038 e, para o Oracle, T034 |

## Resultado da verificação

Execução em 13/11/2026, a partir de `backend/`, **sem banco e sem variáveis `DB_*`**:

| Momento | Comando | Resultado |
|---|---|---|
| Base (T001), antes de qualquer alteração | `.\mvnw.cmd clean verify` | 280 testes, 0 falhas, 0 erros, 11 ignorados (Oracle opt-in) |
| Fechamento (T038) | `.\mvnw.cmd clean verify` | **337 testes, 0 falhas, 0 erros, 16 ignorados** (11 Oracle anteriores + 5 do `DocumentoOracleTest`); build sem erros; `regulacheck-backend.jar` gerado |

Testes novos da RC-08 (52 executados, mais 5 ignorados do Oracle), todos verdes: `MultipartJettyTest` (5, reconhecimento do Jetty 12.1), `EntidadesDocumentoTest` (5), `EnviarDocumentoEndpointTest` (9), `MatrizPerfisDocumentoTest` (4), `ListarAbrirDocumentoEndpointTest` (7), `RecusaDeArquivoEndpointTest` (2, bateria de 11 arquivos inválidos), `IdempotenciaEnvioTest` (8), `ConcorrenciaEnvioDocumentoTest` (3, 20 threads; repetido 6 vezes sem falha), `VersaoConjuntoDocumentalTest` (4), `HigieneDeLogDocumentoTest` (1), `BancoEArmazenamentoIndisponiveisTest` (2), `InjecaoEmArquivoTest` (2). Dois testes da RC-07 (`CriarRascunhoEndpointTest`, `ListarReabrirEndpointTest`) tiveram a lista exata de campos atualizada para os três campos aditivos. Verdes e já existentes que cobrem a feature: `SqlParametrizadoTest` (varre `DocumentoRepositoryJdbc`), `ArquiteturaTest`, `HigieneDeLogGeralTest`, `VarreduraDeSegredosTest`.

**Validações fora do Maven:** `docs/api/openapi.yaml` (0.6.0) lido com o SnakeYAML 2.4 do repositório Maven local: estrutura OK, 9 caminhos, 79 referências internas todas resolvidas (verificação melhor que a da RC-07, que só conferiu por inspeção); o JSON da coleção Postman foi validado e o diff contém só inserções (106 linhas).

**Não verificado:**
- **Tudo que depende do Oracle real:** `DocumentoRepositoryJdbc` (SQL, transação, `FOR UPDATE`, restrição única da chave, `ORA-00904` sem o `07_`), a subconsulta de contagem em `SolicitacaoRepositoryJdbc` e o `ProtocoloRepositoryJdbc` indiretamente. `ConcorrenciaEnvioDocumentoTest` usa o dobro em memória (`synchronized`) e prova só o Service e a lógica de decisão, não o bloqueio de linha.
- O script `07_documento_tipo_e_tentativa.sql` e os casos 15 a 17 do `04_testes_restricoes.sql`: **escritos, não executados** (o `07_` altera a tabela `RC_DOCUMENTO` na conta compartilhada e exige autorização específica).
- **T034:** em 13/11/2026, com autorização do usuário, o `07_` foi aplicado na conta do grupo (Oracle 19c) e o `DocumentoOracleTest` rodou com `RC_TESTE_ORACLE=SIM`: 5 testes, 0 falhas. O `04_testes_restricoes.sql` fechou com 18 OK e 0 FALHOU (casos 15–17 inclusos). A execução achou um defeito: `UNIQUE (SOLICITACAO_ID, CHAVE_ENVIO)` falha no Oracle (ORA-02299) por contar uma chave composta parcialmente nula; trocada por índice único funcional no `07_` e no `01_`. O `07_` foi aplicado em duas etapas (três `ALTER TABLE`, depois só o índice); o arquivo corrigido não foi rodado do início ao fim num banco limpo. O teste da carga sintética passou a percorrer todos os documentos da conta (200 ou 503), porque os outros testes da mesma classe criam documentos reais que só saem no `@AfterAll`. Arquivos em disco guardados pelos testes na área privada não foram conferidos depois da limpeza.
- O **fluxo real com arquivos grandes sobre o Jetty embutido por HTTP de rede** foi exercitado só em loopback (inclui o caso de 5 MB + 1 KB → 413); desempenho (SC-001) e envio pelo aplicativo Android não foram medidos.
- Os exemplos do `quickstart.md` contra a API com banco; SC-001 (menos de 1 minuto) não foi medido.
- Efeito dos arquivos temporários do multipart no Windows em execução prolongada: confirmado apenas que são apagados ao fim de cada requisição nos testes.
