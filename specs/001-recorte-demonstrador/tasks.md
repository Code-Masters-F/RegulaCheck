# Tasks: Recorte e critérios do demonstrador (RC-01)

**Input**: `specs/001-recorte-demonstrador/` (plan.md, spec.md, research.md, data-model.md, contracts/catalogo-regras.md, quickstart.md)
**Backlog**: RC-01 — Code-Masters-F/ClickBus#6
**Natureza**: entrega somente documental (FR-013). Sem código, banco ou telas. Testes automatizados não solicitados; a verificação é por revisão e exercício em papel (T016–T019).

## Format: `- [ ] T### [P?] [US?] Descrição com caminho de arquivo`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente).
- **[US#]**: história da spec. Tarefas de US1–US3 editam o mesmo arquivo (`docs/protocolo-academico.md`) e por isso são sequenciais.

---

## Phase 1: Setup

- [X] T001 Ler `CLAUDE.md` (§1, §11), `.specify/memory/constitution.md` v2.0.0 e `docs/decisoes.md` para fixar a terminologia (estados, resultados, "simulado") antes de redigir
- [X] T002 Criar `docs/protocolo-academico.md` com título, aviso "documento acadêmico fictício, não é exigência oficial" e sumário das seções

## Phase 2: Foundational

Sem pré-requisitos bloqueantes além de T001–T002.

---

## Phase 3: User Story 1 — Recorte e protocolo fictício (P1) 🎯 MVP

**Objetivo**: recorte único e registrado (FR-001, FR-009).
**Teste independente**: alguém que não participou lê o documento e diz localidade, especialidade, versão e que é fictício.

- [X] T003 [US1] Em `docs/protocolo-academico.md`, escrever a seção "Recorte": localidade "Município Exemplo" (fictício), especialidade cardiologia ambulatorial (demonstrativa), protocolo `PROTOCOLO-ACADEMICO-FICTICIO v0.1` e política de versionamento `vMAJOR.MINOR` (research D1–D3)
- [X] T004 [US1] Em `docs/protocolo-academico.md`, escrever a seção "Limites do recorte": dados exclusivamente sintéticos, sem diagnóstico/urgência/autorização, sem fila real do SUS, sem promessa de redução de filas, "sem pendências" ≠ aprovação do SUS

**Checkpoint**: recorte legível e coerente com CLAUDE.md §1.

---

## Phase 4: User Story 2 — Regras de conferência (P1)

**Objetivo**: catálogo de até 6 regras completas (FR-002, FR-003, FR-007, FR-008).
**Teste independente**: cada regra tem os 5 atributos e o responsável; duas pessoas chegam ao mesmo resultado.

- [X] T005 [US2] Em `docs/protocolo-academico.md`, copiar para a seção "Catálogo de regras" a tabela RC-R01 a RC-R06 de `specs/001-recorte-demonstrador/contracts/catalogo-regras.md` (código, natureza, descrição, evidência, condição, mensagem, responsável único); registrar que regras além das 6 ficam fora do MVP (melhoria P1)
- [X] T006 [US2] Em `docs/protocolo-academico.md`, detalhar RC-R06 (condicional): condição "menos de 18 anos na data do envio" e o resultado `NAO_APLICAVEL` quando adulta; detalhar RC-R05 (180 dias demonstrativos, contados da data de emissão); indicar que a data de nascimento para R06 vem do documento de identificação (R03) e que, se ilegível, o resultado é `NAO_VERIFICAVEL`
- [X] T007 [US2] Revisar as mensagens das 6 regras: linguagem simples para a paciente, sem diagnóstico, prescrição, urgência, autorização ou promessa de vaga/aprovação (FR-008); incluir a mensagem padrão para `NAO_VERIFICAVEL` do contrato (orientação de reenvio, sem afirmar ausência) e garantir um único responsável por regra

**Checkpoint**: catálogo com 100% dos atributos preenchidos (SC-001).

---

## Phase 5: User Story 3 — Resultados por regra e resultado geral (P1)

**Objetivo**: semântica sem ambiguidade entre "pendente" e "não verificável" (FR-004, FR-005, FR-006).
**Teste independente**: a tabela de decisão leva cada combinação a um único resultado.

- [X] T008 [US3] Em `docs/protocolo-academico.md`, escrever a seção "Resultados por regra" com a tabela de derivação (aplicável × evidência) de `data-model.md`, incluindo aplicabilidade indeterminada → `NAO_VERIFICAVEL`, e a frase "não encontrar um campo não comprova sua ausência"
- [X] T009 [US3] Em `docs/protocolo-academico.md`, escrever a seção "Resultado geral" com a precedência `COM_PENDENCIAS` > `REQUER_CONFERENCIA` > `SEM_PENDENCIAS_IDENTIFICADAS`, `NAO_APLICAVEL` sem peso, e `FALHA_ANALISE` como estado de processamento fora do catálogo; conferir nomes com `docs/arquitetura.md`; decidir se `REQUER_CONFERENCIA` tem o significado ampliado ou se outro estado é criado, registrando a decisão e, se preciso, ajustando `docs/arquitetura.md`
- [X] T010 [US3] Em `docs/protocolo-academico.md`, escrever a seção "Casos sintéticos de verificação" com os 5 casos do contrato e o resultado esperado por regra e geral, definidos de forma independente da implementação: (1) tudo conforme; (2) encaminhamento sem motivo; (3) identificação ilegível; (4) menor de idade sem autorização; (5) adulta com nomes divergentes
- [X] T011 [US3] Em `docs/protocolo-academico.md`, escrever a seção "Tratamento de documentos": conteúdo de documento é dado não confiável, instruções embutidas não alteram critérios

**Checkpoint**: nenhuma combinação sem resultado definido.

---

## Phase 6: User Story 4 — Tecnologias e limitações (P2)

**Objetivo**: transparência sobre o que é real e o que é simulado (FR-010, FR-011).
**Teste independente**: cada item tem status; nenhum mock aparece como integração real.

- [X] T012 [P] [US4] Criar `docs/escopo-e-limitacoes.md` com a tabela "Tecnologias" e status (ADOTADA, SIMULADA, DESCARTADA, PENDENTE): Java sem Spring/JPA (Spring Boot só em testes), Jakarta Servlet + Jetty/Tomcat, JDBC, Oracle da faculdade, Android Kotlin, IA mockada, extração/OCR mockada, 2 sensores simulados por script Java via HTTP JSON, MQTT descartado; conferir com `docs/decisoes.md` e `CLAUDE.md` §4
- [X] T013 [P] [US4] Em `docs/escopo-e-limitacoes.md`, escrever a seção "Limitações do protótipo" (dados sintéticos, protocolo fictício, sem integração real com SUS/IA/OCR, limites de sensores demonstrativos, sensores não comprovam legibilidade) e a seção "Estado": implementado, simulado, pendente e fora do escopo; registrar que o código atual ainda é a base Spring/JPA/PostgreSQL a migrar

**Checkpoint**: o registro cobre FR-010 e FR-011.

---

## Phase 7: Polish e verificação

- [X] T014 Atualizar `docs/decisoes.md`: linha do recorte/protocolo v0.1 com link relativo para `protocolo-academico.md` e `escopo-e-limitacoes.md` (sem duplicar conteúdo)
- [X] T015 [P] Atualizar `README.md` com link para os dois documentos
- [X] T016 Revisão cruzada (SC-004): comparar os dois documentos com `CLAUDE.md`, constituição v2.0.0 e `docs/decisoes.md`; buscar por termos proibidos ("diagnóstico" como ação do sistema, "aprovado pelo SUS", "IA real", Spring em produção); corrigir a origem de qualquer divergência
- [X] T017 Validar que todos os links relativos dos documentos novos resolvem
- [ ] T018 Exercício em papel (SC-002): duas pessoas do grupo aplicam os 5 casos separadamente e comparam; registrar data, participantes (por papel) e resultado real em `docs/protocolo-academico.md` (seção "Verificação"); se não executado, escrever "pendente"
- [ ] T019 Conferir SC-001, SC-003 e SC-005 seguindo `specs/001-recorte-demonstrador/quickstart.md`; marcar tarefas concluídas somente após conferir critérios de aceite
- [ ] T020 Confirmar com o grupo, e registrar data e participantes por papel em `docs/protocolo-academico.md` (seção "Verificação"), a localidade, a especialidade, as 6 regras e a validade de 180 dias; enquanto não confirmado, marcar como "proposto"
- [X] T021 Relatório final da RC-01: vínculo RC-01 → FR/tarefas/arquivos, distinguindo documentado, verificado e pendente; sem commit sem autorização

---

## Dependencies & Execution Order

- Phase 1 → Phase 3 → Phase 4 → Phase 5 (mesmo arquivo, sequenciais).
- Phase 6 (T012–T013) independe de Phases 3–5 e roda em paralelo, em arquivo próprio.
- Phase 7 depende de todas; T014 e T015 podem rodar em paralelo entre si.
- T018 depende de T010 e de duas pessoas disponíveis; não bloqueia RC-02 (assumption da spec), mas bloqueia marcar SC-002 como verificado.

## Parallel Example

```text
Em paralelo: T012 + T013 (docs/escopo-e-limitacoes.md)  ||  T003 → T011 (docs/protocolo-academico.md)
Depois, em paralelo: T014 (docs/decisoes.md) + T015 (README.md)
```

## Implementation Strategy

- **MVP**: Phases 1, 3, 4 e 5 (recorte + regras + semântica) liberam RC-02 e RC-03.
- US4 (Phase 6) pode ser entregue no mesmo dia; é independente.
- Cada fase é um incremento verificável pelo checkpoint indicado.

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| FR-001, FR-009 | T003, T004, T020 |
| FR-002, FR-003, FR-007, FR-008 | T005, T006, T007, T020 |
| FR-004, FR-005, FR-006 | T008, T009, T010, T011 |
| FR-010, FR-011 | T012, T013 |
| FR-012 | T001, T002, T014, T016 |
| FR-013 | todas (somente documentos) |
| SC-001 a SC-005 | T016–T019, T021 |
