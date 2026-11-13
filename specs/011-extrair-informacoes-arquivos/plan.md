# Implementation Plan: Extrair informações dos arquivos (RC-10)

**Branch**: `011-extrair-informacoes-arquivos` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-extrair-informacoes-arquivos/spec.md`

## Summary

A paciente pede a leitura dos documentos do seu pedido e consulta os dados estruturados. A leitura é **simulada** (decisão de 12/11/2026): um adaptador atrás de contrato próprio devolve dados predefinidos para o PDF e a fotografia sintéticos. Duas rotas novas, privadas e só para `PACIENTE`:

| Rota | Efeito |
|---|---|
| `POST /solicitacoes/{id}/extracoes` | lê os documentos disponíveis da versão atual e guarda o resultado (201 novo, 200 já existia para a versão) |
| `GET /solicitacoes/{id}/extracoes` | devolve a última leitura, com `situacao` `ATUAL` ou `DESATUALIZADA` |

Abordagem:

1. **Contrato de leitura** em `integracao.extracao` (`LeitorDeDocumento`) e implementação **simulada** (`LeitorSimulado`) que identifica o arquivo pelo **SHA-256** e consulta um catálogo predefinido (recurso do classpath). Arquivo desconhecido → `LEITURA_VAZIA`, nunca campos inventados (D1, D2).
2. **Validação da saída do leitor** (`ValidadorDeLeitura`) com as mesmas regras que valeriam para um leitor real: códigos de campo da lista fechada, valor só em `IDENTIFICADO`, origem coerente, tamanhos. Saída fora do contrato = falha do leitor (D3).
3. **Mudança de esquema (script `08_`)**: três tabelas `RC_EXTRACAO`, `RC_EXTRACAO_DOCUMENTO`, `RC_EXTRACAO_CAMPO`. Única por `(pedido, revisão)`; o que é "atual" **deriva** da revisão do pedido, sem coluna de vigência (D4). Só roda no Oracle com autorização.
4. **Leitura fora da transação**: arquivos lidos do armazenamento e passados ao leitor **antes** de abrir a transação; a transação curta (`SELECT … FOR UPDATE` do pedido) confere que a revisão capturada ainda é a atual e grava tudo ou nada (D5, CLAUDE.md §10).
5. **Falha do leitor não grava nada** (503 `LEITURA_INDISPONIVEL`, pode repetir); `PROTEGIDO` e `LEITURA_VAZIA` são resultados **gravados** por documento, com motivo e orientação de reenvio, sem pendência nem resultado de completude (D6).
6. **Sem mudar o estado do pedido e sem análise** (D7); pedido `SUBMETIDA_SIMULACAO` recusa nova leitura (409), mantendo a consulta.
7. **Documentação**: `CONTRATO_API.md`, `docs/api/*`, OpenAPI, Postman, `modelo-de-dados`, `arquitetura`, `decisoes`, `database/scripts/README`.
8. **Amostras sintéticas** em `samples/` (um PDF e uma fotografia) e gabarito; o catálogo de **produção** aponta só para elas pelo hash. Os casos extras de teste (sem motivo, ilegível, conflito, injeção) ficam em um catálogo de `src/test/resources` com amostras também commitadas, para nenhum hash de fixture entrar em produção (D9).
9. **Pontos acrescentados na análise:** campo conflitante vira `ILEGIVEL` (D10); `tipoDocumental` e `TIPO_DOCUMENTO` lido não são reconciliados (D11); estado antes de idempotência no `POST` (D12); `LeitorDeDocumento` documenta o timeout de um leitor real (D13).

Fora desta história: conferência da transcrição (RC-11), regras e resultado geral (RC-12), persistência de análises (RC-13), IA (RC-14), OCR real, telas.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona).

**Primary Dependencies**: nenhuma nova (Jetty 12.1 EE10, HikariCP, driver Oracle, Jackson, SLF4J, PDFBox, JUnit).

**Storage**: Oracle por JDBC (novas `RC_EXTRACAO*`; `RC_SOLICITACAO`, `RC_DOCUMENTO` lidas) e a área privada de arquivos (`DOCUMENT_STORAGE_PATH`) só para leitura. **Mudança de esquema: `08_extracao.sql`** (e `01_` atualizado).

**Testing**: JUnit; Jetty real em porta livre com repositórios em memória e área de arquivos temporária (`ServidorDeTeste`, `CenarioDocumento`); leitor simulado e leitores falsos nos testes (protegido, vazio, falha, saída inválida); concorrência em memória; teste **opt-in** contra o Oracle (`RC_TESTE_ORACLE=SIM`).

**Target Platform**: JVM 21+ em Windows/Linux. **Project Type**: web service (monólito modular, módulo Maven único).

**Performance Goals**: ler um pedido com até 3 documentos em menos de 5 s (leitor simulado em memória); sem requisito de vazão.

**Constraints**: pool de 3 conexões (sem conexão aberta durante a leitura); sem valor extraído, nome de arquivo, hash ou caminho em log ou erro; conteúdo do documento é dado, nunca instrução; nenhuma chamada externa.

**Scale/Scope**: ~12 classes novas de produção (contrato, leitor simulado, validador, entidades/DTOs, repositório, serviço, controller), ~5 alteradas (`Composicao`, roteamento em `SolicitacaoController`, `SolicitacaoRepositoryJdbc.excluirRascunho`, `LeitorSimulado` com catálogo por construtor, docs); `DocumentoRepository` **não** muda (o `ExtracaoRepository` lê os documentos disponíveis direto de `RC_DOCUMENTO`), 1 script SQL, 2 rotas, ~10 classes de teste novas. Dados 100% sintéticos.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: documentos e valores sintéticos; leitura marcada como simulada em toda resposta. |
| II. Limites clínicos | Atende: só campos documentais das regras; nenhuma interpretação clínica, diagnóstico ou urgência. |
| III. Monólito modular, Servlet/JDBC | Atende: Controller → Service → Repository, SQL parametrizado, sem dependência nova; adaptador em `integracao.extracao`, sem decidir estado de negócio. |
| IV. Segurança e autorização por recurso | Atende: dona pela sessão; 404 idêntico para inexistente e alheio; administrador e simulador recusados. |
| V. Motor determinístico / IA / dados não confiáveis | Atende: o texto extraído é dado; saída do leitor validada contra contrato fechado; leitura não decide aprovação, regras ou estado. |
| VI. Persistência, revisões e confiabilidade | Atende: leitura presa à revisão capturada, gravada só se ainda compatível (`FOR UPDATE`); única por `(pedido, revisão)` (sem duplicar em repetição/concorrência); resultado só após gravar; sem transação durante a leitura. |
| VII. Verificação honesta | Atende com **limitação declarada**: o leitor é simulado; testes provam o contrato e o tratamento de falhas, não leitura real. |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nos casos de uso e no contrato; contratos e estados documentados. |

**Gate**: passa. **Reavaliação pós-design**: sem mudança.

### Pontos que merecem revisão

- **Esquema novo em banco compartilhado**: aditivo (só `CREATE TABLE`, prefixo `RC_`); aplicar o `08_` exige autorização específica.
- A leitura **não move** o pedido para `PROCESSANDO`/`REQUER_CONFERENCIA`; isso é decidido em RC-11/RC-12 (registrado, como na RC-09).
- A coluna `SIMULADA` fica com `CHECK = 1` (mesmo padrão de `RC_ANALISE.SIMULADO`): enquanto o leitor for simulado, nenhuma linha pode se passar por real.

## Project Structure

### Documentation (this feature)

```text
specs/011-extrair-informacoes-arquivos/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api-extracao.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── integracao/extracao/
│   ├── LeitorDeDocumento.java            # contrato (interface)
│   ├── ArquivoParaLeitura.java           # record: tipo, páginas, bytes (só em memória)
│   ├── LeituraDeDocumento.java           # record: situação, motivo, campos
│   ├── CampoLido.java                    # record: código, situação, valor, origem
│   ├── CodigoCampo.java / SituacaoCampo.java / SituacaoLeitura.java   # enums
│   ├── LeituraIndisponivelException.java
│   ├── LeitorSimulado.java               # adaptador mockado (catálogo por SHA-256)
│   └── ValidadorDeLeitura.java           # contrato fechado da saída
├── extracao/
│   ├── controller/ExtracaoController.java
│   ├── service/ExtracaoService.java
│   ├── repository/ExtracaoRepository.java / ExtracaoRepositoryJdbc.java / ResultadoGravacao.java
│   ├── entity/Extracao.java / ExtracaoDocumento.java / CampoExtraido.java
│   └── dto/ExtracaoResponse.java (+ DocumentoLidoResponse, CampoExtraidoResponse, OrigemResponse)
└── config/Composicao.java                # liga leitor, serviço e controller; rota /solicitacoes/{id}/extracoes

backend/src/main/resources/extracao-simulada/catalogo.json   # hash → leitura predefinida (só as 2 amostras oficiais)
backend/src/test/resources/extracao-simulada/                # catalogo-testes.json + amostras extras (só testes)

database/scripts/08_extracao.sql, 01_criar_esquema.sql (atualizado), README.md
samples/ (PDF e foto sintéticos + gabarito)

backend/src/test/java/br/com/regulacheck/
├── extracao/   # LeitorSimuladoTest, ValidadorDeLeituraTest, ExtrairEndpointTest, ConsultarExtracaoEndpointTest,
│               # LeituraMalSucedidaTest, ExtracaoDesatualizadaTest, ConcorrenciaExtracaoTest,
│               # MatrizPerfisExtracaoTest, HigieneDeLogExtracaoTest, ExtracaoOracleTest (opt-in)
└── (dobros: ExtracaoRepositoryMemoria; LeitorFalso)

docs/ e CONTRATO_API.md, docs/api/{README,contratos-dados,erros,autorizacao,estados-e-revisoes,openapi.yaml,Postman}
```

**Structure Decision**: o mesmo monólito modular. As rotas de extração ficam no `ExtracaoController`; como a RC-05 só aceita um registro por caminho, o `SolicitacaoController` (dono de `/solicitacoes/*`) entrega o subcaminho `…/extracoes` a ele, como já faz com `…/documentos`.

## Complexity Tracking

Sem violações a justificar. A interface `LeitorDeDocumento` é fronteira externa (exigida pelo CLAUDE.md §4 e §11); não há outra abstração nova.
