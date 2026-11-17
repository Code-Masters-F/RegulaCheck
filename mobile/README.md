# Mobile

**Android nativo com Kotlin.** O projeto está em [`app/`](app/) desde a RC-18 (spec [`specs/017-app-android-login-pedidos/`](../specs/017-app-android-login-pedidos/), issue #41). O app consome somente a API do backend ([contratos](../docs/api/contratos-dados.md), [erros](../docs/api/erros.md), [estados](../docs/api/estados-e-revisoes.md), [rotas](../CONTRATO_API.md)). Nenhuma credencial de banco, de IA ou do simulador no app.

## O que está implementado, verificado e só desenhado (17/11/2026)

| Parte | Situação |
|---|---|
| Base: Gradle 8.7 (Wrapper), AGP 8.5.2, Kotlin 2.0.21, Compose Material 3, minSdk 26, target/compileSdk 34 | Implementado; build verde com JDK 21 |
| Rede: OkHttp 4.12 + kotlinx-serialization, `ErroApi` tipado, tempos 10/30 s (70 s reservado à RC-19), sem retentativa automática, sem log | Implementado; 20 testes com MockWebServer |
| Sessão: token cifrado (AES-256-GCM, chave no Android Keystore), 401 apaga a sessão e volta ao login, 403 mantém a sessão, sair apaga mesmo com falha | Implementado; testado (JVM) e conferido no emulador |
| E1.1 Login, E1.2 Meus pedidos, E1.3 Pedido (dados e ação principal por estado) | Implementado; conferido no emulador ([evidências](../docs/evidencias/2026-11-17_rc18-app-android.md)) |
| Percurso com backend real e Oracle | **Não verificado** (sem credenciais na máquina de desenvolvimento); roteiro em [docs/evidencias/2026-11-17_rc18-roteiro-manual.md](../docs/evidencias/2026-11-17_rc18-roteiro-manual.md) |
| Ações que dependem da RC-19 ("Novo pedido", "Anexar/Ler documentos", "Ver pendências", "Ver o que conferir", "Ver resultado", "Tentar de novo" da análise) | Botão presente; avisa "Esta etapa ainda não está disponível nesta versão do app." Nada é simulado |
| Demais telas (E2–E4: envio, leitura, conferência, resultado, comprovante, administrador) | **Só desenho** (`design/RegulaCheck.pen`) |
| Fonte ampliada, TalkBack, modo escuro, tablet | Fora do épico (#40); não verificados |

## Como executar

Pré-requisitos: Android SDK 34 e emulador (Android Studio), JDK 21. O JDK 25 não serve ao Android Gradle Plugin: use o do Android Studio só na sessão.

```bash
cd mobile/app
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug testDebugUnitTest
adb install -r build/outputs/apk/debug/regulacheck-app-debug.apk
```

PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest`. Se o Gradle falhar com "Unable to establish loopback connection", veja "Problemas comuns" em [docs/ambiente.md](../docs/ambiente.md).

## Endereço da API

| Onde o app roda | Como gerar |
|---|---|
| Emulador Android | padrão: `http://10.0.2.2:8080` |
| Aparelho físico via USB | `adb reverse tcp:8080 tcp:8080` e `./gradlew assembleDebug -PapiBaseUrl=http://localhost:8080` |
| Aparelho físico na mesma rede | `./gradlew assembleDebug -PapiBaseUrl=http://<IP-do-computador>:8080` e inclua o IP, só na sua cópia, em `app/src/debug/res/xml/network_security_config.xml` |

`localhost` **não** funciona no emulador. HTTP em claro só na variante **debug** (`src/debug`), restrito a `10.0.2.2`, `localhost` e `127.0.0.1`; a variante de entrega não permite texto claro (a demonstração remota usa `https://`). Detalhes e firewall: [docs/ambiente.md](../docs/ambiente.md#acesso-do-app-à-api).

## Estrutura

`app/src/main/java/br/com/regulacheck/app/`: `api/` (cliente, modelos, erros), `sessao/` (Keystore e estado da sessão), `ui/` (tema, componentes, navegação por `sealed interface Tela` e `EstadoTela<T>`, telas `login/`, `pedidos/`, `pedido/`). Textos em `ui/Textos.kt`, copiados de [`design/textos.md`](design/textos.md). Fontes Bricolage Grotesque e Nunito Sans (licença OFL em `app/licencas/`); ícones Lucide (ISC) recriados em `ui/tema/Icones.kt`. Decisões: [research.md](../specs/017-app-android-login-pedidos/research.md).

## Regras do app (RC-04 a RC-06, valem para as próximas histórias)

- **Login:** botão bloqueado durante o envio; 401 mantém o e-mail e limpa a senha; 503/rede mostram "serviço indisponível"/"sem conexão" com "Tentar de novo"; conta que não é de paciente é recusada e a sessão dela é encerrada.
- **Respostas:** 401 apaga o token e volta ao login com o aviso de E1.4; 403 mostra "Você não tem permissão para ver esta tela." sem sair; 404 de pedido mostra "Pedido não encontrado." com volta à lista (mesma resposta para pedido de outra paciente).
- **Sem retentativa automática** e cada chamada em conexão nova (uma conexão ociosa fechada pelo servidor não pode virar "sem conexão").
- **Segredos:** só o token de sessão, cifrado; nunca senha, `IOT_API_KEY` ou chave de IA. Nada de token, senha, cabeçalho ou corpo em log.
- **Texto da API como texto:** nunca WebView nem HTML; o resumo simulado (RC-19) deve ser identificado como SIMULADO.
- **Arquivos (RC-19):** 3 arquivos, 5 MB, PDF até 5 páginas, JPEG/PNG; em 400/413/415, mostrar a mensagem e pedir para escolher o arquivo de novo.

## Design das telas (pen.dev)

O desenho é a referência visual: [`design/RegulaCheck.pen`](../design/RegulaCheck.pen) (só pelo pen.dev), com o índice [`design/INDEX.md`](design/INDEX.md), [`design/design-system.md`](design/design-system.md), [`design/textos.md`](design/textos.md), [`design/dados-ficticios.md`](design/dados-ficticios.md), [`design/mapa-de-percursos.md`](design/mapa-de-percursos.md) e [`design/acessibilidade.md`](design/acessibilidade.md). Regras de conteúdo: tudo acadêmico e sintético; SIMULADO em leitura, resumo e comprovante; nenhum texto afirma diagnóstico, urgência, autorização, vaga ou aprovação do SUS; estado nunca só por cor.
