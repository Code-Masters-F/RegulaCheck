# Feature Specification: Recorte e critérios do demonstrador (RC-01)

**Feature Branch**: `001-recorte-demonstrador` (sem branch dedicada; trabalho em `main`)

**Created**: 2026-11-12

**Status**: Draft

**Backlog**: RC-01 — Code-Masters-F/ClickBus#6 · Épico 1 (Escopo e fundação) · P0 · até 12/11/2026 · sem dependências

**Input**: User description: "RC-01 — Fixar o recorte e os critérios do demonstrador. Como equipe, quero definir o serviço e seus critérios para implementar uma conferência verificável."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fixar o recorte e o protocolo acadêmico fictício (Priority: P1)

A equipe precisa de um recorte único e registrado do que o demonstrador confere: localidade demonstrativa, especialidade e versão inicial do protocolo acadêmico fictício. Sem isso, banco, regras, telas e dados de teste divergem.

**Why this priority**: todas as demais histórias (contratos, modelagem, motor de regras, dados sintéticos) dependem desse recorte.

**Independent Test**: um membro da equipe que não participou da definição lê o documento e consegue dizer, sem perguntar, qual é a localidade, a especialidade, a versão do protocolo e que o protocolo é fictício.

**Acceptance Scenarios**:

1. **Given** o documento de recorte publicado, **When** alguém o consulta, **Then** encontra localidade demonstrativa, especialidade e identificador/versão inicial do protocolo, todos marcados como fictícios.
2. **Given** o documento de recorte, **When** é comparado com o CLAUDE.md e a constituição, **Then** não há contradição quanto a escopo, limites clínicos e dados sintéticos.

---

### User Story 2 - Definir as regras de conferência documental (Priority: P1)

A equipe define até 6 regras do protocolo, cada uma com código, descrição, evidência exigida, condição de aplicação e mensagem à paciente, para que o motor de regras e os casos de teste sejam derivados sem interpretação.

**Why this priority**: as regras são o núcleo do que será "verificável" em extração, análise, resumo e testes.

**Independent Test**: dado um conjunto sintético de documentos descrito em papel, duas pessoas aplicam as regras separadamente e chegam ao mesmo resultado por regra.

**Acceptance Scenarios**:

1. **Given** o catálogo de regras, **When** cada regra é examinada, **Then** possui código único, descrição, evidência exigida, condição de aplicação e mensagem.
2. **Given** o catálogo, **When** contado, **Then** há no máximo 6 regras, e ao menos uma é condicional e pode resultar em "não aplicável".
3. **Given** uma regra cuja evidência está ilegível no documento, **When** aplicada, **Then** o resultado é "não verificável" e não "requisito ausente".

---

### User Story 3 - Distinguir os resultados possíveis de cada regra (Priority: P1)

A equipe fixa a semântica dos estados: obrigatório, condicional, não aplicável e informação não verificável, e como cada um afeta o resultado geral, para que ninguém trate "não consegui ler" como "não existe".

**Why this priority**: evita falsa pendência e falsa aprovação, risco central do produto.

**Independent Test**: uma tabela de decisão cobre cada combinação (aplicável/não aplicável × evidência encontrada/não localizada/ilegível) e leva a um único resultado.

**Acceptance Scenarios**:

1. **Given** uma regra obrigatória com evidência encontrada, **When** avaliada, **Then** resultado "atende".
2. **Given** uma regra obrigatória com evidência não localizada em documento legível, **When** avaliada, **Then** resultado "pendente".
3. **Given** uma regra com documento ilegível, **When** avaliada, **Then** resultado "não verificável", que impede o resultado geral "sem pendências identificadas".
4. **Given** uma regra condicional cuja condição não se aplica, **When** avaliada, **Then** resultado "não aplicável" e a regra não gera pendência.

---

### User Story 4 - Registrar tecnologias e limitações do protótipo (Priority: P2)

A equipe registra as tecnologias estudadas/adotadas, as decisões de 12/11/2026 e as limitações do protótipo, para o grupo e o avaliador entenderem o que é real e o que é simulado.

**Why this priority**: exigência de transparência acadêmica; depende menos de outras histórias, mas precisa estar pronta antes da fundação técnica.

**Independent Test**: o registro lista cada tecnologia com status (adotada, simulada, descartada) e cada limitação com seu impacto; nenhum mock é descrito como integração real.

**Acceptance Scenarios**:

1. **Given** o registro, **When** consultado, **Then** informa: Java sem Spring e sem JPA (Spring Boot somente em testes), banco Oracle da faculdade via JDBC, IA e extração/OCR mockadas e identificadas como simuladas, dois sensores simulados por script Java enviando JSON por HTTP.
2. **Given** o registro, **When** consultado, **Then** lista as limitações: dados sintéticos, protocolo fictício, sem diagnóstico/urgência/autorização, sem fila real do SUS, "sem pendências" não é aprovação do SUS, limites de sensores demonstrativos.

---

### Edge Cases

- Documento legível, mas sem o campo exigido: pendência (a ausência é verificável).
- Documento ilegível ou protegido: "não verificável", com orientação de reenvio, nunca pendência por ausência.
- Dois documentos com dados divergentes (ex.: nome diferente): regra de coerência fica pendente e indica quem corrige.
- Paciente menor de 18 anos: a regra condicional passa a se aplicar; para adulta, "não aplicável".
- Informação clínica ausente no encaminhamento: pendência cuja correção cabe à unidade emissora, não à paciente.
- Documento com instruções embutidas (ex.: "ignore as regras"): tratado como dado, sem efeito sobre critérios.
- Mais de 6 regras desejadas: as excedentes ficam fora do MVP (candidatas a melhoria P1).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O recorte MUST registrar uma localidade demonstrativa fictícia, uma especialidade e a versão inicial do protocolo acadêmico, identificando-os como fictícios e não oficiais.
- **FR-002**: O protocolo MUST conter no máximo 6 regras; cada regra MUST ter código único, descrição, evidência exigida, condição de aplicação e mensagem à paciente.
- **FR-003**: Cada regra MUST ser classificada como obrigatória ou condicional; regras condicionais MUST declarar a condição que as torna aplicáveis.
- **FR-004**: O catálogo MUST definir os resultados por regra: atende, pendente, não verificável e não aplicável, com critério objetivo para cada um.
- **FR-005**: Documento ilegível, protegido ou com leitura vazia MUST resultar em "não verificável", nunca em "requisito ausente"; não encontrar um campo não comprova sua ausência.
- **FR-006**: O catálogo MUST definir como os resultados por regra compõem o resultado geral: "sem pendências identificadas" exige que todas as regras aplicáveis estejam "atende" e nenhuma "não verificável"; esse resultado MUST NOT ser descrito como aprovação do SUS.
- **FR-007**: Cada pendência MUST indicar o responsável provável pela correção: paciente ou unidade emissora. Informação clínica ausente MUST ser atribuída à unidade emissora.
- **FR-008**: As mensagens das regras MUST ser compreensíveis à paciente e MUST NOT diagnosticar, prescrever, definir urgência, autorizar atendimento ou prometer vaga/aprovação.
- **FR-009**: O recorte MUST afirmar uso exclusivo de dados sintéticos e MUST NOT apresentar o protocolo como exigência oficial nem prometer redução real de filas.
- **FR-010**: O registro de tecnologias MUST listar cada tecnologia estudada/adotada com status (adotada, simulada, descartada) e as decisões de 12/11/2026: Java sem Spring e sem JPA (Spring Boot somente em testes), Oracle da faculdade via JDBC, IA e extração/OCR mockadas e identificadas como simuladas, dois sensores simulados por script Java enviando JSON por HTTP.
- **FR-011**: O registro MUST listar as limitações do protótipo e diferenciar o que é implementado, simulado, pendente e fora do escopo.
- **FR-012**: Os artefatos MUST permanecer coerentes com CLAUDE.md, constituição v2.0.0 e docs/decisoes.md; qualquer divergência encontrada MUST ser corrigida na origem.
- **FR-013**: Este trabalho MUST ser exclusivamente documental: não inclui código de produção, banco nem telas.

### Key Entities

- **Recorte do demonstrador**: localidade fictícia, especialidade, versão do protocolo, limites de escopo.
- **Protocolo acadêmico**: conjunto versionado de regras, com identificador, versão e aviso de que é fictício.
- **Regra**: código, descrição, evidência exigida, natureza (obrigatória/condicional), condição de aplicação, mensagem, responsável provável pela correção.
- **Resultado de regra**: atende, pendente, não verificável ou não aplicável.
- **Resultado geral**: composição dos resultados por regra (ex.: com pendências, requer conferência, sem pendências identificadas).
- **Registro de tecnologia / limitação**: item, status e impacto.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das regras do catálogo, os cinco atributos (código, descrição, evidência, condição, mensagem) estão preenchidos.
- **SC-002**: Duas pessoas aplicando o catálogo separadamente a 5 casos sintéticos em papel obtêm o mesmo resultado por regra em 100% dos casos.
- **SC-003**: Um membro da equipe localiza localidade, especialidade, versão do protocolo e regras em menos de 2 minutos.
- **SC-004**: Zero contradições entre o documento de recorte, CLAUDE.md, constituição e docs/decisoes.md, verificado por revisão cruzada.
- **SC-005**: As histórias RC-02 e RC-03 podem começar sem pedir nenhuma definição adicional de regras, localidade ou especialidade.

## Assumptions

- Localidade demonstrativa: município fictício ("Município Exemplo"), sem vínculo com rede real de saúde; horário de referência de Salvador.
- Especialidade demonstrativa: consulta ambulatorial em cardiologia, escolhida só por ter documentação típica simples; não implica conteúdo clínico.
- Versão inicial do protocolo: `PROTOCOLO-ACADEMICO-FICTICIO v0.1`, de natureza demonstrativa.
- Catálogo proposto (a confirmar na revisão do grupo): RC-R01 encaminhamento presente; RC-R02 identificação da paciente coerente entre documentos; RC-R03 documento de identificação ou cartão do SUS (sintéticos); RC-R04 motivo/justificativa do encaminhamento presente (correção pela unidade emissora); RC-R05 encaminhamento dentro da validade demonstrativa; RC-R06 (condicional) autorização do responsável legal quando a paciente tiver menos de 18 anos.
- O documento de recorte e o catálogo serão publicados em `docs/` (local exato decidido no plano), com referência em `docs/decisoes.md`.
- Tecnologias já decididas em 12/11/2026 não são rediscutidas aqui, apenas registradas; detalhes técnicos pertencem a CLAUDE.md, constituição e `docs/arquitetura.md`.
- O recorte cobre apenas a conferência documental; sensores, IA e extração são mockados e não alteram as regras.
- A revisão final do catálogo pelo grupo pode ocorrer após o rascunho sem bloquear RC-02.
