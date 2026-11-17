# RC-18 — Roteiro manual com backend real (para executar e preencher)

Critério da issue #41: "entrar com sucesso e com senha errada; backend parado; abrir pedido; sessão expirada", no emulador, com o backend e o Oracle reais. Preencha a coluna "Resultado" com o que **viu** (✅/❌ + observação) e anexe capturas como `AAAA-MM-DD_rc18-manual-<n>.png`.

## Preparação

1. Exporte `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (`.env` do grupo) e suba o backend a partir de `backend/` (`docs/ambiente.md`). Nesta máquina, use também `-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt` no `java -jar` (ver "Problemas comuns" em `docs/ambiente.md`).
2. Confirme a carga sintética (`database/scripts/02_carga_sintetica.sql`) e a senha da conta fictícia em `database/scripts/README.md`.
3. Gere e instale o app (`mobile/README.md`): `./gradlew assembleDebug` e `adb install -r build/outputs/apk/debug/regulacheck-app-debug.apk`.
4. Para ver o log do app: `adb logcat --pid=$(adb shell pidof br.com.regulacheck.app)`.

## Passos

| # | Ação | Esperado | Resultado |
|---|---|---|---|
| 1 | Abrir o app | Login com aviso acadêmico e rodapé | |
| 2 | Entrar com `paciente.a@exemplo.invalid` e senha **errada** | "E-mail ou senha incorretos."; e-mail mantido; senha limpa | |
| 3 | Entrar com a senha correta | Botão "Entrando…" bloqueado; depois "Meus pedidos" com "Olá, Paciente" | |
| 4 | Conferir a lista | Os mesmos pedidos de `GET /solicitacoes` da paciente A, com serviço, estado (ícone + texto) e data | |
| 5 | Abrir um pedido | Dados iguais a `GET /solicitacoes/{id}` e ação do estado (`data-model.md`) | |
| 6 | Tocar na ação principal (exceto "Atualizar") | Aviso "Esta etapa ainda não está disponível nesta versão do app." | |
| 7 | Voltar, parar o backend e tocar num pedido | "Sem conexão com a internet." com "Tentar de novo"; app continua aberto | |
| 8 | Subir o backend e "Tentar de novo" | Pedido carregado | |
| 9 | Sessão expirada: subir o backend com `SESSION_TTL_MINUTES=1`, entrar, esperar 2 min, voltar à lista | Login com "Sua sessão terminou. Entre novamente." | |
| 10 | Entrar com `admin@exemplo.invalid` (carga sintética) | "Esta conta não é de paciente. Entre com uma conta de paciente."; nada guardado | |
| 11 | Sair | Login sem aviso; reabrir o app não entra sozinho | |
| 12 | Logcat do app no percurso todo | Nenhum token, senha, e-mail, `Authorization` ou corpo JSON | |

Executado por: ________ · Data: ________ · Commit/APK: ________
