# Feature Specification: Plano de testes do demonstrador (RC-24)

**Feature Branch**: `023-plano-de-testes`

**Created**: 2026-11-18 (revisada no mesmo dia, depois que a issue #50 recebeu os 16 cenários)

**Status**: Draft

**Input**: RC-24 (issue #50, atualizada em 18/11/2026 14:44 UTC): "Como equipe, quero documentar casos de teste reproduzíveis para verificar o funcionamento, o tratamento de falhas e a segurança do RegulaCheck." P0 · 2 h · Dep.: RC-02 e RC-23 · Entregável: `docs/testes/plano-de-testes.md`.

## Contexto e limite

O plano é o **documento** que diz o que será testado, como e com que evidência. A **execução**, o registro de evidências e os retestes são da **RC-25** (#51). Esta feature não executa nenhum caso, não altera código e não cria teste automatizado. Todo caso nasce com status **Não executado**, e nenhum resultado ou evidência pode ser inventado.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Outro integrante executa um caso sem orientação adicional (Priority: P1)

Uma pessoa da equipe que não escreveu o plano escolhe um caso e o executa só com o que está escrito (pré-condições, ambiente, entrada, passos), sabendo o que deve acontecer e onde guardar a evidência.

**Why this priority**: é o objetivo declarado da issue ("outro integrante execute os testes sem precisar de orientações adicionais") e a base da RC-25.

**Independent Test**: dar o plano a quem não o escreveu, escolher um caso por grupo e verificar que a pessoa chega ao resultado esperado ou registra divergência, sem pedir esclarecimento.

**Acceptance Scenarios**:

1. **Given** um caso do plano, **When** a pessoa lê pré-condições, ambiente, entrada e passos, **Then** sabe qual conta, estado do pedido e arquivo usar e reproduz a situação.
2. **Given** o mesmo caso, **When** compara o que viu com o resultado esperado, **Then** decide "igual" ou "diferente" sem interpretar (resposta, comportamento da interface e efeito nos dados estão descritos).
3. **Given** um caso com documento, **When** a pessoa precisa do material, **Then** o caso aponta o conjunto e o gabarito da RC-23 que existem em `samples/`.

---

### User Story 2 - Cobrir os 16 cenários com rastreabilidade (Priority: P1)

A equipe vê os cenários T01 a T16 da issue, cada variação com resultado distinto como caso separado (por exemplo T01-01 login válido, T01-02 senha incorreta), ligados a histórias (RC-XX) e, quando aplicável, à regra documental (RC-R01..RC-R06).

**Why this priority**: é o critério de aceite principal; sem o vínculo não se sabe o que ficou sem verificação.

**Independent Test**: conferir que existem T01..T16, que cada caso tem os campos obrigatórios e que a matriz história → casos mostra as lacunas.

**Acceptance Scenarios**:

1. **Given** o plano, **When** a equipe o percorre, **Then** os 16 cenários estão presentes, com todos os casos descritos na issue como itens separados.
2. **Given** um caso documental, **When** é lido, **Then** cita a regra, o resultado esperado por regra e o geral, iguais ao gabarito da RC-23.
3. **Given** um cenário que a issue pede mas que o produto ainda não entrega, **When** a matriz é conferida, **Then** a lacuna aparece explícita como **impedimento conhecido** (funcionalidade pendente), o caso continua "Não executado" e nunca é omitido nem "adaptado" para passar.

---

### User Story 3 - Registrar o resultado com honestidade (Priority: P1)

Quem executa registra resultado obtido, status, evidência, responsável e data; o plano deixa claro que nada é aprovado sem execução.

**Why this priority**: o trabalho é acadêmico e a evidência precisa corresponder à versão entregue.

**Independent Test**: abrir o plano recém-criado e verificar que todo caso está "Não executado" e que resultado obtido, evidência, responsável e data estão em branco.

**Acceptance Scenarios**:

1. **Given** o plano entregue, **When** qualquer caso é lido, **Then** o status é "Não executado" e os campos de execução estão vazios.
2. **Given** a necessidade de registrar um resultado, **When** a pessoa escolhe o status, **Then** só há quatro valores (Não executado, Aprovado, Reprovado, Bloqueado), com a regra de uso de cada um.
3. **Given** um caso que não pôde ser executado (Oracle, app ou funcionalidade ainda inexistente), **When** é registrado, **Then** o status é "Bloqueado" com o motivo, e somente depois de a execução ter sido tentada; um impedimento já conhecido, ainda não tentado, mantém "Não executado".

---

### User Story 4 - Preparar, reproduzir falhas e restaurar o ambiente (Priority: P1)

A equipe sabe subir API, banco, app e simulador, quais contas fictícias usar, como provocar falha de IA, de conexão e de sensor de forma controlada, como restaurar os dados para repetir e onde guardar as evidências.

**Why this priority**: sem isso os casos de falha (T12, T15, T16) não são reproduzíveis e as evidências não são comparáveis.

**Independent Test**: seguir só a seção de preparação até executar o primeiro caso de cada tipo (API, app, falha provocada).

**Acceptance Scenarios**:

1. **Given** a seção de preparação, **When** a pessoa a segue, **Then** sabe o que precisa estar de pé e o que é opcional.
2. **Given** um caso de falha, **When** vai executá-lo, **Then** existe procedimento controlado (e comando existente no repositório) para provocá-la e para desfazê-la.
3. **Given** uma execução concluída, **When** a evidência é guardada, **Then** vai para `docs/evidencias/<ID-DO-CASO>/` com o conteúdo mínimo definido (data, versão ou commit, comando, resultado bruto).
4. **Given** um caso que usa leitura simulada ou resumo simulado, **When** é executado, **Then** o plano diz que é simulado e que não comprova integração real nem aprovação do SUS.

---

### User Story 5 - Percurso integrado da paciente (Priority: P2)

Um caso integrado cobre login → criação do pedido → upload → extração → conferência → validação → resumo → correção → reanálise → submissão simulada, reutilizando casos já documentados.

**Why this priority**: mostra o produto funcionando de ponta a ponta; depende dos casos individuais.

**Independent Test**: seguir o percurso e conferir que cada etapa remete ao caso que a verifica.

**Acceptance Scenarios**:

1. **Given** o plano, **When** o percurso integrado é lido, **Then** cada etapa aponta o caso T-yy que a cobre e o estado esperado do pedido ao fim dela.
2. **Given** a etapa de resumo, **When** é lida, **Then** diz que o resumo é simulado (decisão de 12/11/2026), não real.

---

### Edge Cases

- Cenário que a issue pede e o produto não oferece ainda (indicadores do administrador, RC-20; alertas da estação, RC-22): o caso é mantido, ligado à história aberta, com o **impedimento conhecido** anotado; continua "Não executado" e só vira "Bloqueado" quando a RC-25 tentar executá-lo e registrar o impedimento.
- Cenário que a issue descreve com termos diferentes do produto (extração "real", resumo "real por IA", "baixar documento", "campo permitido" do rascunho, PDF protegido): o plano usa o comportamento documentado em `docs/api/` e registra a diferença, em vez de afirmar o que o sistema não faz.
- Caso cujo resultado esperado depende da data (maioridade): o plano fixa a data de referência.
- Caso com duas contas (isolamento): o plano nomeia as contas fictícias.
- Resultado diferente do esperado: registrado como achado; o esperado não é alterado para coincidir.
- Caso que altera dados: o plano diz como restaurar antes de repetir.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O plano MUST ser entregue em `docs/testes/plano-de-testes.md`, legível sem outro documento aberto; ligações usam caminho relativo válido.
- **FR-002**: O plano MUST cobrir os 16 cenários da issue (T01 Autenticação, T02 Isolamento entre pacientes, T03 Permissões administrativas, T04 Criação e edição de rascunho, T05 Upload e extração válidos, T06 Documentos não aceitos, T07 Documento completo, T08 Informações ou documentos ausentes, T09 Regras condicionais, T10 Leitura não confiável, T11 Reenvio e revisão documental, T12 Falhas e limites da IA, T13 Submissão simulada, T14 Exclusão permitida e bloqueada, T15 Conectividade e sessão, T16 Sensores simulados) com **todos os casos** que a issue lista em "Documentar casos para".
- **FR-003**: Variações com resultados distintos MUST ser casos separados, com ID `Txx-yy` único.
- **FR-004**: Cada caso MUST ter: ID, título, histórias relacionadas, regra relacionada (ou "não se aplica"), pré-condições (contas, permissões, estado do pedido, dados), ambiente (versão ou commit, URL da API, dispositivo ou emulador, **sem credenciais**), tipo de execução (`API real + Oracle`, `API em memória (sem banco)`, `Teste automatizado`, `Dublê (não é integração real)` ou `App no emulador + API real`), entrada (payload, arquivo sintético ou leitura de sensor), passos numerados, impedimento conhecido (funcionalidade pendente ou `nenhum`), resultado esperado (resposta, comportamento da interface e efeito nos dados), resultado obtido, status, evidência, responsável e data da execução.
- **FR-005**: O status inicial de todo caso MUST ser "Não executado"; resultado obtido, evidência, responsável e data MUST estar em branco; os únicos status são Não executado, Aprovado, Reprovado e Bloqueado, cada um com regra de uso.
- **FR-006**: Casos documentais MUST referenciar arquivos e gabaritos existentes da RC-23 (`samples/conjuntos/<NN>-<nome>/gabarito.md`) e herdar deles regras, campos e resultado geral, sem divergir.
- **FR-007**: As regras RC-R01..RC-R06 e os resultados atende, pendente, não verificável e não aplicável MUST aparecer em pelo menos um caso cada.
- **FR-008**: O plano MUST explicar: como iniciar API, banco, app e simulador; quais contas sintéticas usar; onde estão arquivos e gabaritos da RC-23; como reproduzir falhas de IA e de conectividade de forma controlada; como restaurar os dados para repetir; quais casos usam integração real (Oracle, API, emulador) e quais usam simulação ou dublê; onde salvar evidências.
- **FR-009**: Evidências MUST seguir `docs/evidencias/<ID-DO-CASO>/`, com conteúdo mínimo definido (data, versão ou commit, comando ou passos, resultado bruto, status).
- **FR-010**: O plano MUST incluir o percurso integrado da paciente (login → pedido → upload → extração → conferência → validação → resumo → correção → reanálise → submissão simulada), reutilizando os casos, com o resumo e a leitura identificados como simulados.
- **FR-011**: Falhas de IA, conexão e sensores MUST ter procedimento reproduzível com comandos ou ações que existam no repositório; onde não existir meio controlado, o caso MUST declarar a limitação em vez de inventá-lo.
- **FR-012**: O plano MUST ter matriz de rastreabilidade história → casos e regra → casos, declarando lacunas e os casos com impedimento conhecido (funcionalidade pendente), **sem** alterar o status inicial deles. Separar **decisões de escopo aprovadas** (por exemplo, leitura e resumo simulados, decisão de 12/11/2026) de **funcionalidades ainda pendentes** (por exemplo, indicadores da RC-20, alertas da RC-22). Casos parcialmente verificáveis MUST ser divididos em casos separados, cada um com seu resultado, e nenhum cenário é aprovado por ter testado só parte do comportamento.
- **FR-013**: Resultados esperados MUST se apoiar no contrato real documentado em `docs/api/` (códigos HTTP, erros, estados); quando a issue e o contrato diferirem, o plano usa o contrato e registra a diferença.
- **FR-014**: O plano MUST declarar que a leitura e o resumo são simulados (decisão de 12/11/2026), que o resultado não é aprovação do SUS e que dublês não comprovam integração real.
- **FR-015**: Esta feature MUST NOT executar casos, registrar resultados ou evidências, alterar código de produção ou de teste, nem concluir a RC-25. Atribuir à RC-25 a execução, as evidências e os retestes.
- **FR-016**: Divergência entre observado e esperado MUST ser registrada como achado, sem editar o esperado.
- **FR-017**: A revisão por outro integrante (critério de aceite da issue) MUST ficar registrada como **pendente** até acontecer; esta feature não a declara feita.

### Key Entities

- **Cenário (Txx)**: grupo temático da issue, com histórias e regras associadas e um ou mais casos.
- **Caso de teste (Txx-yy)**: situação reproduzível com os campos de FR-004.
- **Matriz de rastreabilidade**: histórias e regras → casos, com lacunas e bloqueios previstos.
- **Evidência**: pasta `docs/evidencias/<ID-DO-CASO>/` com o material de uma execução.
- **Ambiente de execução**: serviços, contas fictícias, data de referência, material e procedimentos de falha controlada e de restauração.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 16 de 16 cenários (T01..T16) presentes e 100% dos casos listados na issue existem como itens separados.
- **SC-002**: 100% dos casos têm todos os campos de FR-004, e 100% estão "Não executado" com os campos de execução em branco; 0 aprovados.
- **SC-003**: Toda entrada documental aponta um arquivo ou gabarito que existe em `samples/`; 0 referências quebradas.
- **SC-004**: As 6 regras e os 4 resultados por regra aparecem em pelo menos 1 caso cada; as histórias citadas na issue (RC-01, RC-04 a RC-17, RC-19 a RC-22) têm caso, e as sem funcionalidade pronta estão marcadas como lacuna.
- **SC-005**: Todo comando citado existe no repositório na data da entrega (verificado por leitura do repositório, sem executar o caso).
- **SC-006**: Uma pessoa que não escreveu o plano executa um caso escolhido, da leitura ao registro, sem pedir esclarecimento (a verificação é da RC-25; a revisão por outro integrante fica pendente até ocorrer).

## Assumptions

- A issue #50 foi atualizada em 18/11/2026 com os 16 cenários; a suposição anterior (derivá-los das histórias) foi **descartada**. A spec segue a issue.
- Diferenças entre a issue e o produto, a tratar no plano (FR-013) e a explicitar ao responsável:
  - **Extração e resumo**: a issue diz "extração real" (T05) e "resumo real por IA" (percurso integrado). A decisão do projeto (12/11/2026) é leitura e resumo **simulados**; o plano os testa como simulados e não afirma integração real.
  - **T03 (indicadores)**: as rotas de indicadores do administrador não existem (RC-20, **P1**, aberta; a issue #43 já a classifica como P1, "só se sobrar tempo", e isso é registrado na spec e no plano). Os casos ficam com impedimento conhecido; passam a "Bloqueado" somente quando a RC-25 tentar executá-los e registrar o impedimento.
  - **T16 (sensores)**: separado em casos do **backend** (faixa, leitura desatualizada, credencial, não bloqueio do upload; testáveis hoje) e casos dos **alertas na interface** (dependem da RC-22, aberta; impedimento conhecido). Um grupo não aprova o outro.
  - **T04 (editar campo)**: o único campo editável hoje é o serviço do rascunho; o caso testa a troca de serviço. **T06 (PDF protegido)**: espera-se HTTP 400 `REQUISICAO_INVALIDA` (contrato em `docs/api/erros.md`). O que a issue pede além disso é declarado como lacuna.
  - **Dublês**: todo caso que usa dublê (falha de IA, saída fora do contrato, conexão cortada) é identificado como tal e não é apresentado como integração real.
  - **T02 ("baixar documento")**: existe `GET /solicitacoes/{id}/documentos/{docId}`; o plano o usa.
  - **T12 (provedor de IA indisponível, resposta fora do contrato)**: só é reproduzível com dublê/adaptador de teste; o plano diz qual e que isso não comprova provedor real.
- A RC-23 está entregue: conjuntos 01 a 15 e gabaritos em `samples/` são a fonte do resultado esperado documental. O plano não cria gabarito novo.
- RC-18 e RC-19 (app) já foram mergeados (PRs #71 e #74); casos de app indicam o commit do app testado e só ficam bloqueados se o app não estiver instalável no emulador.
- Casos podem citar testes automatizados já existentes como forma de execução, sem que isso conte como execução desta feature.
- Dados, contas e documentos são fictícios; nenhum dado real entra no plano.
