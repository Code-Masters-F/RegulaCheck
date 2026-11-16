# Implementation Plan: IoT simulada — leituras de dois sensores e alertas da estação de captura (RC-21 + RC-22)

**Branch**: `feat/epico-7-iot-simulada` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-iot-simulada/spec.md`

## Summary

Três rotas e um simulador tornam funcional a telemetria que o banco e o contrato já reservavam:

1. `POST /solicitacoes/{id}/sessoes-captura` — a paciente abre a sessão de captura do próprio pedido; no máximo uma aberta, reabertura devolve a existente (201/200).
2. `POST /sessoes-captura/{id}/leituras` — o simulador (chave `X-Chave-Simulador`) envia uma leitura por requisição; a API valida e grava.
3. `GET /solicitacoes/{id}/leituras` — a paciente recebe, para luminosidade e distância, a última leitura, a situação (dentro/abaixo/acima da faixa, desatualizada, sem leitura) e a orientação.
4. `simulador-iot/SimuladorSensores.java` — arquivo único executado pelo *source launcher* do JDK, com as sequências `normal`, `fora-da-faixa` e `recuperacao`.

Abordagem (detalhes em [research.md](research.md)): rotas pelo mecanismo da RC-05 (D1); abertura sob `FOR UPDATE` do pedido + índice único de sessão aberta (D2); validação em ordem 401→403→404→400 (D4); classificador puro com faixas centralizadas (D5); exclusão do rascunho passa a apagar sessões e leituras (D7); simulador sem dependências (D8). A tela do app fica para o Épico 6 (contrato em [contracts/api-iot.md](contracts/api-iot.md)).

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release` 21); simulador no mesmo JDK, via `java Arquivo.java`.

**Primary Dependencies**: nenhuma nova (Jetty 12.1, HikariCP, ojdbc11, Jackson, SLF4J, JUnit Jupiter). Simulador: só JDK (`java.net.http.HttpClient`).

**Storage**: Oracle por JDBC, SQL parametrizado, instantes em UTC. Tabelas existentes `RC_SESSAO_CAPTURA` e `RC_LEITURA_SENSOR`; novo índice único `RC_SESSAO_CAPTURA_UK_ABERTA` (script `11_iot_sessao_captura.sql`).

**Testing**: JUnit 5. Classificador puro; rotas com Jetty + repositórios em memória (`CenarioAuth` + `CapturaRepositoryMemoria`), `RelogioFixo` para atualidade, 10 threads para abertura simultânea; simulador executado como processo contra `ServidorDeTeste`. Oracle opt-in (`CapturaOracleTest`, `RC_TESTE_ORACLE=SIM`) e casos no `04_` — **não executáveis neste ambiente** (sem banco).

**Target Platform**: JVM 21+ em Windows/Linux. **Project Type**: web service (monólito modular, um módulo Maven) + script de demonstração.

**Performance Goals**: envio de leitura = 1 transação curta (busca da sessão aberta + insert); consulta = 2 consultas (sessão mais recente da dona; última leitura por tipo). Simulador: 1 leitura/s por padrão.

**Constraints**: chave e corpo nunca em log; sucesso só após `COMMIT`; servlets sem estado de requisição; sensores nunca bloqueiam outro fluxo; pool de 3 conexões.

**Scale/Scope**: ~19 tipos de produção novos em `iot` (entity 3, repository 5, service 5, controller 2, dto 4), ~6 alterados (`SolicitacaoController`, `SolicitacaoRepositoryJdbc`, `Dependencias`, `Composicao`, `Servidor`, JavaDoc de `package-info` do `iot`); 1 arquivo de simulador; ~8 classes de teste novas e ~5 construtores de `Dependencias` ajustados; 4 scripts SQL; docs de API, arquitetura, decisões, ambiente, modelo de dados, `simulador-iot/README.md`, README.

## Constitution Check

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: leituras simuladas, `simulada: true` e aviso em toda resposta; limites demonstrativos sem calibração; demonstração com pedido sintético. |
| II. Limites clínicos e integridade documental | Atende: sensores não tocam documento, leitura, análise nem estado; resposta afirma que não comprovam legibilidade. |
| III. Monólito modular, Servlet/JDBC | Atende: `CapturaController`/`LeituraSensorController` → `CapturaService` → `CapturaRepository` (interface por ser fronteira com o banco); classificador sem HTTP/JDBC; simulador fora do módulo Maven, sem Spring. |
| IV. Segurança e autorização por recurso | Atende: paciente pela sessão; dona no `FOR UPDATE` e na consulta; 404 idêntico; administrador 403; chave restrita à telemetria (`SimuladorFiltro`, comparação em tempo constante), lida de `IOT_API_KEY`; nada de chave/corpo em log. |
| V. Regras determinísticas, IA subordinada | Não se aplica (sem IA); classificação determinística e testável. |
| VI. Persistência e confiabilidade | Atende: abertura idempotente com bloqueio + índice único; leitura gravada antes do 201; simulador sem retentativa automática; exclusão de rascunho apaga telemetria na mesma transação; script aditivo manual. |
| VII. Verificação honesta | Atende com limitação declarada: Oracle não disponível aqui; `CapturaOracleTest` e casos do `04_` ficam **NÃO VERIFICADOS** até alguém do grupo rodar. Tela do app não existe: RC-22 verificada pela API. |
| VIII. Documentação e mudanças mínimas | Atende: JavaDoc nos novos tipos; docs de API/arquitetura/decisões/ambiente/modelo e README do simulador atualizados; nenhuma dependência nova. |

**Gate**: passa. Dois pontos justificados em Complexity Tracking. **Reavaliação pós-design**: sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/015-iot-simulada/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api-iot.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
simulador-iot/
├── SimuladorSensores.java        # novo: script Java (HttpClient), sequências reproduzíveis
└── README.md                     # alterado: execução, sequências, faixas, limitações

database/scripts/
├── 01_criar_esquema.sql          # alterado: índice RC_SESSAO_CAPTURA_UK_ABERTA para bancos novos
├── 04_testes_restricoes.sql      # alterado: casos de sessão aberta única
├── 11_iot_sessao_captura.sql     # novo: índice para bancos existentes
└── README.md                     # alterado: ordem de execução

backend/src/main/java/br/com/regulacheck/
├── iot/entity/TipoSensor, SessaoCaptura, LeituraSensor
├── iot/repository/CapturaRepository, CapturaRepositoryJdbc, AberturaSessao, TelemetriaDoPedido, NovaLeitura
├── iot/service/CapturaService, ClassificadorDeLeitura, FaixasDemonstrativas, SituacaoSensor, Orientacao
├── iot/controller/CapturaController, LeituraSensorController
├── iot/dto/SessaoCapturaResponse, LeituraSensorRequest, LeituraRegistradaResponse, TelemetriaResponse
├── solicitacao/controller/SolicitacaoController   # despacha sessoes-captura e leituras
├── solicitacao/repository/SolicitacaoRepositoryJdbc # exclusão apaga leituras e sessões
└── config/Dependencias (+ capturas), Composicao (registrarTelemetria + CapturaService), Servidor

backend/src/test/java/br/com/regulacheck/
├── iot/CapturaRepositoryMemoria, ClassificadorDeLeituraTest, LeituraSensorRequestTest,
│      AbrirSessaoEndpointTest, ConcorrenciaAberturaSessaoTest, EnviarLeituraEndpointTest,
│      ConsultarTelemetriaEndpointTest, MatrizPerfisIotTest, HigieneDeLogIotTest,
│      SimuladorSensoresProcessoTest, CapturaOracleTest (opt-in)
├── auth/CenarioAuth (+ capturas), solicitacao/ExcluirRascunhoEndpointTest (caso com sessão)
└── testes que constroem Dependencias (+ capturas)

docs/api/README.md, contratos-dados.md, erros.md, autorizacao.md, openapi.yaml, Postman;
docs/arquitetura.md, docs/decisoes.md, docs/ambiente.md, docs/modelo-de-dados.md, CONTRATO_API.md, README.md
```

**Structure Decision**: monólito modular existente; o pacote `iot` (hoje só `package-info`) recebe as camadas previstas no `CLAUDE.md` §6. O simulador fica em `simulador-iot/` como arquivo único, fora do build Maven.

## Complexity Tracking

| Ponto | Por que é necessário | Alternativa mais simples rejeitada porque |
|---|---|---|
| Índice único funcional (`CASE WHEN ENCERRADA_EM IS NULL …`) | Garante no banco "uma sessão aberta por pedido" (clarify Q4) mesmo fora do caminho com bloqueio | Restrição única comum em `SOLICITACAO_ID` impediria sessões encerradas antigas; sem índice, só o bloqueio protege |
| `SolicitacaoRepositoryJdbc` (RC-07/RC-09) passa a apagar sessões e leituras | Sem isso, abrir sessão faria a exclusão do rascunho falhar por FK, e os sensores bloqueariam um fluxo (contraria FR-020) | Manter a recusa — a paciente não excluiria o rascunho só por ter usado a estação |
