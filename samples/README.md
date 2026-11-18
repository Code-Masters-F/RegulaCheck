# Amostras sintéticas

Documentos **fictícios**, sem nenhum dado real, usados na demonstração e nos testes. Toda leitura é **simulada**: o leitor não lê o arquivo; reconhece-o pelo SHA-256 e devolve a leitura predefinida em `backend/src/main/resources/extracao-simulada/catalogo.json`. Nada aqui comprova leitura real nem aceitação pela regulação ou pelo SUS.

## Amostras oficiais (RC-10)

| Arquivo | O que é |
|---|---|
| `encaminhamento-sintetico.pdf` | PDF de uma página: encaminhamento completo |
| `identificacao-sintetica.png` | Fotografia (PNG) de um documento de identificação |
| `gabarito.md` | Leitura **simulada** esperada de cada amostra (campos, situações, valores, origens e SHA-256) |

## Conjuntos de demonstração (RC-23)

`conjuntos/<NN>-<nome>/` traz os arquivos de uma situação de teste e o `gabarito.md` com a resposta esperada, **escrita antes da execução**. O índice com a situação e o resultado de cada conjunto está em [`catalogo-conjuntos.md`](catalogo-conjuntos.md).

| Conjunto | Situação |
|---|---|
| 01 | completo (usa as duas amostras oficiais acima) |
| 02 a 04 | requisito ausente, motivo ausente, várias pendências |
| 05, 06 | foto ilegível (borrada) e foto cortada |
| 07 | documento de outra pessoa |
| 08, 09, 10 | regra condicional aplicável atendida, aplicável pendente e não aplicável (18 anos exatos) |
| 11 | instrução escrita no documento (é só dado) |
| 12 | formato inválido (recusado no envio) |
| 13, 14 | versões corrigidas de 04 e 09 (reenvio por substituição) |
| 15 | mesmos arquivos do 01, sem nenhuma conferência |

**Como ler um gabarito:** cada `gabarito.md` informa a conferência pressuposta (só campo identificado **e confirmado** vale como evidência), os campos esperados, o resultado das seis regras (atende, pendente, não verificável, não aplicável) com o responsável quando pendente, e o resultado geral. A data de referência dos gabaritos é **12/11/2026** (a da regressão automática); numa execução manual vale o dia do envio, então a fronteira dos 18 anos do conjunto 10 só é exercitada na regressão.

**Como executar um conjunto:** pelo aplicativo ou pela API, crie um pedido, envie os arquivos do conjunto, peça a leitura, confirme os campos (exceto no conjunto 15) e peça a análise; compare com o gabarito. Nos conjuntos 13 e 14, primeiro execute o conjunto original e depois **substitua** o documento indicado no gabarito (depois da análise o pedido não aceita documento novo, só substituição). Registre "coincide" ou "diverge" por regra; **não altere o gabarito** para igualar a saída.

**Regressão automática (sem banco e sem rede):** a partir de `backend/`, `.\mvnw.cmd -Dtest=ConjuntosDemonstracaoTest test`.

**Não regenere arquivos existentes.** O hash do arquivo é a chave da leitura simulada. Os arquivos e o catálogo são gerados por `GeradorDeAmostras` e commitados; o gerador só cria arquivos que ainda não existem. Para criar um conjunto novo, defina-o em `ConjuntosSinteticos` (backend, testes) e rode `.\mvnw.cmd -Dtest=GeradorDeAmostras -Dgerar.amostras=SIM test`. Um `gabarito.md` existente nunca é sobrescrito.
