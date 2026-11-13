# Feature Specification: Criar rascunho e selecionar serviço (RC-07)

**Feature Branch**: `008-rascunho-selecionar-servico`

**Created**: 2026-11-13

**Status**: Especificação pronta para planejamento (nada implementado)

**Backlog**: RC-07 — Code-Masters-F/ClickBus#14 · História · P0 · A/C (API e cliente) · 3 h · até 14/11/2026 · depende de RC-01, RC-03 e RC-04 (concluídas)

**Input**: User description: "RC-07 — Criar rascunho e selecionar serviço. Como paciente, quero escolher o serviço do meu encaminhamento para usar o conjunto correto de critérios."

## Contexto e limite desta história

É o primeiro passo do percurso da paciente: antes de anexar qualquer documento (RC-08), ela precisa ter um **pedido em rascunho** ligado ao **serviço** do seu encaminhamento, porque é o serviço que define o conjunto de critérios documentais usado depois. No MVP existe um único serviço demonstrativo: o protocolo acadêmico fictício (localidade fictícia e especialidade fictícia), identificado como tal em toda resposta e tela.

Esta história entrega: mostrar os serviços disponíveis, criar um rascunho do pedido, alterá-lo enquanto for rascunho, listar os pedidos próprios e reabrir um deles. O pedido nasce na revisão 1 e o histórico de revisões começa nele.

**Fica fora desta história:** envio e conferência de arquivos (RC-08), extração, análise, resumo de IA, submissão simulada, exclusão de rascunho, lista rica com ação sugerida por estado (RC-18) e o desenho das telas (épico 007). Não existe cadastro de novos serviços nem escolha de localidade livre: só o que o catálogo demonstrativo oferece. Nada aqui diagnostica, prioriza ou coloca a paciente em fila real.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver os serviços disponíveis e criar o rascunho (Priority: P1)

A paciente autenticada abre o formulário de novo pedido e vê a localidade e o serviço demonstrativos, com o aviso de que se trata de protocolo acadêmico fictício. Ela escolhe o serviço e cria o rascunho. O pedido fica ligado a ela e ao serviço escolhido, no estado de rascunho, e ela vê a confirmação com o identificador do pedido.

**Why this priority**: sem rascunho não há onde anexar documentos; é a porta de entrada do percurso inteiro e a base de todas as histórias seguintes.

**Independent Test**: autenticar como paciente sintética, consultar os serviços, criar um rascunho e consultar o pedido criado. Verificar dono, serviço, estado, revisão 1 e aviso de simulação.

**Acceptance Scenarios**:

1. **Given** uma paciente autenticada, **When** abre o formulário de novo pedido, **Then** vê a localidade e o serviço demonstrativos vindos do sistema, cada um acompanhado do aviso de protocolo acadêmico fictício.
2. **Given** uma paciente autenticada e um serviço ativo escolhido, **When** cria o rascunho, **Then** recebe confirmação de criação com o pedido em estado de rascunho, revisão 1, ligado ao serviço escolhido e marcado como simulação.
3. **Given** uma criação concluída, **When** a paciente consulta o pedido, **Then** os mesmos dados são devolvidos, comprovando que foram persistidos e não apenas respondidos.
4. **Given** uma requisição de criação que traga um dono, identificador de paciente ou estado, **When** é processada, **Then** esses campos são ignorados ou recusados: o dono é sempre a paciente da sessão e o estado inicial é sempre rascunho.
5. **Given** uma pessoa sem autenticação, **When** tenta ver os serviços ou criar um rascunho, **Then** é recusada com o erro de autenticação padrão e nada é criado.
6. **Given** uma pessoa com perfil de administrador ou o perfil do simulador, **When** tenta criar um rascunho, **Then** é recusada por falta de permissão e nada é criado.

---

### User Story 2 - Corrigir o serviço escolhido enquanto é rascunho (Priority: P1)

A paciente percebe que escolheu o serviço errado e altera o rascunho. Se a troca muda o conjunto de critérios que será usado, a revisão do pedido aumenta, para que nada produzido com a escolha anterior seja considerado vigente.

**Why this priority**: é critério de aceite da história e protege a regra central do sistema de que resultado antigo não vale para dado novo.

**Independent Test**: criar um rascunho, alterá-lo para outro serviço ativo e conferir a nova revisão; repetir a alteração com o mesmo serviço e conferir que a revisão não muda.

**Acceptance Scenarios**:

1. **Given** um rascunho próprio, **When** a paciente o altera para um serviço ativo diferente, **Then** o pedido passa a apontar para o novo serviço e a revisão aumenta em um, ficando registrado o motivo da nova revisão.
2. **Given** um rascunho próprio, **When** a paciente o salva com o mesmo serviço que já tem, **Then** a resposta é de sucesso e a revisão permanece a mesma.
3. **Given** um pedido que já saiu do estado de rascunho, **When** a paciente tenta alterá-lo por esta funcionalidade, **Then** a alteração é recusada com conflito de estado, explicação clara e nenhum dado muda.
4. **Given** duas alterações simultâneas do mesmo rascunho, **When** ambas são processadas, **Then** o resultado final é coerente: o pedido termina com um único serviço e o número de revisão reflete exatamente as mudanças realmente aplicadas, sem salto nem repetição.
5. **Given** um pedido de outra paciente, **When** a paciente tenta alterá-lo pelo identificador, **Then** recebe o mesmo erro de "não encontrado" de um pedido inexistente e nada muda.

---

### User Story 3 - Listar e reabrir meus pedidos (Priority: P1)

A paciente consulta a lista dos seus pedidos e reabre um deles, recuperando exatamente o que foi salvo, inclusive depois de sair e entrar de novo.

**Why this priority**: é critério de aceite explícito e é o que permite retomar um rascunho; também comprova o isolamento entre pacientes.

**Independent Test**: criar dois rascunhos com a paciente A e um com a paciente B; listar como A (só os dois dela), reabrir um deles e tentar abrir o de B como A.

**Acceptance Scenarios**:

1. **Given** uma paciente com pedidos, **When** consulta a lista, **Then** recebe somente os pedidos dela, com identificador, serviço, estado, revisão e datas de criação e atualização, do mais recente para o mais antigo.
2. **Given** uma paciente sem pedidos, **When** consulta a lista, **Then** recebe uma lista vazia com sucesso, não um erro.
3. **Given** um pedido próprio, **When** a paciente o reabre, **Then** vê os dados persistidos, incluindo o serviço escolhido e o aviso de simulação.
4. **Given** um identificador de pedido de outra paciente, inexistente ou malformado, **When** a paciente o reabre, **Then** recebe sempre o mesmo erro padrão de "não encontrado", sem revelar se o pedido existe.
5. **Given** a paciente que encerrou a sessão e entrou novamente, **When** consulta a lista, **Then** os rascunhos criados antes continuam presentes e iguais.

---

### User Story 4 - Entender o que está errado na entrada (Priority: P2)

Quando algo obrigatório falta ou é inválido, a paciente recebe uma indicação identificável do campo com problema e do que fazer, e o que ela digitou não se perde no cliente.

**Why this priority**: critério de aceite explícito, mas depende das histórias anteriores para ter o que validar; sozinho não entrega o percurso.

**Independent Test**: enviar criações e alterações com serviço ausente, em branco, inexistente, inativo, de tipo errado e com campos desconhecidos, e conferir o erro de cada uma.

**Acceptance Scenarios**:

1. **Given** uma criação sem o serviço, **When** é processada, **Then** é recusada com erro de validação que aponta o campo do serviço como obrigatório e nada é criado.
2. **Given** um serviço que não existe no catálogo, **When** é informado, **Then** é recusado com erro de validação que aponta o campo e orienta escolher um serviço da lista.
3. **Given** um serviço que existe mas não está ativo, **When** é informado, **Then** é recusado da mesma forma que um serviço inexistente, sem expor detalhes internos.
4. **Given** um corpo de requisição malformado, de tipo errado ou vazio, **When** é processado, **Then** a resposta é um erro de contrato identificável, distinto de erro de regra de negócio, e nada é criado ou alterado.
5. **Given** qualquer recusa, **When** a resposta é montada, **Then** segue o formato de erro do projeto, em português, sem texto de SQL, rastro de execução ou caminho interno.

---

### Edge Cases

- A paciente toca duas vezes no botão de criar: o cliente deve evitar o envio duplicado; o servidor **não** promete deduplicar e, se duas criações chegarem, cria dois rascunhos distintos, ambos visíveis na lista. Isso fica documentado.
- Nenhum serviço ativo no catálogo: o formulário informa que não há serviço disponível e a criação é recusada; não é inventado um serviço.
- Um serviço é desativado depois de já ter pedidos: os pedidos existentes continuam consultáveis com o serviço original; novas criações e trocas para ele são recusadas.
- Identificador de pedido com formato inválido (texto, negativo, muito longo): tratado como "não encontrado", nunca como erro interno.
- Falha ao gravar no banco: a resposta é erro padrão sem detalhe técnico e o pedido não aparece pela metade (pedido e revisão inicial são gravados juntos ou nenhum).
- Falha de conexão no cliente: o que a paciente escolheu é preservado na tela e nada é anunciado como salvo sem confirmação do servidor.
- Sessão expirada no meio do preenchimento: o cliente recebe o erro de autenticação padrão e pode retomar após novo login sem perder a escolha na tela.
- Lista com muitos pedidos: a resposta continua limitada e ordenada de forma estável (ver Assumptions).
- Tentativa de enviar documentos, arquivos ou dados clínicos junto do rascunho: fora desta história, ignorados ou recusados conforme o contrato; nenhum dado clínico é aceito ou inventado aqui.

## Requirements *(mandatory)*

### Functional Requirements

**Catálogo de serviços**

- **FR-001**: O sistema MUST permitir que uma paciente autenticada consulte os serviços ativos disponíveis, informando para cada um localidade, especialidade, identificação e versão do protocolo e o aviso de protocolo acadêmico fictício.
- **FR-002**: O sistema MUST apresentar apenas serviços ativos nessa consulta e MUST indicar de forma explícita quando não houver nenhum.
- **FR-003**: Nenhuma resposta MUST apresentar o protocolo como exigência oficial do SUS ou de serviço real.

**Criar rascunho**

- **FR-004**: O sistema MUST permitir que uma paciente autenticada crie um pedido em rascunho escolhendo um serviço ativo.
- **FR-005**: O dono do pedido MUST ser sempre a paciente da sessão; qualquer identificador de dono, paciente ou estado enviado pelo cliente MUST ser ignorado ou recusado.
- **FR-006**: O pedido MUST nascer no estado de rascunho, na revisão 1, com a revisão inicial registrada com motivo de criação, e MUST ser marcado como simulação.
- **FR-007**: O pedido e sua revisão inicial MUST ser gravados de forma atômica; sucesso só é retornado depois de a gravação ser confirmada.
- **FR-008**: A resposta de criação MUST informar identificador, serviço escolhido, estado, revisão, datas de criação e atualização e o aviso de simulação, sem expor campos internos.

**Alterar rascunho**

- **FR-009**: O sistema MUST permitir que a paciente dona altere o serviço de um pedido enquanto ele estiver em rascunho.
- **FR-010**: Trocar para um serviço diferente MUST incrementar a revisão em um e registrar o motivo da nova revisão; salvar com o mesmo serviço MUST NOT alterar a revisão.
- **FR-011**: Alterar um pedido fora do estado de rascunho MUST ser recusado com erro de conflito de estado, sem modificar nada.
- **FR-012**: O incremento de revisão MUST ser seguro sob alterações simultâneas: a verificação do estado e a gravação não podem permitir salto, repetição de número de revisão nem alteração de um pedido que deixou de ser rascunho entre a verificação e a gravação.
- **FR-013**: Cada alteração MUST atualizar a data de atualização do pedido somente quando algo realmente mudar.

**Listar e reabrir**

- **FR-014**: O sistema MUST listar apenas os pedidos da paciente da sessão, ordenados do mais recente para o mais antigo, com identificador, serviço, estado, revisão e datas.
- **FR-015**: O sistema MUST permitir reabrir um pedido próprio e devolver os dados persistidos.
- **FR-016**: Pedido de outra paciente, inexistente ou com identificador inválido MUST produzir o mesmo erro padrão de "não encontrado", em consulta e em alteração.
- **FR-017**: Rascunhos MUST permanecer disponíveis após encerrar e reabrir a sessão.

**Validação, erros e segurança**

- **FR-018**: O sistema MUST validar no servidor: serviço obrigatório, do tipo esperado, existente e ativo; corpo bem formado; tamanho e campos desconhecidos conforme o contrato.
- **FR-019**: Erro de contrato (corpo malformado, tipo errado) MUST ser distinguível de erro de regra (serviço inexistente ou inativo, conflito de estado), e erros de campo MUST identificar o campo.
- **FR-020**: Todas as rotas MUST exigir autenticação e perfil de paciente; administrador e simulador MUST ser recusados nelas, sem acesso a pedidos de pacientes.
- **FR-021**: Respostas de erro MUST seguir o formato padrão do projeto e MUST NOT conter SQL, rastro de execução, caminhos internos ou dados de outras pacientes.
- **FR-022**: Logs MUST registrar apenas identificadores técnicos, etapa e códigos de erro seguros, sem dados pessoais da paciente.
- **FR-023**: O sistema MUST NOT aceitar nem guardar, nesta história, documentos, dados clínicos ou texto livre sobre a condição da paciente.

**Documentação e rastreabilidade**

- **FR-024**: O contrato da API e a documentação de estados e revisões MUST ser atualizados com as rotas, entradas, saídas, erros e regras de revisão desta história, marcando o que passa a estar implementado.
- **FR-025**: A rastreabilidade RC-07 → requisitos → tarefas → arquivos → verificações MUST ser registrada nos artefatos da feature.

### Key Entities *(include if feature involves data)*

- **Serviço (protocolo acadêmico)**: opção de atendimento demonstrativa que define a localidade, a especialidade e o conjunto de critérios usado depois. Tem identificação, versão, indicação de ativo e aviso de que é fictício. No MVP há um serviço; outros só existiriam por carga explícita no banco, nunca por esta funcionalidade.
- **Solicitação (pedido)**: pedido de uma paciente, ligado a exatamente um serviço, com estado (aqui, sempre rascunho), número da revisão vigente e datas de criação e atualização. Pertence a uma única paciente.
- **Revisão**: marco numerado dentro de um pedido, com motivo e data. A revisão 1 nasce na criação; trocar de serviço abre uma nova.
- **Paciente**: dona dos pedidos, identificada pela sessão autenticada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma paciente sintética autenticada consegue ver o serviço, criar um rascunho e vê-lo na lista em menos de 1 minuto, sem precisar informar dado clínico.
- **SC-002**: 100% das criações bem-sucedidas resultam em pedido do próprio usuário da sessão, em rascunho e na revisão 1; nenhuma combinação de campos enviados pelo cliente altera dono ou estado inicial.
- **SC-003**: Em teste com duas pacientes, 0 pedidos de uma aparecem na lista ou são abertos ou alterados pela outra, e a resposta para pedido alheio é idêntica à de pedido inexistente em 100% dos casos testados.
- **SC-004**: Em 100% dos casos de entrada inválida testados (serviço ausente, inexistente, inativo, tipo errado, corpo malformado), a resposta indica o campo ou a causa em português e nenhum pedido é criado ou alterado.
- **SC-005**: Em teste de alterações simultâneas do mesmo rascunho, a sequência de revisões termina sem número repetido nem salto, e nenhuma alteração é aplicada a pedido que já saiu de rascunho.
- **SC-006**: Rascunhos criados continuam idênticos depois de encerrar e reabrir a sessão, verificado em teste.
- **SC-007**: Nenhuma resposta, tela ou documento desta história apresenta o protocolo como exigência oficial; o aviso de protocolo fictício está presente em 100% das respostas que descrevem um serviço ou um pedido.

## Assumptions

- O "serviço" do backlog corresponde ao protocolo acadêmico já modelado (localidade, especialidade, versão); não há tabela nem conceito separado de "serviço" a criar. A localidade e a especialidade são atributos do protocolo, não campos livres.
- Existe um único serviço ativo no MVP, carregado pela carga sintética já existente; a seleção continua obrigatória para manter o contrato correto quando houver mais de um.
- O contrato atual de criação aceita `protocoloId` como único campo de entrada; esta história adota esse campo como a escolha do serviço e não adiciona campos clínicos ou de texto livre. A consulta do catálogo é uma rota nova de leitura, cujo caminho e formato são decididos no plano e registrados em `docs/api/`.
- O único dado alterável do rascunho nesta história é o serviço escolhido. Edição de documentos pertence à RC-08.
- Trocar o serviço de um rascunho é uma alteração relevante (muda o conjunto de critérios) e por isso incrementa a revisão. O catálogo de motivos de revisão hoje não tem um motivo para essa troca; o plano deve prever o ajuste do script SQL e da documentação, sem apagar histórico e sem mexer em banco compartilhado de forma destrutiva.
- Excluir rascunho (`DELETE /solicitacoes/{id}`) consta no contrato geral, mas não nos critérios da RC-07; fica para a história correspondente e não é implementado aqui.
- Ordenação da lista: do mais recente para o mais antigo pela data de criação, com desempate estável. A lista é limitada em quantidade por resposta; paginação detalhada fica para o plano e, se necessária, para a RC-18.
- O servidor não deduplica criações repetidas: cada criação aceita gera um rascunho novo. A proteção contra toque duplo é responsabilidade do cliente e fica documentada.
- Dependências concluídas e reutilizadas, sem reimplementar: esquema Oracle e carga sintética (RC-03), login e sessão (RC-04), perfis e isolamento por dono com "404 padrão" (RC-05), formato padrão de erro e proteções transversais (RC-06).
- A parte de tela (formulário, lista, detalhe) é desenhada no épico 007 e implementada no aplicativo em história própria; esta especificação cobre o comportamento que a API garante e que as telas consomem.
- Somente dados sintéticos; nenhuma integração com IA, extração ou serviço externo participa desta história.
