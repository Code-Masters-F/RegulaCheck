# Feature Specification: Escopo do app Android e protótipo visual na documentação

**Feature Branch**: `027-normalizar-estado-app-desenho`

**Created**: 2026-11-19

**Status**: Implementada em 19/11/2026 (só documentação; ver "Resultado da verificação" em `tasks.md`)

**Input**: alinhar a documentação ao que foi entregue no app Android (Épico 6, [#40](https://github.com/Code-Masters-F/RegulaCheck/issues/40)) e no protótipo visual (Épico 10, [#14](https://github.com/Code-Masters-F/RegulaCheck/issues/14)), e atualizar o Status das specs já entregues. Sem alteração de código.

## Contexto

O escopo da disciplina pede, no frontend mobile, 4 ou mais telas funcionais que leem e gravam pela API e tratam erros de conexão. O app do RegulaCheck entrega 10 telas sobre a API própria. O protótipo visual no pen.dev (94 telas e estados) foi feito como material adicional, para orientar o app. Alguns textos do repositório precisavam refletir esse escopo, e specs entregues seguiam como "Draft".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Avaliador entende o que o app faz (Priority: P1)

Quem lê `docs/decisoes.md`, `mobile/README.md` ou `mobile/design/INDEX.md` encontra a mesma descrição: o app cobre o percurso principal da paciente e a visão do administrador; as demais operações do domínio são usadas pela API, documentadas no OpenAPI e no Postman; o protótipo visual é material de apoio.

**Why this priority**: o avaliador lê o projeto pela documentação; ela precisa descrever com clareza o que foi entregue.

**Independent Test**: ler os três arquivos e conferir que descrevem o mesmo escopo e apontam para a mesma decisão.

**Acceptance Scenarios**:

1. **Given** as pendências da RC-16, RC-17 e RC-21 + RC-22 em `docs/decisoes.md`, **When** lidas, **Then** descrevem o app conforme a linha "Escopo do app Android".
2. **Given** `docs/decisoes.md`, **When** se procura o escopo do app, **Then** a linha "Escopo do app Android" lista as 10 telas e indica onde estão documentadas as demais operações.
3. **Given** `mobile/README.md`, `mobile/design/INDEX.md` e `design/README.md`, **When** comparados com `docs/decisoes.md`, **Then** descrevem o protótipo como material de apoio e remetem à decisão por link.

---

### User Story 2 - Specs refletem o que foi entregue (Priority: P2)

Quem abre uma spec do app ou do protótipo vê no Status o que foi entregue, com PR e evidência.

**Why this priority**: rastreabilidade entre história, implementação e verificação (CLAUDE.md, seção 2.1).

**Independent Test**: para cada spec alterada, o Status cita PR e evidência existentes.

**Acceptance Scenarios**:

1. **Given** as specs 017, 022 e 024 em "Draft", **When** atualizadas, **Then** o Status diz "Implementada e verificada", com data, PR e evidência, incluindo os ajustes de 19/11/2026 (PRs #82 e #83).
2. **Given** as specs 007 e 015, **When** atualizadas, **Then** o Status descreve o protótipo concluído.
3. **Given** as specs 016, 019, 020-reenviar e 021, **When** atualizadas, **Then** dizem onde a tela foi entregue ou que a operação é usada pela API.

### Edge Cases

- Texto já correto não é reescrito.
- Registros de verificação de outras histórias (Oracle, quickstart) ficam como estão.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `docs/decisoes.md` MUST ter a linha "Escopo do app Android (Épico 6, #40)" com as 10 telas do app e a referência ao OpenAPI e ao Postman para as demais operações.
- **FR-002**: `docs/decisoes.md` MUST descrever o protótipo visual (`design/RegulaCheck.pen`, 94 telas e estados) como material adicional de apoio ao app.
- **FR-003**: Os registros da RC-16, RC-17 e RC-21 + RC-22 MUST descrever o app conforme FR-001.
- **FR-004**: `mobile/README.md`, `mobile/design/INDEX.md`, `mobile/design/textos.md` (E3.4 "Documento ausente") e `design/README.md` MUST ser coerentes com FR-001 e FR-002, por link.
- **FR-005**: O Status das specs 007, 015, 016, 017, 019, 020-reenviar, 021, 022 e 024 MUST citar PR e evidência existentes.
- **FR-006**: Nenhum código, script SQL, app, simulador ou `.pen` MUST ser alterado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das menções ao app nos documentos alterados coerentes com a linha "Escopo do app Android".
- **SC-002**: 9 de 9 Status de spec com PR e evidência.
- **SC-003**: Diff restrito a documentação.

## Assumptions

- O escopo do app segue o corpo da #40 (percurso principal), escrito pelo autor das issues.
- Specs fora do app e do protótipo ficam como estão.
