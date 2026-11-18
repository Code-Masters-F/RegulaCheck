# Data Model: Administrador — protocolo e indicadores (RC-20)

Nenhuma tabela, coluna ou índice novo. Tudo é lido; nada é persistido.

## Leituras (Oracle, `database/scripts/01_criar_esquema.sql`)

| Indicador | Origem |
|---|---|
| Protocolos ativos e regras | `RC_PROTOCOLO (ATIVO = 1)` e `RC_REGRA` pela versão — via `ProtocoloRepository` existente |
| Total de pedidos | `COUNT(*)` de `RC_SOLICITACAO` |
| Pedidos em falha | `RC_SOLICITACAO.ESTADO = 'FALHA_ANALISE'` (mesma consulta do total) |
| Análises por resultado | `RC_ANALISE` com `VIGENTE = 1`, `GROUP BY RESULTADO_GERAL` |
| Pendências por regra | `RC_PENDENCIA` ⨝ `RC_ANALISE (VIGENTE = 1)` ⨝ `RC_REGRA`, `GROUP BY CODIGO, DESCRICAO`, separando `TIPO` |

## Tipos do backend (`br.com.regulacheck.indicador`)

- `repository.ContagensIndicadores(long totalPedidos, long pedidosEmFalhaAnalise, Map<ResultadoGeral, Long> analisesVigentesPorResultado, List<PendenciasDaRegra> pendenciasPorRegra)`; `PendenciasDaRegra(String codigo, String descricao, long pendentes, long naoVerificaveis)` com `total()`.
- `service.Indicadores(List<ProtocoloAtivo> protocolos, long totalPedidos, Map<ResultadoGeral, Long> analisesPorResultado /* 3 chaves */, long pedidosEmFalhaAnalise, List<PendenciasDaRegra> regrasComMaisPendencias /* ≤ 5 */, Instant consultadoEm)`; `ProtocoloAtivo(Protocolo protocolo, List<RegraDoProtocolo> regras)`.
- `dto.IndicadoresResponse` — contrato público de [contracts/admin-indicadores.md](contracts/admin-indicadores.md).

Validações: contagens ≥ 0; regras com total 0 não aparecem; ordem total desc, código asc; corte em 5.

## Tipos do app (`api/Modelos.kt`)

`Indicadores`, `ProtocoloIndicadores`, `RegraProtocolo`, `AnalisesPorResultado`, `RegraComPendencias` — espelho do contrato com `ignoreUnknownKeys`; campos numéricos `Long`.

## Sessão do app

`SessaoLocal.perfil` (já guardado, cifrado) passa a ser `PACIENTE` ou `ADMINISTRADOR`; decide a tela inicial (`MeusPedidos` × `Indicadores`).
