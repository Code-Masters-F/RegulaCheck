# Quickstart: Criar rascunho e selecionar serviço (RC-07)

## Verificar (sem banco, sem Oracle)

```powershell
cd backend
.\mvnw.cmd clean verify
```

Esperado: testes de catálogo, criação, troca de serviço, listagem, matriz de perfis, concorrência (repositório em memória) e higiene de log verdes; testes Oracle ignorados e listados como **não verificados**.

Só esta feature:

```powershell
.\mvnw.cmd -Dtest="br.com.regulacheck.solicitacao.*Test,br.com.regulacheck.protocolo.*Test" test
```

## Preparar o Oracle (uma vez, pelo seu cliente SQL, com a sua conta)

Para banco **já criado** nas RC-03/RC-04, rode o novo script aditivo:

```text
database/scripts/06_motivo_revisao_protocolo.sql
```

Banco novo: `01_` → `02_` → `05_` (o `01_` já traz o motivo novo) e **não** rode o `06_`. Sem o `06_` num banco antigo, criar/listar/reabrir funcionam, mas trocar de serviço falha ao registrar a revisão.

## Exercitar as rotas à mão

```powershell
$env:DB_URL="jdbc:oracle:thin:@//HOST:1521/SERVICO"; $env:DB_USERNAME="..."; $env:DB_PASSWORD="..."
.\mvnw.cmd -DskipTests package
java -jar target\regulacheck-backend.jar
```

Em outro terminal (conta sintética `paciente.a@exemplo.invalid`; senha de demonstração em `database/scripts/README.md`):

```powershell
$t = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/login -ContentType application/json `
      -Body '{"email":"paciente.a@exemplo.invalid","senha":"<senha-de-demonstracao>"}').token
$h = @{ Authorization = "Bearer $t" }
Invoke-RestMethod http://localhost:8080/protocolos -Headers $h
Invoke-RestMethod -Method Post http://localhost:8080/solicitacoes -Headers $h -ContentType application/json `
  -Body '{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}'
Invoke-RestMethod http://localhost:8080/solicitacoes -Headers $h
```

Verificações manuais úteis: o mesmo `PUT` com o mesmo serviço mantém `revisao`; `GET /solicitacoes/{id}` com o token da paciente B devolve 404; o token do administrador devolve 403 em todas as rotas.

## Teste Oracle opt-in (concorrência de verdade)

```powershell
$env:RC_TESTE_ORACLE="SIM"   # com DB_* exportadas e scripts 01, 02, 05 e 06 aplicados
.\mvnw.cmd -Dtest=SolicitacaoOracleTest test
```

O teste cria só os próprios pedidos, dispara PUTs simultâneos, confere a sequência de revisões e remove apenas o que criou. Sem a variável, é ignorado e deve ser relatado como NÃO VERIFICADO.
