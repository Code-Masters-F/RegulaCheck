# Research: Submeter solicitação em simulação (RC-17)

Decisões técnicas que resolvem o "como" da [spec](spec.md). Nenhum `NEEDS CLARIFICATION` permanece.

## D1. Rotas

- **Decision**: `POST /solicitacoes/{id}/submissoes-simuladas` (conclui; sem corpo) e `GET` na mesma rota (consulta o comprovante). Privadas de `PACIENTE`, entregues pelo `SolicitacaoController` a um `SubmissaoController` (mesmo padrão de análises, resumo e reenvio). `POST`: 201 ao registrar, 200 se o pedido já tinha submissão (mesmo comprovante). `GET`: 200 com o comprovante; 404 idêntico se o pedido não existe, é de outra paciente ou ainda não foi submetido.
- **Rationale**: A rota `POST` já está no `CONTRATO_API.md`, no catálogo de dados e no contrato de telas (015, E3.7). O contrato de telas deixou a consulta "a definir na RC-17"; `GET` na mesma coleção segue o padrão de `…/extracoes` e `…/analises`. 404 para "nunca submetido" repete a decisão da RC-10 (`GET …/extracoes` sem leitura).
- **Alternatives considered**: `GET /solicitacoes/{id}` com o comprovante embutido — mistura o retrato do pedido com o comprovante; 200 com corpo vazio para "não submetido" — exigiria mais um formato.

## D2. Decisão atômica sob o bloqueio do pedido

- **Decision**: `SubmissaoRepository.submeter` faz tudo numa transação curta com `SELECT … FOR UPDATE` em `RC_SOLICITACAO` (o mesmo bloqueio de envio, conferência, análise e captura): dona; submissão já existente → devolve (idempotência); estado `SUBMETIDA_SIMULACAO` sem registro → somente consulta; `PROCESSANDO` → em andamento; análise vigente da revisão atual (senão, sem análise vigente); `RESULTADO_GERAL = SEM_PENDENCIAS_IDENTIFICADAS` e zero resultados `PENDENTE`/`NAO_VERIFICAVEL` (senão, com pendências); estado `SEM_PENDENCIAS_IDENTIFICADAS` (senão, dado incoerente: sem análise vigente); então `INSERT RC_SUBMISSAO`, `UPDATE RC_SOLICITACAO SET ESTADO = 'SUBMETIDA_SIMULACAO'` e leitura do comprovante, com um único `COMMIT`. `ORA-00001` em `RC_SUBMISSAO_UK_SOLIC` (inserção por fora do bloqueio) → desfaz e devolve a existente.
- **Rationale**: Envio, substituição, remoção e decisão de conferência tiram a vigência sob o mesmo bloqueio; conferir vigência e estado no mesmo instante da gravação impede submeter com análise que deixou de valer (edge case "alterado entre a consulta e o toque"). A restrição única por pedido (RC-03) é a rede de proteção contra duplicata.
- **Alternatives considered**: checar no Service e gravar depois — janela de corrida entre a checagem e a gravação (constituição VI); confiar só no estado do pedido — o estado não muda quando a análise perde a vigência (decisão da RC-09).

## D3. Critério de completude

- **Decision**: análise **vigente** da **revisão atual**, `RESULTADO_GERAL = SEM_PENDENCIAS_IDENTIFICADAS`, nenhum resultado por regra `PENDENTE` ou `NAO_VERIFICAVEL` (redundante com o resultado geral, conferido por defesa) e pedido em `SEM_PENDENCIAS_IDENTIFICADAS`. `NAO_APLICAVEL` não bloqueia. Resumo não é exigido.
- **Rationale**: Leitura da spec (Assumptions): o motor é a autoridade; a conferência já entra na análise e sua contagem de "não verificados" inclui não localizados normais.
- **Alternatives considered**: exigir conferência `CONFERIDA` — tornaria impossível submeter casos normais; exigir resumo — o resumo é explicação, não critério.

## D4. Referência interna

- **Decision**: `SIM-{ano em Salvador}-{id do pedido com no mínimo 6 dígitos}` (ex.: `SIM-2026-000012`), calculada pelo Service antes de gravar.
- **Rationale**: Um pedido tem no máximo uma submissão, então o id do pedido já garante unicidade (e o banco tem `RC_SUBMISSAO_UK_REF`). Seis dígitos nunca coincidem com o formato de quatro da carga (`SIM-2026-0001`). O prefixo `SIM` e o hífen deixam claro que não é protocolo oficial. Determinística: repetir a operação, inclusive depois de falha de rede, leva à mesma referência. O id do pedido já é público para a dona (aparece na URL), então não há dado novo exposto.
- **Alternatives considered**: sufixo aleatório — exige tratar colisão e não acrescenta proteção; numeração sequencial global — exige contador próprio (tabela ou sequência nova, script SQL) sem ganho para o MVP; `RC_SUBMISSAO.ID` — só existe depois do `INSERT` e a coluna é `NOT NULL`.

## D5. Recusa como 409

- **Decision**: todas as recusas de pedido não pronto são `409 CONFLITO_ESTADO`, com mensagens distintas (pendências; sem análise vigente; em andamento; já submetido sem registro). As de completude dizem que a restrição é do simulador e não impede atendimento.
- **Rationale**: Contrato de telas (015, E3.7) e regra de transições ("só `SEM_PENDENCIAS_IDENTIFICADAS` → `SUBMETIDA_SIMULACAO`"); a operação não tem corpo, então não há entrada "semanticamente inválida" para justificar 422. Substitui a previsão de 422 do catálogo de erros.
- **Alternatives considered**: 422 `REGRA_DE_NEGOCIO` para pendências — previsão antiga, sem consumidor; mantê-la deixaria dois códigos para a mesma família de recusa.

## D6. Comprovante

- **Decision**: lido do banco na mesma transação (`RC_SUBMISSAO` ⋈ `RC_ANALISE` ⋈ `RC_PROTOCOLO`, filtrado pela dona): referência, `SUBMETIDA_EM`, id e revisão da análise, identificador e versão do protocolo, localidade, especialidade e aviso fictício. O DTO acrescenta `simulada: true`, o destaque fixo "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" e a nota de `mobile/design/textos.md` (E3.6).
- **Rationale**: Comprovante igual na conclusão, na repetição e na consulta (SC-004), vindo de dados gravados. Textos fixos no servidor evitam que o cliente invente linguagem.

## D7. Dependências e testes

- **Decision**: novo `SubmissaoRepository` (interface, por ser fronteira com o banco), `SubmissaoRepositoryJdbc` e dobro em memória sobre os dobros de pedido e análise (mesmo monitor = bloqueio); `Dependencias` ganha `submissoes`. Testes: unidade da referência; rota em memória (percurso, recusas por situação, idempotência, consulta, 10 simultâneos, autorização, somente consulta depois, higiene de log e termos proibidos); `SubmissaoOracleTest` opt-in (gravação, repetição, consulta, 4 simultâneos sob bloqueio real, leitura da submissão da carga).
- **Rationale**: Mesmo padrão das RC-13/RC-21; prioridades da constituição VII (autorização entre contas, concorrência/idempotência, revisão desatualizada).
