# Ambiente

Verificado em 11/11/2026 (Windows 11): JDK Temurin 25.0.2, Maven 3.9.14 (global; o projeto usa o Wrapper). A fundação atual de `backend/` ainda é a versão antiga (Spring Boot/JPA/PostgreSQL) e será migrada; ver [decisoes.md](decisoes.md).

## Comandos (a partir de `backend/`)
Windows PowerShell:
```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```
Linux/macOS: `./mvnw clean verify`

O comando de execução da aplicação sem Spring (servidor Servlet embutido) e a rota de health (`GET /health`) serão documentados após a migração.

## Banco
**Oracle fornecido pela faculdade**, acesso por JDBC. Os dados de conexão (host, porta, service name, usuário, necessidade de VPN/rede) ainda **não foram informados**. Nunca versione credenciais: exporte `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no shell/IDE. O `database/docker-compose.yml` (PostgreSQL) é da fundação antiga e não será usado.

## Variáveis
Ver `.env.example`. A aplicação não lê `.env` sozinha: exporte no shell (`$env:SERVER_PORT="8081"` no PowerShell; `export SERVER_PORT=8081` no Unix) ou configure a IDE. Variáveis de IA (`AI_*`) não são necessárias: a IA e a extração são mockadas.

## Problemas comuns
- Porta 8080 ocupada: defina `SERVER_PORT`.
- Java incompatível: use JDK 21 ou superior.
- Download do Maven falha: verifique rede/proxy.
- Oracle inacessível: confirme VPN/rede da faculdade e os dados de conexão.
- Mobile: emulador Android padrão -> `http://10.0.2.2:8080`; dispositivo físico -> IP do computador na rede. Não use `localhost` no app.
