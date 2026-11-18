# Implementation Plan: App Android — base, login e meus pedidos (RC-18)

**Branch**: `017-app-android-login-pedidos` | **Date**: 17/11/2026 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-app-android-login-pedidos/spec.md` · Backlog RC-18 (#41), Épico 6 (#40)

## Summary

Criar o app Android nativo em `mobile/app/` (Kotlin + Compose Material 3) com a base usada por todas as telas — cliente OkHttp com erros tipados, sessão cifrada pelo Android Keystore, navegação própria — e as telas E1.1 Login, E1.2 Meus pedidos e E1.3 Pedido (detalhe e ação principal), consumindo só `POST /auth/login`, `POST /auth/logout`, `GET /solicitacoes` e `GET /solicitacoes/{id}`. Visual traduzido de `mobile/design/` para um tema Material 3. Decisões em [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 17), Gradle 8.7 executado com JDK 21 (jbr do Android Studio)

**Primary Dependencies**: AGP 8.5.2; Compose BOM 2024.09.03 (Material 3); activity-compose 1.9.2; lifecycle-viewmodel-compose/runtime-compose 2.8.6; core-ktx 1.13.1; OkHttp 4.12.0; kotlinx-serialization-json 1.7.3. Sem Retrofit, Hilt, Room, Coil ou security-crypto (#40)

**Storage**: `SharedPreferences` privado com a sessão cifrada (AES-256-GCM, chave no Android Keystore)

**Testing**: JUnit 4.13.2 + MockWebServer 4.12.0 + kotlinx-coroutines-test 1.8.1 (`testDebugUnitTest`); verificação visual por screenshot no emulador

**Target Platform**: Android, minSdk 26, compile/targetSdk 34; emulador `Medium_Phone`

**Project Type**: mobile-app (cliente da API do backend)

**Performance Goals**: tempos fixos da #40 — conexão 10 s, leitura 30 s (70 s reservado a ler/analisar, RC-19)

**Constraints**: sem retentativa automática; nenhum log de token/senha/cabeçalho/corpo; HTTP em claro só no debug

**Scale/Scope**: 3 telas, 4 rotas, conta fictícia da carga sintética

## Constitution Check

| Princípio | Situação |
|---|---|
| I. Escopo acadêmico | ✅ Aviso de rodapé em toda tela e aviso de entrada no login; só dados da carga sintética |
| II. Limites clínicos | ✅ Textos de `textos.md`; nenhum diagnóstico, urgência ou aprovação; o app só consulta |
| III. Backend Java | ✅ Não alterado. O app é Kotlin, decisão do grupo (#40, `mobile/README.md`); a constituição deixa o framework mobile ao grupo |
| IV. Segurança | ✅ Paciente identificada pelo token; token cifrado; sem segredo além dele; sem log sensível; texto da API como texto |
| V. Regras/IA | N/A nesta história |
| VI. Persistência/retentativas | ✅ Sem retentativa automática; o app não cria nada |
| VII. Verificação honesta | ✅ Testes reais com MockWebServer; percurso com backend real registrado por quem executar; bloqueios reportados |
| VIII. Código documentado | ✅ KDoc em português; nomes de domínio em português sem acento ("em Java" vale para o backend) |

Sem violações; a tabela de complexidade não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/017-app-android-login-pedidos/
├── spec.md · plan.md · research.md · data-model.md · quickstart.md · tasks.md
├── contracts/api-consumida.md
└── checklists/requirements.md
```

### Source Code (repository root)

```text
mobile/app/                         # projeto Gradle de um módulo (raiz = app)
├── settings.gradle.kts · build.gradle.kts · gradle.properties
├── gradle/libs.versions.toml · gradle/wrapper/ · gradlew · gradlew.bat
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── res/ (font/, values/, xml/)
    │   └── java/br/com/regulacheck/app/
    │       ├── RegulaCheckApp.kt · MainActivity.kt
    │       ├── api/        ApiClient, ErroApi, Resultado, Modelos, RegulaCheckApi
    │       ├── sessao/     SessaoLocal, CofreSessao(+Keystore), GerenciadorSessao, ServicoSessao
    │       └── ui/
    │           ├── tema/        Cores, Tipografia, TemaRegulaCheck, Icones
    │           ├── navegacao/   Tela, EstadoTela, Navegador
    │           ├── componentes/ avisos, cabeçalhos, botões, campos, estado, retorno, esqueleto
    │           ├── EstadoPedidoUi.kt · Formatos.kt
    │           ├── login/ · pedidos/ · pedido/
    ├── debug/res/xml/network_security_config.xml
    └── test/java/br/com/regulacheck/app/   ApiClientTest, ServicoSessaoTest, EstadoPedidoUiTest
```

**Structure Decision**: um único projeto Gradle em `mobile/app/` (sem `app/app/`), como pede a #41. Camadas: `api` (rede e contrato), `sessao` (estado da conta), `ui` (telas + ViewModels). A sessão e o cliente ficam num contêiner simples em `RegulaCheckApp`, sem biblioteca de injeção.

## Phase 0 / Phase 1

- Pesquisa e decisões: [research.md](research.md)
- Modelo de dados do app: [data-model.md](data-model.md)
- Contrato consumido (rotas e tratamento): [contracts/api-consumida.md](contracts/api-consumida.md)
- Como executar e verificar: [quickstart.md](quickstart.md)

**Re-check pós-design**: sem mudanças; nenhuma rota nova, nenhum segredo no app.
