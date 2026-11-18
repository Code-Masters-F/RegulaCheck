# Contrato consumido — RC-18

Fonte: `docs/api/openapi.yaml`, `docs/api/erros.md`, `specs/015-fluxo-telas-app/contracts/telas-operacoes.md` (E1.1–E1.3). O app não cria rota nem altera o backend.

| Tela · ação | Rota | Sucesso | Tratamento no app |
|---|---|---|---|
| E1.1 Entrar | `POST /auth/login` (`{email, senha}`) | 200 `PACIENTE` → sessão + E1.2 | 200 de outro perfil → `POST /auth/logout` com esse token (falha ignorada), nada guardado, "Esta conta não é de paciente. Entre com uma conta de paciente." · 401 → "E-mail ou senha incorretos." (e-mail mantido, senha limpa) · 400 → mensagem do servidor · 503/5xx → serviço indisponível · rede/tempo → sem conexão |
| E1.2 Abrir | `GET /solicitacoes` | lista ou vazia | 5xx/inesperada → Erro · rede/tempo → Sem conexão · 403 → sem permissão |
| E1.2 Sair | `POST /auth/logout` | 204 | qualquer resposta ou falha → sessão local apagada → E1.1 |
| E1.3 Abrir/Atualizar | `GET /solicitacoes/{id}` | dados + ação | 404 → "Pedido não encontrado." + "Voltar para meus pedidos" · 403 → sem permissão + "Voltar" · 5xx/rede → erro com "Tentar de novo" |

**Comum a toda rota privada** (exceto logout): 401 → sessão apagada, login com o aviso de E1.4.

**Cabeçalhos enviados**: `Authorization: Bearer <token>` nas rotas privadas; `Accept: application/json`. Nada de identificador da paciente no corpo.

**Mapeamento HTTP → `ErroApi`** (testado com MockWebServer): 400/422 `RequisicaoInvalida` · 401 login `CredenciaisInvalidas` · 401 outra rota `SessaoExpirada` · 403 `SemPermissao` · 404 `NaoEncontrado` · 409 `Conflito` (+`Retry-After` em segundos) · 413/415 `ArquivoRecusado` · 5xx `Indisponivel` · outro/JSON inválido `RespostaInesperada` · `SocketTimeoutException` `TempoEsgotado` · outra `IOException` `SemConexao`.
