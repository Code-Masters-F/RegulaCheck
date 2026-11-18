# Conjunto 03 — sem-motivo

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Encaminhamento nitido sem o campo de motivo: a unidade emissora precisa completar (RC-R04).
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `54420227dea2f1eb732757fdcec55ed6f97edd7315edc5776fbad03bc8802895` | enviar |
| `identificacao.png` | PNG | `6434c2138faf2b2bab8dbe879bb24477ab34a488f9cef6a531b5ded0e096c220` | enviar |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
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
| RC-R04 | PENDENTE | UNIDADE_EMISSORA |
| RC-R05 | ATENDE | - |
| RC-R06 | NAO_APLICAVEL | - |

## Resultado geral esperado

COM_PENDENCIAS

## Observação

A paciente nao corrige o original: a pendencia e da unidade.
