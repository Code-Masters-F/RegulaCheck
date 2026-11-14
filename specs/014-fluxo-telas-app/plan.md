# Implementation Plan: Fluxo de telas e informação de cada tela do aplicativo

**Branch**: `codex/007-design-system-exploracao` (feature `014-fluxo-telas-app`, sem branch própria) | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-fluxo-telas-app/spec.md`

## Summary

Desenhar no pen.dev o percurso completo do aplicativo — paciente e administrador — **acompanhando o backend**: cada ação desenhada usa uma operação existente (RC-04, RC-07 a RC-11) ou contratada (RC-13 a RC-17, RC-20 a RC-22), e cada informação vem de um campo do backend ou de texto fixo. A abordagem tem quatro peças: (1) um mapa tela × operação com o tratamento de cada resposta ([contracts/telas-operacoes.md](contracts/telas-operacoes.md)); (2) um dicionário de rótulos e a origem de cada dado ([data-model.md](data-model.md)); (3) um inventário fechado de **94 quadros** em 6 fases, por história RC-36 a RC-39 ([contracts/inventario-quadros.md](contracts/inventario-quadros.md)); (4) uma Fase 0 que completa o design system v1.0 (13 ajustes de componentes) e corrige os três exemplos e os documentos de apoio antes de desenhar. Decisões e alternativas: [research.md](research.md).

## Technical Context

**Language/Version**: não se aplica (design e Markdown).

**Primary Dependencies**: pen.dev com o pencil MCP; design system v1.0 em `design/RegulaCheck.pen` (F1 `IkCIw`, F2 `t9lvSH`, F3 `EVIof`, F4 `HBByt`); fontes Bricolage Grotesque e Nunito Sans; ícones Lucide.

**Storage**: `design/RegulaCheck.pen` (somente via MCP); prévias PNG em `design/previas/`; texto em `specs/014-fluxo-telas-app/`, `mobile/design/textos.md`, `mobile/design/dados-ficticios.md`, `mobile/design/INDEX.md`.

**Testing**: revisão, não teste automatizado — screenshots, consulta de limites (`Get` com `ctx.problems`), busca por componentes `Legado/*`, conferência de rastreabilidade tela × operação, busca de linguagem proibida em `textos.md`, percurso por pessoa que não desenhou. Testes Java não mudam (nenhum código alterado).

**Target Platform**: aplicativo Android em retrato; quadros de referência 390 × 844 (largura útil 342), sem modo escuro.

**Project Type**: design de aplicativo móvel + documentação.

**Performance Goals**: não se aplica. Metas de uso: SC-003 (≥ 10 de 11 ações principais identificadas) e SC-004 (percurso até o comprovante em ≤ 5 min).

**Constraints**: telas acompanham o backend, sem lógica nova; só componentes v1.0; dados sintéticos; selo SIMULADO em leitura, conferência, resumo, comprovante e sensores; aviso acadêmico em toda tela; contraste ≥ 4,5:1 e alvos ≥ 48; sem barra inferior; altura de controle numérica (o pen.dev descarta variável em `height`).

**Scale/Scope**: 94 quadros (3 existentes a ajustar), 13 ajustes de componentes, 7 ajustes de exemplos e documentos, 4 histórias. Marco: percurso P1 (Fases 0–3) até 16/11/2026; P2 e P3 até o congelamento de 17/11.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Como o plano atende | Situação |
|---|---|---|
| I. Escopo acadêmico e dados sintéticos | exemplares fictícios e realistas de `data-model.md` §5 (estrutura da carga do backend, sem dado de pessoa real); aviso acadêmico em toda tela; SIMULADO nas telas de demonstração; "não é aprovação do SUS" no resultado e no comprovante | ✅ |
| II. Limites clínicos e integridade documental | Conferência só confirma ou aponta diferença (D8); nenhuma tela edita conteúdo clínico; situações identificado, não localizado e ilegível distintas; "não localizado não prova ausência" | ✅ |
| III. Backend Java em monólito modular | Sem código; nada muda no backend | ✅ (não se aplica) |
| IV. Segurança e autorização por recurso | Identidade sempre da sessão; 401, 403 e 404 idênticos tratados (FR-010); nenhuma credencial ou token desenhado; administrador sem acesso a pedidos | ✅ |
| V. Regras determinísticas e IA subordinada | Resultado por critério sempre visível; resumo SIMULADO com texto padrão em falha; texto de documento como texto puro | ✅ |
| VI. Revisões e confiabilidade | "Resultado não vigente" depois de troca; nada marcado como resolvido antes do novo resultado; envio só "Enviado" após confirmação; conclusão idempotente | ✅ |
| VII. Verificação honesta | Quadros de operação contratada marcados "contrato provisório"; verificações registradas com resultado real | ✅ |
| VIII. Mudanças mínimas e documentadas | Reaproveita componentes v1.0; ajustes listados e rastreados; docs afetados atualizados na Fase 0 | ✅ |
| Fluxo SDD | specify → plan concluídos; tasks e analyze antes de desenhar | ✅ |

**Reavaliação após o desenho (Fase 1 do plano)**: sem violação. Um desvio de vocabulário foi corrigido no próprio plano (estado `PROCESSANDO` rotulado "Em verificação", ajuste A1).

## Project Structure

### Documentation (this feature)

```text
specs/014-fluxo-telas-app/
├── spec.md                       # requisitos e fluxo (/speckit-specify)
├── plan.md                       # este arquivo
├── research.md                   # decisões D1–D17 com fonte no backend
├── data-model.md                 # convenções, dicionário de rótulos, dados por tela
├── quickstart.md                 # como desenhar e verificar
├── contracts/
│   ├── telas-operacoes.md        # tela × operação × resposta
│   └── inventario-quadros.md     # 94 quadros em 6 fases + ajustes da Fase 0
├── checklists/requirements.md    # qualidade da spec
└── tasks.md                      # /speckit-tasks (ainda não criado)
```

### Design e documentos de apoio (repository root)

```text
design/
├── RegulaCheck.pen               # F1–F4 (y=0), exemplos e uma faixa por história (RC-36 … RC-39)
├── README.md                     # retomada (atualizar: X7)
└── previas/                      # PNG por quadro e por fase
mobile/design/
├── design-system.md              # catálogo v1.0 (atualizar com A1–A13)
├── textos.md                     # textos por tela (atualizar: X5)
├── dados-ficticios.md            # pedidos por número (atualizar: X4)
└── INDEX.md                      # inventário (apontar para esta feature: X7)
specs/007-epico-telas-pendev/contracts/telas.md   # nota de substituição (X6)
```

**Structure Decision**: arquivo único `design/RegulaCheck.pen`, como adotado em 13/11; nenhuma pasta nova fora de `specs/014-fluxo-telas-app/`. O épico 007 continua dono do processo de design; esta feature é dona do fluxo, do conteúdo por tela e do inventário.

## Complexity Tracking

Nenhuma violação da constituição a justificar.
