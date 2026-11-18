# Quickstart: App Android — condições da captura (RC-22, parte do app)

Ambiente e comandos do app: [mobile/README.md](../../mobile/README.md) e [docs/ambiente.md](../../docs/ambiente.md). Simulador: [simulador-iot/README.md](../../simulador-iot/README.md).

## Testes automáticos

A partir de `mobile/app/`, com o JDK 21 do Android Studio na sessão:

```bash
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew assembleDebug testDebugUnitTest
```

## Percurso no emulador

1. O usuário inicia o backend com Oracle. Depois, instale o APK (`mobile/app/build/outputs/apk/debug/regulacheck-app-debug.apk`) e entre com `paciente.a@exemplo.invalid`.
2. Abra um pedido em rascunho → "Anexar documento" → um tipo → "Fotografar". A E4.1 abre e pede a sessão de captura. O id da sessão é o `id` da resposta de `POST …/sessoes-captura`, que pode ser consultado pela API ou pelo banco.
3. O usuário roda, a partir da raiz do repositório, uma sequência por vez:
   `java simulador-iot/SimuladorSensores.java --sessao <id> --sequencia normal|fora-da-faixa|recuperacao`
4. Confira os estados normal, luminosidade e distância fora da faixa e recuperada. Depois de mais de 60 s sem envio, confira "desatualizada". Com `svc wifi disable` e `svc data disable`, confira "indisponível".
5. Siga para "Escolher a foto" com os sensores dentro e fora da faixa e anexe `/sdcard/Download/identificacao-sintetica.png`.
6. Confira no logcat do processo do app que não aparecem token, senha, chave do simulador nem corpo de documento.
