# Modelo de dados — RC-04

A história reutiliza `RC_USUARIO` (RC-03) e acrescenta **uma** tabela. Nada existente é alterado.

## RC_USUARIO (existente, somente leitura nesta história)
Colunas usadas: `ID`, `EMAIL` (único, minúsculas), `NOME`, `SENHA_HASH` (BCrypt `$2a$12$…`, 60 caracteres), `PERFIL` (`PACIENTE` | `ADMINISTRADOR`), `ATIVO` (0/1). Consulta: por `EMAIL` e por `ID`, sempre com parâmetro (`?`).

## RC_SESSAO_LOGIN (nova, `database/scripts/05_sessao_login.sql`)

| Coluna | Tipo | Regras |
|---|---|---|
| `ID` | `NUMBER GENERATED ALWAYS AS IDENTITY` | PK `RC_SESSAO_LOGIN_PK` |
| `USUARIO_ID` | `NUMBER NOT NULL` | FK `RC_SESSAO_LOGIN_FK_USUARIO` → `RC_USUARIO(ID)` |
| `TOKEN_HASH` | `CHAR(64 CHAR) NOT NULL` | SHA-256 do token em hexadecimal minúsculo; UK `RC_SESSAO_LOGIN_UK_TOKEN`; CHECK de 64 caracteres hexadecimais |
| `CRIADA_EM` | `TIMESTAMP WITH TIME ZONE NOT NULL` | UTC; padrão `SYSTIMESTAMP AT TIME ZONE 'UTC'` |
| `EXPIRA_EM` | `TIMESTAMP WITH TIME ZONE NOT NULL` | UTC; CHECK `EXPIRA_EM > CRIADA_EM` (`RC_SESSAO_LOGIN_CK_PRAZO`) |
| `ENCERRADA_EM` | `TIMESTAMP WITH TIME ZONE` | nulo enquanto aberta; preenchido no logout |

Índice de FK: `RC_SESSAO_LOGIN_IX_USUARIO (USUARIO_ID)`. A unicidade de `TOKEN_HASH` já indexa a busca por token. Nenhuma coluna guarda o token em claro nem a senha.

**Sessão válida** = `ENCERRADA_EM IS NULL` **e** `EXPIRA_EM > agora (UTC)` **e** conta com `ATIVO = 1`. A comparação de prazo usa o `Clock` da aplicação (testável).

**Cardinalidade**: Usuário 1—N Sessão (várias sessões simultâneas por conta; encerrar uma não afeta as outras). Sem relação com solicitações ou documentos.

**Impacto nos scripts da RC-03**: `01`–`04` não mudam (`04` conta 77 linhas das tabelas originais, e a carga não cria sessões). O `90_limpeza_DESTRUTIVA.sql` passa a citar `RC_SESSAO_LOGIN` como primeira tabela a remover, mantendo a guarda de confirmação.

## Tipos de domínio em Java (não persistidos como entidade pública)

| Tipo | Papel |
|---|---|
| `Usuario` | registro interno: id, email, nome, senhaHash, perfil, ativo (nunca serializado) |
| `Sessao` | id, usuarioId, criadaEm, expiraEm, encerradaEm |
| `SessaoAutenticada` | resultado da validação: sessaoId, usuarioId, nome, perfil, expiraEm; atributo da requisição |
| DTOs `LoginRequest`, `LoginResponse`, `SessaoResponse` | contrato público; nunca contêm hash nem id de sessão |

## Ciclo de vida da sessão

```text
(sem sessão) --login ok--> ABERTA --logout--> ENCERRADA
                              \--prazo vence--> EXPIRADA   (sem escrita: derivada de EXPIRA_EM)
```
EXPIRADA é derivada pelo relógio, não gravada. Token de sessão ENCERRADA ou EXPIRADA recebe 401.
