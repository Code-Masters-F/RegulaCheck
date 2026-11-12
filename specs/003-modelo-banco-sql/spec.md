# Feature Specification: Modelo de banco e scripts SQL (RC-03)

**Feature Branch**: `003-modelo-banco-sql`

**Created**: 2026-11-12

**Status**: Draft

**Backlog**: RC-03 — Code-Masters-F/ClickBus#8 · Épico 1 (Escopo e fundação) · P0 · até 13/11/2026 · depende da RC-01 e da RC-02 (concluídas)

**Input**: User description: "RC-03 — Modelar banco e entregar SQL. Tarefa técnica: como equipe, quero persistência relacional consistente para manter o histórico da conferência."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Modelo relacional normalizado e documentado (Priority: P1)

A equipe tem um diagrama entidade-relacionamento e uma descrição das entidades, chaves, relacionamentos e cardinalidades que representa o que a conferência precisa guardar: contas, protocolo e regras, solicitações com revisão, documentos, análises com resultado por regra, pendências e leituras de sensores.

**Why this priority**: o modelo orienta os scripts, a API de acesso a dados e os testes; erros aqui se propagam.

**Independent Test**: um membro da equipe responde, só com o modelo, perguntas como "onde fica a versão do protocolo usada numa análise?" e "como saber o que mudou entre a revisão 1 e a 2?".

**Acceptance Scenarios**:

1. **Given** o documento do modelo, **When** consultado, **Then** mostra Usuário, Protocolo, Regra, Solicitação, Documento, Análise, Pendência e Leitura de sensor, com chave primária, chaves estrangeiras e cardinalidades de cada relação.
2. **Given** o modelo, **When** examinado, **Then** a paciente é a própria conta de usuário (perfil paciente), sem tabela duplicada de paciente.
3. **Given** o modelo, **When** examinado, **Then** o resultado de cada regra por análise, a revisão da solicitação, a versão do protocolo usada e o histórico por revisão têm onde ser guardados, sem duplicação de dados sem justificativa.
4. **Given** o modelo, **When** comparado com `docs/protocolo-academico.md`, o contrato de dados e a tabela de estados, **Then** nomes de estados, resultados e atributos coincidem.

---

### User Story 2 - Script de criação do esquema que executa em banco vazio (Priority: P1)

A equipe tem um script que cria todas as tabelas com chaves primárias, chaves estrangeiras, unicidade e restrições de valores permitidos (estados, resultados, perfis), executável em banco vazio e na ordem documentada.

**Why this priority**: sem esquema criado, nenhuma funcionalidade de persistência pode ser desenvolvida ou testada.

**Independent Test**: executar o script de criação em um banco vazio e conferir que todas as tabelas e restrições existem; tentar inserir valores inválidos e ver a rejeição.

**Acceptance Scenarios**:

1. **Given** um banco Oracle vazio, **When** o script de criação é executado na ordem indicada, **Then** todas as tabelas e restrições são criadas sem erro.
2. **Given** o esquema criado, **When** é inserido um estado de solicitação fora dos 7 definidos, **Then** o banco rejeita.
3. **Given** o esquema criado, **When** é inserido um resultado de regra fora de atende, pendente, não verificável ou não aplicável, ou um perfil fora de paciente e administrador, **Then** o banco rejeita.
4. **Given** o esquema criado, **When** é inserida uma linha que referencia registro inexistente, **Then** o banco rejeita por integridade referencial.
5. **Given** o esquema criado, **When** são inseridos dois registros com a mesma identificação única (e-mail de conta, código de regra na mesma versão do protocolo, número de revisão do mesmo pedido, submissão do mesmo pedido), **Then** o banco rejeita o segundo.

---

### User Story 3 - Carga sintética de demonstração (Priority: P1)

A equipe tem uma carga de dados fictícios com contas de paciente A, paciente B e administrador, o protocolo acadêmico v0.1 e suas 6 regras, com senhas apenas em hash.

**Why this priority**: sem dados mínimos, login, isolamento entre pacientes e testes não podem começar.

**Independent Test**: executar a carga sobre o esquema recém-criado e consultar: 3 contas, 1 protocolo, 6 regras; nenhuma senha em texto puro.

**Acceptance Scenarios**:

1. **Given** o esquema criado, **When** a carga é executada, **Then** existem as contas de paciente A, paciente B e administrador, o protocolo `PROTOCOLO-ACADEMICO-FICTICIO v0.1` e as regras RC-R01 a RC-R06 com os atributos de `docs/protocolo-academico.md`.
2. **Given** as contas, **When** a coluna de senha é inspecionada, **Then** contém somente hash do algoritmo adotado pela API, e a documentação registra o algoritmo e como o hash foi gerado, sem divulgar senha real.
3. **Given** a carga, **When** executada duas vezes, **Then** não duplica dados nem corrompe o esquema (o segundo uso é rejeitado ou ignorado de forma previsível e documentada).
4. **Given** a carga, **When** examinada, **Then** contém dados exclusivamente sintéticos, com identificação clara de fictícios, e inclui exemplos suficientes (solicitações em diferentes estados e revisões, análises, pendências e leituras) para que as consultas de uso retornem resultado.

---

### User Story 4 - Consultas de uso com junção e agregação (Priority: P1)

A equipe tem pelo menos 5 consultas que respondem às perguntas do negócio: pedidos por paciente, análises por pedido, pedidos por estado, pendências por regra e pedidos corrigidos entre revisões.

**Why this priority**: prova que o modelo responde às perguntas de histórico e sustenta indicadores e telas futuras.

**Independent Test**: executar cada consulta sobre a carga e comparar o resultado com o esperado documentado.

**Acceptance Scenarios**:

1. **Given** a carga de demonstração, **When** a consulta de pedidos por paciente é executada, **Then** lista cada paciente com seus pedidos e a contagem, sem misturar pacientes.
2. **Given** a carga, **When** a consulta de análises por pedido é executada, **Then** mostra as análises em ordem de revisão com resultado geral e versão do protocolo.
3. **Given** a carga, **When** a consulta de pedidos por estado é executada, **Then** devolve a contagem por estado, incluindo estados presentes na carga.
4. **Given** a carga, **When** a consulta de pendências por regra é executada, **Then** devolve a contagem de pendências por código de regra.
5. **Given** a carga, **When** a consulta de pedidos corrigidos entre revisões é executada, **Then** mostra pedidos cuja pendência existia numa revisão e deixou de existir na seguinte.
6. **Given** cada consulta, **When** lida, **Then** usa junções e agregações, está documentada (finalidade e resultado esperado) e usa apenas dados fictícios.

---

### User Story 5 - Execução segura e verificação honesta (Priority: P2)

A equipe sabe em que ordem executar os scripts, como recomeçar em ambiente de desenvolvimento sem apagar dados de outras pessoas por engano, e quais verificações realmente foram feitas no Oracle da faculdade.

**Why this priority**: o banco é compartilhado com outros trabalhos; é preciso evitar destruição acidental e afirmações falsas de verificação.

**Independent Test**: ler o README dos scripts e identificar a ordem, o que cada script faz, o que é destrutivo (e que exige ação explícita) e o resultado registrado de cada verificação.

**Acceptance Scenarios**:

1. **Given** a pasta de scripts, **When** consultada, **Then** o README informa finalidade, ordem, pré-requisitos e o que cada script altera.
2. **Given** os scripts, **When** examinados, **Then** nenhum apaga ou recria tabelas de forma automática; qualquer script de limpeza é separado, claramente identificado como destrutivo e exige execução explícita.
3. **Given** o registro de verificação, **When** consultado, **Then** informa onde e quando os scripts foram de fato executados; se não houve execução em Oracle, o registro diz "não verificado" e não afirma aprovação.

---

### Edge Cases

- Dois pedidos da mesma paciente com mesma revisão: a revisão é única dentro de cada pedido, não globalmente.
- Paciente A tentando associar documento a pedido da paciente B: o modelo não impede sozinho (a autorização é da API), mas toda linha dependente aponta para seu pedido e a consulta por dono é direta.
- Regra com mesmo código em duas versões do protocolo: permitido, desde que única por versão.
- Análise de revisão antiga depois de uma revisão nova: o histórico permanece e só uma análise por pedido é marcada vigente.
- Pendência de regra "não verificável": tem de ser distinguível de pendência por ausência.
- Valor de sensor fora da faixa ou leitura antiga: o modelo guarda unidade e instante; a classificação é da API.
- Submissão simulada repetida: só pode existir uma por pedido.
- Execução dos scripts duas vezes ou fora de ordem: falha de forma clara, sem corromper dados existentes.
- Nomes que ultrapassam o limite de tamanho de identificadores do banco: evitados ou tratados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O modelo MUST cobrir Usuário, Protocolo, Regra, Solicitação, Documento, Análise, Pendência e Leitura de sensor, com chave primária, chaves estrangeiras e cardinalidades documentadas e um diagrama entidade-relacionamento.
- **FR-002**: O modelo MUST representar a paciente pela conta de usuário (perfil paciente), sem tabela duplicada, e MUST aceitar perfis paciente e administrador.
- **FR-003**: O modelo MUST guardar o resultado de cada regra em cada análise (atende, pendente, não verificável, não aplicável), o resultado geral, a versão do protocolo usada, a revisão da solicitação analisada e se a análise é a vigente.
- **FR-004**: O modelo MUST preservar o histórico por revisão: documentos enviados em cada revisão, análises de cada revisão e pendências de cada análise, de forma que seja possível comparar duas revisões do mesmo pedido.
- **FR-005**: O modelo MUST guardar a sessão de captura vinculada a um pedido e as leituras de sensor (tipo, valor, unidade, instante da medição, sessão), e a submissão simulada única por pedido.
- **FR-006**: O modelo MUST impedir valores inválidos por restrições: estados da solicitação (7), resultados de regra (4), resultado geral (3), perfis (2), responsável pela correção (paciente, unidade emissora), natureza da regra (obrigatória, condicional) e tipo de sensor (luminosidade, distância).
- **FR-007**: O modelo MUST impor unicidade para: identificação (e-mail) da conta, código da regra por versão do protocolo, número da revisão por pedido, resultado de uma regra por análise, vigência única por pedido e submissão simulada por pedido.
- **FR-008**: Os scripts MUST ser escritos para Oracle, ficar em `database/scripts/`, ter nome numerado que indique a ordem de execução e explicar finalidade, ordem e efeitos no próprio cabeçalho e no README.
- **FR-008a**: Todas as tabelas MUST ser criadas com o prefixo `RC_` seguido do nome da tabela (ex.: `RC_USUARIO`, `RC_SOLICITACAO`), por ser um banco compartilhado da faculdade; o prefixo vale também para restrições, índices e sequências quando houver nome explícito (ex.: `RC_USUARIO_PK`), respeitando o limite de tamanho de identificadores do Oracle.
- **FR-009**: O script de criação MUST executar em banco vazio sem erro e MUST NOT apagar nem recriar objetos automaticamente.
- **FR-010**: A carga MUST criar contas de paciente A, paciente B e administrador, o protocolo `PROTOCOLO-ACADEMICO-FICTICIO v0.1` e as regras RC-R01 a RC-R06 com os atributos de `docs/protocolo-academico.md`, e dados de exemplo (pedidos em diferentes estados e revisões, documentos, análises, resultados por regra, pendências e leituras) suficientes para as consultas.
- **FR-011**: Senhas MUST ser armazenadas somente como hash do algoritmo adotado pela API (BCrypt ou Argon2 de biblioteca consolidada), nunca em texto puro; a documentação MUST registrar o algoritmo, o custo e como o hash foi gerado, sem divulgar senhas reais.
- **FR-012**: MUST haver no mínimo 5 consultas com junção e agregação: pedidos por paciente, análises por pedido, pedidos por estado, pendências por regra e pedidos corrigidos entre revisões, cada uma com finalidade e resultado esperado documentados.
- **FR-013**: Um script de limpeza, se existir, MUST ser separado, identificado como destrutivo, exigir execução explícita e nunca ser chamado pelos demais.
- **FR-014**: Todos os dados MUST ser sintéticos e identificados como fictícios; nenhum dado pessoal real, credencial de acesso ao banco ou segredo MUST estar no repositório.
- **FR-015**: A documentação (`database/README.md`, `docs/decisoes.md`, `docs/arquitetura.md`) MUST ser atualizada, e o registro de verificação MUST distinguir o que foi executado em Oracle do que não foi; nada MUST ser marcado como aprovado sem execução.
- **FR-016**: Esta história MUST NOT incluir código de acesso a dados, ORM nem conexão da API ao banco.

### Key Entities

- **Usuário**: conta com e-mail único, hash da senha, nome fictício e perfil (paciente ou administrador).
- **Protocolo**: identificador e versão do protocolo acadêmico, com aviso de fictício e indicação de ativo.
- **Regra**: pertence a um protocolo/versão; código, descrição, evidência exigida, natureza, condição de aplicação, mensagem e responsável pela correção.
- **Solicitação**: pedido de uma paciente (usuário), com protocolo, estado atual e número da revisão atual.
- **Documento**: arquivo enviado numa revisão de um pedido (nome, tipo, tamanho, páginas, revisão de envio, situação).
- **Análise**: resultado de uma revisão de um pedido, com versão do protocolo, resultado geral, vigência e indicação de simulado.
- **Resultado de regra**: resultado de uma regra numa análise.
- **Pendência**: ligada a uma análise e a uma regra, com mensagem, responsável pela correção e tipo (pendente ou não verificável).
- **Sessão de captura e Leitura de sensor**: sessão vinculada a um pedido de teste e leituras com tipo, valor, unidade e instante.
- **Submissão simulada**: registro único por pedido, com referência interna e data.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das entidades obrigatórias o modelo documenta chave primária, chaves estrangeiras e cardinalidade.
- **SC-002**: O script de criação executa em banco vazio com 0 erros; no Oracle real da faculdade, a execução é registrada com data e resultado, e qualquer parte não executada é marcada explicitamente como "não verificada".
- **SC-003**: 100% dos valores inválidos testados (estado, resultado, perfil, referência inexistente, duplicidade) são rejeitados pelo banco.
- **SC-004**: A carga cria exatamente 3 contas, 1 protocolo e 6 regras de referência, e 0 senhas em texto puro.
- **SC-005**: Pelo menos 5 consultas retornam resultado não vazio sobre a carga e coincidem com os resultados esperados documentados.
- **SC-006**: Executar os scripts de criação e carga uma segunda vez não corrompe os dados existentes (falha de forma previsível ou é ignorada de modo documentado).
- **SC-007**: Uma pessoa da equipe identifica, só com o README dos scripts, a ordem de execução e o que é destrutivo em menos de 5 minutos.
- **SC-008**: As histórias RC-04 (login) e RC-07 (rascunho) podem começar sem pedir nenhuma definição adicional de tabelas ou colunas.

## Assumptions

- A RC-01 e a RC-02 estão concluídas e mergeadas; o protocolo, as regras, os estados e os contratos de dados vêm de `docs/protocolo-academico.md`, `docs/api/estados-e-revisoes.md` e do modelo conceitual de `specs/002-projeto-executavel-contratos/data-model.md`.
- Além das oito entidades pedidas, o modelo inclui as necessárias para cumprir os contratos: resultado de regra por análise, sessão de captura (as leituras vinculam-se a uma sessão de teste, conforme o contrato de telemetria) e submissão simulada única.
- O banco é o Oracle 19c da faculdade, com acesso verificado em 12/11/2026 (usuário individual com permissão de criar tabelas e sequências). Os scripts são executados de verdade nele e o resultado é registrado; o que não for executado é marcado como "não verificado".
- Convenção de nomes (decisão do usuário em 12/11/2026): tabelas sempre `RC_NomeDaTabela`, pois o banco da faculdade é compartilhado e o prefixo evita colisão com objetos de outros projetos; vale para todas as histórias futuras que criem tabelas.
- Identificadores gerados e tipos de dados específicos serão escolhidos no plano, respeitando a sintaxe e os limites do Oracle.
- O algoritmo de hash da API (BCrypt ou Argon2) será fixado no plano; a carga usa hash de senhas de demonstração geradas no desenvolvimento e documentadas, sem senhas reais; a compatibilidade com a API é conferida na RC-04.
- A autorização por recurso é responsabilidade da API (RC-05); o banco garante integridade, não permissões.
- O modelo suporta a vigência de uma análise por pedido e a invalidação de análises antigas ao mudar a revisão; as regras de transição de estados são aplicadas pela API, e o banco só restringe os valores permitidos.
- Esta história não implementa JDBC nem conexão da API; isso fica para a história posterior que usar o banco.
- Mudanças futuras do esquema serão feitas por novos scripts numerados e aditivos, nunca por recriação automática.
