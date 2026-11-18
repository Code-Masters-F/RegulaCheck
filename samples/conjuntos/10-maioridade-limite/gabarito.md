# Conjunto 10 — maioridade-limite

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Regra condicional nao aplicavel na fronteira: a paciente completa 18 anos na data de referencia.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `d9d5d12eb6eabd6d92c6578e541fe6af65b75a2228917f0e891b763762216dda` | enviar |
| `identificacao.png` | PNG | `1cd53cb6e4c3a5645f7dcf0757a0989542ab3c67c1ae4a7f822b251b9107f19b` | enviar |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Davi Exemplo Costa | pág. 1: "Paciente: Davi Exemplo Costa" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 2008-11-12 | pág. 1: "Nascimento: 12/11/2008" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |
| `identificacao.png` | `TIPO_DOCUMENTO` | IDENTIFICADO | IDENTIFICACAO | "DOCUMENTO DE IDENTIFICACAO (SINTETICO)" |
| `identificacao.png` | `NOME` | IDENTIFICADO | Davi Exemplo Costa | "Nome: Davi Exemplo Costa" |
| `identificacao.png` | `DATA_NASCIMENTO` | IDENTIFICADO | 2008-11-12 | "Nascimento: 12/11/2008" |
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

Nascimento em 2008-11-12: 18 anos exatos em 2026-11-12, entao RC-R06 nao se aplica. A fronteira so e exercitada na regressao (data fixa); em execucao manual vale o dia do envio.
