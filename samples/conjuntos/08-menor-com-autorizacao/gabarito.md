# Conjunto 08 — menor-com-autorizacao

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Regra condicional aplicavel e atendida: paciente menor de 18 anos com autorizacao assinada.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `8d355ba1c3b5bd49a9ef7e759e1023b91b64e1a5bcf9bf0ad62897a71c3aa736` | enviar |
| `identificacao.png` | PNG | `619e28e0a467c634b048a6199841c0751ac5ef7a68622e87534bbe5d6b05f4d6` | enviar |
| `autorizacao.pdf` | PDF | `7d2ed3538545e4dc16051b4d019f72f050cb191280af964fc71bfcecaf81ebca` | enviar |

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

Lia tem 14 anos em 2026-11-12. O nome do responsavel nao entra em NOME, para nao gerar divergencia em RC-R02.
