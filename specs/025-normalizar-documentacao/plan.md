# Implementation Plan: Normalizar documentação e eliminar redundâncias

**Branch**: `025-normalizar-documentacao` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/025-normalizar-documentacao/spec.md`

## Summary

Inventariar todos os documentos rastreados do repositório, declarar uma fonte de verdade por tema, consolidar ou remover sobreposições comprovadas, aplicar convenções uniformes, corrigir links e índices e provar, com build e testes antes/depois, que nenhum comportamento de produção mudou. Abordagem: trabalho em ondas por área (raiz → docs → api → testes → design/mobile → database/samples), cada onda com verificação de links; código só recebe JavaDoc/comentários e remoção de código morto comprovado.

## Technical Context

**Language/Version**: Markdown, YAML/JSON de contrato, SQL (comentários); Java 21 apenas para JavaDoc/verificação de build (sem mudança funcional)

**Primary Dependencies**: nenhuma nova; scripts locais descartáveis (PowerShell/Bash) para inventário e verificação de links, fora do repositório (scratchpad)

**Storage**: N/A (nenhuma alteração de esquema nem de scripts SQL executáveis; só comentários de cabeçalho quando necessário)

**Testing**: `.\mvnw.cmd clean verify` em `backend/` antes e depois (baseline na Fase 0 da implementação); verificador de links relativos; comparação de `docs/api/openapi.yaml` antes/depois

**Target Platform**: repositório Git no Windows/Unix; documentação lida em GitHub/IDE

**Project Type**: monólito Java + mobile + simulador (documentação transversal)

**Performance Goals**: tempo de build e de testes sem aumento superior a 5% (SC-005); nenhuma mudança em código executável

**Constraints**: preservar evidências reais, artefatos Spec Kit e regras do `CLAUDE.md`; sem commits automáticos; `.pen` e binários intocados

**Scale/Scope**: 61 arquivos de documentação rastreados fora de `specs/`, `.agents/`, `.claude/`, `.specify/` e `backend/src` (cerca de 690 KB de Markdown, com `docs/testes/plano-de-testes.md` de 112 KB e `CLAUDE.md` de 33 KB); 204 arquivos em `specs/` tratados só quanto a links

## Constitution Check

*GATE: avaliado contra `.specify/memory/constitution.md`.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Passa: nenhum dado ou texto é alterado de modo a sugerir exigência oficial ou fila real. Linguagem de limitações é preservada |
| II. Limites clínicos e integridade documental | Passa: avisos sobre revisão pela unidade emissora e ausência de diagnóstico permanecem em sua fonte única e são referenciados |
| III. Backend Java em monólito modular | Passa: sem mudança de arquitetura; só JavaDoc |
| IV. Segurança e autorização | Passa: `docs/api/autorizacao.md` e `protecoes.md` mantidos como fonte de verdade |
| V. Regras determinísticas, IA subordinada | Passa: identificação de IA/OCR simulados mantida em todos os pontos onde aparece (consolidar sem retirar o aviso) |
| VI. Persistência e revisões | Passa: sem mudança |
| VII. Verificação honesta | Passa com atenção: distinção implementado/testado/documentado é requisito (FR-008); resultados reais não reescritos |
| VIII. Código documentado e mudanças mínimas | Passa: mudanças pequenas por onda, sem refatoração |

Sem violações; Complexity Tracking vazio. Reavaliado após o desenho: sem alteração.

## Project Structure

### Documentation (this feature)

```text
specs/025-normalizar-documentacao/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── convencoes-documentais.md
│   └── verificacao.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks
```

### Source Code (repository root)

Arquivos potencialmente afetados (nenhum diretório novo de produção):

```text
README.md  CLAUDE.md  AGENTS.md  CONTRATO_API.md  CONTRATO_TESTES.md
docs/            # README, ambiente, arquitetura, decisoes, escopo-e-limitacoes, modelo-de-dados, protocolo-academico
docs/api/        # README, autorizacao, contratos-dados, erros, estados-e-revisoes, protecoes, openapi.yaml, postman
docs/testes/     # README, plano-de-testes, resultado-da-rodada
docs/evidencias/ # somente links/formatação
design/  mobile/  mobile/design/
database/README.md  database/scripts/README.md  database/scripts/*.sql  # só comentários
samples/  simulador-iot/README.md  backend/README.md  entrega/README.md
backend/src/main/java/**   # só JavaDoc/comentários e código morto comprovado
```

**Structure Decision**: sem estrutura nova; os únicos artefatos adicionais são `inventario.md` e `verificacao-resultados.md`, criados na pasta da feature durante a implementação. O mapa de fonte de verdade por tema fica somente em [research.md](research.md) (D1).

## Fases de execução

1. **Baseline**: build e testes (`clean verify`) com tempo (3 execuções, mediana e ruído observado); hash do `openapi.yaml` e da coleção Postman; contagem de links quebrados e de linhas redundantes.
2. **Inventário e mapa de temas** (FR-001, FR-002): classificar cada documento e declarar fonte de verdade.
3. **Ondas de limpeza** (FR-003 a FR-007): raiz; docs; api; testes; design/mobile; database/samples/entrega.
4. **Código** (FR-010): JavaDoc/comentários e código morto só com prova (busca de referências + testes).
5. **Registro** (FR-012): `docs/decisoes.md` e índices.
6. **Verificação final** (SC-001 a SC-007): links, convenções, amostra de temas, build/testes, comparação de contrato.

## Complexity Tracking

Sem violações a justificar.
