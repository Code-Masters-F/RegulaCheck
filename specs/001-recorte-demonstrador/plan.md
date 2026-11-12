# Implementation Plan: Recorte e critérios do demonstrador (RC-01)

**Branch**: `main` (sem branch dedicada) | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-recorte-demonstrador/spec.md`

## Summary

RC-01 é uma entrega **somente documental**: fixar localidade, especialidade e versão do protocolo acadêmico fictício, o catálogo de até 6 regras, a semântica dos resultados (atende, pendente, não verificável, não aplicável) e sua composição no resultado geral, e registrar tecnologias e limitações. Abordagem: dois documentos em `docs/` (`protocolo-academico.md` e `escopo-e-limitacoes.md`), ligação em `docs/decisoes.md` e no `README.md`, e verificação por revisão cruzada com CLAUDE.md, constituição e decisões, mais um exercício de aplicação das regras em papel por duas pessoas. Nenhum código, banco ou tela.

## Technical Context

**Language/Version**: N/A (documentação em Markdown, português-BR). A stack do produto (Java sem Spring/JPA, Servlet/Jetty ou Tomcat, JDBC, Oracle) é apenas registrada, não implementada aqui.

**Primary Dependencies**: Nenhuma.

**Storage**: N/A (arquivos Markdown versionados no repositório).

**Testing**: Verificação documental: (a) checklist de completude dos atributos das regras; (b) aplicação em papel de 5 casos sintéticos por duas pessoas; (c) revisão cruzada de coerência (`grep` de termos proibidos e divergências). Sem testes automatizados.

**Target Platform**: Repositório Git (GitHub Code-Masters-F); leitores: equipe, avaliador.

**Project Type**: Documentação (insumo para o motor de regras, o modelo de dados e os casos de teste das próximas histórias).

**Performance Goals**: N/A. Critério de leitura: localizar recorte e regras em menos de 2 minutos (SC-003).

**Constraints**: Dados sintéticos e protocolo fictício; sem diagnóstico, urgência ou autorização; não alterar regras alheias de CLAUDE.md; prazo do backlog 12/11/2026.

**Scale/Scope**: 1 localidade, 1 especialidade, 1 versão de protocolo, ≤ 6 regras, 2 documentos novos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: localidade e protocolo fictícios, identificados como tal; sem promessa de fila ou aprovação do SUS. |
| II. Limites clínicos e integridade documental | Atende: mensagens sem diagnóstico/urgência; "ilegível" ≠ "ausente"; informação clínica ausente cabe à unidade emissora. |
| III. Backend Java em monólito modular | Não aplicável (sem código); registro de tecnologias reproduz a decisão sem alterá-la. |
| IV. Segurança e autorização | Não aplicável; documentos não contêm segredos nem dados reais. |
| V. Regras determinísticas, IA subordinada | Atende: regras com condição e resultado objetivos para um motor determinístico; IA mockada só explica. |
| VI. Persistência e revisões | Não aplicável; o modelo de dados será tratado em RC-03. |
| VII. Verificação honesta | Atende: sem alegar execução; relatório distinguirá documentado, verificado e pendente. |
| VIII. Código documentado, mudanças mínimas | Atende: só documentação; atualiza README/decisões afetados. |
| Fluxo SDD | Atende: specify → plan; seguem tasks → analyze → implement. |

**Resultado**: sem violações; Complexity Tracking não se aplica. Reavaliação pós-design: mantém-se sem violações.

## Project Structure

### Documentation (this feature)

```text
specs/001-recorte-demonstrador/
├── plan.md                         # Este arquivo
├── research.md                     # Fase 0
├── data-model.md                   # Fase 1: entidades conceituais do protocolo
├── quickstart.md                   # Fase 1: como verificar a entrega
├── contracts/
│   └── catalogo-regras.md          # Fase 1: formato do catálogo e tabela de decisão
├── checklists/requirements.md      # Qualidade da spec (specify)
└── tasks.md                        # Fase 2 (/speckit-tasks; não criado aqui)
```

### Source Code (repository root)

Sem código. Arquivos alterados/criados na implementação:

```text
docs/
├── protocolo-academico.md          # NOVO: recorte, protocolo v0.1, regras, resultados, resultado geral
├── escopo-e-limitacoes.md          # NOVO: tecnologias (status), limitações, implementado/simulado/pendente/fora
└── decisoes.md                     # ALTERADO: referência aos dois documentos e decisão do recorte
README.md                           # ALTERADO: link para os documentos
```

**Structure Decision**: documentos separados por assunto (protocolo versus escopo/limitações) para que RC-02/RC-03/RC-12 consumam o protocolo sem carregar o texto de limitações. `docs/decisoes.md` continua sendo o registro de decisões e apenas referencia os novos arquivos, sem duplicar conteúdo.

## Complexity Tracking

Sem violações a justificar.
