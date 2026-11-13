# Implementation Plan: Executar as regras do protocolo (RC-12)

**Branch**: `20-rc-12-executar-as-regras-do-protocolo` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-executar-regras-protocolo/spec.md`

## Summary

Motor de regras **determinístico** que aplica as regras da versão do protocolo do pedido à leitura atual (simulada, RC-10) e às decisões de conferência (RC-11), devolvendo resultado por regra, pendências explicadas e resultado geral. **Sem rota HTTP, sem gravação e sem mudança de estado** (decisão do responsável, 13/11/2026): a RC-13 grava, publica e muda o estado.

Abordagem:

1. **Regras versionadas** (D1): metadados lidos de `RC_REGRA` pela versão do pedido (nova consulta `ProtocoloRepository.listarRegras`); lógica em uma Strategy por código (`validacao.regra`); o motor recusa a versão inteira se faltar implementação, se a natureza divergir ou se não houver regras.
2. **Evidência = identificado e confirmado** (D2, D3): `EvidenciasDaLeitura` converte leitura + decisões em evidências por campo (`CONFIRMADO`, `NAO_LOCALIZADO`, `ILEGIVEL`, `NAO_CONFERIDO`, `DIVERGENCIA_APONTADA`, `DOCUMENTO_NAO_LIDO`). O tipo do documento é o lido e confirmado, nunca o informado no envio.
3. **Seis regras v0.1** (D4) conforme FR-012 a FR-017; todas avaliadas; precedência do resultado geral em `ResultadoGeral.de`.
4. **Pendências** (D8): mensagem e responsável da versão para `PENDENTE`; mensagem padrão do protocolo + motivo fechado + paciente para `NAO_VERIFICAVEL`; evidências com documento, campo, situação e origem.
5. **Montagem** (D5, D6): `AvaliacaoService.avaliar(sessao, solicitacaoId)` só de leitura, com dona (404 idêntico), recusa sem leitura atual (409) e conferência de coerência do retrato; data de referência = envio mais recente em Salvador.
6. **Documentação**: arquitetura, decisões, estados (nota), protocolo (interpretação do motor), README (estado real).

Fora desta história: rota e gravação de análises, histórico, concorrência e estado (RC-13), resumo por IA (RC-14), telas (RC-15/épico 007), reenvio (RC-16), submissão (RC-17).

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release` 21; JDK 25 local funciona).

**Primary Dependencies**: nenhuma nova (Jetty 12.1, HikariCP, ojdbc11, Jackson, SLF4J, JUnit Jupiter com testes parametrizados).

**Storage**: Oracle por JDBC, **somente leitura** nesta história; nova consulta parametrizada em `RC_REGRA`. Sem mudança de esquema nem script SQL.

**Testing**: JUnit 5. Unidade pura para regras e motor (entradas montadas à mão, gabarito independente em `quickstart.md`); montagem com Jetty + repositórios em memória (`CenarioDocumento`/`CenarioAuth`, leitor simulado com as amostras oficiais e de teste); teste Oracle **opt-in** (`RC_TESTE_ORACLE=SIM`) para `listarRegras`.

**Target Platform**: JVM 21+ em Windows/Linux. **Project Type**: web service (monólito modular, um módulo Maven).

**Performance Goals**: avaliação de até 3 documentos × 6 campos × 6 regras em memória, desprezível; montagem = 5 consultas curtas, sem transação nem bloqueio.

**Constraints**: motor sem HTTP/JDBC/IA/relógio; sem valor ou trecho de documento em log ou mensagem; pool de 3 conexões (cada consulta abre e fecha a sua); conteúdo de documento é dado, nunca instrução.

**Scale/Scope**: ~25 tipos de produção pequenos (enums/records do modelo, 6 regras, motor, exceção, montagem, serviço, 3 tipos de protocolo), 2 alterados (`ProtocoloRepository` + Jdbc; `ConferenciaService.visao` passa a público), ~9 classes de teste, 2 dobros ajustados. Dados 100% sintéticos.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: só amostras sintéticas; resultado marca leitura simulada; "sem pendências identificadas" não é aprovação. |
| II. Limites clínicos e integridade documental | Atende: não avalia conteúdo clínico (motivo só presente/ausente); ilegível nunca vira ausente; ausência → unidade emissora pela mensagem da versão; nada é escrito no documento. |
| III. Monólito modular, Servlet/JDBC | Atende: motor em `validacao` sem HTTP/JDBC/IA; Strategy por regra (necessidade concreta: 6 regras indexadas pelo código versionado); Service → Repository; SQL parametrizado; nenhuma dependência nova; sem rota. |
| IV. Segurança e autorização por recurso | Atende: paciente da sessão; dona aplicada na consulta e 404 idêntico (`Propriedade`); nenhum log com dado de documento. |
| V. Regras determinísticas, IA subordinada, dados não confiáveis | Atende: motor determinístico e versionado, todas as regras avaliadas, quatro resultados; sem IA; texto do documento é só valor (teste de injeção). |
| VI. Persistência, revisões e confiabilidade | Atende: nada é gravado; o resultado carrega revisão, leitura e `conferenciaAtualizadaEm` para a RC-13 publicar só se ainda forem atuais (compensação exigida pela RC-11); montagem recusa retrato incoerente. |
| VII. Verificação honesta | Atende com limitação declarada: leitura simulada; montagem verificada com dobros em memória; Oracle só para `listarRegras` (opt-in). |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nas regras, motor e serviço; docs de arquitetura/decisões; sem refatoração ampla (uma visibilidade alterada). |

**Gate**: passa, sem desvio. **Reavaliação pós-design**: sem mudança.

### Pontos que merecem revisão

- **Sem rota**: até a RC-13, a avaliação só é exercitada por testes (decisão do responsável). `AvaliacaoService` não é instanciado em `Composicao` para não haver objeto sem uso em produção.
- **Data de referência** = envio mais recente (D5). Se o grupo preferir a data da análise, a troca é local à montagem.
- `ConferenciaService.visao` muda de pacote-privado para público (reuso do cálculo de `atualizadaEm`; sem mudança de comportamento).

## Project Structure

### Documentation (this feature)

```text
specs/013-executar-regras-protocolo/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/avaliacao.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── protocolo/entity/RegraDoProtocolo.java, NaturezaRegra.java, ResponsavelCorrecao.java
├── protocolo/repository/ProtocoloRepository.java (+ listarRegras), ProtocoloRepositoryJdbc.java
├── validacao/model/Resultado, ResultadoGeral, MotivoNaoVerificavel, SituacaoEvidencia, CampoAvaliado,
│                   DocumentoAvaliado, EntradaAvaliacao, Evidencia, Veredito, ResultadoDeRegra, Pendencia,
│                   AvaliacaoDeRegras
├── validacao/regra/RegraDocumental, LeituraDosCampos, RegraEncaminhamentoPresente, RegraIdentificacaoCoerente,
│                   RegraDocumentoDeIdentificacao, RegraMotivoInformado, RegraValidadeDoEncaminhamento,
│                   RegraAutorizacaoDoResponsavel
├── validacao/motor/MotorDeRegras.java, ProtocoloNaoSuportadoException.java, package-info.java
├── analise/service/AvaliacaoService.java, AvaliacaoDoPedido.java, EvidenciasDaLeitura.java
└── extracao/service/ConferenciaService.java   # visao(...) passa a público

backend/src/test/java/br/com/regulacheck/
├── validacao/EntradasDeTeste.java (montagem de documentos), CasosDoProtocoloTest, MotorDeRegrasTest,
│             RegraEncaminhamentoEIdentificacaoTest, RegraIdentificacaoCoerenteTest,
│             RegrasDoEncaminhamentoTest, RegraAutorizacaoDoResponsavelTest
├── analise/EvidenciasDaLeituraTest, AvaliacaoServiceTest, HigieneDeLogAvaliacaoTest
├── protocolo/ProtocoloRepositoryMemoria (+ regras v0.1 iguais à carga 02_), RegrasDoProtocoloOracleTest (opt-in)
└── solicitacao/BancoIndisponivelSolicitacaoTest (dobro ganha listarRegras)

docs/arquitetura.md, docs/decisoes.md, docs/protocolo-academico.md (seção do motor),
docs/api/estados-e-revisoes.md (nota RC-12), README.md (estado real)
```

**Structure Decision**: monólito modular existente. O motor fica em `validacao` (`model`, `regra`, `motor`), como o `CLAUDE.md` §6 já previa; a montagem fica em `analise.service`, que a RC-13 completa com repository, entity, dto e controller. As regras versionadas são do domínio do protocolo (`protocolo.entity`/`repository`).

## Complexity Tracking

Sem violações. A única interface nova é `RegraDocumental` (Strategy com 6 implementações indexadas pelo código versionado, previsto no `CLAUDE.md` §5).
