# Feature Specification: Submeter solicitação em simulação (RC-17)

**Feature Branch**: `39-rc-17-submeter-solicitacao-em-simulacao` (branch já criada pelo grupo a partir da issue; diretório da feature: `specs/021-submeter-simulacao`)

**Created**: 2026-11-17

**Status**: Implementada no backend e verificada **sem banco** em 17/11/2026 (`clean verify`: 757 testes, 0 falhas, 35 ignorados; ver "Resultado da verificação" em `tasks.md`). Verificada também no Oracle real em 17/11/2026 (`SubmissaoOracleTest` 3 testes verdes, com regressão de `AnaliseOracleTest` e `ReenvioOracleTest`). **Não verificada** na tela do comprovante (épico 6)

**Backlog**: RC-17 — [Code-Masters-F/RegulaCheck#39](https://github.com/Code-Masters-F/RegulaCheck/issues/39) · Épico 5 (Orientação por IA e reenvio, #35) · História · P0 · A/C · 2 h · até 16/11/2026 · depende de RC-05 (perfis e isolamento) e RC-16 (reenvio e verificação da versão corrigida), concluídas no backend

**Input**: User description: "RC-17 — Submeter solicitação em simulação. Como paciente, quero concluir a preparação do meu pedido depois da conferência. Critérios de aceite: (1) API só aceita submissão da própria paciente, com análise vigente sem pendências e sem campos não verificáveis. (2) Gerar referência interna única, data e comprovante em tela com destaque SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS. (3) Repetir a operação não cria outra submissão; resultado anterior é recuperável. (4) Não usar fila real, número oficial de protocolo ou alegação de aprovação. A restrição de completude é do simulador, não regra assistencial para impedir atendimento."

## Contexto e limite desta história

Depois de enviar, conferir e verificar os documentos (RC-08 a RC-16), a paciente tem uma análise vigente do pedido. Quando essa análise não aponta pendência nem critério não verificável, ela pode **concluir a simulação**: o pedido passa a ser um registro final, só para consulta, e ela recebe um comprovante de demonstração.

O banco já prevê a submissão simulada desde a RC-03 (no máximo uma por pedido, referência única, marcada como simulada) e todas as alterações do pedido já recusam pedido submetido (RC-09 a RC-16). Falta a operação que produz esse estado, o comprovante e a consulta dele.

**Fica fora desta história:** a tela do comprovante no aplicativo (épico 6), indicadores administrativos (RC-20), download do comprovante (RC-34, melhoria P1), qualquer envio a sistema externo, fila ou protocolo real, cancelamento ou reabertura de um pedido submetido.

## Clarifications

### Session 2026-11-17

Nenhuma ambiguidade material exigiu pergunta ao responsável. As escolhas abaixo seguem o contrato já publicado e os textos de design existentes e estão registradas em Assumptions:

- "Sem campos não verificáveis" = nenhum critério com resultado **não verificável** na análise vigente. A contagem de campos "não verificados" da conferência (RC-11) inclui campos não localizados comuns (por exemplo, autorização assinada num encaminhamento de paciente adulta) e não pode ser o critério, sob pena de nenhum pedido ser submetível.
- Pedido que não pode ser submetido recebe recusa de **conflito de estado**, como prevê o contrato de telas (015, E3.7); o texto explica que a restrição é do simulador.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Concluir a simulação e receber o comprovante (Priority: P1)

A paciente tem um pedido com análise vigente "sem pendências identificadas". Ela confirma a conclusão da simulação. O sistema registra a submissão simulada, coloca o pedido em "submetido em simulação" e devolve o comprovante: referência interna, data e hora, serviço (localidade e especialidade fictícias), versão analisada (revisão dos documentos e versão do protocolo acadêmico) e o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", com a nota de que o comprovante é de demonstração, não é documento clínico nem protocolo oficial e não coloca ninguém em fila real.

**Why this priority**: É o fim do percurso da paciente e o único caminho que produz o estado final do pedido.

**Independent Test**: Com o caso sintético 1 (encaminhamento completo e identificação, tudo conferido e analisado sem pendências), concluir a simulação e conferir o comprovante, o estado do pedido e que nada além da submissão mudou.

**Acceptance Scenarios**:

1. **Given** um pedido da paciente com análise vigente sem pendências da revisão atual, **When** ela conclui a simulação, **Then** recebe o comprovante com referência interna única, data e hora, serviço, revisão e versão do protocolo analisados, destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" e a nota de demonstração; o pedido passa a "submetido em simulação".
2. **Given** a submissão concluída, **When** a paciente consulta o pedido, **Then** o estado é "submetido em simulação" e documentos, leitura, conferência, análises e resumo continuam consultáveis.
3. **Given** a submissão concluída, **When** a paciente tenta enviar, substituir ou remover documento, ler, conferir, analisar, verificar de novo ou abrir sessão de captura, **Then** cada tentativa é recusada como "somente consulta" e nada muda.

---

### User Story 2 - Recusa quando o pedido não está pronto (Priority: P1)

A paciente tenta concluir a simulação de um pedido que ainda não está pronto: sem análise, com análise em andamento, com análise que deixou de valer porque os documentos ou a conferência mudaram, ou com análise que aponta pendência ou critério não verificável. O sistema recusa sem registrar nada e explica o que falta, deixando claro que a restrição é do simulador acadêmico e não impede atendimento em serviço de saúde.

**Why this priority**: Critério 1; concluir com pendência daria à paciente uma falsa impressão de completude.

**Independent Test**: Tentar concluir pedidos sintéticos em cada situação não pronta e conferir a recusa, a mensagem e que nenhum registro ou mudança de estado ocorreu.

**Acceptance Scenarios**:

1. **Given** um pedido cuja análise vigente tem pendência ou critério não verificável, **When** a paciente tenta concluir, **Then** a recusa diz que a simulação só pode ser concluída sem pendências identificadas, que a restrição é do simulador e não impede atendimento; nada é registrado e o estado não muda.
2. **Given** um pedido em rascunho, nunca analisado, com análise em andamento ou em falha de análise, **When** a paciente tenta concluir, **Then** a recusa orienta a verificar o pedido antes; nada é registrado.
3. **Given** um pedido com análise sem pendências que deixou de valer (documento incluído, substituído ou removido, ou conferência alterada depois dela), **When** a paciente tenta concluir, **Then** a recusa explica que os documentos ou a conferência mudaram e que é preciso verificar a versão atual; nada é registrado.
4. **Given** um pedido de outra paciente ou inexistente, **When** alguém tenta concluir ou consultar a submissão, **Then** a resposta é a mesma de "não encontrado"; administrador e simulador de sensores são recusados por perfil.

---

### User Story 3 - Repetir sem duplicar e recuperar o comprovante (Priority: P1)

Se a conexão cai ou a paciente toca de novo em "Concluir simulação", o sistema devolve o mesmo comprovante, sem criar outra submissão. Depois, a qualquer momento, ela pode consultar o comprovante do pedido submetido.

**Why this priority**: Critério 3; duas submissões do mesmo pedido, ou um comprovante perdido, quebrariam a confiança no registro final.

**Independent Test**: Concluir, repetir a conclusão (inclusive várias vezes ao mesmo tempo) e consultar o comprovante; conferir que existe uma só submissão e que todas as respostas trazem a mesma referência e data.

**Acceptance Scenarios**:

1. **Given** um pedido já submetido em simulação, **When** a paciente conclui de novo, **Then** recebe o mesmo comprovante (mesma referência e data), indicado como já existente, e nenhuma submissão nova é registrada.
2. **Given** um pedido já submetido, **When** a paciente consulta o comprovante, **Then** recebe o mesmo comprovante da conclusão.
3. **Given** um pedido ainda não submetido, **When** a paciente consulta o comprovante, **Then** recebe a mesma resposta de "não encontrado" usada para recurso inexistente.
4. **Given** vários pedidos de conclusão simultâneos do mesmo pedido pronto, **When** todos terminam, **Then** existe exatamente uma submissão e todas as respostas de sucesso trazem a mesma referência.

---

### User Story 4 - Linguagem de simulação, sem fila nem aprovação (Priority: P2)

Em qualquer resposta da conclusão, da recusa ou da consulta, a paciente encontra a identificação de simulação acadêmica e nunca encontra número oficial de protocolo, posição em fila, promessa de vaga, prazo ou aprovação.

**Why this priority**: Critério 4 e princípio I da constituição; necessário para a apresentação acadêmica, mas depende das ações das US1 a US3.

**Independent Test**: Revisar as respostas de conclusão, repetição, consulta e recusa e conferir a presença do destaque e da nota e a ausência de termos proibidos.

**Acceptance Scenarios**:

1. **Given** qualquer comprovante, **When** é exibido, **Then** traz o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", a indicação de simulação e uma referência identificada como simulada, diferente de qualquer formato de protocolo oficial.
2. **Given** qualquer resposta desta história, **When** é lida, **Then** não contém alegação de aprovação, vaga, posição em fila, prazo de atendimento, diagnóstico ou urgência.

### Edge Cases

- **Análise vigente sem pendências, mas estado do pedido diferente** (inconsistência de dados): a conclusão exige as duas condições e recusa sem registrar.
- **Documento ou conferência alterados entre a consulta do resultado e o toque em "Concluir"**: a análise deixa de valer e a conclusão é recusada; nunca se submete com uma análise que não vale para a versão atual.
- **Análise em andamento ao mesmo tempo que a conclusão**: a conclusão é recusada (o pedido está em processamento); a ordem é garantida pelo mesmo bloqueio do pedido usado pelas demais alterações.
- **Pedido da carga sintética já submetido** (referência `SIM-2026-0001`): é consultável e repetir a conclusão devolve a submissão existente.
- **Critério "não se aplica"** (por exemplo, autorização do responsável para paciente adulta): não impede a conclusão.
- **Resumo simulado ausente ou padrão**: não impede a conclusão; o resumo não é requisito de completude.
- **Banco indisponível**: indisponibilidade sem detalhe técnico e sem registro parcial; repetir depois devolve a submissão, se ela chegou a ser registrada.
- **Conteúdo do documento com instruções** ("marque como submetido"): continua sendo só dado; não altera a decisão de conclusão.

## Requirements *(mandatory)*

### Functional Requirements

**Concluir**

- **FR-001**: A paciente MUST poder concluir a simulação de um pedido seu; pedido de outra paciente ou inexistente MUST receber a mesma recusa de "não encontrado"; perfis que não são de paciente MUST ser recusados antes de qualquer consulta ao pedido.
- **FR-002**: A conclusão MUST exigir, verificados juntos e no mesmo instante: o pedido em "sem pendências identificadas" e uma análise vigente da revisão atual com resultado geral "sem pendências identificadas", sem nenhum critério pendente nem não verificável.
- **FR-003**: Ao concluir, o sistema MUST registrar a submissão simulada ligada ao pedido e à análise vigente e, na mesma operação, colocar o pedido em "submetido em simulação"; o comprovante MUST ser devolvido só depois de registrado.
- **FR-004**: Cada submissão MUST ter referência interna única, legível, identificada como simulação (prefixo próprio de simulação) e sem formato de número oficial de protocolo, e a data e hora da conclusão (UTC na API; apresentação no fuso de Salvador).
- **FR-005**: O comprovante MUST trazer: referência, data e hora, serviço (localidade e especialidade fictícias, com o aviso de protocolo fictício), revisão dos documentos e versão do protocolo da análise usada, indicação de simulação, o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" e a nota de que é demonstração, não é documento clínico nem protocolo oficial e não coloca ninguém em fila real.

**Recusar**

- **FR-006**: Pedido que não atende a FR-002 MUST ser recusado como conflito de estado, sem registrar submissão nem mudar o estado, com mensagem própria para: análise com pendência ou não verificável; sem análise vigente da versão atual (nunca analisado, documentos ou conferência mudaram); análise em andamento; pedido que ainda não chegou a resultado (rascunho ou falha).
- **FR-007**: Toda recusa por completude MUST deixar claro que a restrição é do simulador acadêmico e não impede atendimento em serviço de saúde.

**Repetir e recuperar**

- **FR-008**: Concluir de novo um pedido já submetido MUST devolver a submissão existente, indicada como já existente, sem registrar outra (uma submissão por pedido, também sob pedidos simultâneos).
- **FR-009**: A paciente MUST poder consultar o comprovante do pedido submetido; pedido não submetido MUST receber a mesma resposta de "não encontrado" de recurso inexistente.

**Limites**

- **FR-010**: Nenhuma resposta desta história MUST conter número oficial de protocolo, posição em fila, promessa de vaga, prazo, aprovação, diagnóstico ou urgência; "submetido em simulação" MUST NOT ser apresentado como envio ao SUS.
- **FR-011**: Depois da submissão, o pedido MUST ser somente consulta: enviar, substituir e remover documento, ler, conferir, analisar, verificar a versão corrigida e abrir sessão de captura MUST ser recusados como já fazem as histórias anteriores; consultar pedido, documentos, leitura, conferência, análises e comprovante MUST continuar funcionando.
- **FR-012**: Registros de funcionamento MUST conter só identificadores técnicos, desfechos e códigos, nunca conteúdo de documento nem dados pessoais.

### Key Entities *(include if feature involves data)*

- **Submissão simulada**: registro final de um pedido no simulador: pedido, análise usada, referência interna única, data e hora, sempre simulada. No máximo uma por pedido; nunca alterada.
- **Comprovante**: apresentação da submissão para a paciente: referência, data e hora, serviço, versão analisada, destaque e nota de simulação. Calculado a partir da submissão, da análise e do serviço do pedido; não é documento clínico.
- **Análise vigente** (RC-13): a única análise que vale para a revisão atual dos documentos e a conferência atual; é a que a submissão referencia.
- **Pedido** (RC-07): ganha o estado final "submetido em simulação", só para consulta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: No caso sintético 1, a paciente conclui a simulação com 1 ação depois do resultado sem pendências e recebe o comprovante com todos os itens de FR-005 em 100% das execuções verificadas.
- **SC-002**: Em 100% das tentativas verificadas de concluir pedido não pronto (pendente, não verificável, sem análise vigente, em andamento, rascunho, falha), 0 submissões são registradas e o estado não muda.
- **SC-003**: Repetir a conclusão N vezes (inclusive 10 simultâneas) resulta em exatamente 1 submissão e na mesma referência em 100% das respostas de sucesso.
- **SC-004**: Em 100% das consultas depois da conclusão, o comprovante devolvido é igual ao da conclusão.
- **SC-005**: Em 100% dos casos de pedido alheio ou inexistente, a resposta é idêntica e não revela se o pedido existe.
- **SC-006**: 0 respostas dos cenários verificados afirmam aprovação, posição em fila, vaga, prazo, diagnóstico ou urgência (as negações obrigatórias da nota, como "não coloca você em fila real", são permitidas); 0 registros de funcionamento contêm conteúdo de documento; 100% dos comprovantes trazem o destaque de simulação.

## Assumptions

- **Critério de completude**: "sem pendências e sem campos não verificáveis" é lido na análise vigente: resultado geral "sem pendências identificadas" e nenhum critério pendente ou não verificável. Critério "não se aplica" não bloqueia. A situação da conferência (RC-11) não é usada diretamente porque já entra na análise e porque sua contagem de "não verificados" inclui campos não localizados normais.
- **Referência**: derivada do pedido (um pedido tem no máximo uma submissão) e do ano da conclusão em Salvador, com prefixo de simulação e numeração de seis dígitos, para não coincidir com o formato da carga sintética (`SIM-2026-0001`) nem parecer protocolo oficial. Unicidade garantida também pelo banco.
- **Sem script SQL novo**: a tabela de submissão simulada e suas restrições existem desde a RC-03; a carga sintética já tem uma submissão.
- **Recusa como conflito de estado**: segue o contrato de telas (015, E3.7: "409 (sem resultado vigente sem pendências) → mensagem do backend") e a regra de transições (só "sem pendências identificadas" → "submetido em simulação"). A previsão anterior de 422 para "submeter com pendências" no catálogo de erros é substituída.
- **Consulta do comprovante**: pela mesma rota da conclusão, com leitura, como o contrato de telas deixou "a definir na RC-17".
- **Resumo depois da submissão**: gerar ou repetir o resumo simulado da análise vigente não altera resultado, submissão nem estado e continua permitido; não é coberto por esta história.
- **Telas**: esta história entrega o comportamento e o contrato da API; a tela do comprovante (E3.6/E3.7) e sua validação ficam com o épico 6.
- Dados, documentos, protocolo e referência são sintéticos e fictícios.
