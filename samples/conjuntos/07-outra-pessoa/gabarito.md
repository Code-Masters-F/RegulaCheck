# Conjunto 07 — outra-pessoa

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Documento de outra pessoa: a identificacao enviada tem nome e nascimento diferentes (RC-R02).
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `8019483fe27363947bbff945dbad99ca4a5667c2d19463ba6b0a8cd4ec507f40` | enviar |
| `identificacao.png` | PNG | `abd987cd48101ff89b964dc8fe3e1443d3a04aae51e72486c818084c5061af5d` | enviar |

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
| `identificacao.png` | `NOME` | IDENTIFICADO | Carlos Exemplo Lima | "Nome: Carlos Exemplo Lima" |
| `identificacao.png` | `DATA_NASCIMENTO` | IDENTIFICADO | 1985-01-30 | "Nascimento: 30/01/1985" |
| `identificacao.png` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `DATA_EMISSAO` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## Regras esperadas

| Regra | Resultado | Responsável (se pendente) |
|---|---|---|
| RC-R01 | ATENDE | - |
| RC-R02 | PENDENTE | PACIENTE |
| RC-R03 | ATENDE | - |
| RC-R04 | ATENDE | - |
| RC-R05 | ATENDE | - |
| RC-R06 | NAO_APLICAVEL | - |

## Resultado geral esperado

COM_PENDENCIAS

## Observação

RC-R06 usa so a data da identificacao enviada (1985-01-30): adulta, nao se aplica.
