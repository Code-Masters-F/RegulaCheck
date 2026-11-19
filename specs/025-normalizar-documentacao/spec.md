# Feature Specification: Normalizar documentação e eliminar redundâncias

**Feature Branch**: `025-normalizar-documentacao`

**Created**: 2026-11-19

**Status**: Draft

**Input**: User description: "Vamos normalizar todos os documentos e eliminar as redundancias do projeto, uma revisão/limpeza geral sem comprometer a performance."

## Contexto

A documentação do RegulaCheck cresceu por histórias (RC-01 a RC-25). Há sinais de sobreposição entre documentos e de formatos diferentes para o mesmo tipo de conteúdo: contratos de API descritos em `CONTRATO_API.md`, em `docs/api/` e em `specs/*/contracts/`; contratos de teste em `CONTRATO_TESTES.md` e em `docs/testes/`; notas de design em `design/` e `mobile/design/`; instruções de agentes em `CLAUDE.md` e `AGENTS.md`; parágrafos longos de "situação atual" que repetem o que já está nos artefatos das features. A revisão alcança o repositório inteiro (documentos, índices, links, scripts SQL comentados, JavaDoc), preservando o comportamento e o desempenho da aplicação.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Uma única fonte para cada informação (Priority: P1)

Um integrante do grupo (ou avaliador) procura uma informação (contrato de uma rota, decisão de arquitetura, comando de execução, regra de teste) e a encontra em um único lugar de referência; os demais documentos apenas apontam para ela.

**Why this priority**: Redundância gera divergência, e divergência entre documentos é o principal risco para a entrega acadêmica (README reproduzível, ZIP validado em 19/11).

**Independent Test**: Para uma amostra de temas (rotas, variáveis de ambiente, limites de upload, estados de solicitação, decisões de 12/11/2026), buscar o tema em todo o repositório e confirmar que há exatamente uma definição completa e que as demais ocorrências são referências.

**Acceptance Scenarios**:

1. **Given** um inventário de todos os documentos do repositório, **When** cada tema é comparado entre documentos, **Then** cada tema tem uma fonte de verdade declarada e nenhuma cópia divergente.
2. **Given** um documento que repetia conteúdo de outra fonte, **When** a limpeza é aplicada, **Then** o trecho repetido é substituído por referência relativa válida.
3. **Given** duas fontes com valores conflitantes, **When** são consolidadas, **Then** prevalece o valor verificado no código ou nas regras do `CLAUDE.md`, e o conflito resolvido é registrado em `docs/decisoes.md`.

---

### User Story 2 - Formato e estrutura padronizados (Priority: P2)

Todos os documentos seguem as mesmas convenções de idioma, títulos, datas, nomes de arquivo, termos de domínio e cabeçalho (propósito, situação, fonte de verdade), para que o grupo leia e mantenha sem reaprender o formato a cada arquivo.

**Why this priority**: Facilita revisão, defesa oral e manutenção, mas depende da consolidação da Story 1.

**Independent Test**: Aplicar uma lista de verificação de convenções a todos os documentos mantidos e obter zero desvios.

**Acceptance Scenarios**:

1. **Given** o conjunto de documentos, **When** a lista de convenções é aplicada, **Then** todos usam português-BR, formato único de data, nomenclatura única de arquivos e glossário único de termos.
2. **Given** um documento sem cabeçalho padrão, **When** é normalizado, **Then** passa a declarar propósito e relação com a fonte de verdade.
3. **Given** os documentos de índice (`docs/README.md`, `docs/api/README.md`, `mobile/design/INDEX.md`), **When** a limpeza termina, **Then** todo documento existente aparece em um índice e nenhum índice cita arquivo inexistente.

---

### User Story 3 - Links e referências íntegros (Priority: P2)

Qualquer link relativo, caminho citado, ID de história (RC-XX) ou referência cruzada entre documentos, especificações, scripts SQL e código aponta para algo que existe.

**Why this priority**: A entrega exige README reproduzível; links quebrados comprometem a avaliação.

**Independent Test**: Uma verificação de todos os links relativos e caminhos citados retorna zero quebrados.

**Acceptance Scenarios**:

1. **Given** documentos movidos, renomeados ou removidos, **When** a verificação de links roda, **Then** há zero links quebrados.
2. **Given** um documento removido por redundância, **When** outro documento o citava, **Then** a citação foi redirecionada à fonte de verdade.

---

### User Story 4 - Comportamento e desempenho preservados (Priority: P1)

A limpeza não altera o comportamento da API, do mobile nem do simulador, e não degrada o tempo de build, de testes ou de resposta.

**Why this priority**: Restrição explícita do pedido ("sem comprometer a performance").

**Independent Test**: Comparar build, testes e contratos publicados antes e depois.

**Acceptance Scenarios**:

1. **Given** o estado de referência antes da limpeza, **When** o build e os testes automatizados são executados depois, **Then** o resultado é equivalente (mesmos testes aprovados) e o tempo não piora de forma relevante.
2. **Given** alterações em código (JavaDoc, comentários, remoção de código morto comprovado), **When** a limpeza termina, **Then** nenhuma assinatura pública, rota, resposta, consulta SQL ou configuração em tempo de execução mudou.
3. **Given** um trecho cuja remoção não pode ser comprovada como segura, **When** surge dúvida, **Then** ele é mantido e a dúvida é registrada como pendência.

---

### Edge Cases

- Documento redundante, mas exigido por regra do `CLAUDE.md` (ex.: `docs/ambiente.md`, `docs/decisoes.md`): mantém-se e é normalizado, não removido.
- Artefatos do Spec Kit (`specs/`, `.specify/`) e skills em `.agents/` e `.claude/` são gerados pela ferramenta e não podem ser reestruturados de forma que quebre o fluxo.
- Evidências reais (`docs/evidencias/`, resultados de rodada) são registros históricos e não são reescritas; só links e formatação podem ser ajustados.
- `RegulaCheck.pen` e imagens binárias não são editados como texto.
- Duas fontes divergem e nenhuma é verificável no código: registrar a pendência, não escolher arbitrariamente.
- Documento aparentemente duplicado com público diferente (ex.: README raiz e README de módulo): mantém-se cada um com escopo explícito e referências.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O projeto MUST ter um inventário de todos os documentos (Markdown, contratos YAML/JSON, comentários de scripts SQL e JavaDoc), com tipo, público, fonte de verdade e destino (manter, consolidar, remover).
- **FR-002**: Cada tema documentado MUST ter uma única fonte de verdade; os demais documentos MUST apenas referenciá-la por link relativo.
- **FR-003**: Documentos redundantes MUST ser consolidados ou removidos somente após confirmar que nenhum conteúdo único se perde e que nenhuma regra do `CLAUDE.md` exige sua existência.
- **FR-004**: Os documentos MUST seguir convenções uniformes: português-BR, estrutura de cabeçalho, formato de data, nomenclatura de arquivos e glossário de termos de domínio.
- **FR-005**: O conteúdo MUST permanecer coerente com o código implementado e com o `CLAUDE.md`; divergências MUST ser corrigidas na documentação ou registradas como pendência, nunca ocultadas.
- **FR-006**: Todos os links relativos, caminhos citados e IDs RC-XX MUST ser verificados, e os quebrados MUST ser corrigidos.
- **FR-007**: Os índices (`docs/README.md`, `docs/api/README.md`, `mobile/design/INDEX.md` e equivalentes) MUST listar todos os documentos existentes, sem entradas órfãs.
- **FR-008**: A diferença entre implementado, testado e apenas documentado MUST permanecer explícita; a limpeza não pode transformar proposta em afirmação de conclusão.
- **FR-009**: Evidências e resultados de execução reais MUST ser preservados sem reescrita de conteúdo.
- **FR-010**: O código de produção MUST permanecer funcionalmente idêntico; só são permitidos JavaDoc, comentários e remoção de código morto comprovado por busca de referências e testes.
- **FR-011**: O build e os testes automatizados MUST ter resultado equivalente ao do estado de referência, sem piora relevante de tempo.
- **FR-012**: As decisões da limpeza (fontes de verdade, remoções, conflitos resolvidos) MUST ser registradas em `docs/decisoes.md`.
- **FR-013**: Artefatos gerados pelo Spec Kit e a documentação de skills MUST NOT ser reestruturados de modo a quebrar os comandos do fluxo.
- **FR-014**: Nenhuma remoção MUST apagar conteúdo exigido pela entrega (README, SQL, API, evidências, checklist em `entrega/`).

### Key Entities

- **Documento**: arquivo de texto de referência; atributos: caminho, tipo, público, tema, fonte de verdade, destino da limpeza.
- **Tema**: assunto único (rota, variável, limite, estado, decisão) que deve ter uma só definição completa.
- **Fonte de verdade**: o documento (ou código) que define um tema.
- **Referência**: link relativo de um documento a uma fonte de verdade.
- **Inventário**: lista de todos os documentos e seus destinos, registrada como artefato da feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos temas da amostra de conferência (mínimo 15, cobrindo API, ambiente, limites, estados, decisões, testes e design) têm exatamente uma definição completa.
- **SC-002**: Zero links relativos quebrados e zero entradas órfãs nos índices após a limpeza.
- **SC-003**: Zero desvios na lista de convenções (idioma, cabeçalho, data, nomenclatura) em todos os documentos mantidos.
- **SC-004**: O texto duplicado entre documentos cai em pelo menos 20% das linhas redundantes identificadas no inventário inicial, sem perda de conteúdo único.
- **SC-005**: O build e os testes automatizados têm o mesmo resultado antes e depois, e o tempo de ambos não aumenta mais de 5%.
- **SC-006**: Um integrante que siga apenas o README raiz chega a qualquer documento de referência em no máximo 3 links.
- **SC-007**: Zero mudanças em contrato público de API (rotas, formatos, códigos de erro) comparando antes e depois.

## Assumptions

- A limpeza é documental; qualquer alteração de código se limita a JavaDoc, comentários e código morto comprovado.
- O escopo abrange `README.md`, `CLAUDE.md`/`AGENTS.md`, `CONTRATO_API.md`, `CONTRATO_TESTES.md`, `docs/`, `design/`, `mobile/`, `database/`, `simulador-iot/`, `entrega/` e `samples/`. Artefatos de `specs/` anteriores recebem apenas ajuste de links e consistência, sem reescrita de decisões passadas.
- Dados e evidências sintéticos já registrados permanecem como estão.
- O estado de referência de build e testes é medido no início do plano, no mesmo ambiente.
- Mudanças no `CLAUDE.md` se restringem a remover contradições ou duplicações com outros arquivos, mantendo suas regras.
- Não se introduz novo gerador de documentação, ferramenta de lint ou dependência; a verificação de links pode usar scripts locais descartáveis.
- Pendência: o backlog não traz um ID RC-XX para esta feature; a rastreabilidade usa `025-normalizar-documentacao` até o grupo atribuir um.
