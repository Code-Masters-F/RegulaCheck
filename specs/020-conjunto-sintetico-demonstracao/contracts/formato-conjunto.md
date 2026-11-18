# Contrato: formato do conjunto, do gabarito e do catálogo (RC-23)

Não há endpoint novo. Este contrato fixa o formato do material, para que qualquer pessoa o leia e o teste o verifique.

## Pasta do conjunto

`samples/conjuntos/<NN>-<nome>/` contendo os arquivos do conjunto e `gabarito.md`. Nome em minúsculas, sem acentos.

## `gabarito.md`

```markdown
# Conjunto NN — <nome>

> Material 100% fictício e sintético, sem valor real. Leitura SIMULADA. Resultado esperado não é aprovação do SUS.

- **Objetivo**: <frase>
- **Data de referência**: 2026-11-12
- **Original**: <id ou "-">  (somente em versões corrigidas; descreve o que mudou)
- **Conferência pressuposta**: <TUDO_CONFIRMADO | NADA_CONFERIDO | DIVERGENCIA(<campo>)>
- **Arquivos**: <nome> — SHA-256 `<hash>`

## Campos esperados
| Arquivo | Campo | Situação | Valor | Origem |

## Regras esperadas
| Regra | Resultado | Responsável (se pendente) |
| RC-R01 … RC-R06 |

## Resultado geral esperado
<COM_PENDENCIAS | REQUER_CONFERENCIA | SEM_PENDENCIAS_IDENTIFICADAS>

## Observação
<fronteira, ambiguidade ou ressalva>
```

Para formato inválido, no lugar de campos e regras: **Recusa esperada** (motivo e momento: no envio, sem leitura e sem análise).

## `samples/catalogo-conjuntos.md`

Tabela com: `id`, situação coberta, tipos de arquivo, resultado geral esperado, `original` (quando houver) e o commit em que o gabarito foi registrado.

## Catálogo de leituras

`backend/src/main/resources/extracao-simulada/catalogo.json`: uma entrada por arquivo novo (`descricao`, `hash`, `situacao`, `campos[]`), no mesmo formato já usado pela RC-10. Arquivo desconhecido continua recebendo `LEITURA_VAZIA`.

## Garantias verificadas por teste

1. Existem ≥ 12 conjuntos e as 11 situações da spec (FR-002).
2. Cada arquivo tem a marca "SEM VALOR REAL" legível (PDF: texto extraído; PNG: presente nas linhas de definição).
3. O hash de cada arquivo confere com o `gabarito.md` e, para os conjuntos com leitura (todos menos o 12, de formato inválido, recusado no envio), também com o `catalogo.json`.
4. Cada conjunto corrigido referencia um original existente.
5. Executado pelo percurso real em memória, cada conjunto coincide com seu gabarito (regras, campos, resultado geral) ou a divergência é registrada como achado.
