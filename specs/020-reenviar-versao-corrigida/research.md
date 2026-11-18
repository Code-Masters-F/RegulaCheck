# Research: Reenviar e conferir a versão corrigida (RC-16)

Decisões técnicas que resolvem o "como" da [spec](spec.md). As duas decisões de contrato (inclusão após a análise e ação única) foram tomadas pelo responsável pela história em 17/11/2026 (ver Clarifications da spec). Nenhum `NEEDS CLARIFICATION` permanece.

## D1. Ação única de verificação: rota e resposta

- **Decision**: `POST /solicitacoes/{id}/reenvio`, sem corpo, rota privada de `PACIENTE` dentro do registro `/solicitacoes/*` (mesma entrega de subcaminho de análises e resumo). Respostas: **201** quando esta chamada registrou a nova análise; **200** quando devolveu a análise vigente já existente (nada mudou) ou quando parou na conferência. O corpo sempre traz `etapa` (`CONFERENCIA`, `RESUMO` ou `CONCLUIDA`), a revisão, a leitura e uma `orientacao` textual; a `analise` (com resumo e comparação) vem quando houve análise.
- **Rationale**: O critério 1 pede que o reenvio "execute novamente" as etapas. Uma ação que orquestra as rotas já existentes, sem duplicar suas regras, entrega isso e deixa a paciente com um ponto único de "verificar de novo". `etapa` diz o próximo passo de forma estável para o cliente.
- **Alternatives considered**: (a) só etapas separadas — rejeitada pelo responsável; (b) reenvio multipart que troca o arquivo e processa na mesma requisição — mistura upload com resultado (o critério 2 separa os dois momentos) e duplicaria a substituição da RC-09; (c) 409 quando parar na conferência — não é erro, é o fluxo esperado depois de uma leitura nova.

## D2. Ordem e reaproveitamento das etapas

- **Decision**: `ReenvioService` (pacote novo `br.com.regulacheck.reenvio`) executa: (1) `SolicitacaoService.buscarPropria` (404 idêntico; 409 se submetido, sem etapa); (2) `ExtracaoService.ler` (devolve a leitura existente da revisão ou lê de novo); (3) `ConferenciaService.consultar` da leitura: `DESATUALIZADA` → 409 na etapa leitura; algum campo identificado sem decisão (`camposSemDecisao > 0`, em qualquer documento) → para com `etapa=CONFERENCIA`; senão segue (a contagem, e não a situação, decide, porque `REQUER_CONFERENCIA` tem precedência sobre `PENDENTE` e esconderia campos sem decisão de outro documento); (4) `AnaliseService.analisar` (todas as garantias da RC-13); (5) `ResumoService.gerar` (todas as garantias da RC-14).
- **Rationale**: Reaproveita os casos de uso e suas proteções (bloqueio, descarte por revisão/conferência, limite de 1 minuto, idempotência, fallback) sem reimplementar regra. Parar em `PENDENTE` evita gravar no histórico uma análise "não conferida" que, comparada com a anterior, mostraria critérios atendidos como "nova" pendência só porque a leitura nova ainda não foi conferida. `REQUER_CONFERENCIA` da conferência (ilegível, não lido, divergência) não se resolve conferindo, então a análise registra os não verificáveis com motivo.
- **Alternatives considered**: sempre analisar (como `POST …/analises`) — gera ruído no histórico e na comparação; copiar decisões de conferência de documentos não alterados — fora do escopo (spec, Assumptions), exigiria registrar a origem da decisão no banco.

## D3. Pacote do orquestrador

- **Decision**: `br.com.regulacheck.reenvio` com `controller`, `service` e `dto`.
- **Rationale**: O resumo (`resumo`) já depende de `analise`; colocar o orquestrador em `analise` criaria dependência circular `analise ↔ resumo`. Um pacote por funcionalidade segue o `CLAUDE.md` (§5–6).
- **Alternatives considered**: `analise.service.ReenvioService` (ciclo), `solicitacao` (mistura rascunho com análise).

## D4. Comparação calculada, não gravada

- **Decision**: `ComparacaoDeAnalises` (record em `analise.service`, função pura) compara uma análise com a registrada imediatamente antes dela no mesmo pedido, por código de regra, na ordem da análise atual: `RESOLVIDA` (anterior `PENDENTE`/`NAO_VERIFICAVEL` → atual `ATENDE`/`NAO_APLICAVEL`), `CONTINUA` (pendência nas duas), `NOVA` (sem pendência antes, pendência agora). Regra sem pendência nas duas não aparece. Sem anterior, ou com protocolo/versão diferentes, não há comparação (`null`, omitido no JSON). Regra presente só em uma das análises (não ocorre na mesma versão) não é comparada, para nunca declarar "resolvida" uma regra que não foi avaliada.
- **Rationale**: As análises são imutáveis (RC-13); derivar a comparação delas garante que nada é "resolvido" pelo upload (critério 2) e que o histórico não muda (critério 3). Sem tabela nova, sem script SQL.
- **Alternatives considered**: gravar a marcação "resolvida" na pendência antiga — reescreveria análise gravada (viola RC-13 FR-010); comparar com a última análise de **outra revisão** — a tela compara com "a verificação anterior" (015, E3.5), e a mesma revisão pode ter duas análises por mudança de conferência.

## D5. Onde a comparação aparece

- **Decision**: Novo campo opcional `comparacao` em `AnaliseResponse`, preenchido em `POST …/analises`, `GET …/analises` (cada análise com a sua anterior; a consulta lê `LIMITE_HISTORICO + 1` para a mais antiga devolvida ter anterior), `POST …/resumo` e `POST …/reenvio`. O serviço calcula; o DTO só mapeia.
- **Rationale**: Mantém um único formato de análise para todas as rotas (como a RC-15 fez com `rotuloResultado`/`orientacao`); mudança aditiva, compatível com clientes atuais.
- **Alternatives considered**: rota `GET …/analises/comparacao` — mais uma rota e o cliente teria de cruzar dados.

## D6. Inclusão de documento após a análise

- **Decision**: `POST …/documentos` passa a aceitar qualquer estado exceto `SUBMETIDA_SIMULACAO` (como `PUT`/`DELETE` da RC-09). No JDBC, `registrarBloqueado` troca a recusa "fora de rascunho" pela de "submetido" e chama `invalidarAnalise` na mesma transação; o repositório em memória faz o mesmo. O estado do pedido não muda. `ResultadoRegistro.FORA_DE_RASCUNHO` deixa de existir (sem uso). Em `PROCESSANDO`, a inclusão é aceita e a análise em andamento é descartada pela verificação de revisão da RC-13.
- **Rationale**: Decisão do responsável; sem ela, pendência de documento ausente não tem correção após a primeira análise (limitação registrada em 015 e RC-15). Reaproveita conferência de arquivo, limite, chave de tentativa e revisão já existentes.
- **Alternatives considered**: liberar só nos estados de resultado e recusar `PROCESSANDO` — regra diferente da substituição sem ganho (a RC-13 já descarta a análise velha).

## D7. Erros com etapa e o que repetir

- **Decision**: Falhas da verificação mantêm `status` e `codigo` da etapa que falhou (`SEM_DOCUMENTOS`, `LEITURA_INDISPONIVEL`, `CONFLITO_ESTADO`, `ANALISE_EM_ANDAMENTO`, `ANALISE_INDISPONIVEL`, `DEPENDENCIA_INDISPONIVEL`), com `mensagem` própria do reenvio (o que foi preservado e o que repetir) e `detalhes: [{"campo": "etapa", "mensagem": "LEITURA" | "CONFERENCIA" | "ANALISE"}]` (`CONFERENCIA` só para indisponibilidade do banco ao consultar a conferência). `404` continua idêntico (sem detalhe). `ANALISE_EM_ANDAMENTO` mantém o `Retry-After`. Falha do resumo **depois** da análise não é erro: resposta 201/200 com `etapa=RESUMO`, a análise sem resumo e a orientação de pedir o resumo de novo.
- **Rationale**: Critério 4 ("explica o que precisa ser repetido") sem novo código de erro nem mudança no formato padrão (`docs/api/erros.md`). A análise registrada nunca é desfeita (RC-14 FR-007).
- **Alternatives considered**: código novo `REENVIO_FALHOU` — perderia a distinção que o cliente já trata por código.

## D8. Testes

- **Decision**: Unitário puro para `ComparacaoDeAnalises`; testes de rota em memória (`ServidorDeTeste`, repositórios em memória, leitor simulado) para o percurso completo, parada na conferência, idempotência, autorização (401/403/404), submetido, inclusão após análise, falhas por etapa (leitor que falha, avaliação que falha, mudança durante a análise, resumo que perde a vigência), concorrência (10 pedidos simultâneos) e higiene de log. Teste Oracle opt-in para a inclusão após a análise no JDBC (`registrarBloqueado` + `invalidarAnalise`), executado só com autorização para usar a conta compartilhada.
- **Rationale**: Prioridades da constituição (VII): revisões desatualizadas, concorrência/idempotência, falha e autorização entre contas.
