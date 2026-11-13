# Feature Specification: Conferir a transcrição antes da validação (RC-11)

**Feature Branch**: `012-conferir-transcricao`

**Created**: 2026-11-13

**Status**: Implementada e verificada **sem banco** em 13/11/2026 (ver "Resultado da verificação" em `tasks.md`). `09_conferencia.sql` aplicado e `ConferenciaOracleTest` executado no Oracle da faculdade em 13/11/2026 (3 testes, 0 falhas); não executados: casos 23 a 27 do `04_` e o quickstart. A leitura é simulada. (Corrigida no plano; ver "Ajustes feitos no plano")

**Backlog**: RC-11 — Code-Masters-F/ClickBus#19 · História · P0 · B/C · 3 h · até 15/11/2026 · depende de RC-10 (leitura simulada dos documentos, concluída)

**Input**: User description: "RC-11 — Conferir a transcrição antes da validação. Como paciente, quero conferir o que foi lido para evitar orientações baseadas em erro de transcrição. Mostrar os campos extraídos e sua origem, destacando os que não puderam ser verificados; a paciente pode confirmar ou marcar divergência de leitura; se a implementação permitir corrigir transcrição, a correção fica identificada e vinculada ao original; informação clínica ausente no documento não pode ser acrescentada como se constasse nele, e a orientação é obter documento corrigido pela unidade emissora; campos necessários não verificáveis resultam em requer conferência, sem liberação automática para submissão."

## Contexto e limite desta história

A RC-10 devolve, para cada documento do pedido, os campos que as regras usam, cada um como **identificado**, **não localizado** ou **ilegível** (leitura simulada, dados sintéticos). Antes de qualquer regra (RC-12) ou orientação (RC-14) olhar para esses dados, a paciente precisa poder **conferir o que foi lido**: um erro de leitura não pode virar uma cobrança indevida nem uma liberação indevida.

Esta história entrega:

1. **Mostrar a conferência**: cada campo lido, com sua origem, e os campos que não puderam ser verificados em destaque.
2. **Registrar a decisão da paciente**, campo a campo: **confirmar** que a leitura confere com o documento ou **marcar divergência** (a leitura não confere com o que está no documento).
3. **Não deixar a paciente inventar o documento**: ela confirma ou aponta divergência; **não digita valores clínicos** nem completa campo ausente. Informação que falta ou está errada **no próprio documento** é corrigida pela unidade emissora, e o sistema orienta isso.
4. **Dizer quando a conferência ainda é necessária** (situação `REQUER_CONFERENCIA` da conferência) e nunca liberar nada sozinho para submissão. A situação é **da conferência**: o estado do pedido não é escrito por esta história (ver "Ajustes feitos no plano").

**Decisão desta história:** a opção de "corrigir a transcrição" que o backlog deixa em aberto **não é entregue**. Digitar um valor no lugar do lido deixaria a paciente escrever informação clínica que o documento não traz (proibido pelo `CLAUDE.md` §11). Fica registrada como melhoria futura, caso o grupo a queira, e teria de ser identificada e vinculada ao original.

**Fica fora desta história:** correção livre de valores, execução das regras e resultado geral (RC-12), persistência de análises (RC-13), resumo de IA (RC-14), reenvio guiado (RC-16), submissão simulada (RC-17), leitura real e a interface (épico 007). A conferência **não** diz se o pedido está completo.

## Ajustes feitos no plano

Ao planejar, três pontos da primeira redação foram corrigidos por conflitarem com o que o sistema já faz (detalhes em `plan.md` e `research.md`):

1. **Campo ilegível não recebe decisão**: não há valor a confirmar; ele se resolve com um arquivo mais nítido (nova leitura). Decisão sobre ele é recusada (422).
2. **Esta história não escreve o estado do pedido.** Pôr o pedido em `REQUER_CONFERENCIA` agora o impediria de receber documentos novos (RC-08) e de ser excluído (RC-09), e nada o devolveria a `RASCUNHO`; o fluxo documentado só chega a esse estado a partir da análise (RC-12/RC-13). A **situação da conferência** é calculada e exposta, e quem a usa para mudar o estado é a RC-12/RC-13.
3. **Registrar decisão não cria revisão do pedido**, pois isso tornaria desatualizada a própria leitura conferida.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver o que foi lido, com a origem, e o que não pôde ser verificado (Priority: P1)

A paciente abre a conferência do seu pedido e vê, documento a documento, os campos lidos com o valor e a origem (página e trecho), e os campos **não verificados** em destaque: os ilegíveis, os não localizados e os documentos que não puderam ser lidos. Tudo marcado como leitura **simulada**.

**Why this priority**: sem enxergar o que foi lido, ela não tem como conferir; os destaques evitam que um campo duvidoso passe despercebido.

**Independent Test**: pedido com uma leitura atual contendo campos identificados, ilegíveis, não localizados e um documento não lido; abrir a conferência e checar valor, origem e destaque de cada um.

**Acceptance Scenarios**:

1. **Given** um pedido próprio com leitura atual, **When** a paciente abre a conferência, **Then** vê cada documento com seus campos, o valor e a origem dos identificados, e a indicação de leitura simulada.
2. **Given** campos ilegíveis ou não localizados, **When** a conferência é mostrada, **Then** eles aparecem como **não verificados** (em destaque), sem valor, cada um com orientação do que fazer.
3. **Given** um documento protegido ou de leitura vazia, **When** a conferência é mostrada, **Then** ele aparece como não verificado, com o motivo e a orientação de reenviar.
4. **Given** um pedido sem leitura, **When** a paciente abre a conferência, **Then** recebe a mesma resposta de "não encontrado" de um pedido inexistente.
5. **Given** uma pessoa sem autenticação, com perfil de administrador ou do simulador, ou uma outra paciente, **When** tenta abrir a conferência, **Then** é recusada (401/403) ou recebe o mesmo "não encontrado", e nada vaza.

---

### User Story 2 - Confirmar ou marcar divergência, campo a campo (Priority: P1)

Para cada campo, a paciente diz se a leitura **confere** com o documento ou se há **divergência**. Ao marcar divergência, escolhe um motivo de uma lista fechada (por exemplo, "o valor é diferente do que está no documento" ou "o documento tem o campo e ele não foi lido"). Ela pode mudar de ideia até o pedido ser submetido.

**Why this priority**: é o núcleo da história: a paciente passa a responder pelo que o sistema leu.

**Independent Test**: confirmar um campo, marcar divergência em outro, mudar a decisão e conferir o registro, a data e que nenhum valor digitado é aceito.

**Acceptance Scenarios**:

1. **Given** uma leitura atual, **When** a paciente confirma um campo identificado, **Then** a decisão fica registrada para aquele campo, com data, e a conferência mostra o campo como confirmado.
2. **Given** um campo identificado, **When** ela marca divergência com um motivo válido, **Then** a divergência fica registrada e o campo passa a contar como não verificado para fins de conferência.
3. **Given** uma decisão já registrada, **When** ela decide de novo, **Then** vale a última decisão, e não há duas decisões ativas para o mesmo campo.
4. **Given** qualquer decisão, **When** o pedido traz um valor digitado, texto livre ou campo de outro documento, **Then** a requisição é recusada: só se aceita confirmar ou divergir, com motivo da lista.
5. **Given** um campo que não existe na leitura, um documento de outro pedido ou uma decisão inválida, **When** ela tenta registrar, **Then** recebe 404 ou 400 e nada é gravado.
6. **Given** o mesmo pedido de decisão repetido, **When** chega de novo, **Then** o resultado é o mesmo, sem duplicar.

---

### User Story 3 - Informação ausente não é acrescentada: orientar a unidade emissora (Priority: P1)

Quando o campo falta no documento ("não localizado") ou a divergência é que o próprio documento está errado, a paciente **não completa nem altera** nada. O sistema orienta: procurar a unidade emissora para obter um documento corrigido, ou enviar um arquivo mais nítido quando o problema é a leitura.

**Why this priority**: preserva a regra de que a paciente não escreve o documento clínico e que informação ausente não pode passar como se constasse nele.

**Independent Test**: conferir um campo não localizado e um divergente e verificar a orientação e que nenhum valor é gravado como se fosse do documento.

**Acceptance Scenarios**:

1. **Given** um campo não localizado em documento nítido, **When** a paciente o vê na conferência, **Then** a orientação diz que a informação não consta no documento e que ela deve pedir um documento corrigido à unidade emissora, sem opção de preencher o valor.
2. **Given** uma divergência marcada como "o documento tem o campo e ele não foi lido", **When** registrada, **Then** a orientação é enviar um arquivo mais nítido ou conferir a leitura; o sistema não grava valor algum.
3. **Given** uma divergência marcada como "o valor está diferente do documento", **When** registrada, **Then** nenhum valor novo é guardado; a conferência continua dizendo que o campo não foi verificado.
4. **Given** qualquer resposta desta história, **When** examinada, **Then** nenhuma informação clínica nova aparece como se viesse do documento, e as orientações não diagnosticam, não definem urgência nem prometem atendimento.

---

### User Story 4 - Requer conferência, sem liberação automática (Priority: P1)

A conferência resume se ainda há o que conferir. Enquanto houver campo necessário **não verificado** (ilegível, divergente, ou de documento não lido) e sem resolução, a situação da conferência é **requer conferência** e nada é liberado para submissão. Mesmo com tudo confirmado, esta história **não libera** a submissão: quem decide isso são as regras (RC-12) e a submissão (RC-17).

**Why this priority**: impede liberar sozinho um pedido apoiado em leitura duvidosa.

**Independent Test**: percorrer um pedido com campos ilegíveis, divergências e confirmações, conferindo a situação da conferência em cada passo, que o estado do pedido não muda e que nenhuma rota de submissão é habilitada.

**Acceptance Scenarios**:

1. **Given** uma leitura atual com campo ilegível, documento não lido ou divergência marcada, **When** a conferência é calculada, **Then** a situação da conferência é `REQUER_CONFERENCIA` e `liberadoParaSubmissao` é falso.
2. **Given** todos os campos identificados confirmados e nenhum campo necessário não verificado, **When** a conferência é calculada, **Then** a situação é `CONFERIDA`, **sem** liberar submissão e **sem** produzir resultado de regra, pendência ou resultado geral.
3. **Given** campos identificados ainda sem decisão e nenhuma das condições de `REQUER_CONFERENCIA`, **When** a conferência é calculada, **Then** a situação é `PENDENTE` (a paciente ainda não conferiu), e nada sugere aprovação.
4. **Given** qualquer situação da conferência, **When** ela é registrada ou consultada, **Then** o estado e a revisão do pedido não mudam e nada é liberado por esta história (a submissão é da RC-17).

---

### User Story 5 - A conferência acompanha a leitura e a versão dos documentos (Priority: P2)

A conferência vale para a leitura em que foi feita. Se os documentos mudam (enviar, remover ou substituir) a leitura fica desatualizada e a conferência anterior deixa de valer: é preciso nova leitura e nova conferência. Pedido submetido em simulação só pode ser consultado.

**Why this priority**: evita confirmar dados de um arquivo que já não está no pedido.

**Independent Test**: conferir, trocar um documento, consultar a conferência e ver que está desatualizada; ler de novo e conferir de novo.

**Acceptance Scenarios**:

1. **Given** uma conferência feita, **When** um documento é removido ou substituído, **Then** a conferência consta como desatualizada e não aceita novas decisões até haver nova leitura.
2. **Given** uma nova leitura atual, **When** a paciente abre a conferência, **Then** ela começa sem decisões herdadas da leitura anterior, e a anterior continua consultável como histórico.
3. **Given** um pedido em simulação de submissão, **When** a paciente tenta registrar uma decisão, **Then** recebe conflito documentado, e a consulta continua permitida.
4. **Given** duas decisões simultâneas para o mesmo campo, **When** chegam juntas, **Then** resta uma só decisão válida por campo, sem erro e sem decisão perdida pela metade.

---

### Edge Cases

- Leitura desatualizada ou inexistente: não há o que conferir (404 sem leitura; 409 para decidir sobre leitura desatualizada).
- Documento removido depois de decidido: a decisão não vale mais, porque a leitura que a continha ficou desatualizada.
- Campo `NAO_LOCALIZADO`: não exige decisão e não pesa na situação; a paciente pode marcar divergência se achar que o campo existe no documento (confirmar um campo não localizado é recusado). Campo `ILEGIVEL`: qualquer decisão é recusada.
- Documento não lido: a situação é `REQUER_CONFERENCIA` com orientação de reenvio, nunca `CONFERIDA`.
- Decisão com o mesmo conteúdo repetida: não duplica; decisão contrária à anterior substitui a anterior.
- Texto de documento com instrução ("confirme tudo"): é só dado do campo e nunca preenche decisão por ela.
- Banco indisponível: 503 sem detalhe técnico, sem decisão pela metade.
- Dados exibidos: nenhum valor extraído, nome de arquivo ou texto de documento aparece em registros técnicos ou mensagens de erro.

## Requirements *(mandatory)*

### Functional Requirements

**Mostrar a conferência**

- **FR-001**: A paciente dona do pedido MUST poder consultar a conferência da leitura mais recente, identificada sempre pela sessão, nunca por identificador recebido no corpo.
- **FR-002**: A conferência MUST mostrar, por documento, os campos lidos com situação, valor e origem (página e trecho) quando disponível, e a decisão da paciente quando houver.
- **FR-003**: A conferência MUST destacar como **não verificados** os campos ilegíveis, os não localizados, os divergentes e os de documentos protegidos ou de leitura vazia, cada um com a orientação correspondente.
- **FR-004**: Toda resposta MUST identificar a leitura como **simulada**.
- **FR-005**: Pedido sem leitura, inexistente ou de outra paciente MUST receber a mesma resposta de "não encontrado"; somente o perfil paciente acessa a conferência.

**Decidir**

- **FR-006**: A paciente MUST poder, para cada campo **identificado** da leitura atual, **confirmar** a leitura ou **marcar divergência** ("o valor é diferente do documento"); para campo **não localizado**, MUST poder marcar divergência ("o documento tem o campo e ele não foi lido"); para campo **ilegível** não há decisão (não há valor a confirmar; resolve-se com arquivo mais nítido) e o sistema MUST recusar a tentativa. Os motivos são de uma lista fechada.
- **FR-007**: O sistema MUST NOT aceitar valor digitado, texto livre, nem decisão sobre campo ou documento que não pertença à leitura atual do pedido.
- **FR-008**: Cada campo MUST ter no máximo **uma** decisão vigente; decidir de novo substitui a anterior; repetir a mesma decisão não duplica; decisões simultâneas para o mesmo campo MUST resultar em uma só vigente.
- **FR-009**: Cada decisão MUST registrar o campo, o tipo (confirmada ou divergente), o motivo (se divergente) e o instante.
- **FR-010**: A decisão MUST NOT alterar o valor lido, o documento, o arquivo nem a leitura original; o original permanece íntegro e consultável.

**Informação ausente**

- **FR-011**: O sistema MUST NOT acrescentar, inferir ou gravar informação clínica que não conste no documento; um campo não localizado ou divergente continua sem valor.
- **FR-012**: Para informação ausente ou errada **no documento**, a orientação MUST ser obter documento corrigido pela unidade emissora; para erro de leitura, MUST ser enviar arquivo mais nítido ou conferir a leitura. As orientações MUST NOT diagnosticar, definir urgência, prometer atendimento nem afirmar aprovação.

**Situação da conferência**

- **FR-013**: A conferência MUST ter uma situação calculada: `REQUER_CONFERENCIA` (há documento não lido, campo ilegível em documento lido ou decisão divergente), senão `PENDENTE` (há campo identificado sem decisão), senão `CONFERIDA` (todo campo identificado confirmado); `DESATUALIZADA` quando a leitura deixou de ser a atual. Campo não localizado sem divergência não pesa na situação.
- **FR-014**: Nenhuma situação da conferência MUST liberar o pedido para submissão (`liberadoParaSubmissao` é sempre falso) nem colocá-lo em estado de submissão; esta história MUST NOT alterar o estado nem a revisão do pedido (a RC-12/RC-13 usam a situação para decidir o estado).
- **FR-015**: A situação `CONFERIDA` MUST NOT produzir resultado por regra, pendência, resultado geral nem afirmar que o pedido está completo ou aprovado.

**Versão, estado e concorrência**

- **FR-016**: A conferência MUST estar presa à leitura em que foi feita; quando a leitura ficar desatualizada, a conferência MUST constar como desatualizada e MUST NOT aceitar novas decisões; uma nova leitura MUST começar sem decisões herdadas, preservando a anterior como histórico.
- **FR-017**: Pedido em simulação de submissão MUST recusar novas decisões com conflito documentado, mantendo a consulta permitida.
- **FR-018**: Registrar decisões MUST ser atômico por requisição (ou todas valem ou nenhuma) e correto sob concorrência com remover, substituir e enviar documentos.

**Segurança, erros e documentação**

- **FR-019**: Conteúdo de documentos e valores extraídos MUST ser dado não confiável: nunca preenche decisão, altera regras, perfil ou estado.
- **FR-020**: Valores extraídos, trechos e nomes de arquivo MUST NOT aparecer em registros técnicos nem em mensagens de erro; respostas MUST NOT expor caminho, hash ou nome interno.
- **FR-021**: Todas as respostas, incluindo erros, MUST seguir o formato padrão de erro do projeto, com códigos HTTP coerentes e documentados.
- **FR-022**: A API (rotas, respostas, erros, estados, a regra de não digitar valores e a marcação de leitura simulada) MUST ser documentada em `docs/api` e no contrato, e as decisões e o modelo de dados MUST ser atualizados.

### Key Entities

- **Conferência**: visão da leitura atual do pedido com as decisões da paciente e a situação (`PENDENTE`, `REQUER_CONFERENCIA`, `CONFERIDA`); presa à leitura; sempre marcada como simulada.
- **Decisão de conferência**: por campo de um documento da leitura; confirmada ou divergente (com motivo da lista fechada); instante; uma vigente por campo.
- **Campo não verificado**: campo ilegível, não localizado, divergente ou de documento não lido, com sua orientação.
- **Leitura, Documento, Pedido, Revisão**: já existentes (RC-08 a RC-10).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% dos casos de teste, todo campo não verificado (ilegível, não localizado, divergente ou de documento não lido) aparece em destaque, sem valor, com a orientação correspondente.
- **SC-002**: Em 100% das tentativas de enviar valor digitado, texto livre ou decisão sobre campo fora da leitura atual, a requisição é recusada e nada é gravado.
- **SC-003**: Em 100% dos testes, nenhuma resposta desta história contém informação clínica que não conste na leitura original, e nenhuma orientação diagnostica, define urgência ou promete atendimento.
- **SC-004**: Em 100% dos pedidos com campo necessário não verificado, a situação da conferência é `REQUER_CONFERENCIA`; em 100% dos casos de teste o estado e a revisão do pedido permanecem como antes e `liberadoParaSubmissao` é falso.
- **SC-005**: Depois de enviar, remover ou substituir um documento, 100% das consultas passam a marcar a conferência anterior como desatualizada e recusam novas decisões.
- **SC-006**: Em testes com decisões repetidas e simultâneas sobre o mesmo campo, nunca existe mais de uma decisão vigente por campo.
- **SC-007**: Em 100% dos testes de autorização, perfis indevidos e outras pacientes não obtêm dados da conferência, e o "não encontrado" é idêntico para pedido inexistente, alheio e sem leitura.
- **SC-008**: Nenhum valor extraído, trecho, nome de arquivo ou caminho aparece em registros técnicos ou mensagens de erro nos testes de higiene de registros.

## Assumptions

- A paciente só **confirma ou marca divergência**; não digita valores nem corrige a transcrição (decisão desta história, por causa do `CLAUDE.md` §11). Correção identificada e vinculada ao original fica como melhoria futura.
- Os motivos de divergência são uma lista fechada de dois: `VALOR_DIFERENTE_DO_DOCUMENTO` (campo identificado) e `CAMPO_EXISTE_E_NAO_FOI_LIDO` (campo não localizado).
- Os "campos necessários" são os seis campos extraídos pela RC-10, todos usados pelas regras; a aplicabilidade fina por tipo de documento e por regra pertence à RC-12, então aqui qualquer campo identificado ou ilegível conta para a conferência, e campo não localizado só conta se for marcado como divergente.
- A conferência não depende de a paciente decidir todos os campos para ser consultada; `PENDENTE` significa apenas que ainda há campos sem decisão.
- Esta história não escreve o estado do pedido; mudar o estado a partir da situação da conferência, voltar dele (por reenvio, nova leitura ou conferência concluída) e liberar a submissão são decididos com a RC-12, a RC-13 e a RC-16.
- Registrar a conferência não cria revisão do pedido (o esquema prevê o motivo `CONFERENCIA_ALTERADA`, mas usá-lo tornaria desatualizada a própria leitura conferida). É um **desvio justificado** do princípio VI da constituição (ver "Complexity Tracking" do plano); a resposta traz `atualizadaEm` e a RC-12/RC-13 devem capturar a conferência ao publicar uma análise e invalidá-la se ela mudar.
- O armazenamento da conferência pode exigir mudança de esquema, decidida no plano, com prefixo `RC_` e sem alterar tabelas de outros trabalhos.
- Valores e documentos são sintéticos; a leitura é simulada e nunca é apresentada como leitura real.
