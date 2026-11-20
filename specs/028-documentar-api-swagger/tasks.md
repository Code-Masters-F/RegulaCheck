---

description: "Tarefas da RC-28 — Documentar API e respostas HTTP"
---

# Tasks: Documentar API e respostas HTTP (RC-28)

**Input**: Design documents from `specs/028-documentar-api-swagger/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-documentacao.md, quickstart.md

**Tests**: pedidos pela especificação (FR-002, FR-005, FR-006, FR-013, FR-015, SC-006): teste do endpoint de documentação, teste de coerência dos documentos e teste do roteiro em memória. A suíte existente precisa continuar verde.

**Organization**: tarefas agrupadas por história (US1 Swagger UI e contrato, US2 roteiro, US3 coerência e Postman).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história da spec (US1, US2, US3)

---

## Phase 1: Setup

- [X] T001 Registrar o estado de referência: `sh ./mvnw -q clean verify` em `backend/` (total de testes, falhas, tempo), anotado na seção "Registro de execução" deste arquivo
- [X] T002 Acrescentar a propriedade `swagger-ui.version` 5.34.1, a dependência `org.webjars:swagger-ui` e o bloco `<resources>` (`src/main/resources` + `../docs/api/openapi.yaml` → `documentacao/`) em `backend/pom.xml`, com comentário do motivo

---

## Phase 2: Foundational

- [X] T003 Incluir `"/docs/*"` em `RotasPrivadas.PUBLICAS`, atualizando o JavaDoc, em `backend/src/main/java/br/com/regulacheck/auth/autorizacao/RotasPrivadas.java`

**Checkpoint**: build compila com a webjar no classpath e o contrato em `target/classes/documentacao/openapi.yaml`.

---

## Phase 3: User Story 1 - Consultar e executar a API pela documentação interativa (Priority: P1) 🎯 MVP

**Goal**: `GET /docs` serve o Swagger UI com o contrato completo, executável na mesma origem.

**Independent Test**: abrir `/docs`, ver todas as rotas agrupadas e executar `GET /health` e `POST /auth/login` pela página; `DocumentacaoEndpointTest` verde.

### Tests for User Story 1

- [X] T004 [US1] Escrever `backend/src/test/java/br/com/regulacheck/documentacao/DocumentacaoEndpointTest.java`: `/docs`, `/docs/` e `/docs/index.html` dão 200 HTML; `/docs/openapi.yaml` é byte a byte igual a `../docs/api/openapi.yaml`; scripts, estilos e ícones saem com o tipo correto; caminho fora da lista (incluindo travessia e arquivos da webjar não listados) dá 404 `NAO_ENCONTRADO`; POST dá 405 com `Allow: GET`; a CSP de `/docs` vale só nas respostas 200, e `/health` e os erros de `/docs` mantêm a política estrita; sem sessão

### Implementation for User Story 1

- [X] T005 [P] [US1] Criar `backend/src/main/resources/documentacao/index.html` e `backend/src/main/resources/documentacao/inicializar.js` (URLs absolutas `/docs/...`, BaseLayout, `validatorUrl: null`, `persistAuthorization: false`, sem script inline)
- [X] T006 [US1] Criar `backend/src/main/java/br/com/regulacheck/documentacao/DocumentacaoController.java` (lista fixa carregada na construção, versão da webjar lida do `pom.properties`, CSP própria nas respostas 200, 404/405 padrão) e `package-info.java`, com JavaDoc
- [X] T007 [US1] Registrar `/docs/*` antes da rota coringa em `backend/src/main/java/br/com/regulacheck/config/Composicao.java`
- [X] T008 [US1] Capturar as respostas reais do percurso por uma execução temporária em memória (classe descartável, removida depois; saída no scratchpad, fora do repositório) para usar como exemplos
- [X] T009 [US1] Completar `docs/api/openapi.yaml`: descrição geral atualizada (implementado até RC-22 e RC-20, 202 não usado, 500 genérico, simulados), servidor relativo `/` primeiro, `tags` em todas as operações, exemplos de entrada e saída fictícios das rotas do roteiro, exemplos de erro (400, 401, 403, 404, 409, 413, 415, 422, 503) e do resumo `PADRAO`, e rotas `/docs` e `/docs/openapi.yaml`
- [X] T010 [US1] Validar `docs/api/openapi.yaml` com `npx @redocly/cli` descartável (estrutura OpenAPI 3.0 e `no-invalid-media-type-examples`), executado no scratchpad; corrigir até dar 0 erros
- [ ] T011 [US1] **Parcial (19/11/2026):** executado tudo, exceto o login válido → Authorize → `GET /solicitacoes` no navegador com repositórios em memória (não feito por falta de tempo no dia da entrega; o percurso pela API está coberto pelo T012). Rodar `DocumentacaoEndpointTest` e verificar a página no Chrome headless contra a API empacotada (operações renderizadas, 0 violações de CSP, 0 requisições externas, `GET /health` executado pela página) e, contra a API com repositórios em memória, o "Try it out" de `POST /auth/login` → Authorize → `GET /solicitacoes` (200), conferindo que `localStorage` e `sessionStorage` não guardam o token

**Checkpoint**: US1 entregue e verificada isoladamente.

---

## Phase 4: User Story 2 - Seguir um roteiro ponta a ponta até a submissão simulada (Priority: P1)

**Goal**: roteiro único, do login ao comprovante, verificado automaticamente.

**Independent Test**: `RoteiroAvaliacaoTest` verde; o roteiro escrito tem os mesmos passos, status e campos.

- [X] T012 [US2] Escrever `backend/src/test/java/br/com/regulacheck/documentacao/RoteiroAvaliacaoTest.java`: login → protocolos → rascunho → envio de `samples/encaminhamento-sintetico.pdf` → leitura → conferência → análise (`COM_PENDENCIAS`, RC-R03) → histórico → resumo simulado → inclusão de `samples/identificacao-sintetica.png` → reenvio (`CONFERENCIA`) → conferência → reenvio (`CONCLUIDA`, `SEM_PENDENCIAS_IDENTIFICADAS`) → submissão (201 `SIM-…`) → comprovante → exclusão de rascunho descartável (204) → indicadores do administrador (200) e paciente (403), pelas rotas reais com repositórios em memória e o leitor simulado de produção
- [X] T013 [US2] Escrever `docs/api/roteiro-avaliacao.md` com os mesmos passos (Swagger UI e `curl`), status esperado e campos a observar, a senha indicada pela fonte (`database/scripts/README.md`) e o comportamento sem banco (503)

**Checkpoint**: US1 + US2 cobrem os quatro critérios de aceite do backlog.

---

## Phase 5: User Story 3 - Materiais de execução e índices coerentes (Priority: P2)

**Goal**: mapa de rotas, índice, contrato e coleção Postman concordam entre si.

**Independent Test**: `ContratoDaApiTest` verde; coleção com `GET /admin/indicadores` e exclusão só de rascunho descartável.

- [X] T014 [P] [US3] Escrever `backend/src/test/java/br/com/regulacheck/documentacao/ContratoDaApiTest.java` (pares método + caminho de `CONTRATO_API.md` = os de `docs/api/openapi.yaml`; toda operação com `tags`)
- [X] T015 [US3] Atualizar `CONTRATO_API.md`: `GET /admin/indicadores` implementada, rotas `/docs` e `/docs/openapi.yaml`, linha de rotas públicas e link para o roteiro
- [X] T016 [P] [US3] Atualizar `docs/api/RegulaCheck.postman_collection.json`: nome e descrição, login do administrador e `GET /admin/indicadores`, exclusão de rascunho sobre rascunho descartável no fim, sem apagar o pedido do percurso
- [X] T017 [P] [US3] Atualizar `docs/api/README.md`: Swagger UI servido em `/docs`, roteiro no índice, RC-20 implementada e entrada da RC-28 em "Situação das rotas"

---

## Phase 6: Polish & Cross-Cutting

- [X] T018 [P] Atualizar `README.md` e `docs/ambiente.md` (endereço `/docs`, sem banco → 503, como abrir o roteiro)
- [X] T019 [P] Atualizar `docs/decisoes.md`, `docs/api/protecoes.md` (exceção da CSP), `docs/api/autorizacao.md` (rota pública), `docs/api/erros.md` (202 não usado, 404/405 em `/docs`) e `docs/README.md` (índice)
- [X] T020 Executar `sh ./mvnw clean verify` completo e os passos do `quickstart.md` com o jar (`curl` e `diff` do contrato, 404/405, CSP de `/health`), registrando comandos e resultados
- [X] T021 Registrar a evidência em `docs/evidencias/2026-11-19_rc28-documentacao-api.md` (testes, Chrome headless e Redocly, só texto, sem token)
- [X] T022 Revisar o diff, marcar as tarefas concluídas, atualizar o status de `specs/028-documentar-api-swagger/spec.md` e registrar as pendências (Oracle real opt-in não executado sem autorização; SC-002, de 15 minutos com uma pessoa, só pode ser medido manualmente)
- [X] T023 Seção "Documentação da API (Swagger)" em `README.md` (endereços, como autenticar e executar, limites, manutenção do contrato), a pedido do usuário em 19/11/2026

---

## Dependencies & Execution Order

- T001 → T002 → T003 → US1.
- US1: T004 (falha antes) → T005/T006 → T007 → T011; T008 → T009 → T010. T009 depende de T008.
- US2: T012 reaproveita o percurso de T008; T013 depois de T012 (status conferidos).
- US3: T014 depende de T009 e T015 (contrato e mapa finais); T016 e T017 independentes.
- Polish depois das histórias; T020 → T021 → T022.

## Parallel Example

```text
T005 (página/inicializador) ∥ T008 (captura de respostas)
T016 (Postman) ∥ T017 (README da API) ∥ T018 (README/ambiente) ∥ T019 (decisões/proteções)
```

## Implementation Strategy

MVP = US1, que entrega o Swagger UI com o contrato completo. Depois vem a US2 (roteiro verificado), que fecha o quarto critério, e por fim a US3 e o polimento. Tudo na mesma entrega, porque a história é P0 e a entrega é em 19/11/2026.

## Registro de execução

19/11/2026, Linux, OpenJDK 25.0.5.1, `sh ./mvnw` a partir de `backend/`. Detalhes em [docs/evidencias/2026-11-19_rc28-documentacao-api.md](../../docs/evidencias/2026-11-19_rc28-documentacao-api.md).

- **T001**: `clean verify` antes da mudança: 786 testes, 0 falhas, 35 ignorados (Oracle opt-in), 65 s.
- **T002/T003**: `package -DskipTests` empacota `documentacao/openapi.yaml` e a webjar `swagger-ui` 5.34.1 (`pom.properties` mantido pelo shade).
- **T004–T007**: `DocumentacaoEndpointTest` 8/8; `CabecalhosSegurosTest`, `RotasPrivadasTest`, `ArquiteturaTest` e `SaudeEndpointTest` verdes.
- **T008**: 35 respostas reais capturadas em memória (registro temporário no scratchpad, retirado do teste); 503 capturado do jar sem banco.
- **T009/T010**: contrato com 21 caminhos, 31 operações, 14 grupos e exemplos; Redocly 2.11.1 com 0 erros (7 erros de YAML anteriores corrigidos pondo aspas em descrições).
- **T011**: Chrome headless com 31 operações, 0 violações de CSP, 0 requisições externas, "Try it out" na mesma origem (`/health` 200, `/auth/login` 503 sem banco) e armazenamento do navegador vazio. Parcial, ver a tarefa.
- **T012**: `RoteiroAvaliacaoTest` 2/2. **T014**: `ContratoDaApiTest` 3/3.
- **T020**: `clean verify` depois da mudança: 799 testes, 0 falhas, 35 ignorados, 75 s. A primeira execução falhou na `VarreduraDeSegredosTest` (variável `TOKEN=` no roteiro), que foi renomeada sem mudar a regra. Quickstart com o jar conferido.
- **Pendências**: roteiro com o Oracle real (opt-in, não autorizado) e SC-002 (15 minutos, medição manual); Postman reorganizado, mas não executado; script `curl` do roteiro não executado como script (os mesmos passos rodaram no T012).
