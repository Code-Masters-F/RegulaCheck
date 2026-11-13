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
Não precisa de banco nem de variável alguma para subir e responder `/health`. Opcionais: `SERVER_PORT` (padrão 8080), `SERVER_HOST` (padrão `0.0.0.0`) e `SESSION_TTL_MINUTES` (validade da sessão, 1 a 1440, padrão 30). As rotas `/auth/*` precisam do banco: sem `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` exportadas elas respondem 503 `DEPENDENCIA_INDISPONIVEL`.

## Acesso do app à API
- **Emulador Android:** `http://10.0.2.2:8080` (alias do computador). `localhost` aponta para o próprio emulador e **não** funciona.
- **Aparelho físico:** `http://<IP-do-computador>:8080` na mesma rede (descubra o IP com `ipconfig`); libere a porta 8080 no firewall do Windows para redes privadas.
- A API escuta em `0.0.0.0` por padrão justamente para aceitar essas conexões; em rede pública prefira `SERVER_HOST=127.0.0.1`.
- Android 9+ bloqueia HTTP sem TLS por padrão: ao criar o app, habilitar tráfego em texto claro **somente em desenvolvimento** (configuração de segurança de rede restrita aos endereços acima). Execução remota autenticada exige HTTPS.

## Banco
**Oracle fornecido pela faculdade**, acesso por JDBC. Host `oracle.fiap.com.br`, porta `1521`, SID `orcl` (URL `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`). Verificado em 12/11/2026: conexão JDBC funcionando a partir da máquina de desenvolvimento com o driver `ojdbc11` 23.26.4.0.0; servidor Oracle Database 19c Enterprise Edition, charset AL32UTF8, usuário com CREATE TABLE e CREATE SEQUENCE e quota ilimitada. Tabelas do RegulaCheck usam o prefixo `RC_`. Usuário e senha individuais ficam só no `.env` local. Nunca versione credenciais: exporte `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no shell/IDE. O PostgreSQL/Docker da fundação antiga foi removido.

## Testes
- `.\mvnw.cmd clean verify` (a partir de `backend/`) roda os testes unitários e de servlet **sem banco**, com repositórios em memória.
- Os testes que usam o Oracle real (`AuthOracleTest`) são **opt-in**: só rodam se `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` estiverem exportadas **e** `RC_TESTE_ORACLE=SIM`; sem isso são ignorados e devem ser registrados como **não verificados**. Pré-requisito: scripts `01_`, `02_` e `05_` executados na sua conta. Eles removem apenas as sessões que criaram.
- `InjecaoSqlOracleTest` (RC-06) também é opt-in e **somente leitura**: confere que entradas com aspas e comandos de banco não alteram a consulta de usuário; mesmas variáveis e `RC_TESTE_ORACLE=SIM`.
- Os logs da API nunca contêm senha, hash, token nem e-mail.

## Variáveis
Ver `.env.example`. A aplicação não lê `.env` sozinha: exporte no shell (`$env:SERVER_PORT="8081"` no PowerShell; `export SERVER_PORT=8081` no Unix) ou configure a IDE. Variáveis de IA (`AI_*`) não são necessárias: a IA e a extração são mockadas.

## Chave do simulador (RC-05)
`IOT_API_KEY` é a chave que o simulador de sensores envia no cabeçalho `X-Chave-Simulador`. É **opcional** para iniciar a API: sem ela a rota de telemetria responde sempre 401. Defina só no shell/IDE ou no seu `.env` local (ignorado pelo Git); nunca versione valor real. A chave só vale na telemetria; em qualquer outra rota responde 403. Os testes usam uma chave fictícia própria.

## Arquivos e canal seguro (RC-06)
- `DOCUMENT_STORAGE_PATH`: raiz da **área privada de arquivos**. Aponte para uma pasta **fora do repositório** e de qualquer pasta pública (as pastas `uploads/` e `storage/` do repositório são ignoradas pelo Git, mas o ideal é uma pasta fora dele). Opcional para iniciar: sem ela a API sobe, a conferência de arquivos funciona (quarentena temporária privada) e guardar um arquivo responde 503 `DEPENDENCIA_INDISPONIVEL`. Cada arquivo é gravado com um nome interno aleatório de 32 caracteres, sem extensão e sem o nome original.
- `API_PUBLIC_URL`: endereço pelo qual o app alcança a API na **demonstração remota**. Se for remoto, precisa ser `https://...`; a API **não inicia** com `http` remoto, URL inválida ou URL com usuário e senha. `http` só é aceito em `localhost`, `127.0.0.1`, `[::1]` e `10.0.2.2`. O servidor embutido continua em HTTP: o TLS é feito por um proxy ou túnel da equipe (esta aplicação não emite certificado). Com `https`, a API responde `Strict-Transport-Security`.
- Varredura de segredos: `.\mvnw.cmd -Dtest=VarreduraDeSegredosTest test` (a partir de `backend/`); ela também roda no `clean verify`. Se você tiver `.env` local ou `DB_*`/`IOT_API_KEY` no ambiente, ela confere que o **valor real** não está em nenhum arquivo do repositório e nunca imprime o valor. Opcionalmente use também o [gitleaks](https://github.com/gitleaks/gitleaks).
- Mais detalhes e o guia para rotas novas: [api/protecoes.md](api/protecoes.md).

## Problemas comuns
- Porta 8080 ocupada: defina `SERVER_PORT`.
- Java incompatível: use JDK 21 ou superior.
- Download do Maven falha: verifique rede/proxy.
- Oracle inacessível: confirme VPN/rede da faculdade e os dados de conexão.
- Mobile: emulador Android padrão -> `http://10.0.2.2:8080`; dispositivo físico -> IP do computador na rede. Não use `localhost` no app.
