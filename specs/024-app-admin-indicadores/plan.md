# Implementation Plan: Administrador — protocolo e indicadores (RC-20)

**Branch**: `024-app-admin-indicadores` | **Date**: 18/11/2026 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/024-app-admin-indicadores/spec.md` · Backlog RC-20 (#43), Épico 6 (#40) · Base do app: [plan 017](../017-app-android-login-pedidos/plan.md) e [plan 022](../022-app-android-documentos-resultado/plan.md)

## Summary

Criar no backend a rota `GET /admin/indicadores` (módulo `indicador`, Controller → Service → Repository JDBC), publicada por `RotasPrivadas` só para `ADMINISTRADOR`, que devolve apenas agregados: protocolos ativos com as regras, total de pedidos, análises vigentes por resultado geral, pedidos em `FALHA_ANALISE` e as 5 regras com mais pendências nas análises vigentes. No app, o login passa a aceitar a conta de administrador e a escolher a tela pelo perfil; a nova tela E4.2 consome a rota. Decisões em [research.md](research.md); contrato em [contracts/admin-indicadores.md](contracts/admin-indicadores.md).

## Technical Context

**Language/Version**: backend Java 21 (Maven Wrapper, Jetty 12 ee10, sem Spring/ORM); app Kotlin 2.0.21 com Gradle 8.7 no JDK 21 do Android Studio — sem mudança de versões

**Primary Dependencies**: as existentes; **nenhuma dependência nova** em nenhum dos dois projetos

**Storage**: Oracle por JDBC, só leitura (`RC_SOLICITACAO`, `RC_ANALISE`, `RC_PENDENCIA`, `RC_REGRA`, `RC_PROTOCOLO`); **nenhum script SQL novo** (sem coluna nem índice novo)

**Testing**: backend JUnit 5 com API real em Jetty e repositórios em memória (`ServidorDeTeste`, `CenarioAuth`); app JUnit 4 + MockWebServer + coroutines-test; verificação manual no emulador com o backend real

**Target Platform**: servidor local (porta 8080); Android minSdk 26, target 34, emulador `Medium_Phone`

**Project Type**: monólito web (API) + mobile-app (cliente da API)

**Performance Goals**: consulta em uma conexão, 3 consultas agregadas sem parâmetro do cliente; tela em até 3 s na rede local (SC-004)

**Constraints**: só agregados (FR-006); 403 antes de qualquer consulta (FR-001); sem retentativa automática no app; textos sem afirmar redução de filas; sem log de dados

**Scale/Scope**: 1 rota, 1 tela, 1 mudança no login; volume da carga sintética (dezenas de pedidos)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Situação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | OK: resposta marca `dadosSinteticos` e aviso; tela com rodapé acadêmico e nota de demonstração; nenhum texto de redução de filas |
| II. Limites clínicos | OK: só contagens e nomes de regras; nenhum conteúdo clínico, nenhuma decisão |
| III. Java em monólito modular | OK: pacote reservado `indicador` com controller/service/repository/dto, JDBC parametrizado, sem Spring/ORM |
| IV. Segurança e autorização | OK: `RotasPrivadas.registrar(..., Perfil.ADMINISTRADOR)`; paciente 403 antes do Service; sem dado identificável (teste automático) |
| V. Regras determinísticas, IA subordinada | N/A: não toca motor nem IA |
| VI. Persistência e revisões | OK: só leitura; usa `VIGENTE = 1` existente (invalidado por mudança de documento/conferência) |
| VII. Verificação honesta | OK: testes com valores esperados independentes; SQL contra Oracle só verificado na execução real (registrado) |
| VIII. Código documentado e mínimo | OK: JavaDoc/KDoc em português; reaproveita componentes; sem refatoração ampla |

Sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/024-app-admin-indicadores/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/admin-indicadores.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── indicador/controller/IndicadorController.java     # novo: GET /admin/indicadores
├── indicador/service/IndicadorService.java           # novo: monta os indicadores (zeros, top 5, protocolos)
├── indicador/service/Indicadores.java                # novo: resultado do caso de uso
├── indicador/repository/IndicadorRepository.java     # novo: contrato (contagens agregadas)
├── indicador/repository/ContagensIndicadores.java    # novo: contagens lidas do banco
├── indicador/repository/IndicadorRepositoryJdbc.java # novo: 3 consultas agregadas
├── indicador/dto/IndicadoresResponse.java            # novo: contrato público
├── config/Dependencias.java                          # + IndicadorRepository
├── config/Servidor.java                              # + IndicadorRepositoryJdbc
└── config/Composicao.java                            # + rota ADMINISTRADOR
backend/src/test/java/br/com/regulacheck/
├── indicador/IndicadorRepositoryMemoria.java         # novo
├── indicador/IndicadorServiceTest.java               # novo
├── indicador/IndicadorEndpointTest.java              # novo: 200/401/403/405/503 e privacidade
└── auth/CenarioAuth.java + 4 testes que montam Dependencias (novo parâmetro)

mobile/app/src/main/java/br/com/regulacheck/app/
├── api/Modelos.kt, api/RegulaCheckApi.kt             # + Indicadores e consultarIndicadores()
├── sessao/ServicoSessao.kt                           # aceita PACIENTE e ADMINISTRADOR
├── ui/navegacao/Navegacao.kt, AppRegulaCheck.kt      # Tela.Indicadores e tela por perfil
├── ui/indicadores/IndicadoresTela.kt                 # novo: E4.2 + ViewModel
├── ui/login/LoginTela.kt, ui/Textos.kt               # textos
mobile/app/src/test/java/br/com/regulacheck/app/
├── sessao/ServicoSessaoTest.kt, ui/FluxoViewModelTest.kt, api/IndicadoresApiTest.kt (novo)

docs/api/openapi.yaml, docs/api/autorizacao.md, mobile/README.md, mobile/design/textos.md,
docs/evidencias/2026-11-18_rc20-indicadores-administrador.md
```

**Structure Decision**: módulo `indicador` já reservado (`package-info.java`); o app segue a organização por tela das RC-18/RC-19.

## Complexity Tracking

Sem violações a justificar.
