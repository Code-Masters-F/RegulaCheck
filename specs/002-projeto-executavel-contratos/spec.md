# Feature Specification: Projeto executável e contratos (RC-02)

**Feature Branch**: `002-projeto-executavel-contratos`

**Created**: 2026-11-12

**Status**: Draft

**Backlog**: RC-02 — Code-Masters-F/ClickBus#7 · Épico 1 (Escopo e fundação) · P0 · até 12/11/2026 · depende da RC-01 (concluída)

**Input**: User description: "RC-02 — Preparar projeto executável e contratos. Tarefa técnica: como equipe, quero uma base compartilhada para desenvolver app e API sem divergência de contratos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - API base que inicia e informa que está viva (Priority: P1)

Uma pessoa da equipe, em máquina limpa com o JDK instalado, segue o README, inicia a API e consulta uma rota técnica de saúde que responde "UP". A API inicia sem precisar de banco, IA, sensores nem credenciais.

**Why this priority**: sem uma base que inicie, nenhuma outra história pode ser desenvolvida ou testada.

**Independent Test**: iniciar a API pelo comando documentado e consultar `GET /health`; obter HTTP 200 e status UP em menos de 1 minuto, sem nenhuma variável de banco definida.

**Acceptance Scenarios**:

1. **Given** uma máquina com o JDK e nenhuma variável de ambiente de banco, **When** a API é iniciada pelo comando documentado, **Then** ela sobe sem erro e `GET /health` retorna 200 com status UP.
2. **Given** a API em execução, **When** `GET /health` é consultado, **Then** a resposta expõe apenas o estado técnico (sem detalhes internos, caminhos, versões sensíveis ou credenciais).
3. **Given** a API em execução, **When** é chamada uma rota inexistente, **Then** a resposta segue o formato padrão de erro, sem stack trace.
4. **Given** o código da base, **When** examinado, **Then** existem Controller, Service e Repository com responsabilidades separadas, ilustrados por um exemplo mínimo que não simula funcionalidade de negócio.

---

### User Story 2 - Repositório organizado com configuração de exemplo (Priority: P1)

A equipe encontra, no repositório, pastas definidas para API, mobile, banco, simulador e documentação, com arquivos de configuração de exemplo que mostram quais variáveis existem, sem nenhum segredo.

**Why this priority**: evita que cada pessoa invente sua estrutura e que segredos entrem no Git.

**Independent Test**: clonar o repositório, listar as pastas e abrir o arquivo de exemplo; confirmar que as 5 áreas existem, que cada uma explica sua finalidade e que não há segredo.

**Acceptance Scenarios**:

1. **Given** o repositório clonado, **When** a raiz é listada, **Then** estão presentes as áreas de API, mobile, banco, simulador e documentação, cada uma com um README de propósito.
2. **Given** o arquivo de configuração de exemplo, **When** aberto, **Then** lista as variáveis (nomes apenas) sem valores reais, e nenhuma delas é obrigatória para iniciar a base.
3. **Given** uma varredura de segredos nos arquivos versionados, **When** executada, **Then** não encontra credenciais, tokens ou dados reais.

---

### User Story 3 - Contratos compartilhados entre app e API (Priority: P1)

A equipe do app e a da API têm um contrato escrito para solicitação, upload, análise, pendência e erros, de modo que as duas frentes trabalhem em paralelo sem divergir.

**Why this priority**: é o objetivo declarado da história ("sem divergência de contratos").

**Independent Test**: duas pessoas, uma lendo como app e outra como API, listam os campos de cada mensagem e chegam à mesma lista; cada regra de erro aparece com código e mensagem.

**Acceptance Scenarios**:

1. **Given** o documento de contratos, **When** consultado, **Then** descreve solicitação, upload, análise, pendência e erro, com campos, tipos, obrigatoriedade, exemplos fictícios e códigos HTTP aplicáveis.
2. **Given** o contrato de erro, **When** consultado, **Then** define código, mensagem compreensível e detalhes por campo, e proíbe vazar dados internos (SQL, stack trace, chaves).
3. **Given** o contrato de análise e pendência, **When** consultado, **Then** usa os resultados por regra e o resultado geral de [protocolo-academico.md](../../docs/protocolo-academico.md), e marca o resumo de IA e a extração como simulados.
4. **Given** o contrato de upload, **When** consultado, **Then** registra os limites (até 3 arquivos de 5 MB, PDF até 5 páginas, JPEG ou PNG) e os erros 413 e 415.
5. **Given** os contratos, **When** comparados com `CONTRATO_API.md`, **Then** não há contradição de rotas ou comportamentos.

---

### User Story 4 - Estados e revisões da solicitação (Priority: P1)

A equipe tem definidos os estados da solicitação (rascunho, processando, requer conferência, com pendências, sem pendências identificadas, falha de análise e submetida em simulação), as transições permitidas e a regra de revisão.

**Why this priority**: estados e revisões orientam banco, regras, app e testes; erros aqui se propagam.

**Independent Test**: uma tabela de transições responde, para cada par (estado atual, evento), se a transição é permitida e qual o novo estado; ninguém precisa perguntar.

**Acceptance Scenarios**:

1. **Given** o documento de estados, **When** consultado, **Then** lista os 7 estados com significado e as transições permitidas e proibidas.
2. **Given** uma solicitação em qualquer estado, **When** o arquivo ou dado relevante é alterado, **Then** a revisão aumenta, o resultado anterior deixa de ser o vigente e o histórico é preservado.
3. **Given** o estado "sem pendências identificadas", **When** descrito, **Then** só ele pode levar a "submetida em simulação", e a submissão é identificada como simulada e não envia nada ao SUS.
4. **Given** um processamento iniciado numa revisão antiga, **When** conclui depois de uma nova revisão, **Then** seu resultado não é publicado como vigente.

---

### User Story 5 - Endereçamento da API a partir do emulador ou aparelho (Priority: P2)

Quem desenvolve o app consegue alcançar a API rodando no computador, tanto pelo emulador Android quanto por um aparelho físico, com instruções claras.

**Why this priority**: "localhost" não aponta para o computador dentro do emulador; sem isso o app não conversa com a API.

**Independent Test**: seguindo o README, a rota de saúde é acessada com sucesso a partir do navegador do emulador (e, se disponível, do aparelho físico na mesma rede).

**Acceptance Scenarios**:

1. **Given** a API em execução no computador, **When** o emulador usa o endereço documentado, **Then** a rota de saúde responde 200.
2. **Given** um aparelho físico na mesma rede, **When** usa o endereço documentado do computador, **Then** a rota de saúde responde 200, ou a documentação explica o bloqueio (firewall/rede) e como contorná-lo.
3. **Given** a documentação, **When** consultada, **Then** registra o endereço real usado e avisa que `localhost` não vale dentro do emulador ou aparelho.

---

### Edge Cases

- Porta padrão já ocupada: a API informa o problema de forma clara e a porta é configurável.
- Banco indisponível ou não configurado: a base ainda inicia e o health continua informando apenas o estado técnico; a ausência do banco não é escondida nem tratada como falha de inicialização.
- Variável de ambiente ausente: não impede o início (nenhuma é obrigatória nesta história).
- JDK diferente do esperado: a documentação diz a versão exigida e o que acontece com versões distintas.
- Estados com dois eventos simultâneos (reenvio durante processamento): o contrato define qual prevalece (revisão mais nova).
- Cliente envia corpo inválido ou rota inexistente: resposta padronizada, sem detalhe interno.
- Arquivo de exemplo copiado sem editar: nada quebra e nenhum segredo é necessário.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A base MUST ser uma única aplicação de backend (monólito modular com um módulo de build) que inicia pelo comando documentado, sem exigir banco, IA, sensores nem variáveis de ambiente.
- **FR-002**: A base MUST expor `GET /health` com HTTP 200 e status UP, mostrando apenas o estado técnico.
- **FR-003**: O código da base MUST demonstrar a separação Controller → Service → Repository com um exemplo mínimo, sem entidades de negócio, CRUD, autenticação completa, regras documentais, IA ou sensores funcionais e sem código que finja funcionalidade.
- **FR-004**: A stack MUST obedecer à constituição v2.0.0: Java sem Spring e sem JPA/Hibernate em produção (Spring Boot só em testes), servidor embutido Jakarta Servlet (Jetty ou Tomcat), acesso a dados por JDBC e SQL parametrizado, banco Oracle da faculdade; a migração da fundação antiga (Spring Boot/JPA/PostgreSQL) MUST ser concluída ou explicitamente registrada como pendência remanescente.
- **FR-005**: O repositório MUST ter pastas e README de propósito para API, mobile, banco, simulador e documentação.
- **FR-006**: MUST existir arquivo de configuração de exemplo com os nomes das variáveis (sem valores reais), cuja ausência de valores não impeça o início da base.
- **FR-007**: MUST ser definido o contrato de **solicitação** (criação, consulta, estados, revisão atual), de **upload** (limites e erros), de **análise** (resultado por regra, geral, versão do protocolo, indicação de simulado), de **pendência** (regra, mensagem, responsável pela correção) e de **erro** (código, mensagem, detalhes por campo, identificador de rastreamento opcional).
- **FR-008**: O contrato de erro MUST cobrir 400, 401, 403/404, 409, 413, 415 e falha de dependência, sem expor stack trace, SQL, chaves ou conteúdo de documentos.
- **FR-009**: Os contratos MUST ser coerentes com `CONTRATO_API.md`, `docs/protocolo-academico.md` e `docs/arquitetura.md`; qualquer divergência MUST ser corrigida na origem.
- **FR-010**: MUST ser definido o conjunto de 7 estados da solicitação, as transições permitidas e a regra de revisão (alteração relevante incrementa a revisão, invalida o resultado anterior e preserva histórico; resultado de processamento só é vigente se a revisão ainda for a mesma).
- **FR-011**: Somente "sem pendências identificadas" MUST poder levar a "submetida em simulação"; a submissão MUST ser identificada como simulada.
- **FR-012**: O endereçamento da API a partir do emulador Android e de aparelho físico MUST estar documentado e verificado ao menos no ambiente de desenvolvimento usado.
- **FR-013**: MUST haver ao menos um teste automatizado da base (health) que não dependa de banco nem rede externa, executado pelo comando de build documentado, com resultado real registrado.
- **FR-014**: README e `docs/ambiente.md` MUST ser atualizados com pré-requisitos, versão do JDK, comando de execução e de testes realmente verificados.
- **FR-015**: Nenhum segredo, documento real ou dado pessoal MUST estar nos arquivos versionados; o sistema não diagnostica, não define urgência nem autoriza atendimento.

### Key Entities

- **Solicitação**: pedido da paciente, com estado atual e número de revisão.
- **Documento (upload)**: arquivo enviado numa revisão, com tipo, tamanho e páginas validados.
- **Análise**: resultado de uma revisão, com versão do protocolo, resultado por regra, resultado geral e indicação de simulado.
- **Pendência**: item da análise com regra, mensagem e responsável pela correção.
- **Erro**: resposta padronizada com código, mensagem e detalhes de campo.
- **Estado e transição**: os 7 estados e as passagens permitidas entre eles.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma pessoa da equipe, só com o README, inicia a API e obtém "UP" na rota de saúde em até 10 minutos numa máquina com o JDK instalado e sem configurar banco.
- **SC-002**: Em 100% das áreas exigidas (API, mobile, banco, simulador, documentação) existe pasta com README de propósito.
- **SC-003**: Zero segredos ou dados reais encontrados nos arquivos versionados por varredura.
- **SC-004**: Dois leitores independentes (app e API) extraem a mesma lista de campos de cada contrato em 100% dos contratos (solicitação, upload, análise, pendência, erro).
- **SC-005**: A tabela de transições responde 100% dos pares (estado, evento) revisados sem dúvida.
- **SC-006**: O comando de build executa com sucesso e o teste de saúde passa, com o resultado registrado.
- **SC-007**: A rota de saúde é alcançada a partir do emulador Android seguindo apenas a documentação.
- **SC-008**: As histórias RC-03 (banco) e RC-04 (login) podem começar sem pedir nenhuma definição adicional de estados, erros ou estrutura.

## Assumptions

- A RC-01 está concluída e mergeada; as regras e o resultado geral vêm de `docs/protocolo-academico.md`.
- O JDK é o 21 (proposta registrada em `docs/decisoes.md`); a verificação real é feita no JDK instalado na máquina de desenvolvimento.
- Jetty ou Tomcat embutido: a escolha entre os dois é decisão do plano.
- A migração da fundação antiga (Spring Boot 4.1.2/JPA/PostgreSQL) faz parte desta história; se algum resíduo ficar, é registrado como pendência explícita.
- Os dados de conexão do Oracle ainda não foram fornecidos; por isso o banco não é obrigatório para iniciar e a conexão real fica para a RC-03.
- O mobile (Android Kotlin) ainda não existe; esta história só cria a pasta e documenta o endereçamento.
- O formato de documentação dos contratos é Markdown e OpenAPI/Postman escritos à mão; sem Swagger gerado por Spring.
- Autenticação (RC-04), perfis (RC-05) e proteção de entradas (RC-06) ficam fora desta história, que apenas reserva os códigos de erro 401 e 403.
- A numeração dos estados e os nomes seguem `docs/arquitetura.md` e `docs/protocolo-academico.md`; mudanças de nome registram-se nos dois lugares.
