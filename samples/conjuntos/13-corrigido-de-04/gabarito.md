# Conjunto 13 — corrigido-de-04

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Reenvio do conjunto 04: encaminhamento completo e dentro da validade substitui o anterior e a identificacao e acrescentada.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: conjunto 04 (versão corrigida; o gabarito do original não muda)
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `fbdae6e8cb3338d07b3a310c6313a2d397b5e46bc973ed1f2cf29b809607c8f8` | substituir o documento 1 do conjunto 04 |
| `identificacao.png` | PNG | `5ea4c95b8628caaf0abf6633c5208d259c4574eeb72afd34bbc313178d52f6b4` | acrescentar ao pedido do conjunto 04 |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `TIPO_DOCUMENTO` | IDENTIFICADO | IDENTIFICACAO | "DOCUMENTO DE IDENTIFICACAO (SINTETICO)" |
| `identificacao.png` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | "Nome: Maria Exemplo da Silva" |
| `identificacao.png` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | "Nascimento: 12/04/1990" |
| `identificacao.png` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `DATA_EMISSAO` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | ATENDE | - |
| RC-R02 | ATENDE | - |
| RC-R03 | ATENDE | - |
| RC-R04 | ATENDE | - |
| RC-R05 | ATENDE | - |
| RC-R06 | NAO_APLICAVEL | - |

## Resultado geral esperado

SEM_PENDENCIAS_IDENTIFICADAS

## Observação

Nova revisao: a analise do conjunto 04 deixa de ser vigente e continua no historico. O gabarito do conjunto 04 nao muda.
