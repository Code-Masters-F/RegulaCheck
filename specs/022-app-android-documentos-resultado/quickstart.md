# Quickstart — RC-19

Pré-requisitos e ambiente: [quickstart da RC-18](../017-app-android-login-pedidos/quickstart.md) e `docs/ambiente.md` (JDK 21 do Android Studio para o Gradle; `JAVA_TOOL_OPTIONS` desta máquina; emulador `Medium_Phone`).

## Build e testes

A partir de `mobile/app/` (PowerShell, só na sessão):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt"
.\gradlew.bat assembleDebug testDebugUnitTest
adb install -r build\outputs\apk\debug\regulacheck-app-debug.apk
```

## Percurso manual (backend real com Oracle e `DOCUMENT_STORAGE_PATH` configurado)

1. Copiar as amostras para o emulador: `adb push samples/encaminhamento-sintetico.pdf /sdcard/Download/` e `adb push samples/identificacao-sintetica.png /sdcard/Download/`.
2. Entrar com `paciente.a@exemplo.invalid` (senha em `database/scripts/README.md`) → "Novo pedido" → "Criar rascunho".
3. "Anexar documento" → Encaminhamento → escolher o PDF; repetir com Identificação → PNG.
4. "Ler documentos" → "Verificar critérios" → confirmar → resultado; abrir uma pendência.
5. Conferir com `GET /solicitacoes/{id}/analises` (mesma conta) que as pendências coincidem.
6. Falhas: arquivo inválido (ex.: `.txt` renomeado), rede cortada com `adb shell svc wifi disable` e `adb shell svc data disable` durante o envio e a análise (depois `enable`), toques repetidos.
7. `adb logcat --pid=$(adb shell pidof br.com.regulacheck.app)` sem token, senha nem corpo.

Registrar em `docs/evidencias/AAAA-MM-DD_<assunto>.md`.
