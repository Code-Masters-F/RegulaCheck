# Implementation Plan: Escopo do app Android e protótipo visual na documentação

**Branch**: `027-normalizar-estado-app-desenho` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

## Summary

Registrar em `docs/decisoes.md` o escopo do app (10 telas sobre a API) e o protótipo visual como material de apoio; ajustar as pendências que citavam telas do app; alinhar `mobile/README.md`, `mobile/design/INDEX.md`, `mobile/design/textos.md` e `design/README.md` por link; atualizar o Status das specs do app e do protótipo com PR e evidência.

Artefatos gerados com o Spec Kit (`specify`, `plan`, `tasks`, `analyze`) e renumerados para 027 ao partir da `main` de 19/11/2026, onde o número 025 já estava em uso.

## Technical Context

**Language/Version**: Markdown (só documentação)
**Primary Dependencies**: nenhuma
**Testing**: busca de texto e revisão do diff; sem testes de código (CLAUDE.md, seção 14)
**Constraints**: sem alteração de código, SQL, app, simulador ou `.pen`; `.env` não lido
**Scale/Scope**: 5 documentos e 9 linhas de Status

## Constitution Check

| Princípio | Situação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | OK: nenhum dado novo |
| II. Limites clínicos | OK |
| III. Backend Java | N/A |
| IV. Segurança | OK: nenhum segredo |
| V. Dados não confiáveis | OK |
| VI. Persistência | N/A |
| VII. Verificação honesta | OK: Status só com PR e evidência existentes |
| VIII. Mudanças mínimas | OK: só as linhas afetadas, com links |

O ponteiro do plano no `CLAUDE.md` não é alterado nesta feature, para não disputar a mesma linha com o PR da RC-29.

## Arquivos alterados

```text
docs/decisoes.md
mobile/README.md
mobile/design/INDEX.md
mobile/design/textos.md
design/README.md
specs/{007,015,016,017,019,020-reenviar-versao-corrigida,021,022,024}-*/spec.md   # linha Status
specs/027-normalizar-estado-app-desenho/
```
