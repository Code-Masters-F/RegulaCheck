# Conjunto 12 — formato-invalido

> Material 100% fictício e sintético, sem valor real. A leitura dos arquivos é **SIMULADA** (o leitor
> reconhece o arquivo pelo SHA-256 e devolve a leitura predefinida). O resultado esperado não é
> aprovação do SUS, vaga, prazo nem autorização de atendimento.

- **Objetivo**: Arquivos que a aplicacao deve recusar no envio: formato nao aceito e PDF com mais de 5 paginas.
- **Data de referência**: 2026-11-12 (regressão automática; em execução manual vale o dia do envio)
- **Gabarito registrado em**: 2026-11-17, antes da primeira execução do conjunto
- **Original**: -

## Arquivos

| Arquivo | Formato | SHA-256 | Ação no envio |
|---|---|---|---|
| `documento-sintetico.txt` | TXT | `a573f85be191398e29599825a84852cf4d319558fc470cd49742a5b957f91848` | enviar |
| `pdf-seis-paginas.pdf` | PDF | `241e1367889fa7339445834b13ee0c4651beeb0d1a5e0f4ed0863e23f4d2b1e1` | enviar |

## Recusa esperada

Recusado **no envio**: nada é gravado, não há leitura nem análise e nenhuma regra é avaliada.

| Arquivo | HTTP | Código aceito | Motivo |
|---|---|---|---|
| `documento-sintetico.txt` | 415 | TIPO_NAO_SUPORTADO ou EXTENSAO_INCOMPATIVEL | Conteudo e extensao nao sao PDF, JPEG nem PNG; qual verificacao responde primeiro nao e fixado pela documentacao, entao qualquer dos dois codigos vale. |
| `pdf-seis-paginas.pdf` | 400 | REQUISICAO_INVALIDA | PDF com 6 paginas; o limite do MVP e 5 (docs/api/protecoes.md). |

## Observação

Recusa no envio: nada e gravado, nao ha leitura nem analise, e nenhuma regra e avaliada.
