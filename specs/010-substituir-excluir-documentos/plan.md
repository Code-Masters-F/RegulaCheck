# Implementation Plan: Substituir documento e excluir rascunho (RC-09)

**Branch**: `010-substituir-excluir-documentos` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-substituir-excluir-documentos/spec.md`

## Summary

A paciente corrige o que enviou (remover ou substituir um documento) e desiste de um rascunho (excluir). Três operações novas, todas privadas e só para `PACIENTE`:

| Rota | Efeito |
|---|---|
| `DELETE /solicitacoes/{id}/documentos/{docId}` | remove o documento (200 com a versão nova) |
| `PUT /solicitacoes/{id}/documentos/{docId}` | substitui por outro arquivo, em uma só operação (201 novo, 200 repetição) |
| `DELETE /solicitacoes/{id}` | exclui o rascunho, exige o cabeçalho `X-Confirmar-Exclusao: EXCLUIR` (204) |

Abordagem, reaproveitando a RC-08 sem mudar seu contrato:

1. **Sem mudança de esquema.** `REVISAO_REMOCAO`, os motivos `DOCUMENTO_REMOVIDO`/`DOCUMENTO_SUBSTITUIDO` e `RC_ANALISE.VIGENTE` já existem. Não há script `08_`.
2. **Remoção lógica + arquivo apagado.** O documento removido fica no banco como histórico (`REVISAO_REMOCAO` preenchida, chave de envio anulada) e some das consultas; o arquivo é apagado depois do `COMMIT` (research D1).
3. **Uma transação por operação, sob `SELECT … FOR UPDATE` do pedido**, o mesmo padrão do envio e da troca de serviço: serializa tudo no pedido e dá numeração de revisão sem salto (D10). Remover e substituir criam **uma** revisão; excluir apaga documentos, revisões e pedido (D5).
4. **Substituição atômica** com a mesma conferência da RC-06: arquivo novo conferido e guardado **antes** da transação; recusa ou falha remove o novo e deixa o antigo intacto (D2). Idempotência pela chave de envio, reconhecida **antes** de procurar o `docId` (D3).
5. **Confirmação de exclusão em cabeçalho** com valor literal; 400 sem apagar nada (D4).
6. **Invalidação da conferência sem iniciar análise**: `UPDATE RC_ANALISE SET VIGENTE = 0`; o estado do pedido não muda (D6).
7. **Pedido submetido é só consulta** (D7).
8. **Documentação e contratos** atualizados na mesma entrega (`CONTRATO_API.md`, `docs/api/*`, OpenAPI, Postman, estados e revisões, modelo de dados, ambiente).

Fora desta história: análise, extração, resumo de IA, submissão, retenção automática, lixeira, telas.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona), como nas histórias anteriores.

**Primary Dependencies**: nenhuma nova (Jetty 12.1 EE10 com multipart, HikariCP, driver Oracle, Jackson, SLF4J, PDFBox, JUnit).

**Storage**: Oracle por JDBC (`RC_SOLICITACAO`, `RC_REVISAO`, `RC_DOCUMENTO`, `RC_ANALISE`) e a área privada de arquivos (`DOCUMENT_STORAGE_PATH`). **Sem mudança de esquema.**

**Testing**: JUnit; Jetty real em porta livre com repositórios em memória e área de arquivos em pasta temporária (`ServidorDeTeste`, `CenarioDocumento`, `DocumentoRepositoryMemoria`, `SolicitacaoRepositoryMemoria`); concorrência em memória; um teste **opt-in** contra o Oracle (`RC_TESTE_ORACLE=SIM`). Os dobros em memória precisam de estados arbitrários do pedido para cobrir `SUBMETIDA_SIMULACAO` e os de análise.

**Target Platform**: JVM 21+ em Windows/Linux.

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: remover, substituir (arquivo de até 5 MB) e excluir em menos de 5 s com o Oracle da faculdade; sem requisito de vazão.

**Constraints**: pool de 3 conexões — conexão só aberta depois da conferência e da gravação do arquivo, presa só pela transação curta; arquivo apagado **depois** do commit e fora da transação; sem log de nome de arquivo, nome interno, hash, chave ou confirmação; nenhuma chamada externa.

**Scale/Scope**: ~4 classes novas de produção e ~10 alteradas (repositórios, serviços, controllers, `Composicao`), 0 scripts SQL, 3 rotas, ~13 classes de teste novas (inclui os testes de Service, de armazenamento/banco indisponíveis e de injeção em arquivo) e 1 auxiliar `ConferirIntegridade`, ~12 documentos atualizados. Dados 100% sintéticos.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: só dados sintéticos; nenhuma promessa de fila real. |
| II. Limites clínicos | Atende: o sistema não lê nem interpreta arquivos; substituir não altera o documento clínico original (a paciente troca o **arquivo enviado**, que é o dela). |
| III. Monólito modular, Servlet/JDBC | Atende: Controller → Service → Repository, SQL parametrizado, sem dependência nova. `SolicitacaoService` passa a receber `ArmazenamentoPrivado` (infraestrutura comum), sem acoplar `solicitacao` a `documento`. |
| IV. Segurança e autorização por recurso | Atende: dona pela sessão; 404 idêntico para inexistente, alheio, de outro pedido, já removido ou malformado; perfis administrador e simulador recusados; confirmação explícita para o irreversível. |
| V. Motor determinístico / IA / dados não confiáveis | N/A (sem motor nem IA); o conteúdo do arquivo continua sem ser lido. |
| VI. Persistência, revisões e confiabilidade | Atende: toda mudança documental incrementa a revisão e invalida a conferência (D6); atômico sob `FOR UPDATE`; arquivo só depois do `COMMIT`; repetição não duplica (D3). Sem alteração destrutiva de esquema; a exclusão é ação da usuária, autorizada e confirmada. |
| VII. Verificação honesta | Atende com **limitação declarada**: estados de análise e submissão não são produzidos por nenhuma rota, então FR-013 a FR-016 são testados com pedidos montados em memória e **não** de ponta a ponta no Oracle. O relatório final dirá isso. |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nos novos casos de uso; contratos e estados documentados; sem refatoração ampla. |

**Gate**: passa, sem violação. **Reavaliação pós-design**: sem mudança; a nota do VII permanece.

### Pontos que merecem revisão

- O `docs/api/estados-e-revisoes.md` hoje diz que reenviar documento corrigido leva a `PROCESSANDO`. Esta história **não** dispara processamento (D6). A nota fica no documento e a decisão final pertence às histórias de análise/reenvio (RC-13, RC-16).
- A falha ao apagar arquivo depois do commit deixa arquivo sem registro (aceito, como na RC-08). O spec fala em "registrado para limpeza"; na prática é um `WARN` técnico sem nome interno, porque o nome interno é proibido em log. Spec ajustada.

## Project Structure

### Documentation (this feature)

```text
specs/010-substituir-excluir-documentos/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api-alteracao-documentos.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── documento/
│   ├── controller/DocumentoController.java        # + doDelete, doPut para …/{docId}; Allow
│   ├── service/DocumentoService.java              # + remover, substituir
│   ├── repository/DocumentoRepository.java        # + remover, substituir
│   ├── repository/DocumentoRepositoryJdbc.java    # transações FOR UPDATE
│   ├── repository/ResultadoRemocao.java           # novo
│   ├── repository/ResultadoRegistro.java          # + situação SUBMETIDO
│   ├── repository/ResultadoSubstituicao.java      # novo: embrulha ResultadoRegistro + documento antigo
│   └── dto/RemocaoResponse.java                   # novo: versão e disponíveis
├── solicitacao/
│   ├── controller/SolicitacaoController.java      # + doDelete (confirmação); Allow
│   ├── service/SolicitacaoService.java            # + excluirRascunho (recebe ArmazenamentoPrivado)
│   ├── repository/SolicitacaoRepository.java      # + excluirRascunho
│   ├── repository/SolicitacaoRepositoryJdbc.java
│   └── repository/ResultadoExclusao.java          # novo
└── config/Composicao.java                         # injeta ArmazenamentoPrivado no SolicitacaoService

backend/src/test/java/br/com/regulacheck/
├── documento/   # RemoverDocumentoEndpointTest, SubstituirDocumentoEndpointTest, IdempotenciaSubstituicaoTest,
│                # ConcorrenciaAlteracaoDocumentoTest, ConferenciaInvalidadaTest, AlteracaoDocumentoOracleTest (opt-in)
├── solicitacao/ # ExcluirRascunhoEndpointTest, PedidoSubmetidoSomenteConsultaTest, HigieneDeLogAlteracaoTest
└── (dobros em memória estendidos: DocumentoRepositoryMemoria, SolicitacaoRepositoryMemoria)

docs/ e CONTRATO_API.md, docs/api/{README,contratos-dados,erros,protecoes,autorizacao,estados-e-revisoes,openapi.yaml,Postman}
```

**Structure Decision**: o mesmo monólito modular das histórias anteriores. As rotas de documento continuam no `DocumentoController` (delegadas pelo `SolicitacaoController`, como na RC-08); o `DELETE` do pedido entra no `SolicitacaoController`, dono do caminho `/solicitacoes/{id}`.

## Complexity Tracking

Sem violações a justificar. Dependência de `SolicitacaoService` em `ArmazenamentoPrivado` é infraestrutura comum, não nova camada.
