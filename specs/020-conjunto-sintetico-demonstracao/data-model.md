# Data Model: Conjunto sintético de demonstração (RC-23)

Não há tabela nem mudança de banco. O modelo é de material de teste (arquivos + texto estruturado).

## Conjunto sintético

| Campo | Descrição |
|---|---|
| `id` | Número de dois dígitos e nome curto (`04-varias-pendencias`); único. |
| `objetivo` | Uma frase com a situação coberta. |
| `arquivos[]` | Um ou mais arquivos (PDF textual, PNG ou, no formato inválido, texto/PDF longo). Cada um traz a marca de fictício. |
| `originalDe` | (opcional) `id` do conjunto original quando o conjunto é uma versão corrigida. |
| `dataReferencia` | 2026-11-12 em todos. |
| `gabarito` | Ver abaixo. |

## Gabarito

| Campo | Descrição |
|---|---|
| `conferencia` | Estado da conferência pressuposto: `TUDO_CONFIRMADO` ou `NADA_CONFERIDO` (`DIVERGENCIA(<campo>)` fica reservada, sem uso nesta versão). Só campo identificado e confirmado vale como evidência (spec 013); sem isso as regras dependentes ficam `NAO_VERIFICAVEL`. |
| `regras[RC-R01..RC-R06]` | **Todas as seis** preenchidas. |
| `regras[RC-R01..RC-R06]` (detalhe) | `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL` ou `NAO_APLICAVEL`; para `PENDENTE`, o responsável esperado (`PACIENTE` ou `UNIDADE_EMISSORA`). |
| `campos[arquivo][CAMPO]` | Situação (`IDENTIFICADO`, `NAO_LOCALIZADO`, `ILEGIVEL`), valor e origem (página/trecho) quando houver. Campos: `TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO`, `AUTORIZACAO_ASSINADA`. |
| `resultadoGeral` | `COM_PENDENCIAS`, `REQUER_CONFERENCIA` ou `SEM_PENDENCIAS_IDENTIFICADAS`. |
| `recusaNoEnvio` | Só no formato inválido: motivo e momento; substitui regras e campos. |
| `observacao` | Ambiguidade, fronteira ou ressalva (leitura simulada; não é aprovação do SUS). |

## Relações

- Conjunto corrigido → conjunto original (1:1, por `originalDe`). O gabarito do original não muda.
- Cada arquivo de leitura simulada → uma entrada de `catalogo.json` (chave: SHA-256 do arquivo).

## Regras de validade

- Toda célula do gabarito preenchida (sem campos em branco) para conjuntos com análise.
- Todo arquivo contém o texto visível de material fictício.
- Nomes, datas e números inventados; nenhum dado real.
- Arquivo commitado nunca é regenerado; alterar um arquivo muda o hash e quebra a leitura (detecção por teste).
