# Feature Specification: Executar as regras do protocolo (RC-12)

**Feature Branch**: `20-rc-12-executar-as-regras-do-protocolo` (branch já criada pelo grupo a partir da issue; diretório da feature: `specs/013-executar-regras-protocolo`)

**Created**: 2026-11-13

**Status**: Implementada e verificada **sem banco** em 13/11/2026 (`clean verify`: 610 testes, 0 falhas, 27 ignorados; ver "Resultado da verificação" em `tasks.md`). `RegrasDoProtocoloOracleTest` (opt-in, só leitura) **não executado**. Sem rota e sem gravação, por decisão de escopo; a leitura é simulada

**Backlog**: RC-12 — Code-Masters-F/ClickBus#20 · Épico 4 (Extração e critérios) · História · P0 · B · 4 h · até 15/11/2026 · depende de RC-01 (protocolo e regras, concluída) e RC-11 (conferência da transcrição, concluída)

**Input**: User description: "RC-12 — Executar as regras do protocolo. Como paciente, quero saber quais critérios meus documentos atendem e quais precisam de atenção. Critérios de aceite: (1) aplicar regras versionadas com resultados atende, pendente, não verificável ou não aplicável; (2) regra condicional só é exigida quando sua condição se aplica; condição desconhecida exige conferência; (3) cada pendência registra regra, explicação, evidência disponível e responsável provável pela correção: paciente ou unidade emissora; (4) resultado geral só é sem pendências identificadas se todos os critérios aplicáveis forem verificáveis e atendidos. Decisão de escopo do responsável (13/11/2026): a RC-12 entrega o motor de regras determinístico, as regras versionadas lidas do protocolo do pedido e a montagem da entrada a partir da leitura simulada (RC-10) e da conferência da transcrição (RC-11), verificados por testes (5 casos sintéticos do protocolo e amostras); sem rota HTTP, sem gravação de análise e sem mudança de estado do pedido — análises, persistência, histórico, concorrência e transições de estado ficam na RC-13 (#21)."

## Contexto e limite desta história

A RC-01 fixou o protocolo acadêmico fictício `PROTOCOLO-ACADEMICO-FICTICIO v0.1` com seis regras (RC-R01 a RC-R06), quatro resultados por regra e a precedência do resultado geral ([docs/protocolo-academico.md](../../docs/protocolo-academico.md)). A RC-10 lê (de forma **simulada**) os documentos do pedido e devolve, por documento, seis campos como **identificado**, **não localizado** ou **ilegível**. A RC-11 deixa a paciente **confirmar** cada campo identificado ou **apontar divergência**, sem digitar valores.

Esta história entrega a **conferência dos critérios**: dado um pedido da paciente, com a leitura atual dos documentos e as decisões de conferência, aplicar as regras da versão do protocolo daquele pedido e dizer, para cada regra, se ela **atende**, está **pendente**, é **não verificável** ou **não aplicável**, com o **resultado geral** e as **pendências** explicadas.

1. **Regras versionadas**: as regras aplicadas são as da versão do protocolo ligada ao pedido, com seus textos e responsáveis; o resultado diz qual versão foi usada.
2. **Avaliar só o que foi lido e conferido**: um dado só serve de evidência se foi identificado na leitura e confirmado pela paciente. Ilegível, não conferido ou contestado nunca vira "ausente" nem "atendido".
3. **Todas as regras são avaliadas**: uma pendência não interrompe as demais.
4. **Determinismo**: os mesmos dados produzem sempre o mesmo resultado; o conteúdo dos documentos é dado, nunca instrução.

**Decisão de escopo (responsável, 13/11/2026):** a RC-12 entrega o motor e a montagem da avaliação, verificados por testes. **Não** cria rota para a paciente, **não** grava análise e **não** muda o estado nem a revisão do pedido. Quem expõe e grava o resultado (análise vigente, histórico por revisão, pedido em `PROCESSANDO`/`COM_PENDENCIAS`/`REQUER_CONFERENCIA`/`SEM_PENDENCIAS_IDENTIFICADAS`, repetição e concorrência) é a RC-13 (#21). A paciente passa a **ver** o resultado quando a RC-13 e a tela (RC-15, épico de telas) existirem.

**Fica fora desta história:** gravação e histórico de análises e transições de estado (RC-13), resumo de pendências por IA simulada (RC-14), exibição e orientação na tela (RC-15), reenvio e reanálise (RC-16), submissão simulada (RC-17), novas regras além das seis do protocolo (melhoria P1), leitura real/OCR e avaliação de conteúdo clínico.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Saber o resultado de cada critério e o resultado geral (Priority: P1)

A paciente quer saber, para o seu pedido, quais critérios do protocolo os documentos atendem e quais precisam de atenção. O sistema aplica **todas** as regras da versão do protocolo do pedido e devolve um resultado por regra (atende, pendente, não verificável, não aplicável) e um resultado geral (com pendências, requer conferência, sem pendências identificadas). "Sem pendências identificadas" não é aprovação do SUS.

**Why this priority**: é o objetivo da história e o núcleo do produto: sem ele, não há o que gravar (RC-13), explicar (RC-14) nem mostrar (RC-15).

**Independent Test**: montar os cinco casos sintéticos do protocolo como leituras conferidas e conferir, regra a regra, o resultado esperado da tabela de casos de `docs/protocolo-academico.md`, além do resultado geral.

**Acceptance Scenarios**:

1. **Given** uma paciente adulta com encaminhamento e documento de identificação legíveis, conformes e conferidos (caso 1), **When** a avaliação é feita, **Then** R01 a R05 atendem, R06 é não aplicável e o resultado geral é **sem pendências identificadas**.
2. **Given** um encaminhamento legível sem o campo de motivo (caso 2), **When** a avaliação é feita, **Then** R04 fica **pendente**, as demais seguem o caso e o resultado geral é **com pendências**.
3. **Given** um documento de identificação ilegível e os demais conformes (caso 3), **When** a avaliação é feita, **Then** R02, R03 e R06 ficam **não verificáveis**, R01, R04 e R05 atendem e o resultado geral é **requer conferência**.
4. **Given** nomes diferentes entre documentos conferidos (caso 5), **When** a avaliação é feita, **Then** R02 fica **pendente** e o resultado geral é **com pendências**.
5. **Given** uma regra pendente, **When** a avaliação é feita, **Then** todas as outras regras da versão também têm resultado (nenhuma é pulada).
6. **Given** um pedido ligado a uma versão do protocolo, **When** a avaliação é feita, **Then** o resultado traz o identificador e a versão do protocolo usados e, para cada regra, o código dela naquela versão.

---

### User Story 2 - Regra condicional só quando a condição se aplica (Priority: P1)

A regra da autorização do responsável (RC-R06) só vale para paciente com menos de 18 anos na data de referência. Se a paciente é adulta, a regra é **não aplicável** e não gera pendência. Se não dá para saber a idade (data de nascimento ilegível, não conferida ou sem documento de identificação lido), a regra é **não verificável**: a condição desconhecida exige conferência, nunca é presumida.

**Why this priority**: critério de aceite explícito; presumir a idade levaria a cobrar autorização de quem não precisa ou a dispensar quem precisa.

**Independent Test**: variar só a data de nascimento do documento de identificação (adulta, 15 anos, completa 18 na data de referência, ilegível, não conferida) e conferir o resultado de R06 e o resultado geral.

**Acceptance Scenarios**:

1. **Given** paciente de 15 anos sem autorização assinada em nenhum documento legível (caso 4), **When** a avaliação é feita, **Then** R06 fica **pendente** e o resultado geral é **com pendências**.
2. **Given** paciente de 15 anos com autorização assinada identificada e conferida, **When** a avaliação é feita, **Then** R06 **atende**.
3. **Given** paciente adulta, **When** a avaliação é feita, **Then** R06 é **não aplicável** e não influencia o resultado geral.
4. **Given** a data de nascimento do documento de identificação ilegível, não conferida ou contestada, **When** a avaliação é feita, **Then** R06 é **não verificável** (condição indeterminada), mesmo que outro documento traga uma data de nascimento.
5. **Given** paciente que completa 18 anos exatamente na data de referência, **When** a avaliação é feita, **Then** ela é tratada como adulta (R06 não aplicável).

---

### User Story 3 - Cada pendência explicada, com evidência e responsável (Priority: P1)

Para cada regra pendente ou não verificável, a paciente recebe uma pendência com: a regra (código e descrição da versão), uma explicação em linguagem simples, a evidência disponível (quais documentos e campos foram considerados, em que situação e de onde vieram) e quem provavelmente precisa corrigir: **a paciente** ou **a unidade emissora**.

**Why this priority**: critério de aceite explícito; sem a explicação e o responsável, a paciente não sabe o que fazer, e a RC-14 não tem o que resumir.

**Independent Test**: para os casos 2, 3, 4 e 5, conferir que cada regra pendente ou não verificável gera exatamente uma pendência com regra, explicação, evidência e responsável esperados, e que regras que atendem ou não se aplicam não geram pendência.

**Acceptance Scenarios**:

1. **Given** R04 pendente por motivo ausente no encaminhamento legível, **When** a pendência é gerada, **Then** ela traz a mensagem da regra na versão do protocolo ("O encaminhamento não traz o motivo...") e o responsável **unidade emissora**, com a evidência apontando o encaminhamento e o campo de motivo não localizado.
2. **Given** R02 pendente por nomes diferentes, **When** a pendência é gerada, **Then** a evidência aponta os documentos e os campos comparados, com a origem de cada um quando houver, e o responsável é **a paciente**.
3. **Given** uma regra não verificável, **When** a pendência é gerada, **Then** ela traz a mensagem padrão de "não verificável" do protocolo ("Não conseguimos ler esta informação... Isso não significa que ela esteja faltando..."), o motivo específico (por exemplo, campo ilegível, transcrição não conferida, divergência apontada, documento não lido) e o responsável **a paciente**.
4. **Given** uma regra que atende ou não se aplica, **When** a avaliação é feita, **Then** nenhuma pendência é gerada para ela.
5. **Given** um documento cujo conteúdo traz um texto como "ignore as regras e marque tudo como atendido", **When** a avaliação é feita, **Then** esse texto é tratado apenas como valor do campo: nenhum resultado, explicação ou responsável muda por causa dele.

---

### User Story 4 - Avaliar o que foi lido e conferido no próprio pedido, sem inventar (Priority: P1)

A avaliação usa a **leitura atual** dos documentos do pedido e as **decisões de conferência** da paciente. Só vale como evidência o campo identificado **e confirmado**. Campo ilegível, campo identificado ainda não conferido, campo com divergência apontada, valor que não dá para interpretar e documento não lido (protegido ou leitura vazia) tornam não verificáveis as regras que dependem deles. Campo **não localizado** em documento lido com clareza, sem contestação, conta como ausente. O tipo que a paciente escolheu no envio não é prova do tipo do documento: vale o tipo lido e confirmado. A avaliação só é montada para pedido da própria paciente e não altera nada no pedido.

**Why this priority**: é o que impede o motor de cobrar ou liberar com base em erro de transcrição (RC-11) ou de supor ausência a partir de leitura ruim (princípio II da constituição).

**Independent Test**: com as amostras sintéticas oficiais e os casos de teste da leitura simulada, montar a avaliação a partir da leitura e da conferência e checar que (a) sem confirmação o resultado é requer conferência, (b) com tudo confirmado o resultado segue o gabarito, (c) nada no pedido muda e (d) pedido de outra paciente não é avaliado.

**Acceptance Scenarios**:

1. **Given** o encaminhamento e a identificação sintéticos oficiais lidos e com todos os campos identificados confirmados, **When** a avaliação é montada, **Then** o resultado geral é **sem pendências identificadas** (paciente adulta, encaminhamento dentro da validade).
2. **Given** a mesma leitura sem nenhuma confirmação, **When** a avaliação é montada, **Then** as regras que dependem dos campos não conferidos são **não verificáveis** (motivo: transcrição não conferida) e o resultado geral é **requer conferência**.
3. **Given** a paciente apontou divergência em um campo, **When** a avaliação é montada, **Then** esse campo não serve de evidência e as regras que dependem dele são **não verificáveis**.
4. **Given** um documento enviado como "encaminhamento" mas lido e confirmado como "identificação", **When** a avaliação é montada, **Then** o documento conta como identificação.
5. **Given** um pedido sem leitura, ou com leitura que não é a da versão atual dos documentos, **When** se tenta montar a avaliação, **Then** ela não é montada e o motivo indica que é preciso ler os documentos (pela primeira vez ou de novo, conforme o caso); nenhum resultado parcial é produzido.
6. **Given** um pedido de outra paciente ou inexistente, **When** se tenta montar a avaliação, **Then** a resposta é a mesma de "não encontrado", sem revelar se o pedido existe.
7. **Given** uma avaliação montada, **When** ela termina, **Then** o estado, a revisão, os documentos, a leitura e as decisões do pedido continuam iguais e nenhuma análise foi gravada.

---

### Edge Cases

- **Versão do protocolo sem regras, ou com regra que o motor não sabe avaliar:** a avaliação falha por inteiro, com motivo identificado; nunca produz resultado parcial nem "sem pendências" por falta de regra.
- **Nenhum documento lido com o tipo exigido, mas há documento não lido ou de tipo não verificável:** R01 (ou R03) é **não verificável**, não pendente — o documento não lido pode ser o que falta.
- **Todos os documentos lidos com clareza e nenhum é encaminhamento:** R01 **pendente**; R04 e R05 **não verificáveis** (dependem do encaminhamento ausente), com motivo próprio.
- **Mais de um encaminhamento:** R04 e R05 consideram todos os encaminhamentos confirmados; basta um deles sem motivo ou fora da validade para a regra ficar pendente.
- **Mais de um documento de identificação com datas de nascimento diferentes:** R02 **pendente** (divergência comprovada) e R06 **não verificável** (condição indeterminada).
- **Data de emissão exatamente 180 dias antes da data de referência:** atende; 181 dias: pendente; data de emissão posterior à data de referência: pendente (fora da validade).
- **Valor confirmado que não pode ser interpretado** (data em formato inesperado, tipo de documento desconhecido, autorização diferente de sim/não): o campo não serve de evidência; a regra fica **não verificável** (motivo: valor não interpretável). O sistema não adivinha o valor.
- **Comparação de nomes:** diferenças só de maiúsculas, acentos ou espaços extras não são divergência; letras diferentes são.
- **Apenas um documento com nome e data de nascimento conferidos:** R02 é **não verificável** (são precisos ao menos dois documentos legíveis com identificação).
- **Divergência comprovada e campo ilegível na mesma regra:** a divergência comprovada prevalece (pendente), pois não depende do campo ilegível.
- **Banco indisponível ao montar a avaliação:** a montagem falha com erro identificado; nenhum resultado é produzido.
- **Pedido já submetido em simulação:** a montagem continua só de leitura; decidir se uma nova análise é permitida nesse estado é da RC-13.

## Requirements *(mandatory)*

### Functional Requirements

**Regras versionadas e resultados**

- **FR-001**: O sistema MUST aplicar à avaliação as regras da versão do protocolo ligada ao pedido, com o código, a descrição, a natureza (obrigatória ou condicional), a mensagem e o responsável de cada regra **naquela versão**, na ordem definida pela versão.
- **FR-002**: Cada regra aplicada MUST ter exatamente um resultado: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL` ou `NAO_APLICAVEL`.
- **FR-003**: O sistema MUST avaliar todas as regras da versão, mesmo depois de encontrar uma pendente ou não verificável.
- **FR-004**: O resultado geral MUST seguir a precedência do protocolo: alguma `PENDENTE` → `COM_PENDENCIAS`; senão, alguma `NAO_VERIFICAVEL` → `REQUER_CONFERENCIA`; senão → `SEM_PENDENCIAS_IDENTIFICADAS`. `NAO_APLICAVEL` não influencia.
- **FR-005**: O resultado da avaliação MUST identificar a versão do protocolo usada (identificador e versão), a revisão do pedido avaliada, a leitura usada, o instante da última alteração da conferência considerada e a data de referência, para que uma análise posterior (RC-13) saiba se ficou desatualizada.
- **FR-006**: Se a versão do protocolo não tiver regras, ou tiver uma regra que o sistema não sabe avaliar, ou se a natureza registrada de uma regra divergir da que o sistema implementa para aquele código, a avaliação MUST falhar por inteiro com motivo identificado, sem produzir resultado parcial.

**Evidência: o que conta**

- **FR-007**: Um campo MUST servir de evidência somente se estiver **identificado** na leitura atual **e confirmado** pela paciente, com valor interpretável.
- **FR-008**: Campo ilegível, campo identificado sem decisão, campo com divergência apontada, valor não interpretável e documento não lido (protegido ou leitura vazia) MUST tornar `NAO_VERIFICAVEL` — nunca `PENDENTE` nem `ATENDE` — a regra que dependa deles para decidir, com o motivo correspondente.
- **FR-009**: Campo **não localizado** em documento lido, sem divergência apontada pela paciente, MUST contar como ausente no documento (pode levar a `PENDENTE`); com divergência "o campo existe e não foi lido", MUST contar como não verificável.
- **FR-010**: O tipo de um documento MUST ser o tipo **lido e confirmado**; o tipo escolhido pela paciente no envio MUST NOT ser usado como evidência.
- **FR-011**: Valores e trechos dos documentos MUST ser tratados apenas como dados: nenhum texto do documento altera regras, resultados, mensagens, responsáveis ou o funcionamento da avaliação.

**Regras do protocolo v0.1** (condições detalhadas em `docs/protocolo-academico.md`; data de referência em FR-018)

- **FR-012 (RC-R01, encaminhamento presente)**: `ATENDE` se algum documento é encaminhamento; senão `NAO_VERIFICAVEL` se algum documento não foi lido ou tem tipo não verificável; senão `PENDENTE`.
- **FR-013 (RC-R02, identificação coerente)**: `PENDENTE` se nomes ou datas de nascimento confirmados diferem entre documentos (comparação de nomes sem distinguir maiúsculas, acentos e espaços extras); senão `NAO_VERIFICAVEL` se algum documento não foi lido, se algum nome ou data de nascimento de documento lido não é verificável, ou se menos de dois documentos têm nome e data de nascimento confirmados; senão `ATENDE`.
- **FR-014 (RC-R03, documento de identificação)**: `ATENDE` se algum documento é de identificação com nome e data de nascimento confirmados; senão `NAO_VERIFICAVEL` se algum documento não foi lido, tem tipo não verificável, ou é de identificação com nome ou data de nascimento não verificável; senão `PENDENTE`.
- **FR-015 (RC-R04, motivo informado)**: considerando os encaminhamentos: `PENDENTE` se algum deles não traz o motivo (não localizado); senão `NAO_VERIFICAVEL` se algum motivo não é verificável ou se não há encaminhamento confirmado (motivo: o do documento de tipo não verificável, que pode ser o encaminhamento, ou, se todos os tipos são conhecidos, "depende de documento ausente"); senão `ATENDE`. O conteúdo do motivo não é avaliado.
- **FR-016 (RC-R05, validade de 180 dias)**: considerando os encaminhamentos: `PENDENTE` se algum não traz a data de emissão (não localizada) ou se ela está a mais de 180 dias da data de referência ou é posterior a ela; senão `NAO_VERIFICAVEL` se alguma data não é verificável ou se não há encaminhamento confirmado (mesmo motivo de FR-015); senão `ATENDE`.
- **FR-017 (RC-R06, autorização do responsável, condicional)**: a condição usa a data de nascimento **do documento de identificação**. Se não for possível determiná-la de forma única (sem documento de identificação lido e confirmado, data não verificável ou datas diferentes entre documentos de identificação), o resultado MUST ser `NAO_VERIFICAVEL` (condição indeterminada). Se a paciente tem 18 anos ou mais na data de referência, `NAO_APLICAVEL`. Se tem menos de 18: `ATENDE` se algum documento traz autorização assinada confirmada como "sim"; senão `NAO_VERIFICAVEL` se algum documento não foi lido ou a informação de autorização não é verificável; senão `PENDENTE`.
- **FR-018**: A data de referência da avaliação MUST ser a data, no horário de Salvador, do envio mais recente entre os documentos avaliados (a "data do envio" do conjunto documental), para que avaliar de novo a mesma revisão produza o mesmo resultado.

**Pendências**

- **FR-019**: Cada regra `PENDENTE` ou `NAO_VERIFICAVEL` MUST gerar exatamente uma pendência; regras `ATENDE` ou `NAO_APLICAVEL` MUST NOT gerar pendência.
- **FR-020**: Cada pendência MUST conter: a regra (código e descrição da versão), o tipo (pendente ou não verificável), a explicação, a evidência disponível e o responsável provável (`PACIENTE` ou `UNIDADE_EMISSORA`).
- **FR-021**: Para `PENDENTE`, a explicação MUST ser a mensagem da regra na versão do protocolo e o responsável MUST ser o da regra. Para `NAO_VERIFICAVEL`, a explicação MUST ser a mensagem padrão de "não verificável" do protocolo acompanhada de um motivo de uma lista fechada (campo ilegível, documento não lido, transcrição não conferida, divergência apontada, valor não interpretável, condição indeterminada, documentos insuficientes para comparar, depende de documento ausente) e o responsável MUST ser a paciente.
- **FR-022**: A evidência disponível MUST identificar os documentos e campos considerados, a situação de cada um (confirmado, não localizado, ilegível, não conferido, divergência apontada, documento não lido) e a origem (página e trecho) quando a leitura a tiver; pode ser vazia quando não há documento a apontar, e então a explicação diz por quê.

**Montagem a partir do pedido**

- **FR-023**: A avaliação MUST ser montada a partir da **leitura atual** do pedido (a da versão atual dos documentos) e das decisões de conferência vigentes; sem leitura atual, a montagem MUST ser recusada com motivo identificado, sem resultado.
- **FR-024**: A montagem MUST ser feita apenas para pedido da paciente autenticada; pedido de outra paciente ou inexistente MUST receber a mesma recusa de "não encontrado".
- **FR-025**: Montar a avaliação MUST NOT gravar análise nem alterar estado, revisão, documentos, leitura ou decisões do pedido; esta história MUST NOT criar rota de acesso para a paciente.
- **FR-026**: A avaliação MUST ser determinística: os mesmos dados (versão, leitura, decisões, data de referência) produzem o mesmo resultado, nas mesmas ordem e quantidade.
- **FR-027**: O resultado MUST indicar que se baseia em leitura **simulada** e em protocolo **acadêmico fictício**; nenhuma mensagem diagnostica, define urgência, promete vaga ou afirma aprovação do SUS.
- **FR-028**: Registros de funcionamento (logs) MUST NOT conter valores ou trechos dos documentos; apenas identificadores técnicos, códigos de regra, resultados e motivos.

### Key Entities *(include if feature involves data)*

- **Regra da versão do protocolo**: critério do protocolo numa versão: código, descrição, evidência exigida, natureza, condição de aplicação, mensagem à paciente, responsável e ordem. Já cadastrada para a versão v0.1.
- **Evidência de campo**: o que a avaliação sabe de um campo de um documento: situação (confirmado, não localizado, ilegível, não conferido, divergência apontada, documento não lido), valor quando confirmado e origem (página, trecho) quando houver. Derivada da leitura atual e da conferência.
- **Avaliação**: resultado de aplicar a versão do protocolo a um pedido: versão do protocolo, revisão avaliada, leitura usada, instante da conferência considerada, data de referência, resultados por regra, pendências e resultado geral. Não é gravada nesta história.
- **Resultado de regra**: a regra e um dos quatro resultados, com a evidência considerada.
- **Pendência**: para resultado pendente ou não verificável: regra, tipo, explicação (mensagem e, se não verificável, motivo), evidência disponível e responsável provável.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Os 5 casos sintéticos do protocolo produzem, em 100% das regras (30 resultados) e dos 5 resultados gerais, exatamente o esperado na tabela de casos do protocolo.
- **SC-002**: Em 100% das avaliações verificadas, toda regra da versão do protocolo aparece exatamente uma vez com um dos quatro resultados, inclusive quando há pendência.
- **SC-003**: 100% das regras pendentes ou não verificáveis nos casos verificados geram exatamente uma pendência com regra, explicação, evidência e um dos dois responsáveis; 0 pendências para regras que atendem ou não se aplicam.
- **SC-004**: Em 0 casos um campo ilegível, não conferido, contestado ou não interpretável, ou um documento não lido, leva uma regra a `ATENDE` ou a `PENDENTE` quando a decisão depende dele.
- **SC-005**: Avaliar duas vezes os mesmos dados produz resultados idênticos em 100% dos casos verificados, e montar a avaliação deixa estado, revisão, leitura, decisões e análises do pedido inalterados (0 gravações).
- **SC-006**: Um texto de instrução dentro de um documento sintético altera 0 resultados, explicações ou responsáveis.
- **SC-007**: Com as duas amostras sintéticas oficiais lidas e todos os campos identificados confirmados, o resultado geral é "sem pendências identificadas"; sem nenhuma confirmação, é "requer conferência".
- **SC-008**: Pedidos de outra paciente e pedidos inexistentes recebem a mesma recusa em 100% das tentativas verificadas.

## Assumptions

- **Escopo** (decisão do responsável, 13/11/2026): sem rota HTTP, sem gravação e sem mudança de estado; a RC-13 usa esta avaliação para gravar a análise, mudar o estado e expor o resultado. Até lá, a avaliação é verificada por testes.
- **"Data do envio"** (protocolo, R05 e R06) é interpretada como a data, em Salvador, do envio mais recente entre os documentos avaliados. Isso torna o resultado reprodutível para a mesma revisão. Se o grupo preferir a data da análise, a mudança fica na RC-13.
- **Campo identificado sem decisão não serve de evidência**: a RC-11 existe para que nada seja cobrado ou liberado com base em leitura não conferida, e a tabela de estados leva de `REQUER_CONFERENCIA` a nova análise quando a conferência é confirmada. Campo não localizado não precisa de decisão (como na RC-11).
- **Mensagem padrão de "não verificável"** é a do protocolo v0.1 (`docs/protocolo-academico.md`), com responsável a paciente; a versão do protocolo não guarda essa mensagem por regra.
- **Mais de um encaminhamento ou documento de identificação** é raro no demonstrador (máximo de 3 arquivos); a regra conservadora (todos os encaminhamentos precisam atender; datas de nascimento diferentes deixam a condição indeterminada) evita "sem pendências" por combinação de documentos.
- **Valores lidos** seguem as convenções da leitura simulada (datas `AAAA-MM-DD`, tipo de documento com os nomes dos tipos documentais, autorização `SIM`/`NAO`); qualquer outro formato é tratado como não interpretável.
- Depende da leitura simulada (RC-10), da conferência (RC-11), do protocolo e regras já cadastrados (RC-01/RC-03) e do mecanismo de dona do pedido (RC-05). Nenhuma mudança de esquema é esperada: as regras já estão cadastradas.
- Todos os dados são sintéticos; o protocolo é acadêmico e fictício.
