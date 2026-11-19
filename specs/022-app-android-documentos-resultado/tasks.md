# Tasks: App Android — envio de documentos e resultado da análise (RC-19)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-consumida.md](contracts/api-consumida.md), [quickstart.md](quickstart.md)

**Status (17/11/2026)**: todas as tarefas executadas; resultados e limites em [docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md](../../docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md). SC-008 (avaliação com pessoas) não realizada.

**Tests**: exigidos pela #42 (MockWebServer das rotas de documentos, leitura e análise) e pelas regras de apresentação da spec 019.

Caminhos relativos a `mobile/app/src/` salvo indicação; `A/` = `main/java/br/com/regulacheck/app/`, `T/` = `test/java/br/com/regulacheck/app/`.

## Phase 1: Setup

- [X] T001 Conferir base (branch 020 a partir da 017; PR #71 aberto) e o build atual verde com `.\gradlew.bat assembleDebug testDebugUnitTest` em `mobile/app/` — base 017 (`17c8ef2`, PR #71 aberto); build de base verde

## Phase 2: Foundational (bloqueia todas as histórias)

- [X] T002 Modelos do contrato da RC-19 (`Protocolo`, `ListaProtocolos`, `NovaSolicitacao`, `Documento`, `ListaDocumentos`, `DocumentoEnviado`, `Extracao`, `HistoricoAnalises`, `AnaliseGravada`, `PendenciaGravada`, `ResumoSimulado`) em `A/api/Modelos.kt`
- [X] T003 `ErroApi.ArquivoInacessivel` e `Indisponivel(status, codigo)` em `A/api/ErroApi.kt`; mapeamento em `A/api/ApiClient.kt`
- [X] T004 `FonteArquivo`, `ArquivoInacessivelException` e corpo multipart em streaming em `A/api/Envio.kt`; `RequisicaoApi` com corpo multipart e cabeçalhos extras em `A/api/ApiClient.kt` (research D1, D2)
- [X] T005 Rotas da RC-19 (`listarProtocolos`, `criarSolicitacao`, `listarDocumentos`, `enviarDocumento`, `lerDocumentos`, `analisar`, `consultarAnalises`, `gerarResumo`) em `A/api/RegulaCheckApi.kt`
- [X] T006 [P] Rótulos dos valores da API (tipo documental, campo, situação, resultado por regra, responsável, motivo, formato/tamanho, versão do protocolo) em `A/ui/RotulosApi.kt`
- [X] T007 [P] Textos da RC-19 (E2.1, E2.2, E2.3, E2.7, E3.1, E3.8, E3.3, E3.4, E4.3) em `A/ui/Textos.kt`
- [X] T008 [P] `SeloSimulado`, cores `rc-simulated`, `CabecalhoVoltar(titulo, simulado)`, `Faixa` e `OpcaoSelecao` em `A/ui/componentes/` e `A/ui/tema/Cores.kt`
- [X] T009 Telas novas em `Tela`, `Navegador.substituir` em `A/ui/navegacao/Navegacao.kt`; roteamento em `A/ui/navegacao/AppRegulaCheck.kt`
- [X] T010 [P] Testes do multipart (partes `arquivo`/`tipoDocumental`, `X-Chave-Envio`, arquivo inacessível sem derrubar o processo) em `T/api/EnvioDocumentosTest.kt`

**Checkpoint**: build verde; rotas e modelos prontos.

## Phase 3: User Story 1 — Criar um pedido (P1) 🎯 MVP

**Goal**: E2.1 com o primeiro serviço, criação bloqueada durante o envio, falhas recuperáveis e sem serviços.
**Independent Test**: criar um rascunho e vê-lo em "Meus pedidos" com o mesmo serviço.

- [X] T011 [US1] `NovoPedidoViewModel` + `NovoPedidoTela` (Padrão, Criando, Erro, Sem serviços, tempo esgotado com aviso de possível criação) em `A/ui/novopedido/NovoPedidoTela.kt`
- [X] T012 [US1] "Novo pedido" de "Meus pedidos" abre E2.1; criado → `substituir(Documentos(id))` em `A/ui/pedidos/MeusPedidosTela.kt` e `A/ui/navegacao/AppRegulaCheck.kt`
- [X] T013 [P] [US1] Testes de `GET /protocolos` (vazio, lista) e `POST /solicitacoes` (201, 422) em `T/api/NovoPedidoApiTest.kt`

## Phase 4: User Story 2 — Enviar documentos (P1)

**Goal**: E2.2 + E2.7 + E2.3 com confirmação da API, chave reutilizada e recusas.
**Independent Test**: anexar, cortar a rede, repetir e conferir um único documento.

- [X] T014 [US2] `FonteArquivoUri` (nome, tamanho, tipo por `ContentResolver`) em `A/ui/documentos/FonteArquivoUri.kt`
- [X] T015 [US2] `DocumentosViewModel` (carga pedido+lista, envio em curso: enviando, não enviado, recusado, escolher de novo, pedido fechado) em `A/ui/documentos/DocumentosViewModel.kt`
- [X] T016 [US2] `DocumentosTela` (Vazia, Com arquivos, Enviando, Não enviado, recusa, limite, pedido verificado/submetido, "Ler documentos") com folha de tipo e `OpenDocument` em `A/ui/documentos/DocumentosTela.kt`
- [X] T017 [US2] Ação "Anexar documentos" do pedido e linha "Documentos" navegam para E2.2 em `A/ui/pedido/PedidoTela.kt`, `A/ui/EstadoPedidoUi.kt`
- [X] T018 [P] [US2] Testes MockWebServer: envio 201, repetição 200 com a mesma chave, 413, 415, 422 limite, 409 chave reutilizada em `T/api/EnvioDocumentosTest.kt`
- [X] T019 [P] [US2] Testes do `DocumentosViewModel` (só lista após 201/200; "Tentar de novo" reutiliza fonte e chave; recusa não lista; inacessível pede nova escolha) em `T/ui/DocumentosViewModelTest.kt`

## Phase 5: User Story 3 — Ler e verificar (P1)

**Goal**: E3.1 + E3.8 com bloqueio, sem retentativa automática, histórico consultado após tempo esgotado.
**Independent Test**: toques repetidos geram uma única análise.

- [X] T020 [US3] `decidirAposFalhaDaAnalise` (research D6) em `A/ui/leitura/DecisaoAnalise.kt`
- [X] T021 [US3] `LeituraViewModel` (ler ao abrir; analisar após confirmação; 409/503/rede; resumo uma vez após sucesso) em `A/ui/leitura/LeituraViewModel.kt`
- [X] T022 [US3] `LeituraTela` (Lendo, Lida, Indisponível, Sem documentos, Documentos mudaram, diálogo E3.8, Analisando, Em andamento, Incerta) em `A/ui/leitura/LeituraTela.kt`
- [X] T023 [US3] Ações "Ler documentos" e "Tentar de novo" (falha) abrem E3.1 em `A/ui/pedido/PedidoTela.kt`, `A/ui/pedidos/MeusPedidosTela.kt`
- [X] T024 [P] [US3] Testes MockWebServer: 422 `SEM_DOCUMENTOS`, 409 sem leitura, 409 `ANALISE_EM_ANDAMENTO` com `Retry-After`, 503 leitura e análise, 201/200, tempo de 70 s em `T/api/LeituraAnaliseTest.kt`
- [X] T025 [P] [US3] Testes de `decidirAposFalhaDaAnalise` em `T/ui/DecisaoAnaliseTest.kt`

## Phase 6: User Story 4 — Resultado e pendência (P1)

**Goal**: E3.3 + E3.4 conforme o contrato da spec 019.
**Independent Test**: em 3 pedidos, pendências iguais a `GET …/analises`.

- [X] T026 [US4] `apresentarHistorico` (vigente, anterior, em andamento, falha, sem análise) e `DetalhePendencia` em `A/ui/resultado/ApresentacaoResultado.kt`
- [X] T027 [US4] `ResultadoViewModel` + `ResultadoTela` (resumo SIMULADO/indisponível/PADRAO com "Tentar de novo", 409 reconsulta, O que falta, Outros critérios, avisos, Confira a leitura, Sem pendências, erros 403/404/503) em `A/ui/resultado/ResultadoTela.kt`
- [X] T028 [US4] `PendenciaTela` (critério, versão, situação, responsável registrado, orientação, evidências com página/trecho só se existirem, limite do reenvio) em `A/ui/pendencia/PendenciaTela.kt`
- [X] T029 [US4] Ações "Ver pendências", "Ver o que conferir", "Ver resultado" e linha "Resultado" abrem E3.3 em `A/ui/pedido/PedidoTela.kt`, `A/ui/pedidos/MeusPedidosTela.kt`, `A/ui/EstadoPedidoUi.kt`
- [X] T030 [P] [US4] Testes de `apresentarHistorico` e do detalhe (nome do arquivo por `documentoId`, sem trecho inventado, texto literal) em `T/ui/ApresentacaoResultadoTest.kt`
- [X] T031 [P] [US4] Testes de `RotulosApi` (valores conhecidos e desconhecidos) em `T/ui/RotulosApiTest.kt`

## Phase 7: User Story 5 — Conexão ruim e acessibilidade (P2)

**Goal**: falhas legíveis com recuperação e rótulos de acessibilidade nas telas novas.
**Independent Test**: rede cortada em cada operação; leitor de tela nos campos e ícones.

- [X] T032 [US5] Revisar textos E4.3, preservação da escolha e semântica (cabeçalhos, rótulos, `liveRegion`, alvos ≥ 48 dp, estado com texto + ícone) nas telas de `A/ui/novopedido/`, `A/ui/documentos/`, `A/ui/leitura/`, `A/ui/resultado/`, `A/ui/pendencia/`
- [X] T033 [US5] Remover o aviso "etapa ainda não disponível" e o campo `AcaoPedido.disponivel` que deixou de ser usado em `A/ui/EstadoPedidoUi.kt`, `A/ui/Textos.kt`, `A/ui/pedidos/MeusPedidosTela.kt`

## Phase 8: Polish & verificação

- [X] T034 `.\gradlew.bat assembleDebug testDebugUnitTest` verde (registrar saída) — 82 testes, 0 falhas (17/11/2026)
- [X] T035 Comparar cada tela no emulador com as prévias PNG (`adb exec-out screencap -p`) e ajustar — 8 telas comparadas; ajustes: limites antes do botão (t31hN), largura do diálogo (Xe5GF), frase da evidência
- [X] T036 Percurso real cronometrado (≤ 5 min), 3 pedidos conferidos com `GET …/analises`, arquivo inválido, rede cortada no envio e na análise, toques repetidos, `logcat` do processo do app em `docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md` — feito com backend real (duas execuções; a segunda com a `main` e os campos da RC-15); não verificado: 409 em andamento real, SC-008
- [X] T037 [P] Atualizar `mobile/README.md` (implementado × só desenho × não verificado) e `docs/ambiente.md` se algo mudar
- [X] T038 [P] Registrar os textos RC-19 em `mobile/design/textos.md`
- [X] T039 Revisar o diff (sem segredo, sem log sensível, sem código morto) e marcar tarefas com o resultado real

## Phase 9: Ajuste de textos (19/11/2026)

Decisão em spec.md, Clarifications, sessão 2026-11-19 (FR-018, FR-019, FR-020, FR-022, FR-026). Só texto; nenhuma chamada à API muda.

- [X] T040 Resultado: tirar a revisão de "Verificado em", o selo e a nota do resumo, o aviso da API fora de "Sem pendências identificadas" e a frase "só no desenho" (`ui/Textos.kt`, `ui/resultado/ResultadoTela.kt`)
- [X] T041 Tirar o código da regra das linhas de "O que falta" e "Outros critérios" e do apoio do detalhe; teste da linha sem código (`ui/resultado/ApresentacaoResultado.kt`, `ui/resultado/ResultadoTela.kt`, `ui/pendencia/PendenciaTela.kt`, `ApresentacaoResultadoTest.kt`)
- [X] T042 Tirar as frases "só no desenho" da leitura e das orientações do detalhe (`ui/leitura/LeituraTela.kt`, `ui/resultado/ApresentacaoResultado.kt`)
- [X] T043 [P] Atualizar `mobile/design/textos.md` e `mobile/README.md`
- [X] T044 `.\gradlew.bat assembleDebug testDebugUnitTest` verde e telas conferidas no emulador — 123 testes, 0 falhas (19/11/2026, com o contorno de loopback de `docs/ambiente.md`); resultado "Confira a leitura" (pedido 148), "Com pendências" e detalhe da RC-R04 (pedido 2) conferidos com o backend real: sem revisão, sem código da regra, resumo sem selo e sem nota, aviso permanente e rodapé mantidos

## Dependencies & Execution Order

- Setup → Foundational (T002–T010) → US1 → US2 → US3 → US4 → US5 → Polish. US1–US4 compartilham `AppRegulaCheck.kt` e `PedidoTela.kt`, por isso seguem em sequência; os testes [P] de cada fase podem rodar em paralelo à tela da mesma fase.
- US3 depende de US2 só no percurso (precisa de documentos); US4 depende de US3 só no percurso (precisa de análise); as funções puras e os testes são independentes.

## Parallel Example

```text
T006 RotulosApi.kt · T007 Textos.kt · T008 componentes · T010 EnvioDocumentosTest.kt
T024 LeituraAnaliseTest.kt · T025 DecisaoAnaliseTest.kt
T030 ApresentacaoResultadoTest.kt · T031 RotulosApiTest.kt
```

## Implementation Strategy

MVP = US1 + US2 (pedido com documentos confirmados). Em seguida US3 e US4, que completam o percurso da #42; US5 consolida falhas e acessibilidade; a verificação real (T036) só é marcada com evidência.

## Rastreabilidade

RC-19 (#42) ↔ spec 022 (FR-001–FR-030, SC-001–SC-008) ↔ T001–T039. FR-001–004 → T011–T013 · FR-005–011 → T014–T019 · FR-012–016 → T020–T025 · FR-017–024 → T026–T031 · FR-025–030 → T008, T032, T033, T036 · SC-001–006 → T036 · SC-007 → T010, T018, T024 · SC-008 → T036 (avaliação com pessoas: registrada como realizada ou não). Ajuste de textos de 19/11/2026 (FR-018, FR-019, FR-020, FR-022, FR-026) → T040–T044.
