# Data model — RC-18 (app)

O app não tem banco. Modelos de contrato (`@Serializable`, `ignoreUnknownKeys`, `explicitNulls=false`) espelham `docs/api/openapi.yaml`; campos não exibidos são opcionais para não derrubar a tela.

| Modelo | Campos usados | Origem |
|---|---|---|
| `LoginRequisicao` | `email`, `senha` (`toString` não expõe a senha) | corpo de `POST /auth/login` |
| `LoginResposta` | `token` (`toString` oculta), `expiraEm`, `usuario{nome, perfil}` | 200 do login |
| `ListaSolicitacoes` | `itens[]`, `limite` | `GET /solicitacoes` |
| `Solicitacao` | `id`, `servico{especialidade, localidade, avisoFicticio}`, `estado` (texto livre), `revisao`, `criadaEm`, `atualizadaEm`, `documentosDisponiveis`, `analise{situacao, mensagem, resultadoGeral?}` | lista e `GET /solicitacoes/{id}` |
| `ErroCorpo` | `codigo`, `mensagem`, `detalhes[]`, `idRastreamento?` | corpo de erro (`docs/api/erros.md`) |

## Estado local

- **SessaoLocal** `{token, nome, perfil, expiraEm}` — existe entre login e logout/401/expiração; cifrada (research D4).
- **EstadoSessao**: `Ativa(sessao)` · `Encerrada(motivo)` com motivo `NENHUMA` (abriu sem sessão), `SAIU`, `EXPIROU`.
- **EstadoTela<T>**: `Carregando` · `Vazio` · `Sucesso(dados)` · `Falha(erro: ErroApi)`.
- **Tela**: `Login(sessaoExpirada: Boolean)` · `MeusPedidos` · `Pedido(id)`.

## Estado do pedido → interface

`estado` é lido como texto e convertido em `EstadoPedido`; valor fora da lista vira `DESCONHECIDO`.

| `estado` | Rótulo · ícone | Ação principal |
|---|---|---|
| `RASCUNHO` (0 documentos) | Rascunho · lápis | Anexar documentos (RC-19) |
| `RASCUNHO` (≥1 documento) | Rascunho · lápis | Ler documentos (RC-19) |
| `PROCESSANDO` | Em verificação · relógio | Atualizar (funciona) |
| `REQUER_CONFERENCIA` | Confira a leitura · olho | Ver o que conferir (RC-19) |
| `COM_PENDENCIAS` | Com pendências · alerta | Ver pendências (RC-19) |
| `SEM_PENDENCIAS_IDENTIFICADAS` | Sem pendências identificadas · check | Ver resultado (RC-19) |
| `FALHA_ANALISE` | Falha na verificação · X | Tentar de novo (RC-19) |
| `SUBMETIDA_SIMULACAO` | Simulação concluída · documento | — (só consulta) |
| outro | Estado não reconhecido · interrogação | — |

Mensagens de próximo passo: `mobile/design/textos.md` (E1.3). Resultado geral (`analise.resultadoGeral` em `CONCLUIDA`) usa os mesmos rótulos.
