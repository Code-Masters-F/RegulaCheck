# Tasks: Projeto executável e contratos (RC-02)

**Input**: `specs/002-projeto-executavel-contratos/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)
**Backlog**: RC-02 — Code-Masters-F/ClickBus#7
**Testes**: incluídos (FR-013 exige teste automatizado do health e do formato de erro). Sem Spring Boot em teste.
**Escopo**: sem entidades de negócio, CRUD, autenticação, regras documentais, IA ou sensores funcionais.

## Format: `- [ ] T### [P?] [US?] Descrição com caminho de arquivo`

---

## Phase 1: Setup (migração da base)

- [X] T001 Registrar o estado inicial em `specs/002-projeto-executavel-contratos/research.md` (seção "Estado antes da migração"): rodar `.\mvnw.cmd -version` e `.\mvnw.cmd clean verify` em `backend/` e anotar o resultado real da fundação antiga, para comparação
- [X] T002 Reescrever `backend/pom.xml` sem `spring-boot-starter-parent` e sem dependências Spring/JPA/PostgreSQL: `maven.compiler.release=21`, dependências `jetty-ee10-servlet` 12.1.15, `jackson-databind` 2.23.0, `slf4j-api` e `slf4j-simple` estável, `junit-jupiter` 6.1.3 (test); plugins compiler, surefire e `maven-shade-plugin` (ou `maven-assembly-plugin`) para gerar um jar executável `backend/target/regulacheck-backend.jar` (`finalName` fixo), todos com versão fixa; registrar as versões dos plugins no `research.md` e definir o comando de execução `java -jar backend/target/regulacheck-backend.jar`
- [X] T003 [P] Remover os resíduos da fundação antiga: `backend/src/main/resources/application.yml`, `backend/src/test/java/br/com/regulacheck/RegulaCheckApplicationTests.java` e `database/docker-compose.yml`; listar as remoções no relatório final (T036)
- [X] T004 [P] Criar `backend/src/main/resources/simplelogger.properties` com nível INFO e formato sem dados sensíveis

**Checkpoint**: `.\mvnw.cmd clean verify` compila (ainda sem classes de negócio).

---

## Phase 2: Foundational (bloqueia todas as histórias)

- [X] T005 Criar `backend/src/main/java/br/com/regulacheck/config/Configuracao.java`: lê `SERVER_PORT` (padrão 8080) e `SERVER_HOST` (padrão `0.0.0.0`) de variáveis de ambiente com validação; `DB_*`, `DOCUMENT_STORAGE_PATH`, `IOT_API_KEY` não são obrigatórias; JavaDoc em português
- [X] T006 [P] Criar `backend/src/main/java/br/com/regulacheck/common/exception/ErroPadrao.java` (record: `codigo`, `mensagem`, `detalhes`, `idRastreamento`) conforme `contracts/erros.md`
- [X] T007 [P] Criar `backend/src/main/java/br/com/regulacheck/common/exception/ExcecaoNegocio.java` com código e status HTTP, sem stack trace exposto
- [X] T008 Criar `backend/src/main/java/br/com/regulacheck/common/exception/RespostaErro.java`: escreve `ErroPadrao` em JSON com o status HTTP, gera `idRastreamento`, registra o detalhe técnico apenas no log e nunca devolve stack trace, SQL ou caminho (depende de T006)

---

## Phase 3: User Story 1 — API base que inicia e informa que está viva (P1) 🎯 MVP

**Objetivo**: FR-001, FR-002, FR-003, FR-004, FR-013.
**Teste independente**: iniciar a API sem banco e obter `GET /health` = 200 `{"status":"UP"}`.

### Testes (escrever antes e confirmar que falham)
- [X] T009 [P] [US1] Criar `backend/src/test/java/br/com/regulacheck/saude/SaudeEndpointTest.java`: sobe o servidor em porta efêmera e valida `GET /health` = 200 com corpo `{"status":"UP"}` e `Content-Type` JSON, sem banco nem variáveis
- [X] T010 [P] [US1] Criar `backend/src/test/java/br/com/regulacheck/common/exception/ErroPadraoTest.java`: rota inexistente devolve 404 `NAO_ENCONTRADO` no formato padrão, método não permitido devolve 405, e o corpo não contém "Exception", "at br.com" nem SQL
- [X] T011 [P] [US1] Criar `backend/src/test/java/br/com/regulacheck/ArquiteturaTest.java`: nenhum arquivo `.java` de `src/main` referencia `org.springframework`, `jakarta.persistence` ou `org.hibernate`, e o `pom.xml` não declara Spring em escopo de produção

### Implementação
- [X] T012 [P] [US1] Criar `backend/src/main/java/br/com/regulacheck/saude/SaudeRepository.java` (contrato) e `SaudeRepositoryMemoria.java` (implementação em memória, estado técnico fixo), com JavaDoc dizendo que é só demonstração da separação de camadas e não finge negócio
- [X] T013 [US1] Criar `backend/src/main/java/br/com/regulacheck/saude/SaudeService.java` que consulta o repositório e devolve o estado (depende de T012)
- [X] T014 [US1] Criar `backend/src/main/java/br/com/regulacheck/saude/SaudeController.java` (servlet Jakarta, `GET /health`, 200 JSON, só estado técnico, sem estado por requisição em campos) (depende de T013)
- [X] T015 [US1] Criar `backend/src/main/java/br/com/regulacheck/config/Composicao.java`: monta manualmente Repository → Service → Controller por construtor e registra o servlet de erro/404 padronizado (depende de T008, T014)
- [X] T016 [US1] Criar `backend/src/main/java/br/com/regulacheck/config/Servidor.java`: `Server` + `ServletContextHandler`, `start`, `stop` e porta efêmera para teste; mensagem clara se a porta estiver ocupada (depende de T005, T015)
- [X] T017 [US1] Reescrever `backend/src/main/java/br/com/regulacheck/RegulaCheckApplication.java` com `main` que lê `Configuracao`, monta `Composicao` e inicia `Servidor`, sem exigir banco
- [X] T018 [US1] Executar `.\mvnw.cmd clean verify` em `backend/` e confirmar T009–T011 verdes; iniciar a API e consultar `GET /health` com `curl`; iniciar com `java -jar target/regulacheck-backend.jar` e registrar o comando e o resultado reais em `specs/002-projeto-executavel-contratos/research.md` (seção "Verificação")

**Checkpoint**: US1 funcional e verificada.

---

## Phase 4: User Story 2 — Repositório organizado e configuração de exemplo (P1)

**Objetivo**: FR-005, FR-006, FR-015.
**Teste independente**: as 5 áreas existem com README; `.env.example` só com nomes; sem segredos.

- [X] T019 [P] [US2] Atualizar `mobile/README.md`: propósito (app Android Kotlin consumindo a API), estado "não criado", endereço do emulador (referência ao T031)
- [X] T020 [P] [US2] Atualizar `simulador-iot/README.md`: propósito, 2 sensores, JSON por HTTP, estado "não implementado"
- [X] T021 [P] [US2] Atualizar `database/README.md`: Oracle da faculdade, `docker-compose.yml` removido, scripts em `database/scripts/` (RC-03)
- [X] T022 [P] [US2] Atualizar `backend/README.md`: pacotes por funcionalidade, camadas, como executar e testar (comandos verificados no T018)
- [X] T023 [US2] Revisar `.env.example`: manter apenas nomes das variáveis (`SERVER_PORT`, `SERVER_HOST`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DOCUMENT_STORAGE_PATH`, `IOT_API_KEY`), com comentário "nenhuma é obrigatória para iniciar" e sem valores reais
- [X] T024 [US2] Varredura de segredos nos arquivos versionados (`git grep -n -i -E "password=|token|secret|BEGIN .* KEY|github_pat"` e `git ls-files .env`); registrar o resultado e corrigir qualquer achado

---

## Phase 5: User Story 3 — Contratos compartilhados (P1)

**Objetivo**: FR-007, FR-008, FR-009.
**Teste independente**: dois leitores extraem a mesma lista de campos de cada contrato.

- [X] T025 [P] [US3] Publicar `specs/002-projeto-executavel-contratos/contracts/solicitacao-upload-analise.md` em `docs/api/contratos-dados.md`, ajustando links relativos para `docs/`
- [X] T026 [P] [US3] Publicar `specs/002-projeto-executavel-contratos/contracts/erros.md` em `docs/api/erros.md` e copiar `contracts/openapi.yaml` para `docs/api/openapi.yaml`
- [X] T027 [US3] Atualizar `docs/api/README.md` e `CONTRATO_API.md` com links para os contratos novos e o status por rota (implementada ou planejada); conferir que não há contradição de rotas, campos ou códigos HTTP
- [X] T028 [P] [US3] Alinhar `docs/api/openapi.yaml` com o comportamento real do `GET /health` e do 404 (corpo e cabeçalhos) após T018, e atualizar `docs/api/RegulaCheck.postman_collection.json` para `GET {{baseUrl}}/health` (hoje aponta para `/actuator/health`)

---

## Phase 6: User Story 4 — Estados e revisões (P1)

**Objetivo**: FR-010, FR-011.
**Teste independente**: a tabela de transições responde cada par (estado, evento).

- [X] T029 [US4] Publicar `contracts/estados-e-revisoes.md` em `docs/api/estados-e-revisoes.md` e atualizar a seção "Estados" de `docs/arquitetura.md` com link, a regra "após correção ou reenvio vai para `PROCESSANDO` (revisão + 1), sem voltar a `RASCUNHO`" e a decisão de nova revisão durante `PROCESSANDO`
- [X] T030 [US4] Revisar a tabela de transições: cada estado alcançável e com saída (exceto `SUBMETIDA_SIMULACAO`), nenhum par ambíguo; corrigir a origem se houver contradição com `docs/protocolo-academico.md`

---

## Phase 7: User Story 5 — Endereçamento do emulador e do aparelho (P2)

**Objetivo**: FR-012.
**Teste independente**: o health é alcançado a partir do emulador seguindo só a documentação.

- [X] T031 [US5] Documentar em `docs/ambiente.md` (seção "Acesso do app à API"): `10.0.2.2:8080` para emulador, IP do computador para aparelho físico, regra de firewall, `SERVER_HOST`, e o aviso de que `localhost` não funciona; incluir a nota sobre tráfego HTTP em desenvolvimento no Android 9+ e que HTTPS é exigido em execução remota
- [X] T032 (PARCIAL: loopback e IP local verificados; emulador e aparelho PENDENTES por decisão do usuário) [US5] Verificar o endereçamento: curl em `127.0.0.1`, com o IP da rede local do computador e, se houver emulador ou aparelho disponível, a partir dele; registrar apenas o que foi realmente executado e marcar como "não verificado" o que não foi

---

## Phase 8: Polish e fechamento

- [X] T033 Atualizar `README.md` e `docs/ambiente.md` (pré-requisitos, JDK, comando de execução e de testes verificados, variáveis opcionais) e `docs/decisoes.md` (substituir as linhas antigas "Dependências" e "Health `/actuator/health`" e registrar Jetty 12.1.15, Jackson 2.23.0, JUnit 6.1.3, nova revisão durante processamento, remoção de PostgreSQL/Docker); remover o aviso "fundação antiga" onde estiver superado
- [X] T034 Revisão cruzada: `CLAUDE.md`, constituição v2.0.0, `docs/arquitetura.md`, `docs/decisoes.md` e os contratos; buscar termos proibidos em produção (`git grep -n -i -E "springframework|persistence|hibernate|postgres" backend/src/main backend/pom.xml`)
- [X] T035 (PARCIAL: ver tabela no research.md; SC-004, SC-005 e SC-008 dispensados por decisão do usuário; SC-007 PENDENTE, teste no emulador) Conferir os critérios SC-001 a SC-008 seguindo `specs/002-projeto-executavel-contratos/quickstart.md` e marcar como verificado só o que foi executado
- [X] T036 Relatório final da RC-02: vínculo RC-02 → FR/tarefas/arquivos, distinguindo implementado, verificado, pendente e fora do escopo; listar remoções da fundação antiga; sem commit sem autorização

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → Phase 3 (US1). US2, US3 e US4 só dependem do que cada uma cita e podem rodar em paralelo a partir do fim da Phase 2, exceto: T022 depende de T018; T028 depende de T018; T032 depende de T018.
- Dentro de US1: testes (T009–T011) primeiro; T012 → T013 → T014 → T015 → T016 → T017 → T018.
- US3 e US4 editam arquivos diferentes e podem andar juntas; T027 depende de T025 e T026.
- Phase 8 depende de todas.

## Parallel Example

```text
Depois da Phase 2:
  US1: T009 T010 T011 (testes) || T012 (repository)  →  T013 … T018
  US2: T019 T020 T021 (README)       US3: T025 T026       US4: T029
```

## Implementation Strategy

- **MVP**: Phases 1, 2 e 3 (API inicia e responde `/health`, testada). Libera o desenvolvimento de RC-03 e RC-04.
- Em seguida US2, US3 e US4 (todas P1), depois US5 (P2) e o fechamento.
- Parar e validar em cada checkpoint; nenhuma tarefa é marcada concluída sem a verificação correspondente.

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| FR-001, FR-002 | T005, T009, T014–T018 |
| FR-003 | T012–T015 |
| FR-004 | T002, T003, T011, T034 |
| FR-005, FR-006 | T019–T023 |
| FR-007, FR-008, FR-009 | T006–T008, T010, T025–T028 |
| FR-010, FR-011 | T029, T030 |
| FR-012 | T031, T032 |
| FR-013 | T009–T011, T018 |
| FR-014 | T022, T033 |
| FR-015 | T024, T034 |
| SC-001 a SC-008 | T018, T024, T030, T032, T035 |
