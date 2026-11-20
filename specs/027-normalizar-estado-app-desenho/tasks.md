# Tasks: Escopo do app Android e protótipo visual na documentação

**Input**: `specs/027-normalizar-estado-app-desenho/` (spec.md, plan.md)

**Tests**: sem testes de código (só documentação).

## Phase 1: Setup

- [X] T001 Partir da `main` atualizada (19/11/2026, depois dos PRs #81, #82 e #83) na branch `027-normalizar-estado-app-desenho`

## Phase 2: User Story 1 — Avaliador entende o que o app faz (P1)

- [X] T002 [US1] Linha "Escopo do app Android (Épico 6, #40)" e linha "Mobile" ampliada em `docs/decisoes.md`
- [X] T003 [US1] Linha do protótipo visual em `docs/decisoes.md` (material adicional, 94 telas e estados)
- [X] T004 [US1] Registros da RC-16, RC-17 e RC-21 + RC-22 em `docs/decisoes.md` coerentes com o escopo do app
- [X] T005 [P] [US1] `mobile/README.md`: operações usadas pela API, regra de login por perfil (RC-20) e seção do protótipo
- [X] T006 [P] [US1] `mobile/design/INDEX.md`, `mobile/design/textos.md` (E3.4) e `design/README.md` coerentes com a decisão

## Phase 3: User Story 2 — Specs refletem o que foi entregue (P2)

- [X] T007 [P] [US2] Status de 017, 022 e 024 com PR, evidência e ajustes de 19/11/2026
- [X] T008 [P] [US2] Status de 007 e 015 (protótipo)
- [X] T009 [P] [US2] Status de 016, 019, 020-reenviar e 021

## Phase 4: Verificação

- [X] T010 Conferir as menções ao app nos arquivos alterados e revisar o diff (só documentação)

## Resultado da verificação (19/11/2026)

- **SC-001:** as menções ao app em `docs/decisoes.md`, `mobile/README.md`, `mobile/design/INDEX.md`, `design/README.md` e nos Status alterados seguem a linha "Escopo do app Android".
- **SC-002:** 9 de 9 Status citam PR e evidência.
- **SC-003:** `git diff --numstat` mostra só Markdown de documentação e specs; código, SQL, app, simulador e `.pen` sem alteração.
