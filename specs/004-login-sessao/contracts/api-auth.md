# Contrato da API de autenticação (RC-04)

JSON em UTF-8, campos em camelCase, instantes em ISO-8601 UTC. Erros no formato padrão de [`docs/api/erros.md`](../../../docs/api/erros.md). Todos os exemplos são fictícios.

## POST /auth/login
Autentica uma conta e abre uma sessão. Rota pública.

**Requisição** (`Content-Type: application/json`, corpo até 4 KB):
```json
{ "email": "paciente.a@exemplo.invalid", "senha": "<senha>" }
```
- `email`: obrigatório, até 254 caracteres; espaços nas pontas e maiúsculas são ignorados.
- `senha`: obrigatória, até 72 bytes UTF-8; não é alterada (sem `trim`).

**200 OK**
```json
{
  "token": "<43 caracteres, base64url>",
  "tipo": "Bearer",
  "expiraEm": "2026-11-12T22:30:00Z",
  "usuario": { "nome": "Paciente A (fictícia)", "perfil": "PACIENTE" }
}
```
O token só aparece aqui, uma vez. Nunca há hash de senha nem id interno na resposta.

| HTTP | Código | Quando |
|---|---|---|
| 400 | `REQUISICAO_INVALIDA` | JSON ilegível, `email` ou `senha` ausentes/vazios, tamanho excedido; `detalhes` lista os campos; nenhuma sessão aberta |
| 401 | `CREDENCIAIS_INVALIDAS` | senha errada, e-mail inexistente, conta desativada ou senha acima de 72 bytes — **sempre a mesma resposta** (código, mensagem e corpo iguais) |
| 415 | `TIPO_NAO_SUPORTADO` | `Content-Type` diferente de `application/json` |
| 405 | `METODO_NAO_PERMITIDO` | método diferente de POST |
| 503 | `DEPENDENCIA_INDISPONIVEL` | banco não configurado ou inacessível; nenhuma sessão aberta |

Mensagem de 401: "E-mail ou senha inválidos."

## GET /auth/sessao
Mostra a identidade da sessão atual. Cabeçalho `Authorization: Bearer <token>`.

**200 OK**
```json
{ "usuario": { "nome": "Paciente A (fictícia)", "perfil": "PACIENTE" }, "expiraEm": "2026-11-12T22:30:00Z" }
```
| HTTP | Código | Quando |
|---|---|---|
| 401 | `NAO_AUTENTICADO` | token ausente, malformado, desconhecido, expirado ou encerrado, ou conta desativada; **mesma resposta** em todos os casos. Mensagem: "Sessão ausente ou expirada. Entre novamente." |
| 503 | `DEPENDENCIA_INDISPONIVEL` | banco indisponível |

## POST /auth/logout
Encerra a sessão do token enviado, sem corpo. Cabeçalho `Authorization: Bearer <token>`.

**204 No Content** — o token deixa de valer imediatamente. As outras sessões da mesma conta continuam válidas.
Respostas de erro: iguais às de `GET /auth/sessao` (inclusive 401 ao repetir o logout com o mesmo token).

## Regras transversais
- **Identidade**: sempre pela sessão; nenhuma rota aceita `usuarioId`/dono no corpo.
- **Filtro reutilizável**: as próximas histórias protegem suas rotas com `AutenticacaoFiltro`; perfis e isolamento por paciente são da RC-05.
- **Segredos**: senha, hash e token nunca aparecem em logs nem em respostas de erro.
- **Sem proteção contra força bruta** nesta etapa (limitação conhecida).
- **Prazo**: padrão 30 minutos fixos (`SESSION_TTL_MINUTES`); `expiraEm` informa o instante.

## Comportamento esperado do aplicativo (a tela é de outra história)
1. Ao tocar em *Entrar*, desabilitar o botão e mostrar progresso até a resposta; ignorar toques repetidos enquanto houver envio em andamento.
2. Em 401 `CREDENCIAIS_INVALIDAS`, mostrar a mensagem recebida, manter o e-mail digitado, limpar a senha e reabilitar o botão.
3. Em 503, mostrar "serviço indisponível, tente novamente" sem abrir sessão; em falha de rede, o mesmo, preservando o e-mail.
4. Guardar o token só no armazenamento seguro da plataforma Android (nunca em log, arquivo público ou texto comum) e enviá-lo em `Authorization: Bearer`.
5. Em qualquer 401 `NAO_AUTENTICADO`, apagar o token e voltar à tela de login pedindo novo acesso.
6. Ao sair, chamar `POST /auth/logout` e apagar o token local, mesmo que a chamada falhe por rede.
