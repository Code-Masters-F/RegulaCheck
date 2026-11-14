# Research: Persistir análises e histórico por revisão (RC-13)

Decisões técnicas da RC-13, tomadas a partir do código existente (RC-09 a RC-12), do esquema `01_criar_esquema.sql` e de `docs/api/estados-e-revisoes.md`. Nenhum item ficou como NEEDS CLARIFICATION.

## D1 — Análise síncrona, com `PROCESSANDO` como trava de prazo limitado

**Decision**: o pedido de análise é atendido na própria requisição, em três passos, nenhum com transação aberta durante a avaliação:

1. **Iniciar** (transação curta, `SELECT … FOR UPDATE` do pedido): confere dona, estado, análise em andamento, análise vigente e leitura atual; se puder começar, grava `ESTADO = 'PROCESSANDO'` e `ANALISE_INICIADA_EM = agora`.
2. **Avaliar** sem bloqueio, com o `AvaliacaoService` da RC-12 (cada consulta abre e fecha a própria conexão).
3. **Publicar** (transação curta, mesmo bloqueio): confere de novo e grava a análise inteira, ou **encerra sem resultado** (descarte ou falha).

`ANALISE_INICIADA_EM` (coluna nova em `RC_SOLICITACAO`, preenchida se e somente se o estado é `PROCESSANDO`) é ao mesmo tempo o início, a referência do limite de tempo e o **token** de quem está processando: só o processamento cujo instante de início ainda é o gravado pode publicar ou encerrar.

**Rationale**: a avaliação é determinística, em memória, e a leitura e a IA são simuladas, então esperar a conclusão na requisição é simples e honesto (a resposta só sai depois de gravada). Persistir `PROCESSANDO` cumpre a tabela de estados, torna visível o processamento em curso, recusa o segundo processamento simultâneo (critério 4) e dá ao limite de tempo algo concreto para medir. O token impede que um processamento atrasado (depois do limite) publique por cima de outro.

**Alternatives considered**:
- *Sem `PROCESSANDO` gravado (só conferência otimista na publicação)*: evita duplicata, mas o estado nunca aparece, um segundo processamento roda inteiro à toa e o "limite de tempo" do critério 4 ficaria sem objeto. Rejeitada.
- *Usar `ATUALIZADA_EM` como token*: substituir/remover documento também muda `ATUALIZADA_EM` (RC-09), o que confunde "outro processamento assumiu" com "a entrada mudou" e impediria restaurar o estado. Rejeitada.
- *Tabela própria de execuções*: daria histórico de falhas, mas acrescenta tabela, limpeza na exclusão do rascunho e mais código sem atender critério novo; falhas e descartes ficam nos logs. Rejeitada.
- *Processamento assíncrono (202 + fila)*: exigiria executor e retomada; vetado pela constituição sem necessidade (mensageria) e desnecessário para avaliação em memória. Rejeitada.

## D2 — O que é conferido na publicação

**Decision**: sob o bloqueio do pedido, na ordem: (a) o pedido existe e é da paciente; (b) `ESTADO = 'PROCESSANDO'` e `ANALISE_INICIADA_EM` igual ao início deste processamento — senão o resultado é **descartado sem tocar em nada** (outro processamento assumiu ou já terminou); (b2) `agora` ainda antes de `ANALISE_INICIADA_EM + limite` — senão o resultado tardio não é publicado e, como esta ainda é a dona do `PROCESSANDO`, o pedido vai a `FALHA_ANALISE` (revisão do CodeRabbit no PR #67, FR-018); (c) `REVISAO_ATUAL` igual à revisão avaliada e o instante atual da conferência igual ao `conferenciaAtualizadaEm` avaliado — senão **descarte com restauração do estado** (D4); (d) grava.

O instante da conferência é recalculado no banco com a mesma definição de `ConferenciaService.visao`: `MAX(DECIDIDA_EM)` das decisões da leitura avaliada ou, sem decisão, `RC_EXTRACAO.CRIADA_EM`. Para que ele mude a **cada** gravação que muda o conteúdo da conferência, o `ConferenciaRepositoryJdbc` grava as decisões com `max(agora, instante atual da conferência + 1 µs)`, calculado **sob o bloqueio** (`InstanteDaConferencia`): o `agora` é capturado antes do bloqueio, e duas gravações concorrentes confirmadas fora de ordem, ou no mesmo microssegundo, deixariam o `MAX` igual, aceitando uma análise velha e fazendo a seguinte colidir com `RC_ANALISE_UK_CONFERENCIA` (revisão do CodeRabbit no PR #67). Alternativa rejeitada: contador de geração por leitura (exigiria outra coluna, num esquema já aplicado). As escritas de conferência (RC-11), de leitura (RC-10) e de documentos (RC-08/09) já pegam o mesmo bloqueio de linha, então nada muda entre a conferência e a gravação.

**Rationale**: atende o critério 3 e a compensação registrada pela RC-11 (a decisão de conferência não cria revisão, então o instante dela também precisa ser conferido). Comparar os dois valores sob o bloqueio evita a "checagem ingênua seguida de gravação" que o `CLAUDE.md` §10 proíbe.

**Alternatives considered**: comparar só a revisão (deixaria passar mudança de conferência durante o processamento, critério 3 falharia); avaliar dentro da transação bloqueada (exigiria duplicar no repositório da análise toda a carga de leitura, conferência e regras, ou compartilhar conexão entre repositórios).

## D3 — Idempotência e concorrência

**Decision**:
- **Repetição sequencial**: se o pedido tem análise vigente **da revisão atual**, `iniciar` a devolve (200, `criada: false`) sem gravar nem mudar estado. Isso basta porque toda mudança de entrada tira a vigência: documentos (já na RC-09) e, agora, decisão de conferência que altere algo (`ConferenciaRepositoryJdbc` passa a executar `UPDATE RC_ANALISE SET VIGENTE = 0 … WHERE VIGENTE = 1` na mesma transação, só quando grava alguma decisão).
- **Repetição simultânea**: o segundo pedido encontra `PROCESSANDO` dentro do prazo e recebe 409 `ANALISE_EM_ANDAMENTO`; nada é iniciado.
- **Rede de proteção no banco**: o índice único de vigência que já existe (`RC_ANALISE_VIGENTE_UK`) e uma restrição única nova `(SOLICITACAO_ID, REVISAO, CONFERENCIA_EM)` que substitui `RC_ANALISE_UK_REVISAO` (que impediria a segunda análise da mesma revisão depois de completar a conferência).

**Rationale**: critério 4 e `CLAUDE.md` §10 (identificadores, restrições e controle transacional). Sem mudança de entrada, a avaliação é determinística (RC-12 FR-026), então devolver a vigente é a resposta correta.

**Alternatives considered**: chave de idempotência enviada pelo cliente (desnecessária: a própria entrada identifica a análise); calcular a vigência na leitura comparando revisão e instante de conferência (cada consumidor — RC-15, RC-17 — teria de repetir o cálculo; a invalidação na escrita já é o padrão da RC-09).

## D4 — Estado do pedido depois de um descarte

**Decision**: quando o resultado é descartado por mudança de entrada (D2-c), ou a avaliação recusa a leitura porque os documentos mudaram depois de iniciar, o pedido volta ao **resultado geral da análise concluída mais recente** (maior `CRIADA_EM`, desempate por `ID`) ou a `RASCUNHO` se nunca houve análise concluída; `ANALISE_INICIADA_EM` volta a nulo.

**Rationale**: nenhuma coluna extra para guardar "o estado de antes"; o estado continua coerente com o histórico (como a RC-09 já faz ao manter o estado e tirar a vigência). `RASCUNHO` só volta para pedido nunca analisado, então "não há volta a `RASCUNHO` depois do primeiro processamento" continua valendo, e a exclusão do rascunho (só em `RASCUNHO`) nunca encontra análise.

**Alternatives considered**: guardar o estado anterior em coluna (mais esquema, e restauraria `FALHA_ANALISE` depois de um descarte, o que não ajuda a paciente); ir a `FALHA_ANALISE` (descarte não é erro, e `FALHA_ANALISE` bloquearia envio de documento novo, que só vale em `RASCUNHO`).

## D5 — Limite de tempo e falhas

**Decision**:
- Limite de **1 minuto** a partir de `ANALISE_INICIADA_EM` (`AnaliseService.LIMITE_EM_ANDAMENTO`). Dentro dele, novo pedido → 409 `ANALISE_EM_ANDAMENTO` (com `Retry-After` em segundos). Passado o limite, `iniciar` assume (novo início = novo token) e registra no log a interrupção; o processamento antigo, se ainda terminar, cai em D2-b e é descartado. A consulta das análises informa `execucao.situacao = INTERROMPIDA`.
- **Falha da avaliação** (`ProtocoloNaoSuportadoException`, erro inesperado ou banco indisponível durante a avaliação): tenta encerrar como `FALHA_ANALISE` (só se o token ainda for o seu), sem gravar resultado, e responde 503 `ANALISE_INDISPONIVEL` (ou 503 `DEPENDENCIA_INDISPONIVEL` se o banco caiu). Se nem o encerramento puder ser gravado, o limite libera o pedido.
- Pedido em `FALHA_ANALISE` pode pedir a análise de novo (fluxo normal).

**Rationale**: critério 4 ("limite de tempo, falha recuperável e opção de tentar novamente") e FR-016 a FR-020 da spec. Um minuto é muito acima da duração esperada e curto o bastante para a demonstração.

**Alternatives considered**: timeout de consulta JDBC (útil, mas não substitui a trava: uma requisição que cai no meio não executa nada depois); job de limpeza periódico (infraestrutura a mais; a tomada da trava vencida no próximo pedido resolve).

## D6 — `CONFERENCIA_EM` como `TIMESTAMP` em UTC

**Decision**: a coluna nova `RC_ANALISE.CONFERENCIA_EM` é `TIMESTAMP(6)` (sem fuso) gravado em UTC, diferente da convenção `TIMESTAMP WITH TIME ZONE` do esquema.

**Rationale**: o Oracle não aceita coluna `TIMESTAMP WITH TIME ZONE` em chave única ou primária (restrições de `CREATE TABLE`/constraint na SQL Language Reference), e a coluna faz parte de `RC_ANALISE_UK_CONFERENCIA`. A gravação e a leitura convertem explicitamente para/de UTC (`LocalDateTime` em UTC), e o comentário no script registra a exceção.

**Alternatives considered**: índice único funcional com `SYS_EXTRACT_UTC` (válido, mas foge do padrão das restrições nomeadas e não pôde ser executado contra Oracle nesta história); sem restrição única (perde a rede de proteção do D3).

## D7 — O que é gravado de cada pendência

**Decision**: `RC_PENDENCIA` ganha `MOTIVO` (lista fechada de `MotivoNaoVerificavel`, nulo quando `PENDENTE`); a tabela nova `RC_PENDENCIA_EVIDENCIA` guarda, por pendência e na ordem do motor, `DOCUMENTO_ID`, `CODIGO` do campo e `SITUACAO` da evidência. **Página e trecho não são copiados**: a consulta os busca na leitura da mesma revisão (`RC_EXTRACAO` é uma por revisão e nunca é alterada), por junção com `RC_EXTRACAO_CAMPO`. `RC_RESULTADO_REGRA` não muda (o motivo do não verificável está na pendência, que sempre existe para ele).

**Rationale**: critério 1 e "saber a qual documento cada resultado se refere"; a decisão de conferência é substituível, então a situação da evidência no momento da análise precisa ser gravada (não pode ser recalculada). Não duplicar o trecho segue `CLAUDE.md` §10 e diminui cópias de conteúdo de documento.

**Alternatives considered**: guardar as evidências em JSON numa coluna (fora do padrão relacional pedido pela disciplina); recalcular tudo a partir da leitura (impossível depois que a conferência muda).

## D8 — Situação da análise no pedido

**Decision**: `analise.situacao` do pedido (`GET/POST/PUT /solicitacoes…`) passa a ser calculada: `EM_ANDAMENTO` quando o estado é `PROCESSANDO`; `CONCLUIDA` (com `resultadoGeral`) quando há análise vigente da revisão atual; senão `NAO_REALIZADA` (significado mantido: nenhuma análise vale para esta versão). A consulta usa só colunas que existem desde a RC-03 (`RC_ANALISE.VIGENTE`, `REVISAO`, `RESULTADO_GERAL`), por subconsulta em `SolicitacaoRepositoryJdbc`. "Interrompida" aparece só na consulta das análises.

**Rationale**: o DTO já prometia esse cálculo "quando a análise existir"; manter `NAO_REALIZADA` fixo seria informação falsa. Não depender do script `10_` nas consultas do pedido mantém o resto da API funcionando num banco sem o script.

## D9 — Banco sem o script `10_`

**Decision**: só as rotas de análise dependem das colunas/tabela novas. `AnaliseRepositoryJdbc` traduz ORA-00904 (coluna inexistente) e ORA-00942 (tabela inexistente) em 503 `DEPENDENCIA_INDISPONIVEL`, com a orientação de executar `database/scripts/10_analise_historico.sql` apenas no log. As alterações em `ConferenciaRepositoryJdbc` e `SolicitacaoRepositoryJdbc` usam só colunas da RC-03.

**Rationale**: mesmo padrão dos scripts `08_` e `09_`.

## D10 — Rotas, códigos de erro e montagem

**Decision**: `POST` e `GET /solicitacoes/{id}/analises`, só `PACIENTE`, entregues pelo `SolicitacaoController` a um `AnaliseController` (como extração e conferência). Códigos novos: 409 `ANALISE_EM_ANDAMENTO` e 503 `ANALISE_INDISPONIVEL`. `Dependencias` ganha `AnaliseRepository`; `Composicao` instancia `AvaliacaoService` (até aqui sem uso em produção) e `AnaliseService`. Sem dependência nova.
