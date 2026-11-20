# Quickstart: verificar a RC-20

Ambiente e variáveis: `docs/ambiente.md`. Nesta máquina, toda execução Java usa `JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt"`; o Gradle usa `JAVA_HOME` do JDK 21 do Android Studio só na sessão.

## Automático

```bash
cd backend && ./mvnw clean verify          # falha conhecida só em SimuladorSensoresProcessoTest (ambiente)
cd mobile/app && ./gradlew assembleDebug testDebugUnitTest
```

Testes da feature: `IndicadorServiceTest`, `IndicadorEndpointTest` (backend); `IndicadoresApiTest`, `ServicoSessaoTest`, `FluxoViewModelTest` (app).

## Manual (backend real com Oracle, iniciado pelo responsável a partir desta branch)

1. Token de administrador: `POST /auth/login` com `admin@exemplo.invalid` (senha de demonstração em `database/scripts/README.md`).
2. `GET /admin/indicadores` com `Authorization: Bearer <token>` → 200; anotar os números.
3. Mesmo `GET` com token de `paciente.a@exemplo.invalid` → 403 `ACESSO_NEGADO`; sem token → 401.
4. Emulador `Medium_Phone`, APK `mobile/app/build/outputs/apk/debug/regulacheck-app-debug.apk`: entrar como administrador → E4.2 com os mesmos números do passo 2; "Sair" → login.
5. Entrar como paciente → "Meus pedidos"; não há caminho para E4.2.

Registrar só em texto em `docs/evidencias/2026-11-18_rc20-indicadores-administrador.md` (sem capturas).
