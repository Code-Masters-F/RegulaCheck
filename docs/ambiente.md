# Ambiente

Verificado em 12/11/2026 (Windows 11): JDK Temurin 25.0.2 e Maven 3.9.14 (o projeto usa o Wrapper). Requisito: JDK 21 ou superior. O backend é Java puro com Jetty embutido; ver [decisoes.md](decisoes.md).

## Comandos (a partir de `backend/`)
Windows PowerShell:
```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```
Linux/macOS: `./mvnw clean verify`

## Executar a API (verificado em 12/11/2026)
Depois do `clean verify`, a partir de `backend/`:
```powershell
java -jar target\regulacheck-backend.jar
curl http://localhost:8080/health   # 200 {"status":"UP"}
```
Documentação interativa (Swagger UI, RC-28): `http://localhost:8080/docs`. A rota é pública e só aceita GET, mas pela página é possível executar todas as rotas, inclusive as que alteram dados, com o token informado em Authorize. Mostra o contrato `docs/api/openapi.yaml`, copiado para o jar no build e servido em `/docs/openapi.yaml`, e executa as rotas na mesma origem. Os arquivos do Swagger UI vêm da dependência `org.webjars:swagger-ui` 5.34.1 do `pom.xml`, sem CDN e sem rede. Para o percurso completo, veja [api/roteiro-avaliacao.md](api/roteiro-avaliacao.md).

Não precisa de banco nem de variável alguma para subir e responder `/health`. Opcionais: `SERVER_PORT` (padrão 8080), `SERVER_HOST` (padrão `0.0.0.0`) e `SESSION_TTL_MINUTES` (validade da sessão, 1 a 1440, padrão 30). As rotas `/auth/*`, `/protocolos`, `/solicitacoes` (RC-07) e `/solicitacoes/{id}/documentos` (RC-08) precisam do banco: sem `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` exportadas elas respondem 503 `DEPENDENCIA_INDISPONIVEL`. O **envio e o download** de documentos precisam também de `DOCUMENT_STORAGE_PATH` (ver "Arquivos e canal seguro"); sem ela, respondem 503 e a lista de documentos e o pedido continuam funcionando.

## Acesso do app à API
- **Emulador Android:** `http://10.0.2.2:8080` (alias do computador). `localhost` aponta para o próprio emulador e **não** funciona.
- **Aparelho físico:** `http://<IP-do-computador>:8080` na mesma rede (descubra o IP com `ipconfig`); libere a porta 8080 no firewall do Windows para redes privadas.
- A API escuta em `0.0.0.0` por padrão justamente para aceitar essas conexões; em rede pública prefira `SERVER_HOST=127.0.0.1`.
- Android 9+ bloqueia HTTP sem TLS por padrão: o app (RC-18) só permite texto claro na variante **debug**, para `10.0.2.2`, `localhost` e `127.0.0.1` (`mobile/app/src/debug/res/xml/network_security_config.xml`). Execução remota autenticada exige HTTPS.
- A URL vem de `BuildConfig.API_BASE_URL`: padrão `http://10.0.2.2:8080`; aparelho físico com `-PapiBaseUrl=...` (ou `adb reverse tcp:8080 tcp:8080` + `-PapiBaseUrl=http://localhost:8080`). Detalhes: [mobile/README.md](../mobile/README.md).

## App Android (RC-18, verificado em 17/11/2026)
- Projeto Gradle em `mobile/app/` com Wrapper (Gradle 8.7, AGP 8.5.2, Kotlin 2.0.21, compile/targetSdk 34, minSdk 26). Requer Android SDK 34 (`ANDROID_HOME` ou `local.properties`, não versionado) e **JDK 21**: o JDK 25 padrão não serve ao Android Gradle Plugin; use o do Android Studio só na sessão.
- A partir de `mobile/app/`: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`) e `./gradlew assembleDebug testDebugUnitTest` (`.\gradlew.bat` no PowerShell). APK: `build/outputs/apk/debug/regulacheck-app-debug.apk`; instalar com `adb install -r`.
- Os testes do app (JUnit 4 + MockWebServer) não precisam de emulador, backend nem rede externa. Evidências: [RC-18](evidencias/2026-11-17_rc18-app-android.md) e [RC-19](evidencias/2026-11-17_rc19-app-documentos-resultado.md).
- Envio de documentos pelo app (RC-19): o backend precisa de `DOCUMENT_STORAGE_PATH`; para escolher as amostras no seletor do emulador, copie-as para a pasta Download (`adb push samples/encaminhamento-sintetico.pdf /sdcard/Download/`). No Git Bash, prefixe o comando com `MSYS_NO_PATHCONV=1`, senão `/sdcard/...` vira um caminho do Windows.

## Banco
**Oracle fornecido pela faculdade**, acesso por JDBC. Host `oracle.fiap.com.br`, porta `1521`, SID `orcl` (URL `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`). Verificado em 12/11/2026: conexão JDBC funcionando a partir da máquina de desenvolvimento com o driver `ojdbc11` 23.26.4.0.0; servidor Oracle Database 19c Enterprise Edition, charset AL32UTF8, usuário com CREATE TABLE e CREATE SEQUENCE e quota ilimitada. Tabelas do RegulaCheck usam o prefixo `RC_`. O grupo usa **uma única conta** do Oracle: usuário e senha são os mesmos para todos e ficam só no `.env` local de cada pessoa (peça ao grupo, não copie para o repositório). Nunca versione credenciais: exporte `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no shell/IDE. O PostgreSQL/Docker da fundação antiga foi removido.

## Testes
- `.\mvnw.cmd clean verify` (a partir de `backend/`) roda os testes unitários e de servlet **sem banco**, com repositórios em memória.
- Os testes que usam o Oracle real (`AuthOracleTest`) são **opt-in**: só rodam se `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` estiverem exportadas **e** `RC_TESTE_ORACLE=SIM`; sem isso são ignorados e devem ser registrados como **não verificados**. Pré-requisito: scripts `01_`, `02_` e `05_` executados na conta do grupo (e o `06_` se o esquema foi criado antes da RC-07; já aplicado em 13/11/2026). Eles removem apenas as sessões que criaram.
- `SolicitacaoOracleTest` (RC-07) e `DocumentoOracleTest` (RC-08) também são opt-in (mesmas variáveis e `RC_TESTE_ORACLE=SIM`). O de documentos exige os scripts `01_`, `02_`, `05_`, `06_` e `07_` aplicados, usa uma pasta temporária própria para os arquivos e remove só os pedidos, revisões, documentos e arquivos que criou. Sem a variável, ficam ignorados e devem ser registrados como **não verificados**. `AlteracaoDocumentoOracleTest` (RC-09) também é opt-in, no mesmo padrão, e exige os scripts até o `07_` já aplicados (a RC-09 não tem script novo).
- `AlteracaoDocumentoOracleTest` (RC-09) e `ExtracaoOracleTest` (RC-10) também são opt-in, no mesmo padrão. O da RC-10 exige o script `08_extracao.sql` aplicado (só com autorização do responsável), usa as amostras sintéticas de `samples/` e uma pasta temporária própria, e remove só as leituras, documentos, revisões, pedidos e sessões que criou. Sem a execução real, a RC-10 fica **não verificada no Oracle**.
- `ConferenciaOracleTest` (RC-11) também é opt-in, no mesmo padrão. Exige os scripts até o `08_` e o `09_conferencia.sql` aplicados (**só com autorização específica do responsável**; aplicado e executado em 13/11/2026, 3 testes verdes), usa as amostras sintéticas de `samples/` e uma pasta temporária própria, e remove só as decisões, leituras, documentos, revisões, pedidos e sessões que criou.
- `AnaliseOracleTest` (RC-13) também é opt-in, no mesmo padrão (`RC_TESTE_ORACLE=SIM` e `DB_*`). Exige os scripts até o `09_` e o `10_analise_historico.sql` aplicados (ou um `01_` atual); cria pedidos, leituras e análises próprios com a conta fictícia da carga e remove só o que criou. Sem o `10_`, só as rotas `…/analises` respondem 503, e o próprio teste falha no início com a orientação, sem criar dados. Aplicado e executado em 14/11/2026: 2 testes verdes, sem resíduo. Para carregar o `.env` só no processo do teste (bash, a partir da raiz): `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw test -Dtest=AnaliseOracleTest)` (o `RC_TESTE_ORACLE=SIM` explícito liga o teste mesmo que o `.env` não o tenha).
- `ReenvioOracleTest` (RC-16) também é opt-in, no mesmo padrão (`RC_TESTE_ORACLE=SIM` e `DB_*`). Exige os scripts até o `10_` (a RC-16 não tem script novo); confere no Oracle real que incluir documento depois da análise tira a vigência na mesma transação e que a verificação da versão corrigida grava a nova análise com a comparação. Cria pedido, leituras e análises próprios com a conta fictícia da carga e remove só o que criou. Comando: `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw test -Dtest=ReenvioOracleTest)`. Executado em 17/11/2026 junto com o `AnaliseOracleTest`: 3 testes verdes, limpeza sem erro.
- `SubmissaoOracleTest` (RC-17) também é opt-in, no mesmo padrão (`RC_TESTE_ORACLE=SIM` e `DB_*`). Exige os scripts até o `10_` (a tabela `RC_SUBMISSAO` é da RC-03; nenhum script novo); confere a gravação da submissão com a análise vigente e o estado `SUBMETIDA_SIMULACAO` na mesma transação, repetição e consulta com o mesmo comprovante, 4 conclusões simultâneas sob o bloqueio real e a leitura da submissão da carga (`SIM-2026-0001`, sem alterá-la). Cria pedidos próprios com a conta fictícia da carga e remove só o que criou. Comando: `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw test -Dtest=SubmissaoOracleTest)`. Executado em 17/11/2026: 3 testes verdes, limpeza sem erro.
- `RegrasDoProtocoloOracleTest` (RC-12) também é opt-in, no mesmo padrão (`RC_TESTE_ORACLE=SIM` e `DB_*` exportadas). Só **lê** as regras da v0.1 em `RC_REGRA` (scripts `01_` e `02_`; nenhum script novo) e confere que o motor as aceita; não cria nem apaga nada. Sem banco, os testes do motor e da avaliação (`validacao/*Test`, `analise/*Test`) rodam no `clean verify` normal.
- **Amostras e catálogo da leitura simulada (RC-10):** `samples/` e `backend/src/test/resources/extracao-simulada/` são gerados por `GeradorDeAmostras` e **commitados**; só se regeneram de propósito: `.\mvnw.cmd -Dtest=GeradorDeAmostras -Dgerar.amostras=SIM test` (a partir de `backend/`). Sem a propriedade o gerador é ignorado.
- **Conjuntos sintéticos de demonstração (RC-23):** 15 conjuntos em `samples/conjuntos/` (índice em `samples/catalogo-conjuntos.md`), cada um com `gabarito.md` escrito antes da execução; leituras **simuladas** acrescentadas a `backend/src/main/resources/extracao-simulada/catalogo.json`. Regressão sem banco e sem rede, a partir de `backend/`: `.mvnw.cmd -Dtest=ConjuntosDemonstracaoTest test`. `GeradorDeAmostras` **nunca sobrescreve** arquivo existente (o SHA-256 identifica a leitura) e não reescreve `gabarito.md` existente; `samples/**/*.txt` está como `-text` em `.gitattributes` para o hash não mudar com o fim de linha. Detalhes em `samples/README.md`.
- `InjecaoSqlOracleTest` (RC-06) também é opt-in e **somente leitura**: confere que entradas com aspas e comandos de banco não alteram a consulta de usuário; mesmas variáveis e `RC_TESTE_ORACLE=SIM`.
- Os logs da API nunca contêm senha, hash, token nem e-mail.

## Spec Kit
- Os comandos `/speckit-*` (Claude) e as skills `speckit-*` (Codex) usam os scripts PowerShell de `.specify/scripts/powershell/`.
- A feature ativa fica em `.specify/feature.json`, que **não é versionado**: cada pessoa mantém o seu. O `/speckit-specify` o cria; para retomar uma feature existente, crie-o à mão, por exemplo `{ "feature_directory": "specs/015-fluxo-telas-app" }`. Ordem usada pelos scripts: variável `SPECIFY_FEATURE_DIRECTORY`, se exportada; depois `.specify/feature.json`; por fim, o prefixo numérico do nome da branch.

## Variáveis
Ver `.env.example`. A aplicação não lê `.env` sozinha: exporte no shell (`$env:SERVER_PORT="8081"` no PowerShell; `export SERVER_PORT=8081` no Unix) ou configure a IDE. Variáveis de IA (`AI_*`) não são necessárias: a IA e a extração são mockadas.

## Chave do simulador (RC-05)
`IOT_API_KEY` é a chave que o simulador de sensores envia no cabeçalho `X-Chave-Simulador`, na rota `POST /sessoes-captura/{id}/leituras` (RC-21). É **opcional** para iniciar a API: sem ela a rota de telemetria responde sempre 401. O simulador lê a **mesma** variável (só dela, nunca de argumento) e, opcionalmente, `API_URL` (padrão `http://localhost:8080`); como rodar está em [simulador-iot/README.md](../simulador-iot/README.md). Defina só no shell/IDE ou no seu `.env` local (ignorado pelo Git); nunca versione valor real. A chave só vale na telemetria; em qualquer outra rota responde 403. Os testes usam uma chave fictícia própria.

## Arquivos e canal seguro (RC-06)
- `DOCUMENT_STORAGE_PATH`: raiz da **área privada de arquivos**, usada de verdade pelo envio e pelo download de documentos desde a RC-08 (a API recusa subir se a pasta não puder ser preparada, por exemplo se o caminho apontar para um arquivo comum). Aponte para uma pasta **fora do repositório** e de qualquer pasta pública (as pastas `uploads/` e `storage/` do repositório são ignoradas pelo Git, mas o ideal é uma pasta fora dele). Opcional para iniciar: sem ela a API sobe, a conferência de arquivos funciona (quarentena temporária privada) e guardar um arquivo responde 503 `DEPENDENCIA_INDISPONIVEL`. Cada arquivo é gravado com um nome interno aleatório de 32 caracteres, sem extensão e sem o nome original. Desde a RC-09 ela também é usada para **apagar** arquivos (remover, substituir e excluir rascunho). Sem ela, remover e excluir têm sucesso no banco e não apagam arquivo; substituir responde 503 (não consegue guardar o arquivo novo).
- `API_PUBLIC_URL`: endereço pelo qual o app alcança a API na **demonstração remota**. Se for remoto, precisa ser `https://...`; a API **não inicia** com `http` remoto, URL inválida ou URL com usuário e senha. `http` só é aceito em `localhost`, `127.0.0.1`, `[::1]` e `10.0.2.2`. O servidor embutido continua em HTTP: o TLS é feito por um proxy ou túnel da equipe (esta aplicação não emite certificado). Com `https`, a API responde `Strict-Transport-Security`.
- Varredura de segredos: `.\mvnw.cmd -Dtest=VarreduraDeSegredosTest test` (a partir de `backend/`); ela também roda no `clean verify`. Se você tiver `.env` local ou `DB_*`/`IOT_API_KEY` no ambiente, ela confere que o **valor real** não está em nenhum arquivo do repositório e nunca imprime o valor. Opcionalmente use também o [gitleaks](https://github.com/gitleaks/gitleaks).
- Mais detalhes e o guia para rotas novas: [api/protecoes.md](api/protecoes.md).

## Problemas comuns
- Porta 8080 ocupada: defina `SERVER_PORT`.
- Java incompatível: use JDK 21 ou superior.
- Download do Maven falha: verifique rede/proxy.
- Oracle inacessível: confirme VPN/rede da faculdade e os dados de conexão.
- Mobile: emulador Android padrão -> `http://10.0.2.2:8080`; dispositivo físico -> IP do computador na rede. Não use `localhost` no app.
- Gradle, Jetty ou qualquer Java falha com "Unable to establish loopback connection" ou "Invalid argument: connect" (visto em 17/11/2026 na máquina de desenvolvimento; a causa de fundo não foi confirmada): o JDK não consegue criar o pipe interno (socket AF_UNIX) na pasta temporária padrão. Aponte-o para uma pasta sem espaços, só na sessão: `mkdir -p /c/Users/Public/rcgt` e `export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt"` (vale para o Gradle e o daemon; para o backend, passe a mesma `-D...` no `java -jar`). Não altere variáveis permanentes da máquina.
- Com `JAVA_TOOL_OPTIONS` exportada (contorno acima), `./mvnw clean verify` falha em `SimuladorSensoresProcessoTest.modoSecoEReproduzivelEAsSequenciasTemOsValoresEsperados:41` ("expected: <0> but was: <1>", verificado em 17/11/2026): cada processo Java imprime `Picked up JAVA_TOOL_OPTIONS: …` e o teste conta essa linha junto com a saída do simulador. Sem a variável, os testes que iniciam o simulador caem no erro de loopback. Os demais 756 testes passam. Para gerar o jar e subir a API nessa situação: `./mvnw clean package -DskipTests` (depois de rodar o `verify` e conferir que só esse teste falhou) e `java -jar target/regulacheck-backend.jar`.
