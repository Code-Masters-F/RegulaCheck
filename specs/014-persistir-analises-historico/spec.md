# Feature Specification: Persistir análises e histórico por revisão (RC-13)

**Feature Branch**: `21-rc-13-persistir-análises-e-histórico-por-revisão` (branch já criada pelo grupo a partir da issue; diretório da feature: `specs/014-persistir-analises-historico`)

**Created**: 2026-11-13

**Status**: Implementada e verificada **sem banco** em 13/11/2026 (`clean verify`: 642 testes, 0 falhas, 29 ignorados; ver "Resultado da verificação" em `tasks.md`). Verificada também no Oracle real em 14/11/2026 (`10_` aplicado, `04_` com 32 OK, `AnaliseOracleTest` com 2 testes verdes)

**Backlog**: RC-13 — Code-Masters-F/RegulaCheck#34 (antes Code-Masters-F/ClickBus#21; o backlog foi transferido em 13/11/2026) · Épico 4 (Extração e critérios, #15) · História · P0 · A/B · 3 h · até 16/11/2026 · depende de RC-09 (substituir/remover documento, concluída) e RC-12 (regras do protocolo, concluída)

**Input**: User description: "RC-13 — Persistir análises e histórico por revisão. Como paciente, quero acompanhar minhas conferências e saber a qual documento cada resultado se refere. Critérios de aceite: (1) salvar revisão documental, protocolo/versão, resultados por regra, pendências e data de cada execução; (2) não sobrescrever silenciosamente resultados anteriores; a tela distingue histórico e resultado vigente; (3) alteração durante processamento não permite aplicar resultado antigo à revisão nova; (4) requisição duplicada não cria análises simultâneas indevidas; há limite de tempo, falha recuperável e opção de tentar novamente. Contexto: a RC-12 entregou o motor e a montagem da avaliação, sem rota, sem gravação e sem mudança de estado, e deixou para a RC-13 a gravação, o histórico, a rota de análises, as transições de estado e a publicação como vigente só se a revisão e a conferência ainda forem as atuais. Fora do escopo: resumo por IA simulada (RC-14), tela de resultado (RC-15), comparação entre revisões e reenvio orquestrado (RC-16), submissão simulada (RC-17)."

## Contexto e limite desta história

A RC-10 lê (de forma **simulada**) os documentos de uma versão do pedido; a RC-11 deixa a paciente **confirmar** ou **apontar divergência** em cada campo lido; a RC-12 aplica as regras da versão do protocolo do pedido a essa leitura conferida e devolve, **sem gravar nada**, o resultado de cada regra, as pendências explicadas e o resultado geral. Até aqui a paciente nunca recebe esse resultado e nada fica registrado.

Esta história transforma a avaliação em **análise registrada**:

1. **Pedir a análise**: a paciente pede a análise do seu pedido; o sistema faz a avaliação da RC-12 e **grava** o que saiu dela (revisão dos documentos, versão do protocolo, resultado de cada regra, pendências com a evidência apontando os documentos, data de execução), muda o estado do pedido para o resultado geral e devolve a análise.
2. **Histórico e resultado vigente**: cada análise fica guardada; nenhuma é apagada ou reescrita. No máximo uma é a **vigente** (vale para a versão atual dos documentos e para a conferência atual); as demais são **histórico**.
3. **Nada velho vira vigente**: se os documentos ou a conferência mudarem enquanto a análise é feita, o resultado não é publicado.
4. **Um processamento por vez, com prazo**: um segundo pedido enquanto a análise está em andamento não cria outra; a análise em andamento tem limite de tempo; falhas deixam o pedido num estado do qual a paciente pode tentar de novo.

**Fica fora desta história:** resumo de pendências por IA simulada (RC-14 — os campos de resumo continuam vazios), exibição e orientação na tela (RC-15), reenvio orquestrado, comparação entre revisões e marcação de pendência resolvida (RC-16), submissão simulada (RC-17), telas (épico de telas) e novas regras do protocolo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pedir a análise e guardar o resultado (Priority: P1)

A paciente, depois de ler e conferir os documentos, pede a análise do pedido. O sistema aplica as regras da versão do protocolo do pedido à leitura atual e às decisões de conferência (como na RC-12), **grava** a análise e coloca o pedido no estado correspondente ao resultado geral: `COM_PENDENCIAS`, `REQUER_CONFERENCIA` ou `SEM_PENDENCIAS_IDENTIFICADAS`. A resposta traz a análise gravada: revisão dos documentos avaliada, protocolo e versão, data de execução, resultado geral, resultado de cada regra e as pendências com explicação, responsável e evidência (que documento, que campo, em que situação e, quando houver, de onde veio). Pedir de novo sem que nada tenha mudado devolve a mesma análise, sem criar outra.

**Why this priority**: é o núcleo da história e o primeiro momento em que a paciente recebe o resultado dos critérios; sem a análise gravada não há histórico (US2), resumo (RC-14), tela (RC-15) nem submissão (RC-17).

**Independent Test**: com as amostras sintéticas lidas e conferidas pela API em memória, pedir a análise e conferir o estado do pedido, a resposta e o que ficou gravado; pedir de novo e conferir que nada novo foi criado.

**Acceptance Scenarios**:

1. **Given** um pedido em `RASCUNHO` com o encaminhamento e a identificação sintéticos oficiais lidos e todos os campos identificados confirmados, **When** a paciente pede a análise, **Then** a análise é gravada com resultado geral `SEM_PENDENCIAS_IDENTIFICADAS`, o pedido passa a `SEM_PENDENCIAS_IDENTIFICADAS` e a resposta indica que a análise foi criada agora, que é a vigente, a revisão avaliada, o protocolo `PROTOCOLO-ACADEMICO-FICTICIO` `v0.1`, a data de execução e seis resultados por regra.
2. **Given** a mesma leitura sem nenhuma confirmação, **When** a paciente pede a análise, **Then** a análise gravada tem resultado geral `REQUER_CONFERENCIA`, o pedido passa a `REQUER_CONFERENCIA` e cada regra não verificável tem uma pendência com responsável paciente, evidência apontando os documentos e campos e motivo "transcrição não conferida" (R01 a R05) ou "condição indeterminada" (R06, idade desconhecida, como na RC-12).
3. **Given** uma pendência gravada, **When** a análise é consultada, **Then** cada evidência identifica o documento (identificador e nome do arquivo enviado), o campo, a situação e, quando a leitura tinha, a página e o trecho de origem.
4. **Given** uma análise vigente para a versão atual dos documentos e a conferência atual, **When** a paciente pede a análise de novo (inclusive por toque duplicado), **Then** recebe a mesma análise, indicada como já existente; nenhuma análise nova é gravada e o estado não muda.
5. **Given** um pedido sem leitura, ou com leitura que não é a da versão atual dos documentos, **When** a paciente pede a análise, **Then** o pedido é recusado com a orientação de ler os documentos, nada é gravado e o estado do pedido continua o mesmo.
6. **Given** um pedido de outra paciente ou inexistente, **When** se pede ou consulta a análise, **Then** a resposta é a mesma de "não encontrado".
7. **Given** um pedido já submetido em simulação, **When** a paciente pede a análise, **Then** o pedido é recusado (só consulta) e o histórico continua consultável.

---

### User Story 2 - Consultar o histórico e distinguir o resultado vigente (Priority: P1)

A paciente consulta as análises do pedido: todas as que foram gravadas, da mais recente para a mais antiga, cada uma com a revisão dos documentos, a versão do protocolo, a data de execução, o resultado geral, os resultados por regra e as pendências. No máximo uma está marcada como **vigente** — a que vale para a versão atual dos documentos e para a conferência atual. Quando a paciente envia, substitui ou remove um documento, ou muda uma decisão de conferência, a análise vigente **deixa de ser vigente**, mas continua no histórico, sem alteração. O pedido informa se há análise válida para a versão atual dos documentos.

**Why this priority**: critérios de aceite (1) e (2): sem histórico a paciente não acompanha as conferências e um resultado novo apagaria o anterior.

**Independent Test**: analisar um pedido, substituir um documento (nova revisão), ler, conferir e analisar de novo; consultar o histórico e conferir as duas análises, cada uma com sua revisão, só a nova vigente e a antiga intacta.

**Acceptance Scenarios**:

1. **Given** um pedido com uma análise da revisão 4 e outra, mais recente, da revisão 6, **When** a paciente consulta o histórico, **Then** vê as duas, a da revisão 6 primeiro e marcada como vigente, a da revisão 4 como histórico, cada uma com seus resultados e pendências como foram gravados.
2. **Given** uma análise vigente, **When** a paciente substitui ou remove um documento, **Then** a análise deixa de ser vigente, continua no histórico com os mesmos resultados, e o pedido informa que não há análise válida para a versão atual dos documentos.
3. **Given** uma análise vigente com resultado `REQUER_CONFERENCIA`, **When** a paciente confirma mais campos da mesma leitura, **Then** a análise deixa de ser vigente e uma nova análise pode ser pedida para a mesma versão dos documentos; as duas ficam no histórico, distinguíveis pela data de execução e pelo instante da conferência considerada.
4. **Given** um pedido que nunca foi analisado, **When** a paciente consulta o histórico, **Then** recebe uma lista vazia, sem vigente, e não um erro.
5. **Given** qualquer análise do histórico, **When** é consultada, **Then** ela identifica os documentos avaliados naquela revisão (inclusive os que depois foram substituídos ou removidos, indicados como não disponíveis) e informa que a leitura é simulada e o protocolo é acadêmico e fictício.

---

### User Story 3 - Alteração durante o processamento não publica resultado velho (Priority: P1)

Enquanto a análise está sendo feita, a paciente (em outra tela ou aparelho) pode substituir ou remover um documento ou mudar uma decisão de conferência. O resultado calculado sobre os dados antigos **não** é gravado nem publicado como vigente: a paciente é avisada de que os documentos ou a conferência mudaram durante a análise e pode pedi-la de novo para a versão nova.

**Why this priority**: critério de aceite (3); publicar o resultado antigo como se fosse da versão nova levaria a paciente a corrigir (ou submeter) com base em documentos que já não são os dela.

**Independent Test**: provocar uma substituição de documento e, separadamente, uma decisão de conferência entre o início da análise e a gravação do resultado; conferir que nenhuma análise é gravada, que nenhuma fica vigente e que o estado volta ao de antes.

**Acceptance Scenarios**:

1. **Given** uma análise em andamento, **When** um documento do pedido é substituído antes de o resultado ser gravado, **Then** o resultado não é gravado, a paciente recebe a orientação de que os documentos mudaram durante a análise e o pedido volta ao estado da última análise concluída (ou a `RASCUNHO`, se nunca houve).
2. **Given** uma análise em andamento, **When** a paciente muda uma decisão de conferência da leitura avaliada antes de o resultado ser gravado, **Then** o resultado não é gravado e o tratamento é o mesmo do cenário anterior.
3. **Given** a análise recusada por alteração, **When** a paciente lê, confere e pede a análise de novo, **Then** a nova análise é feita sobre a versão atual e gravada normalmente.

---

### User Story 4 - Pedido repetido, limite de tempo e falha recuperável (Priority: P1)

Enquanto uma análise do pedido está em andamento, outro pedido de análise do mesmo pedido (toque duplicado, duas telas) **não** inicia uma segunda: é recusado com a informação de que já há uma em andamento. A análise em andamento tem **limite de tempo**: passado o limite sem conclusão, ela é considerada interrompida, a consulta informa isso e a paciente pode pedir de novo; um resultado que chegue depois do limite não é publicado. Se a análise falhar (por exemplo, a versão do protocolo não puder ser aplicada), nada parcial é gravado, o pedido vai a `FALHA_ANALISE` e a paciente pode tentar de novo.

**Why this priority**: critério de aceite (4); sem ele dois processamentos simultâneos poderiam gravar duas análises da mesma versão, e um processamento travado deixaria o pedido preso para sempre.

**Independent Test**: com duas requisições simultâneas de análise do mesmo pedido, conferir que só uma análise é gravada; com o relógio avançado além do limite, conferir que um novo pedido é aceito e que o resultado tardio do anterior não é publicado; com uma falha simulada da avaliação, conferir o estado `FALHA_ANALISE`, a ausência de gravação e o sucesso de uma nova tentativa.

**Acceptance Scenarios**:

1. **Given** uma análise do pedido em andamento dentro do limite de tempo, **When** chega outro pedido de análise do mesmo pedido, **Then** ele é recusado com a informação de que a análise está em andamento e nenhuma análise extra é gravada.
2. **Given** vários pedidos de análise simultâneos do mesmo pedido, sem análise vigente, **When** todos terminam, **Then** existe exatamente uma análise nova gravada e vigente; cada requisição recebeu essa análise ou a recusa de "em andamento".
3. **Given** uma análise iniciada há mais tempo que o limite e não concluída, **When** a paciente consulta, **Then** é informada de que a análise foi interrompida e pode ser pedida de novo; **When** pede de novo, **Then** uma nova análise começa.
4. **Given** a análise antiga (interrompida pelo limite) termina depois que outra já começou, **When** tenta gravar o resultado, **Then** o resultado é descartado e não vira vigente nem histórico.
5. **Given** a avaliação falha (versão do protocolo não aplicável ou erro inesperado), **When** a falha acontece, **Then** nenhum resultado parcial é gravado, o pedido vai a `FALHA_ANALISE` e a paciente recebe orientação de tentar de novo; **When** tenta de novo e a causa não existe mais, **Then** a análise é feita e gravada.
6. **Given** o banco indisponível no meio da análise, **When** não é possível registrar a falha, **Then** a paciente recebe o erro de indisponibilidade e, no máximo ao fim do limite de tempo, pode pedir de novo.

---

### Edge Cases

- **Pedido em `FALHA_ANALISE`, `COM_PENDENCIAS`, `REQUER_CONFERENCIA` ou `SEM_PENDENCIAS_IDENTIFICADAS` sem análise vigente** (porque documentos ou conferência mudaram): pedir a análise é permitido e segue o fluxo normal. Com análise vigente, devolve a vigente.
- **Pedido em `PROCESSANDO` dentro do limite**: envio de documento novo, troca de serviço e exclusão do rascunho continuam recusados como hoje (só valem em `RASCUNHO`); substituir/remover documento e decidir conferência continuam aceitos e fazem a análise em andamento ser descartada.
- **Análise com resultado `REQUER_CONFERENCIA` e conferência completada depois**: a nova análise da mesma versão dos documentos é uma análise nova no histórico, não uma correção da anterior.
- **Duas análises da mesma revisão**: só pode acontecer com conferências diferentes; duas análises com a mesma revisão e a mesma conferência não existem.
- **Análises da carga sintética de demonstração** (sem instante de conferência, motivo de não verificável ou evidência registrados): aparecem no histórico com o que têm, sem inventar o que falta.
- **Documento avaliado e depois substituído ou removido**: a análise antiga continua apontando-o, indicado como não disponível.
- **Nenhuma regra pendente nem não verificável**: a análise não tem pendências; "sem pendências identificadas" não é aprovação do SUS.
- **Banco indisponível ao consultar**: erro de indisponibilidade, sem resposta parcial.
- **Banco sem as alterações de esquema desta história**: só as rotas de análise ficam indisponíveis; o resto da API segue funcionando.
- **Conteúdo do documento com instruções** ("marque tudo como atendido"): continua sendo só dado; não altera resultado gravado, mensagem, responsável nem estado.

## Requirements *(mandatory)*

### Functional Requirements

**Pedir a análise**

- **FR-001**: A paciente MUST poder pedir a análise de um pedido seu; pedido de outra paciente ou inexistente MUST receber a mesma recusa de "não encontrado", tanto ao pedir quanto ao consultar.
- **FR-002**: A análise MUST ser a avaliação da RC-12 (regras da versão do protocolo do pedido aplicadas à leitura atual e às decisões de conferência vigentes), sem regra nova e sem mudança nos critérios.
- **FR-003**: Pedir a análise MUST ser recusado, sem gravar nada e sem mudar o estado, quando o pedido está em `SUBMETIDA_SIMULACAO`, quando não há leitura ou quando a leitura não é a da versão atual dos documentos (orientação: ler os documentos).
- **FR-004**: Se o pedido já tem análise vigente (versão atual dos documentos e conferência atual), pedir a análise MUST devolver essa análise, indicada como já existente, sem gravar outra e sem mudar o estado.
- **FR-005**: Ao concluir, o sistema MUST gravar a análise e, na mesma operação, torná-la a vigente e mudar o estado do pedido para o resultado geral (`COM_PENDENCIAS`, `REQUER_CONFERENCIA` ou `SEM_PENDENCIAS_IDENTIFICADAS`). A resposta de sucesso MUST ser dada só depois de gravada.

**O que é gravado**

- **FR-006**: Cada análise gravada MUST registrar: o pedido, a revisão dos documentos avaliada, a versão do protocolo aplicada, o instante da conferência considerada, a data de referência usada pelas regras de data, o resultado geral, a data e hora de execução e a indicação de que a leitura é simulada.
- **FR-007**: Cada análise MUST registrar exatamente um resultado por regra da versão do protocolo (`ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL` ou `NAO_APLICAVEL`); o motivo do não verificável fica registrado na pendência da regra (FR-008).
- **FR-008**: Cada análise MUST registrar uma pendência por regra pendente ou não verificável, com: regra, tipo, explicação, motivo (só não verificável), responsável provável e as evidências (documento, campo, situação). A origem (página e trecho) MUST poder ser mostrada a partir da leitura daquela revisão, sem copiar novamente o conteúdo do documento.
- **FR-009**: A análise gravada MUST ser completa ou não existir: nenhum resultado parcial (sem parte das regras, sem pendências, sem evidências) pode ficar gravado, nem em falha.

**Histórico e vigência**

- **FR-010**: Uma análise gravada MUST NOT ser alterada nem apagada por análises seguintes; a única informação que muda é deixar de ser vigente.
- **FR-011**: Cada pedido MUST ter no máximo uma análise vigente. Uma análise nova vigente MUST fazer a anterior deixar de ser vigente.
- **FR-012**: Enviar, substituir ou remover documento (já assim desde a RC-09) e registrar uma decisão de conferência que mude algo MUST fazer a análise vigente deixar de ser vigente, sem apagá-la nem mudar o estado do pedido. Repetir uma decisão igual não muda nada.
- **FR-013**: A paciente MUST poder consultar as análises do pedido, da mais recente para a mais antiga, **até as 50 mais recentes** (sem paginação no MVP, como a lista de pedidos; cada nova análise exige mudança nos documentos ou na conferência), cada uma com os dados de FR-006 a FR-008, a marcação de vigente ou histórico e os documentos avaliados naquela revisão (identificador, nome do arquivo enviado, tipo declarado e se ainda está disponível).
- **FR-014**: A consulta de um pedido MUST informar a situação da análise para a versão atual dos documentos: nenhuma análise válida, análise em andamento ou análise concluída (com o resultado geral). A consulta das análises MUST informar também se a análise em andamento foi interrompida pelo limite de tempo.
- **FR-015**: Duas análises do mesmo pedido com a mesma revisão e o mesmo instante de conferência MUST NOT existir.

**Processamento, concorrência e limite de tempo**

- **FR-016**: Ao começar, a análise MUST colocar o pedido em `PROCESSANDO`; enquanto houver análise em andamento dentro do limite de tempo, outro pedido de análise do mesmo pedido MUST ser recusado como "em andamento", sem iniciar outra.
- **FR-017**: O resultado MUST ser gravado somente se, no momento da gravação, a análise ainda for a que está em andamento, ainda estiver dentro do limite de tempo (FR-018) e a revisão dos documentos e o instante da conferência ainda forem os avaliados. Se a análise **já não for a que está em andamento** (outra assumiu depois do limite ou ela já foi encerrada), o resultado MUST ser descartado **sem mudar o estado do pedido**. Se a **revisão ou a conferência mudaram**, o resultado MUST ser descartado e o pedido MUST voltar ao estado da última análise concluída, ou a `RASCUNHO` se nunca houve análise concluída. Toda decisão de conferência que muda algo MUST mudar o instante da conferência, mesmo que o relógio esteja parado ou fora de ordem.
- **FR-018**: Uma análise em andamento MUST ter limite de tempo de **1 minuto**. Passado o limite, ela MUST ser tratada como interrompida: a consulta o informa, um novo pedido de análise MUST ser aceito e o resultado tardio da análise interrompida MUST ser descartado (FR-017). Se ninguém assumiu, o resultado tardio também não é gravado e o pedido vai a `FALHA_ANALISE` (falha recuperável).
- **FR-019**: Se a avaliação falhar (versão do protocolo não aplicável ou erro inesperado), o sistema MUST NOT gravar resultado, MUST colocar o pedido em `FALHA_ANALISE` e MUST orientar a paciente a tentar de novo; pedir a análise em `FALHA_ANALISE` MUST ser permitido.
- **FR-020**: Se nem a falha puder ser registrada (banco indisponível), o pedido MUST poder ser analisado de novo no máximo ao fim do limite de tempo, sem intervenção manual.

**Limites, dados e registros**

- **FR-021**: A análise e a consulta MUST indicar que a leitura é simulada e o protocolo é acadêmico e fictício; `SEM_PENDENCIAS_IDENTIFICADAS` MUST NOT ser apresentado como aprovação do SUS; nenhuma mensagem diagnostica, define urgência ou promete vaga.
- **FR-022**: Valores e trechos dos documentos MUST continuar sendo apenas dados: não alteram resultado, mensagem, responsável, estado ou funcionamento.
- **FR-023**: Registros de funcionamento (logs) MUST conter só identificadores técnicos, códigos, resultados e motivos, nunca valores ou trechos dos documentos.
- **FR-024**: Esta história MUST NOT gerar resumo por IA (os campos de resumo continuam vazios até a RC-14), MUST NOT submeter o pedido e MUST NOT marcar pendências como resolvidas.

### Key Entities *(include if feature involves data)*

- **Análise**: uma execução concluída e gravada da avaliação de um pedido: revisão dos documentos, versão do protocolo, instante da conferência considerada, data de referência, resultado geral, data de execução, vigente ou histórico, leitura simulada. Pertence a um pedido; nunca é reescrita.
- **Resultado de regra (gravado)**: a regra da versão e seu resultado naquela análise, com o motivo quando não verificável.
- **Pendência (gravada)**: regra, tipo, explicação, motivo, responsável e evidências, para cada resultado pendente ou não verificável da análise.
- **Evidência (gravada)**: documento, campo e situação considerados numa pendência; a origem (página, trecho) vem da leitura daquela revisão.
- **Análise em andamento**: o pedido em `PROCESSANDO` com o instante de início, que define o limite de tempo; não é uma análise gravada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das análises concluídas verificadas, o que fica gravado (revisão, versão do protocolo, data, um resultado por regra — 6 no protocolo v0.1 —, pendências com evidências) é igual ao que a avaliação da RC-12 produz para os mesmos dados, e o estado do pedido é igual ao resultado geral.
- **SC-002**: Depois de N análises de um pedido (N ≤ 50), o histórico mostra N análises com os dados originais e no máximo 1 vigente (0 sobrescritas, 0 apagadas); acima de 50, mostra as 50 mais recentes e as demais continuam gravadas.
- **SC-003**: Em 100% das alterações (substituir/remover documento, mudar decisão) feitas durante uma análise verificadas, 0 resultados antigos são gravados ou publicados como vigentes.
- **SC-004**: Com 10 pedidos de análise simultâneos do mesmo pedido, existe exatamente 1 análise nova gravada.
- **SC-005**: Repetir o pedido de análise sem mudança nos documentos ou na conferência grava 0 análises novas em 100% das tentativas verificadas.
- **SC-006**: Uma análise interrompida (sem conclusão) deixa de bloquear novos pedidos de análise em no máximo 1 minuto, e uma análise que falha deixa o pedido em `FALHA_ANALISE` com nova tentativa bem-sucedida assim que a causa deixa de existir.
- **SC-007**: Pedidos de outra paciente e inexistentes recebem a mesma recusa em 100% das tentativas verificadas, ao pedir e ao consultar.
- **SC-008**: 0 registros de funcionamento contêm valor ou trecho de documento nos cenários verificados.

## Assumptions

- **Análise síncrona**: no MVP a avaliação é rápida e não chama serviço externo (leitura e IA são simuladas), então o pedido de análise espera a conclusão e responde com a análise gravada. `PROCESSANDO` existe durante esse tempo e é o que impede um segundo processamento simultâneo; o limite de tempo cobre a requisição que trava ou cai no meio.
- **Limite de tempo de 1 minuto**: valor demonstrativo, muito acima da duração esperada de uma análise; definido como constante documentada.
- **Estado depois de um descarte**: o "estado de antes" é definido como o resultado da última análise concluída do pedido, ou `RASCUNHO` se nunca houve, o que mantém o pedido coerente com o histórico mesmo quando o descarte acontece depois de uma falha.
- **Decisão de conferência invalida a análise vigente**: é a compensação registrada pela RC-11 (a decisão não cria revisão, então a análise precisa guardar o instante da conferência e deixar de valer quando ela muda). Isso acrescenta à RC-11 apenas o efeito "a análise vigente deixa de ser vigente", sem mudar o estado do pedido nem o contrato da conferência.
- **Mais de uma análise por revisão**: necessária porque a conferência não cria revisão (RC-11); a identificação de cada análise passa a ser revisão + instante da conferência.
- **Estado mantido depois de alteração**: como decidido na RC-09, alterar documento (ou, agora, a conferência) não muda o estado do pedido, só tira a vigência; quem dispara a nova análise é a paciente (ou, no futuro, o fluxo de reenvio da RC-16).
- **Pedir a análise em qualquer estado não final**: necessário para haver histórico por revisão (analisar de novo depois de corrigir); a comparação entre revisões e a marcação de pendência resolvida continuam na RC-16.
- **Evidências sem cópia do conteúdo**: a evidência gravada guarda documento, campo e situação; página e trecho vêm da leitura gravada daquela revisão, que nunca é alterada.
- Depende da leitura (RC-10), da conferência (RC-11), da avaliação (RC-12), da regra de dona (RC-05) e da invalidação por alteração de documento (RC-09). Exige uma alteração aditiva do esquema, entregue como script SQL próprio e refletida no script de criação.
- Todos os dados são sintéticos; o protocolo é acadêmico e fictício.
