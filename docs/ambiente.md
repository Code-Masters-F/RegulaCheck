# Ambiente

Verificado em 11/11/2026 (Windows 11): JDK Temurin 25.0.2, Maven 3.9.14 (global; o projeto usa o Wrapper), Docker 29.5.2 (não usado), porta 8080 livre.

## Comandos (a partir de `backend/`)
Windows PowerShell:
```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```
Linux/macOS:
```bash
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Health: `GET http://localhost:8080/actuator/health` -> 200 `{"status":"UP"}`. Não comprova banco, IA, mobile ou sensores.

## Banco
Suba o PostgreSQL conforme `database/README.md` e exporte `DB_PASSWORD` (e, se necessário, `DB_URL`/`DB_USERNAME`) antes de rodar o backend; sem `DB_PASSWORD` o app não inicia. Os testes (`verify`) não usam o banco.

## Variáveis
Ver `.env.example`. O Spring Boot não lê `.env` sozinho: exporte no shell (`$env:SERVER_PORT="8081"` no PowerShell; `export SERVER_PORT=8081` no Unix) ou configure a IDE. `SERVER_PORT` e `DB_*` são usadas agora.

## Problemas comuns
- Porta 8080 ocupada: defina `SERVER_PORT`.
- Java incompatível: use JDK 21 ou superior.
- Docker não responde: abra o Docker Desktop e aguarde.
- Download do Maven falha: verifique rede/proxy.
- Mobile: emulador Android padrão -> `http://10.0.2.2:8080`; dispositivo físico -> IP do computador na rede. Não use `localhost` no app.
