# Implementation Plan: App Android — envio de documentos e resultado da análise (RC-19)

**Branch**: `022-app-android-documentos-resultado` | **Date**: 17/11/2026 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/022-app-android-documentos-resultado/spec.md` · Backlog RC-19 (#42), Épico 6 (#40) · Base RC-18 ([plan 017](../017-app-android-login-pedidos/plan.md))

## Summary

Estender o app de `mobile/app/` (RC-18) com cinco telas — E2.1 Novo pedido, E2.2 Seus documentos (com a folha E2.7 de tipo e a faixa E2.3 de recusa), E3.1 Ler documentos (com o diálogo E3.8), E3.3 Resultado da verificação e E3.4 O que falta resolver — consumindo `GET /protocolos`, `POST /solicitacoes`, `GET`/`POST …/documentos` (multipart com `X-Chave-Envio`), `POST …/extracoes`, `POST`/`GET …/analises` e `POST …/resumo`. Reaproveita `ApiClient`, `ErroApi`, sessão, navegação e componentes da RC-18; acrescenta envio multipart em streaming, o selo SIMULADO e funções puras (seleção da análise vigente, decisão após tempo esgotado, rótulos) cobertas por testes. Decisões em [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 17), Gradle 8.7 com JDK 21 (jbr do Android Studio) — sem mudança

**Primary Dependencies**: as da RC-18 ([research 017 D1](../017-app-android-login-pedidos/research.md)); **nenhuma dependência nova** (seletor do sistema por `activity-compose`, folha e diálogo do Material 3 já presentes, multipart do OkHttp)

**Storage**: nenhum novo; envio em curso só em memória (ViewModel)

**Testing**: JUnit 4 + MockWebServer + coroutines-test (`testDebugUnitTest`); verificação visual por screenshot no emulador; percurso manual com backend real

**Target Platform**: Android minSdk 26, compile/targetSdk 34; emulador `Medium_Phone`; API `http://10.0.2.2:8080`

**Project Type**: mobile-app (cliente da API)

**Performance Goals**: percurso completo em até 5 min (SC-001); tempos da #40 (leitura 70 s em ler/analisar)

**Constraints**: sem retentativa automática; sem log de token/senha/cabeçalho/corpo/documento; texto da API como texto puro; sem alterar o backend; sem reenvio/substituição (RC-16)

**Scale/Scope**: 5 telas novas, 9 rotas, 1 folha e 1 diálogo; conta fictícia da carga sintética

## Constitution Check

| Princípio | Situação |
|---|---|
| I. Escopo acadêmico | ✅ Rodapé acadêmico em toda tela nova; SIMULADO em leitura, resultado, resumo e pendência; aviso fictício do serviço; só amostras de `samples/` |
| II. Limites clínicos | ✅ Sem diagnóstico, urgência, vaga, prazo ou aprovação; nenhuma edição do documento original; orientação condicional da unidade emissora vem da API (RC-15) |
| III. Backend Java | ✅ Não alterado |
| IV. Segurança | ✅ Paciente pela sessão; nada de identificador no corpo; 401/403/404 como na RC-18; arquivo lido pelo seletor do sistema, sem permissão de armazenamento; sem log sensível |
| V. Regras/IA | ✅ O app só exibe o resultado determinístico; resumo simulado identificado; falha do resumo não esconde pendências; texto de documento é dado |
| VI. Retentativas | ✅ Nenhuma automática; `X-Chave-Envio` torna a repetição do envio idempotente; tempo esgotado na análise consulta o histórico antes de nova tentativa; criação de rascunho sem deduplicação no backend é avisada (research D7) |
| VII. Verificação honesta | ✅ Testes MockWebServer exigidos pela #42; percurso real registrado em `docs/evidencias/`; o que não for verificado fica dito |
| VIII. Código documentado | ✅ KDoc em português; mudanças restritas a `mobile/app/` e documentação |

Sem violações; a tabela de complexidade não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/022-app-android-documentos-resultado/
├── spec.md · plan.md · research.md · data-model.md · quickstart.md · tasks.md
├── contracts/api-consumida.md
└── checklists/requirements.md
```

### Source Code (mudanças em `mobile/app/`)

```text
src/main/java/br/com/regulacheck/app/
├── api/
│   ├── ApiClient.kt          # + corpo multipart, cabeçalhos extras, código de status no sucesso
│   ├── ErroApi.kt            # + ArquivoInacessivel; Indisponivel com código
│   ├── Envio.kt              # novo: FonteArquivo (contrato do arquivo escolhido) e corpo em streaming
│   ├── Modelos.kt            # + Protocolo, Documento, Extracao, HistoricoAnalises, AnaliseGravada…
│   └── RegulaCheckApi.kt     # + rotas da RC-19
├── ui/
│   ├── Textos.kt · RotulosApi.kt (novo: valores da API → rótulos)
│   ├── navegacao/            # + telas novas e Navegador.substituir
│   ├── componentes/          # + SeloSimulado, cabeçalho com "Pedido N", Faixa, Opção
│   ├── novopedido/           # E2.1
│   ├── documentos/           # E2.2 + E2.7 + E2.3 (FonteArquivoUri para o content://)
│   ├── leitura/              # E3.1 + E3.8 + decisão após tempo esgotado
│   ├── resultado/            # E3.3 + seleção da análise vigente
│   └── pendencia/            # E3.4
└── (pedido/, pedidos/ — ações passam a navegar)
src/test/java/br/com/regulacheck/app/
├── api/EnvioDocumentosTest.kt, LeituraAnaliseTest.kt   # rotas 5–11 com MockWebServer
└── ui/ ApresentacaoResultadoTest, DecisaoAnaliseTest, RotulosApiTest, DocumentosViewModelTest
```

**Structure Decision**: mesmo projeto de um módulo da RC-18; uma pasta por tela em `ui/`, com ViewModel e Composable juntos, como `ui/pedido/`. Regras de apresentação ficam em funções puras testáveis fora do Composable.

## Phase 0 / Phase 1

- Pesquisa e decisões: [research.md](research.md)
- Modelo de dados e estados das telas: [data-model.md](data-model.md)
- Contrato consumido (rotas, respostas e tratamento): [contracts/api-consumida.md](contracts/api-consumida.md)
- Como executar e verificar: [quickstart.md](quickstart.md)

**Re-check pós-design**: sem mudanças; nenhuma rota nova, nenhuma dependência nova, nenhum segredo no app.
