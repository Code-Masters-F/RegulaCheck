# Research: Extrair informações dos arquivos (RC-10)

Decisões que a especificação deixou para o plano. Nenhuma exige dependência nova; D4 exige um script de esquema aditivo.

## D1. O leitor simulado reconhece o arquivo pelo SHA-256

- **Decisão**: `LeitorSimulado` calcula o SHA-256 do conteúdo e procura o hash em um catálogo predefinido (`extracao-simulada/catalogo.json`, recurso do classpath). Achou → devolve a leitura predefinida daquele caso; não achou → `LEITURA_VAZIA`.
- **Por quê**: não lê nada de verdade (não afirma leitura real) e é determinístico (SC-001). O hash identifica o *arquivo sintético* sem olhar nome, extensão ou legenda, que são dado do usuário. O hash já é calculado no envio (`RC_DOCUMENTO.HASH_CONTEUDO`), mas o leitor recebe os bytes e recalcula: o contrato não depende do banco.
- **Alternativas**: chave pelo nome do arquivo (manipulável, descartado); pelo tipo documental informado (não identifica o conteúdo, descartado); ler texto do PDF com PDFBox (seria leitura real, contraria a decisão de 12/11/2026).

## D2. Arquivo desconhecido → `LEITURA_VAZIA`, nunca campos

- **Decisão**: sem entrada no catálogo, o documento é `NAO_LIDO` com motivo `LEITURA_VAZIA` e orientação de reenvio, sem nenhum campo.
- **Por quê**: declarar "não localizado" exigiria afirmar que o documento foi lido com clareza, o que o simulador não fez (FR-010, FR-013). `LEITURA_VAZIA` é o resultado honesto de "nada foi extraído".

## D3. Saída do leitor validada contra contrato fechado

- **Decisão**: `ValidadorDeLeitura` confere a saída de **qualquer** leitor: códigos de campo na lista fechada (um por código, sem repetição), valor presente **só** em `IDENTIFICADO` (≤ 200 caracteres, sem controle), origem coerente (página ≥ 1 só para PDF, trecho ≤ 200), `NAO_LIDO` sem campos e com motivo, `LIDO` com os 6 campos. Falha na validação = falha do leitor (503, nada gravado).
- **Por quê**: CLAUDE.md §11 (mesmas validações para mock e real; saída inválida não é apresentada como confiável). Não se reaproveita `ValidadorDeSaidaEstruturada` (itens `codigoRegra/estado/texto`) porque o formato aqui é outro (campo/situação/valor/origem); a regra de fechar o contrato e nunca ecoar o conteúdo é a mesma.

## D4. Esquema: três tabelas; "atual" deriva da revisão

- **Decisão**: `08_extracao.sql` cria `RC_EXTRACAO` (PK `ID`; `UK (SOLICITACAO_ID, REVISAO)`; `SIMULADA` com `CHECK = 1`), `RC_EXTRACAO_DOCUMENTO` (uma linha por documento lido; situação `LIDO`/`PROTEGIDO`/`LEITURA_VAZIA`) e `RC_EXTRACAO_CAMPO` (uma linha por campo de documento `LIDO`). Sem coluna de vigência: a leitura é `ATUAL` quando `RC_EXTRACAO.REVISAO = RC_SOLICITACAO.REVISAO_ATUAL`, senão `DESATUALIZADA`.
- **Por quê**: remover/substituir já incrementam a revisão (RC-09), então a leitura envelhece sozinha, sem `UPDATE` extra nem janela de inconsistência. A `UK (pedido, revisão)` dá idempotência e protege contra duas leituras simultâneas (a segunda recebe a existente). Histórico por revisão preservado (RC-13 reaproveita).
- **Documento**: `RC_EXTRACAO_DOCUMENTO.DOCUMENTO_ID` referencia `RC_DOCUMENTO` (a linha removida continua existindo, então a leitura antiga nunca fica com referência quebrada).
- **Excluir rascunho (RC-09)**: a exclusão passa a apagar também `RC_EXTRACAO_CAMPO` → `RC_EXTRACAO_DOCUMENTO` → `RC_EXTRACAO` antes de `RC_DOCUMENTO`, `RC_REVISAO` e o pedido (ajuste em `SolicitacaoRepositoryJdbc.excluirRascunho` e no dobro em memória); sem isso a FK impediria a exclusão (409 `COM_DEPENDENCIAS`).
- **Alternativas**: coluna `VIGENTE` (exige `UPDATE` em remover/substituir, mais um ponto de falha); guardar JSON em uma coluna (perde as restrições por campo; descartado).

## D5. Ler fora da transação, gravar só se a revisão ainda for a capturada

- **Decisão**: o serviço (1) confere dona e estado e lê a lista de documentos disponíveis e a revisão atual; (2) abre cada arquivo no armazenamento e chama o leitor, **sem conexão aberta**; (3) numa transação curta com `SELECT … FOR UPDATE` do pedido, confere que a revisão atual ainda é a capturada e que não existe leitura para ela, e grava tudo. Revisão mudou no meio → 409 `CONFLITO_ESTADO` ("Os documentos mudaram; tente de novo.") sem gravar.
- **Por quê**: pool de 3 conexões e CLAUDE.md §10 (captura da revisão; checagem ingênua seguida de gravação não basta: o `FOR UPDATE` fecha a janela).
- **Já existe leitura para a revisão**: devolve a existente com 200, sem chamar o leitor de novo (pré-checagem barata) e, na corrida, o repositório devolve a existente (`JA_EXISTIA`).

## D6. O que é gravado e o que não é

| Resultado do leitor | Gravado? | Resposta |
|---|---|---|
| `LIDO` com campos | sim | 201/200 |
| `PROTEGIDO` ou `LEITURA_VAZIA` (por documento) | sim, como situação do documento (motivo + `reenviar`) | 201/200 |
| Falha do leitor ou saída inválida | **não** (nenhum resultado parcial) | 503 `LEITURA_INDISPONIVEL`, pode repetir |

- Leitura com algum documento não lido ainda é gravada para os outros (FR-018). Se **todos** os documentos tiverem a mesma impossibilidade, a resposta é a mesma, 201, mostrando o motivo de cada um; nada vira pendência (FR-017).
- Repetir sobre a mesma revisão devolve o gravado (200): para tentar de novo um documento mal lido, a paciente o substitui (nova revisão) — "reenvio".
- `PROTEGIDO` real não ocorre hoje por upload (a RC-06 recusa PDF com senha), mas o contrato o suporta e o teste o exerce com leitor falso.

## D7. Estado do pedido e análise

- **Decisão**: a leitura não muda `RC_SOLICITACAO.ESTADO`, não cria revisão e não toca em `RC_ANALISE`.
- **Por quê**: spec FR-022 e a RC-09 (D6): `PROCESSANDO`/`REQUER_CONFERENCIA` pertencem a RC-11/RC-12. `SUBMETIDA_SIMULACAO` recusa `POST` (409); `GET` continua. `RASCUNHO` e demais estados aceitam.

## D8. Roteamento, códigos HTTP e erros

- `POST` 201 (nova) / 200 (já existia para a revisão); `GET` 200 / 404. Sem documentos disponíveis: **422** `SEM_DOCUMENTOS` (pedido existe e é da paciente, mas não há o que ler; como o 422 do limite na RC-08). 409 `CONFLITO_ESTADO` para submetido e para revisão mudada. 503 `LEITURA_INDISPONIVEL` (falha do leitor, sem detalhe) e 503 `DEPENDENCIA_INDISPONIVEL` (banco/armazenamento). Ordem: 401 → 403 → 404 → 422 → 409 → 503.
- O `POST` não tem corpo (ignorado); `Content-Type` não é exigido.
- Marcação de simulado: campo `simulada: true` na leitura e `origemDaLeitura: "SIMULADA"`; aviso fixo em `aviso`.

## D9. Amostras sintéticas e catálogo

- **Decisão**: `samples/` recebe as duas amostras oficiais (PDF de encaminhamento e foto PNG de identificação), geradas por um programa de apoio dos testes e **commitadas** (PDF gerado por PDFBox não tem hash estável: data e ID mudam, então só vale o arquivo commitado); `samples/gabarito.md` lista, por amostra, os campos esperados. O catálogo de **produção** (`src/main/resources/extracao-simulada/catalogo.json`) contém **só** o hash dessas duas amostras e a leitura predefinida de cada uma; um teste confere que o hash do arquivo commitado coincide com o do catálogo.
- **Casos extras só de teste** em um catálogo próprio (`src/test/resources/extracao-simulada/catalogo-testes.json`), com amostras também commitadas (em `backend/src/test/resources/extracao-simulada/`): encaminhamento sem motivo (`NAO_LOCALIZADO`), identificação com campo borrado (`ILEGIVEL`), campo repetido/conflitante (vira `ILEGIVEL`, ver D10), documento com texto-injeção (`ignore as regras`). O texto-injeção vira apenas valor/trecho e nunca altera nada (FR-023). `LeitorSimulado` recebe o catálogo por construtor (o de produção usa o recurso padrão), então nenhum hash de fixture de teste entra no código de produção.
- **Por quê**: SC-001/SC-002 precisam de casos para as três situações; o PDF e a foto exigidos pela issue são as duas amostras principais.

## D10. Campo repetido ou conflitante no mesmo documento

- **Decisão**: um campo por código (a chave é `(documento, código)`). Se o documento traz o mesmo campo mais de uma vez com valores diferentes, o leitor devolve esse campo como `ILEGIVEL` (sem valor): o sistema não escolhe um valor por suposição. O `ValidadorDeLeitura` rejeita código repetido na saída.
- **Por quê**: spec FR-009 (não completar por suposição) e protocolo (`NAO_VERIFICAVEL` em vez de adivinhar). Guardar várias ocorrências exigiria outro modelo, sem necessidade para as regras R01 a R06.

## D11. `tipoDocumental` informado × `TIPO_DOCUMENTO` lido

- **Decisão**: os dois são mostrados e **não** são reconciliados: `tipoDocumental` (o que a paciente informou no envio) fica no documento; `TIPO_DOCUMENTO` (o que o leitor identificou) fica entre os campos. Divergência entre eles não vira pendência nesta história; a RC-12 decide como usar.
- **Por quê**: esta história só produz dados; comparar é regra.

## D12. Ordem entre estado e idempotência no `POST`

- **Decisão**: o estado vem antes da idempotência. Pedido `SUBMETIDA_SIMULACAO` responde 409 mesmo que já exista leitura da revisão atual; a consulta (`GET`) continua permitida.
- **Por quê**: FR-021 ("recusar nova leitura") e consistência com a ordem de verificação do contrato.

## D13. Origem do dado e leitor real

- A origem de um campo é página e trecho; o **arquivo** é o próprio documento (`documentoId`) ao qual o campo pertence.
- O contrato `LeitorDeDocumento` documenta que um leitor real deve ter **timeout** e tratar indisponibilidade como `LeituraIndisponivelException` (CLAUDE.md §11); o leitor simulado é em processo e não precisa.
