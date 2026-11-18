# Conjunto 09 — menor-sem-autorizacao

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Regra condicional aplicavel e pendente: menor de 18 anos sem autorizacao do responsavel.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `ed9794843d88f5a65ba5e1f6c0b3862bb810db3f99fd0e21a97d72da680a9119` | enviar |
| `identificacao.png` | PNG | `532c20259ce683ee3d45202b3a7443981db758bd23cf2bb3b7a9bceab751bad2` | enviar |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Lia Exemplo Souza | pág. 1: "Paciente: Lia Exemplo Souza" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 2012-06-20 | pág. 1: "Nascimento: 20/06/2012" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `TIPO_DOCUMENTO` | IDENTIFICADO | IDENTIFICACAO | "DOCUMENTO DE IDENTIFICACAO (SINTETICO)" |
| `identificacao.png` | `NOME` | IDENTIFICADO | Lia Exemplo Souza | "Nome: Lia Exemplo Souza" |
| `identificacao.png` | `DATA_NASCIMENTO` | IDENTIFICADO | 2012-06-20 | "Nascimento: 20/06/2012" |
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
| RC-R06 | PENDENTE | PACIENTE |

## Resultado geral esperado

COM_PENDENCIAS

## Observação

Lia tem 14 anos em 2026-11-12.
