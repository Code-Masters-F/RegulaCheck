# Implementation Plan: App Android — condições da captura (RC-22, parte do app)

**Branch**: `023-app-android-condicoes-captura` | **Date**: 18/11/2026 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/023-app-android-condicoes-captura/spec.md` · Backlog RC-22 (#47), Épico 7; dependência do Épico 6 (#40) · Backend e simulador: [spec 016](../016-iot-simulada/plan.md) · Base do app: [plan 017](../017-app-android-login-pedidos/plan.md) e [plan 022](../022-app-android-documentos-resultado/plan.md)

## Summary

A folha E2.7 do app ganha o passo "Origem", com as opções "Fotografar" e "Escolher arquivo". O app também ganha a tela E4.1 Condições da captura, que pede a sessão de captura (`POST …/sessoes-captura`, idempotente) e consulta `GET …/leituras` a cada 4 s enquanto está visível. As consultas não se sobrepõem e param quando a tela sai de vista. A tela mostra os dois sensores simulados com situação em texto e ícone, a orientação da API, a idade da leitura, a nota de demonstração e o selo SIMULADO, nos seis estados do desenho. Em qualquer estado, a paciente segue para o mesmo seletor do sistema da RC-19, com o tipo já escolhido: o app não usa câmera.

A lógica de apresentação (situação, recuperação, idade, formatação) fica em funções puras testadas. O laço de atualização fica num ViewModel, testado com MockWebServer. As decisões estão em [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 17), Gradle 8.7 com o JDK 21 do Android Studio (`jbr`), sem mudança

**Primary Dependencies**: as da RC-18 e da RC-19. **Nenhuma dependência nova**: `repeatOnLifecycle` e `LocalLifecycleOwner` vêm do `lifecycle-runtime-compose` 2.8.6, que o app já usa.

**Storage**: nenhum; a sessão de captura e a memória de "recuperada" ficam só no ViewModel, durante a visita

**Testing**: JUnit 4, MockWebServer e coroutines-test (`testDebugUnitTest`); percurso no emulador com o backend real e o simulador rodado pelo usuário

**Target Platform**: Android minSdk 26, compile/targetSdk 34; emulador `Medium_Phone`; API em `http://10.0.2.2:8080`

**Project Type**: mobile-app (cliente da API)

**Performance Goals**: a mudança de leitura aparece em até 5 s (SC-002); a consulta usa os tempos padrão da #40 (conexão 10 s, leitura 30 s)

**Constraints**: nenhuma retentativa automática de operação que falhou; consultas sequenciais (nunca sobrepostas); nada é registrado em log; texto da API é exibido como texto puro; sensor nunca bloqueia o envio; backend e simulador não mudam

**Scale/Scope**: 1 tela nova (E4.1, 6 estados), 1 passo novo na folha, 2 rotas consumidas

## Constitution Check

| Princípio | Situação |
|---|---|
| I. Escopo acadêmico | ✅ Selo SIMULADO, "sensor simulado" em cada leitura, nota de valores de demonstração e rodapé acadêmico |
| II. Limites clínicos | ✅ A tela não conclui nada sobre a legibilidade e não bloqueia envio nem salvamento |
| III. Backend Java | ✅ Não alterado |
| IV. Segurança | ✅ A paciente é identificada pela sessão (token); o app não envia identificador nem chave do simulador; 401/403/404 seguem o tratamento existente; nada é registrado em log |
| V. Regras/IA | ✅ O app só exibe a situação e a orientação calculadas pela API; a orientação aparece como texto puro |
| VI. Retentativas e idempotência | ✅ A sessão é aberta por uma rota idempotente e pedida uma vez por visita. A consulta periódica é só leitura, sequencial, e para fora de vista; não é repetição de operação que falhou (research D3) |
| VII. Verificação honesta | ✅ Testes MockWebServer da #47; o percurso no emulador é registrado em texto em `docs/evidencias/`; o que não for verificado fica dito |
| VIII. Código documentado | ✅ KDoc em português; mudanças só em `mobile/app/` e na documentação |

Sem violações; a tabela de complexidade não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/023-app-android-condicoes-captura/
├── spec.md · plan.md · research.md · data-model.md · quickstart.md · tasks.md
├── contracts/api-consumida.md
└── checklists/requirements.md
```

### Source Code (mudanças em `mobile/app/`)

```text
src/main/java/br/com/regulacheck/app/
├── api/Modelos.kt                    # + SessaoCaptura, Telemetria, SensorTelemetria (contrato da spec 016)
├── api/RegulaCheckApi.kt             # + abrirSessaoCaptura, consultarTelemetria
├── ui/Textos.kt                      # + textos de E2.7 Origem e E4.1 (criados/ajustados registrados em textos.md)
├── ui/captura/ApresentacaoCaptura.kt # novo: funções puras (situação, recuperada, idade, valor)
├── ui/captura/CondicoesCapturaViewModel.kt # novo: sessão + laço de consulta sequencial
├── ui/captura/CondicoesCapturaTela.kt      # novo: E4.1
└── ui/documentos/DocumentosTela.kt   # folha com passo Origem; E4.1 como passo do anexar; seletor por tipos

src/test/java/br/com/regulacheck/app/
├── api/TelemetriaApiTest.kt          # novo: rotas, situações, erros
├── ui/ApresentacaoCapturaTest.kt     # novo: estados do desenho e recuperação
└── ui/CondicoesCapturaViewModelTest.kt # novo: laço sem sobreposição, parada, indisponível, 409/403/404
```

Documentação: `mobile/design/textos.md` (textos criados/ajustados), `mobile/README.md` (implementado × só desenho × não verificado; o que cada sensor mede), `docs/evidencias/2026-11-18_rc22-condicoes-captura-app.md` e `docs/ambiente.md`, se algo mudar.

**Structure Decision**: o app existente em `mobile/app/` ganha o pacote `ui/captura`. A E4.1 é um passo do fluxo "anexar" dentro da tela de documentos (research D1), e não uma entrada nova da pilha de navegação.

## Complexity Tracking

Não se aplica.
