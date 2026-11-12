# Mobile

**Android nativo com Kotlin** (decidido). **Projeto ainda não criado**: esta pasta só reserva o lugar e documenta como o app alcança a API.

Telas previstas: login, lista de solicitações, rascunho, envio de documentos, conferência da transcrição, resultado da análise, submissão simulada.

Contrato: o app consome somente a API do backend ([contratos](../docs/api/contratos-dados.md), [erros](../docs/api/erros.md), [estados](../docs/api/estados-e-revisoes.md), [rotas](../CONTRATO_API.md)). Nenhuma credencial de banco ou IA no app.

## Endereço da API em desenvolvimento
| Onde o app roda | Endereço |
|---|---|
| Emulador Android | `http://10.0.2.2:8080` |
| Aparelho físico (mesma rede do computador) | `http://<IP-do-computador>:8080` |

`localhost` **não** funciona no emulador nem no aparelho. Detalhes, firewall e a nota sobre HTTP em desenvolvimento: [docs/ambiente.md](../docs/ambiente.md#acesso-do-app-à-api).
