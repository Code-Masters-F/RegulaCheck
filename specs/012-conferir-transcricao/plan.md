# Implementation Plan: Conferir a transcrição antes da validação (RC-11)

**Branch**: `012-conferir-transcricao` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-conferir-transcricao/spec.md`

## Summary

A paciente confere a leitura (simulada, RC-10) dos documentos do pedido: vê o que foi lido com a origem, o que não pôde ser verificado, e **confirma** ou **marca divergência** campo a campo. Duas rotas novas, privadas e só para `PACIENTE`, sob a leitura (como o `CONTRATO_API.md` já previa):

| Rota | Efeito |
|---|---|
| `GET /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` | mostra a conferência da leitura: campos, origem, destaques, decisões, situação |
| `PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` | registra decisões (confirmar ou divergir) de um ou mais campos, de uma vez só |

Abordagem:

1. **A paciente nunca digita valor.** O corpo traz só `documentoId`, `codigo`, `decisao` (`CONFIRMADA` ou `DIVERGENTE`) e, na divergência, um `motivo` de lista fechada. Chave desconhecida em qualquer item (por exemplo `valor`) é recusada (400) (D1).
2. **A decisão só vale para a leitura atual**, presa à revisão (D2). Ela **não cria revisão** e **não muda o estado do pedido** (D3, ajuste à spec): a conferência é uma anotação da leitura, e a situação dela é calculada.
3. **Situação calculada, não gravada**: `PENDENTE`, `REQUER_CONFERENCIA`, `CONFERIDA` (ou `DESATUALIZADA` quando a leitura envelheceu) a partir da leitura e das decisões, por uma regra pura e determinística (D4). `liberadoParaSubmissao` é **sempre `false`** (D5).
4. **Esquema novo (script `09_conferencia.sql`)**: `RC_CONFERENCIA_CAMPO`, uma decisão vigente por campo (PK + FK composta para `RC_EXTRACAO_CAMPO`), `CHECK`s de decisão/motivo. Só roda no Oracle com autorização (D6).
5. **Transação curta sob `SELECT … FOR UPDATE` do pedido** (o mesmo bloqueio de RC-07 a RC-10): confere dona, estado, leitura atual e cada campo, e grava todas as decisões ou nenhuma (D7).
6. **Orientações fixas** por situação do campo, sem diagnóstico, urgência ou promessa; informação ausente → unidade emissora; erro de leitura → arquivo mais nítido (D8).
7. **Documentação**: `CONTRATO_API.md`, `docs/api/*`, OpenAPI, Postman, modelo de dados, arquitetura, decisões, `database/scripts/README`.

Fora desta história: correção de valores pela paciente (decisão da spec), regras e resultado geral (RC-12), persistência de análises (RC-13), IA (RC-14), reenvio guiado (RC-16), submissão (RC-17), telas.

### Ajustes à spec feitos no plano

A leitura do estado do sistema mudou três pontos (a spec foi corrigida junto):

- **Campo ilegível não recebe decisão.** Confirmar "a leitura confere" de um campo sem valor não tem sentido; ele só se resolve com um arquivo mais nítido (nova leitura). Tentar decidir sobre ele é 422 `DECISAO_NAO_APLICAVEL` (FR-006, FR-013).
- **Esta história não escreve o estado do pedido** (FR-014, SC-004, US4). O fluxo documentado de estados só chega a `REQUER_CONFERENCIA` a partir de `PROCESSANDO` (análise, RC-12/RC-13), e o `POST …/documentos` (RC-08) só aceita pedido em `RASCUNHO`: pôr o pedido em `REQUER_CONFERENCIA` agora o impediria de receber documentos e de ser excluído, sem nenhuma rota que o devolvesse a `RASCUNHO`. A **situação da conferência** é calculada e exposta; quem a usa para mudar o estado é a RC-12/RC-13.
- **Registrar decisão não cria revisão.** O motivo `CONFERENCIA_ALTERADA` existe no esquema, mas criar revisão tornaria desatualizada a própria leitura conferida (a leitura é "atual" só se a revisão do pedido for a dela). Fica para a RC-12/RC-13 decidir como uma análise captura a conferência (a resposta traz `atualizadaEm` para isso).

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona).

**Primary Dependencies**: nenhuma nova (Jetty 12.1 EE10, HikariCP, driver Oracle, Jackson, SLF4J, JUnit).

**Storage**: Oracle por JDBC (nova `RC_CONFERENCIA_CAMPO`; `RC_EXTRACAO*`, `RC_SOLICITACAO` lidas). **Mudança de esquema: `09_conferencia.sql`** (e `01_` atualizado).

**Testing**: JUnit; Jetty real em porta livre com repositórios em memória e área de arquivos temporária (`CenarioDocumento`, `CenarioExtracao`, `LeitorSimulado` com as amostras); dobro `ConferenciaRepositoryMemoria`; concorrência em memória; teste **opt-in** contra o Oracle (`RC_TESTE_ORACLE=SIM`).

**Target Platform**: JVM 21+ em Windows/Linux. **Project Type**: web service (monólito modular, módulo Maven único).

**Performance Goals**: consultar e registrar em menos de 2 s com o Oracle da faculdade (até 18 campos); sem requisito de vazão.

**Constraints**: pool de 3 conexões (transação curta, sem chamada externa); sem valor extraído, trecho, nome de arquivo, hash ou caminho em log ou erro; conteúdo do documento é dado, nunca instrução; corpo JSON ≤ 4 KB (limite de `CorpoJson`); no máximo 18 decisões por requisição (3 documentos × 6 campos).

**Scale/Scope**: ~10 classes novas de produção (rotas, serviço, repositório, regra de situação, DTOs), ~4 alteradas (`Composicao`, `Dependencias`/`Servidor`, `SolicitacaoController`, `ExtracaoRepository` ganha `buscarPorId`, `SolicitacaoRepositoryJdbc.excluirRascunho`), 1 script SQL, 2 rotas, ~9 classes de teste novas. Dados 100% sintéticos.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: documentos e valores sintéticos; leitura marcada como simulada. |
| II. Limites clínicos | Atende: a paciente não altera nem completa o documento clínico original; ausência → unidade emissora; nenhuma orientação diagnostica, define urgência ou promete atendimento. |
| III. Monólito modular, Servlet/JDBC | Atende: Controller → Service → Repository, SQL parametrizado, regra de situação pura (sem HTTP/JDBC), sem dependência nova; pacote `extracao` (já descrito no `CLAUDE.md` como "extração e conferência da transcrição"). |
| IV. Segurança e autorização por recurso | Atende: dona pela sessão; 404 idêntico para inexistente, alheio, sem leitura, leitura de outro pedido e documento fora da leitura; administrador e simulador recusados; nenhum identificador de dono no corpo. |
| V. Motor determinístico / IA / dados não confiáveis | Atende: situação calculada por regra determinística; valores extraídos são dado, nunca preenchem decisão; nenhuma IA; não decide aprovação, regra nem estado do pedido. |
| VI. Persistência, revisões e confiabilidade | Atende, **com um desvio justificado**: decisão presa à leitura e à revisão capturada, gravada sob `FOR UPDATE` (sem checagem ingênua); uma vigente por campo (PK); tudo ou nada; resposta só depois do `COMMIT`. **Desvio:** a decisão não incrementa a revisão do pedido (a constituição e `estados-e-revisoes.md` citam "conferência muda" como motivo de revisão); ver "Complexity Tracking". |
| VII. Verificação honesta | Atende com **limitação declarada**: leitura simulada; o dobro em memória prova a lógica, não o bloqueio de linha do Oracle. |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nos casos de uso e na regra de situação; contratos e estados documentados; sem refatoração ampla. |

**Gate**: passa **com o desvio do princípio VI justificado em "Complexity Tracking"** (a constituição exige a justificativa no plano). **Reavaliação pós-design**: sem mudança.

### Pontos que merecem revisão

- **Esquema novo em banco compartilhado**: aditivo (só `CREATE TABLE`, prefixo `RC_`); aplicar o `09_` exige autorização específica.
- O estado do pedido **não** reflete a conferência até a RC-12/RC-13; o `estados-e-revisoes.md` registra a nota.
- A exclusão de rascunho (RC-09) e a lista de tabelas apagadas passam a incluir `RC_CONFERENCIA_CAMPO`.

## Project Structure

### Documentation (this feature)

```text
specs/012-conferir-transcricao/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api-conferencia.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/extracao/
├── controller/ConferenciaController.java
├── service/ConferenciaService.java
├── service/SituacaoConferencia.java        # regra pura: PENDENTE / REQUER_CONFERENCIA / CONFERIDA
├── repository/ConferenciaRepository.java / ConferenciaRepositoryJdbc.java / ResultadoRegistroDecisao.java
├── repository/ExtracaoRepository.java     # + buscarPorId
├── entity/DecisaoConferencia.java / TipoDecisao.java / MotivoDivergencia.java
└── dto/ConferenciaRequest.java, ConferenciaResponse.java (+ CampoConferidoResponse, DocumentoConferidoResponse)

backend/src/main/java/br/com/regulacheck/
├── config/Composicao.java, Dependencias.java, Servidor.java   # liga conferência; Dependencias ganha `conferencias`
└── solicitacao/controller/SolicitacaoController.java          # entrega …/extracoes/{id}/conferencia

database/scripts/09_conferencia.sql, 01_criar_esquema.sql, 04_testes_restricoes.sql, 90_limpeza_DESTRUTIVA.sql, README.md

backend/src/test/java/br/com/regulacheck/extracao/
  SituacaoConferenciaTest, ConferenciaRequestTest, ConsultarConferenciaEndpointTest, RegistrarDecisaoEndpointTest,
  OrientacaoSemInformacaoNovaTest, SituacaoEEstadoDoPedidoTest, ConferenciaDesatualizadaTest,
  ConcorrenciaConferenciaTest, MatrizPerfisConferenciaTest, HigieneDeLogConferenciaTest, ConferenciaOracleTest (opt-in)
  (dobros: ConferenciaRepositoryMemoria; ExtracaoRepositoryMemoria ganha buscarPorId)

docs/ e CONTRATO_API.md, docs/api/{README,contratos-dados,erros,autorizacao,estados-e-revisoes,openapi.yaml,Postman}
```

**Structure Decision**: o mesmo monólito modular. A conferência mora no pacote `extracao`, que o `CLAUDE.md` define como "extração e conferência da transcrição". Como a RC-05 só aceita um registro por caminho, o `SolicitacaoController` entrega o subcaminho `…/extracoes/{extracaoId}/conferencia` ao `ConferenciaController`, como já faz com `documentos` e `extracoes`.

## Complexity Tracking

| Desvio | Por que é necessário | Alternativa mais simples rejeitada / escolhida |
|---|---|---|
| Registrar decisão de conferência **não** cria a revisão `CONFERENCIA_ALTERADA` (princípio VI e `estados-e-revisoes.md`: "conferência muda" incrementa a revisão) | Hoje a leitura é "atual" só se a revisão dela for a do pedido (RC-10); criar revisão a cada decisão tornaria desatualizada a própria leitura conferida e exigiria nova leitura a cada clique. A decisão é uma **anotação da leitura** (não muda documentos, serviço nem o que foi lido) e a leitura original fica intacta. Nenhum resultado que dependa dela existe ainda (a análise é da RC-12/RC-13). | Rejeitada: criar a revisão e redefinir "atual" da leitura como "nenhuma revisão que altere documentos ou serviço veio depois dela" (cumpre a letra da constituição, mas altera o cálculo da RC-10 e a numeração de revisões). **Compensação:** a resposta traz `atualizadaEm`, e a RC-12/RC-13 **MUST** capturar a conferência junto com a revisão ao publicar uma análise e invalidá-la se `atualizadaEm` mudar; `estados-e-revisoes.md` e `decisoes.md` registram a regra para essa história. Se o grupo preferir a letra da constituição, a alternativa rejeitada pode ser feita depois como mudança aditiva. |

Nenhuma interface nova além do repositório (fronteira com o banco, como nas histórias anteriores).
