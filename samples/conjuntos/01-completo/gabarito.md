# Conjunto 01 — completo

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Caminho feliz: encaminhamento e identificacao completos de adulta; os arquivos oficiais da RC-10.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento-sintetico.pdf` | PDF | `a2954d2c8797c39ea26fcfc8cd7477bc49284cd0b8e93e8e9c04713835b8e160` | enviar |
| `identificacao-sintetica.png` | PNG | `001117f663acd6b8fd2e6f2b03bcfc4456ca75619f60b9877fb1762d319b90da` | enviar |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento-sintetico.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento-sintetico.pdf` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `encaminhamento-sintetico.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `encaminhamento-sintetico.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento-sintetico.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento-sintetico.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |
| `identificacao-sintetica.png` | `TIPO_DOCUMENTO` | IDENTIFICADO | IDENTIFICACAO | "DOCUMENTO DE IDENTIFICACAO (SINTETICO)" |
| `identificacao-sintetica.png` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | "Nome: Maria Exemplo da Silva" |
| `identificacao-sintetica.png` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | "Nascimento: 12/04/1990" |
| `identificacao-sintetica.png` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `identificacao-sintetica.png` | `DATA_EMISSAO` | NAO_LOCALIZADO | - | - |
| `identificacao-sintetica.png` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

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

Sem pendencias identificadas nao e aprovacao do SUS. RC-R06 nao se aplica: adulta.
