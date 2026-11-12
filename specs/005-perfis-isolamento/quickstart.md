# Quickstart: verificar a autorização e proteger uma rota nova (RC-05)

Pré-requisito: JDK 21+ e `backend/` compilável. **Não precisa de banco** para verificar esta história.

## 1. Verificar

```powershell
cd backend
.\mvnw.cmd clean verify
```

Esperado: todos os testes verdes, incluindo `auth.autorizacao.*` (matriz de perfil × recurso × dona, omissão de rota, simulador, higiene de log). Registre o total real de testes no relatório da história.

## 2. Percurso manual (opcional, API em execução com banco da RC-04)

```powershell
$env:IOT_API_KEY = "chave-ficticia-de-teste"   # fictícia; nunca versionar valor real
java -jar target\regulacheck-backend.jar
```

| Passo | Chamada | Esperado |
|---|---|---|
| 1 | `GET /auth/sessao` sem token | 401 |
| 2 | login de paciente A e `GET /auth/sessao` | 200 |
| 3 | rota administrativa com token de paciente | 403 |
| 4 | `X-Chave-Simulador` válida em `GET /auth/sessao` | 403 |
| 5 | token de paciente em rota de telemetria | 403 |

Os passos 3 e 5 dependem de rotas que só existem nas histórias seguintes; até lá eles são cobertos pelos testes com rotas de exemplo.

## 3. Proteger uma rota nova (guia para as próximas histórias)

1. Declare a rota no registro: `RotasPrivadas.registrar(contexto, "/solicitacoes/*", servlet, Perfil.PACIENTE)` (um ou mais perfis).
2. No serviço, identifique a paciente por `AutenticacaoFiltro.sessaoDe(req).usuarioId()`; nunca pelo corpo.
3. No repositório, escreva a busca **já filtrada pela dona** (junção até `RC_SOLICITACAO.PACIENTE_ID`) e passe o resultado por `Propriedade.exigir(...)`.
4. Copie a linha do seu recurso na matriz de testes (própria, de outra paciente, inexistente × paciente, administrador, sem token).
5. Rode `mvnw clean verify`: o teste de omissão falha se a rota ficou sem declaração.

Resultado esperado para a rota nova: 401 sem token, 403 com perfil indevido, 404 para alheio ou inexistente, sucesso para a dona.
