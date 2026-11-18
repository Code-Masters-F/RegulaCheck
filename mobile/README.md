# Mobile

**Android nativo com Kotlin.** O projeto está em [`app/`](app/) desde a RC-18 (spec [`specs/017-app-android-login-pedidos/`](../specs/017-app-android-login-pedidos/), issue #41); a RC-19 acrescentou envio de documentos, leitura, análise e resultado (spec [`specs/022-app-android-documentos-resultado/`](../specs/022-app-android-documentos-resultado/), issue #42). O app consome somente a API do backend ([contratos](../docs/api/contratos-dados.md), [erros](../docs/api/erros.md), [estados](../docs/api/estados-e-revisoes.md), [rotas](../CONTRATO_API.md)). Nenhuma credencial de banco, de IA ou do simulador no app.

## O que está implementado, verificado e só desenhado (17/11/2026)

| Parte | Situação |
|---|---|
| Base: Gradle 8.7 (Wrapper), AGP 8.5.2, Kotlin 2.0.21, Compose Material 3, minSdk 26, target/compileSdk 34 | Implementado; build verde com JDK 21 |
| Rede: OkHttp 4.12 + kotlinx-serialization, `ErroApi` tipado, tempos 10/30 s (70 s em ler e analisar), sem retentativa automática, sem log; envio multipart em streaming com `X-Chave-Envio` | Implementado; 40 testes de API com MockWebServer |
| Sessão: token cifrado (AES-256-GCM, chave no Android Keystore), 401 apaga a sessão e volta ao login, 403 mantém a sessão, sair apaga mesmo com falha | Implementado; testado (JVM) e conferido no emulador |
| E1.1 Login, E1.2 Meus pedidos, E1.3 Pedido (dados e ação principal por estado; desde a RC-19 as ações abrem as telas abaixo) | Implementado; conferido no emulador e com backend real ([RC-18](../docs/evidencias/2026-11-17_rc18-app-android.md), [roteiro](../docs/evidencias/2026-11-17_rc18-roteiro-manual.md)) |
| RC-19: E2.1 Novo pedido, E2.2 Seus documentos (com a folha E2.7 de tipo e as recusas E2.3), E3.1 Ler documentos, E3.8 Verificar critérios, E3.3 Resultado da verificação, E3.4 O que falta resolver | Implementado; 82 testes verdes; percurso com backend real e Oracle verificado em 17/11/2026 (≈ 4 min, 3 pedidos iguais a `GET …/analises`, arquivo inválido, rede cortada no envio e na análise, toques repetidos, logcat) — [evidências](../docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md) |
| Resumo simulado (RC-14) com SIMULADO, "Tentar de novo" e resumo padrão | Implementado; resumo exibido com o backend real; "Tentar de novo", `PADRAO` e 409 só nos testes (as análises reais já vinham com resumo) |
| `rotuloResultado` e `orientacao` da RC-15 ("Quem pode resolver") | Implementado; verificado com o backend real atualizado (segunda execução, 17/11/2026). Sem esses campos (backend antigo), o app usa o rótulo local e omite a orientação |
| 409 `ANALISE_EM_ANDAMENTO`, 503 da leitura/análise, E2.1 erro e "sem serviços" | Implementado; **só nos testes** (não reproduzidos com o backend real) |
| Avaliação com ≥ 3 pessoas (SC-005 da spec 019) | **Não realizada** |
| Conferência da leitura (E3.2), substituir/remover/excluir (E2.4–E2.6), "Abrir documento", "Fotografar" e condições da captura (E2.7 Origem, E4.1), correção e comparação (E3.5, RC-16), conclusão e comprovante (E3.6/E3.7, RC-17), administrador (E4.2) | **Só desenho** (`design/RegulaCheck.pen`); o app não mostra botões para elas e explica o limite em texto quando preciso |
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

`app/src/main/java/br/com/regulacheck/app/`: `api/` (cliente, modelos, erros), `sessao/` (Keystore e estado da sessão), `ui/` (tema, componentes, navegação por `sealed interface Tela` e `EstadoTela<T>`, telas `login/`, `pedidos/`, `pedido/` e, na RC-19, `novopedido/`, `documentos/`, `leitura/`, `resultado/`, `pendencia/`; rótulos dos valores da API em `ui/RotulosApi.kt`). Textos em `ui/Textos.kt`, copiados de [`design/textos.md`](design/textos.md). Fontes Bricolage Grotesque e Nunito Sans (licença OFL em `app/licencas/`); ícones Lucide (ISC) recriados em `ui/tema/Icones.kt`. Decisões: [research da RC-18](../specs/017-app-android-login-pedidos/research.md) e [da RC-19](../specs/022-app-android-documentos-resultado/research.md).

## Regras do app (RC-04 a RC-06, valem para as próximas histórias)

- **Aviso acadêmico (decisão de 17/11/2026):** toda tela usa `EstruturaTela`, cujo rodapé "Protótipo acadêmico · dados fictícios" (com ícone de informação) abre o aviso completo num balão ao toque ou toque longo. Nenhuma tela repete a caixa grande; o login mostra só o aviso curto `AvisoProposta`. Telas novas não podem tirar o rodapé.
- **Login:** botão bloqueado durante o envio; 401 mantém o e-mail e limpa a senha; 503/rede mostram "serviço indisponível"/"sem conexão" com "Tentar de novo"; conta que não é de paciente é recusada e a sessão dela é encerrada.
- **Respostas:** 401 apaga o token e volta ao login com o aviso de E1.4; 403 mostra "Você não tem permissão para ver esta tela." sem sair; 404 de pedido mostra "Pedido não encontrado." com volta à lista (mesma resposta para pedido de outra paciente).
- **Sem retentativa automática** e cada chamada em conexão nova (uma conexão ociosa fechada pelo servidor não pode virar "sem conexão").
- **Segredos:** só o token de sessão, cifrado; nunca senha, `IOT_API_KEY` ou chave de IA. Nada de token, senha, cabeçalho ou corpo em log.
- **Texto da API como texto:** nunca WebView nem HTML; leitura, resultado, resumo e pendência têm o selo SIMULADO.
- **Arquivos (RC-19):** seletor do sistema (PDF, JPEG, PNG), sem permissão de armazenamento; 3 arquivos, 5 MB, PDF até 5 páginas; o arquivo só entra na lista depois de 201/200; `X-Chave-Envio` nasce na escolha e se repete nas novas tentativas; em 400/413/415/422, mostrar a mensagem da API e pedir para escolher o arquivo de novo.
- **Ler e analisar (RC-19):** botões bloqueados durante a operação; em tempo esgotado ou queda na análise, consultar `GET …/analises` antes de oferecer nova tentativa; 409 `ANALISE_EM_ANDAMENTO` mostra a espera de `Retry-After`; o resumo é pedido uma vez depois da análise e só a paciente repete.
- **Resultado (RC-15 na RC-19):** só a análise com `vigenteId` e `vigente`; outra só como "Resultado anterior"; nenhum botão de ação indisponível.

## Design das telas (pen.dev)

O desenho é a referência visual: [`design/RegulaCheck.pen`](../design/RegulaCheck.pen) (só pelo pen.dev), com o índice [`design/INDEX.md`](design/INDEX.md), [`design/design-system.md`](design/design-system.md), [`design/textos.md`](design/textos.md), [`design/dados-ficticios.md`](design/dados-ficticios.md), [`design/mapa-de-percursos.md`](design/mapa-de-percursos.md) e [`design/acessibilidade.md`](design/acessibilidade.md). Regras de conteúdo: tudo acadêmico e sintético; SIMULADO em leitura, resumo e comprovante; nenhum texto afirma diagnóstico, urgência, autorização, vaga ou aprovação do SUS; estado nunca só por cor.
