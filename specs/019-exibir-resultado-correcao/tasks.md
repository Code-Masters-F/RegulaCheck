# Tasks: RC-15 — recorte backend

**Branch**: `019-exibir-resultado-correcao` | **Backlog**: RC-15, issue #37.

**Mudança de escopo**: em 17/11/2026, o responsável pediu para ignorar as telas e focar no backend. Estas tarefas substituem as tarefas Android deste arquivo. A apresentação móvel, a acessibilidade em aparelho e a SC-005 seguem pendentes fora desta entrega; não são consideradas implementadas.

## Phase 1: Contrato e teste inicial

- [X] T001 Escrever testes de contrato em `backend/src/test/java/br/com/regulacheck/analise/OrientacaoResultadoEndpointTest.java` para os três rótulos de resultado, orientação da RC-R02 sem inferir erro no original, pendência não verificável sem afirmar ausência, orientação da unidade emissora, resumo e revisão preservados; executar e registrar falha inicial.
- [X] T002 Conferir nos testes existentes de `backend/src/test/java/br/com/regulacheck/analise/` que autorização por paciente, ausência de análise, histórico e resumo padrão mantêm os contratos atuais.

## Phase 2: Implementação

- [X] T003 Acrescentar `rotuloResultado` e `pendencias[].orientacao` em `backend/src/main/java/br/com/regulacheck/analise/dto/AnaliseResponse.java` usando somente dados gravados da análise, sem novo endpoint, persistência, diagnóstico ou alteração do responsável; rodar o teste focal até passar.
- [X] T004 Atualizar `specs/019-exibir-resultado-correcao/contracts/apresentacao-resultado.md`, `specs/014-persistir-analises-historico/contracts/api-analises.md` e `docs/api/openapi.yaml` para os campos aditivos, deixando claro que ação de UI e disponibilidade de reenvio não são decididas pelo backend da RC-15.

## Phase 3: Verificação

- [X] T005 Executar os testes focados de análise e resumo e o build necessário do backend; registrar comandos e resultados reais em `specs/019-exibir-resultado-correcao/quickstart.md`.
- [X] T006 Conferir spec, contrato e diff; registrar explicitamente que a apresentação Android, SC-005 e ações de reenvio continuam pendentes.

## Dependencies

T001 → T002 → T003 → T004 → T005 → T006. Testes antes do código. Marcar `[X]` somente após evidência.
