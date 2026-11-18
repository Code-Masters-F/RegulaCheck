# Conjunto 04 — varias-pendencias

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Varias pendencias de uma vez: sem identificacao, sem motivo e emissao fora dos 180 dias.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `f4f79b57822e0ac54330ac6cb2234c90d00c1c995287dcf26f2c75bcf1c79031` | enviar |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-03-10 | pág. 1: "Emissao: 10/03/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | ATENDE | - |
| RC-R02 | NAO_VERIFICAVEL | - |
| RC-R03 | PENDENTE | PACIENTE |
| RC-R04 | PENDENTE | UNIDADE_EMISSORA |
| RC-R05 | PENDENTE | UNIDADE_EMISSORA |
| RC-R06 | NAO_VERIFICAVEL | - |

## Resultado geral esperado

COM_PENDENCIAS

## Observação

Emissao em 2026-03-10: 247 dias ate 2026-11-12, acima dos 180 dias demonstrativos. Todas as regras sao avaliadas; uma pendencia nao encerra a conferencia.
