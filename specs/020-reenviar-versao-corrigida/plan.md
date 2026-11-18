# Implementation Plan: Reenviar e conferir a versão corrigida (RC-16)

**Branch**: `38-rc-16-reenviar-e-conferir-a-versão-corrigida` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Especificação da RC-16 em `specs/020-reenviar-versao-corrigida/spec.md` (issue #38).

## Summary

Fechar o ciclo de correção no backend: (1) nova ação `POST /solicitacoes/{id}/reenvio` que orquestra, para a revisão atual, leitura → conferência → análise → resumo, reaproveitando os casos de uso das RC-10/11/13/14 e parando sem registrar análise quando a paciente ainda precisa conferir; (2) comparação calculada (`RESOLVIDA`/`CONTINUA`/`NOVA`) entre cada análise e a anterior, exposta como campo aditivo `comparacao` da análise; (3) inclusão de documento após a análise (exceto em pedido submetido), invalidando a análise vigente na mesma transação; (4) erros da verificação com a etapa e o que repetir, sem desfazer a última análise concluída. Sem tabela, script SQL, dependência ou framework novo.

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release`), backend existente; JDK local 25 compila para 21.

**Primary Dependencies**: Jakarta Servlet em Jetty embutido, JDBC Oracle, Jackson e SLF4J já declarados. Nenhuma dependência nova.

**Storage**: Oracle via JDBC (sem mudança de esquema). Única alteração JDBC: `DocumentoRepositoryJdbc.registrarBloqueado` aceita estados pós-análise e chama `invalidarAnalise` na mesma transação.

**Testing**: JUnit 5 com `ServidorDeTeste` e repositórios em memória (`CenarioDocumento`/`CenarioExtracao`, leitor simulado); teste unitário puro da comparação; teste Oracle opt-in para o caminho JDBC da inclusão. Build: `sh ./mvnw clean verify` (referência: 695 testes, 0 falhas, 31 ignorados).

**Target Platform**: API Java (Linux/Windows), consumida pelo app Android futuro (épico 6).

**Project Type**: Monólito modular web-service; novo pacote por funcionalidade `br.com.regulacheck.reenvio`.

**Performance Goals**: Verificação síncrona, como a análise da RC-13 (leitura e resumo simulados, sem chamada externa). A consulta do histórico lê uma análise a mais (`LIMITE_HISTORICO + 1`) para comparar a mais antiga devolvida.

**Constraints**: Nenhuma transação aberta entre etapas (cada caso de uso já usa transações curtas); análise só publicada se revisão e conferência ainda forem as avaliadas (RC-13); conteúdo de documento nunca em log ou mensagem; resumo e leitura sempre identificados como simulados; paciente não altera documento clínico.

**Scale/Scope**: Até 3 documentos e 6 regras por análise, histórico de até 50 análises. Fora: telas Android, usabilidade, submissão (RC-17), cópia de decisões de conferência entre revisões.

## Constitution Check

*GATE: verificado antes da pesquisa e novamente após o desenho (Phase 1).*

| Princípio | Conformidade |
|---|---|
| I. Escopo acadêmico | Respostas mantêm `simulada: true`, aviso do protocolo fictício e "resolvida"/"sem pendências" sem promessa de aprovação. Dados de teste sintéticos (`AmostrasSinteticas`). |
| II. Limites clínicos | Nada altera documento original; orientação da RC-15 preservada; inclusão é de documento novo, não edição. |
| III. Monólito modular | Controller (`ReenvioController`) só traduz HTTP; `ReenvioService` coordena Services existentes; nenhum acesso de Controller a Repository; pacote por funcionalidade; sem framework. |
| IV. Autorização | Rota dentro de `/solicitacoes/*` (`PACIENTE`); dona conferida por `SolicitacaoService.buscarPropria` e por cada caso de uso; 404 idêntico. |
| V. Regras determinísticas e IA subordinada | Comparação deriva de resultados determinísticos gravados; resumo segue o contrato validado da RC-14 e nunca altera análise. |
| VI. Revisões e confiabilidade | Inclusão incrementa revisão e invalida a vigente atomicamente; análise só publicada se a revisão ainda for a avaliada; repetição sem mudança não duplica; nenhum resultado parcial; sucesso só após persistir. |
| VII. Verificação honesta | Testes de concorrência, autorização entre contas, revisões desatualizadas e falhas por etapa; Oracle registrado como não verificado se não executado. |
| VIII. Documentação e mudanças mínimas | JavaDoc pt-BR nas classes novas; contrato, OpenAPI, erros, estados e decisões atualizados; sem refatoração ampla. |

**Gate inicial**: aprovado. **Gate após Phase 1**: aprovado; nenhuma violação a justificar.

## Project Structure

### Documentation (this feature)

```text
specs/020-reenviar-versao-corrigida/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/api-reenvio.md
├── quickstart.md
└── tasks.md            # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── analise/
│   ├── service/ComparacaoDeAnalises.java        # NOVO: comparação pura entre análise e anterior
│   ├── service/AnaliseService.java              # comparação em analisar() e consultar()
│   ├── service/AnaliseEmAndamentoException.java # construtor com mensagem e detalhes (etapa)
│   ├── dto/AnaliseResponse.java                 # campo aditivo 'comparacao'
│   ├── dto/HistoricoAnalisesResponse.java       # mapeia comparação por análise
│   └── controller/AnaliseController.java        # usa os novos retornos do serviço
├── resumo/
│   ├── service/ResumoService.java               # devolve a comparação junto
│   └── controller/ResumoController.java
├── reenvio/                                     # NOVO pacote (RC-16)
│   ├── package-info.java
│   ├── controller/ReenvioController.java
│   ├── service/ReenvioService.java
│   └── dto/ReenvioResponse.java
├── documento/
│   ├── service/DocumentoService.java            # inclusão fora de rascunho, recusa só submetido
│   ├── repository/DocumentoRepository.java      # JavaDoc da regra de estado
│   ├── repository/DocumentoRepositoryJdbc.java  # aceita pós-análise + invalidarAnalise
│   └── repository/ResultadoRegistro.java        # remove FORA_DE_RASCUNHO (sem uso)
├── solicitacao/controller/SolicitacaoController.java  # entrega /{id}/reenvio
└── config/Composicao.java                       # monta ReenvioService/Controller

backend/src/test/java/br/com/regulacheck/
├── analise/ComparacaoDeAnalisesTest.java        # NOVO
├── analise/ComparacaoEndpointTest.java          # NOVO
├── reenvio/ (ReenvioEndpointTest, FalhasDoReenvioTest, MatrizPerfisReenvioTest,
│             HigieneDeLogReenvioTest, ReenvioOracleTest)   # NOVOS
├── documento/InclusaoAposAnaliseTest.java       # NOVO
├── documento/EnviarDocumentoEndpointTest.java   # regra de estado atualizada
├── documento/DocumentoRepositoryMemoria.java    # mesma regra + invalidação
└── solicitacao/PedidoSubmetidoSomenteConsultaTest.java  # inclusão em submetido → 409

docs/api/{contratos-dados.md, openapi.yaml, erros.md, estados-e-revisoes.md, autorizacao.md}
docs/{decisoes.md, arquitetura.md}
```

**Structure Decision**: Backend existente (`backend/`), monólito modular. O orquestrador fica em pacote próprio `reenvio` para não criar ciclo `analise ↔ resumo` (research D3). Nenhuma mudança em `mobile/` ou `database/scripts/`.

## Complexity Tracking

Sem violações da constituição a justificar.
