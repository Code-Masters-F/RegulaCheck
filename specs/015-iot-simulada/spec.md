# Feature Specification: IoT simulada — leituras de dois sensores e alertas da estação de captura (RC-21 + RC-22)

**Feature Branch**: `feat/epico-7-iot-simulada` (branch já existente; nenhuma outra foi criada; diretório da feature: `specs/015-iot-simulada`)

**Created**: 2026-11-15

**Status**: Draft — especificação esclarecida em 16/11/2026 (4 perguntas no `/speckit-clarify`); pronta para `/speckit-plan`

**Backlog**: Épico 7 (IoT simulada)
- **RC-21** — Gerar leituras de dois sensores (#46) · Tarefa técnica · P0 · D · 2 h · até 15/11/2026 · depende de RC-02 (projeto executável e contratos), RC-05 (perfis e isolamento) e RC-07 (rascunho do pedido)
- **RC-22** — Mostrar alertas da estação de captura (#47) · História · P0 · C/D · 2 h · até 16/11/2026 · depende de RC-08 (enviar e consultar documentos) e RC-21

**Input**: User description: "RC-21 + RC-22 — Épico 7 (IoT simulada). Uma especificação reunindo as duas histórias (RC-22 depende da RC-21). RC-21: script executável em Java envia leituras de luminosidade (lux) e distância (cm) em JSON por HTTP POST a /sessoes-captura/{id}/leituras, com sessão de captura vinculada a pedido de teste, tipo, valor, unidade e instante; credencial restrita e vínculo da sessão verificados; valores inválidos rejeitados e válidos persistidos; sequências reproduzíveis normal, fora da faixa e recuperação, com limites demonstrativos documentados. RC-22: a paciente consulta a telemetria da própria sessão (GET /solicitacoes/{id}/leituras) com os dois sensores; fora da faixa gera orientação de melhorar a iluminação ou ajustar a distância, e a recuperação altera o aviso; leitura antiga é identificada como desatualizada e a indisponibilidade dos sensores não bloqueia upload nem salvamento; a documentação explica o que cada sensor mede e que as leituras não comprovam a legibilidade do arquivo. O app Android ainda não existe: a tela fica como dependência do Épico 6. Pendências para o clarify: quem cria a sessão de captura, faixas demonstrativas e prazo de leitura desatualizada."

## Contexto e limite destas histórias

O RegulaCheck precisa demonstrar IoT **no mesmo domínio** da paciente: uma "estação de captura" com dois sensores — **luminosidade** e **distância** — ajuda a paciente a fotografar ou digitalizar seus documentos em boas condições. Não há hardware: os sensores são **simulados** por um programa da equipe que envia as leituras à API, e os limites usados são **demonstrativos**, sem calibração real.

O banco já prevê a sessão de captura (vinculada a um pedido) e as leituras de sensor (tipo, valor, unidade, instante da medição e do recebimento), e o contrato da API já reserva as duas rotas e a credencial do simulador. Estas histórias tornam isso funcional:

1. **RC-21 — gerar e receber leituras**: o simulador envia leituras de uma sessão de captura; a API confere a credencial do simulador e a sessão, rejeita leituras inválidas e grava as válidas. O simulador oferece sequências reproduzíveis (normal, fora da faixa, recuperação).
2. **RC-22 — orientar a paciente**: a paciente consulta a telemetria do próprio pedido e recebe, para cada sensor, a última leitura, se está dentro da faixa demonstrativa, se está desatualizada e uma orientação em linguagem simples.

**Fica fora destas histórias:** a tela do aplicativo (o app Android ainda não existe; a exibição fica registrada como dependência do **Épico 6**), hardware ou sensores reais, calibração, qualquer conclusão sobre a legibilidade do documento, bloqueio de upload ou salvamento por causa dos sensores, indicadores administrativos de telemetria, outros sensores e protocolos de mensageria (MQTT foi descartado em 12/11/2026).

## Clarifications

### Session 2026-11-16

- Q: Quem cria a sessão de captura? → A: A paciente, pela API, em uma nova rota `POST /solicitacoes/{id}/sessoes-captura`, restrita à dona do pedido (404 idêntico para pedido de outra paciente ou inexistente); a resposta devolve o identificador que o simulador usa. A credencial do simulador continua restrita ao envio de leituras e não cria sessões.
- Q: Quais faixas demonstrativas adotar? → A: Luminosidade de 300 a 1000 lux e distância de 20 a 40 cm, limites inclusivos; valores demonstrativos, sem calibração real.
- Q: Qual o prazo para considerar a leitura desatualizada? → A: 60 segundos, contados do instante da medição até o momento da consulta (relógio do servidor, UTC).
- Q: O que acontece ao abrir uma sessão quando o pedido já tem uma aberta? → A: No máximo uma sessão aberta por pedido; abrir de novo devolve a sessão aberta existente (indicando que já existia) em vez de criar outra. Não há rota de encerramento nesta entrega.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Simulador envia leituras válidas da sessão de captura (Priority: P1) — RC-21

A equipe executa o simulador apontando para uma sessão de captura de um pedido de teste. O simulador envia leituras de luminosidade (em lux) e distância (em centímetros), cada uma com o tipo do sensor, o valor, a unidade e o instante da medição, usando a credencial restrita do simulador. A API confere a credencial e a sessão, grava cada leitura válida e confirma o recebimento.

**Why this priority**: sem leituras gravadas não há telemetria a mostrar (US3) nem demonstração de IoT; é a base do épico.

**Independent Test**: com a API em execução e uma sessão de captura aberta de um pedido sintético, executar o simulador na sequência "normal" e conferir que cada leitura enviada foi confirmada e ficou gravada com sessão, tipo, valor, unidade, instante da medição e instante de recebimento.

**Acceptance Scenarios**:

1. **Given** uma paciente autenticada com um pedido seu sem sessão aberta, **When** ela abre uma sessão de captura para esse pedido, **Then** a sessão é criada aberta e a resposta traz o identificador que será informado ao simulador; para pedido de outra paciente ou inexistente a resposta é idêntica de "não encontrado".
2. **Given** um pedido que já tem sessão aberta, **When** a paciente abre a sessão de novo (inclusive por toque duplicado), **Then** recebe a mesma sessão, indicada como já existente, e nenhuma sessão nova é criada.
3. **Given** uma sessão de captura aberta vinculada a um pedido de teste e a credencial correta do simulador, **When** o simulador envia uma leitura de luminosidade de 350 lux com instante de medição, **Then** a leitura é gravada para essa sessão com tipo luminosidade, unidade lux, o valor e o instante informados, e a API confirma a criação.
4. **Given** a mesma sessão, **When** o simulador envia uma leitura de distância de 30 cm, **Then** a leitura é gravada com tipo distância e unidade cm.
5. **Given** o simulador executado duas vezes com a mesma sequência e a mesma semente, **When** se comparam os valores enviados, **Then** a ordem dos tipos e os valores são idênticos (só os instantes de medição mudam).
6. **Given** uma leitura gravada, **When** se consulta o registro, **Then** o instante de recebimento é o do servidor (UTC) e não o informado pelo simulador.

---

### User Story 2 - API recusa credencial, sessão ou leitura inválida (Priority: P1) — RC-21

A rota de leituras só aceita o simulador, com a credencial restrita, e só para uma sessão de captura existente e aberta. Leituras com tipo desconhecido, unidade incompatível com o tipo, valor ausente, não numérico ou fisicamente impossível, ou instante de medição ausente ou inválido, são recusadas sem gravar nada.

**Why this priority**: critério (3) da RC-21 e exigência da constituição (credencial restrita ao escopo de telemetria, validação no servidor).

**Independent Test**: enviar, sem credencial, com credencial errada, com a sessão de uma paciente autenticada, para sessão inexistente ou encerrada, e com cada tipo de payload inválido; conferir o código de erro padronizado e que nenhuma leitura foi gravada.

**Acceptance Scenarios**:

1. **Given** uma requisição sem a credencial do simulador, **When** ela tenta enviar uma leitura, **Then** é recusada como não autenticada e nada é gravado.
2. **Given** uma paciente ou um administrador autenticados por sessão (sem a credencial do simulador), **When** tentam enviar uma leitura, **Then** são recusados como sem permissão e nada é gravado.
3. **Given** a credencial correta, **When** o simulador envia para uma sessão inexistente ou encerrada, **Then** a resposta é "não encontrado" (mesma resposta nos dois casos) e nada é gravado.
4. **Given** a credencial correta e uma sessão aberta, **When** a leitura tem tipo desconhecido, unidade que não corresponde ao tipo (por exemplo distância em lux), valor negativo, valor fora do limite físico aceito, valor não numérico, ou instante de medição ausente, inválido ou no futuro além de uma pequena tolerância, **Then** a leitura é recusada como entrada inválida, com o campo indicado, e nada é gravado.
5. **Given** um corpo maior que o limite aceito ou em formato que não seja JSON, **When** é enviado, **Then** é recusado sem gravar nada.
6. **Given** qualquer recusa, **When** se consultam os registros técnicos, **Then** não aparecem a credencial nem o corpo da requisição.

---

### User Story 3 - Paciente consulta a telemetria e recebe orientação (Priority: P1) — RC-22

A paciente consulta as leituras do próprio pedido. Para cada um dos dois sensores, recebe a última leitura (valor, unidade e instante da medição), a situação — **dentro da faixa**, **abaixo da faixa**, **acima da faixa**, **desatualizada** ou **sem leitura** — e uma orientação em linguagem simples: melhorar a iluminação, reduzir a iluminação/reflexo, aproximar ou afastar o documento. Quando uma nova leitura volta à faixa, a orientação muda para "condição adequada". A resposta sempre lembra que as leituras são simuladas, que os limites são demonstrativos e que não comprovam a legibilidade do arquivo.

**Why this priority**: é o valor percebido pela paciente no épico; critérios (1) e (2) da RC-22.

**Independent Test**: com leituras gravadas pelo simulador nas sequências "normal", "fora da faixa" e "recuperação", consultar a telemetria do pedido após cada sequência e conferir situação e orientação de cada sensor.

**Acceptance Scenarios**:

1. **Given** um pedido da paciente cuja sessão de captura tem leituras recentes dentro da faixa nos dois sensores, **When** ela consulta a telemetria, **Then** recebe os dois sensores com situação "dentro da faixa" e orientação de condição adequada.
2. **Given** a última leitura de luminosidade abaixo da faixa, **When** a paciente consulta, **Then** a luminosidade aparece "abaixo da faixa" com a orientação de melhorar a iluminação, e a distância mantém sua própria situação (um sensor não esconde o outro).
3. **Given** a última leitura de distância fora da faixa, **When** a paciente consulta, **Then** a distância aparece fora da faixa com a orientação de aproximar ou afastar o documento, conforme o lado.
4. **Given** leituras fora da faixa seguidas de uma leitura dentro da faixa (sequência de recuperação), **When** a paciente consulta, **Then** a situação passa a "dentro da faixa" e a orientação muda; as leituras anteriores continuam gravadas.
5. **Given** um pedido de outra paciente ou inexistente, **When** a paciente consulta a telemetria, **Then** a resposta é idêntica à de "não encontrado".
6. **Given** um administrador autenticado, **When** consulta a telemetria de um pedido, **Then** é recusado como sem permissão.

---

### User Story 4 - Leitura desatualizada ou sensores indisponíveis não bloqueiam a paciente (Priority: P2) — RC-22

Se a última leitura de um sensor é mais antiga que o prazo de atualidade, ela é mostrada como **desatualizada**, com o instante da medição, e não gera orientação como se fosse atual. Se o pedido não tem sessão de captura ou a sessão não tem leituras, a consulta responde normalmente com "sem leitura". Em nenhum caso a situação dos sensores impede a paciente de enviar, substituir ou salvar documentos e o rascunho.

**Why this priority**: critério (3) da RC-22 e regra do projeto ("indisponibilidade não bloqueia salvamento/upload"); é menos central que a orientação em si, mas evita que a demonstração atrapalhe o percurso principal.

**Independent Test**: com uma leitura gravada antes do prazo de atualidade, consultar e ver "desatualizada"; com um pedido sem sessão, consultar e ver "sem leitura"; em ambos, enviar um documento e salvar o rascunho com sucesso.

**Acceptance Scenarios**:

1. **Given** a última leitura de luminosidade medida há mais tempo que o prazo de atualidade, **When** a paciente consulta, **Then** o sensor aparece como "desatualizado", com o instante da medição e orientação de verificar se a estação está ligada, sem afirmar que a iluminação está adequada ou inadequada agora.
2. **Given** um pedido sem sessão de captura, ou com sessão sem leituras, **When** a paciente consulta, **Then** recebe resposta de sucesso com os dois sensores "sem leitura" e a orientação de que a captura pode seguir mesmo assim.
3. **Given** sensores sem leitura, desatualizados ou fora da faixa, **When** a paciente envia, substitui ou remove um documento ou salva o rascunho, **Then** a operação segue exatamente como sem sensores.

---

### User Story 5 - Sequências reproduzíveis e documentação para a demonstração (Priority: P2) — RC-21 e RC-22

A equipe escolhe, ao executar o simulador, uma de três sequências reproduzíveis — **normal** (valores dentro da faixa), **fora da faixa** (luminosidade baixa e distância inadequada) e **recuperação** (começa fora e volta à faixa) — além do endereço da API, da sessão e do intervalo entre envios. A documentação explica como executar o simulador, o que cada sensor mede, a unidade, a faixa demonstrativa, o prazo de atualidade e que nada disso comprova a legibilidade do arquivo nem tem calibração real.

**Why this priority**: critério (4) da RC-21 e (4) da RC-22; necessário para a demonstração e a avaliação, mas depende de US1 a US3 existirem.

**Independent Test**: seguindo apenas a documentação, executar as três sequências contra a API local e observar a mudança de situação/orientação na consulta da paciente.

**Acceptance Scenarios**:

1. **Given** a documentação do simulador, **When** alguém do grupo segue os passos, **Then** consegue executar cada sequência sem editar código, informando endereço, sessão e credencial por configuração.
2. **Given** a sequência "fora da faixa" seguida da "recuperação" na mesma sessão, **When** a paciente consulta após cada uma, **Then** vê primeiro as orientações de correção e depois a condição adequada.
3. **Given** a API indisponível ou a credencial recusada, **When** o simulador envia, **Then** informa a falha com o código recebido, não repete indefinidamente e encerra com indicação de erro.

---

### Edge Cases

- Leituras chegando fora de ordem (instante de medição anterior ao da última gravada): são gravadas, mas a "última leitura" mostrada é a de **instante de medição** mais recente, não a última recebida.
- Leitura exatamente no limite da faixa demonstrativa: conta como dentro da faixa (limites inclusivos).
- Pedido com mais de uma sessão de captura (só uma pode estar aberta; as demais vêm, por exemplo, da carga sintética já encerradas): a telemetria considera a sessão mais recente do pedido.
- Pedido já submetido em simulação ou removido: a consulta segue as regras de acesso do pedido; envio para sessão de pedido removido é tratado como sessão inexistente.
- Envio repetido da mesma leitura (retentativa do simulador): pode gerar registro duplicado; aceitável na demonstração, desde que o simulador não faça retentativas automáticas ilimitadas.
- Valor com muitas casas decimais: arredondado/limitado à precisão gravada (três casas), sem rejeitar.
- Credencial do simulador não configurada no servidor: a rota recusa todo envio (nunca aceita sem chave).
- Relógio do simulador adiantado: instante de medição no futuro além da tolerância é recusado.

## Requirements *(mandatory)*

### Functional Requirements

**Recebimento de leituras (RC-21)**

- **FR-001**: O sistema MUST aceitar leituras de sensor de uma sessão de captura identificada no caminho, cada leitura com tipo (luminosidade ou distância), valor numérico, unidade e instante da medição.
- **FR-002**: O sistema MUST aceitar o envio somente com a credencial restrita do simulador; sem ela responde "não autenticado", e contas de paciente ou administrador recebem "sem permissão". A credencial do simulador MUST valer apenas para o envio de leituras.
- **FR-003**: O sistema MUST verificar que a sessão de captura existe, está aberta e pertence a um pedido existente; caso contrário responde "não encontrado", com a mesma resposta para todos esses casos.
- **FR-004**: O sistema MUST exigir unidade compatível com o tipo (luminosidade em lux, distância em cm) e rejeitar valores negativos, não numéricos, ausentes ou acima de um limite físico de plausibilidade definido para cada tipo.
- **FR-005**: O sistema MUST rejeitar instante de medição ausente, em formato inválido ou no futuro além de uma tolerância curta de relógio, e MUST registrar o instante de recebimento pelo relógio do servidor, em UTC.
- **FR-006**: O sistema MUST gravar cada leitura válida e só confirmar sucesso depois de gravada; leitura recusada MUST NOT deixar registro.
- **FR-007**: O sistema MUST responder às recusas no formato de erro padronizado do projeto (código, mensagem e campo quando pertinente), sem expor detalhes internos, e MUST NOT registrar a credencial nem o corpo completo em logs.
- **FR-008**: A paciente MUST poder abrir uma sessão de captura para um pedido seu; a resposta MUST devolver o identificador da sessão e o instante de início. Pedido de outra paciente ou inexistente MUST receber resposta idêntica de "não encontrado"; administrador e simulador MUST receber "sem permissão". O simulador MUST NOT criar sessões. Cada pedido MUST ter no máximo uma sessão aberta: se já houver uma, abrir de novo (inclusive por toque duplicado ou pedidos simultâneos) MUST devolver essa sessão, indicando que já existia, sem criar outra. Encerrar sessão fica fora desta entrega.

**Simulador (RC-21)**

- **FR-009**: O simulador MUST ser um programa executável independente, que envia as leituras em JSON por HTTP à rota de leituras usando a credencial restrita, lida de configuração (nunca embutida no código).
- **FR-010**: O simulador MUST oferecer as sequências "normal", "fora da faixa" e "recuperação", reproduzíveis: a mesma sequência produz a mesma ordem de tipos e os mesmos valores em execuções diferentes.
- **FR-011**: O simulador MUST permitir configurar endereço da API, sessão de captura, sequência e intervalo entre envios, MUST relatar o resultado de cada envio e MUST encerrar com indicação de erro após falha, sem retentativas ilimitadas.

**Consulta e orientação (RC-22)**

- **FR-012**: O sistema MUST permitir que a paciente consulte a telemetria de um pedido seu, recebendo sempre os dois sensores, cada um com: última leitura (valor, unidade, instante da medição) quando houver, situação e orientação.
- **FR-013**: A situação de cada sensor MUST ser uma de: dentro da faixa, abaixo da faixa, acima da faixa, desatualizada, sem leitura. A "última leitura" é a de instante de medição mais recente da sessão mais recente do pedido.
- **FR-014**: Fora da faixa, a orientação MUST indicar a correção: luminosidade baixa → melhorar a iluminação; alta → reduzir iluminação ou reflexo; distância pequena → afastar o documento; grande → aproximar. Dentro da faixa, a orientação MUST indicar condição adequada.
- **FR-015**: As faixas demonstrativas MUST ser luminosidade de 300 a 1000 lux e distância de 20 a 40 cm, com limites inclusivos, definidas em um único ponto central e documentadas como não calibradas. Abaixo do mínimo é "abaixo da faixa"; acima do máximo, "acima da faixa".
- **FR-016**: Uma leitura cujo instante de medição seja anterior a 60 segundos do momento da consulta (relógio do servidor, UTC) MUST ser identificada como desatualizada e MUST NOT gerar orientação como se fosse atual. O prazo MUST ficar no mesmo ponto central das faixas e ser documentado como demonstrativo.
- **FR-017**: A consulta MUST responder com sucesso e "sem leitura" quando o pedido não tem sessão ou leituras, em vez de erro.
- **FR-018**: A consulta MUST aplicar o isolamento por paciente: pedido de outra paciente ou inexistente recebe resposta idêntica de "não encontrado"; o administrador recebe "sem permissão".
- **FR-019**: Toda resposta de telemetria MUST indicar que as leituras são simuladas, que os limites são demonstrativos e que as leituras não comprovam a legibilidade do arquivo.
- **FR-020**: A situação dos sensores MUST NOT bloquear nem alterar envio, substituição, remoção de documentos, salvamento do rascunho, leitura, análise ou submissão simulada.

**Documentação**

- **FR-021**: A documentação MUST explicar o que cada sensor mede, unidade, faixa demonstrativa, prazo de atualidade, como executar o simulador e as três sequências, as variáveis de configuração necessárias, e deixar explícito que não há calibração real e que as leituras não comprovam a legibilidade.
- **FR-022**: A documentação MUST registrar que a exibição no aplicativo depende do Épico 6, com o contrato de resposta que a tela deve consumir.

### Key Entities

- **Sessão de captura**: período em que a estação de captura acompanha a paciente ao capturar documentos de um pedido; aberta pela própria paciente, pertence a um pedido, tem início e, opcionalmente, encerramento. Só sessão aberta recebe leituras.
- **Leitura de sensor**: uma medição de um dos dois sensores em uma sessão: tipo (luminosidade ou distância), valor, unidade (lux ou cm, amarrada ao tipo), instante da medição (informado pelo simulador) e instante de recebimento (do servidor).
- **Estado do sensor (derivado, não gravado)**: para cada tipo, a última leitura, a situação em relação à faixa demonstrativa e ao prazo de atualidade, e a orientação correspondente; calculado na consulta.
- **Faixa demonstrativa**: limites mínimo e máximo de cada tipo e prazo de atualidade, configurados de forma centralizada e documentados como não calibrados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: As três sequências do simulador executam do início ao fim contra a API local, com 100% das leituras válidas confirmadas e gravadas, seguindo apenas a documentação.
- **SC-002**: 100% das tentativas inválidas da matriz de testes (sem credencial, credencial errada, conta de paciente/administrador, sessão inexistente/encerrada, tipo/unidade/valor/instante inválidos) são recusadas sem deixar leitura gravada.
- **SC-003**: Após a sequência "fora da faixa", a consulta da paciente mostra orientação de correção nos dois sensores; após a "recuperação", mostra condição adequada nos dois — verificável em uma única demonstração de menos de 2 minutos.
- **SC-004**: Duas execuções da mesma sequência produzem valores idênticos em 100% das leituras.
- **SC-005**: Em 100% dos testes de acesso, a consulta de pedido de outra paciente recebe resposta indistinguível da de pedido inexistente.
- **SC-006**: Com sensores sem leitura, desatualizados ou fora da faixa, 100% das operações de envio de documento e salvamento do rascunho dos testes continuam funcionando como sem sensores.
- **SC-007**: Nenhum texto de resposta ou documentação afirma calibração real ou que as leituras comprovam a legibilidade do documento.

## Assumptions

- O esquema do banco (sessão de captura e leitura de sensor, com tipo e unidade amarrados) e a credencial do simulador já existem e são reutilizados; mudanças de esquema só se o clarify ou o plano demonstrarem necessidade.
- A "sessão de teste" da demonstração é de um pedido sintético da carga de dados; nenhum dado real.
- O contrato das rotas segue o já reservado em `CONTRATO_API.md`; detalhes de formato serão fixados no plano.
- Limites físicos de plausibilidade (para rejeitar valores impossíveis) são diferentes e mais largos que a faixa demonstrativa; serão definidos no plano.
- Leituras não são apagadas nem reescritas por estas histórias; não há expurgo automático.
- A tela do app não faz parte desta entrega; a verificação de RC-22 é feita pela API e pela documentação. A exibição fica como dependência do Épico 6.
- Retentativas do simulador são limitadas; duplicidade eventual de leitura por retentativa é aceita na demonstração e não afeta a orientação.
