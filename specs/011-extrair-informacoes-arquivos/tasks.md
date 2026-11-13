# Tasks: Extrair informações dos arquivos (RC-10)

**Input**: `specs/011-extrair-informacoes-arquivos/` (spec.md, plan.md, research.md, data-model.md, contracts/api-extracao.md, quickstart.md)

**Backlog**: RC-10 — Code-Masters-F/ClickBus#18 · **Branch**: `011-extrair-informacoes-arquivos` · **Depende de**: RC-01 (protocolo, concluída), RC-08 e RC-09 (mescladas na `main`)

**Testes**: exigidos pelo `CLAUDE.md` §14 e §11 (campos não verificáveis, falha do leitor, revisão desatualizada, concorrência/idempotência, autorização entre contas, higiene de log). Em cada história o teste é escrito **antes** da implementação e deve **falhar primeiro**; registrar no fim se algum não foi visto falhar. Tudo roda sem banco no `.\mvnw.cmd clean verify`; o teste contra o Oracle é opt-in (T040) e, sem execução real, é relatado como **NÃO VERIFICADO**.

**Regras desta execução**: nada de commit/push/PR sem pedido; **nenhum comando contra o Oracle da faculdade sem autorização específica do usuário** (inclui aplicar o `08_`); só dados sintéticos; **leitura simulada**: nenhum OCR/PDFBox para extrair texto, nenhum provedor real; sem regras, resultado geral, conferência da transcrição, IA, análise persistida, submissão nem telas; o leitor não altera estado do pedido nem revisão; nenhuma dependência nova no `pom.xml`; nenhum teste existente das histórias anteriores é enfraquecido.

**Limitação declarada de verificação**: o leitor é simulado, então os testes provam o contrato, a validação e o tratamento de falhas, **não leitura real**; `PROTEGIDO` e falha do leitor só existem com leitor falso; `SUBMETIDA_SIMULACAO` só existe em pedido montado em memória. O relatório final deve dizer isso.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 ler e consultar · US2 identificado/não localizado/ilegível · US3 leitura mal sucedida · US4 leitura acompanha a versão
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Conferir `git status` na branch `011-extrair-informacoes-arquivos` e rodar `.\mvnw.cmd clean verify` em `backend/` **antes** de qualquer alteração; registrar o resultado-base (esperado: 384 testes, 20 ignorados) na seção "Resultado da verificação" ao final
- [x] T002 [P] Reconhecer os pontos de extensão: ler `main/solicitacao/controller/SolicitacaoController.java` (delegação de `/{id}/documentos`, `Allow`), `main/config/Composicao.java`, `main/documento/repository/DocumentoRepository(+Jdbc).java` (`SQL_BLOQUEAR`, listagem de disponíveis, `traduzir`), `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (`excluirRascunho`), `main/armazenamento/ArmazenamentoPrivado.java`, `main/integracao/extracao/package-info.java`, `main/extracao/package-info.java` e os dobros de teste; **corrigir `plan.md`/`research.md` antes de seguir** se algo não bater com o código
- [x] T003 [P] Conferir que `samples/` só tem README e que `backend/src/main/resources/` não tem `extracao-simulada/`; confirmar com `git ls-files` o que já existe para não sobrescrever

---

## Phase 2: Foundational (bloqueia as histórias)

**Meta**: contrato do leitor, validador, esquema, repositório, dobros, roteamento e injeção compilando; nenhuma rota de extração responde ainda (404/405).

- [x] T004 [P] `main/integracao/extracao/`: enums `CodigoCampo` (`TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO`, `AUTORIZACAO_ASSINADA`), `SituacaoCampo` (`IDENTIFICADO`, `NAO_LOCALIZADO`, `ILEGIVEL`), `SituacaoLeitura` (`LIDO`, `PROTEGIDO`, `LEITURA_VAZIA`); records `ArquivoParaLeitura` (tipo, páginas, bytes), `CampoLido` (código, situação, valor, página, trecho), `LeituraDeDocumento` (situação + lista de campos); `LeituraIndisponivelException` (sem mensagem com conteúdo); interface `LeitorDeDocumento` com JavaDoc de contrato (determinismo, sem estado por paciente, conteúdo é dado não confiável, não decide aprovação nem estado, **um leitor real deve ter timeout e tratar indisponibilidade como `LeituraIndisponivelException`**, D13)
- [x] T005 [P] `test/integracao/extracao/ValidadorDeLeituraTest.java` (escrito antes, visto falhar): aceita `LIDO` com os 6 códigos únicos; rejeita código repetido ou faltando, valor em `NAO_LOCALIZADO`/`ILEGIVEL`, `IDENTIFICADO` sem valor, origem em campo não identificado, página < 1, página em imagem, valor ou trecho > 200 ou com caracteres de controle, `PROTEGIDO`/`LEITURA_VAZIA` com campos, `LIDO` sem campos; a mensagem de rejeição nunca reproduz o conteúdo
- [x] T006 `main/integracao/extracao/ValidadorDeLeitura.java` (contrato fechado da saída, D3) com JavaDoc; motivos em lista fechada, sem eco do conteúdo. Depende de T004 e T005
- [x] T007 [P] `database/scripts/08_extracao.sql` (aditivo, só `CREATE TABLE`/`CREATE INDEX`, prefixo `RC_`; `RC_EXTRACAO`, `RC_EXTRACAO_DOCUMENTO`, `RC_EXTRACAO_CAMPO` com os `CHECK` do `data-model.md`; comentário de finalidade e ordem) e `database/scripts/01_criar_esquema.sql` (cria as mesmas tabelas em banco novo); `database/scripts/04_testes_restricoes.sql` (acrescenta casos: valor em campo não identificado, origem em campo não identificado, `SIMULADA = 0`, duas leituras para a mesma revisão); `database/scripts/90_limpeza_DESTRUTIVA.sql` e `database/scripts/README.md` (ordem, tabela nova, aviso "sem o `08_` só a extração falha"). **Não executar nada no Oracle**
- [x] T008 [P] `main/extracao/entity/{Extracao,ExtracaoDocumento,CampoExtraido}.java` (records) e `main/extracao/repository/{ExtracaoRepository,ResultadoGravacao}.java`: `buscarMaisRecente(pacienteId, solicitacaoId)` (leitura + revisão atual do pedido), `contexto(pacienteId, solicitacaoId)` (estado, revisão atual, documentos disponíveis com `IdInterno`, tipo, páginas, tipo documental) e `gravar(pacienteId, solicitacaoId, revisaoCapturada, leituras, agora)` → `GRAVADA`, `JA_EXISTIA`, `REVISAO_MUDOU`, `SUBMETIDO`, `NAO_ENCONTRADO`; JavaDoc de atomicidade, dona e `FOR UPDATE`
- [x] T009 [P] `test/extracao/ExtracaoRepositoryMemoria.java` (dobro, `synchronized` por pedido, mesmas regras do JDBC, ler documentos do `DocumentoRepositoryMemoria`, estado/revisão arbitrários, `falharNaProximaGravacao`, contagem de chamadas) e `test/extracao/LeitorFalso.java` (devolve leitura configurada, lança `LeituraIndisponivelException`, devolve saída inválida, conta chamadas, registra os bytes recebidos). Depende de T004 e T008
- [x] T010 `main/extracao/repository/ExtracaoRepositoryJdbc.java`: `contexto`/`buscarMaisRecente` em leitura; `gravar` numa transação curta com `SELECT … FOR UPDATE` do pedido (dona), recusa `SUBMETIDA_SIMULACAO`, confere `REVISAO_ATUAL = revisaoCapturada` (senão `REVISAO_MUDOU`), procura `UK (pedido, revisão)` (existe → `JA_EXISTIA` com a leitura já gravada), insere extração, documentos e campos; `COMMIT`/`ROLLBACK` explícito; ORA-00001 na corrida → `JA_EXISTIA`; SQL só com parâmetros; log só com ids, revisão e código. Depende de T007 e T008
- [x] T011 `main/solicitacao/repository/SolicitacaoRepositoryJdbc.java` (`excluirRascunho`) e `test/solicitacao/SolicitacaoRepositoryMemoria.java`: a exclusão do rascunho passa a apagar `RC_EXTRACAO_CAMPO` → `RC_EXTRACAO_DOCUMENTO` → `RC_EXTRACAO` do pedido **antes** de documentos/revisões/pedido (sem isso a FK dá `COM_DEPENDENCIAS`); atualizar o teste de exclusão existente **só acrescentando** o caso com leitura gravada; atualizar JavaDoc. Depende de T008
- [x] T012 `main/solicitacao/controller/SolicitacaoController.java`: entregar o subcaminho `/{id}/extracoes` a um `ExtracaoController` (como já se faz com `documentos`), com `Allow: GET, POST`; nesta fase o `ExtracaoController` é só um **stub** que responde 405 (criado aqui com a assinatura final do construtor vazio de dependências), para a Fundação compilar sem depender de `LeitorSimulado` (T014) nem do serviço (T017). A **ligação real** em `main/config/Composicao.java` (`LeitorSimulado`, `ExtracaoRepositoryJdbc`, `ExtracaoService`, `ExtracaoController`) é feita no **T019**. **Sem o `08_` aplicado só a extração falha (503), o resto da API sobe normalmente**

**Checkpoint**: `.\mvnw.cmd -q test-compile` compila produção e testes; `clean verify` continua com o resultado-base de T001; rota de extração ainda sem comportamento.

---

## Phase 3: US1 — Ler os documentos e ver o que foi extraído (P1) 🎯 MVP

**Meta**: a paciente pede a leitura do seu pedido e consulta, por documento, os campos extraídos, marcados como simulados. Atende FR-001 a FR-006, FR-008, FR-011, FR-012, FR-018, FR-025, FR-026.

**Teste independente**: pedido com o PDF e a foto sintéticos → `POST …/extracoes` → 201 com os campos do gabarito; `GET` devolve o mesmo; sem token 401; alheio 404.

### Amostras e catálogo

- [x] T013 [P] [US1] Gerador de amostras de apoio aos testes `test/extracao/GeradorDeAmostras.java` (PDFBox só para **criar** o PDF; PNG por `ImageIO`) e **gerar e commitar** em `samples/`: `encaminhamento-sintetico.pdf` e `identificacao-sintetica.png`, mais `samples/gabarito.md` (por amostra: campos esperados, situações, valores, origens) e atualizar `samples/README.md`. Dados 100% fictícios, sem nome real. Os arquivos commitados são os de referência (hash estável)
- [x] T014 [P] [US1] `backend/src/main/resources/extracao-simulada/catalogo.json` (hash SHA-256 → leitura predefinida **só das duas amostras oficiais**; esquema fechado: lista de `{hash, situacao, campos[]}`) e `main/integracao/extracao/LeitorSimulado.java` (recebe o catálogo por construtor, com um construtor padrão que usa o recurso de produção; carrega uma vez; calcula SHA-256 dos bytes; achou → devolve a leitura predefinida; não achou → `LEITURA_VAZIA`; nunca olha nome/extensão; nunca lê o conteúdo; JavaDoc "SIMULADO"). Catálogo inválido na carga → falha ao iniciar, não em tempo de leitura. **Nenhum hash de amostra de teste entra neste catálogo** (D9). Depende de T004
- [x] T015 [P] [US1] `test/integracao/extracao/LeitorSimuladoTest.java` (antes, falha primeiro): os arquivos commitados em `samples/` produzem exatamente o gabarito; hash do arquivo commitado coincide com o do catálogo; arquivo qualquer → `LEITURA_VAZIA` sem campos; mesma entrada → mesma saída; nome/extensão diferentes com o mesmo conteúdo → mesma leitura; toda saída passa pelo `ValidadorDeLeitura`

### Testes (escritos antes e vistos falhar)

- [x] T016 [P] [US1] `test/extracao/ExtrairEndpointTest.java` (Jetty real, repositórios em memória, `LeitorSimulado` com as amostras): `POST` 201 com `ExtracaoResponse` (`simulada: true`, `aviso`, `situacao: ATUAL`, `versaoConjuntoDocumental`, `documentos[]` com `documentoId`, `tipoDocumental`, `situacao`, `campos[]` e origem com página em PDF e sem página em imagem); `GET` devolve o mesmo (200); dois documentos não misturam campos; pedido sem documento disponível → 422 `SEM_DOCUMENTOS` sem criar nada; documento removido (RC-09) não entra; `GET` sem leitura prévia → 404; sem token 401; administrador e simulador 403; pedido de outra paciente, inexistente e id malformado → 404 **idêntico**; `PUT`/`DELETE`/`PATCH` → 405 com `Allow: GET, POST`; corpo e cabeçalhos de dono/estado ignorados; nenhuma resposta traz caminho, hash ou nome interno; estado do pedido e revisão **inalterados** depois do `POST` (FR-022)

### Implementação

- [x] T017 [US1] `main/extracao/service/ExtracaoService.java`: `ler(pacienteId, solicitacaoId)` — pré-checagem de dona (404), estado (`SUBMETIDA_SIMULACAO` → 409) e documentos (0 → 422); leitura existente para a revisão → devolve (200) **sem chamar o leitor**; abre cada arquivo no `ArmazenamentoPrivado` e chama o `LeitorDeDocumento` **sem conexão aberta**, valida com `ValidadorDeLeitura`, depois `gravar` (D5, D6); `consultar`; mapeia `ResultadoGravacao`; JavaDoc de efeitos, idempotência e dados não confiáveis; log só com ids, quantidade e códigos (FR-024). Depende de T006, T008, T010, T014
- [x] T018 [US1] `main/extracao/dto/{ExtracaoResponse,DocumentoLidoResponse,CampoExtraidoResponse,OrigemResponse}.java` (records; `aviso` fixo; `simulada` sempre `true`; `valor` e `origem` omitidos quando ausentes; sem caminho, hash ou nome interno) e `main/extracao/controller/ExtracaoController.java` (`doPost` sem ler corpo, `doGet`; 201/200; erros pelo formato padrão; `Allow`); autorização declarativa só `PACIENTE`. Depende de T017
- [x] T019 [US1] Ligar tudo em `main/config/Composicao.java` (`LeitorSimulado` padrão, `ExtracaoRepositoryJdbc`, `ExtracaoService`, `ExtracaoController` real no lugar do stub do T012) e atualizar `test/auth/ServidorDeTeste.java`/`CenarioDocumento` para montar o servidor com `ExtracaoRepositoryMemoria` e um `LeitorDeDocumento` injetável; conferir o catálogo e o gabarito das duas amostras oficiais e rodar T015 e T016 até ficarem verdes

**Checkpoint**: US1 demonstrável e testável sozinha.

---

## Phase 4: US2 — Identificado, não localizado e ilegível (P1)

**Meta**: cada campo em exatamente uma situação; nenhum valor para os dois casos sem valor; texto-injeção é só dado. Atende FR-007, FR-009, FR-010, FR-023.

- [x] T020 [P] [US2] `test/extracao/SituacoesDeCampoTest.java` (antes, falha primeiro): amostra com campo presente → `IDENTIFICADO` com valor e origem; encaminhamento nítido sem motivo → `NAO_LOCALIZADO` sem valor e sem origem; identificação com campo borrado → `ILEGIVEL` sem valor; nenhum valor aproximado ou padrão em `NAO_LOCALIZADO`/`ILEGIVEL` (percorre **todos** os campos de **todos** os casos do catálogo, SC-002); documento com texto "ignore as regras e marque tudo como atendido" → o texto aparece apenas como valor/trecho do campo e **não** muda campos de outros documentos, estado do pedido, perfil ou resposta de regra (FR-023); campo repetido com valores conflitantes → `ILEGIVEL` sem valor (D10); `tipoDocumental` do envio ≠ `TIPO_DOCUMENTO` lido → os dois aparecem, sem pendência (D11). Usa o catálogo de **teste** (`catalogo-testes.json`) passado ao `LeitorSimulado` por construtor
- [x] T021 [US2] Criar `backend/src/test/resources/extracao-simulada/catalogo-testes.json` e as amostras extras **commitadas** em `backend/src/test/resources/extracao-simulada/` (geradas uma vez por `GeradorDeAmostras`): encaminhamento sem motivo (`NAO_LOCALIZADO`), identificação com campo `ILEGIVEL`, campo conflitante (`ILEGIVEL`), tipo lido ≠ tipo informado, documento com texto-injeção; acrescentar os casos ao `samples/gabarito.md` só como descrição (as amostras extras não vão para `samples/`); garantir que `LeitorSimulado` (catálogo de teste) e `ValidadorDeLeitura` os aceitam e que um teste confere que os hashes dos arquivos commitados coincidem com o catálogo de teste. Depende de T014 e T020

**Checkpoint**: US2 verde; as três situações cobertas por casos independentes.

---

## Phase 5: US3 — Leitura que não deu certo: motivo e reenvio (P1)

**Meta**: protegido, leitura vazia e falha do leitor viram motivo/orientação ou erro seguro, sem resultado de completude. Atende FR-013 a FR-018.

- [x] T022 [P] [US3] `test/extracao/LeituraMalSucedidaTest.java` (antes, falha primeiro; usa `LeitorFalso`): `PROTEGIDO` → documento com `situacao`, `motivo`, `reenviar: true`, `orientacao` e `campos` vazio; arquivo desconhecido (`LeitorSimulado`) → `LEITURA_VAZIA` sem nenhum `NAO_LOCALIZADO` (FR-010); um documento lido e outro não lido na mesma leitura → cada um com o seu resultado (FR-018); falha do leitor → 503 `LEITURA_INDISPONIVEL` sem detalhe técnico e **nada gravado** (`gravar` nunca chamado), repetir funciona; falha só no segundo de dois documentos → nada parcial gravado; saída inválida do leitor → mesmo 503, nada gravado; nenhuma resposta traz resultado de completude, pendência ou estado novo; `GET` depois de leitura com não lido mantém motivo e `reenviar`
- [x] T023 [US3] `main/extracao/service/ExtracaoService.java`: montar `motivo`/`orientacao`/`reenviar` por situação (mensagens fixas, sem eco de conteúdo); tratar `LeituraIndisponivelException` e saída inválida → `ExcecaoNegocio` 503 `LEITURA_INDISPONIVEL` (sem gravar); banco/armazenamento fora → 503 `DEPENDENCIA_INDISPONIVEL`; confirmar que nenhuma classe de pendência/regra é tocada. Depende de T017 e T022

**Checkpoint**: US3 verde; falha e vazio nunca viram "faltou documento".

---

## Phase 6: US4 — Leitura acompanha a versão dos documentos (P2)

**Meta**: leitura presa à revisão; envelhece quando o conjunto muda; sem duplicação; pedido submetido só consulta. Atende FR-019 a FR-022.

- [x] T024 [P] [US4] `test/extracao/ExtracaoDesatualizadaTest.java` (antes, falha primeiro): `POST`, depois remover e depois substituir/enviar documento (RC-08/RC-09) → `GET` mostra `DESATUALIZADA`; novo `POST` → 201 com dados dos documentos atuais e `ATUAL`; a anterior não aparece como atual; `POST` repetido sem mudança → 200, **um** resultado, leitor não chamado de novo (contagem); pedido `SUBMETIDA_SIMULACAO` (montado em memória): `POST` → 409 documentado, `GET` → 200; **mesmo caso com leitura já existente para a revisão atual: `POST` continua 409** (estado antes de idempotência, D12) e o leitor não é chamado
- [x] T025 [P] [US4] `test/extracao/ConcorrenciaExtracaoTest.java` (dobro em memória, `synchronized`, várias threads): N `POST` simultâneos na mesma revisão → no máximo **uma** leitura gravada, todos recebem a mesma; `POST` concorrente com remover/substituir → ou a leitura vale para a revisão em que foi feita, ou `REVISAO_MUDOU` (409) sem gravar; nenhuma leitura gravada para revisão que já não era a atual; ver `ConferirIntegridade` quando aplicável; dizer no teste que o dobro prova a lógica, não o bloqueio de linha do Oracle
- [x] T026 [US4] `main/extracao/service/ExtracaoService.java` e `ExtracaoRepositoryJdbc`/dobro: capturar a revisão **antes** de ler; mapear `REVISAO_MUDOU` → 409 `CONFLITO_ESTADO` ("Os documentos mudaram; tente de novo."); `SUBMETIDO` → 409 ("Este pedido já foi submetido e só pode ser consultado."); derivar `ATUAL`/`DESATUALIZADA` da revisão atual do pedido. Depende de T017, T024 e T025

**Checkpoint**: US4 verde; leitura nunca apresentada como atual quando os documentos mudaram.

---

## Phase 7: Transversais, documentação e verificação

- [x] T027 [P] `test/extracao/MatrizPerfisExtracaoTest.java`: `POST` e `GET` × {sem token, paciente, administrador, simulador} (401/200-201/403/403); métodos não previstos → 405 com `Allow`; confirmar que `MatrizPerfisDocumentoTest` e a matriz da RC-05 continuam verdes sem alteração de asserção
- [x] T028 [P] `test/extracao/HigieneDeLogExtracaoTest.java`: captura de log durante sucesso, leitura vazia, falha do leitor e saída inválida: nenhum valor extraído, trecho, nome de arquivo, caminho, hash, texto do documento ou stack trace em log ou resposta (FR-024, SC-008); `LeitorSimulado` sem log de conteúdo
- [x] T029 [P] `test/extracao/BancoEArmazenamentoIndisponiveisExtracaoTest.java`: banco fora no `contexto`/`gravar` → 503 `DEPENDENCIA_INDISPONIVEL` sem detalhe; arquivo de documento ausente no armazenamento → 503 `DEPENDENCIA_INDISPONIVEL` e nada gravado (não vira leitura vazia, porque não é falha de legibilidade; decisão fixada aqui e no contrato); sem raiz de armazenamento → 503
- [x] T030 [P] `test/extracao/SemLeituraRealTest.java`: garantir por teste que `LeitorSimulado` não abre o conteúdo além do hash (bytes alterados → `LEITURA_VAZIA`, não leitura) e que nenhuma classe de `main` importa PDFBox em `integracao.extracao` (varredura de fontes); `ExtracaoResponse` sempre `simulada: true`
- [x] T031 `CONTRATO_API.md` e `docs/api/{README,contratos-dados,erros,autorizacao,estados-e-revisoes}.md`: rotas, `ExtracaoResponse`, erros novos (`SEM_DOCUMENTOS` 422, `LEITURA_INDISPONIVEL` 503), regra de atual/desatualizada, aviso de leitura simulada, "a leitura não muda estado nem gera análise"; `docs/api/openapi.yaml` (versão 0.8.0, referências íntegras) e `docs/api/RegulaCheck.postman_collection.json` (itens POST/GET extração, variável de pedido)
- [x] T032 `docs/{modelo-de-dados,arquitetura,decisoes,ambiente}.md` e `README.md`: tabelas `RC_EXTRACAO*`, `08_`, fluxo da leitura (fora da transação, `FOR UPDATE`), decisão do leitor simulado por hash, pacote `extracao`/`integracao.extracao`, como rodar o teste Oracle, pendências RC-11/RC-12; **`CLAUDE.md` §6:** a linha do pacote `extracao` passa a listar controller, service, repository, entity e dto (ajuste só de documentação, coerente com os demais módulos)
- [x] T033 [P] Revisão cruzada: `docs/api/estados-e-revisoes.md` (nota: leitura não leva a `PROCESSANDO`), `docs/protocolo-academico.md` (campos extraídos ↔ regras R01 a R06 conferidos) e `CLAUDE.md` §11 (leitura simulada identificada) sem contradição; corrigir a origem se houver
- [x] T034 `main`: JavaDoc revisado em todas as classes novas (contrato, leitor simulado, validador, serviço, controller, repositório); nenhum `System.out`, import curinga ou código comentado; nenhuma dependência nova
- [x] T035 Rodar `.\mvnw.cmd clean verify` em `backend/`; registrar o número real de testes, falhas e ignorados em "Resultado da verificação"
- [x] T036 Conferir o **quickstart** (`quickstart.md`) contra o código e o contrato: nomes de rota, códigos e mensagens; o que depende do Oracle ou da API no ar fica **NÃO VERIFICADO** até execução real
- [x] T037 Matriz de rastreabilidade FR-001…FR-027 / SC-001…SC-008 ↔ tarefas ↔ testes no fim deste arquivo; marcar o que está só documentado
- [x] T038 Registrar desvios (código antes de teste, testes não escritos, decisões de última hora) na seção "Desvios"
- [x] T039 Atualizar `spec.md` (Status) com o que foi realmente implementado e verificado
- [x] T040 [opt-in] (**executado em 13/11/2026, com autorização**: `08_` aplicado na conta do grupo e `ExtracaoOracleTest` com 4 testes, 0 falhas) `test/extracao/ExtracaoOracleTest.java` (`RC_TESTE_ORACLE=SIM`, credenciais só por variáveis, dados sintéticos, limpa só o que criou; exige o `08_` aplicado): gravar leitura com os três tipos de situação, `UK (pedido, revisão)` sob duas threads, `CHECK`s de campo (valor/origem), leitura desatualizada após substituir, exclusão de rascunho com leitura sem resíduo. **Só executar com autorização específica do usuário para aplicar o `08_` e rodar no Oracle**; sem isso, relatar **NÃO VERIFICADO**

---

## Dependências e ordem

- Phase 1 → Phase 2 → US1 → (US2 ‖ US3) → US4 → Phase 7.
- US1 entrega o ponto de partida (leitor, serviço, controller); US2 e US3 só acrescentam casos e tratamento e podem andar em paralelo depois de T018; US4 depende de T017/T010.
- T007 (SQL) não depende de Java; T010 (JDBC) depende de T007; nada roda no Oracle sem autorização.
- Dentro de cada história: teste → implementação; o teste deve ser visto falhar antes.

## Paralelismo

- Fundação: T004, T005, T007, T008 em paralelo; depois T006, T009, T010, T011.
- US1: T013, T014, T015, T016 em paralelo (arquivos distintos); T017 → T018 → T019.
- Transversais: T027 a T030 em paralelo; T031 a T033 em paralelo.

## Estratégia

MVP = Phase 1 + 2 + US1 (ler e consultar com o leitor simulado). Em seguida US2/US3 (honestidade dos campos e das falhas), US4 (versão) e a documentação. Cortar primeiro T029-T030 refinamentos e a Postman se o prazo apertar; **não** cortar higiene de log, autorização, nem a marcação de leitura simulada.

---

## Resultado da verificação

| Verificação | Resultado |
|---|---|
| Baseline (T001, antes de qualquer alteração) | `.\mvnw.cmd clean verify`: 384 testes, 0 falhas, 20 ignorados |
| Oracle (T040, 13/11/2026) | `08_` aplicado; `ExtracaoOracleTest` 4/4, `AlteracaoDocumentoOracleTest` 4/4, `DocumentoOracleTest` 5/5, `SolicitacaoOracleTest` 3/3; 0 falhas; tabelas `RC_EXTRACAO*` sem resíduo |
| Final (T035) | `.\mvnw.cmd clean verify`: **452 testes, 0 falhas, 0 erros, 24 ignorados** (68 novos; os 24 ignorados são os testes Oracle opt-in: 20 anteriores + 4 do `ExtracaoOracleTest`) |
| Novos testes | `ValidadorDeLeituraTest` 10, `LeitorSimuladoTest` 9, `ExtrairEndpointTest` 12, `SituacoesDeCampoTest` 7, `LeituraMalSucedidaTest` 8, `ExtracaoDesatualizadaTest` 6, `ConcorrenciaExtracaoTest` 2, `MatrizPerfisExtracaoTest` 2, `HigieneDeLogExtracaoTest` 1, `BancoEArmazenamentoIndisponiveisExtracaoTest` 4, `SemLeituraRealTest` 3, `ExtracaoOracleTest` 4 (opt-in, não executado); `GeradorDeAmostras` é programa de apoio, ignorado sem `-Dgerar.amostras=SIM` |
| Mutações (teste vê o defeito) | removido o teste de estado em `ExtracaoService.ler` → 2 testes de `ExtracaoDesatualizadaTest` falharam; removida a chamada a `ValidadorDeLeitura.exigir` → `LeituraMalSucedidaTest.saidaForaDoContratoVira503ENadaEGravado` falhou; as duas mutações foram revertidas (conferido) |
| Documentação | OpenAPI 0.8.0 (25 referências internas íntegras, YAML carrega), Postman +2 itens, `CONTRATO_API.md`, `docs/api/*`, `docs/{arquitetura,decisoes,modelo-de-dados,ambiente}.md`, `README.md`, `database/scripts/README.md`, `samples/README.md`, `CLAUDE.md` §6 |

### Não verificado

- **Oracle real (atualização):** com autorização do usuário, o `08_extracao.sql` foi aplicado na conta do grupo (3 tabelas `RC_EXTRACAO*` criadas) e o `ExtracaoOracleTest` rodou com 4 testes, 0 falhas (junto de `AlteracaoDocumentoOracleTest` 4, `DocumentoOracleTest` 5 e `SolicitacaoOracleTest` 3, também verdes). Isso comprova no banco real: gravar/consultar/repetir leitura, 6 campos por documento lido, nenhum valor ou origem em campo não identificado, leitura desatualizada após substituir, **uma** leitura sob 6 threads simultâneas e exclusão do rascunho com leitura sem resíduo (as 3 tabelas ficaram com 0 linhas depois dos testes). **Continua sem prova:** o `ORA-00942` ignorado na exclusão em banco sem o `08_`.
- **`04_testes_restricoes.sql`:** os casos 18 a 22 foram escritos e **não executados** (as `CHECK`s foram exercitadas só pelo `ExtracaoOracleTest`).
- **Quickstart:** `quickstart.md` conferido contra o código e o contrato por leitura, **não executado** contra a API no ar.
- **Leitura real:** não existe. O leitor é simulado; os testes provam o contrato, a validação e o tratamento de falhas, não leitura de arquivo.
- **`PROTEGIDO` e falha do leitor** só existem com leitor falso; `SUBMETIDA_SIMULACAO` só com pedido montado em memória.
- **SC-001 em termos de tempo (5 s):** não medido.
- Os dobros em memória (`synchronized`) provam a lógica de concorrência, não o bloqueio de linha do Oracle.

## Desvios

1. **Código antes do teste** (contra a regra "teste visto falhar antes"): `ValidadorDeLeitura`, `LeitorSimulado`, a persistência, o serviço e o controller foram escritos antes dos testes. O substituto foi a verificação por mutação (acima), que só cobre duas regras; as demais não foram vistas falhar.
2. **Sem classes `*ServiceTest` separadas:** o serviço é exercitado pelos testes de endpoint (Jetty real, dobros em memória), como na RC-09.
3. **`CampoExtraido` (entidade) não existe:** o repositório e o serviço reutilizam o record `CampoLido` do contrato do leitor (evita um mapeamento idêntico).
4. **Rota `extracoes` (plural) e `id` na resposta:** o plano dizia `/extracao`; mudou durante a implementação para `POST/GET /solicitacoes/{id}/extracoes`, que é o nome já previsto no `CONTRATO_API.md` (e que a RC-11 usa em `…/extracoes/{extracaoId}/conferencia`). `spec.md` não cita a rota; plano, contrato, quickstart e tasks foram atualizados.
5. **T012/T019:** em vez de um stub 405 na Fundação, o `ExtracaoController` real foi criado direto e ligado em `Composicao` (o resultado final é o mesmo do T019).
6. **`Dependencias` ganhou `extracoes` e `leitor`:** exigiu ajustar 6 testes existentes que a constroem (só o construtor; nenhuma asserção foi alterada ou enfraquecida).
7. **Exclusão de rascunho (RC-09):** `SolicitacaoRepositoryJdbc.excluirRascunho` e o dobro em memória passaram a apagar também as leituras; `DocumentoRepositoryMemoria` não mudou.
8. **Arquivo ausente na área privada** devolve 503 `DEPENDENCIA_INDISPONIVEL` (não vira leitura vazia); documento da carga sintética (sem arquivo) vira `LEITURA_VAZIA`.

## Rastreabilidade

| Requisito | Tarefas | Testes |
|---|---|---|
| FR-001 a FR-005 (ler, consultar, perfis, dona, "não encontrado") | T016–T018, T027 | `ExtrairEndpointTest`, `MatrizPerfisExtracaoTest` |
| FR-006, FR-008 (campos das regras, origem) | T004, T013, T014 | `LeitorSimuladoTest`, `ExtrairEndpointTest` |
| FR-007, FR-009, FR-010 (três situações, sem suposição) | T006, T020, T021 | `ValidadorDeLeituraTest`, `SituacoesDeCampoTest` |
| FR-011 a FR-013 (leitor simulado, aviso, desconhecido) | T014, T015, T030 | `LeitorSimuladoTest`, `SemLeituraRealTest`, `ExtrairEndpointTest` |
| FR-014 a FR-018 (protegido, vazio, falha, independência) | T022, T023 | `LeituraMalSucedidaTest` |
| FR-019 a FR-022 (versão, sem duplicar, submetido, estado intacto) | T024–T026 | `ExtracaoDesatualizadaTest`, `ConcorrenciaExtracaoTest`, `ExtrairEndpointTest` |
| FR-023 (conteúdo é dado) | T020 | `SituacoesDeCampoTest` (texto-injeção) |
| FR-024, FR-025 (sem vazamento) | T028 | `HigieneDeLogExtracaoTest`, `ExtrairEndpointTest` |
| FR-026, FR-027 (erros padrão e documentação) | T029, T031–T033 | `BancoEArmazenamentoIndisponiveisExtracaoTest`; OpenAPI/Postman/docs |
| SC-001, SC-002 | T015, T020 | `LeitorSimuladoTest`, `SituacoesDeCampoTest` |
| SC-003 | T022 | `LeituraMalSucedidaTest` |
| SC-004 | T016, T030 | `SemLeituraRealTest`, `ExtrairEndpointTest` |
| SC-005 | T024 | `ExtracaoDesatualizadaTest` |
| SC-006 | T025 | `ConcorrenciaExtracaoTest` (em memória); Oracle: `ExtracaoOracleTest` **não executado** |
| SC-007 | T027 | `MatrizPerfisExtracaoTest`, `ExtrairEndpointTest` |
| SC-008 | T028 | `HigieneDeLogExtracaoTest` |

**Só documentado / não executado:** casos 18–22 do `04_`, `quickstart.md`, medição de tempo.
