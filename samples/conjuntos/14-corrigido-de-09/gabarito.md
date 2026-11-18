# Conjunto 14 — corrigido-de-09

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Reenvio do conjunto 09: a autorizacao assinada do responsavel e acrescentada.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: conjunto 09 (versão corrigida; o gabarito do original não muda)
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `autorizacao.pdf` | PDF | `d886cc3a4326b523d925c343c25fa30605c101d895a32a0bdf2394c9faaa1da9` | acrescentar ao pedido do conjunto 09 |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `autorizacao.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | AUTORIZACAO_RESPONSAVEL | pág. 1: "AUTORIZACAO DO RESPONSAVEL LEGAL (SINTETICA)" |
| `autorizacao.pdf` | `NOME` | NAO_LOCALIZADO | - | - |
| `autorizacao.pdf` | `DATA_NASCIMENTO` | NAO_LOCALIZADO | - | - |
| `autorizacao.pdf` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `autorizacao.pdf` | `DATA_EMISSAO` | NAO_LOCALIZADO | - | - |
| `autorizacao.pdf` | `AUTORIZACAO_ASSINADA` | IDENTIFICADO | SIM | pág. 1: "Autorizacao assinada: SIM" |

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | ATENDE | - |
| RC-R02 | ATENDE | - |
| RC-R03 | ATENDE | - |
| RC-R04 | ATENDE | - |
| RC-R05 | ATENDE | - |
| RC-R06 | ATENDE | - |

## Resultado geral esperado

SEM_PENDENCIAS_IDENTIFICADAS

## Observação

Nova revisao: a analise do conjunto 09 deixa de ser vigente e continua no historico. O gabarito do conjunto 09 nao muda.
