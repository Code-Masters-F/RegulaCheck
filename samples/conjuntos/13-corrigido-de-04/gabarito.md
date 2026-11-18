# Conjunto 13 — corrigido-de-04

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Reenvio parcial do conjunto 04: o encaminhamento e substituido por um completo e valido; a identificacao continua faltando.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17 e **revisto em 2026-11-18, depois da primeira execução**, que recusou o acréscimo de documento (409 `CONFLITO_ESTADO`); a versão original está no commit `3fefe2e`. Motivo e decisão em `specs/020-conjunto-sintetico-demonstracao/research.md` (D9)
- **Original**: conjunto 04 (versão corrigida; o gabarito do original não muda)
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `fbdae6e8cb3338d07b3a310c6313a2d397b5e46bc973ed1f2cf29b809607c8f8` | substituir o documento 1 do conjunto 04 |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | ATENDE | - |
| RC-R02 | NAO_VERIFICAVEL | - |
| RC-R03 | PENDENTE | PACIENTE |
| RC-R04 | ATENDE | - |
| RC-R05 | ATENDE | - |
| RC-R06 | NAO_VERIFICAVEL | - |

## Resultado geral esperado

COM_PENDENCIAS

## Observação

As pendencias RC-R04 e RC-R05 do conjunto 04 ficam resolvidas; RC-R03 continua pendente, entao o corrigido nao e sem pendencias. So se substitui documento: depois da primeira analise o pedido nao aceita documento novo (409 CONFLITO_ESTADO), achado registrado em specs/020-conjunto-sintetico-demonstracao/research.md (D9). O gabarito do conjunto 04 nao muda.
