# Research: Criar rascunho e selecionar serviço (RC-07)

Decisões do plano. Formato: decisão, motivo, alternativas. Nenhum item ficou como "NEEDS CLARIFICATION".

## D1 — "Serviço" é o protocolo acadêmico; `protocoloId` = identificador + "-" + versão
- **Decisão**: o serviço escolhido é uma linha ativa de `RC_PROTOCOLO`. O identificador público é o texto `IDENTIFICADOR || '-' || VERSAO` (ex.: `PROTOCOLO-ACADEMICO-FICTICIO-v0.1`), que é exatamente o valor que `docs/api/contratos-dados.md` já usa. A busca é `WHERE ATIVO = 1 AND IDENTIFICADOR || '-' || VERSAO = ?` (parametrizada).
- **Motivo**: nenhum conceito novo, nenhuma tabela nova, contrato existente preservado. A tabela tem poucas linhas; a concatenação não precisa de índice.
- **Alternativas**: expor o `ID` numérico (revela chave interna e muda o contrato); criar tabela `RC_SERVICO` (fora do modelo já entregue na RC-03 e sem necessidade com um serviço só).

## D2 — Catálogo em rota própria: `GET /protocolos`
- **Decisão**: rota nova, só `PACIENTE`, devolvendo `{ "itens": [ { protocoloId, identificador, versao, localidade, especialidade, avisoFicticio, simulacao } ] }` apenas com serviços ativos. Lista vazia é sucesso (200) e o cliente mostra "nenhum serviço disponível".
- **Motivo**: o critério de aceite diz que o formulário "carrega da API a localidade e o serviço"; sem rota o cliente teria o serviço embutido. `protocolos` (e não `servicos`) segue o nome da tabela e o campo `protocoloId` já contratado.
- **Alternativas**: embutir o catálogo na resposta de `GET /solicitacoes` (mistura recursos); rota pública (a RC-05 manda negar por omissão e o catálogo só interessa a quem está logada).

## D3 — Identificador do pedido: decimal opaco como string; id malformado vira 404
- **Decisão**: `id` na resposta é o `ID` numérico do Oracle escrito como string (`"12"`); o contrato antigo usava `"sol-0001"` só como exemplo. No caminho, o id precisa casar com `^[1-9][0-9]{0,17}$`; qualquer outra coisa (texto, negativo, zero, longo demais, subcaminho desconhecido) responde 404 `NAO_ENCONTRADO` sem consultar o banco.
- **Motivo**: evita criar e manter um segundo identificador; string preserva a liberdade de trocar o formato depois sem quebrar o cliente; 404 uniforme é exigência da spec (FR-016) e da RC-05.
- **Alternativas**: prefixo `sol-` com zeros (cosmético, exige conversão ida e volta); número JSON (o contrato já define string).

## D4 — Ordem de verificação e leitura do corpo no PUT
- **Decisão**: no `PUT /solicitacoes/{id}` o Controller primeiro pede ao Service a busca do pedido **filtrada pela dona** (404 se não é dela), e só depois lê e valida o corpo (400/415) e chama a troca. No `POST` não há dono a conferir: lê e valida o corpo direto.
- **Motivo**: `docs/api/erros.md` fixa a ordem autenticação → perfil → dona → validação → regra, justamente para que um corpo ruim não revele se um pedido alheio existe.
- **Alternativas**: validar o corpo antes (mais simples, mas vaza existência); fazer tudo no Service (o Service não conhece HTTP, e a leitura do corpo é do Controller).

## D5 — Atomicidade e concorrência: bloqueio de linha na troca de serviço
- **Decisão**: `trocarProtocolo(id, pacienteId, novoProtocoloId, agora)` abre uma transação, executa `SELECT ESTADO, PROTOCOLO_ID, REVISAO_ATUAL FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE` e decide:
  - nenhuma linha → `NAO_ENCONTRADA`;
  - estado ≠ `RASCUNHO` → `FORA_DE_RASCUNHO` (409);
  - mesmo `PROTOCOLO_ID` → `SEM_MUDANCA` (revisão e `ATUALIZADA_EM` intactas);
  - senão `UPDATE` (`PROTOCOLO_ID`, `REVISAO_ATUAL = REVISAO_ATUAL + 1`, `ATUALIZADA_EM`), `INSERT` em `RC_REVISAO` com motivo `PROTOCOLO_ALTERADO` e `COMMIT` → `ALTERADA`.
  Qualquer falha faz `ROLLBACK`. O segundo PUT simultâneo espera o primeiro terminar, relê o estado e o protocolo já gravados e decide sobre eles; a chave primária `(SOLICITACAO_ID, NUMERO)` de `RC_REVISAO` é a rede de segurança final contra número repetido.
- **Motivo**: elimina a "checagem ingênua seguida de gravação" (CLAUDE.md §10) com a primitiva que o Oracle oferece, sem coluna nova de versão e sem retentativa.
- **Alternativas**: `UPDATE … WHERE REVISAO_ATUAL = ?` com retentativa (mais código e laço de retry); controle otimista com `If-Match` (muda o contrato do cliente); `synchronized` na JVM (não vale com mais de uma instância e esconde o problema).

## D6 — Criação atômica, sem deduplicação
- **Decisão**: `criar(pacienteId, protocoloId, agora)` insere em `RC_SOLICITACAO` (estado `RASCUNHO`, `REVISAO_ATUAL = 1`, `CRIADA_EM = ATUALIZADA_EM = agora`) recuperando o `ID` gerado, insere `RC_REVISAO(ID, 1, 'CRIACAO')` e faz `COMMIT`; falha em qualquer passo faz `ROLLBACK`. Duas criações iguais geram dois rascunhos (decisão da spec); a proteção contra toque duplo é do cliente e fica documentada.
- **Motivo**: o pedido nunca existe sem sua revisão inicial; sucesso só depois do `COMMIT` (FR-007). Deduplicar exigiria uma chave de idempotência que o contrato não tem e que a spec deixou fora.
- **Alternativas**: cabeçalho `Idempotency-Key` (escopo maior, sem pedido do backlog); limitar a um rascunho por paciente (regra de negócio inventada).

## D7 — Erros: 400 de contrato, 422 de regra, 409 de estado
- **Decisão**: `protocoloId` ausente, em branco, de tipo errado (objeto/lista) ou acima de 100 caracteres → 400 `REQUISICAO_INVALIDA` com `detalhes[{campo:"protocoloId"}]`. Protocolo inexistente ou inativo (mesma resposta) → 422 `REGRA_DE_NEGOCIO` com o mesmo campo e a orientação "Escolha um dos serviços da lista." Pedido fora de rascunho → 409 `CONFLITO_ESTADO`. Sem protocolo ativo nenhum → também 422, e o catálogo vazio já avisou o cliente.
- **Motivo**: FR-019 pede erro de contrato distinguível de erro de regra; 422 e 409 já existem no catálogo de `docs/api/erros.md` (o 422 era "opcional" e passa a ser usado). Campos desconhecidos (`pacienteId`, `estado`) são ignorados pelo `Json` existente e nunca lidos (FR-005); isso é testado.
- **Alternativas**: tudo 400 (apaga a distinção que a spec exige); 404 para protocolo inexistente (confunde com recurso do caminho).

## D8 — Listagem: limite fixo de 50, mais recentes primeiro
- **Decisão**: `ORDER BY s.CRIADA_EM DESC, s.ID DESC FETCH FIRST 50 ROWS ONLY`, filtrando `s.PACIENTE_ID = ?`. A resposta é `{ "itens": [...], "limite": 50 }`. Sem parâmetros de paginação nesta história.
- **Motivo**: dá resposta limitada e ordem estável (desempate por `ID`) sem criar contrato de paginação; Oracle 19c suporta `FETCH FIRST`. Paginação real, se necessária, entra na RC-18.
- **Alternativas**: sem limite (risco de resposta ilimitada); `page/size` agora (contrato maior que o pedido).

## D9 — Motivo de revisão `PROTOCOLO_ALTERADO` por script aditivo
- **Decisão**: `06_motivo_revisao_protocolo.sql` faz `ALTER TABLE RC_REVISAO DROP CONSTRAINT RC_REVISAO_CK_MOTIVO` e `ADD CONSTRAINT RC_REVISAO_CK_MOTIVO CHECK (MOTIVO IN ('CRIACAO','DOCUMENTO_ENVIADO','DOCUMENTO_SUBSTITUIDO','DOCUMENTO_REMOVIDO','CONFERENCIA_ALTERADA','PROTOCOLO_ALTERADO'))`. Nenhuma linha é apagada ou alterada, e os valores antigos continuam válidos. O `01_criar_esquema.sql` ganha o mesmo valor para instalações novas. O enum `MotivoRevisao` do código lista os seis valores.
- **Motivo**: o banco continua recusando motivos inventados, o histórico registra a verdade e bancos já criados migram com um comando que o grupo executa conscientemente. Não é limpeza nem recriação de tabela.
- **Alternativas**: reaproveitar `CONFERENCIA_ALTERADA` (motivo falso); remover a restrição (enfraquece o banco); recriar o esquema (destrutivo, proibido).
- **Risco**: o script precisa ser executado no Oracle da faculdade antes da primeira troca de serviço real. Sem isso, `PUT` que muda de serviço falha com violação de restrição (tratada como erro interno genérico, sem vazar SQL) enquanto criar, listar e reabrir continuam funcionando. O teste Oracle opt-in e o `quickstart.md` verificam isso. **Isto é uma pendência de execução do grupo, não algo que o código pode fazer sozinho.**

## D10 — Rotas e crescimento para as próximas histórias
- **Decisão**: `/solicitacoes/*` é registrada uma vez em `RotasPrivadas` (o padrão `/solicitacoes/*` também atende `/solicitacoes`, como o exemplo de `docs/api/autorizacao.md`). O `SolicitacaoController` trata só `/`, `/{id}`; qualquer outro subcaminho responde 404 padrão. As histórias de documentos, extração e análise (RC-08 em diante) acrescentarão seus subcaminhos a esse mesmo ponto de entrada ou trocarão o registro por um roteador interno; essa decisão pertence a elas.
- **Motivo**: o mecanismo da RC-05 recusa dois registros para o mesmo caminho, e antecipar um roteador seria construir para histórias que não foram pedidas.
- **Alternativas**: roteador genérico agora (YAGNI, CLAUDE.md §5); registrar `/solicitacoes` e `/solicitacoes/*` separados (dois controllers para o mesmo recurso).

## D11 — Logs e dados pessoais
- **Decisão**: os logs da feature registram só `usuarioId`, `solicitacaoId`, etapa e código de erro (ex.: `Rascunho criado. solicitacaoId=12 revisao=1`). Nunca e-mail, nome ou corpo da requisição. O teste de higiene de log existente (`HigieneDeLogGeralTest`) é estendido para cobrir as novas rotas.
- **Motivo**: FR-022 e CLAUDE.md §9.
- **Alternativas**: nenhum log de negócio (perde rastreabilidade de demonstração).
