# Implementation Plan: Persistir análises e histórico por revisão (RC-13)

**Branch**: `21-rc-13-persistir-análises-e-histórico-por-revisão` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-persistir-analises-historico/spec.md`

## Summary

A avaliação da RC-12 passa a ser **pedida pela paciente, gravada e consultável**: `POST /solicitacoes/{id}/analises` faz a análise na própria requisição e devolve a análise gravada (201, ou 200 se a vigente já vale para a versão atual); `GET /solicitacoes/{id}/analises` devolve o histórico com a vigente marcada.

Abordagem (detalhes em [research.md](research.md)):

1. **Três passos curtos** (D1): `iniciar` sob `SELECT … FOR UPDATE` (dona, estado, em andamento, vigente, leitura atual; grava `PROCESSANDO` + `ANALISE_INICIADA_EM`) → `AvaliacaoService.avaliar` sem bloqueio → `publicar` sob bloqueio (grava tudo e muda o estado) ou `encerrar` (descarte/falha).
2. **Nada velho vira vigente** (D2): a publicação confere o token (`ANALISE_INICIADA_EM`), a revisão e o instante da conferência sob o mesmo bloqueio que as escritas de documento, leitura e conferência usam.
3. **Idempotência** (D3): vigente da revisão atual é devolvida; decisões de conferência que mudam algo tiram a vigência; segundo pedido simultâneo → 409 `ANALISE_EM_ANDAMENTO`; restrições únicas como rede de proteção.
4. **Prazo e falhas** (D5): limite de 1 minuto a partir de `ANALISE_INICIADA_EM`; tomada da trava vencida; falha → `FALHA_ANALISE` + 503 `ANALISE_INDISPONIVEL`; descarte → estado da última análise concluída ou `RASCUNHO` (D4).
5. **Esquema aditivo** (data-model): coluna em `RC_SOLICITACAO`, duas em `RC_ANALISE`, uma em `RC_PENDENCIA`, tabela `RC_PENDENCIA_EVIDENCIA`, troca de `RC_ANALISE_UK_REVISAO` por `RC_ANALISE_UK_CONFERENCIA`; script `10_` + `01_` atualizado + casos no `04_` + `90_`.
6. **Pedido** informa `analise.situacao` real (D8), sem depender do `10_`.

Fora desta história: resumo IA simulada (RC-14), tela (RC-15), comparação/reenvio orquestrado (RC-16), submissão (RC-17).

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release` 21).

**Primary Dependencies**: nenhuma nova (Jetty 12.1, HikariCP, ojdbc11, Jackson, SLF4J, JUnit Jupiter).

**Storage**: Oracle por JDBC, SQL parametrizado, instantes em UTC. Escrita em `RC_SOLICITACAO`, `RC_ANALISE`, `RC_RESULTADO_REGRA`, `RC_PENDENCIA`, `RC_PENDENCIA_EVIDENCIA` em transações curtas com `SELECT … FOR UPDATE` do pedido e `ROLLBACK` explícito. Script novo `database/scripts/10_analise_historico.sql`.

**Testing**: JUnit 5. Serviço e rota com Jetty + repositórios em memória (`CenarioDocumento`/`CenarioAuth`, leitor simulado e amostras sintéticas); `RelogioFixo` para o prazo; subclasse de teste do `AvaliacaoService` para provocar alteração durante a avaliação e falhas; 10 threads para concorrência. Oracle opt-in (`AnaliseOracleTest`, `RC_TESTE_ORACLE=SIM`), **não executável neste ambiente** (sem banco).

**Target Platform**: JVM 21+ em Windows/Linux. **Project Type**: web service (monólito modular, um módulo Maven).

**Performance Goals**: uma análise = 3 transações curtas + ~6 consultas de leitura; consulta do histórico = 6 consultas por pedido (independe do número de análises, até 50).

**Constraints**: nenhuma transação aberta durante a avaliação; nenhum valor ou trecho de documento em log ou mensagem; sucesso só depois do `COMMIT`; servlets sem estado de requisição; pool de 3 conexões (cada passo usa e devolve uma).

**Scale/Scope**: ~14 tipos de produção novos (`analise.entity` 6, `analise.repository` 5, `analise.service` 1, `analise.controller` 1, `analise.dto` ~4), ~8 alterados (`Solicitacao`, `SolicitacaoResponse`, `SolicitacaoRepositoryJdbc`, `SolicitacaoController`, `ConferenciaRepositoryJdbc`, `Dependencias`, `Composicao`, `Servidor`); ~6 classes de teste novas, ~6 dobros/cenários ajustados; 5 scripts SQL; docs de API, estados, arquitetura, decisões, modelo de dados, README.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: análise marcada `simulada: true` com aviso; "sem pendências identificadas" não é aprovação; testes só com amostras sintéticas. |
| II. Limites clínicos e integridade documental | Atende: nada é escrito no documento, na leitura ou nas decisões; a análise só registra o que o motor produziu; nenhuma mensagem nova diagnostica. |
| III. Monólito modular, Servlet/JDBC | Atende: `AnaliseController` (HTTP) → `AnaliseService` (caso de uso) → `AnaliseRepository` (JDBC, interface por ser fronteira com o banco); sem dependência nova; motor intocado. |
| IV. Segurança e autorização por recurso | Atende: paciente da sessão; dona aplicada no `FOR UPDATE` e em toda consulta; 404 idêntico; logs só com ids, códigos e contagens. |
| V. Regras determinísticas, IA subordinada | Atende: a análise grava exatamente o resultado do motor; sem IA nesta história (`resumo: null`). |
| VI. Persistência, revisões e confiabilidade | Atende: histórico preservado (só `VIGENTE` 1→0); publicação só com revisão **e** conferência compatíveis, sob bloqueio; retentativa não duplica (vigente devolvida, trava, restrições únicas); sucesso só após `COMMIT`; `PROCESSANDO` ≠ concluído; sem transação durante a avaliação; script aditivo manual. |
| VII. Verificação honesta | Atende com limitação declarada: Oracle não disponível neste ambiente; `AnaliseOracleTest` e casos 28–31 do `04_` ficam **NÃO VERIFICADOS** até alguém do grupo rodar. Dobros em memória não provam o bloqueio do Oracle. |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nos novos tipos; docs de API/estados/modelo/decisões atualizados; a RC-11 ganha só a invalidação da vigente (compensação que ela mesma exigiu). |

**Gate**: passa. Dois pontos justificados em Complexity Tracking. **Reavaliação pós-design**: sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/014-persistir-analises-historico/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api-analises.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
database/scripts/
├── 01_criar_esquema.sql          # alterado: colunas/tabela/restrições da RC-13 para bancos novos
├── 04_testes_restricoes.sql      # alterado: casos 28-31
├── 10_analise_historico.sql      # novo: alteração aditiva para bancos existentes
├── 90_limpeza_DESTRUTIVA.sql     # alterado: RC_PENDENCIA_EVIDENCIA
└── README.md                     # alterado

backend/src/main/java/br/com/regulacheck/
├── analise/entity/Analise, DocumentoDaAnalise, ResultadoGravado, PendenciaGravada, EvidenciaGravada,
│                  HistoricoDeAnalises
├── analise/repository/AnaliseRepository, AnaliseRepositoryJdbc, AnaliseParaGravar, InicioAnalise,
│                      PublicacaoAnalise, Encerramento
├── analise/service/AnaliseService (novo), AvaliacaoService (JavaDoc: agora usado pela RC-13)
├── analise/controller/AnaliseController
├── analise/dto/AnaliseResponse, HistoricoAnalisesResponse
├── solicitacao/entity/Solicitacao (+ resultadoVigente), solicitacao/dto/SolicitacaoResponse (analise calculada),
├── solicitacao/repository/SolicitacaoRepositoryJdbc (subconsulta da vigente),
├── solicitacao/controller/SolicitacaoController (entrega /{id}/analises)
├── extracao/repository/ConferenciaRepositoryJdbc (tira a vigência quando grava decisão)
└── config/Dependencias (+ analises), Composicao (monta AvaliacaoService/AnaliseService), Servidor

backend/src/test/java/br/com/regulacheck/
├── analise/AnaliseRepositoryMemoria, CenarioAnalise, PedirAnaliseEndpointTest, HistoricoAnalisesEndpointTest,
│          AlteracaoDuranteAnaliseTest, ConcorrenciaAnaliseTest, PrazoEFalhaAnaliseTest, MatrizPerfisAnaliseTest,
│          HigieneDeLogAnaliseTest, AnaliseOracleTest (opt-in)
├── auth/CenarioAuth (+ analises), solicitacao/SolicitacaoRepositoryMemoria (ganchos de estado e vigente),
├── documento/DocumentoRepositoryMemoria e extracao/ConferenciaRepositoryMemoria (avisam a invalidação)
└── testes que constroem Dependencias (+ analises)

docs/api/README.md, contratos-dados.md, erros.md, estados-e-revisoes.md, openapi.yaml, Postman;
docs/arquitetura.md, docs/decisoes.md, docs/modelo-de-dados.md, README.md
```

**Structure Decision**: monólito modular existente; o módulo `analise` ganha as camadas que o `CLAUDE.md` §6 previa (controller, service, repository, entity, dto). O motor (`validacao`) não muda.

## Complexity Tracking

| Ponto | Por que é necessário | Alternativa mais simples rejeitada porque |
|---|---|---|
| Remover `RC_ANALISE_UK_REVISAO` (alteração de restrição existente) | A conferência não cria revisão (desvio justificado da RC-11), então uma nova análise depois de completar a conferência é da **mesma** revisão | Criar revisão a cada conferência deixaria a leitura desatualizada (RC-10/11); manter a restrição impediria o fluxo `REQUER_CONFERENCIA` → conferir → analisar de novo |
| `CONFERENCIA_EM` como `TIMESTAMP` sem fuso | O Oracle não aceita `TIMESTAMP WITH TIME ZONE` em restrição única, e a nova restrição precisa da coluna | Índice funcional não verificável aqui; sem restrição, perde-se a garantia no banco |
| `ConferenciaRepositoryJdbc` (RC-11) passa a tirar a vigência | É a compensação que a própria RC-11 registrou como obrigação da RC-12/13 | Calcular a vigência em cada leitura espalharia a regra por RC-13, RC-15 e RC-17 |
