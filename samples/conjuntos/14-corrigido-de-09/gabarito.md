# Conjunto 14 — corrigido-de-09

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Reenvio do conjunto 09: o encaminhamento e substituido por uma versao com a autorizacao assinada do responsavel.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17 e **revisto em 2026-11-18, depois da primeira execução**, que recusou o acréscimo de documento (409 `CONFLITO_ESTADO`); a versão original está no commit `3fefe2e`. Motivo e decisão em `specs/020-conjunto-sintetico-demonstracao/research.md` (D9)
- **Original**: conjunto 09 (versão corrigida; o gabarito do original não muda)
- **Conferência pressuposta**: TUDO_CONFIRMADO

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `encaminhamento.pdf` | PDF | `b596d6768aa0bd8a89c22b88a8f065099fe3f80aca6b4ae3f43c0ab2f7080f76` | substituir o documento 1 do conjunto 09 |

## Campos esperados

| Arquivo | Campo | Situação | Valor | Origem |
|---|---|---|---|---|
| `encaminhamento.pdf` | `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `encaminhamento.pdf` | `NOME` | IDENTIFICADO | Lia Exemplo Souza | pág. 1: "Paciente: Lia Exemplo Souza" |
| `encaminhamento.pdf` | `DATA_NASCIMENTO` | IDENTIFICADO | 2012-06-20 | pág. 1: "Nascimento: 20/06/2012" |
| `encaminhamento.pdf` | `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `encaminhamento.pdf` | `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `encaminhamento.pdf` | `AUTORIZACAO_ASSINADA` | IDENTIFICADO | SIM | pág. 1: "Autorizacao assinada: SIM" |

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

A autorizacao vem no proprio encaminhamento reemitido, porque nao se acrescenta documento depois da analise (D9). O gabarito do conjunto 09 nao muda.
