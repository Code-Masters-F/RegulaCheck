# Research: Normalizar documentação e eliminar redundâncias

Sem `NEEDS CLARIFICATION` pendente. Decisões de abordagem abaixo; os candidatos de redundância vêm do inventário inicial de arquivos rastreados e devem ser confirmados lendo o conteúdo na implementação.

## D1 — Fonte de verdade por tema (mapa inicial)

| Tema | Fonte de verdade proposta | Candidatos a virar referência |
|---|---|---|
| Rotas e contrato HTTP | `docs/api/openapi.yaml` + `docs/api/README.md` | `CONTRATO_API.md` (parágrafo de situação por RC), `specs/*/contracts/` (histórico da feature, mantido) |
| Formatos de dados, erros, estados | `docs/api/contratos-dados.md`, `erros.md`, `estados-e-revisoes.md` | trechos repetidos em `docs/arquitetura.md` e `CONTRATO_API.md` |
| Autorização e proteções | `docs/api/autorizacao.md`, `protecoes.md` | `CLAUDE.md` §9 (regra), `docs/arquitetura.md` |
| Cenários de teste | `docs/testes/plano-de-testes.md` | `CONTRATO_TESTES.md` (tabela mínima T01 a T16) |
| Resultados reais de teste | `docs/testes/resultado-da-rodada.md`, `docs/evidencias/` | nenhum (preservar) |
| Ambiente e comandos | `docs/ambiente.md` | `README.md`, `backend/README.md`, `mobile/README.md`, `simulador-iot/README.md` |
| Decisões | `docs/decisoes.md` | `CLAUDE.md` (decisões de 12/11/2026), `docs/escopo-e-limitacoes.md` |
| Modelo de dados | `docs/modelo-de-dados.md` + `database/scripts/` | `database/README.md` vs `database/scripts/README.md` |
| Design e telas | `mobile/design/*` e `design/README.md` | `AGENTS.md` (seção de design), `design/verificacao-2026-11-13.md` (histórico) |
| Instruções de agentes | `CLAUDE.md` (Claude) e `AGENTS.md` (Codex) | duplicação entre ambos reduzida a referência cruzada |

**Rationale**: o código e o OpenAPI são verificáveis; documentos narrativos referenciam, não repetem.
**Alternatives**: gerar contratos a partir do código (rejeitado: nova ferramenta, fora do escopo).

## D2 — Remover, consolidar ou manter

**Decision**: critério em três passos: (1) conteúdo único? se sim, migrar para a fonte; (2) alguém exige o arquivo (CLAUDE.md, constituição, entrega)? se sim, manter e reduzir a referência; (3) só então remover, com redirecionamento dos links.
**Alternatives**: remover primeiro e corrigir links depois (rejeitado: risco de perda).

## D3 — Convenções

**Decision**: português-BR; título H1 + linha de propósito + fonte de verdade quando for derivado; datas `DD/MM/AAAA` em prosa e `AAAA-MM-DD` em nomes de arquivo (padrão já usado em `docs/evidencias/`); nomes de arquivo em minúsculas com hífen (exceções existentes: `README.md`, `INDEX.md`, `CLAUDE.md`, `AGENTS.md`, `CONTRATO_*.md` na raiz, avaliados caso a caso); links relativos; status explícito (implementado, testado, documentado, planejado). Detalhe em [contracts/convencoes-documentais.md](contracts/convencoes-documentais.md).
**Alternatives**: renomear em massa (rejeitado: quebra links e histórico sem ganho).

## D4 — Verificação de links e duplicação

**Decision**: script local descartável (scratchpad) que extrai links relativos e caminhos citados e confere existência; duplicação medida por parágrafos de ≥ 3 linhas repetidos entre arquivos. Nada é adicionado ao repositório além do resultado registrado.
**Alternatives**: ferramenta de lint em CI (rejeitado: nova dependência, `CLAUDE.md` §1 item 7).

## D5 — Garantia de desempenho

**Decision**: o build/teste de baseline e o final usam o mesmo comando e ambiente; alteração de código limitada a comentários, logo sem impacto em execução. Tempo medido por execução fria e quente quando viável; variações dentro do ruído são registradas como tal, sem afirmar melhora.
**Alternatives**: benchmark de endpoints (rejeitado: nada de execução muda; seria evidência sem relação causal).

## D6 — Itens protegidos

**Decision**: não editar `specs/*` históricos além de links; não reescrever `docs/evidencias/`; não tocar `.pen`, `.agents/`, `.claude/`, `.specify/` (exceto o ponteiro do plano ativo em `CLAUDE.md`); `CLAUDE.md` só ganha remoção de contradição/duplicação comprovada.

## Riscos

- `plano-de-testes.md` (112 KB) é o maior alvo; reduzir sem perder casos (94 casos nunca podem diminuir).
- `CONTRATO_API.md` tem parágrafo único longo de situação por RC; consolidar exige conferir cada rota com o `openapi.yaml`.
- Desatualização relativa ao código: divergência encontrada vira pendência, não decisão silenciosa.
