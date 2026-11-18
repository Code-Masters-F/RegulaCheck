# Conjunto 15 — nada-conferido

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Mesmos arquivos do conjunto 01, sem nenhuma conferencia: nenhum campo vale como evidencia.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Arquivos**: os mesmos do conjunto 01
- **Conferência pressuposta**: NADA_CONFERIDO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento-sintetico.pdf` | PDF | `a2954d2c8797c39ea26fcfc8cd7477bc49284cd0b8e93e8e9c04713835b8e160` | enviar |
| `identificacao-sintetica.png` | PNG | `001117f663acd6b8fd2e6f2b03bcfc4456ca75619f60b9877fb1762d319b90da` | enviar |

## Campos esperados

Os mesmos do conjunto 01, mas **nenhum foi confirmado**: sem confirmação, nenhum campo vale como evidência.

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | NAO_VERIFICAVEL | - |
| RC-R02 | NAO_VERIFICAVEL | - |
| RC-R03 | NAO_VERIFICAVEL | - |
| RC-R04 | NAO_VERIFICAVEL | - |
| RC-R05 | NAO_VERIFICAVEL | - |
| RC-R06 | NAO_VERIFICAVEL | - |

## Resultado geral esperado

REQUER_CONFERENCIA

## Observação

Sem confirmacao da paciente, as regras dependentes ficam nao verificaveis (spec 013).
