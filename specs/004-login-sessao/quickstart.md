# Quickstart — verificar a RC-04

Pré-requisitos: JDK 21+, rede da faculdade para o Oracle, tabelas da RC-03 criadas e carregadas (`01`→`02`), `05_sessao_login.sql` executado. Credenciais do banco só no ambiente (nunca no repositório). A senha de demonstração fictícia está em [`database/scripts/README.md`](../../database/scripts/README.md).

## 1. Build e testes sem banco
```powershell
cd backend
.\mvnw.cmd clean verify
```
Esperado: testes unitários e de servlet verdes, inclusive o que verifica a senha de demonstração contra os 3 hashes da carga.

## 2. Testes contra o Oracle (opt-in)
```powershell
$env:DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl"
$env:DB_USERNAME="<seu usuário>"; $env:DB_PASSWORD="<sua senha>"; $env:RC_TESTE_ORACLE="SIM"
.\mvnw.cmd verify
```
Sem as variáveis, esses testes são ignorados e isso deve ser anotado como "não verificado".

## 3. Percurso manual
```powershell
java -jar target\regulacheck-backend.jar            # com DB_* exportadas
curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"paciente.a@exemplo.invalid","senha":"<senha de demonstração>"}'
curl -s http://localhost:8080/auth/sessao -H "Authorization: Bearer <token>"     # 200
curl -s -X POST http://localhost:8080/auth/logout -H "Authorization: Bearer <token>"   # 204
curl -s http://localhost:8080/auth/sessao -H "Authorization: Bearer <token>"     # 401 NAO_AUTENTICADO
```
Senha errada ou e-mail inexistente: o mesmo 401 `CREDENCIAIS_INVALIDAS`. Sem `DB_*`, o login dá 503 e `GET /health` continua 200.

## 4. Conferências finais
- Busca por senha de demonstração, hash completo e token nos logs gerados e nos arquivos versionados da história: 0 ocorrências (exceto a senha fictícia já documentada).
- `PEQUENOVAREJO`, `VENDA` e as 12 tabelas da RC-03 intactas; só `RC_SESSAO_LOGIN` é nova.
