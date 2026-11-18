# Data Model: Plano de testes (RC-24)

Sem tabela nem mudança de banco. O modelo é o da estrutura do documento.

## Cenário (`Txx`)

| Campo | Descrição |
|---|---|
| `id` | `T01`..`T16` (e `P01` para o percurso integrado) |
| `titulo` | Nome do cenário da issue |
| `historias` | Referências `RC-XX` |
| `casos[]` | Um ou mais casos; um por resultado distinto |

## Caso de teste (`Txx-yy`)

| Campo | Regra |
|---|---|
| `id` | Único; `Txx-yy` |
| `titulo` | Um comportamento específico |
| `grupo` | `funcionamento`, `falha` ou `seguranca` |
| `historias` | `RC-XX`; obrigatório |
| `regra` | `RC-R01..RC-R06` com resultado esperado por regra, ou `não se aplica` |
| `preCondicoes` | Contas, permissões, estado do pedido, dados |
| `ambiente` | Versão ou commit, URL da API, dispositivo/emulador; **sem credenciais** |
| `tipoExecucao` | `API real + Oracle`, `API em memória (sem banco)`, `Teste automatizado`, `Dublê (não é integração real)` ou `App no emulador + API real` |
| `entrada` | Payload, arquivo ou gabarito existente em `samples/`, ou leitura de sensor |
| `passos` | Lista numerada |
| `resultadoEsperado` | Resposta (código e erro), comportamento da interface e efeito nos dados; verificável |
| `impedimentoConhecido` | Funcionalidade pendente que pode impedir a execução (ou `nenhum`) |
| `resultadoObtido` | **Em branco** na entrega |
| `status` | `Não executado` na entrega |
| `evidencia` | **Em branco** na entrega; caminho futuro `docs/evidencias/<ID>/` |
| `responsavelEData` | **Em branco** na entrega |

## Status

| Valor | Quando vale |
|---|---|
| Não executado | Estado inicial; também quando há impedimento conhecido que ninguém tentou executar |
| Aprovado | Executado e **todos** os itens do resultado esperado do caso conferem, com evidência |
| Reprovado | Executado e algum item difere; registrar como achado, sem alterar o esperado |
| Bloqueado | Tentou-se executar e um impedimento (ambiente, dependência ou funcionalidade pendente) foi identificado e registrado com motivo |

Transições: `Não executado → Aprovado | Reprovado | Bloqueado`; `Bloqueado/Reprovado → Aprovado | Reprovado` no reteste. Nenhuma transição ocorre nesta feature.

## Matriz de rastreabilidade

- História (`RC-XX`) → casos; regra (`RC-R01..RC-R06`) → casos; conjunto RC-23 → casos.
- Colunas extras: `impedimentoConhecido` e `tipoExecucao`.
- Uma história sem caso aparece como **lacuna** com motivo.

## Evidência (definida aqui, criada na RC-25)

`docs/evidencias/<ID-DO-CASO>/` com, no mínimo: data, versão ou commit, comando ou passos executados, resultado bruto (saída, resposta HTTP ou captura), status e executor. Nada com token, senha ou conteúdo de documento real.
