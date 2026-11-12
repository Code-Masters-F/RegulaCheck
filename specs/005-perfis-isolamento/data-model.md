# Data Model: Perfis e isolamento de dados (RC-05)

**Sem tabelas, colunas ou scripts SQL novos.** O banco da RC-03/RC-04 já tem tudo de que a autorização precisa; esta história só define como lê-lo.

## Perfis (domínio)

| Perfil | Origem | Quem é | Rotas |
|---|---|---|---|
| `PACIENTE` | `RC_USUARIO.PERFIL` | pessoa, usuária principal | seus próprios recursos |
| `ADMINISTRADOR` | `RC_USUARIO.PERFIL` | pessoa | protocolo e indicadores agregados; nenhum recurso individual |
| simulador | chave de configuração `IOT_API_KEY` (não é conta nem perfil do banco) | script de sensores | somente telemetria |

O enum `Perfil` continua igual ao CHECK `RC_USUARIO_CK_PERFIL`; o simulador **não** entra nele.

## Cadeia de dona dos recursos (já existente na RC-03)

```text
RC_USUARIO (PERFIL = 'PACIENTE')
   └─ RC_SOLICITACAO.PACIENTE_ID  ← dona de tudo abaixo
        ├─ RC_DOCUMENTO.SOLICITACAO_ID
        ├─ RC_ANALISE.SOLICITACAO_ID      (resumo é colunas de RC_ANALISE)
        ├─ RC_SUBMISSAO.SOLICITACAO_ID
        └─ RC_SESSAO_CAPTURA.SOLICITACAO_ID
              └─ RC_LEITURA_SENSOR.SESSAO_ID
```

Regra de consulta: toda busca de recurso individual junta até `RC_SOLICITACAO` e filtra `PACIENTE_ID = <conta da sessão>` **na mesma instrução SQL** (parametrizada). Resultado vazio significa "não encontrado" para inexistente e alheio.

## Tipos de código (sem persistência)

| Tipo | Papel |
|---|---|
| `RotasPrivadas` | registro de `(caminho, perfis permitidos)`; instala os filtros; `exigirTudoDeclarado()` falha a montagem se houver servlet fora do registro |
| `AutorizacaoFiltro` | compara `SessaoAutenticada.perfil()` com os perfis da rota; recusa com 403 |
| `Propriedade` | `exigir(Optional<T>, SessaoAutenticada)` → recurso ou o 404 padrão idêntico (só o id técnico da conta vai ao log) |
| `SimuladorFiltro` | valida `X-Chave-Simulador` (tempo constante); só na telemetria |
| `Erros` | `acessoNegado()` (403 `ACESSO_NEGADO`) e `naoEncontrado()` (404 `NAO_ENCONTRADO`) com mensagens fixas |

## Mensagens fixas (não variam por motivo)

| Código | Mensagem |
|---|---|
| `ACESSO_NEGADO` (403) | "Voce nao tem permissao para esta operacao." |
| `NAO_ENCONTRADO` (404) | "Recurso nao encontrado." |

Os textos não citam nome de dona, identificador interno, perfil exigido nem o nome do recurso.

## Configuração

| Variável | Uso | Obrigatória para iniciar? |
|---|---|---|
| `IOT_API_KEY` | chave do simulador (comparada em tempo constante) | não; sem ela a telemetria responde 401 |
