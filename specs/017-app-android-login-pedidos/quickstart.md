# Quickstart — RC-18

Pré-requisitos (verificados em 16/11/2026): Android SDK 34 em `%LOCALAPPDATA%\Android\Sdk`, emulador `Medium_Phone`, JDK 21 do Android Studio.

```bash
cd mobile/app
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # só nesta sessão
./gradlew assembleDebug testDebugUnitTest
adb install -r build/outputs/apk/debug/app-debug.apk
```

PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest`.

- API no emulador: `http://10.0.2.2:8080` (padrão). Aparelho físico: `./gradlew assembleDebug -PapiBaseUrl=http://<IP-do-computador>:8080`.
- Backend + Oracle + carga sintética: `docs/ambiente.md`. Conta fictícia: `database/scripts/README.md`.
- Roteiro manual com backend real: `docs/evidencias/rc18-roteiro-manual.md`.
