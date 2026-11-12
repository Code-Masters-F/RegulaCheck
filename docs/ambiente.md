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
Não precisa de banco nem de variável alguma. Opcionais: `SERVER_PORT` (padrão 8080) e `SERVER_HOST` (padrão `0.0.0.0`).

## Acesso do app à API
- **Emulador Android:** `http://10.0.2.2:8080` (alias do computador). `localhost` aponta para o próprio emulador e **não** funciona.
- **Aparelho físico:** `http://<IP-do-computador>:8080` na mesma rede (descubra o IP com `ipconfig`); libere a porta 8080 no firewall do Windows para redes privadas.
- A API escuta em `0.0.0.0` por padrão justamente para aceitar essas conexões; em rede pública prefira `SERVER_HOST=127.0.0.1`.
- Android 9+ bloqueia HTTP sem TLS por padrão: ao criar o app, habilitar tráfego em texto claro **somente em desenvolvimento** (configuração de segurança de rede restrita aos endereços acima). Execução remota autenticada exige HTTPS.

## Banco
**Oracle fornecido pela faculdade**, acesso por JDBC. Host `oracle.fiap.com.br`, porta `1521`, SID `orcl` (URL `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl`). Verificado em 12/11/2026: conexão JDBC funcionando a partir da máquina de desenvolvimento com o driver `ojdbc11` 23.26.4.0.0; servidor Oracle Database 19c Enterprise Edition, charset AL32UTF8, usuário com CREATE TABLE e CREATE SEQUENCE e quota ilimitada. Tabelas do RegulaCheck usam o prefixo `RC_`. Usuário e senha individuais ficam só no `.env` local. Nunca versione credenciais: exporte `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no shell/IDE. O PostgreSQL/Docker da fundação antiga foi removido.

## Variáveis
Ver `.env.example`. A aplicação não lê `.env` sozinha: exporte no shell (`$env:SERVER_PORT="8081"` no PowerShell; `export SERVER_PORT=8081` no Unix) ou configure a IDE. Variáveis de IA (`AI_*`) não são necessárias: a IA e a extração são mockadas.

## Problemas comuns
- Porta 8080 ocupada: defina `SERVER_PORT`.
- Java incompatível: use JDK 21 ou superior.
- Download do Maven falha: verifique rede/proxy.
- Oracle inacessível: confirme VPN/rede da faculdade e os dados de conexão.
- Mobile: emulador Android padrão -> `http://10.0.2.2:8080`; dispositivo físico -> IP do computador na rede. Não use `localhost` no app.
