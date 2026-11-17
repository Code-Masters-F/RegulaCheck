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
| 1 | Abrir o app | Login com aviso acadêmico e rodapé | ✅ Login com `Aviso/Proposta` e rodapé acadêmico (versão de 17/11) |
| 2 | Entrar com `paciente.a@exemplo.invalid` e senha **errada** | "E-mail ou senha incorretos."; e-mail mantido; senha limpa | ✅ 401 do backend real ([1](2026-11-17_rc18-manual-1-senha-errada.png)) |
| 3 | Entrar com a senha correta | Botão "Entrando…" bloqueado; depois "Meus pedidos" com "Olá, Paciente" | ✅ "Meus pedidos" com "Olá, Paciente" ([2](2026-11-17_rc18-manual-2-lista.png)); o estado "Entrando…" passou rápido demais para a captura |
| 4 | Conferir a lista | Os mesmos pedidos de `GET /solicitacoes` da paciente A, com serviço, estado (ícone + texto) e data | ✅ Iguais à API consultada por `curl` no mesmo momento: pedidos 4 (`RASCUNHO`, 1 documento → "Ler documentos"), 2 (`COM_PENDENCIAS`) e 1 (`SUBMETIDA_SIMULACAO`), atualizados em 12/11/2026 22:13 UTC (fuso do emulador) |
| 5 | Abrir um pedido | Dados iguais a `GET /solicitacoes/{id}` e ação do estado (`data-model.md`) | ✅ Pedido 2: 2 de 3 documentos, revisão 1, resultado "Com pendências", "Ver pendências", aviso fictício do backend. Pedido 1: revisão 2, "Sem pendências identificadas", sem ação (só consulta) |
| 6 | Tocar na ação principal (exceto "Atualizar") | Aviso "Esta etapa ainda não está disponível nesta versão do app." | ✅ ([3](2026-11-17_rc18-manual-3-pedido2-acao.png)) |
| 7 | Voltar, parar o backend e tocar num pedido | "Sem conexão com a internet." com "Tentar de novo"; app continua aberto | ✅ **Variação:** em vez de parar o backend do responsável (carregado com o `.env`), a rede do emulador foi cortada (`svc wifi/data disable`); o app recebe a mesma falha de conexão ([4](2026-11-17_rc18-manual-4-sem-conexao.png)). Backend parado de fato foi verificado antes, sem banco (seção 2 de [2026-11-17_rc18-app-android.md](2026-11-17_rc18-app-android.md)) |
| 8 | Subir o backend e "Tentar de novo" | Pedido carregado | ✅ Rede religada e "Tentar de novo": pedido 1 carregado ([5](2026-11-17_rc18-manual-5-pedido1-retentado.png)) |
| 9 | Sessão expirada: subir o backend com `SESSION_TTL_MINUTES=1`, entrar, esperar 2 min, voltar à lista | Login com "Sua sessão terminou. Entre novamente." | ⏳ **Não executado com o backend real** (exige reiniciar o backend com outra validade de sessão). Coberto por teste automatizado (`ServicoSessaoTest`) e no emulador com servidor de teste |
| 10 | Entrar com `admin@exemplo.invalid` (carga sintética) | "Esta conta não é de paciente. Entre com uma conta de paciente."; nada guardado | ✅ ([7](2026-11-17_rc18-manual-7-conta-admin.png)); após reabrir, o app não tem sessão |
| 11 | Sair | Login sem aviso; reabrir o app não entra sozinho | ✅ Reaberto (force-stop + start) cai no login vazio ([8](2026-11-17_rc18-manual-8-reaberto-sem-sessao.png)) |
| 12 | Logcat do app no percurso todo | Nenhum token, senha, e-mail, `Authorization` ou corpo JSON | ✅ 17.559 linhas; 193 dos processos do app, todas sem senha, e-mail, token, `Authorization`, `Bearer` ou corpo JSON. As 5 ocorrências de senha/e-mail no log são do `adbd` (comando `adb shell input text` da digitação automatizada) |

Extra: o rodapé abre o balão com o aviso completo ([6](2026-11-17_rc18-manual-6-rodape-balao.png)).

Executado por: Claude Code, a pedido do responsável, no emulador `Medium_Phone` (digitação por `adb`) · Data: 17/11/2026 · Commit/APK: `6f87302` (APK gerado nesta data; Wrapper local atualizado para Gradle 9.7.1, não commitado) · Backend: `main` com Oracle real (`.env` do responsável, não lido pelo agente)
