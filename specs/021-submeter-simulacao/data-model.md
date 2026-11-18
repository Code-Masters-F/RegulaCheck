# Data Model: Submeter solicitação em simulação (RC-17)

**Sem mudança de esquema.** `RC_SUBMISSAO` existe desde a RC-03 (`01_criar_esquema.sql`) e a carga (`02_`) já tem uma linha (`SIM-2026-0001`).

## RC_SUBMISSAO (existente)

| Coluna | Tipo | Uso na RC-17 |
|---|---|---|
| `ID` | identidade | id técnico da submissão |
| `SOLICITACAO_ID` | FK `RC_SOLICITACAO`, **único** (`RC_SUBMISSAO_UK_SOLIC`) | no máximo uma submissão por pedido (idempotência) |
| `ANALISE_ID` | FK `RC_ANALISE` | a análise vigente usada na conclusão |
| `REFERENCIA` | `VARCHAR2(40)`, **única** (`RC_SUBMISSAO_UK_REF`) | `SIM-{AAAA}-{id do pedido, ≥ 6 dígitos}` |
| `SIMULADA` | `1` (`CHECK`) | sempre simulada |
| `SUBMETIDA_EM` | `TIMESTAMP WITH TIME ZONE` (UTC) | instante da conclusão, gravado pelo relógio da aplicação |

## Submissão (entidade de leitura)

`Submissao(id, solicitacaoId, analiseId, referencia, submetidaEm, revisaoAnalisada, protocoloIdentificador, protocoloVersao, localidade, especialidade, avisoFicticio)` — a linha de `RC_SUBMISSAO` com a revisão e o protocolo da análise usada. Nunca é alterada; não é serializada diretamente (usar `SubmissaoResponse`).

## Desfecho da conclusão

`ResultadoSubmissao(situacao, submissao)`:

| Situação | Condição (sob o bloqueio do pedido, nesta ordem) | Resposta |
|---|---|---|
| `NAO_ENCONTRADO` | pedido inexistente ou de outra paciente | 404 |
| `JA_SUBMETIDA` | já existe linha em `RC_SUBMISSAO` para o pedido | 200 com o comprovante existente |
| `SOMENTE_CONSULTA` | estado `SUBMETIDA_SIMULACAO` sem linha (dado montado fora do fluxo) | 409 |
| `EM_ANDAMENTO` | estado `PROCESSANDO` | 409 |
| `SEM_ANALISE_VIGENTE` | nenhuma análise `VIGENTE = 1` da `REVISAO_ATUAL` | 409 |
| `COM_PENDENCIAS` | análise vigente com resultado geral diferente de `SEM_PENDENCIAS_IDENTIFICADAS` ou com algum resultado `PENDENTE`/`NAO_VERIFICAVEL` | 409 |
| `SEM_ANALISE_VIGENTE` | análise vigente sem pendências, mas estado do pedido diferente de `SEM_PENDENCIAS_IDENTIFICADAS` (dado incoerente) | 409 |
| `SUBMETIDA` | todas as condições atendidas: `INSERT` + estado `SUBMETIDA_SIMULACAO` na mesma transação | 201 com o comprovante |

## Transição de estado

| De | Evento | Para |
|---|---|---|
| `SEM_PENDENCIAS_IDENTIFICADAS` (com análise vigente sem pendências da revisão atual) | concluir a simulação | `SUBMETIDA_SIMULACAO` (final, só consulta) |
| qualquer outro | concluir a simulação | sem mudança (409) |
| `SUBMETIDA_SIMULACAO` | concluir de novo | sem mudança (200, mesma submissão) |

## Comprovante (DTO)

| Campo | Origem |
|---|---|
| `solicitacaoId`, `referencia`, `submetidaEm` | `RC_SUBMISSAO` |
| `servico { localidade, especialidade, avisoFicticio }` | `RC_PROTOCOLO` da análise |
| `versaoAnalisada { analiseId, revisao, protocolo { identificador, versao } }` | `RC_ANALISE` + `RC_PROTOCOLO` |
| `simulada` | sempre `true` |
| `destaque` | fixo: "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" |
| `nota` | fixa: "Este comprovante é de demonstração. Não é documento clínico, não é protocolo oficial e não coloca você em fila real." |
