# Feature Specification: Perfis e isolamento de dados (RC-05)

**Feature Branch**: `005-perfis-isolamento`

**Created**: 2026-11-12

**Status**: Implementado e verificado em 12/11/2026 como mecanismo (ver tasks.md; itens não verificados listados no relatório final)

**Backlog**: RC-05 — Code-Masters-F/ClickBus#11 · Épico 2 (Conta e proteção de acesso) · P0 · até 14/11/2026 · depende da RC-04 (concluída)

**Input**: User description: "RC-05 — Aplicar perfis e isolamento de dados. Como paciente, quero que apenas pessoas autorizadas acessem meus pedidos e arquivos."

## Contexto e limite desta história

A RC-04 entregou o login, a sessão e o ponto único que identifica quem chamou. Esta história decide **o que cada tipo de conta pode fazer** e garante que uma paciente jamais enxergue dados de outra. As entidades de negócio (solicitação, documento, análise, resumo, submissão, leitura de sensor) pertencem a outras histórias e ainda não têm rotas. Por isso esta história entrega a **regra de autorização reutilizável**, os **códigos de resposta documentados** e a **prova de que a regra funciona** em todas as combinações de perfil e dona do recurso, sem criar telas, listas ou cadastros de negócio. As histórias seguintes só precisarão declarar a qual perfil cada rota pertence e quem é a dona do recurso.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Paciente só enxerga o que é seu (Priority: P1)

Uma paciente autenticada só consegue ler ou alterar solicitações, documentos, análises, resumos e submissões que pertencem a ela. Mesmo que conheça ou adivinhe o identificador de um registro de outra paciente, a resposta é a mesma de um registro que não existe.

**Why this priority**: é o critério central do aceite e protege dados de saúde de terceiros; um erro aqui expõe documentos de outra pessoa.

**Independent Test**: com duas pacientes sintéticas (A e B) e um recurso de cada uma, a paciente A pede o recurso de B pelo identificador e recebe "não encontrado", idêntico ao pedido de um identificador inexistente; pede o próprio e recebe sucesso.

**Acceptance Scenarios**:

1. **Given** a paciente A autenticada e um recurso que pertence à paciente B, **When** A tenta consultá-lo pelo identificador, **Then** recebe 404 "não encontrado", com corpo idêntico ao de um identificador que nunca existiu.
2. **Given** o mesmo cenário, **When** A tenta alterar ou excluir o recurso de B, **Then** recebe o mesmo 404 e nada é alterado.
3. **Given** a paciente A autenticada, **When** consulta um recurso próprio, **Then** recebe sucesso normalmente.
4. **Given** qualquer pedido de criação ou alteração, **When** o corpo traz um campo que indica a dona do recurso (por exemplo, um identificador de dono diferente da sessão), **Then** esse campo é ignorado ou recusado e a dona é sempre a conta da sessão.
5. **Given** uma listagem de recursos da paciente A, **When** a paciente B também tem recursos, **Then** a listagem de A contém somente os de A.
6. **Given** a regra vale para os cinco tipos de recurso (solicitação, documento, análise, resumo, submissão), **When** cada um é consultado por outra paciente, **Then** o resultado é o mesmo 404 para todos.

---

### User Story 2 - Administrador vê protocolo e indicadores, não documentos (Priority: P1)

O perfil administrador pode consultar o protocolo acadêmico e os indicadores agregados (totais e proporções sem identificar pacientes), mas não recebe acesso aos documentos, análises, resumos ou submissões individuais das pacientes por ter esse perfil.

**Why this priority**: o aceite proíbe expressamente acesso irrestrito do administrador; sem isso, a conta administrativa seria uma porta para todos os documentos.

**Independent Test**: com a conta de administrador sintética, acessar uma rota de protocolo/indicadores e receber sucesso; acessar qualquer rota de recurso individual de uma paciente e receber recusa por perfil, sem nenhum dado do recurso na resposta.

**Acceptance Scenarios**:

1. **Given** a conta de administrador autenticada, **When** acessa uma rota reservada ao administrador (protocolo ou indicadores agregados), **Then** recebe sucesso.
2. **Given** a conta de administrador autenticada, **When** tenta ler, baixar, alterar ou excluir um documento, solicitação, análise, resumo ou submissão de uma paciente, **Then** recebe 403 "acesso negado" e nenhum dado do recurso é revelado.
3. **Given** uma paciente autenticada, **When** tenta acessar uma rota reservada ao administrador, **Then** recebe 403 "acesso negado".
4. **Given** os indicadores agregados, **When** consultados, **Then** não contêm nome, e-mail, identificador de paciente nem conteúdo de documento.
5. **Given** a ausência de acesso por padrão, **When** no futuro um acesso excepcional for desejado, **Then** ele exige uma decisão explícita registrada e não existe nesta história.

---

### User Story 3 - Todas as rotas privadas exigem autenticação e autorização no servidor (Priority: P1)

Toda rota privada confere, no servidor, primeiro quem chama (autenticação) e depois se aquele perfil pode usar aquela rota e aquele recurso (autorização). A validação feita pelo aplicativo móvel não substitui a do servidor. Os códigos de resposta de cada situação estão documentados no contrato.

**Why this priority**: a proteção só vale se não houver rota esquecida; um acesso sem conferência anula as demais regras.

**Independent Test**: percorrer a lista de rotas privadas do contrato e, para cada uma, chamar sem token, com token inválido, com perfil indevido e com perfil correto; conferir o código esperado do contrato em cada caso. Tentar registrar uma rota privada sem perfil falha na montagem, e uma rota publicada fora do mecanismo é detectada pelo teste de omissão no build.

**Acceptance Scenarios**:

1. **Given** uma rota privada, **When** chamada sem token, com token inválido ou expirado, **Then** recebe 401 "não autenticado" antes de qualquer conferência de perfil ou de recurso.
2. **Given** uma rota privada, **When** chamada por um perfil que não é permitido nela, **Then** recebe 403 "acesso negado".
3. **Given** uma rota de recurso individual, **When** chamada por um perfil permitido mas por quem não é a dona do recurso, **Then** recebe 404 "não encontrado".
4. **Given** uma rota privada nova, **When** se tenta registrá-la sem perfil permitido ou publicá-la fora do mecanismo de declaração, **Then** o registro é recusado na montagem ou o teste de omissão falha no build, de modo que ela não chega a produção sem conferência.
5. **Given** o contrato de erros, **When** consultado, **Then** descreve quando cada código ocorre (401 sem identidade válida; 403 perfil sem permissão; 404 recurso inexistente ou de outra paciente) e a ordem de verificação.
6. **Given** a rota pública de saúde técnica e as rotas de login, **When** chamadas sem token, **Then** continuam públicas e funcionam normalmente.

---

### User Story 4 - Credencial do simulador restrita à telemetria (Priority: P1)

O simulador de sensores usa uma credencial própria (uma chave de configuração, não uma conta de pessoa), que serve apenas para enviar leituras a uma sessão de captura existente e aberta. Essa credencial não abre nenhum documento nem nenhuma outra rota, e a conta de paciente ou de administrador também não consegue enviar leituras como se fosse o simulador.

**Why this priority**: o simulador roda fora do aplicativo, num script; uma credencial com alcance maior que o necessário seria uma porta lateral para os documentos.

**Independent Test**: com a credencial do simulador, enviar uma leitura para uma sessão de captura aberta e receber sucesso; usar a mesma credencial em qualquer rota de documento, solicitação, análise ou indicador e receber 403; usar a credencial para uma sessão de captura inexistente ou encerrada e receber 404.

**Acceptance Scenarios**:

1. **Given** a credencial do simulador, **When** usada na rota de telemetria de uma sessão de captura existente e aberta, **Then** é aceita.
2. **Given** a credencial do simulador, **When** usada em qualquer rota de documentos, solicitações, análises, resumos, submissões, protocolo ou indicadores, **Then** recebe 403 "acesso negado".
3. **Given** a credencial do simulador, **When** usada numa sessão de captura inexistente ou já encerrada, **Then** a resposta é 404 "não encontrado", sem revelar qual das duas situações ocorreu.
4. **Given** uma conta de paciente ou de administrador, **When** tenta enviar leituras na rota de telemetria do simulador, **Then** recebe 403 "acesso negado".
5. **Given** a credencial do simulador, **When** examinada nos arquivos do repositório e nos logs, **Then** não aparece em texto puro (só o nome da variável de configuração está documentado).

---

### User Story 5 - Regra de autorização reutilizável e verificada (Priority: P2)

As próximas histórias (rascunho, upload, análise, submissão, telemetria, indicadores) declaram numa linha qual perfil usa cada rota e quem é a dona do recurso, sem reescrever a regra. Há testes que provam a regra para cada combinação de perfil e dona, e um guia curto explica como aplicá-la a uma rota nova.

**Why this priority**: o risco real é uma história futura esquecer a conferência; um mecanismo simples de usar e testes reaproveitáveis reduzem esse risco.

**Independent Test**: seguir o guia para proteger uma rota de exemplo usada só em teste e ver as quatro respostas esperadas (401, 403, 404, sucesso) sem escrever nova lógica de autorização.

**Acceptance Scenarios**:

1. **Given** o guia de uso, **When** uma pessoa do grupo o segue para proteger uma rota de exemplo, **Then** obtém as quatro respostas esperadas sem lógica própria.
2. **Given** a matriz de testes, **When** executada, **Then** cobre perfil × tipo de recurso × dona (própria, de outra paciente, inexistente) e todas passam.
3. **Given** um teste de regressão, **When** uma rota privada é publicada fora do mecanismo de declaração, **Then** o teste falha, impedindo a rota esquecida.

---

### Edge Cases

- Identificador de recurso em formato inválido (texto, negativo, enorme): responde como "não encontrado" ou requisição inválida de forma consistente, sem erro interno e sem revelar existência.
- Paciente tenta acessar recurso de outra pela rota aninhada (ex.: documento de B dentro da solicitação de A): a dona do documento é verificada, não só a da solicitação informada na rota.
- Recurso cuja dona foi desativada: continua inacessível a todas as outras contas; a conta desativada não autentica (RC-04).
- Sessão válida cujo perfil mudou no banco depois do login: vale o perfil gravado no momento da requisição, não o do login. [Premissa registrada; ver Assumptions]
- Timing: a diferença de tempo entre "não é seu" e "não existe" não deve permitir descobrir existência de forma prática; a verificação de dono é feita na mesma consulta que busca o recurso. (Garantia de desenho; não há teste de cronometragem, por ser instável.)
- Administrador pede lista de recursos individuais: recebe 403, não uma lista vazia, para não sugerir que não há dados.
- Mensagens de erro de 403 e 404 não contêm nome da dona, identificador interno, SQL nem detalhes do recurso.
- Tentativa de usar token de uma conta em rota do simulador e vice-versa: 403 "acesso negado" nos dois sentidos, sem tratamento especial.
- Método HTTP não permitido numa rota privada responde 405 depois da autenticação, sem vazar existência.
- Logs de recusa registram identificador técnico, perfil, rota e código, nunca o token, a credencial do simulador nem o conteúdo do recurso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que a paciente acesse somente solicitações, documentos, análises, resumos e submissões de que ela é a dona.
- **FR-002**: Quando a paciente pedir um recurso de outra paciente pelo identificador, o sistema MUST responder 404 "não encontrado", com corpo idêntico ao de um identificador inexistente (não revelar existência).
- **FR-003**: Leitura, alteração e exclusão de recurso de outra paciente MUST ser recusadas da mesma forma (404) e MUST NOT alterar nenhum dado.
- **FR-004**: A dona de um recurso MUST ser sempre a conta da sessão autenticada; o sistema MUST NOT aceitar identificador de dono vindo do corpo, do caminho ou de parâmetro como autoridade.
- **FR-005**: Listagens MUST conter somente recursos da conta autenticada.
- **FR-006**: Em recursos aninhados (documento dentro de solicitação), o sistema MUST verificar a dona do recurso final e a coerência entre pai e filho.
- **FR-007**: O perfil administrador MUST poder consultar protocolo e indicadores agregados, e MUST NOT receber acesso a documentos, solicitações, análises, resumos ou submissões individuais por padrão; essas tentativas MUST responder 403.
- **FR-008**: Indicadores agregados MUST NOT identificar pacientes nem expor conteúdo de documentos.
- **FR-009**: Toda rota privada MUST exigir autenticação válida e MUST conferir no servidor se o perfil da conta é permitido na rota, antes de executar qualquer efeito.
- **FR-010**: O mecanismo de declaração de rotas privadas MUST exigir ao menos um perfil permitido (rota sem perfil não pode ser registrada) e o build MUST falhar se existir rota privada publicada fora desse mecanismo (negar por omissão, verificado em teste automatizado).
- **FR-011**: A ordem de verificação MUST ser: autenticação (401), perfil permitido na rota (403), dona do recurso (404), validação do pedido (400) e regra de negócio.
- **FR-012**: As rotas públicas (saúde técnica e login) MUST permanecer sem exigência de sessão.
- **FR-013**: O perfil MUST ser o gravado na conta no momento da requisição, não uma cópia confiada do momento do login. [Ver premissa]
- **FR-014**: O papel de simulador MUST ter credencial própria, restrita ao envio de leituras para uma sessão de captura existente e aberta. Limitação conhecida e documentada: com chave única, a credencial não distingue de qual paciente é a sessão; o vínculo por sessão/paciente fica para a história dos sensores (RC-21), se for exigido.
- **FR-015**: A credencial do simulador MUST NOT permitir acesso a documentos, solicitações, análises, resumos, submissões, protocolo, indicadores ou qualquer outra rota privada (403).
- **FR-016**: Contas de paciente e de administrador MUST NOT conseguir usar a rota de telemetria do simulador (403).
- **FR-017**: A credencial do simulador MUST ficar fora do repositório, dos logs e do aplicativo móvel; o repositório documenta apenas o nome da variável de configuração.
- **FR-018**: O contrato de erros MUST documentar 401, 403 e 404 com os casos de uso de cada um, a ordem de verificação e o formato padrão de erro.
- **FR-019**: A documentação das rotas privadas do contrato MUST indicar, para cada uma, o perfil permitido e a regra de dona.
- **FR-020**: O sistema MUST fornecer um mecanismo único e reutilizável para declarar perfil permitido e regra de dona numa rota, de modo que histórias futuras não repitam a lógica.
- **FR-021**: O sistema MUST registrar recusas (401, 403, 404 por dona) com identificador técnico, perfil, rota e código, sem token, credencial do simulador, senha nem conteúdo de recurso.
- **FR-022**: A história MUST entregar testes cobrindo perfil × tipo de recurso × dona (própria, de outra paciente, inexistente) e a recusa por omissão de rota sem declaração.
- **FR-023**: A história MUST NOT criar CRUD de negócio, tela, leitura de sensor funcional nem regra documental; para a prova, MAY usar rotas de exemplo existentes apenas no ambiente de teste.
- **FR-024**: Qualquer necessidade de nova coluna ou tabela MUST usar prefixo `RC_`, script aditivo em `database/scripts/` e dados sintéticos, sem alterar o que a RC-03 e a RC-04 já criaram.

### Key Entities *(include if feature involves data)*

- **Perfil**: conjunto fechado de tipos de conta de pessoa — paciente e administrador. Define o que a conta pode fazer. O simulador **não** é perfil de conta: é uma credencial de configuração à parte (ver abaixo) e não usa o login da paciente.
- **Regra de acesso da rota**: para cada rota privada, o perfil permitido e se há dona de recurso a conferir. Não é possível registrar rota privada sem essa regra.
- **Dona do recurso**: a conta de paciente à qual um pedido, documento, análise, resumo, submissão ou sessão de captura pertence. É derivada da sessão, nunca do pedido.
- **Credencial do simulador**: segredo de configuração que identifica o script de sensores; única por ambiente; alcance restrito à telemetria de sessões de captura abertas.
- **Sessão autenticada** (da RC-04): identidade da conta e perfil, usada como ponto de partida da autorização.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das tentativas de acesso por uma paciente a recursos de outra paciente, nos cinco tipos de recurso e nas operações de leitura, alteração e exclusão, a resposta é a mesma de um recurso inexistente e nenhum dado é alterado.
- **SC-002**: Em 100% das tentativas do administrador de acessar recursos individuais de pacientes, a resposta é recusa por perfil sem nenhum dado do recurso.
- **SC-003**: Em 100% das rotas privadas do contrato, a chamada sem sessão válida é recusada antes de qualquer outra conferência.
- **SC-004**: Não é possível registrar uma rota privada sem perfil declarado pelo mecanismo do projeto, e uma rota publicada fora dele é detectada por teste automatizado no build.
- **SC-005**: A credencial do simulador é recusada em 100% das chamadas a qualquer rota que não seja a de telemetria.
- **SC-006**: Nenhum valor de token, credencial do simulador ou senha aparece em respostas, logs ou arquivos versionados (verificado por varredura).
- **SC-007**: Uma pessoa do grupo consegue proteger uma rota nova seguindo apenas o guia, em até 10 minutos, e obter as quatro respostas esperadas (401, 403, 404, sucesso). [Meta; só vale se medida com uma pessoa.]
- **SC-008**: A matriz de testes de perfil × recurso × dona roda em menos de 1 minuto sem depender de banco externo e é repetível.

## Assumptions

- A RC-04 está mergeada: login, sessão de 30 minutos e o ponto único de autenticação existem e identificam conta e perfil.
- As rotas de negócio (solicitações, documentos, análises, resumos, submissões e leituras) ainda não existem. Esta história as protege **por contrato e por mecanismo**, e prova o comportamento com rotas de exemplo restritas a testes. Cada história futura deve aplicar o mecanismo à sua rota e repetir a matriz de testes do seu recurso.
- O contrato de erros já reserva 403 `ACESSO_NEGADO` (perfil sem permissão) e 404 `NAO_ENCONTRADO` (inexistente ou de outra paciente); esta história os formaliza e documenta o uso, sem mudar o formato.
- Decisão adotada: para recurso de outra paciente a resposta é **404**, não 403, para não revelar que o identificador existe. 403 fica para perfil sem permissão na rota.
- O perfil é lido a cada requisição a partir da conta (premissa de segurança: mudança de perfil vale na hora). Se o custo de uma consulta por requisição se mostrar um problema, a decisão será revista no plano.
- Decisão do plano (12/11/2026): o simulador é uma chave de configuração (`IOT_API_KEY`, já prevista no projeto), não uma conta em `RC_USUARIO`; logo esta história não altera o banco. A chave não pertence a uma paciente, então "sessão de outra paciente" não é distinguível; a recusa vale para sessão inexistente ou encerrada.
- A rota de telemetria só será implementada na história dos sensores; aqui a regra é provada com uma rota de exemplo de teste. A verificação de sessão de captura "existente e aberta" usa a tabela de sessões de captura da RC-03, que já existe.
- Perfis do banco continuam paciente e administrador; o simulador não é perfil de conta. Se uma história futura exigir vínculo da chave a uma sessão/paciente, será por script aditivo `RC_`, executado na conta Oracle do usuário somente com autorização.
- Acesso excepcional do administrador a documentos (por exemplo, auditoria) está **fora do escopo** e exigiria decisão explícita.
- Fora do escopo: cadastro de contas, recuperação de senha, MFA, proteção contra força bruta, tela Android, CRUDs de negócio, gestão de papéis por interface, revogação em massa de sessões, auditoria completa de acesso.
- Dados sintéticos apenas: as contas e documentos de prova são fictícios (domínio `.invalid`).
