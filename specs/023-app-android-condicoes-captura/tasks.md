# Tasks: App Android — condições da captura (RC-22, parte do app)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-consumida.md](contracts/api-consumida.md), [quickstart.md](quickstart.md)

**Backlog**: RC-22 (#47), Épico 7; dependência do Épico 6 (#40). Backend e simulador: spec 016 (não alterados).

**Tests**: pedidos explicitamente pela #47 e pelo usuário (MockWebServer): abrir sessão (201 e 200), cada `situacao`, SEM_LEITURA sem sessão, transição fora → dentro, 409/403/404, sem conexão levando a "indisponível" e atualização sem sobreposição.

Caminhos relativos a `mobile/app/src/` (código) ou à raiz do repositório (documentação).

## Phase 1: Setup

- [X] T001 Conferir que a branch `023-app-android-condicoes-captura` parte da `main` atualizada (RC-18 #71 e RC-19 #74 mesclados) e que `.specify/feature.json` aponta para `specs/023-app-android-condicoes-captura`

## Phase 2: Foundational (contrato consumido)

- [X] T002 [P] Acrescentar os modelos `SessaoCaptura`, `Telemetria`, `SessaoTelemetria`, `SensorTelemetria`, `FaixaSensor`, `UltimaLeitura` e `OrientacaoSensor` em `main/java/br/com/regulacheck/app/api/Modelos.kt`, conforme o contrato da spec 016 (campos opcionais quando a API os omite)
- [X] T003 Acrescentar `abrirSessaoCaptura(id)` (`POST …/sessoes-captura`) e `consultarTelemetria(id)` (`GET …/leituras`) em `main/java/br/com/regulacheck/app/api/RegulaCheckApi.kt`
- [X] T004 [P] Acrescentar os textos de E2.7 Origem e E4.1 (incluindo os ajustados de D2 e os locais por código de orientação de D7) em `main/java/br/com/regulacheck/app/ui/Textos.kt`
- [X] T005 [P] Teste de contrato em `test/java/br/com/regulacheck/app/api/TelemetriaApiTest.kt`: POST 201 e 200 (sem corpo, com token, sem identificador da paciente); GET com cada `situacao`; SEM_LEITURA sem `sessao` nem `ultimaLeitura`; 409/403/404/503 mapeados em `ErroApi`

## Phase 3: User Story 1 — Ver as condições da captura antes de anexar uma foto (P1)

**Goal**: folha com o passo Origem; E4.1 com os dois sensores e o caminho até o seletor.

**Independent Test**: com a sequência `normal`, ver os dois sensores "Adequada", seguir para "Escolher a foto" e anexar uma imagem.

- [X] T006 [US1] Criar as funções puras `apresentarTelemetria`, `formatarValor` e `formatarIdade`, com `ApresentacaoCaptura`, `ApresentacaoSensor`, `SituacaoSensorUi` e `MemoriaCaptura`, em `main/java/br/com/regulacheck/app/ui/captura/ApresentacaoCaptura.kt` (data-model e research D4–D7)
- [X] T007 [US1] Criar `CondicoesCapturaViewModel` (`novaVisita`, `acompanhar` com sessão única por visita, laço sequencial de 4 s com `Mutex.tryLock` e estados `Carregando`/`Exibindo`/`Encerrada`) em `main/java/br/com/regulacheck/app/ui/captura/CondicoesCapturaViewModel.kt` (research D3/D6)
- [X] T008 [US1] Criar `CondicoesCapturaTela`: cabeçalho com SIMULADO, título e apoio, cartões dos sensores (texto e ícone), nota de demonstração, ações principal e "Escolher arquivo", e `repeatOnLifecycle(STARTED)` chamando `acompanhar`, em `main/java/br/com/regulacheck/app/ui/captura/CondicoesCapturaTela.kt`
- [X] T009 [US1] Na `DocumentosTela`, acrescentar o passo "Como você quer anexar?" à folha, abrir a E4.1 como passo do anexar (`capturaTipo` salvo e `BackHandler`) e abrir o seletor com JPEG/PNG (foto) ou PDF/JPEG/PNG (arquivo), em `main/java/br/com/regulacheck/app/ui/documentos/DocumentosTela.kt`, e passar a fábrica do ViewModel em `main/java/br/com/regulacheck/app/ui/navegacao/AppRegulaCheck.kt`

## Phase 4: User Story 2 — Orientação muda com as leituras (P1)

**Goal**: fora da faixa mostra a orientação da API; a volta à faixa mostra "Adequada de novo"; as consultas não se sobrepõem e param fora de vista.

**Independent Test**: com a E4.1 aberta, rodar `fora-da-faixa` e depois `recuperacao`.

- [X] T010 [P] [US2] Testes puros em `test/java/br/com/regulacheck/app/ui/ApresentacaoCapturaTest.kt`: normal, luminosidade baixa/alta, distância fora, recuperada (fora → dentro, com "antes"), marca que some ao sair de novo, fora → desatualizada → dentro, idade pelo relógio do servidor (nunca negativa) e valor em pt-BR
- [X] T011 [P] [US2] Testes do ViewModel com MockWebServer em `test/java/br/com/regulacheck/app/ui/CondicoesCapturaViewModelTest.kt`: sessão 201/200 seguida de consulta; transição fora → dentro entre consultas; no máximo uma consulta em andamento com a resposta lenta; nenhuma consulta depois do cancelamento; `Mutex` impede um segundo laço

## Phase 5: User Story 3 — Leitura antiga, indisponível e erros não bloqueiam (P2)

**Goal**: desatualizada e indisponível mantêm a ação "… mesmo assim"; 403/404/409 explicam e param.

**Independent Test**: esperar mais de 60 s sem envio; cortar a rede do emulador; anexar nos dois casos.

- [X] T012 [US3] Completar os testes de `test/java/br/com/regulacheck/app/ui/CondicoesCapturaViewModelTest.kt` e `ApresentacaoCapturaTest.kt`: desatualizada com "mesmo assim"; SEM_LEITURA nos dois sensores → indisponível; sem conexão e 503 → `Exibindo` indisponível (não `Encerrada`), com a consulta continuando; 409 no POST → `Encerrada` sem consultar; 401/403/404 → `Encerrada`, com a consulta parando
- [X] T013 [US3] Na `CondicoesCapturaTela`, ligar os blocos de `Encerrada` (403 sem permissão; 404 com "Voltar para meus pedidos"; 409 "pedido já submetido" com "Voltar", que recarrega os documentos), em `main/java/br/com/regulacheck/app/ui/captura/CondicoesCapturaTela.kt`

## Phase 6: Polish & verificação

- [X] T014 Rodar `./gradlew assembleDebug testDebugUnitTest` em `mobile/app/` (JDK 21 do Android Studio) e corrigir até ficar verde
- [X] T015 Registrar os textos criados e ajustados em `mobile/design/textos.md` (E2.7 Origem e E4.1, com o motivo)
- [X] T016 Percurso no emulador com o backend real, com o usuário rodando o simulador: normal, fora da faixa, recuperação, desatualizada, indisponível (`svc wifi/data disable`), anexo depois da E4.1 dentro e fora da faixa, e logcat sem token, senha, chave nem corpo de documento
- [X] T017 [P] Registrar a verificação em texto em `docs/evidencias/2026-11-18_rc22-condicoes-captura-app.md` (sem capturas, PNG nem resultados de teste versionados)
- [X] T018 [P] Atualizar `mobile/README.md` (implementado × só desenho × não verificado; o que cada sensor mede e que não comprova a legibilidade) e `docs/ambiente.md`, se algo mudar
- [X] T019 Marcar o progresso real neste arquivo e registrar o resultado da verificação
- [X] T020 Ajustes da revisão automática do PR #80 (18/11/2026): teste do 503 confere que a consulta continua; teste de sobreposição espera pelo número de consultas, não pelo relógio; `TelemetriaApiTest` confere as situações dos dois sensores; teste novo para "recuperada → desatualizada/sem leitura → dentro" e research D5 restrito a esse comportamento; SC-002 com o limite da resposta da API; nomes das situações em data-model; quickstart com `JAVA_HOME` do Git Bash, senha de demonstração, uma sequência por comando e `adb shell svc` com religar a rede; exceções registradas na checklist
- [X] T021 Ajustes da segunda revisão automática do PR #80 (18/11/2026): FR-007 com a exceção dos dois sensores sem leitura (vale o FR-009); SC-005 e quickstart passam a incluir o corpo da telemetria no logcat; quickstart com `adb push` da amostra e `IOT_API_KEY` no terminal do simulador; research D3 (a volta do segundo plano só pede a sessão se ela não foi confirmada); diagrama do data-model com 5xx no POST
- [X] T022 Reconferir o logcat do processo do app no emulador procurando o corpo da telemetria (`consultadaEm`, `ultimaLeitura`, `DENTRO_DA_FAIXA`), exigido pelo SC-005 desde T021. A conferência de T016 não procurou esses termos. Feito em 18/11/2026 com o backend real (seção 5 da [evidência](../../docs/evidencias/2026-11-18_rc22-condicoes-captura-app.md)): 0 ocorrências no processo do app

## Dependencies & Execution Order

- T001 → Phase 2. T002 → T003 → (T005, T006, T007). T004 bloqueia T008 e T009.
- US1 (T006–T009) é o MVP. Os testes da US2 (T010, T011) dependem de T006 e T007. A US3 depende do ViewModel (T007) e da tela (T008).
- Phase 6 depois de todas as histórias. T016 depende do usuário (backend com Oracle e simulador).

## Parallel Example

```text
T002 (Modelos.kt) ∥ T004 (Textos.kt)
T010 (ApresentacaoCapturaTest.kt) ∥ T011 (CondicoesCapturaViewModelTest.kt)
T017 (evidência) ∥ T018 (README)
```

## Implementation Strategy

O MVP é a US1 (folha com Origem e E4.1 com os sensores, até o seletor). A US2 e a US3 completam os estados do desenho e os erros, sobre o mesmo ViewModel. Tudo é entregue numa só alteração do app, sem commit nem PR (o usuário decide).

## Resultado da verificação (18/11/2026)

- **Automática**: `./gradlew assembleDebug testDebugUnitTest` em `mobile/app/` → BUILD SUCCESSFUL, 112 testes, 0 falhas (30 novos: `TelemetriaApiTest` 6, `ApresentacaoCapturaTest` 13, `CondicoesCapturaViewModelTest` 11).
- **Depois do merge da RC-20 e dos ajustes de T020** (18/11/2026): mesmo comando → BUILD SUCCESSFUL, 121 testes, 0 falhas (`ApresentacaoCapturaTest` passa a 14). T020 não mudou código de produção, e o percurso no emulador não foi refeito.
- **Emulador** (AVD `RC22_Phone`, backend da 023 com Oracle, simulador rodado pelo responsável, sessão 41 do pedido 119): conferidos o passo Origem e os estados indisponível (sem leitura e sem rede), desatualizada, normal, luminosidade e distância fora da faixa e recuperada. Também o anexo depois da E4.1, dentro e fora da faixa, e o logcat do app sem dados sensíveis. Detalhes em [docs/evidencias/2026-11-18_rc22-condicoes-captura-app.md](../../docs/evidencias/2026-11-18_rc22-condicoes-captura-app.md).
- **Só nos testes**: 403, 404 e 409 da E4.1. **Não verificados**: TalkBack, fonte ampliada e rotação. **Reconferido** (T022, 18/11/2026): o corpo da telemetria não aparece no logcat do processo do app (0 ocorrências em 106 linhas, com a E4.1 recebendo telemetria do backend real).
- **Rastreabilidade**:
  - Critério 1 da #47 (consultar a telemetria e exibir os dois sensores) → FR-004 e FR-006 → T003, T006–T009.
  - Critério 2 (orientação fora da faixa; a recuperação muda o aviso) → FR-007 e FR-008 → T006, T010, T011.
  - Critério 3 (desatualizada; a indisponibilidade não bloqueia) → FR-009 e FR-010 → T007, T012, T013.
  - Critério 4 (documentação dos sensores) → FR-015 → T018.
