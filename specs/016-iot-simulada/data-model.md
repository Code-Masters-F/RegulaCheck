# Data Model: IoT simulada (RC-21 + RC-22)

Tabelas existentes desde a RC-03 (`database/scripts/01_criar_esquema.sql`); esta feature acrescenta **apenas** um índice único (script `11_iot_sessao_captura.sql`).

## RC_SESSAO_CAPTURA (existente)

| Coluna | Tipo | Regra |
|---|---|---|
| `ID` | `NUMBER` identidade | PK |
| `SOLICITACAO_ID` | `NUMBER` | FK → `RC_SOLICITACAO`; a dona é a do pedido |
| `INICIADA_EM` | `TIMESTAMP WITH TIME ZONE` | padrão `SYSTIMESTAMP` UTC; gravado pelo servidor |
| `ENCERRADA_EM` | `TIMESTAMP WITH TIME ZONE` nulo | `NULL` = aberta; nenhuma rota encerra nesta entrega (só carga/script) |

**Novo**: `CREATE UNIQUE INDEX RC_SESSAO_CAPTURA_UK_ABERTA ON RC_SESSAO_CAPTURA (CASE WHEN ENCERRADA_EM IS NULL THEN SOLICITACAO_ID END)` — no máximo uma sessão aberta por pedido (linhas encerradas geram `NULL` e não entram no índice).

**Estados**: `ABERTA` (`ENCERRADA_EM IS NULL`) → `ENCERRADA` (fora desta entrega). Só aberta recebe leitura.

## RC_LEITURA_SENSOR (existente)

| Coluna | Tipo | Regra |
|---|---|---|
| `ID` | `NUMBER` identidade | PK |
| `SESSAO_ID` | `NUMBER` | FK → `RC_SESSAO_CAPTURA` |
| `TIPO` | `VARCHAR2(20)` | `LUMINOSIDADE` \| `DISTANCIA` (CHECK) |
| `VALOR` | `NUMBER(12,3)` | validado na API: luminosidade 0–200000; distância >0–500 |
| `UNIDADE` | `VARCHAR2(10)` | `LUX` com luminosidade, `CM` com distância (CHECK) |
| `MEDIDA_EM` | `TIMESTAMP WITH TIME ZONE` | informado pelo simulador; ≤ agora + 30 s |
| `RECEBIDA_EM` | `TIMESTAMP WITH TIME ZONE` | relógio do servidor (UTC), nunca do corpo |

Leituras não são alteradas nem apagadas, exceto na exclusão do rascunho (D7), que apaga leituras e sessões do pedido na mesma transação.

## Tipos Java (novos, pacote `br.com.regulacheck.iot`)

| Tipo | Camada | Conteúdo |
|---|---|---|
| `TipoSensor` | entity (enum) | `LUMINOSIDADE(LUX)`, `DISTANCIA(CM)` — amarra unidade ao tipo |
| `SessaoCaptura` | entity (record) | `id`, `solicitacaoId`, `iniciadaEm`, `encerradaEm` (nulo = aberta) |
| `LeituraSensor` | entity (record) | `id`, `sessaoId`, `tipo`, `valor` (`BigDecimal`), `medidaEm`, `recebidaEm` |
| `AberturaSessao` | repository (record) | resultado de abrir: `naoEncontrado` \| `submetido` \| `existente(sessao)` \| `criada(sessao)` |
| `CapturaRepository` | repository (interface) | `abrirSessao(solicitacaoId, pacienteId, agora)`, `sessaoAbertaExiste(sessaoId)`, `gravarLeitura(nova)` (sob bloqueio do pedido), `telemetriaDaPaciente(solicitacaoId, pacienteId)` |
| `CapturaRepositoryJdbc` | repository | SQL parametrizado, `FOR UPDATE` no pedido, `ROLLBACK` explícito |
| `TelemetriaDoPedido` | repository (record) | `sessao` (opcional) + última leitura por tipo (opcional) |
| `NovaLeitura` | repository (record) | dados validados para gravar: `sessaoId`, `tipo`, `valor`, `medidaEm`, `recebidaEm` |
| `FaixasDemonstrativas` | service | constantes D5 (faixas, 60 s, 30 s, limites físicos) |
| `SituacaoSensor` | service (enum) | `DENTRO_DA_FAIXA`, `ABAIXO_DA_FAIXA`, `ACIMA_DA_FAIXA`, `DESATUALIZADA`, `SEM_LEITURA` |
| `Orientacao` | service (enum) | código + texto fixo (D5) |
| `ClassificadorDeLeitura` | service | puro: (tipo, última leitura opcional, agora) → situação + orientação |
| `CapturaService` | service | abrir sessão, registrar leitura, consultar telemetria; autorização por recurso |
| `CapturaController` | controller | `POST /solicitacoes/{id}/sessoes-captura`, `GET /solicitacoes/{id}/leituras` (despachado pelo `SolicitacaoController`) |
| `LeituraSensorController` | controller | `POST /sessoes-captura/{id}/leituras` (via `registrarTelemetria`) |
| `SessaoCapturaResponse`, `LeituraSensorRequest`, `LeituraRegistradaResponse`, `TelemetriaResponse` | dto | contrato em [contracts/api-iot.md](contracts/api-iot.md) |

`Dependencias` ganha o campo `capturas` (`CapturaRepository`).

## Regras de validação (resumo)

- Sessão: dona do pedido pela sessão autenticada; pedido submetido → 409; aberta existente → devolvida.
- Leitura: `sessaoId` do corpo = `{id}` do caminho; tipo/unidade coerentes; valor finito dentro dos limites físicos; `medidaEm` ≤ agora + 30 s; sessão existente e aberta.
- Consulta: sempre dois sensores; situação pela precedência do D5; nunca erro por falta de sessão ou leitura.
