# Implementation Plan: Plano de testes do demonstrador (RC-24)

**Branch**: `023-plano-de-testes` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Input**: `specs/023-plano-de-testes/spec.md` (revisada com os 16 cenários da issue #50 e com os ajustes do responsável de 18/11/2026).

## Summary

Entregar `docs/testes/plano-de-testes.md`: documento Markdown com os cenários T01..T16 da issue #50 desdobrados em casos `Txx-yy` (um caso por resultado distinto), mais o percurso integrado `P01`, a seção de preparação e execução, a matriz de rastreabilidade e a convenção de evidências em `docs/evidencias/<ID-DO-CASO>/`. É trabalho **documental**: nada é executado, nenhum código muda, e todos os casos nascem "Não executado".

Abordagem: (1) levantar no repositório o contrato real (rotas, códigos, estados, contas, comandos, amostras, gabaritos) de cada cenário; (2) escrever cada caso com resultado esperado apoiado nesse contrato; (3) separar o que é decisão de escopo aprovada (leitura e resumo simulados, decisão de 12/11/2026) do que é funcionalidade pendente (RC-20 e RC-22); (4) conferir comandos e arquivos citados por leitura do repositório; (5) deixar a revisão por outro integrante pendente.

## Technical Context

**Language/Version**: Markdown (documentação). Os casos citam Java 21, Maven Wrapper, Kotlin/Gradle do app e Java source launcher do simulador, já existentes.

**Primary Dependencies**: nenhuma nova. Pré-requisitos documentados em `docs/ambiente.md` (JDK 21, Oracle do grupo, emulador Android, `IOT_API_KEY`, `DOCUMENT_STORAGE_PATH`).

**Storage**: arquivos em `docs/testes/` (plano) e `docs/evidencias/<ID>/` (evidências, criadas só na execução, RC-25). Sem banco, sem script SQL novo.

**Testing**: o plano referencia (a) testes automatizados existentes do backend (sem banco, em memória; Oracle opt-in com `RC_TESTE_ORACLE=SIM`), (b) testes do app (JUnit + MockWebServer, `.\gradlew.bat testDebugUnitTest`), (c) execução manual por HTTP e pelo emulador. Esta feature não roda nenhum deles como caso.

**Target Platform**: API Java (Jetty) local; app Android no emulador; simulador de sensores por `java simulador-iot/SimuladorSensores.java`.

**Project Type**: documentação de qualidade sobre sistema existente (backend + mobile + simulador).

**Performance Goals**: não se aplica (o plano não mede desempenho; o tempo de percurso do app é da RC-26/RC-27).

**Constraints**: dados 100% fictícios; sem credenciais no plano (a senha de demonstração já é pública em `database/scripts/README.md` e é referenciada, não copiada para evidências); nenhum resultado inventado; prazo da issue 18/11/2026 (2 h).

**Scale/Scope**: 16 cenários, cerca de 45 casos e 1 percurso integrado; 15 conjuntos sintéticos da RC-23 como entrada documental.

## Constitution Check

*GATE: passa antes da pesquisa e após o desenho.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Passa: contas `.invalid`, `samples/` sintéticos, avisos de simulação no plano. |
| II. Limites clínicos | Passa: casos de unidade emissora × paciente reproduzem o contrato, sem editar original. |
| III. Backend Java modular | Não se aplica: sem código. |
| IV. Segurança por recurso | Passa: T02, T03, T14 e T16 cobrem isolamento, perfil e credencial. |
| V. Regras determinísticas, IA subordinada, dados não confiáveis | Passa: T07..T12 e conjunto 11 verificam isso; resumo identificado como simulado. |
| VI. Persistência e revisões | Passa: T11 e T13 cobrem revisão, histórico e idempotência. |
| VII. Verificação honesta | Passa e é o núcleo: status inicial "Não executado", sem aprovação sem execução, dublês identificados, "Bloqueado" só após tentativa registrada, cenário não é aprovado por testar parte. |
| VIII. Mudanças mínimas e documentadas | Passa: só documentação; `docs/` e README atualizados. |

Reavaliação após o desenho: sem violação; `Complexity Tracking` vazio.

## Project Structure

### Documentation (this feature)

```text
specs/023-plano-de-testes/
├── plan.md              # este arquivo
├── research.md          # decisões e levantamento do contrato real por cenário
├── data-model.md        # campos do caso, status, matriz, evidência
├── quickstart.md        # como revisar e como a RC-25 deve usar o plano
├── contracts/
│   └── formato-plano-de-testes.md   # estrutura obrigatória do documento e de cada caso
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks (não criado aqui)
```

### Source Code (repository root)

```text
docs/testes/
├── README.md                 # existente; apontar para o plano
└── plano-de-testes.md        # NOVO: entregável da RC-24

docs/evidencias/
└── README.md                 # existente; documentar a convenção <ID-DO-CASO>/ (sem criar pastas de caso)

docs/ambiente.md             # só se faltar link para o plano (ajuste mínimo)
```

**Structure Decision**: um único arquivo de plano, para o leitor não alternar entre documentos; seções fixas (contexto, ambiente, contas, procedimentos de falha controlada, restauração, catálogo de casos, percurso integrado, matriz, convenção de evidências, revisão pendente). Nenhuma pasta de evidência de caso é criada nesta feature (seria sugerir execução).

## Complexity Tracking

Sem violações a justificar.
