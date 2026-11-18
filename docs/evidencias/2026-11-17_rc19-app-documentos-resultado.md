# RC-19 — App Android: envio de documentos e resultado (verificação de 17/11/2026)

História [RC-19 #42](https://github.com/Code-Masters-F/RegulaCheck/issues/42) · spec [022](../../specs/022-app-android-documentos-resultado/spec.md) · tarefas [T001–T039](../../specs/022-app-android-documentos-resultado/tasks.md). Registro só em texto: as capturas de tela usadas na conferência com as prévias não foram versionadas.

**Ambiente**: emulador `Medium_Phone` (1080 × 2400, API 37) · Gradle com o JDK 21 do Android Studio · backend e Oracle reais, iniciados pelo responsável (`GET /health` → `{"status":"UP"}`; `.env` não lido pelo agente) · conta fictícia `paciente.a@exemplo.invalid`, digitação por `adb shell input` · amostras de `samples/` copiadas para a pasta Download do emulador. Duas execuções:

1. **Primeira** (seções 2 a 5): branch da feature ainda sobre a RC-18, backend anterior à RC-15 (sem `rotuloResultado` nem `pendencias[].orientacao`; o app usou o rótulo local, research D12).
2. **Segunda** (seção 6): branch `022-app-android-documentos-resultado` com a `main` (RC-15, RC-16, RC-17) e backend reiniciado a partir dela; textos ajustados depois da RC-16 (research D13).

## 1. Testes automatizados

```text
mobile/app> .\gradlew.bat assembleDebug testDebugUnitTest   (JAVA_HOME = jbr do Android Studio)
BUILD SUCCESSFUL — 82 testes, 0 falhas, 0 ignorados
```

| Suíte | Testes | O que cobre (#42) |
|---|---|---|
| `api.EnvioDocumentosTest` | 6 | multipart (`arquivo`, `tipoDocumental`, `X-Chave-Envio`); **201** e repetição **200** com a mesma chave; **413** e **415** com a mensagem da API; 422 limite; 409 chave reutilizada; arquivo que não abre não derruba o processo; 503 com código |
| `api.LeituraAnaliseTest` | 11 | leitura 201/200; **422 `SEM_DOCUMENTOS`**; **503** na leitura e na análise; **409 sem leitura** (`CONFLITO_ESTADO`); **409 `ANALISE_EM_ANDAMENTO` com `Retry-After`**; leitura de 70 s só em ler/analisar; histórico; resumo `PADRAO` e 409; backend sem os campos da RC-15 |
| `api.NovoPedidoApiTest` | 3 | `GET /protocolos` (lista e vazia); `POST /solicitacoes` 201 e 422 |
| `ui.FluxoViewModelTest` | 13 | documento só entra após 201; "Tentar de novo" reutiliza arquivo e chave, repetição 200 não duplica; recusa não lista; arquivo inacessível pede nova escolha; análise sem resumo pede o resumo uma vez; queda na análise consulta o histórico antes de nova tentativa (resultado gravado, nada gravado, consulta também falhou); 409 em andamento sem repetir; resumo: nova tentativa, 409 reconsulta, falha mantém pendências |
| `ui.ApresentacaoResultadoTest` | 11 | só a análise com `vigenteId` **e** `vigente`; anterior só como "Resultado anterior"; em andamento/falha/sem análise sem conclusão; rótulo da RC-15 com alternativa local; nome do arquivo pelo `documentoId`; trecho literal (`<a> & b`); sem origem, sem trecho nem página; unidade emissora × reenvio indisponível; RC-R02 condicional |
| `ui.RotulosApiTest` | 4 | rótulos da spec 015 e valores desconhecidos neutros |
| RC-18 (`ApiClientTest`, `ServicoSessaoTest`, `EstadoPedidoUiTest`) | 34 | sem regressão; o teste "só Atualizar funciona" (decisão D7 da RC-18) foi trocado por um que confere os rótulos das ações, porque a RC-19 substitui essa decisão |

## 2. Primeira execução: percurso cronometrado

Relógio do computador (UTC−3). A digitação foi automatizada por `adb`, então o tempo é menor que o de uma pessoa; o tempo de uma pessoa **não foi medido**.

| Hora | Passo |
|---|---|
| 18:36 | Entrar (uma primeira tentativa errou o toque no campo de senha; nada foi enviado) |
| 18:37:15 | "Novo pedido" → serviço demonstrativo com o aviso fictício do backend |
| 18:37:27 | "Criar rascunho" → pedido **111**, "Seus documentos" |
| 18:38:11 | Folha de tipo → seletor do sistema (só PDF/JPEG/PNG, sem permissão de armazenamento) → `encaminhamento-sintetico.pdf`; "Enviando…" e, após a API, "Enviado" |
| 18:38:34 | Identificação → `identificacao-sintetica.png` |
| 18:38:48 | "Ler documentos" → leitura simulada (SIMULADO), 2 documentos lidos |
| 18:39:16 | "Verificar critérios" → diálogo → **dois toques** em confirmar e um no botão de baixo → "Analisando…" → resultado "Confira a leitura" com resumo SIMULADO |
| 18:40:10 | Pendência RC-R01: critério, versão, evidências com página e trecho literal, situação, responsável registrado e o limite desta versão em texto |

**Tempo**: de "Novo pedido" ao resultado, 2 min 01 s; da tela de login à pendência aberta, cerca de 4 min — dentro dos 5 min da #42 (SC-001), com a ressalva da digitação automatizada.

## 3. Pendências iguais a `GET /solicitacoes/{id}/analises`

Consulta feita por script com a mesma conta logo depois de cada percurso (token e senha não impressos).

| Pedido | API | Tela | Igual? |
|---|---|---|---|
| 111 (novo) | 1 análise (57, vigente, revisão 3) `REQUER_CONFERENCIA`; RC-R01 a RC-R05 `NAO_VERIFICAVEL`/`TRANSCRICAO_NAO_CONFERIDA` e RC-R06 `NAO_VERIFICAVEL`/`CONDICAO_INDETERMINADA`, todas `PACIENTE`; resumo `ADAPTADOR_MOCKADO` | "Confira a leitura", revisão 3, resumo SIMULADO, as mesmas 6 pendências "Não verificável · Quem resolve: você" | ✅ |
| 112 (novo) | 1 documento; 1 análise (58, vigente, revisão 2) `REQUER_CONFERENCIA`; as mesmas 6 pendências | idem, revisão 2 | ✅ |
| 2 (carga) | análise 3 vigente `COM_PENDENCIAS`; RC-R04 e RC-R05 `PENDENTE`/`UNIDADE_EMISSORA`; RC-R01/02/03 `ATENDE`; RC-R06 `NAO_APLICAVEL` | "Com pendências"; O que falta: RC-R04, RC-R05 "Quem resolve: unidade emissora"; Outros critérios iguais | ✅ |
| 118 (novo, segunda execução) | 1 análise (66, vigente, revisão 3) `REQUER_CONFERENCIA`, `rotuloResultado` "Confira a leitura", resumo `ADAPTADOR_MOCKADO`, as mesmas 6 pendências | idem, com o rótulo vindo da API | ✅ |

## 4. Falhas e toques repetidos (pedido 112, primeira execução)

| Caso | Resultado |
|---|---|
| Arquivo inválido (texto com extensão `.pdf`) | ✅ Faixa "“texto-renomeado.pdf” não foi enviado." + "Envie um arquivo PDF, JPEG ou PNG." + "Nada foi salvo." + "Escolher outro arquivo"; o arquivo não entrou na lista |
| Rede cortada no envio (`svc wifi/data disable`) | ✅ "Não enviado" + "O envio de … foi interrompido." + "Tentar de novo" (o texto do corpo foi depois ajustado para "Sem conexão com a internet. O arquivo ainda não foi enviado…") |
| Rede religada + **dois toques** em "Tentar de novo" | ✅ "Enviado"; a API mostra **1** documento no pedido |
| Rede cortada antes de confirmar a verificação ("backend parado durante a análise") | ✅ "Não foi possível confirmar a operação…" + "Consultar de novo" — sem resultado presumido |
| Rede religada + "Consultar de novo" | ✅ O histórico confirmou que nada foi gravado; só então "Tentar novamente"; depois, análise 58 e o resultado |
| **Três toques** em "Criar rascunho" | ✅ Só o pedido 112 foi criado (`GET /solicitacoes`) |
| Dois toques em "Verificar critérios" | ✅ Pedidos 111 e 118 com **1** análise cada |

## 5. Logcat

- **Primeira execução** (`adb logcat -d`, 80.792 linhas; 96 do processo do app): senha de demonstração 2 e e-mail 3 ocorrências, **todas do `adbd`** (`adb shell input text` da digitação automatizada); "token" só em serviços do sistema (`WindowManager`, tokens de janela); **nenhum** `Bearer`/`Authorization`, conteúdo de documento ou `X-Chave-Envio`. As linhas do app são do framework (`WindowOnBackDispatcher`, `ImeTracker`, `Surface`…).
- **Segunda execução** (logcat limpo antes; 3.838 linhas, 122 do app): senha 0, `Bearer`/`Authorization` 0, conteúdo de documento 0, `X-Chave-Envio` 0; e-mail 1 vez, do `adbd`.

## 6. Segunda execução: `main` integrada e backend com a RC-15

Backend reiniciado pelo responsável a partir da branch `022-app-android-documentos-resultado`; jar gerado com `./mvnw clean package -DskipTests` depois de `./mvnw clean verify` com 1 falha conhecida do ambiente (`SimuladorSensoresProcessoTest`, ver `docs/ambiente.md`). O app mostra a hora no fuso do emulador (UTC).

| Hora | Passo | Resultado |
|---|---|---|
| 23:38 | Abrir o app com a sessão de antes do reinício do backend | ✅ Login com "Sua sessão terminou. Entre novamente." |
| 23:38:58 | Entrar | ✅ |
| 23:39:25 → 23:39:33 | "Novo pedido" → "Criar rascunho" | ✅ pedido **118** |
| 23:39:51 / 23:40:01 | Encaminhamento (PDF) e identificação (PNG) | ✅ "Enviado" após a API |
| 23:40:07 | Ler documentos | ✅ SIMULADO, 2 documentos lidos |
| 23:40:32 | Verificar critérios (diálogo sem a frase que a RC-16 invalidou; **dois toques** em confirmar) | ✅ "Confira a leitura" com o `rotuloResultado` da API |
| 23:40:48 | Pendência RC-R02 | ✅ quadro **"Quem pode resolver" com a `orientacao` da API** (texto puro), responsável registrado "Você", trechos literais com página |

**Tempo**: da entrada à pendência aberta, 1 min 50 s (digitação automatizada).

- **Pedido verificado** (E2.2): "Enviar uma versão corrigida ainda não está disponível neste app." e sem "Anexar documento".
- **Pedido 2, RC-R04** (unidade emissora): "Quem pode resolver" com "Solicite à unidade emissora a revisão do encaminhamento. O documento médico original deve ser corrigido pela unidade." e o aviso do app de que o original não é alterado no aplicativo, como no quadro `XCNVm`; `rotuloResultado` "Com pendências" conferido na API.

## 7. Comparação com o desenho (prévias em `design/previas/`)

| Tela | Prévia | Situação |
|---|---|---|
| E2.1 Novo pedido | `c78REx` | ✅ igual; acrescenta o aviso fictício do backend (#42) |
| E2.2 Vazia, Enviando, Com arquivos, Não enviado | `t31hN`, `hHwfV`, `CvQRm`, `WPH7O` | ✅ sem "Abrir" e "Mais opções" (fora da #42); "Ler documentos" com "Leitura simulada com dados sintéticos." (o desenho promete a conferência, que fica só no desenho) |
| E2.7 Tipo | `fwOMP` | ✅ igual; o passo "Origem" (`ncnpi`, "Fotografar") não existe: só o seletor de arquivo |
| E2.3 Recusa | `mNcfi` | ✅ igual (sem ícone de pasta no botão) |
| E3.1 Lendo | `QxKN9` | ✅ com botão "Lendo…" bloqueado (#42) |
| E3.8 Diálogo | `Xe5GF` | ✅ texto ajustado (sem a contagem de informações sem decisão e sem a frase sobre anexar depois) |
| E3.3 Resultado | `L5Keg`, `zMx9T` | ✅ sem "Conferir leitura" (só desenho) e com o aviso permanente da RC-15 |
| E3.4 Pendência | `XCNVm`, `HK8ZN` | ✅ sem botões ("Abrir documento", "Já tenho uma nova versão", "Substituir arquivo", "Conferir leitura"): as limitações são explicadas em texto |

## 8. Não verificado

- Com o backend real: 409 `ANALISE_EM_ANDAMENTO` (a análise termina em menos de 1 s), resumo ausente/`PADRAO` com "Tentar de novo" (todas as análises vieram com resumo), E2.1 erro e "sem serviços", 503 da leitura/análise — cobertos só pelos testes automatizados (seção 1).
- SC-008 (spec 019 SC-005): avaliação com ≥ 3 pessoas que não desenharam o fluxo — **não realizada**.
- Tempo de uma pessoa no percurso; TalkBack e fonte ampliada (fora do épico, #40).
- Observação: durante a primeira execução apareceu na conta da paciente A um pedido 113 que não foi criado por esta verificação; depois do reinício do backend ele não estava mais na lista.
