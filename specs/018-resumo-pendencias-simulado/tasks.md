# Tasks: RC-14 resumo de pendências simulado

**Input**: [spec.md](spec.md), [plan.md](plan.md), [contracts/api-resumo.md](contracts/api-resumo.md)

## Phase 1: Setup

- [x] T001 Conferir campos `RESUMO_TEXTO` e `RESUMO_ORIGEM` em `database/scripts/01_criar_esquema.sql` e migrações.

## Phase 2: Foundational

- [x] T002 Definir saída estruturada e contrato do gerador em `backend/src/main/java/br/com/regulacheck/resumo/service/`.
- [x] T003 Implementar validação de códigos e conteúdo em `backend/src/main/java/br/com/regulacheck/resumo/service/`.

## Phase 3: User Story 1 — Entender pendências (P1)

**Goal**: Gerar e consultar orientação da análise vigente.
**Independent Test**: Análise sintética com pendências gera itens por regra e mantém resultados.

- [x] T004 [US1] Implementar adaptador simulado em `backend/src/main/java/br/com/regulacheck/resumo/service/`.
- [x] T005 [US1] Persistir resumo condicionado à vigência em `backend/src/main/java/br/com/regulacheck/analise/repository/`.
- [x] T006 [US1] Incluir resumo nas consultas em `backend/src/main/java/br/com/regulacheck/analise/`.
- [x] T007 [US1] Criar serviço, rota e composição em `backend/src/main/java/br/com/regulacheck/resumo/`, `backend/src/main/java/br/com/regulacheck/solicitacao/controller/SolicitacaoController.java` e `backend/src/main/java/br/com/regulacheck/config/Composicao.java`.

## Phase 4: User Story 2 — Recuperar falha (P2)

**Goal**: Fallback identificado e repetição recuperável.
**Independent Test**: Falha e código desconhecido levam a `PADRAO`; nova tentativa recupera.

- [x] T008 [US2] Implementar fallback e repetição em `backend/src/main/java/br/com/regulacheck/resumo/service/` e `backend/src/main/java/br/com/regulacheck/analise/repository/`.
- [x] T009 [US2] Verificar erro e recuperação em `backend/src/test/java/br/com/regulacheck/analise/ResumoEndpointTest.java`.

## Phase 5: Verificação e documentação

- [x] T010 Verificar contrato HTTP, autorização e vigência em memória em `backend/src/test/java/br/com/regulacheck/analise/ResumoEndpointTest.java`.
- [x] T011 Atualizar `README.md`, `docs/decisoes.md`, `docs/api/openapi.yaml` e `docs/api/RegulaCheck.postman_collection.json`.
- [x] T012 Executar verificação relevante e registrar evidência em `specs/018-resumo-pendencias-simulado/tasks.md`.
- [x] T013 Verificar gravação e consulta do resumo no Oracle real em `backend/src/test/java/br/com/regulacheck/analise/AnaliseOracleTest.java` após autorização explícita para criar e remover dados de teste na conta compartilhada.

## Dependencies

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 → T012. T013 depende de acesso à conta Oracle compartilhada. US2 usa a rota da US1.

## Implementation Strategy

Entregar primeiro a orientação para análise vigente; acrescentar fallback, recuperação e verificações. Não criar commit nem publicar.

## Evidência de 16/11/2026

- `mvn -DskipTests compile`: BUILD SUCCESS, 256 arquivos Java compilados.
- `mvn -DskipTests test-compile`: BUILD SUCCESS, 151 arquivos de teste compilados.
- `mvn -Dtest=*Analise*Test,ResumoEndpointTest test`: 37 testes, 0 falhas, 0 erros, 2 ignorados (Oracle não habilitado).
- `mvn -Dtest=ResumoEndpointTest test`: 6 testes, 0 falhas, 0 erros; cobre pendências, sem pendências, conferência, autorização, fallback e mudança durante geração.
- Teste Oracle ampliado com asserções de resumo, compilado com `mvn -DskipTests test-compile`; execução bloqueada pela revisão automática porque usa conta Oracle compartilhada para criar e remover dados.
- OpenAPI YAML e coleção Postman JSON analisados sem erro de sintaxe; `git diff --check` sem erros.
- **Pendente**: execução contra Oracle real. O teste opt-in cria/remove linhas sintéticas na conta compartilhada. A revisão automática rejeitou a execução por falta de autorização específica para essa operação.
- **17/11/2026 — Oracle real**: `RC_TESTE_ORACLE=SIM mvnw test -Dtest=AnaliseOracleTest`: 2 testes, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS (execução autorizada pelo responsável).
