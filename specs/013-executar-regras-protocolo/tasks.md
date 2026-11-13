---

description: "Tarefas da RC-12 — Executar as regras do protocolo"
---

# Tasks: Executar as regras do protocolo (RC-12)

**Input**: Design documents from `specs/013-executar-regras-protocolo/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/avaliacao.md, quickstart.md

**Tests**: obrigatórios nesta história — a decisão de escopo (sem rota) faz dos testes a única verificação (spec, Success Criteria). Gabarito dos casos escrito à mão em `quickstart.md`, independente do código.

**Organization**: por história da spec. Caminhos relativos à raiz do repositório; código em `backend/src/main/java/br/com/regulacheck/` (abreviado `main/`) e testes em `backend/src/test/java/br/com/regulacheck/` (abreviado `test/`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: US1 a US4 da spec

---

## Phase 1: Setup

- [X] T001 Registrar a linha de base: `./mvnw clean verify` em `backend/` (total de testes, falhas, ignorados) antes de qualquer mudança, para comparar no fechamento

---

## Phase 2: Foundational (bloqueia todas as histórias)

**Purpose**: modelo do motor, regras versionadas lidas do banco e o motor genérico (com regras falsas nos testes).

- [X] T002 [P] Criar `NaturezaRegra`, `ResponsavelCorrecao` e o record `RegraDoProtocolo` em `main/protocolo/entity/` (JavaDoc: linha de `RC_REGRA`, versão do protocolo)
- [X] T003 Acrescentar `List<RegraDoProtocolo> listarRegras(long protocoloId)` a `main/protocolo/repository/ProtocoloRepository.java` e implementar em `ProtocoloRepositoryJdbc.java` (SQL parametrizado em `RC_REGRA`, `ORDER BY ORDEM, CODIGO`, sem filtro de `ATIVO`, 503 como nas demais consultas)
- [X] T004 Ajustar dobros: `test/protocolo/ProtocoloRepositoryMemoria.java` (regras da v0.1 com os mesmos textos da carga `database/scripts/02_carga_sintetica.sql`, configuráveis por protocolo) e `test/solicitacao/BancoIndisponivelSolicitacaoTest.java` (`ProtocolosQueFalham.listarRegras`)
- [X] T005 [P] Criar os tipos de `main/validacao/model/`: `Resultado`, `ResultadoGeral` (com `de(...)` e a precedência do protocolo), `MotivoNaoVerificavel`, `SituacaoEvidencia`, `CampoAvaliado`, `DocumentoAvaliado`, `EntradaAvaliacao`, `Evidencia`, `Veredito`, `ResultadoDeRegra`, `Pendencia`, `AvaliacaoDeRegras` (cópias defensivas; `toString` de `CampoAvaliado`/`Evidencia` sem valor nem trecho)
- [X] T006 [P] Criar `main/validacao/regra/RegraDocumental.java` (Strategy: `codigo()`, `natureza()`, `avaliar(EntradaAvaliacao)`) e `LeituraDosCampos.java` (tipo do documento lido e confirmado; data `AAAA-MM-DD` estrita; `SIM`/`NAO`; nome normalizado sem acento, caixa e espaços extras; tudo que não interpreta → não interpretável)
- [X] T007 Criar `main/validacao/motor/MotorDeRegras.java` e `ProtocoloNaoSuportadoException.java` (+ `package-info.java`): valida a versão (sem regras, código desconhecido, natureza divergente, código repetido → exceção, sem resultado parcial), avalia **todas** as regras na ordem, monta pendências (FR-019 a FR-021, `MENSAGEM_NAO_VERIFICAVEL` igual à da carga `02_`) e o resultado geral
- [X] T008 [P] Criar `test/validacao/EntradasDeTeste.java` (montagem legível de documentos e campos com cada `SituacaoEvidencia`) e `test/validacao/MotorDeRegrasTest.java` com regras falsas: todas avaliadas mesmo após pendência, precedência do geral (inclui `NAO_APLICAVEL` neutro), ordem por `ORDEM`, uma pendência por `PENDENTE`/`NAO_VERIFICAVEL` e nenhuma nas demais, mensagem/responsável da versão vs. padrão + paciente, as quatro falhas de versão, determinismo (duas avaliações iguais)

**Checkpoint**: `./mvnw -q -Dtest=MotorDeRegrasTest test` verde.

---

## Phase 3: User Story 1 — Resultado de cada critério e resultado geral (P1) 🎯 MVP

**Goal**: as regras obrigatórias R01 a R05 avaliadas sobre evidências confirmadas.

**Independent Test**: testes por regra com entradas montadas à mão (ver FR-012 a FR-016 e research D4).

- [X] T009 [P] [US1] Implementar `main/validacao/regra/RegraEncaminhamentoPresente.java` (RC-R01) e `RegraDocumentoDeIdentificacao.java` (RC-R03), já com as evidências (FR-022) em cada veredito, como em T010, T011 e T015
- [X] T010 [P] [US1] Implementar `main/validacao/regra/RegraIdentificacaoCoerente.java` (RC-R02: divergência comprovada prevalece; < 2 documentos comparáveis → `DOCUMENTOS_INSUFICIENTES`)
- [X] T011 [P] [US1] Implementar `main/validacao/regra/RegraMotivoInformado.java` (RC-R04) e `RegraValidadeDoEncaminhamento.java` (RC-R05: 0 a 180 dias inclusive; futuro → pendente; sem encaminhamento → `DEPENDE_DE_DOCUMENTO_AUSENTE`)
- [X] T012 [P] [US1] `test/validacao/RegraEncaminhamentoEIdentificacaoTest.java`: R01/R03 com encaminhamento confirmado, sem nenhum, com documento não lido, tipo não conferido, tipo não interpretável, tipo informado diferente do lido, identificação com nome/data ilegível ou não localizada
- [X] T013 [P] [US1] `test/validacao/RegraIdentificacaoCoerenteTest.java`: nomes iguais com acento/caixa/espaços diferentes (atende), letras diferentes (pendente), datas diferentes (pendente), um só documento comparável, documento não lido, divergência apontada, divergência comprovada + ilegível (pendente)
- [X] T014 [P] [US1] `test/validacao/RegrasDoEncaminhamentoTest.java`: R04 motivo confirmado/não localizado/ilegível/não conferido, texto de instrução no motivo (só atende, nada mais muda), dois encaminhamentos (um sem motivo → pendente); R05 180 dias (atende), 181 (pendente), emissão posterior (pendente), não localizada (pendente), data não interpretável, sem encaminhamento

**Checkpoint**: R01 a R05 verdes isoladamente.

---

## Phase 4: User Story 2 — Regra condicional (P1)

**Goal**: RC-R06 só exigida para menor de 18; condição desconhecida → não verificável. Com ela, o motor aceita a v0.1 inteira.

**Independent Test**: variar só a data de nascimento da identificação; depois os 5 casos do protocolo.

- [X] T015 [US2] Implementar `main/validacao/regra/RegraAutorizacaoDoResponsavel.java` (RC-R06: data de nascimento só do documento de identificação; indeterminada → `CONDICAO_INDETERMINADA`; 18 anos na data → não aplicável)
- [X] T016 [US2] Completar `MotorDeRegras.doProtocoloAcademico()` em `main/validacao/motor/MotorDeRegras.java` com as seis implementações
- [X] T017 [P] [US2] `test/validacao/RegraAutorizacaoDoResponsavelTest.java`: adulta (não aplicável), 15 anos sem autorização (pendente), com `SIM` confirmado (atende), `NAO` confirmado (pendente), data ilegível/não conferida/divergente (indeterminada mesmo com data no encaminhamento), sem identificação, duas identificações com datas diferentes, 18º aniversário na data de referência, menor com documento não lido e sem `SIM`
- [X] T018 [US2] `test/validacao/CasosDoProtocoloTest.java`: os 5 casos do gabarito de `quickstart.md` (30 resultados + 5 gerais) com as regras da v0.1 de `ProtocoloRepositoryMemoria`, mais o caso 3 com identificação borrada (data ilegível); conferir que as explicações de `PENDENTE` são as mensagens da versão (FR-027: textos do protocolo, sem diagnóstico)

**Checkpoint**: SC-001 verificável.

---

## Phase 5: User Story 3 — Pendência explicada, com evidência e responsável (P1)

**Goal**: cada pendência traz regra, tipo, explicação, motivo, evidência e responsável corretos.

**Independent Test**: casos 2 a 5 e injeção.

- [X] T019 [US3] Conferir (e corrigir onde faltar) as evidências devolvidas por cada regra em `main/validacao/regra/*.java`: documento, campo, situação, página e trecho quando houver; evidência vazia só quando não há documento a apontar
- [X] T020 [P] [US3] Acrescentar a `test/validacao/CasosDoProtocoloTest.java` as pendências esperadas por caso (código, tipo, mensagem da versão ou padrão, motivo, responsável, documentos e campos da evidência) e o caso de injeção (mesmos resultados, explicações e responsáveis que o caso 1)

**Checkpoint**: SC-003 e SC-006 verificáveis.

---

## Phase 6: User Story 4 — Avaliar o que foi lido e conferido no próprio pedido (P1)

**Goal**: montar a avaliação a partir da leitura atual e da conferência, só para a dona, sem alterar nada.

**Independent Test**: amostras sintéticas pela API em memória (ver `quickstart.md` §3).

- [X] T021 [US4] Tornar público `ConferenciaService.visao(ConferenciaRegistrada)` em `main/extracao/service/ConferenciaService.java` (JavaDoc: usado pela avaliação para `atualizadaEm`)
- [X] T022 [P] [US4] Criar `main/analise/service/EvidenciasDaLeitura.java` (tabela D2 do research; decisão só conta se `SituacaoConferencia.aplicavel`)
- [X] T023 [US4] Criar `main/analise/service/AvaliacaoDoPedido.java` e `AvaliacaoService.java` (D5/D6: dona → 404 idêntico; sem leitura / desatualizada / retrato incoerente → 409 `CONFLITO_ESTADO`; regras da versão do pedido; data de referência em `America/Bahia`; um log `INFO` só com ids, revisão, geral e contagens); atualizar `main/analise/service/package-info.java`
- [X] T024 [P] [US4] `test/analise/EvidenciasDaLeituraTest.java`: cada linha da tabela D2, decisão incoerente ignorada, origem preservada, valor só em confirmado
- [X] T025 [US4] `test/analise/AvaliacaoServiceTest.java` (API em memória + amostras): sem confirmação → `REQUER_CONFERENCIA`/`TRANSCRICAO_NAO_CONFERIDA`; tudo confirmado → `SEM_PENDENCIAS_IDENTIFICADAS`; divergência apontada → não verificável; identificação borrada; paciente B e pedido inexistente → mesma recusa 404; sem leitura → 409; leitura desatualizada (documento enviado depois) → 409; data de referência = envio mais recente; estado, revisão, leitura e decisões inalterados; duas avaliações iguais; metadados (`protocolo`, `revisao`, `extracaoId`, `conferenciaAtualizadaEm`, `leituraSimulada = true`); pedido em `SUBMETIDA_SIMULACAO` continua avaliável só para leitura; repositório indisponível → 503 propagado, sem resultado
- [X] T026 [P] [US4] `test/analise/HigieneDeLogAvaliacaoTest.java`: avaliar o caso de injeção e conferir que o log não contém valores nem trechos lidos (padrão de `HigieneDeLogConferenciaTest`)
- [X] T027 [P] [US4] `test/protocolo/RegrasDoProtocoloOracleTest.java` (opt-in `RC_TESTE_ORACLE=SIM`): `listarRegras` da v0.1 devolve as 6 regras em ordem, com natureza e responsável esperados, e `MotorDeRegras.doProtocoloAcademico()` aceita a versão (caso 1 → sem pendências)

**Checkpoint**: SC-004, SC-005, SC-007 e SC-008 verificáveis.

---

## Phase 7: Polish & documentação

- [X] T028 [P] `docs/arquitetura.md`: parágrafo da RC-12 (motor em `validacao`, montagem em `analise.service`, sem rota) e regra de dependência do motor
- [X] T029 [P] `docs/decisoes.md`: decisões D1 a D8 resumidas (escopo sem rota, data de referência, evidência confirmada, tipo lido, regras conservadoras) e o que fica para a RC-13
- [X] T030 [P] `docs/protocolo-academico.md`: seção "Aplicação pelo motor (RC-12)" com a interpretação das evidências e os motivos de não verificável (sem mudar as regras v0.1)
- [X] T031 [P] `docs/api/estados-e-revisoes.md` (nota: RC-12 não muda estado; entrega revisão e `conferenciaAtualizadaEm` para a RC-13) e `README.md` ("Estado real": motor existe, sem rota)
- [X] T032 Executar `./mvnw clean verify` em `backend/`, comparar com T001 e registrar comando e números reais em "Resultado da verificação" abaixo; rodar o T027 só com autorização e credenciais
- [X] T033 Fechamento: preencher a rastreabilidade RC-12 → FR/SC → tarefas → arquivos → verificação; atualizar o status de `spec.md`; revisar o diff (sem segredos, sem dados reais, sem código morto)

---

## Dependencies & Execution Order

- T001 → Phase 2. T002 → T003 → T004 (o dobro precisa do método novo). T005 → T006 → T007 → T008.
- US1 (T009–T014) depende da Phase 2. US2 (T015–T018) depende de US1 (os casos precisam das 6 regras). US3 (T019–T020) depende de US2. US4 (T021–T027) depende da Phase 2 e de T016 (motor completo); T022/T024 podem começar logo após a Phase 2.
- Polish depois de todas as histórias; T032 antes de T033.

### Parallel Opportunities

- Phase 2: T002, T005 e (após T003) T004 em paralelo; T006 após T005.
- US1: T009, T010, T011 em paralelo; T012, T013, T014 em paralelo após suas regras.
- US4: T022 e T024 em paralelo com US1/US2; T026 e T027 em paralelo após T023.
- Polish: T028–T031 em paralelo.

## Implementation Strategy

MVP = Phase 2 + US1 + US2 (motor completo e 5 casos verdes). Depois US3 (pendências conferidas) e US4 (montagem a partir do pedido). Se faltar tempo até 15/11, cortar primeiro partes de T030/T031 (documentação secundária), **nunca** os testes de isolamento, de não verificável e de determinismo.

## Rastreabilidade

| Item | Requisitos | Tarefas | Arquivos principais | Verificação |
|---|---|---|---|---|
| RC-12 AC1 — regras versionadas, 4 resultados | FR-001 a FR-006, FR-026 | T002–T008, T016 | `protocolo/entity/RegraDoProtocolo`, `ProtocoloRepository(.Jdbc).listarRegras`, `validacao/model/*`, `validacao/motor/MotorDeRegras` | `MotorDeRegrasTest` (12), `CasosDoProtocoloTest` (8); Oracle: `RegrasDoProtocoloOracleTest` **não executado** |
| RC-12 AC2 — condicional / condição desconhecida | FR-017, FR-018 | T015, T017 | `validacao/regra/RegraAutorizacaoDoResponsavel` | `RegraAutorizacaoDoResponsavelTest` (11), casos 3 e 4 |
| RC-12 AC3 — pendência com regra, explicação, evidência, responsável | FR-019 a FR-022, FR-011 | T007, T019, T020 | `validacao/model/Pendencia`, `Evidencia`, `MotorDeRegras.pendencia` | `CasosDoProtocoloTest` (pendências por caso, injeção, completude), `MotorDeRegrasTest` |
| RC-12 AC4 — sem pendências só com aplicáveis verificáveis e atendidos | FR-004, FR-007 a FR-010, FR-012 a FR-016 | T005, T009–T014 | `ResultadoGeral`, `LeituraDosCampos`, `Regra*` (R01–R05) | `RegraEncaminhamentoEIdentificacaoTest` (12), `RegraIdentificacaoCoerenteTest` (9), `RegrasDoEncaminhamentoTest` (10) |
| Montagem a partir do pedido (decisão de escopo) | FR-005, FR-018, FR-023 a FR-028 | T021–T026 | `analise/service/{AvaliacaoService,AvaliacaoDoPedido,EvidenciasDaLeitura}`, `ConferenciaService.visao` | `EvidenciasDaLeituraTest` (9), `AvaliacaoServiceTest` (14), `HigieneDeLogAvaliacaoTest` (1) |
| Documentação | Princípio VIII | T028–T031 | `docs/{arquitetura,decisoes,protocolo-academico,ambiente}.md`, `docs/api/estados-e-revisoes.md`, `README.md` | revisão do diff |

## Resultado da verificação

| Quando | Comando (em `backend/`) | Resultado |
|---|---|---|
| 13/11/2026, antes (T001) | `sh mvnw -q clean verify` | 523 testes, 0 falhas, 0 erros, 26 ignorados |
| 13/11/2026, depois (T032) | `sh mvnw -q clean verify` | **610 testes, 0 falhas, 0 erros, 27 ignorados**; `regulacheck-backend.jar` gerado. Novos: 86 executados + 1 ignorado (`RegrasDoProtocoloOracleTest`, opt-in) |

- O `mvnw` está no Git sem permissão de execução (modo `100644`); no Linux foi chamado com `sh mvnw`. Não foi alterado (fora do escopo).
- **Não verificado:** `RegrasDoProtocoloOracleTest` contra o Oracle da faculdade (precisa de `RC_TESTE_ORACLE=SIM`, credenciais e autorização). A montagem foi verificada só com os repositórios em memória; ela reutiliza consultas JDBC já verificadas no Oracle nas RC-07, RC-10 e RC-11, e a única consulta nova é `listarRegras`.
- **Fora desta história (não implementado):** rota `/solicitacoes/{id}/analises`, gravação em `RC_ANALISE`/`RC_RESULTADO_REGRA`/`RC_PENDENCIA`, histórico, concorrência e transições de estado (RC-13); resumo simulado (RC-14); telas.
- **Ajuste feito na implementação:** R04/R05 sem encaminhamento confirmado usam o motivo do documento de tipo não verificável (ex.: transcrição não conferida) e só usam `DEPENDE_DE_DOCUMENTO_AUSENTE` quando todos os tipos são conhecidos; spec (FR-015/FR-016) e research (D4) atualizados junto.
