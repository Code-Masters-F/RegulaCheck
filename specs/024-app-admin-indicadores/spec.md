# Feature Specification: Administrador — protocolo e indicadores (RC-20)

**Feature Branch**: `024-app-admin-indicadores`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "RC-20 (issue #43, Épico 6 #40): o administrador entra no app Android e vê, numa tela simples (E4.2 Protocolo e indicadores), o protocolo ativo com as regras e indicadores agregados (total de pedidos, análises por resultado e regras com mais pendências). Cria a rota GET /admin/indicadores (só ADMINISTRADOR; paciente 403; sem sessão 401; só agregados). No app, o login passa a encaminhar por perfil: PACIENTE → Meus pedidos; ADMINISTRADOR → E4.2. Fora do escopo: editar protocolo/regras, ver pedidos/documentos individuais, outras telas de administrador."

**Rastreabilidade**: RC-20 = issue [#43](https://github.com/Code-Masters-F/RegulaCheck/issues/43) (Épico 6, [#40](https://github.com/Code-Masters-F/RegulaCheck/issues/40)). Contratos de origem: `CONTRATO_API.md` (`GET /admin/indicadores`), `CONTRATO_TESTES.md` (T03), `docs/api/autorizacao.md` (RC-05). Base do app: `specs/017-app-android-login-pedidos/` (RC-18) e `specs/022-app-android-documentos-resultado/` (RC-19), referenciadas e não copiadas. Referência visual: `mobile/design/INDEX.md` §18 (quadros `T8XMZ`, `Ifg71`, `mcN6S`) e `mobile/design/textos.md` "E4.2". Numeração: a 023 foi ocupada por outra feature em paralelo (`023-app-android-condicoes-captura`); por decisão do usuário em 18/11/2026, esta feature é a 024.

## Clarifications

### Session 2026-11-18

Decididas pelo agente com a recomendação, por pedido do usuário ("decida e registre o que contar"), sem rodada de perguntas.

- Q: O que contar em "análises por resultado", já que a análise que falha não é gravada (só o pedido fica em `FALHA_ANALISE`)? → A: Só as análises **vigentes** (a que vale para a versão atual de cada pedido; mudar documento ou conferência tira a vigência), agrupadas pelo resultado geral (sem pendências identificadas, com pendências, confira a leitura); "falha" é contada **à parte**, como número de **pedidos** cujo estado atual é `FALHA_ANALISE`, e a tela diz isso em texto. Os grupos não são excludentes por pedido: um pedido com análise vigente antiga e nova tentativa que falhou aparece nos dois.
- Q: Sobre qual base contar as "regras com mais pendências"? → A: As pendências (pendente e não verificável) das mesmas análises vigentes da pergunta anterior, por regra; a resposta traz o total e as duas parcelas. O histórico de análises substituídas não entra, para que o painel mostre o retrato atual e não acumule reenvios do mesmo pedido.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar agregados pela API como administrador (Priority: P1)

O administrador, autenticado, pede os indicadores e recebe o protocolo ativo (identificação, versão, quantidade e lista de regras com código e nome), o total de pedidos, as análises por resultado e as regras com mais pendências, sem nenhum dado que identifique paciente, arquivo ou conteúdo clínico.

**Why this priority**: a issue #43 registra que a rota ainda não existe e que a tela só entra depois dela. Sem a rota não há demonstração da operação do protótipo.

**Independent Test**: com a API em memória e dados de teste conhecidos, chamar a rota com a conta de administrador e conferir cada número contra o esperado calculado à mão; chamar com paciente e sem sessão.

**Acceptance Scenarios**:

1. **Given** uma sessão de ADMINISTRADOR e dados de teste com pedidos e análises conhecidos, **When** consulta os indicadores, **Then** recebe 200 com os agregados iguais aos esperados e o aviso de dados sintéticos.
2. **Given** uma sessão de PACIENTE, **When** consulta os indicadores, **Then** recebe 403 `ACESSO_NEGADO`, sem nenhum agregado, e nenhuma consulta aos dados é feita.
3. **Given** nenhuma sessão (ou sessão expirada), **When** consulta os indicadores, **Then** recebe 401 no formato padrão.
4. **Given** qualquer conjunto de dados, **When** o administrador recebe os indicadores, **Then** a resposta não contém nome, e-mail, identificador de paciente ou de pedido, nome ou caminho de arquivo, trecho lido, resumo nem mensagem de pendência.
5. **Given** um banco sem pedidos nem análises, **When** o administrador consulta, **Then** recebe 200 com zeros e lista vazia de regras com pendências (não um erro).

---

### User Story 2 - Entrar no app como administrador e ver a tela E4.2 (Priority: P1)

O administrador entra no app com a conta fictícia e é levado direto à tela "Protocolo e indicadores", que mostra o protocolo ativo com as regras, o total de pedidos, as análises por resultado e as regras com mais pendências, com a nota de dados agregados de demonstração, o rodapé acadêmico e "Sair".

**Why this priority**: é o critério de aceite visível da RC-20; hoje o app recusa a conta de administrador.

**Independent Test**: com a API simulada em teste, entrar como administrador e confirmar que a tela inicial é E4.2 com os números da resposta; entrar como paciente e confirmar que continua em "Meus pedidos".

**Acceptance Scenarios**:

1. **Given** a tela de login, **When** a conta de ADMINISTRADOR entra, **Then** o app abre E4.2 e não mostra "Meus pedidos" nem qualquer tela de paciente.
2. **Given** a tela de login, **When** a conta de PACIENTE entra, **Then** o app continua abrindo "Meus pedidos", como na RC-18.
3. **Given** E4.2 carregando, **When** a resposta ainda não chegou, **Then** aparece o esqueleto com "Carregando indicadores…".
4. **Given** falha de rede ou do servidor, **When** a consulta falha, **Then** aparece "Não foi possível carregar os indicadores." com "Tentar de novo", e tentar de novo faz uma nova consulta (sem retentativa automática).
5. **Given** a consulta responde 401, **When** a tela recebe a resposta, **Then** a sessão local é apagada e o app volta ao login com o aviso de sessão expirada.
6. **Given** a consulta responde 403, **When** a tela recebe a resposta, **Then** aparece a mensagem de "sem permissão" (E1.5) sem encerrar a sessão.
7. **Given** E4.2, **When** o administrador toca "Sair", **Then** a sessão local é apagada antes da resposta do servidor e o app volta ao login.
8. **Given** o app reaberto com uma sessão de administrador guardada, **When** inicia, **Then** abre E4.2 (não "Meus pedidos").

---

### User Story 3 - Isolamento entre perfis no app (Priority: P2)

Uma conta de paciente nunca chega à tela de indicadores e uma conta de administrador nunca chega às telas de pedidos; uma conta com perfil desconhecido continua recusada.

**Why this priority**: reforça no cliente a matriz da RC-05; a proteção real é a da API (US1).

**Independent Test**: em teste de unidade da escolha de tela por perfil e do serviço de sessão.

**Acceptance Scenarios**:

1. **Given** uma conta com perfil que não é PACIENTE nem ADMINISTRADOR, **When** entra, **Then** o app recusa, encerra a sessão aberta no servidor e não guarda nada no aparelho.
2. **Given** uma sessão de PACIENTE, **When** o app escolhe a tela inicial, **Then** nunca escolhe E4.2.

### Edge Cases

- Nenhum protocolo ativo: os indicadores de pedidos e análises continuam sendo devolvidos e o protocolo ativo vem ausente; a tela diz "Nenhum protocolo ativo." no lugar do cartão.
- Mais de um protocolo ativo (não ocorre na carga sintética, que tem um): a resposta lista todos; a tela mostra um cartão por protocolo.
- Regras sem nenhuma pendência não entram em "Regras com mais pendências"; com nenhuma, a tela diz "Nenhuma pendência registrada."
- Pedido em análise (`PROCESSANDO`) ou em rascunho conta no total de pedidos, mas não em "análises por resultado" (a menos que já tenha análise vigente de uma revisão anterior).
- Pedido em falha na análise com análise vigente anterior: conta na falha e no grupo da análise vigente (grupos não excludentes, ver Clarifications).
- Análises substituídas (não vigentes) não entram em nenhum número.
- Valor de resultado desconhecido pelo app recebe rótulo neutro e não derruba a tela.
- Número grande não quebra o leiaute; textos da API são exibidos como texto puro.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A API MUST oferecer a consulta de indicadores (`GET /admin/indicadores`, já prevista em `CONTRATO_API.md`) somente ao perfil ADMINISTRADOR, pelo mesmo registro de rotas privadas da RC-05: sem sessão válida → 401; PACIENTE → 403 `ACESSO_NEGADO`, verificado antes de qualquer consulta.
- **FR-002**: A resposta MUST trazer o protocolo ativo: identificador público, nome, versão, quantidade de regras e a lista das regras (código e nome, na ordem do protocolo).
- **FR-003**: A resposta MUST trazer o total de pedidos existentes (todos os estados).
- **FR-004**: A resposta MUST trazer as análises vigentes (a que vale para a versão atual de cada pedido) por resultado geral — sem pendências identificadas, com pendências, confira a leitura, sempre os três grupos, com zero quando não houver — e, à parte, o número de pedidos cujo estado atual é falha na análise. A tela mostra os quatro números sob "Análises por resultado" e diz em texto que "falha" conta pedidos.
- **FR-005**: A resposta MUST trazer as regras com mais pendências nas análises vigentes (código, nome, total e as parcelas pendente e não verificável), em ordem decrescente de total, com desempate pelo código, limitadas às 5 primeiras e só com total maior que zero.
- **FR-006**: A resposta MUST conter apenas agregados e dados do protocolo: nenhum nome, e-mail, identificador de paciente ou de pedido, nome ou caminho de arquivo, trecho lido, resumo, mensagem de pendência ou outro conteúdo clínico.
- **FR-007**: A resposta MUST identificar os dados como sintéticos/de demonstração e a hora da consulta (UTC).
- **FR-008**: Falha de acesso aos dados MUST responder 503 `DEPENDENCIA_INDISPONIVEL`, no formato padrão, sem detalhe interno.
- **FR-009**: O app MUST encaminhar pelo perfil da conta após o login e ao reabrir com sessão guardada: PACIENTE → "Meus pedidos"; ADMINISTRADOR → E4.2. Perfil desconhecido continua recusado sem guardar sessão.
- **FR-010**: A tela E4.2 MUST mostrar, com os textos de `mobile/design/textos.md`: "Protocolo e indicadores", o cartão do protocolo ativo com a lista de regras, "Total de pedidos", "Análises por resultado", "Regras com mais pendências", a nota "Dados agregados de demonstração. Documentos e pedidos individuais não são exibidos.", o rodapé acadêmico e "Sair".
- **FR-011**: A tela E4.2 MUST tratar carregando (esqueleto + "Carregando indicadores…"), erro ("Não foi possível carregar os indicadores." + "Tentar de novo"), 401 (volta ao login) e 403 (sem permissão), sem retentativa automática.
- **FR-012**: Nenhum texto da API ou do app MUST afirmar redução real de filas, aprovação, autorização ou diagnóstico; estado nunca é comunicado só por cor.
- **FR-013**: A documentação da API (`docs/api/openapi.yaml`, `docs/api/autorizacao.md`) MUST descrever a rota, os campos e as respostas 200/401/403/503.

### Key Entities

- **Indicadores do administrador**: retrato agregado no momento da consulta; contém os protocolos ativos, o total de pedidos, as contagens por resultado, as regras com mais pendências, a marca de dados sintéticos e a hora da consulta. Não é persistido.
- **Protocolo ativo (resumo)**: identificador público, nome, versão, quantidade de regras e regras (código, nome).
- **Regra com pendências**: código, nome, total e parcelas (pendente, não verificável) contadas nas análises vigentes (FR-005).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das chamadas de teste, paciente recebe 403 e chamada sem sessão recebe 401, sem nenhum número na resposta.
- **SC-002**: Com dados de teste conhecidos, 100% dos números devolvidos ao administrador coincidem com o esperado calculado de forma independente.
- **SC-003**: Nenhuma resposta de indicadores contém dado identificável (verificação automática de ausência de nome, e-mail, identificadores, arquivos e mensagens).
- **SC-004**: Ao entrar como administrador, a tela E4.2 aparece em até 3 s numa rede local; ao entrar como paciente, "Meus pedidos" aparece como antes (sem regressão nos testes do app).
- **SC-005**: No emulador com o backend real, os números exibidos em E4.2 coincidem com a consulta direta à API (verificação registrada em `docs/evidencias/`).

## Assumptions

- Escopo P1 da issue #43: uma única tela de administrador (E4.2). Edição de protocolo ou regras, pedidos ou documentos individuais, reenvio/submissão e outras telas de administrador ficam fora.
- "Nome" da regra é a descrição curta já gravada (ex.: "Encaminhamento presente"); a evidência exigida e a mensagem da regra não entram (a mensagem é texto de orientação à paciente).
- A carga sintética tem um protocolo ativo (`PROTOCOLO-ACADEMICO-FICTICIO` v0.1, 6 regras); a resposta ainda assim aceita mais de um.
- Todos os dados do banco de demonstração são sintéticos (CLAUDE.md §1); por isso não há regra de supressão de contagens pequenas. Isso fica registrado como limitação para um uso com dados reais.
- O app reaproveita cliente HTTP, sessão cifrada, navegação e componentes das RC-18/RC-19; não há nova dependência.
- A API é a proteção real; a escolha de tela por perfil no app é só experiência de uso.
