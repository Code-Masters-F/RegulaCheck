# Research: Substituir documento e excluir rascunho (RC-09)

Decisões que a especificação deixou para o plano. Nada aqui exige nova dependência ou mudança de esquema.

## D1. Remover documento = remoção lógica; arquivo apagado

- **Decisão**: remover marca `RC_DOCUMENTO.REVISAO_REMOCAO` com o número da revisão nova (o esquema já prevê isso: FK composta para `RC_REVISAO` e `CHECK REVISAO_REMOCAO > REVISAO_ENVIO`) e **anula `CHAVE_ENVIO`** da linha. A linha permanece como histórico; o arquivo é apagado **depois do `COMMIT`**.
- **Por quê**: o critério pede "referências e arquivos removidos ou mantidos conforme política explícita, sem órfãos ativos". Documento disponível = `REVISAO_REMOCAO IS NULL` (todas as consultas da RC-08 já filtram isso); então a linha removida não é "ativa". Manter a linha preserva o histórico por revisão (CLAUDE.md §10) e deixa o `CAMINHO_INTERNO` no banco, o que permite achar um arquivo que sobrou numa falha de limpeza.
- **Anular a chave**: o índice único `RC_DOCUMENTO_UK_CHAVE` cobre também linhas removidas. Sem anular, reenviar com a mesma chave depois de remover violaria a unicidade (ORA-00001). A chave só serve para repetir uma tentativa **viva**; depois de removido o documento, reenviar com a chave cria um documento novo.
- **Alternativas**: apagar a linha (perde histórico e quebra a FK `REVISAO_ENVIO` de quem veio depois — descartado); manter o arquivo (viola "sem órfãos"; descartado).

## D2. Substituir = uma operação (`PUT /solicitacoes/{id}/documentos/{docId}`)

- **Decisão**: o `PUT` recebe o mesmo multipart do `POST` (`arquivo`, `tipoDocumental`, `X-Chave-Envio` opcional). Na **mesma transação**: marca o documento antigo como removido (D1) e insere o novo; **uma** revisão nova com motivo `DOCUMENTO_SUBSTITUIDO`; a contagem de disponíveis não muda (não precisa conferir o limite de 3, porque sai um e entra um).
- **Por quê**: remover + enviar em duas chamadas perderia a vaga se o arquivo novo fosse recusado e geraria duas revisões. O arquivo novo é conferido e guardado **antes** da transação (como na RC-08); se a transação recusar ou falhar, o novo é removido e o antigo fica intacto. Depois do `COMMIT`, o arquivo antigo é apagado.
- **Resposta**: 201 com `Location` do **novo** documento (id novo) e o mesmo corpo `DocumentoEnviadoResponse` do `POST`; 200 se for repetição da mesma tentativa.
- **Alternativas**: `POST …/{docId}/substituir` (verbo na rota; descartado); atualizar a mesma linha no lugar (perde o histórico do documento antigo e o `HASH`/`CAMINHO`; descartado).

## D3. Repetição da substituição (idempotência) vem **antes** de olhar o documento do caminho

- **Decisão**: com `X-Chave-Envio`, o serviço procura primeiro um documento **vivo do pedido** com essa chave. Se existe e `hash` e `tipoDocumental` batem → repetição (200, nada novo gravado). Se existe e diferem → 409 `CHAVE_ENVIO_REUTILIZADA`. Só sem chave, ou sem documento vivo com ela, o `{docId}` do caminho é procurado.
- **Por quê**: depois de uma substituição bem-sucedida, o `{docId}` do caminho (o antigo) já não existe; a repetição por queda de conexão receberia 404 e a paciente não saberia que deu certo (cenário US2-5). Mesma regra do `POST`, que já ignora o `docId`.
- **Efeito colateral aceito**: a mesma chave vale para `POST` e `PUT` no pedido; é uma identificação por tentativa, não por rota.

## D4. Excluir rascunho: `DELETE /solicitacoes/{id}` com confirmação em cabeçalho

- **Decisão**: confirmação = cabeçalho **`X-Confirmar-Exclusao: EXCLUIR`** (valor exato, sem diferenciar nada além disso). Ausente ou diferente → 400 `REQUISICAO_INVALIDA` com detalhe `confirmacao`; **nada é apagado**. Sucesso → 204 sem corpo.
- **Por quê**: `DELETE` com corpo é tratado de forma desigual por clientes e proxies; query string aparece em logs de acesso e pode ser disparada por prefetch/link; um cabeçalho exige ação deliberada do cliente e não vaza em URL. O valor literal impede um `DELETE` acidental ou um cliente que mande o cabeçalho por padrão.
- **Ordem** (a da RC-05): 401 → 403 → 404 (pedido da dona) → **400 confirmação** → 409 estado (só rascunho) → 503.
- **Alternativas**: `?confirmar=true` (descartado); corpo JSON `{"confirmo": true}` (descartado pelo motivo do `DELETE`).

## D5. Exclusão apaga tudo numa transação; arquivos depois do `COMMIT`

- **Decisão**: sob `SELECT … FOR UPDATE` do pedido: confere dona e `RASCUNHO`; lê os `CAMINHO_INTERNO` de **todos** os documentos (vivos e removidos); apaga `RC_DOCUMENTO`, `RC_REVISAO` e `RC_SOLICITACAO` (nessa ordem, por causa das FKs); `COMMIT`; só então apaga os arquivos.
- **Falha ao apagar arquivo depois do commit**: o pedido já não existe; o arquivo sobra sem registro. Mesmo risco aceito na RC-08 (arquivo sem registro, nunca o contrário). Registra-se `WARN` com identificadores técnicos e **contagem**, **sem** nome interno (FR-022). Não há retentativa automática.
- **Filhos inesperados**: se uma FK (ORA-02292) impedir o `DELETE` (por exemplo, uma sessão de captura de sensor ligada ao rascunho numa história futura), a transação é desfeita e a resposta é 409 `CONFLITO_ESTADO` "Este pedido não pode ser excluído". Nada é apagado pela metade.
- **Concorrência**: o `FOR UPDATE` serializa com envio, remoção, substituição e troca de serviço. Um `POST` de documento que espera o bloqueio acha o pedido inexistente e responde 404.
- **Alternativas**: apagar arquivos antes do commit (deixaria pedido com documento sem arquivo se a transação falhasse; descartado); exclusão lógica do pedido (acumula lixo e complica a listagem; descartado, pois a política é apagar).

## D6. Invalidação da conferência sem iniciar análise

- **Decisão**: toda remoção/substituição, na mesma transação, faz `UPDATE RC_ANALISE SET VIGENTE = 0 WHERE SOLICITACAO_ID = ? AND VIGENTE = 1` (sem efeito hoje, pois nenhuma rota cria análise) e atualiza `REVISAO_ATUAL`/`ATUALIZADA_EM`. O **estado do pedido não muda**.
- **Por quê**: o estado `PROCESSANDO` significa análise em andamento (CLAUDE.md §10); mover para ele sem processar seria falso. `docs/api/estados-e-revisoes.md` diz que "reenviar documento corrigido" leva a `PROCESSANDO`, mas é a história de reenvio/análise (RC-16) que dispara o processamento. Esta história só garante: resultado anterior não vigente, histórico preservado, e a resposta do pedido dizendo que **não há análise para a nova versão** (`analise.situacao = NAO_REALIZADA`, já devolvido desde a RC-08).
- **Registro**: `estados-e-revisoes.md` ganha a nota de que remover/substituir em estado de resultado não dispara a análise; quem dispara é a história de análise/reenvio. Isso é uma **decisão aberta para RC-13/RC-16**, listada em `tasks.md`.
- **Limitação de verificação**: nenhuma rota produz análise nem os estados `COM_PENDENCIAS` etc.; os testes dessas regras montam o pedido direto no estado (memória) e, no Oracle, só o `RASCUNHO` real. O `UPDATE` de `RC_ANALISE` é exercitado contra o Oracle apenas como "0 linhas afetadas" (não verificável de ponta a ponta até existir análise).

## D7. Estado `SUBMETIDA_SIMULACAO` e demais

- **Decisão**: remover e substituir: recusados com 409 `CONFLITO_ESTADO` **somente** em `SUBMETIDA_SIMULACAO`; nos demais estados são aceitos (D6). Excluir: **só** `RASCUNHO`; qualquer outro estado → 409 `CONFLITO_ESTADO` (inclusive os de análise, para não perder histórico).
- **Mensagens distintas** por operação, todas explicando o motivo sem detalhe técnico: "Este pedido já foi submetido e só pode ser consultado." (remover/substituir/excluir submetido) e "Somente rascunhos podem ser excluídos." (excluir fora de rascunho que não seja submetido).

## D8. Onde fica cada coisa

- **`DocumentoService`** ganha `remover` e `substituir`; **`SolicitacaoService`** ganha `excluirRascunho` e passa a receber o `ArmazenamentoPrivado` para apagar os arquivos depois do commit (dependência de infraestrutura comum, sem acoplar `solicitacao` a `documento`).
- **`DocumentoRepository`** ganha `remover` e `substituir` (transações com `FOR UPDATE`); **`SolicitacaoRepository`** ganha `excluirRascunho` (apaga também `RC_DOCUMENTO` do pedido, porque a exclusão é do pedido e as FKs exigem a ordem).
- **`DocumentoController`** ganha `doDelete` (`…/{docId}`) e `doPut` (`…/{docId}`); **`SolicitacaoController`** ganha `doDelete` (`/{id}`) e sua lista `Allow` passa a incluir `DELETE`.
- **Sem novo script SQL**: `REVISAO_REMOCAO`, os motivos `DOCUMENTO_REMOVIDO` e `DOCUMENTO_SUBSTITUIDO` e `RC_ANALISE.VIGENTE` já existem no esquema atual.
- **Respostas**: `DELETE …/{docId}` → 200 com `{ "versaoConjuntoDocumental": n, "documentosDisponiveis": m }` (o cliente precisa da versão nova); `DELETE /solicitacoes/{id}` → 204.

## D9. Remover/excluir documento da carga sintética

- Linhas da carga (`CAMINHO_INTERNO` fictício, `caminhoInterno()` nulo na entidade) podem ser removidas e substituídas: o apagamento do arquivo é **pulado** quando não há caminho válido. Nunca falha por isso.

## D10. Concorrência sem salto de revisão

- Todas as operações de escrita do pedido (envio, troca de serviço, remoção, substituição, exclusão) abrem a transação com o mesmo `SELECT … FOR UPDATE` do pedido. O número da revisão é lido **dentro** do bloqueio. Isso já vale para envio e troca; as novas seguem o mesmo padrão.
