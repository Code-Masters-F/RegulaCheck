# RC-22 — App Android: condições da captura (verificação de 18/11/2026)

História [RC-22 #47](https://github.com/Code-Masters-F/RegulaCheck/issues/47) (parte do app) · spec [023](../../specs/023-app-android-condicoes-captura/spec.md) · tarefas [T001–T019](../../specs/023-app-android-condicoes-captura/tasks.md) · backend e simulador da [spec 016](../../specs/016-iot-simulada/spec.md), sem alteração. O registro é só em texto: as capturas usadas na conferência ficaram fora do repositório.

**Ambiente**:

- AVD `RC22_Phone` (`emulator-5556`, 1080 × 2400, API 37), criado nesta verificação a partir da configuração do `Medium_Phone`. Outra sessão usava o `Medium_Phone` ao mesmo tempo, com toques e login de administrador que não eram desta verificação.
- Gradle com o JDK 21 do Android Studio.
- Backend da branch `023-app-android-condicoes-captura` em `http://localhost:8080`, com Oracle real, iniciado pelo responsável. O `.env` não foi lido pelo agente.
- Simulador `simulador-iot/SimuladorSensores.java` executado pelo responsável, com a `IOT_API_KEY` dele.
- Conta fictícia `paciente.a@exemplo.invalid`, com digitação por `adb shell input`.
- Pedido de teste 119, criado no app, e sessão de captura 41.
- Amostras de `samples/` na pasta Download do emulador.

## 1. Testes automatizados

```text
mobile/app> JAVA_HOME="…\Android Studio\jbr" ./gradlew assembleDebug testDebugUnitTest
BUILD SUCCESSFUL — 112 testes, 0 falhas, 0 ignorados (30 novos)
```

| Suíte nova | Testes | O que cobre |
|---|---|---|
| `api/TelemetriaApiTest` | 6 | `POST …/sessoes-captura`: 201 e 200, sem corpo, com o token, sem chave do simulador. `GET …/leituras` em cada `situacao`. `SEM_LEITURA` sem `sessao` nem `ultimaLeitura`. Erros 409, 403, 404 e 503 |
| `ui/ApresentacaoCapturaTest` | 13 | Seis estados do desenho com os textos de `textos.md`: luminosidade alta, distância fora para os dois lados, recuperada (com "antes"; a marca some ao sair de novo; fora → desatualizada → dentro), um sensor sem leitura, situação desconhecida, orientação vazia, idade pelo relógio do servidor (nunca negativa) e valor em pt-BR |
| `ui/CondicoesCapturaViewModelTest` | 11 | Sessão 201 e 200 seguida da consulta. Fora → dentro gera recuperada sem toque. **No máximo uma consulta em andamento** (resposta de 200 ms, intervalo de 10 ms) e nenhuma depois de cancelar. Segundo laço simultâneo não faz nada. Sem conexão e 503 → "indisponível", sem encerrar e com a consulta seguindo. Pedido de sessão não repetido na visita. 409 ao abrir → encerra sem consultar. 403 ao abrir, 404 e 401 na consulta → encerra (401 apaga a sessão local). Nova visita pede a sessão de novo |

## 2. Percurso no emulador (backend real, horários em UTC)

| # | Passo | Resultado observado |
|---|---|---|
| 1 | Pedido 119 → "Anexar documento" → tipo → passo Origem | "Como você quer anexar?", com "Fotografar" / "Veja as condições da captura e depois escolha a foto" e "Escolher arquivo" / "PDF, JPEG ou PNG do seu aparelho" (quadro `ncnpi`, apoio ajustado) |
| 2 | "Fotografar" sem leituras (18:30:55) | Sessão criada (o mesmo `POST` repetido por curl devolveu 200, `criada:false`, id 41). **Indisponível**: "Sensores indisponíveis agora", "—", "Você pode anexar e salvar seus documentos normalmente.", "Sem leitura disponível · sensor simulado" e "Escolher a foto mesmo assim" (quadro `kaHJz`) |
| 3 | Leituras da sequência `normal` com mais de 60 s (20:02:25) | **Desatualizada** nos dois sensores: 550 lux e 35 cm, a orientação da API ("Leitura antiga. Verifique se a estação de captura está ligada…"), "Última leitura há 2 min · sensor simulado" e "Escolher a foto mesmo assim" (quadro `Cna25`) |
| 4 | `--sequencia normal` (20:10:20–20:10:24) | **Normal**: 520 e 480 lux, 30 e 32 cm, "Adequada" nos dois, "Leitura há 0–2 s · sensor simulado" e "Escolher a foto" (quadro `o3kkGI`). A tela atualizou sozinha |
| 5 | `--sequencia fora-da-faixa` (20:10:28–20:10:42) | **Luminosidade fora**: 120–180 lux, "Baixa", "Ilumine melhor o documento antes de capturar." **Distância fora**: 50–60 cm, "Fora do ideal", "Aproxime um pouco a câmera do documento." A ação continuou "Escolher a foto" (quadros `fOrmB` e `NyDDm`; as orientações são as da API) |
| 6 | `--sequencia recuperacao` (20:10:46–20:10:51) | **Recuperada**: "Adequada de novo", "Condições adequadas de novo.", "Leitura há 0 s · antes: 180 lux · sensor simulado" e "antes: 50 cm" (quadro `DGf2T`) |
| 7 | Dentro da faixa → "Escolher a foto" | O seletor abriu com **só imagens** (o PDF não aparece). `identificacao-sintetica.png` → "Enviado" na lista (1 de 3) |
| 8 | Fora da faixa (20:14:54: 90 lux "Baixa", 55 cm "Fora do ideal") → "Escolher arquivo" | O seletor abriu com PDF e PNG. `encaminhamento-sintetico.pdf` → "Enviado" (2 de 3) |
| 9 | Rede cortada (`svc wifi disable; svc data disable`, 20:16:21) | Em até 12 s: **indisponível**, com "Escolher a foto mesmo assim" e "Escolher arquivo" disponíveis; nada travou |
| 10 | Rede religada | Em até 15 s a tela voltou sozinha a mostrar a telemetria (desatualizada, última leitura de 110 lux) |
| 11 | Logcat do processo do app (pid 3173, 200 linhas) | 0 ocorrências de `Bearer`, `Authorization`, `token`, senha, `X-Chave`, `IOT_API_KEY`, `%PDF`, `IHDR`, `paciente.a` e das rotas. No logcat inteiro, a única ocorrência de "Bearer" é do `TelephonyProvider` do sistema |

O ritmo das consultas (cerca de 4 s, sequenciais) se confirma pelo monitor da tela: as mudanças apareceram a cada 4–5 s durante as sequências.

## 3. Observações

- **Duas rodadas sem leituras.** A primeira `normal` não chegou à 8080, e a API continuou `SEM_LEITURA` na sessão 41. A causa provável foi a `IOT_API_KEY`, que não estava configurada no terminal do simulador e no backend. Depois que o responsável a configurou, o log do backend mostrou um reinício do pool (16:59:41 local) e, em seguida, as leituras 25 a 34 gravadas na sessão 41. O app mostrou "indisponível" o tempo todo, como deve.
- **Reiniciar o backend encerra a sessão do app.** Depois de cada reinício (19:59:41, por exemplo), a próxima chamada do app recebeu 401, e ele voltou ao login com "Sua sessão terminou. Entre novamente." (E1.4), como especificado. Um login em paralelo por curl não derrubou a sessão do app (conferido às 19:29).
- O simulador usa `API_URL` antes do padrão `http://localhost:8080`. Com dois backends ligados, usar `--api` explícito.

## 4. Não verificado

- TalkBack lendo as regiões anunciadas (situação e orientação) e fonte ampliada na E4.1: fora do épico (#40).
- 403, 404 e 409 da E4.1 com o backend real: só nos testes automatizados, porque exigiriam outra conta ou um pedido submetido.
- Rotação da tela com a E4.1 aberta.

## 5. Reconferência do logcat com o corpo da telemetria (T022, 18/11/2026, ~21:37 UTC)

Feita depois que o SC-005 passou a incluir o corpo da telemetria (T021). Na conferência do passo 11, esses termos não tinham sido procurados.

- **Ambiente**: emulador `emulator-5554` (1080 × 2400). APK da branch `023-app-android-condicoes-captura` no commit `3c4ee9a`, instalado com `adb install -r`. Backend com Oracle iniciado pelo responsável (`GET /health` → `{"status":"UP"}`). Simulador não usado. Conta `paciente.a@exemplo.invalid`, digitada por `adb shell input`. Logcat limpo (`logcat -c`) antes de abrir o app.
- **Percurso**: login → pedido 119 → "Documentos" → "Anexar documento" → "Outro documento" → "Fotografar". A E4.1 ficou aberta cerca de 22 s, o que dá várias consultas no intervalo de 4 s. Ela mostrou a telemetria recebida da API: os dois sensores "Desatualizada" (110 lux, 58 cm), a orientação da API "Leitura antiga. Verifique se a estação de captura está ligada…", "Última leitura há 1 h · sensor simulado" e "Escolher a foto mesmo assim". Isso confirma que corpos de telemetria chegaram ao app durante a busca.
- **Busca** em `logcat -d`, sem diferenciar maiúsculas: 1.891 linhas no total, 106 do processo do app (pid 32608).

| Termos procurados | Processo do app | Logcat inteiro |
|---|---|---|
| Corpo da telemetria: `consultadaEm`, `ultimaLeitura`, `medidaEm`, `prazoAtualidadeSegundos`, `DENTRO_DA_FAIXA`, `ABAIXO_DA_FAIXA`, `ACIMA_DA_FAIXA`, `DESATUALIZADA`, `SEM_LEITURA`, `VERIFICAR_ESTACAO`, `LUMINOSIDADE`, `DISTANCIA`, `"sensores"`, "Leitura antiga" | 0 | 0 |
| Rotas: `/leituras`, `sessoes-captura` | 0 | 0 |
| `Bearer`, `Authorization`, `X-Chave`, `IOT_API_KEY`, `%PDF`, `IHDR` | 0 | 0 |
| `token` | 0 | 15, todas do `WindowManager` do sistema (tokens internos de janela) |
| E-mail `paciente.a` e a senha de demonstração | 0 | 1 cada, na mesma linha do `adbd`, que registra o comando `adb shell input text` usado para digitar no login. Vem do método de teste, não do app |

- **Código**: em `mobile/app/src/main`, não há chamada de log (`Log.*`, `println`, `printStackTrace`, `System.out`/`err`), biblioteca de log no build nem interceptor no OkHttp.
- **Resultado**: o SC-005 é atendido também para o corpo da telemetria. Nenhuma linha do processo do app contém token, senha, chave do simulador, corpo da telemetria ou corpo de documento.
