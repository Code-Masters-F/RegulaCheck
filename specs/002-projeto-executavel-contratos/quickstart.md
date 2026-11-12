# Quickstart — verificar a entrega da RC-02

Pré-requisito: JDK 21 ou superior (verificado com Temurin 25.0.2). Nenhum banco nem variável é necessária.

1. `cd backend` e `.\mvnw.cmd clean verify` (Linux/macOS: `./mvnw clean verify`): compila e roda os testes de health, erro e arquitetura. Registrar o resultado real.
2. Iniciar a API pelo comando documentado em `docs/ambiente.md` (definido na implementação após verificação).
3. `curl http://localhost:8080/health` → HTTP 200 e `{"status":"UP"}`.
4. `curl http://localhost:8080/rota-inexistente` → 404 no formato de erro, sem stack trace.
5. Parar a API, ocupar a porta 8080 e iniciar de novo para ver a mensagem clara; usar `SERVER_PORT` para trocar.
6. No emulador Android, abrir `http://10.0.2.2:8080/health` no navegador do emulador → 200. Em aparelho físico, `http://<IP-do-computador>:8080/health` na mesma rede (liberar a porta no firewall). `localhost` não funciona nesses casos.
7. Conferir a estrutura: pastas `backend/`, `mobile/`, `database/`, `simulador-iot/`, `docs/` com README; `.env.example` só com nomes.
8. Varredura de segredos (ex.: busca por `password=`, tokens, `.env` versionado) sem achados.
9. Verificar que `backend/src/main` não contém `org.springframework`, `jakarta.persistence` ou `org.hibernate` e que o `pom.xml` não depende de Spring em escopo de produção.
10. Ler os contratos e confirmar coerência com `CONTRATO_API.md`, `docs/protocolo-academico.md` e `docs/arquitetura.md`.
