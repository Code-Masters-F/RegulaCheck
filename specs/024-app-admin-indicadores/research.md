# Research: Administrador — protocolo e indicadores (RC-20)

Decisões técnicas da feature 024. As decisões de produto (o que contar) estão em `spec.md` § Clarifications.

## D1 — Onde agregar: SQL agregado no Repository, ordenação e corte no Service

- **Decision**: `IndicadorRepositoryJdbc` faz 3 consultas `COUNT`/`SUM ... GROUP BY` numa única conexão e devolve contagens brutas (`ContagensIndicadores`): total de pedidos, pedidos em `FALHA_ANALISE`, análises vigentes por `RESULTADO_GERAL` e pendências vigentes por regra (só regras com alguma). O `IndicadorService` completa os grupos ausentes com zero, ordena as regras (total desc, código asc), corta em 5 e junta os protocolos ativos via `ProtocoloRepository` existente.
- **Rationale**: nenhum dado individual sai do banco (só contagens); a regra de apresentação fica testável sem Oracle, com um repositório em memória de contagens fixas.
- **Alternatives considered**: (a) calcular em Java a partir das linhas de pedidos/análises — traria dados individuais para a memória sem necessidade; (b) `FETCH FIRST 5` no SQL — deixaria o critério de desempate só no banco e fora dos testes sem Oracle.

## D2 — Base das contagens

- **Decision**: análises e pendências só com `RC_ANALISE.VIGENTE = 1`; pedidos em falha pelo `RC_SOLICITACAO.ESTADO = 'FALHA_ANALISE'`; total de pedidos sem filtro.
- **Rationale**: decisão do clarify. `VIGENTE` já é zerado por documento enviado/removido e por conferência alterada (`DocumentoRepositoryJdbc`, `ConferenciaRepositoryJdbc`, `AnaliseRepositoryJdbc`), então reflete a versão atual de cada pedido.
- **Alternatives considered**: todas as análises (histórico) — contaria reenvios do mesmo pedido várias vezes.

## D3 — Agrupamento das regras com pendências

- **Decision**: agrupar por `RC_REGRA.CODIGO, RC_REGRA.DESCRICAO`.
- **Rationale**: o código é o identificador estável que a tela mostra; se duas versões de protocolo tiverem a mesma regra com descrições diferentes, aparecem separadas, sem misturar textos. A carga sintética tem uma só versão.
- **Alternatives considered**: por `REGRA_ID` com `protocoloId` na resposta — mais campos sem uso na tela.

## D4 — Nome do campo da regra: `descricao`

- **Decision**: o "nome" da regra sai como `descricao` (coluna `RC_REGRA.DESCRICAO`, ex.: "Encaminhamento presente").
- **Rationale**: o termo "nome" é reservado a pessoa na verificação de privacidade da RC-05 (`MatrizAdministradorTest` procura `nome`, `email`, `@`, `Paciente`, `pacienteId`, `caminho` no corpo); o mesmo filtro é aplicado à rota real.

## D5 — Autorização e montagem

- **Decision**: `rotas.registrar("/admin/indicadores", new IndicadorController(servico), Perfil.ADMINISTRADOR)` em `Composicao`; o `IndicadorRepository` entra em `Dependencias` (real em `Servidor`, memória nos testes). O Service não recebe a conta: a rota não tem recurso por dono (`CONTRATO_API.md`: "não se aplica").
- **Rationale**: único caminho de rota privada da RC-05; 401/403 antes do controller, logo antes de qualquer consulta (verificado por contador no repositório de teste).

## D6 — Falha do banco

- **Decision**: `SQLException` → `ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL", ...)` com log só do tipo e do código do fornecedor, como `ProtocoloRepositoryJdbc`. Nenhum código de erro novo em `docs/api/erros.md`.

## D7 — App: escolha de tela por perfil

- **Decision**: `ServicoSessao.entrar` guarda a sessão de `PACIENTE` e de `ADMINISTRADOR`; outro perfil continua recusado (logout remoto, nada guardado). Uma função pura `telaDaSessao(perfil)` decide `MeusPedidos` ou `Indicadores` e é usada na tela inicial e na reação à sessão aberta. A pilha é reiniciada ao trocar de conta (já era), então nenhuma tela de um perfil sobra para o outro.
- **Rationale**: muda só o ponto de entrada; a API continua sendo a proteção (403 nas rotas de paciente para o administrador e vice-versa).

## D8 — App: tela E4.2

- **Decision**: `IndicadoresViewModel` com `EstadoTela<Indicadores>` (Carregando, Sucesso, Falha), carga ao entrar e em "Tentar de novo"; sem retentativa automática; 401 já encerra a sessão no `ApiClient` (volta ao login); 403 → `BlocoRetorno(SEM_PERMISSAO)`; outros erros → "Não foi possível carregar os indicadores." + "Tentar de novo". Componentes: `EstruturaTela`, `CabecalhoMarca(aoSair)`, `TituloTela`, `Esqueleto`, `BlocoRetorno`, `AvisoRodape` (dentro da estrutura). Rótulos dos grupos reaproveitam os de resultado já usados nas telas da paciente onde existirem; textos novos ficam em `Textos.kt` e são registrados em `mobile/design/textos.md`.
- **Alternatives considered**: atualizar periodicamente — fora do desenho e contrário a "sem retentativa automática".
