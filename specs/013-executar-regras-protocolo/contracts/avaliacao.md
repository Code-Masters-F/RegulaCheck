# Contrato interno: avaliação dos critérios (RC-12 → RC-13/RC-14)

Esta história **não expõe rota HTTP** (decisão do responsável, 13/11/2026). O contrato abaixo é a interface Java que a RC-13 (análises) e a RC-14 (resumo simulado) consomem. Não há JSON público nem OpenAPI novos.

## `AvaliacaoService.avaliar(SessaoAutenticada sessao, long solicitacaoId) → AvaliacaoDoPedido`

Somente leitura. Não grava, não muda estado nem revisão, não chama IA.

| Situação | Resultado |
|---|---|
| pedido da paciente com leitura atual e documentos coerentes | `AvaliacaoDoPedido` |
| pedido inexistente ou de outra paciente | `ExcecaoNegocio` 404 `NAO_ENCONTRADO` (idêntica, via `Propriedade`) |
| pedido sem leitura | `ExcecaoNegocio` 409 `CONFLITO_ESTADO` — "Leia os documentos antes de conferir os critérios." |
| leitura desatualizada, ou retrato mudou durante a montagem | `ExcecaoNegocio` 409 `CONFLITO_ESTADO` — "Os documentos mudaram; leia os documentos de novo." |
| versão do protocolo sem regras, com regra sem implementação, natureza divergente ou código repetido | `ProtocoloNaoSuportadoException` (sem resultado parcial; a RC-13 a trata como falha de análise) |
| banco indisponível | `ExcecaoNegocio` 503 `DEPENDENCIA_INDISPONIVEL` (dos repositórios) |

Ordem das verificações: dona (404) → leitura (409) → regras (falha de protocolo) → avaliação.

## `AvaliacaoDoPedido`

| Campo | Significado |
|---|---|
| `solicitacaoId`, `revisao` | pedido e revisão avaliada (= revisão da leitura) |
| `protocolo` | versão aplicada (`identificador`, `versao`, `protocoloId()`) |
| `extracaoId` | leitura usada |
| `conferenciaAtualizadaEm` | última decisão de conferência ou, sem decisão, o instante da leitura (mesma regra de `ConferenciaService.visao`) |
| `dataReferencia` | data (Salvador) do envio mais recente entre os documentos avaliados |
| `avaliacao.resultados` | uma entrada por regra da versão, na ordem `ORDEM`: regra + resultado + motivo (só em não verificável) + evidências |
| `avaliacao.pendencias` | uma por regra `PENDENTE`/`NAO_VERIFICAVEL`: código, descrição, tipo, explicação, motivo, evidências, responsável |
| `avaliacao.resultadoGeral` | `COM_PENDENCIAS` › `REQUER_CONFERENCIA` › `SEM_PENDENCIAS_IDENTIFICADAS` |
| `leituraSimulada` | sempre `true` (leitura simulada; protocolo acadêmico fictício) |

## Para a RC-13

- Gravar `RC_ANALISE (SOLICITACAO_ID, REVISAO, PROTOCOLO_ID, RESULTADO_GERAL)`, `RC_RESULTADO_REGRA` (por `RC_REGRA.ID`; o motor devolve o código — a RC-13 resolve o id na mesma versão) e `RC_PENDENCIA (TIPO, MENSAGEM = explicacao, RESPONSAVEL)`. Motivo e evidências não têm coluna hoje: a RC-13 decide se os grava (coluna/tabela aditiva) ou se os recalcula a partir da leitura da revisão.
- Publicar como vigente só se `revisao` e `conferenciaAtualizadaEm` ainda forem os atuais **sob o bloqueio do pedido** (princípio VI; compensação registrada na RC-11).
- Ligar `AvaliacaoService` em `config/Composicao` junto com a rota.
