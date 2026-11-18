# RC-18 — App Android: base, login e meus pedidos (17/11/2026)

> As capturas de tela desta verificação foram retiradas do repositório em 18/11/2026 para não versionar arquivos grandes; o registro abaixo é o resultado em texto.

Spec: [specs/017-app-android-login-pedidos/](../../specs/017-app-android-login-pedidos/) · Issue #41 · Épico #40. Máquina: Windows 11, emulador `Medium_Phone` (1080 × 2400, 420 dpi, fuso UTC), JDK 21 do Android Studio (`jbr`, 21.0.10).

## 1. Build e testes automatizados

```bash
cd mobile/app
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt"   # ver docs/ambiente.md
./gradlew assembleDebug testDebugUnitTest
```

Resultado real (última execução, após a correção da seção 4): `BUILD SUCCESSFUL`, APK `build/outputs/apk/debug/regulacheck-app-debug.apk`.

| Suíte | Testes | Falhas | Cobre |
|---|---|---|---|
| `api.ApiClientTest` | 20 | 0 | 400, 401 login, 401 sessão (callback), 401 logout, 403, 404, 409 com e sem `Retry-After`, 413, 415, 503, sem conexão, tempo esgotado, sem retentativa, conexão nova por chamada, token no cabeçalho, JSON fora do contrato, estado desconhecido, tempos 10/30/70 s, `toString` sem segredo |
| `sessao.ServicoSessaoTest` | 8 | 0 | entrar, conta não paciente (logout com o próprio token, nada guardado), credencial inválida, sair com 503 e sem rede (sessão apagada), 401 apaga sessão, sessão vencida, sessão válida |
| `ui.EstadoPedidoUiTest` | 6 | 0 | estado desconhecido, rascunho com/sem documentos, rótulo e ação por estado, só "Atualizar" funciona, resultado geral, data no fuso |

## 2. Emulador com o backend real (sem banco)

Backend da `main` empacotado e iniciado **sem** `DB_*`: `./mvnw -DskipTests package` e `java -Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt -jar target/regulacheck-backend.jar`. `GET /health` → `{"status":"UP"}`; `POST /auth/login` → `503 DEPENDENCIA_INDISPONIVEL`.

| Verificação | Resultado |
|---|---|
| Login com o backend real respondendo 503 | "O serviço está indisponível agora." + "Tentar de novo"; e-mail e senha preservados |
| Backend parado e "Tentar de novo" | "Sem conexão com a internet." + "Verifique sua rede e tente de novo."; e-mail preservado |

## 3. Conferência visual com servidor de teste local (não é o backend)

Sem credenciais do Oracle nesta máquina, a lista e o detalhe foram conferidos com um servidor HTTP de teste fora do repositório, que devolvia respostas no formato de `docs/api/openapi.yaml` e dados da carga sintética. **Isso não comprova integração com o backend.**

| Tela · estado | Prévia | Resultado |
|---|---|---|
| E1.1 Padrão | KulXG | Confere: textos, aviso de entrada, rodapé. Bricolage no tamanho óptico 96 ("Entrar" 84 dp, igual à prévia) |
| E1.1 Credenciais inválidas | o1mx3h | Confere: "E-mail ou senha incorretos.", e-mail mantido, senha limpa |
| E1.4 no login (401 em rota privada) | — | Sessão apagada, login com "Sua sessão terminou. Entre novamente." |
| E1.2 Sucesso | H0BJi | Confere: saudação, destaque com próximo passo, linhas com ícone + texto |
| E1.2 Todos os estados + desconhecido | F4 | 8 estados com ícone + texto; `ESTADO_FUTURO` → "Estado não reconhecido" sem falha |
| E1.2 Vazia | v6aVEO | Confere |
| E1.2 Erro (503) | FzpeX | Confere |
| E1.3 Com pendências | DGEB7 | Confere, exceto a linha "Leitura e conferência" e a versão do protocolo (dependem de rotas da RC-19) |
| E1.3 Rascunho com documentos | xRLu9 (parcial) | "Ler documentos"; rascunho fino (a conferir, pronto) fica para a RC-19 |
| Ação da RC-19 | — | "Esta etapa ainda não está disponível nesta versão do app." |
| Reabrir com sessão salva | — | Abre direto em "Meus pedidos" (token cifrado lido do Keystore) |
| Sair | — | Volta ao login sem aviso; login seguinte começa vazio |

**Não conferidos visualmente:** E1.2 Carregando e Sem conexão; E1.3 Carregando, Não encontrado (404) e Sem permissão (403), cobertos só pelos testes de mapeamento; conta não paciente na tela; fonte ampliada e TalkBack (fora do épico).

## 4. Defeito encontrado e corrigido

Abrir um pedido mais de 30 s depois de carregar a lista mostrava "Sem conexão"; "Tentar de novo" funcionava. Causa: o OkHttp reaproveitava uma conexão ociosa que o servidor já tinha fechado, e o app não repete requisições (#40). Correção: pool sem conexões ociosas (`ConnectionPool(0, …)`, `ApiClient.criarHttpPadrao`) e teste `cada chamada usa conexao nova…`. Reverificado: pedido aberto 40 s depois da lista.

## 5. Logcat

`adb logcat -d` após todo o percurso (3.425 linhas). Busca por token de teste, `Authorization`, `Bearer`, corpos (`"itens"`, `avisoFicticio`), senhas e e-mail digitados: **0 ocorrências no processo do app**. As únicas 5 ocorrências (senha e e-mail) são do `adbd`, que registra o comando `adb shell input text` usado para digitar no teste; na digitação manual, ele não participa.

## 6. Percurso com backend real e Oracle

- Primeira tentativa: bloqueada, sem `.env` com `DB_*` na máquina.
- Executado no mesmo dia, depois que o responsável preencheu o `.env` e subiu o backend: **12 de 12 passos ✅** — ver [2026-11-17_rc18-roteiro-manual.md](2026-11-17_rc18-roteiro-manual.md). O passo "backend parado" foi feito cortando a rede do emulador; a sessão expirada (passo 9) foi executada pelo responsável com o backend reiniciado com `SESSION_TTL_MINUTES=1`.

## 7. Aviso acadêmico discreto (mesma data, depois do commit 84ae058)

Decisão do responsável: a caixa grande com o aviso acadêmico pesava no visual. Desenho: spec 015, T075–T078; app: spec 017, T024–T028. As verificações do login acima foram feitas com a versão anterior do aviso.

`./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`, 34 testes, 0 falhas. No emulador:

| Verificação | Prévia | Resultado |
|---|---|---|
| Login com `Aviso/Proposta` | KulXG (reexportada) | Confere: "Prepare seus documentos" / "Veja o que está completo e o que falta antes de enviar." (a quebra de linha muda com a largura da tela) |
| Tocar no rodapé | F3 `EVIof` | Balão escuro centralizado acima do rodapé, "Sobre este aplicativo" + aviso completo, sem sombra |
| Toque longo no rodapé | — | Abre o mesmo balão (comparação por pixels com o balão aberto por toque simples: idênticas) |
| Tocar fora | — | Fecha; a tela fica idêntica à de antes de abrir (comparação por pixels) |

Não verificado: leitura do rodapé pelo TalkBack (rótulo "Protótipo acadêmico · dados fictícios. Toque para ler o aviso completo." declarado no código).
