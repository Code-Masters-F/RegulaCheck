# Implementation Plan: Submeter solicitação em simulação (RC-17)

**Branch**: `39-rc-17-submeter-solicitacao-em-simulacao` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Especificação da RC-17 em `specs/021-submeter-simulacao/spec.md` (issue #39).

## Summary

Implementar a conclusão da simulação no backend: `POST /solicitacoes/{id}/submissoes-simuladas` registra a submissão simulada (tabela `RC_SUBMISSAO`, já existente) e leva o pedido a `SUBMETIDA_SIMULACAO` numa transação sob o bloqueio do pedido, só se houver análise vigente da revisão atual sem pendência nem não verificável; repetir devolve a mesma submissão; `GET` na mesma rota recupera o comprovante. Referência `SIM-{ano}-{pedido}`; comprovante com destaque e nota fixos de simulação. Recusas como 409 explicando que a restrição é do simulador. Sem script SQL, dependência ou framework novo.

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release`), backend existente.

**Primary Dependencies**: Jakarta Servlet/Jetty, JDBC Oracle, Jackson, SLF4J já declarados. Nada novo.

**Storage**: Oracle via JDBC; `RC_SUBMISSAO` (RC-03) com `UNIQUE (SOLICITACAO_ID)` e `UNIQUE (REFERENCIA)`. Sem mudança de esquema.

**Testing**: JUnit 5 com `ServidorDeTeste` e dobros em memória (`CenarioDocumento`, leitor simulado); utilitários da RC-16 (`CenarioReenvio.analisado`); `SubmissaoOracleTest` opt-in. Build: `sh ./mvnw clean verify` (referência: 731 testes, 0 falhas, 32 ignorados).

**Target Platform**: API Java; tela do comprovante no app Android (épico 6).

**Project Type**: Monólito modular; pacote existente `br.com.regulacheck.submissao` (hoje só `package-info.java`).

**Performance Goals**: Operação síncrona curta (uma transação com poucas consultas).

**Constraints**: Decisão e gravação no mesmo bloqueio do pedido (constituição VI); idempotente por pedido; nenhuma alegação de aprovação, fila, vaga ou protocolo oficial (I); dona pela sessão e 404 idêntico (IV); logs só com ids e desfechos (IV, VIII).

**Scale/Scope**: Uma submissão por pedido. Fora: telas, indicadores (RC-20), download (RC-34), cancelar/reabrir.

## Constitution Check

*GATE: verificado antes da pesquisa e novamente após o desenho.*

| Princípio | Conformidade |
|---|---|
| I. Escopo acadêmico | Referência com prefixo `SIM`, `simulada: true`, destaque e nota fixos; nada de fila, vaga, prazo, aprovação ou protocolo oficial (teste de termos). |
| II. Limites clínicos | A recusa diz que a restrição é do simulador e não impede atendimento; nenhum dado clínico é criado ou alterado. |
| III. Monólito modular | Controller → Service → Repository no pacote `submissao`; entidade separada do DTO; sem framework. Interface de repositório por ser fronteira com o banco (JDBC e memória). |
| IV. Autorização | Rota em `/solicitacoes/*` (`PACIENTE`); dona aplicada na própria consulta SQL (`PACIENTE_ID = ?`); 404 idêntico também para "não submetido". |
| V. Regras determinísticas | A completude é lida da análise determinística vigente; IA/resumo não influem. |
| VI. Revisões e confiabilidade | Vigência, revisão, estado e gravação conferidos sob `SELECT … FOR UPDATE`; `UNIQUE (SOLICITACAO_ID)` como rede; sucesso só depois do `COMMIT`; repetição devolve a existente. |
| VII. Verificação honesta | Testes de idempotência, 10 simultâneos, revisão desatualizada, autorização entre contas; Oracle opt-in registrado como não verificado se não executado. |
| VIII. Documentação e mudanças mínimas | JavaDoc pt-BR; contratos, OpenAPI, erros, estados, autorização, Postman, `CONTRATO_API.md`, decisões e arquitetura atualizados. |

**Gate inicial**: aprovado. **Gate após Phase 1**: aprovado; nenhuma violação.

## Project Structure

### Documentation (this feature)

```text
specs/021-submeter-simulacao/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/api-submissao.md
├── quickstart.md
└── tasks.md            # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── submissao/
│   ├── entity/Submissao.java                    # NOVO: submissão lida com revisão e protocolo da análise
│   ├── repository/SubmissaoRepository.java      # NOVO: fronteira com o banco
│   ├── repository/SubmissaoRepositoryJdbc.java  # NOVO: transação sob o bloqueio do pedido
│   ├── repository/ResultadoSubmissao.java       # NOVO: desfecho da conclusão
│   ├── service/SubmissaoService.java            # NOVO: casos de uso, referência e recusas
│   ├── dto/SubmissaoResponse.java               # NOVO: comprovante
│   └── controller/SubmissaoController.java      # NOVO: POST/GET …/submissoes-simuladas
├── solicitacao/controller/SolicitacaoController.java  # entrega o subcaminho
├── config/Dependencias.java                     # + submissoes
├── config/Servidor.java                         # + SubmissaoRepositoryJdbc
└── config/Composicao.java                       # monta serviço e controller

backend/src/test/java/br/com/regulacheck/
├── submissao/ (SubmissaoRepositoryMemoria, ReferenciaSimuladaTest, SubmeterEndpointTest,
│               RecusasDaSubmissaoTest, MatrizPerfisSubmissaoTest, HigieneDeLogSubmissaoTest,
│               SubmissaoOracleTest)             # NOVOS
├── auth/CenarioAuth.java                        # + submissoes nas dependências em memória
└── (5 testes que montam Dependencias à mão)    # + o novo repositório

docs/api/{contratos-dados.md, openapi.yaml, erros.md, estados-e-revisoes.md, autorizacao.md,
          RegulaCheck.postman_collection.json, README.md}
CONTRATO_API.md, README.md, docs/{decisoes.md, arquitetura.md, ambiente.md}
```

**Structure Decision**: Backend existente, pacote `submissao` já reservado pela fundação. Nenhuma mudança em `database/scripts/` nem em `mobile/`.

## Complexity Tracking

Sem violações da constituição a justificar.
