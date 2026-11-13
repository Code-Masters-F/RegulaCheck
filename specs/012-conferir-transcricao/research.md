# Research: Conferir a transcrição antes da validação (RC-11)

Decisões que a especificação deixou para o plano. Nenhuma exige dependência nova; D6 exige um script de esquema aditivo.

## D1. Corpo fechado: a paciente nunca digita valor

- **Decisão**: `PUT` recebe `{ "decisoes": [ { "documentoId": "41", "codigo": "NOME", "decisao": "CONFIRMADA" }, { "documentoId": "41", "codigo": "DATA_EMISSAO", "decisao": "DIVERGENTE", "motivo": "VALOR_DIFERENTE_DO_DOCUMENTO" } ] }`. O DTO é lido como `JsonNode` e validado à mão: só as chaves `documentoId`, `codigo`, `decisao`, `motivo`; qualquer outra (`valor`, `texto`, `observacao`…) → 400 `REQUISICAO_INVALIDA` com detalhe no campo.
- **Por quê**: o mapeador do projeto ignora propriedades desconhecidas (`FAIL_ON_UNKNOWN_PROPERTIES` desligado), então a recusa precisa ser explícita para cumprir FR-007. A lista fechada de motivos impede texto livre (que poderia carregar dado clínico ou instrução).
- **Limites**: 1 a 18 decisões por requisição (3 documentos × 6 campos), sem par `(documentoId, codigo)` repetido no mesmo corpo; corpo ≤ 4 KB (limite de `CorpoJson`).
- **Alternativas**: um `PUT` por campo (mais chamadas, sem atomicidade entre campos; descartado); aceitar campo `valor` e ignorá-lo (esconde erro de quem chama; descartado).

## D2. A decisão pertence à leitura e só vale para a leitura atual

- **Decisão**: a rota usa o `extracaoId` (já exposto pela RC-10). O serviço exige que a leitura pertença ao pedido da paciente; se `Extracao.revisao ≠ REVISAO_ATUAL` do pedido (leitura desatualizada), o `GET` mostra `DESATUALIZADA` somente-leitura e o `PUT` responde 409 `CONFLITO_ESTADO` ("Os documentos mudaram; leia os documentos de novo."). Nova leitura (RC-10) nasce sem decisões.
- **Por quê**: spec FR-016; as decisões referenciam `RC_EXTRACAO_CAMPO` (FK), então a nova leitura, por ser outra linha, não herda nada, e a anterior continua como histórico.
- `GET` de leitura de outro pedido, de outra paciente ou inexistente → o mesmo 404.

## D3. Não escreve o estado do pedido e não cria revisão (ajuste à spec)

- **Decisão**: registrar decisão não muda `RC_SOLICITACAO.ESTADO`, `REVISAO_ATUAL` nem `RC_REVISAO`, e não toca em `RC_ANALISE`. `ATUALIZADA_EM` do pedido não muda.
- **Por quê** (três razões do código e dos contratos existentes):
  1. O fluxo documentado (`estados-e-revisoes.md`) só leva a `REQUER_CONFERENCIA` a partir de `PROCESSANDO` (análise); essa transição é da RC-12/RC-13.
  2. `POST …/documentos` (RC-08) exige `RASCUNHO`: um pedido em `REQUER_CONFERENCIA` deixaria de aceitar documentos novos e (RC-09) de ser excluído, e nenhuma rota o devolveria a `RASCUNHO`.
  3. Criar a revisão `CONFERENCIA_ALTERADA` tornaria desatualizada a própria leitura conferida (atual = revisão da leitura igual à do pedido).
- **Desvio da constituição (princípio VI) e sua compensação:** "conferência muda" aparece como motivo de revisão na constituição e em `estados-e-revisoes.md`. Aqui a decisão é tratada como anotação da leitura, não como dado do pedido. A compensação é `atualizadaEm` na resposta e a regra de que a RC-12/RC-13 capturem a conferência ao publicar uma análise e a invalidem se ela mudar. Justificativa formal em "Complexity Tracking" do plano; alternativa rejeitada: criar a revisão e redefinir "atual" da leitura.
- **Efeito**: a **situação da conferência** é o resultado desta história; "sem liberação automática para submissão" vira `liberadoParaSubmissao: false` sempre, e nenhuma rota de submissão é criada ou habilitada. A RC-12/RC-13 usam a situação (e `atualizadaEm`) para decidir estado e para saber se uma análise ficou velha frente a uma conferência nova.

## D4. Situação calculada por regra pura

- **Decisão**: `SituacaoConferencia.calcular(leitura, decisões)` (sem HTTP nem JDBC), com precedência:
  1. **`REQUER_CONFERENCIA`** se algum documento não foi lido (`PROTEGIDO`/`LEITURA_VAZIA`), ou algum campo está `ILEGIVEL` num documento lido, ou alguma decisão vigente é `DIVERGENTE`;
  2. senão **`PENDENTE`** se algum campo `IDENTIFICADO` está sem decisão;
  3. senão **`CONFERIDA`** (todo campo identificado confirmado; os `NAO_LOCALIZADO` sem divergência não pesam, porque ausência no documento é assunto das regras, RC-12).
- E, fora dessa precedência, leitura desatualizada → `DESATUALIZADA`.
- **Campo ilegível**: não aceita decisão (422 `DECISAO_NAO_APLICAVEL`); resolve-se com nova leitura. **Campo não localizado**: aceita só `DIVERGENTE` com `CAMPO_EXISTE_E_NAO_FOI_LIDO`. **Campo identificado**: aceita `CONFIRMADA` ou `DIVERGENTE` com `VALOR_DIFERENTE_DO_DOCUMENTO`.
- **Por quê**: determinístico, testável sem banco, e coerente com o protocolo (documento ilegível ≠ requisito ausente).
- **Contagens** devolvidas: `camposNaoVerificados`, `camposSemDecisao`, `divergencias`.

## D5. Nunca libera submissão

- **Decisão**: `liberadoParaSubmissao` existe na resposta e é sempre `false` nesta história; `CONFERIDA` apenas diz que a paciente conferiu a leitura. Não há resultado por regra, pendência nem resultado geral.
- **Por quê**: spec FR-014/FR-015; a liberação depende de regras (RC-12) e da submissão simulada (RC-17).

## D6. Esquema: uma tabela, uma decisão vigente por campo

- **Decisão**: `09_conferencia.sql` cria `RC_CONFERENCIA_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO, DECISAO, MOTIVO, DECIDIDA_EM)` com `PK (EXTRACAO_DOCUMENTO_ID, CODIGO)`, `FK (EXTRACAO_DOCUMENTO_ID, CODIGO) → RC_EXTRACAO_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO)` (garante que o campo existe na leitura) e `CHECK`s: `DECISAO IN ('CONFIRMADA','DIVERGENTE')`; `MOTIVO` só e sempre em `DIVERGENTE` e na lista fechada.
- **Upsert**: dentro da transação sob `FOR UPDATE` do pedido, `UPDATE` e, se nenhuma linha mudou, `INSERT` (sem janela de corrida, porque o bloqueio serializa).
- **Sem histórico**: vale a última decisão (FR-008); o histórico por revisão é da leitura anterior, que continua gravada. `DECIDIDA_EM` só muda quando a decisão muda (repetir a mesma é no-op).
- **Excluir rascunho (RC-09)**: passa a apagar `RC_CONFERENCIA_CAMPO` antes de `RC_EXTRACAO_CAMPO`; `ORA-00942` (banco sem o `09_`) é ignorado, como já é para o `08_`.
- **Alternativas**: coluna de decisão em `RC_EXTRACAO_CAMPO` (altera tabela da RC-10 e mistura "o que foi lido" com "o que ela decidiu"; descartado); tabela de histórico de decisões (sem requisito; descartado).

## D7. Transação e concorrência

- **Decisão**: `registrar` abre uma transação curta: `SELECT … FOR UPDATE` do pedido (dona) → recusa `SUBMETIDA_SIMULACAO` → confere que a leitura pertence ao pedido e é a atual (`REVISAO = REVISAO_ATUAL`) → para cada decisão: o documento pertence à leitura (senão 404 idêntico), o campo existe, a decisão é aplicável à situação do campo (senão 422) → upserts → `COMMIT`; qualquer recusa → `ROLLBACK` e nada é gravado.
- **Por quê**: o mesmo bloqueio já serializa enviar/remover/substituir/ler; assim uma decisão nunca é gravada para uma leitura que ficou velha entre a conferência e a gravação (CLAUDE.md §10).
- **Resposta**: depois do `COMMIT`, relê a conferência e devolve 200 com a situação nova.

## D8. Orientações fixas, sem diagnóstico

| Situação | Orientação |
|---|---|
| `NAO_LOCALIZADO` | "Esta informação não consta no documento lido. Se ela for necessária, peça um documento corrigido à unidade emissora. Não é possível preencher aqui." |
| `ILEGIVEL` | "Não foi possível ler este campo. Envie um arquivo mais nítido." |
| Documento não lido | a da RC-10 (reenviar) |
| `DIVERGENTE` / `VALOR_DIFERENTE_DO_DOCUMENTO` | "Você indicou que o valor lido não confere. Se o erro está no documento, peça um documento corrigido à unidade emissora; se foi erro de leitura, envie um arquivo mais nítido." |
| `DIVERGENTE` / `CAMPO_EXISTE_E_NAO_FOI_LIDO` | "Você indicou que o documento tem este campo, mas ele não foi lido. Envie um arquivo mais nítido." |

Textos fixos no servidor (nenhum eco de valor ou trecho); sem termos de diagnóstico, urgência, prioridade ou promessa de atendimento (verificado por teste que varre as respostas).

## D9. Roteamento, códigos HTTP e erros

- `GET`/`PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` (`Allow: GET, PUT`); outros métodos → 405.
- Ordem: 401 → 403 → 404 (pedido, leitura do pedido) → 415/400 (corpo, só no `PUT`) → 409 (submetido; leitura desatualizada) → 404 (documento fora da leitura) → 422 `DECISAO_NAO_APLICAVEL` → 503.
- 400 `REQUISICAO_INVALIDA`: corpo ausente/ilegível, `decisoes` vazia ou > 18, chave desconhecida (inclui `valor`), `decisao` ou `motivo` fora da lista, motivo ausente em `DIVERGENTE` ou presente em `CONFIRMADA`, par repetido, `codigo` desconhecido, `documentoId` malformado.
- `extracaoId` malformado, de outro pedido ou inexistente → 404 `NAO_ENCONTRADO` idêntico.

## D10. Higiene e dados não confiáveis

- Logs só com ids técnicos, quantidade de decisões e códigos; nenhum valor, trecho, nome de arquivo ou motivo livre. Textos de documento (inclusive "confirme tudo") são só dado do campo e nunca preenchem decisão.
